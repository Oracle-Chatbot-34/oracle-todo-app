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
    try {
      const response = await api.get(`${config.apiEndpoint}/sprints`);
      console.log('Sprint service raw response:', response);

      let sprintsData;

      // Handle the API response structure
      if (response.data && typeof response.data === 'object') {
        if (Array.isArray(response.data)) {
          sprintsData = response.data;
        } else if (response.data.data && Array.isArray(response.data.data)) {
          // For {"data": [...]} structure
          sprintsData = response.data.data;
        } else if (
          response.data.success !== undefined &&
          Array.isArray(response.data.data)
        ) {
          // For {"success": true, "data": [...]} structure
          sprintsData = response.data.data;
        } else {
          // For object with numeric keys format like {"0": {...}, "1": {...}}
          sprintsData = Object.values(response.data);
        }
      } else {
        sprintsData = [];
      }

      console.log('Processed sprints data:', sprintsData);
      return sprintsData;
    } catch (error) {
      console.error('Error in getAllSprints:', error);
      return [];
    }
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
