import { useState, useCallback, useMemo, memo } from 'react';
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
import TaskCard from '../components/TaskCard';
import { Task } from '../services/tasksService';
import taskService from '../services/tasksService';
import TaskCreateModal from '@/components/TaskCreateModal';
import TaskUpdateModal from '../components/TaskUpdateModal';
import { useTaskData } from '@/hooks/useTaskData';

export default function Tasks() {
  // Use custom hook for task data
  const {
    tasks,
    users,
    loading,
    error,
    activeTasksCount,
    onTimeCount,
    behindScheduleCount,
    beyondDeadlineCount,
    refreshTasks,
    getUserName,
  } = useTaskData();

  // Filtering and sorting states
  const [orderBy, setOrderBy] = useState('dueDate');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [showPastTasks, setShowPastTasks] = useState(false);
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [showUpdateModal, setShowUpdateModal] = useState(false);

  // Filter tasks based on criteria
  const filteredTasks = useMemo(() => {
    return tasks.filter((task) => {
      // Filter by done status
      if (!showPastTasks && task.done) {
        return false;
      }

      // Filter by priority
      if (priorityFilter && priorityFilter !== 'all' && task.priority !== priorityFilter) {
        return false;
      }

      // Filter by assignee
      if (assigneeFilter && assigneeFilter !== 'all') {
        return task.assigneeId === parseInt(assigneeFilter);
      }

      return true;
    });
  }, [tasks, showPastTasks, priorityFilter, assigneeFilter]);

  // Sort filtered tasks
  const sortedTasks = useMemo(() => {
    return [...filteredTasks].sort((a, b) => {
      if (orderBy === 'dueDate') {
        // Handle null due dates
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
      } else if (orderBy === 'created-at') {
        // Handle null creation dates
        const aDate = a.creation_ts || a.creationTs;
        const bDate = b.creation_ts || b.creationTs;
        if (!aDate) return 1;
        if (!bDate) return -1;
        return new Date(aDate).getTime() - new Date(bDate).getTime();
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
  }, [filteredTasks, orderBy]);

  // Task handlers
  const handleTaskCreated = useCallback(async () => {
    await refreshTasks();
  }, [refreshTasks]);

  const handleEditTask = useCallback((task: Task) => {
    setSelectedTask(task);
    setShowUpdateModal(true);
  }, []);

  const handleTaskUpdated = useCallback(async () => {
    await refreshTasks();
    setShowUpdateModal(false);
    setSelectedTask(null);
  }, [refreshTasks]);

  const handleDeleteTask = useCallback(async (taskId: number) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await taskService.deleteTask(taskId);
        await refreshTasks();
      } catch (err) {
        console.error('Error deleting task:', err);
        alert('Failed to delete task. Please try again.');
      }
    }
  }, [refreshTasks]);

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
                { label: 'All priorities', value: 'all' },
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
                { label: 'All assignees', value: 'all' },
                ...users.map((user) => ({
                  label: user.fullName,
                  value: user.id?.toString() || '',
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
                  key={(task.id || task.ID) + '-' + task.title}
                  task={task}
                  onEdit={handleEditTask}
                  onDelete={handleDeleteTask}
                  userName={getUserName(task.assigneeId)}
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

            <div className="bg-white h-full rounded-lg shadow-md p-6 flex flex-col justify-center items-center">
              <p className="text-4xl">There are</p>
              <p className="text-[130px]">
                {activeTasksCount}
              </p>
              <p className="text-4xl">active tasks</p>
            </div>

            <div className="flex flex-col gap-4 bg-white h-full justify-around items-center rounded-lg shadow-md p-6 min-h-48 text-2xl lg:text-3xl">
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

type SelectProps = {
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  values: { value: string; label: string }[];
};

const SelectOrder = memo(({
  label,
  placeholder,
  value,
  onChange,
  values,
}: SelectProps) => {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-[180px]">
        <SelectValue className="text-lg" placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel>{label}</SelectLabel>
          {values.map((option) => (
            <SelectItem 
              key={option.value || 'empty-value'} 
              value={option.value || 'empty-value'}
            >
              {option.label}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
});

SelectOrder.displayName = 'SelectOrder';