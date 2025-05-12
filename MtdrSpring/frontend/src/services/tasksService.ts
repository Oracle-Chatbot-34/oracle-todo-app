import api from './api';
import { config } from '../lib/config';

export interface Task {
  id?: number;
  ID?: number; // For compatibility with backend response
  title: string;
  description?: string;
  dueDate?: string;
  assigneeId?: number;
  teamId?: number;
  status?: string;
  estimatedHours?: number;
  actualHours?: number;
  sprintId?: number;
  priority?: string;
  done?: boolean;
  creation_ts?: string;
  creationTs?: string; // For compatibility
  completedAt?: string;
}

const normalizeTask = (task: Partial<Task & { ID?: number; creationTs?: string; creation_ts?: string }>): Task => {
  // Handle different field names between backend and frontend
  return {
    id: task.ID || task.id,
    ID: task.ID || task.id,
    title: task.title || '',
    description: task.description,
    dueDate: task.dueDate,
    assigneeId: task.assigneeId,
    teamId: task.teamId,
    status: task.status,
    estimatedHours: task.estimatedHours,
    actualHours: task.actualHours,
    sprintId: task.sprintId,
    priority: task.priority,
    done: task.done,
    creation_ts: task.creationTs || task.creation_ts,
    creationTs: task.creationTs || task.creation_ts,
    completedAt: task.completedAt,
  };
};

const taskService = {
  getAllTasks: async (): Promise<Task[]> => {
    try {
      const response = await api.get(`${config.apiEndpoint}/tasks`);
      return response.data.map(normalizeTask);
    } catch (error) {
      console.error('Error fetching tasks:', error);
      return [];
    }
  },

  getTaskById: async (id: number): Promise<Task | null> => {
    try {
      const response = await api.get(`${config.apiEndpoint}/tasks/${id}`);
      return normalizeTask(response.data);
    } catch (error) {
      console.error(`Error fetching task with ID ${id}:`, error);
      return null;
    }
  },

  createTaskWithEstimation: async (task: Task): Promise<number> => {
    try {
      const response = await api.post(`${config.apiEndpoint}/tasks`, task);
      // If the response contains the full task, return its ID
      if (response.data && (response.data.id || response.data.ID)) {
        return response.data.id || response.data.ID;
      }
      // Otherwise, try to get it from the location header
      const locationHeader = response.headers.location;
      return locationHeader ? parseInt(locationHeader) : 0;
    } catch (error) {
      console.error('Error creating task with estimation:', error);
      throw error;
    }
  },

  updateTask: async (id: number, task: Task): Promise<Task> => {
    try {
      const response = await api.put(`${config.apiEndpoint}/tasks/${id}`, task);
      return normalizeTask(response.data);
    } catch (error) {
      console.error(`Error updating task with ID ${id}:`, error);
      throw error;
    }
  },

  deleteTask: async (id: number): Promise<boolean> => {
    try {
      const response = await api.delete(`${config.apiEndpoint}/tasks/${id}`);
      return response.data?.success === true;
    } catch (error) {
      console.error(`Error deleting task with ID ${id}:`, error);
      throw error;
    }
  },

  assignToSprint: async (taskId: number, sprintId: number): Promise<Task> => {
    try {
      const response = await api.post(
        `${config.apiEndpoint}/tasks/${taskId}/assign-to-sprint/${sprintId}`
      );
      return normalizeTask(response.data);
    } catch (error) {
      console.error(`Error assigning task ${taskId} to sprint ${sprintId}:`, error);
      throw error;
    }
  },

  startTask: async (taskId: number, userId: number): Promise<Task> => {
    try {
      const response = await api.post(
        `${config.apiEndpoint}/tasks/${taskId}/start?userId=${userId}`
      );
      return normalizeTask(response.data);
    } catch (error) {
      console.error(`Error starting task ${taskId} for user ${userId}:`, error);
      throw error;
    }
  },

  completeTask: async (
    taskId: number,
    actualHours: number,
    comments?: string
  ): Promise<Task> => {
    try {
      let url = `${config.apiEndpoint}/tasks/${taskId}/complete?actualHours=${actualHours}`;
      if (comments) {
        url += `&comments=${encodeURIComponent(comments)}`;
      }
      const response = await api.post(url);
      return normalizeTask(response.data);
    } catch (error) {
      console.error(`Error completing task ${taskId}:`, error);
      throw error;
    }
  },

  getSprintTasks: async (sprintId: number): Promise<Task[]> => {
    try {
      const response = await api.get(`${config.apiEndpoint}/tasks/sprint/${sprintId}`);
      return response.data.map(normalizeTask);
    } catch (error) {
      console.error(`Error fetching tasks for sprint ${sprintId}:`, error);
      return [];
    }
  },

  getUserActiveTasks: async (userId: number): Promise<Task[]> => {
    try {
      const response = await api.get(`${config.apiEndpoint}/tasks/user/${userId}/active`);
      return response.data.map(normalizeTask);
    } catch (error) {
      console.error(`Error fetching active tasks for user ${userId}:`, error);
      return [];
    }
  },

  getUserTaskStats: async (userId: number): Promise<{ 
    active: number, 
    completed: number, 
    overdue: number, 
    avgCompletionTime: number 
  }> => {
    try {
      const response = await api.get(`${config.apiEndpoint}/tasks/stats/user/${userId}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching task stats for user ${userId}:`, error);
      // Return default values
      return {
        active: 0,
        completed: 0,
        overdue: 0,
        avgCompletionTime: 0
      };
    }
  }
};

export default taskService;