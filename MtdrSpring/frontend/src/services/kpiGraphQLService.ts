import api from './api';
import { config } from '../lib/config';

export interface KpiGraphQLResult {
  data: {
    getKpiData: KpiResult;
  };
}

export interface KpiResult {
  data: KpiData;
  sprintData: SprintData[];
  sprintHours: SprintDataForPie[];
  sprintTasks: SprintDataForPie[];
  sprintsForTasks: SprintForTask[];
}

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
  startDate: string;
  endDate: string;
  userId?: number;
  teamId?: number;
}

export interface SprintData {
  id: number;
  name: string;
  entries: MemberEntry[];
  totalHours: number;
  totalTasks: number;
}

export interface MemberEntry {
  member: string;
  hours: number;
  tasksCompleted: number;
}

export interface SprintDataForPie {
  id: number;
  name: string;
  count: number;
}

export interface SprintForTask {
  sprintId: number;
  sprintName: string;
}

const kpiGraphQLService = {
  getKpiData: async (
    startSprintId: number,
    endSprintId?: number
  ): Promise<KpiGraphQLResult> => {
    if (!startSprintId) {
      throw new Error('Start sprint ID is required');
    }

    const query = `
      query GetKpiData($startSprintId: ID!, $endSprintId: ID) {
        getKpiData(startSprintId: $startSprintId, endSprintId: $endSprintId) {
          data {
            taskCompletionRate
            taskCompletionTrend
            trendLabels
            onTimeCompletionRate
            overdueTasksRate
            inProgressRate
            ociResourcesUtilization
            tasksCompletedPerWeek
            workedHours
            plannedHours
            hoursUtilizationPercent
            averageTasksPerEmployee
            startDate
            endDate
            userId
            teamId
          }
          sprintData {
            id
            name
            entries {
              member
              hours
              tasksCompleted
            }
            totalHours
            totalTasks
          }
          sprintHours {
            id
            name
            count
          }
          sprintTasks {
            id
            name
            count
          }
          sprintsForTasks {
            sprintId
            sprintName
          }
        }
      }
    `;

    try {
      const response = await api.post(`${config.apiEndpoint}/graphql`, {
        query,
        variables: {
          startSprintId: String(startSprintId),
          endSprintId: endSprintId ? String(endSprintId) : null,
        },
      });

      return response.data;
    } catch (error) {
      console.error('Error fetching KPI data with GraphQL:', error);
      throw error;
    }
  },
};

export default kpiGraphQLService;
