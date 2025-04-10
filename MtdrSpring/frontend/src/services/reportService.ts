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

const reportService = {
  generateReport: async (
    request: ReportRequest
  ): Promise<Record<string, string>> => {
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
};

export default reportService;
