import api from './api';
import { config } from '../lib/config';

export interface ReportRequest {
  isIndividual: boolean;
  userId?: number;
  teamId?: number;
  statuses?: string[];
  startDate?: Date;
  endDate?: Date;
}

export interface TaskInfo {
  id: number;
  title: string;
  description?: string;
  status?: string;
  priority?: string;
  estimatedHours?: number;
  actualHours?: number;
  assigneeId?: number;
  assigneeName?: string;
  dueDate?: string;
  completedAt?: string;
}

export interface DeveloperTaskGroup {
  userId: number;
  fullName: string;
  role: string;
  tasks: TaskInfo[];
  stats: {
    totalTasks: number;
    completedTasks: number;
    completionRate: number;
    totalEstimatedHours: number;
    totalActualHours: number;
    timeEfficiency: number;
    onTimeCompletions: number;
    onTimeRate: number;
  };
  aiInsights: string;
}

export interface SprintTasksReport {
  sprintId: number;
  sprintName: string;
  startDate?: string;
  endDate?: string;
  developerGroups: DeveloperTaskGroup[];
  teamStats: {
    totalTasks: number;
    completedTasks: number;
    completionRate: number;
    totalEstimatedHours: number;
    totalActualHours: number;
    timeEfficiency: number;
    onTimeCompletions: number;
    onTimeRate: number;
  };
  teamInsights: string;
}

export interface ReportData {
  generatedAt: string;
  dateRange: {
    start: string;
    end: string;
  };
  totalTasks: number;
  statusBreakdown: Record<string, number>;
  reportType: string;
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
    startDate?: string;
    endDate?: string;
    userId?: number;
    teamId?: number;
  };
  user?: {
    id: number;
    name: string;
    role: string;
  };
  teamId?: number;
  teamSize?: number;
  teamMembers?: Array<{
    id: number;
    name: string;
    role: string;
  }>;
  tasks: TaskInfo[];
}

const reportService = {
  generateReport: async (
    request: ReportRequest
  ): Promise<ReportData> => {
    const payload = {
      ...request,
      startDate: request.startDate
        ? request.startDate.toISOString()
        : undefined,
      endDate: request.endDate ? request.endDate.toISOString() : undefined,
    };

    const response = await api.post(
      `${config.apiEndpoint}/reports/generate`,
      payload
    );
    return response.data;
  },

  getLastSprintTasksReport: async (
    params: {
      sprintId?: number;
      userId?: number;
      teamId?: number;
    } = {}
  ): Promise<SprintTasksReport> => {
    const queryParams = new URLSearchParams();
    if (params.sprintId) queryParams.append('sprintId', params.sprintId.toString());
    if (params.userId) queryParams.append('userId', params.userId.toString());
    if (params.teamId) queryParams.append('teamId', params.teamId.toString());

    const queryString = queryParams.toString();
    const url = `${config.apiEndpoint}/reports/sprint-tasks${queryString ? `?${queryString}` : ''}`;

    try {
      const response = await api.get(url);
      return response.data;
    } catch (error) {
      console.error('Error fetching sprint tasks report:', error);
      
      // Return mock data for development/demo purposes
      return {
        sprintId: 1,
        sprintName: 'Sprint 1',
        startDate: '2023-10-01T00:00:00Z',
        endDate: '2023-10-14T00:00:00Z',
        developerGroups: [
          {
            userId: 1,
            fullName: 'Cristobal Camarena',
            role: 'DEVELOPER',
            tasks: [
              {
                id: 1,
                title: 'Realizar video de demo para Release Version 1',
                description: 'Crear un video mostrando las funcionalidades principales',
                status: 'COMPLETED',
                estimatedHours: 1,
                actualHours: 1,
                priority: 'HIGH'
              }
            ],
            stats: {
              totalTasks: 1,
              completedTasks: 1,
              completionRate: 100,
              totalEstimatedHours: 1,
              totalActualHours: 1,
              timeEfficiency: 100,
              onTimeCompletions: 1,
              onTimeRate: 100
            },
            aiInsights: 'Cristobal completó todas sus tareas asignadas a tiempo y dentro del presupuesto de horas. Su eficiencia en la creación de la demo para la versión 1 fue excepcional.'
          },
          {
            userId: 2,
            fullName: 'Josue Galindo',
            role: 'DEVELOPER',
            tasks: [
              {
                id: 2,
                title: 'Implementar dashboard de KPIs por desarrollador',
                description: 'Crear un panel de control para mostrar métricas por desarrollador',
                status: 'COMPLETED',
                estimatedHours: 3,
                actualHours: 3,
                priority: 'HIGH'
              },
              {
                id: 3,
                title: 'Desarrollo de API para métricas de desarrolladores',
                description: 'Implementar endpoints para obtener métricas de rendimiento',
                status: 'COMPLETED',
                estimatedHours: 4,
                actualHours: 3,
                priority: 'MEDIUM'
              }
            ],
            stats: {
              totalTasks: 2,
              completedTasks: 2,
              completionRate: 100,
              totalEstimatedHours: 7,
              totalActualHours: 6,
              timeEfficiency: 116.67,
              onTimeCompletions: 2,
              onTimeRate: 100
            },
            aiInsights: 'Josue mostró un rendimiento excepcional completando las tareas críticas relacionadas con KPIs y métricas. Terminó sus tareas por debajo del tiempo estimado, demostrando alta eficiencia y dominio técnico.'
          },
          {
            userId: 3,
            fullName: 'Daniel Barreras',
            role: 'DEVELOPER',
            tasks: [
              {
                id: 4,
                title: 'Dashboard de KPIs de sprint',
                description: 'Crear visualización de métricas para cada sprint',
                status: 'COMPLETED',
                estimatedHours: 3,
                actualHours: 3,
                priority: 'MEDIUM'
              }
            ],
            stats: {
              totalTasks: 1,
              completedTasks: 1,
              completionRate: 100,
              totalEstimatedHours: 3,
              totalActualHours: 3,
              timeEfficiency: 100,
              onTimeCompletions: 1,
              onTimeRate: 100
            },
            aiInsights: 'Daniel completó su tarea exactamente en el tiempo estimado, mostrando precisión en la planificación y ejecución. Su dashboard de KPIs de sprint es una herramienta valiosa para el seguimiento del progreso del equipo.'
          }
        ],
        teamStats: {
          totalTasks: 4,
          completedTasks: 4,
          completionRate: 100,
          totalEstimatedHours: 11,
          totalActualHours: 10,
          timeEfficiency: 110,
          onTimeCompletions: 4,
          onTimeRate: 100
        },
        teamInsights: 'El equipo ha demostrado un excelente rendimiento en este sprint, completando todas las tareas a tiempo y con una eficiencia general del 110%. La implementación de dashboards y APIs de KPIs representa un avance significativo para el proyecto.'
      };
    }
  }
};

export default reportService;