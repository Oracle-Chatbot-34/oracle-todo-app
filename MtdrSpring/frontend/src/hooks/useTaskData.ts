import { useState, useEffect, useCallback } from 'react';
import taskService, { Task } from '../services/tasksService';
import userService, { User } from '../services/userService';
import { useAuth } from './useAuth';

export interface TaskDataHookReturn {
  tasks: Task[];
  users: User[];
  loading: boolean;
  error: string | null;
  activeTasksCount: number;
  onTimeCount: number;
  behindScheduleCount: number;
  beyondDeadlineCount: number;
  refreshTasks: () => Promise<void>;
  getUserName: (userId?: number) => string;
}

export const useTaskData = (): TaskDataHookReturn => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTasksCount, setActiveTasksCount] = useState(0);
  const [onTimeCount, setOnTimeCount] = useState(0);
  const [behindScheduleCount, setBehindScheduleCount] = useState(0);
  const [beyondDeadlineCount, setBeyondDeadlineCount] = useState(0);
  const { isAuthenticated } = useAuth();

  // Create a memoized function to get user name from ID
  const getUserName = useCallback((userId?: number) => {
    if (!userId) return 'Unassigned';
    const user = users.find((u) => u.id === userId);
    return user ? user.fullName : 'Unknown User';
  }, [users]);

  // Calculate task statistics
  const calculateTaskStats = useCallback((taskList: Task[]) => {
    const now = new Date();
    const active = taskList.filter((task) => !task.done);
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
  }, []);

  // Function to fetch tasks
  const fetchTasks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const tasksData = await taskService.getAllTasks();
      setTasks(tasksData);
      calculateTaskStats(tasksData);
    } catch (err) {
      console.error('Error fetching tasks:', err);
      setError('Failed to load tasks. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [calculateTaskStats]);

  // Function to fetch users
  const fetchUsers = useCallback(async () => {
    try {
      const usersData = await userService.getAllUsers();
      setUsers(usersData);
    } catch (err) {
      console.error('Error fetching users:', err);
      setError('Failed to load users. Please try again.');
    }
  }, []);

  // Function to refresh data
  const refreshTasks = useCallback(async () => {
    if (!isAuthenticated) return;
    await fetchTasks();
  }, [isAuthenticated, fetchTasks]);

  // Initial data load
  useEffect(() => {
    if (!isAuthenticated) return;
    
    const loadData = async () => {
      try {
        setLoading(true);
        await Promise.all([fetchTasks(), fetchUsers()]);
      } catch (err) {
        console.error('Error loading data:', err);
        setError('Failed to load data. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [isAuthenticated, fetchTasks, fetchUsers]);

  return {
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
  };
};