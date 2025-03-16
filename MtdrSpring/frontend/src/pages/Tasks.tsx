import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { FileCheck, Plus } from 'lucide-react';
import taskService, { Task } from '../services/tasksService';
import userService, { User } from '../services/userService';
import TaskCreateModal from '@/components/TaskCreateModal';
import TaskUpdateModal from '../components/TaskUpdateModal';

export default function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [orderBy, setOrderBy] = useState('dueDate');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [showPastTasks, setShowPastTasks] = useState(false);
  const [activeTasksCount, setActiveTasksCount] = useState(0);
  const [onTimeCount, setOnTimeCount] = useState(0);
  const [behindScheduleCount, setBehindScheduleCount] = useState(0);
  const [beyondDeadlineCount, setBeyondDeadlineCount] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [showUpdateModal, setShowUpdateModal] = useState(false);

  const handleTaskCreated = (newTask: Task) => {
    setTasks((prevTasks) => [...prevTasks, newTask]);
  };

  const handleEditTask = (task: Task) => {
    setSelectedTask(task);
    setShowUpdateModal(true);
  };

  const handleTaskUpdated = (updatedTask: Task) => {
    setTasks((prevTasks) =>
      prevTasks.map((task) => (task.ID === updatedTask.ID ? updatedTask : task))
    );

    // Recalculate task statistics
    const now = new Date();
    const active = tasks.filter((task) => !task.done);
    setActiveTasksCount(active.length);

    let onTime = 0;
    let behindSchedule = 0;
    let beyondDeadline = 0;

    active.forEach((task) => {
      if (!task.dueDate) {
        onTime++;
      } else {
        const dueDate = new Date(task.dueDate);
        const daysUntilDue = Math.floor(
          (dueDate.getTime() - now.getTime()) / (1000 * 3600 * 24)
        );

        if (daysUntilDue < 0) {
          beyondDeadline++;
        } else if (daysUntilDue <= 2) {
          behindSchedule++;
        } else {
          onTime++;
        }
      }
    });

    setOnTimeCount(onTime);
    setBehindScheduleCount(behindSchedule);
    setBeyondDeadlineCount(beyondDeadline);
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // Fetch tasks and users in parallel
        const [tasksResponse, usersResponse] = await Promise.all([
          taskService.getAllTasks(),
          userService.getAllUsers(),
        ]);

        setTasks(tasksResponse);
        setUsers(usersResponse);

        // Calculate task statistics
        const now = new Date();
        const active = tasksResponse.filter((task) => !task.done);
        setActiveTasksCount(active.length);

        let onTime = 0;
        let behindSchedule = 0;
        let beyondDeadline = 0;

        active.forEach((task) => {
          if (!task.dueDate) {
            onTime++; // If no due date, consider it on time
          } else {
            const dueDate = new Date(task.dueDate);
            const daysUntilDue = Math.floor(
              (dueDate.getTime() - now.getTime()) / (1000 * 3600 * 24)
            );

            if (daysUntilDue < 0) {
              beyondDeadline++; // Already past due
            } else if (daysUntilDue <= 2) {
              behindSchedule++; // Due in 2 days or less
            } else {
              onTime++; // Due in more than 2 days
            }
          }
        });

        setOnTimeCount(onTime);
        setBehindScheduleCount(behindSchedule);
        setBeyondDeadlineCount(beyondDeadline);
      } catch (err) {
        console.error('Error fetching data:', err);
        setError('Failed to load tasks. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Apply filters and sorting to tasks
  const filteredTasks = tasks.filter((task) => {
    // Filter by done status
    if (!showPastTasks && task.done) {
      return false;
    }

    // Filter by priority
    if (priorityFilter && task.priority !== priorityFilter) {
      return false;
    }

    // Filter by assignee
    if (assigneeFilter && task.assigneeId !== parseInt(assigneeFilter)) {
      return false;
    }

    return true;
  });

  // Sort tasks
  const sortedTasks = [...filteredTasks].sort((a, b) => {
    if (orderBy === 'dueDate') {
      // Handle null due dates
      if (!a.dueDate) return 1;
      if (!b.dueDate) return -1;
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    } else if (orderBy === 'created-at') {
      // Handle null creation dates
      if (!a.creation_ts) return 1;
      if (!b.creation_ts) return -1;
      return (
        new Date(a.creation_ts).getTime() - new Date(b.creation_ts).getTime()
      );
    } else if (orderBy === 'priority') {
      // Sort by priority (assuming HIGH > MEDIUM > LOW)
      const priorityOrder: { [key: string]: number } = {
        HIGH: 0,
        MEDIUM: 1,
        LOW: 2,
      };
      const aPriority = a.priority ? priorityOrder[a.priority] ?? 999 : 999;
      const bPriority = b.priority ? priorityOrder[b.priority] ?? 999 : 999;
      return aPriority - bPriority;
    }
    return 0;
  });

  const handleDeleteTask = async (taskId: number) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await taskService.deleteTask(taskId);
        // Remove task from state
        setTasks(tasks.filter((task) => task.ID !== taskId));
      } catch (err) {
        console.error('Error deleting task:', err);
        alert('Failed to delete task. Please try again.');
      }
    }
  };

  // Find user name by ID
  const getUserName = (userId?: number) => {
    if (!userId) return 'Unassigned';
    const user = users.find((u) => u.id === userId);
    return user ? user.fullName : 'Unknown User';
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-start items-start p-6 lg:p-10 gap-y-6 bg-whitie w-full h-full rounded-lg shadow-xl">
        {/* Title */}
        <div className="flex flex-row items-center justify-between w-full">
          <div className="flex flex-row items-center gap-[20px]">
            <FileCheck className="w-8 h-8" />
            <p className="text-[24px] font-semibold">Task Manager</p>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
        </div>

        {/* Filter Bar */}
        <Card className="flex flex-row justify-between items-center w-full px-4 lg:px-6 shadow-lg">
          <div className="flex gap-4">
            <p className="text-2xl">Order by:</p>
            <SelectOrder
              label={'Due date'}
              placeholder={'Due date'}
              value={orderBy}
              onChange={setOrderBy}
              values={[
                { label: 'Due date', value: 'dueDate' },
                { label: 'Created at', value: 'created-at' },
                { label: 'Priority', value: 'priority' },
              ]}
            />

            <SelectOrder
              label={'Priority'}
              placeholder={'All priorities'}
              value={priorityFilter}
              onChange={setPriorityFilter}
              values={[
                { label: 'All priorities', value: '' },
                { label: 'High', value: 'HIGH' },
                { label: 'Medium', value: 'MEDIUM' },
                { label: 'Low', value: 'LOW' },
              ]}
            />

            <SelectOrder
              label={'Assignee'}
              placeholder={'All assignees'}
              value={assigneeFilter}
              onChange={setAssigneeFilter}
              values={[
                { label: 'All assignees', value: '' },
                ...users.map((user) => ({
                  label: user.fullName,
                  value: user.id!.toString(),
                })),
              ]}
            />
          </div>

          <div className="flex gap-4 items-center justify-center">
            <label
              htmlFor="past-tasks"
              className="text-lg font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
            >
              Show completed tasks
            </label>
            <Checkbox
              id="past-tasks"
              checked={showPastTasks}
              onCheckedChange={(checked) => setShowPastTasks(checked === true)}
            />
          </div>
        </Card>

        <div className="flex flex-col md:flex-row w-full gap-6 p-4 z-50">
          <div className="md:w-3/4 max-h-[calc(90vh-300px)] h-full flex flex-col space-y-4 overflow-y-auto pb-6 pr-2">
            {loading ? (
              <div className="flex justify-center items-center h-40">
                <p className="text-xl">Loading tasks...</p>
              </div>
            ) : sortedTasks.length === 0 ? (
              <div className="flex justify-center items-center h-40">
                <p className="text-xl">No tasks found matching your filters</p>
              </div>
            ) : (
              sortedTasks.map((task) => (
                <TaskCard
                  key={task.ID}
                  taskId={task.ID!}
                  title={task.title}
                  description={task.description}
                  created={
                    task.creation_ts ? new Date(task.creation_ts) : undefined
                  }
                  due={task.dueDate ? new Date(task.dueDate) : undefined}
                  autor={getUserName(task.assigneeId)}
                  status={task.status}
                  onDelete={handleDeleteTask}
                  onEdit={() => handleEditTask(task)}
                />
              ))
            )}
          </div>

          <div className="md:w-1/4 flex flex-col space-y-4">
            <button
              className="bg-primary shadow-xl text-white rounded-lg p-6 h-24 flex items-center justify-center text-3xl hover:scale-101 transition-transform ease-in-out duration-150"
              onClick={() => setShowCreateModal(true)}
            >
              <Plus className="mr-2" /> Create new task
            </button>

            <div className="bg-white h-full rounded-lg shadow-md p-6 min-h-40 flex flex-col justify-center items-center">
              <p className="text-4xl">There are</p>
              <p className="text-[130px] flex justify-center items-center">
                {activeTasksCount}
              </p>
              <p className="text-4xl">active tasks</p>
            </div>

            <div className="flex flex-col gap-4 bg-white h-full justify-around items-center rounded-lg shadow-md p-6 min-h-48">
              <p className="text-2xl lg:text-3xl">
                {onTimeCount} tasks are on time
              </p>
              <p className="text-2xl lg:text-3xl">
                {behindScheduleCount} tasks are behind schedule
              </p>
              <p className="text-2xl lg:text-3xl">
                {beyondDeadlineCount} tasks are beyond deadline
              </p>
            </div>
          </div>
        </div>
      </div>

      <TaskCreateModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onTaskCreated={handleTaskCreated}
      />

      <TaskUpdateModal
        isOpen={showUpdateModal}
        onClose={() => setShowUpdateModal(false)}
        onTaskUpdated={handleTaskUpdated}
        task={selectedTask}
      />
    </div>
  );
}

