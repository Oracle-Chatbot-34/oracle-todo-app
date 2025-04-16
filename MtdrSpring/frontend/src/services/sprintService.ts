import api from './api';
import { config } from '../lib/config';

export interface Sprint {
  id?: number;
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  teamId: number;
  createdAt?: string;
  updatedAt?: string;
}

const sprintService = {
  getAllSprints: async (): Promise<Sprint[]> => {
    const response = await api.get(`${config.apiEndpoint}/sprints`);
    return response.data;
  },

  getSprintById: async (id: number): Promise<Sprint> => {
    const response = await api.get(`${config.apiEndpoint}/sprints/${id}`);
    return response.data;
  },

  getTeamSprints: async (teamId: number): Promise<Sprint[]> => {
    const response = await api.get(
      `${config.apiEndpoint}/teams/${teamId}/sprints`
    );
    return response.data;
  },

  getActiveSprintForTeam: async (teamId: number): Promise<Sprint> => {
    const response = await api.get(
      `${config.apiEndpoint}/teams/${teamId}/active-sprint`
    );
    return response.data;
  },

  createSprint: async (sprint: Sprint): Promise<Sprint> => {
    const response = await api.post(`${config.apiEndpoint}/sprints`, sprint);
    return response.data;
  },

  updateSprint: async (id: number, sprint: Sprint): Promise<Sprint> => {
    const response = await api.put(
      `${config.apiEndpoint}/sprints/${id}`,
      sprint
    );
    return response.data;
  },

  startSprint: async (id: number): Promise<Sprint> => {
    const response = await api.post(
      `${config.apiEndpoint}/sprints/${id}/start`
    );
    return response.data;
  },

  completeSprint: async (id: number): Promise<Sprint> => {
    const response = await api.post(
      `${config.apiEndpoint}/sprints/${id}/complete`
    );
    return response.data;
  },

  deleteSprint: async (id: number): Promise<void> => {
    await api.delete(`${config.apiEndpoint}/sprints/${id}`);
  },

  getSprintBoard: async (id: number): Promise<Record<string, string>[]> => {
    const response = await api.get(`${config.apiEndpoint}/sprints/${id}/board`);
    return response.data;
  },
};

export default sprintService;
