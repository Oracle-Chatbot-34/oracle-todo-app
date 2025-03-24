import api from './api';

export interface Task {
  ID?: number;
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
  completedAt?: string;
}

const taskService = {
  getAllTasks: async (): Promise<Task[]> => {
    try {
      const response = await api.get(`/todolist`);
      return response.data;
    } catch (error) {
      console.error('Error fetching tasks:', error);
      return [];
    }
  },

  getTaskById: async (id: number): Promise<Task | null> => {
    try {
      const response = await api.get(`/todolist/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching task with ID ${id}:`, error);
      return null;
    }
  },

  createTask: async (task: Task): Promise<number> => {
    try {
      const response = await api.post(`/todolist`, task);
      const locationHeader = response.headers.location;
      return locationHeader ? parseInt(locationHeader) : 0;
    } catch (error) {
      console.error('Error creating task:', error);
      return 0;
    }
  },

  createTaskWithEstimation: async (task: Task): Promise<number> => {
    try {
      const response = await api.post(`/tasks`, task);
      const locationHeader = response.headers.location;
      return locationHeader ? parseInt(locationHeader) : 0;
    } catch (error) {
      console.error('Error creating task with estimation:', error);
      return 0;
    }
  },

  updateTask: async (id: number, task: Task): Promise<Task | null> => {
    try {
      const response = await api.put(`/todolist/${id}`, task);
      return response.data;
    } catch (error) {
      console.error(`Error updating task with ID ${id}:`, error);
      return null;
    }
  },

  deleteTask: async (id: number): Promise<boolean> => {
    try {
      const response = await api.delete(`/todolist/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error deleting task with ID ${id}:`, error);
      return false;
    }
  },

  assignToSprint: async (
    taskId: number,
    sprintId: number
  ): Promise<Task | null> => {
    try {
      const response = await api.post(
        `/tasks/${taskId}/assign-to-sprint/${sprintId}`
      );
      return response.data;
    } catch (error) {
      console.error(
        `Error assigning task ${taskId} to sprint ${sprintId}:`,
        error
      );
      return null;
    }
  },

  startTask: async (taskId: number, userId: number): Promise<Task | null> => {
    try {
      const response = await api.post(
        `/tasks/${taskId}/start?userId=${userId}`
      );
      return response.data;
    } catch (error) {
      console.error(`Error starting task ${taskId} for user ${userId}:`, error);
      return null;
    }
  },

  completeTask: async (
    taskId: number,
    actualHours: number,
    comments?: string
  ): Promise<Task | null> => {
    try {
      let url = `/tasks/${taskId}/complete?actualHours=${actualHours}`;
      if (comments) {
        url += `&comments=${encodeURIComponent(comments)}`;
      }
      const response = await api.post(url);
      return response.data;
    } catch (error) {
      console.error(`Error completing task ${taskId}:`, error);
      return null;
    }
  },

  getSprintTasks: async (sprintId: number): Promise<Task[]> => {
    try {
      const response = await api.get(`/sprints/${sprintId}/tasks`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching tasks for sprint ${sprintId}:`, error);
      return [];
    }
  },

  getUserActiveTasks: async (userId: number): Promise<Task[]> => {
    try {
      const response = await api.get(`/users/${userId}/active-tasks`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching active tasks for user ${userId}:`, error);
      return [];
    }
  },
};

export default taskService;
