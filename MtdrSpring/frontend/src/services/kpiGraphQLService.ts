import api from './api';
import { config } from '../lib/config';

export interface KpiGraphQLResult {
  data: {
    kpiData: {
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
    };
    charts: {
      hoursByDeveloper: Array<{
        developerId: number;
        developerName: string;
        values: number[];
        sprints: string[];
      }>;
      tasksByDeveloper: Array<{
        developerId: number;
        developerName: string;
        values: number[];
        sprints: string[];
      }>;
      hoursBySprint: Array<{
        sprintId: number;
        sprintName: string;
        value: number;
      }>;
      tasksBySprint: Array<{
        sprintId: number;
        sprintName: string;
        value: number;
      }>;
      taskInformation: Array<{
        sprintId: number;
        sprintName: string;
        tasks: Array<{
          id: number;
          title: string;
          description?: string;
          status: string;
          priority: string;
          estimatedHours?: number;
          actualHours?: number;
          assigneeId?: number;
          assigneeName?: string;
          dueDate?: string;
          completedAt?: string;
        }>;
      }>;
    };
    insights: string;
  };
}

const kpiGraphQLService = {
  getKpiData: async (
    userId?: number,
    teamId?: number,
    startSprintId?: number,
    endSprintId?: number
  ): Promise<KpiGraphQLResult> => {
    if (!startSprintId) {
      throw new Error('Start sprint ID is required');
    }

    if (!userId && !teamId) {
      throw new Error('Either userId or teamId must be provided');
    }

    const query = `
      query GetKpiData($userId: ID, $teamId: ID, $startSprintId: ID!, $endSprintId: ID) {
        getKpiData(userId: $userId, teamId: $teamId, startSprintId: $startSprintId, endSprintId: $endSprintId) {
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
          charts {
            hoursByDeveloper {
              developerId
              developerName
              values
              sprints
            }
            tasksByDeveloper {
              developerId
              developerName
              values
              sprints
            }
            hoursBySprint {
              sprintId
              sprintName
              value
            }
            tasksBySprint {
              sprintId
              sprintName
              value
            }
            taskInformation {
              sprintId
              sprintName
              tasks {
                id
                title
                description
                status
                priority
                estimatedHours
                actualHours
                assigneeId
                assigneeName
                dueDate
                completedAt
              }
            }
          }
          insights
        }
      }
    `;

    try {
      const response = await api.post(
        `${config.apiEndpoint}/graphql`,
        {
          query,
          variables: {
            userId: userId ? String(userId) : null,
            teamId: teamId ? String(teamId) : null,
            startSprintId: String(startSprintId),
            endSprintId: endSprintId ? String(endSprintId) : null,
          },
        }
      );

      return response.data;
    } catch (error) {
      console.error('Error fetching KPI data with GraphQL:', error);
      throw error;
    }
  },
};

export default kpiGraphQLService;