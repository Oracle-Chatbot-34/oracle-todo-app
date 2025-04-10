import api from './api';
import { config } from '../lib/config';

export interface User {
  id?: number;
  username: string;
  password?: string;
  fullName: string;
  role: string;
  employeeId?: string;
  telegramId?: number;
  createdAt?: string;
  updatedAt?: string;
}

const userService = {
  getAllUsers: async (): Promise<User[]> => {
    const response = await api.get(`${config.apiEndpoint}/users`);
    return response.data;
  },

  getUserById: async (id: number): Promise<User> => {
    const response = await api.get(`${config.apiEndpoint}/users/${id}`);
    return response.data;
  },

  createUser: async (user: User): Promise<User> => {
    const response = await api.post(`${config.apiEndpoint}/users`, user);
    return response.data;
  },

  updateUser: async (id: number, user: User): Promise<User> => {
    const response = await api.put(`${config.apiEndpoint}/users/${id}`, user);
    return response.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await api.delete(`${config.apiEndpoint}/users/${id}`);
  },

  getUsersByRole: async (role: string): Promise<User[]> => {
    const response = await api.get(`${config.apiEndpoint}/users/roles/${role}`);
    return response.data;
  },
};

export default userService;
