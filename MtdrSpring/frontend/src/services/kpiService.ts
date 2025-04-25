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
    try {
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
    } catch (error) {
      console.error('Error fetching user KPIs:', error);

      // Return default values on error
      return {
        taskCompletionRate: 0,
        taskCompletionTrend: [70, 75, 80, 85, 82, 88, 90],
        trendLabels: [
          'Week 1',
          'Week 2',
          'Week 3',
          'Week 4',
          'Week 5',
          'Week 6',
          'Week 7',
        ],
        onTimeCompletionRate: 75,
        overdueTasksRate: 15,
        inProgressRate: 10,
        ociResourcesUtilization: 85,
        tasksCompletedPerWeek: 12,
        workedHours: 38,
        plannedHours: 40,
        hoursUtilizationPercent: 95,
        averageTasksPerEmployee: 5,
        userId: userId,
      };
    }
  },

  getTeamKpis: async (
    teamId: number,
    startDate?: Date,
    endDate?: Date
  ): Promise<KpiData> => {
    try {
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
    } catch (error) {
      console.error('Error fetching team KPIs:', error);

      // Return default values on error
      return {
        taskCompletionRate: 0,
        taskCompletionTrend: [65, 70, 75, 80, 78, 85, 82],
        trendLabels: [
          'Week 1',
          'Week 2',
          'Week 3',
          'Week 4',
          'Week 5',
          'Week 6',
          'Week 7',
        ],
        onTimeCompletionRate: 80,
        overdueTasksRate: 12,
        inProgressRate: 8,
        ociResourcesUtilization: 90,
        tasksCompletedPerWeek: 35,
        workedHours: 155,
        plannedHours: 160,
        hoursUtilizationPercent: 97,
        averageTasksPerEmployee: 7,
        teamId: teamId,
      };
    }
  },
};

export default kpiService;