type TaskCardProps = {
  taskId: number;
  title: string;
  description?: string;
  created?: Date;
  due?: Date;
  autor: string;
  status?: string;
  onDelete: (taskId: number) => void;
  onEdit: () => void;
};

function TaskCard({
  taskId,
  title,
  description,
  created,
  due,
  autor,
  status,
  onDelete,
  onEdit,
}: TaskCardProps) {
  const getStatusColor = () => {
    switch (status) {
      case 'COMPLETED':
      case 'DONE':
        return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'DELAYED':
        return 'bg-yellow-100 text-yellow-800';
      case 'SELECTED_FOR_DEVELOPMENT':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="flex bg-card flex-col shrink-0 shadow-md justify-start w-full items-center p-6 rounded-xl gap-2 min-h-32">
      <div className="flex justify-between w-full items-center h-fit">
        <div className="flex items-center gap-3">
          <p className="font-bold text-2xl">{title}</p>
          {status && (
            <span
              className={`px-2 py-1 rounded text-sm font-medium ${getStatusColor()}`}
            >
              {status.replace(/_/g, ' ')}
            </span>
          )}
        </div>
        <p className="text-lg text-slate-700">{autor}</p>
      </div>

      {description && <p className="text-lg text-left w-full">{description}</p>}

      <div className="flex justify-between w-full h-fit items-center">
        <div className="flex flex-col gap-2 text-slate-800">
          {created && <p>Created: {created.toLocaleDateString()}</p>}
          {due && (
            <p className="text-slate-900">Due: {due.toLocaleDateString()}</p>
          )}
        </div>

        <div className="flex flex-col lg:flex-row w-fit min-w-[6.25rem] h-fit gap-2">
          <Button onClick={onEdit}>Edit</Button>
          <Button variant={'destructive'} onClick={() => onDelete(taskId)}>
            Delete
          </Button>
        </div>
      </div>
    </div>
  );
}

type SelectProps = {
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  values: { value: string; label: string }[];
};

function SelectOrder({
  label,
  placeholder,
  value,
  onChange,
  values,
}: SelectProps) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-[180px]">
        <SelectValue className="text-lg" placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>{label}</SelectLabel>
          {values.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
