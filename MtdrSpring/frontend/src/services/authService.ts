import api from './api';
import { config } from '../lib/config';

interface LoginCredentials {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  username: string;
  fullName: string;
}

interface RegistrationData {
  username: string;
  password: string;
  fullName: string;
  role: string;
  employeeId: string;
}

const authService = {
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    const response = await api.post(
      `${config.authEndpoint}/login`,
      credentials
    );
    return response.data;
  },

  register: async (
    userData: RegistrationData
  ): Promise<Record<string, string>> => {
    const response = await api.post(
      `${config.authEndpoint}/register`,
      userData
    );
    return response.data;
  },

  logout: (): void => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('token');
  },
};

export default authService;
