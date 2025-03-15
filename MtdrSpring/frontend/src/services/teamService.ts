import api from './api';
import { config } from '../lib/config';

export interface Team {
  id?: number;
  name: string;
  description?: string;
  managerId?: number;
  createdAt?: string;
}

interface User {
  id?: number;
  username: string;
  fullName: string;
  role: string;
  employeeId?: string;
  createdAt?: string;
  updatedAt?: string;
}

const teamService = {
  getAllTeams: async (): Promise<Team[]> => {
    const response = await api.get(`${config.apiEndpoint}/teams`);
    return response.data;
  },

  getTeamById: async (id: number): Promise<Team> => {
    const response = await api.get(`${config.apiEndpoint}/teams/${id}`);
    return response.data;
  },

  createTeam: async (team: Team): Promise<Team> => {
    const response = await api.post(`${config.apiEndpoint}/teams`, team);
    return response.data;
  },

  updateTeam: async (id: number, team: Team): Promise<Team> => {
    const response = await api.put(`${config.apiEndpoint}/teams/${id}`, team);
    return response.data;
  },

  deleteTeam: async (id: number): Promise<void> => {
    await api.delete(`${config.apiEndpoint}/teams/${id}`);
  },

  getTeamMembers: async (teamId: number): Promise<User[]> => {
    const response = await api.get(
      `${config.apiEndpoint}/teams/${teamId}/members`
    );
    return response.data;
  },

  addMemberToTeam: async (teamId: number, userId: number): Promise<Team> => {
    const response = await api.post(
      `${config.apiEndpoint}/teams/${teamId}/members/${userId}`
    );
    return response.data;
  },

  removeMemberFromTeam: async (
    teamId: number,
    userId: number
  ): Promise<Team> => {
    const response = await api.delete(
      `${config.apiEndpoint}/teams/${teamId}/members/${userId}`
    );
    return response.data;
  },

  assignManager: async (teamId: number, userId: number): Promise<Team> => {
    const response = await api.post(
      `${config.apiEndpoint}/teams/${teamId}/manager/${userId}`
    );
    return response.data;
  },
};

export default teamService;
