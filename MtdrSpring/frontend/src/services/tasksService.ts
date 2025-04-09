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
    const response = await api.get(`/todolist`);
    return response.data;
  },

  getTaskById: async (id: number): Promise<Task> => {
    const response = await api.get(`/todolist/${id}`);
    return response.data;
  },

  createTask: async (task: Task): Promise<number> => {
    const response = await api.post(`/todolist`, task);
    const locationHeader = response.headers.location;
    return locationHeader ? parseInt(locationHeader) : 0;
  },

  createTaskWithEstimation: async (task: Task): Promise<number> => {
    const response = await api.post(`/tasks`, task);
    const locationHeader = response.headers.location;
    return locationHeader ? parseInt(locationHeader) : 0;
  },

  updateTask: async (id: number, task: Task): Promise<Task> => {
    const response = await api.put(`/todolist/${id}`, task);
    return response.data;
  },

  deleteTask: async (id: number): Promise<boolean> => {
    const response = await api.delete(`/todolist/${id}`);
    return response.data;
  },

  assignToSprint: async (taskId: number, sprintId: number): Promise<Task> => {
    const response = await api.post(
      `/tasks/${taskId}/assign-to-sprint/${sprintId}`
    );
    return response.data;
  },

  startTask: async (taskId: number, userId: number): Promise<Task> => {
    const response = await api.post(`/tasks/${taskId}/start?userId=${userId}`);
    return response.data;
  },

  completeTask: async (
    taskId: number,
    actualHours: number,
    comments?: string
  ): Promise<Task> => {
    let url = `/tasks/${taskId}/complete?actualHours=${actualHours}`;
    if (comments) {
      url += `&comments=${encodeURIComponent(comments)}`;
    }
    const response = await api.post(url);
    return response.data;
  },

  getSprintTasks: async (sprintId: number): Promise<Task[]> => {
    const response = await api.get(`/sprints/${sprintId}/tasks`);
    return response.data;
  },

  getUserActiveTasks: async (userId: number): Promise<Task[]> => {
    const response = await api.get(`/users/${userId}/active-tasks`);
    return response.data;
  },
};

export default taskService;
