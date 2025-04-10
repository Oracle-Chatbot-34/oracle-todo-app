import api from './api';
import { config } from '../lib/config';

export interface KpiData {
  taskCompletionRate: number;
  taskCompletionTrend: number[];
  trendLabels: string[];
  onTimeCompletionRate: number;
  overdueTasksRate: number;
  inProgressRate: number;
  ociResourcesUtilization: number;
  tasksCompletedPerWeek: number;
  workedHours: number;
  plannedHours: number;
  hoursUtilizationPercent: number;
  averageTasksPerEmployee: number;
  startDate?: string;
  endDate?: string;
  userId?: number;
  teamId?: number;
}

const kpiService = {
  getUserKpis: async (
    userId: number,
    startDate?: Date,
    endDate?: Date
  ): Promise<KpiData> => {
    let url = `${config.apiEndpoint}/kpi/users/${userId}`;

    // Add date parameters if provided
    if (startDate || endDate) {
      url += '?';
      if (startDate) {
        url += `startDate=${startDate.toISOString()}`;
      }
      if (endDate) {
        url += (startDate ? '&' : '') + `endDate=${endDate.toISOString()}`;
      }
    }

    const response = await api.get(url);
    return response.data;
  },

  getTeamKpis: async (
    teamId: number,
    startDate?: Date,
    endDate?: Date
  ): Promise<KpiData> => {
    let url = `${config.apiEndpoint}/kpi/teams/${teamId}`;

    // Add date parameters if provided
    if (startDate || endDate) {
      url += '?';
      if (startDate) {
        url += `startDate=${startDate.toISOString()}`;
      }
      if (endDate) {
        url += (startDate ? '&' : '') + `endDate=${endDate.toISOString()}`;
      }
    }

    const response = await api.get(url);
    return response.data;
  },
};

export default kpiService;
