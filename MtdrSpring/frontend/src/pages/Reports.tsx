import { useState, useEffect } from 'react';
import { ChevronDown, Sparkles } from 'lucide-react';
import StatusSelections from '../components/StatusSelections';
import ReportScopeSelection from '@/components/reports/ReportScopeSelection';
import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import PdfDisplayer from '@/components/PdfDisplayer';
import userService from '../services/userService';
import reportService from '../services/reportService';
import { useAuth } from '@/hooks/useAuth';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import ReportUserSelection from '@/components/reports/ReportUserSelection';

type Member = {
  id: number;
  name: string;
};
type SprintData = {
  id: number;
  name: string;
  members: Member[];
};

export default function Reports() {
  const form = useForm({
    resolver: zodResolver(
      z.object({
        startDate: z.date(),
        endDate: z.date(),
      })
    ),
  });

  const { isAuthenticated } = useAuth();

  const [sprints, setSprints] = useState<SprintData[]>([]);

  const [startSprint, setStartSprint] = useState<SprintData | null>(null);
  const [endSprint, setEndSprint] = useState<SprintData | null>(null);

  const [users, setUsers] = useState<Member[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<number | null>(null);

  const [selectedTaskOptions, setselectedTaskOptions] = useState<string[]>([]);
  const [selectAllTasksType, setselectAllTasksType] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  interface ReportData {
    reportType: string;
    generatedAt: string;
    totalTasks: string;
    dateRange?: string | Date;
    endDate?: string | Date;
    user?: { name: string; role: string };
    teamId?: string;
    teamSize?: string;
    kpiData: {
      taskCompletionRate: number;
      onTimeCompletionRate: number;
      overdueTasksRate: number;
      plannedHours: number;
      workedHours: number;
      hoursUtilizationPercent: number;
    };
    tasks: Array<{
      id: string | number;
      title: string;
      status: string;
      dueDate?: string;
      assignee?: { name: string };
    }>;
  }

  const [reportData, setReportData] = useState<ReportData | null>(null);

  // Load users and teams on component mount
  useEffect(() => {
    const fetchData = async () => {
      try {
        const usersResponse = await userService.getAllUsers();
        // Map User objects to Member interface
        const mappedUsers = usersResponse
          .filter((user) => user.id !== undefined)
          .map((user) => ({
            id: user.id as number,
            name: user.fullName,
          }));
        setUsers(mappedUsers);
      } catch (err) {
        console.error('Error fetching users and teams:', err);
        setError('Failed to load users and teams data');
      }
    };

    fetchData();
  }, []);

  useEffect(() => {
    if (!isAuthenticated) return;
    console.log('Sprints:', sprints);
  }, [sprints]);

  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchAllSprints = async (): Promise<SprintData[]> => {
      const sprintsTemp: SprintData[] = Array.from({ length: 6 }, (_, i) => {
        const memberCount = Math.floor(Math.random() * 3) + 3;
        const entries = Array.from({ length: memberCount }, (_, j) => ({
          id: j + 1,
          name: `Member ${j + 1}`,
        }));

        return {
          id: i + 1,
          name: `Sprint ${i + 1}`,
          members: entries,
        };
      });

      return sprintsTemp;
    };

    fetchAllSprints().then((result) => {
      setSprints(result);

      // Only set start/end if they are still null
      if (!startSprint) setStartSprint(result[0]);
      if (!endSprint) setEndSprint(result[result.length - 1]);
    });
  }, [isAuthenticated]);

  const handleGenerateReport = async () => {
    try {
      setLoading(true);
      setError('');

      // Prepare dates
      const startDate = form.getValues()['startDate'];
      const endDate = form.getValues()['endDate'];

      // Create report request
      const reportRequest = {
        teamId: !selectedTeam || undefined,
        statuses:
          selectedTaskOptions.length > 0 ? selectedTaskOptions : undefined,
        startDate,
        endDate,
      };

      // Generate report
      // Call the API to generate the report
      //const response = await reportService.generateReport(reportRequest);

      // Example mocked result for development/testing
      const result = {
        reportType: 'Performance',
        generatedAt: new Date().toISOString(),
        totalTasks: '24',
        dateRange: startDate.toISOString(),
        endDate: endDate.toISOString(),
        user: { name: 'John Doe', role: 'Developer' },
        teamId: selectedTeam ? selectedTeam.toString() : undefined,
        teamSize: '5',
        taskCompletionRate: 78.5,
        onTimeCompletionRate: 82.3,
        overdueTasksRate: 17.7,
        plannedHours: 120,
        workedHours: 105,
        hoursUtilizationPercent: 87.5,
        tasks: [
          {
            id: 1,
            title: 'Implement user authentication',
            status: 'Completed',
            dueDate: '2023-06-15',
            assignee: { name: 'Jane Smith' },
          },
          {
            id: 2,
            title: 'Design landing page',
            status: 'In Progress',
            dueDate: '2023-06-20',
            assignee: { name: 'John Doe' },
          },
          {
            id: 3,
            title: 'Fix navigation bug',
            status: 'Completed',
            dueDate: '2023-06-10',
            assignee: { name: 'Mike Johnson' },
          },
          {
            id: 4,
            title: 'API integration',
            status: 'Blocked',
            dueDate: '2023-06-25',
            assignee: { name: 'Sarah Wilson' },
          },
          {
            id: 5,
            title: 'Performance optimization',
            status: 'To Do',
            dueDate: '2023-06-30',
            assignee: { name: 'Alex Brown' },
          },
        ],
      };

      // Convert API result to match ReportData interface
      const formattedResult: ReportData = {
        reportType: result.reportType || '',
        generatedAt: result.generatedAt || new Date().toISOString(),
        totalTasks: result.totalTasks || '0',
        dateRange: result.dateRange,
        endDate: result.endDate,
        user:
          typeof result.user === 'string'
            ? { name: result.user, role: '' }
            : (result.user as { name: string; role: string }),
        teamId: result.teamId,
        teamSize: result.teamSize,
        kpiData: {
          taskCompletionRate: Number(result.taskCompletionRate || 0),
          onTimeCompletionRate: Number(result.onTimeCompletionRate || 0),
          overdueTasksRate: Number(result.overdueTasksRate || 0),
          plannedHours: Number(result.plannedHours || 0),
          workedHours: Number(result.workedHours || 0),
          hoursUtilizationPercent: Number(result.hoursUtilizationPercent || 0),
        },
        tasks: Array.isArray(result.tasks) ? result.tasks : [],
      };
      setReportData(formattedResult);

      console.log('Report data:', formattedResult);
    } catch (err) {
      console.error('Error generating report:', err);
      setError('Failed to generate report. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center">
      <div className="flex flex-col lg:flex-row p-6 lg:p-10 gap-y-6 bg-whitie w-full h-full rounded-lg shadow-xl">
        <div className="flex flex-col gap-4 justify-center items-start w-full h-fit mx-2">
          {/* Title */}
          <div className="flex flex-row items-center gap-[20px]">
            <Sparkles className="w-8 h-8" />
            <p className="text-[24px] font-semibold">Intelligent Reports</p>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
              {error}
            </div>
          )}

          {/* Form */}
          <div className="flex flex-col items-center justify-around text-2xl bg-whitiish2 w-full max-w-[70vh] lg:min-h-[75vh] space-y-3 h-full rounded-4xl shadow-xl p-10">
            <ReportScopeSelection
              sprints={sprints}
              startSprint={startSprint!}
              endSprint={endSprint}
              setStartSprint={setStartSprint}
              setEndSprint={setEndSprint}
            />

            <Popover>
              <PopoverTrigger className="w-full font-semibold p-4 border-2 border-gray-300 rounded-lg text-left flex justify-between items-center">
                Member Selection
                <ChevronDown className="w-8 h-8" />
              </PopoverTrigger>
              <PopoverContent className="w-full">
                <p className="font-semibold mb-3 text-2xl px-4">Filter by Member</p>
                <ReportUserSelection members={users} />
              </PopoverContent>
            </Popover>

            <StatusSelections
              selectedTaskOptions={selectedTaskOptions}
              setselectedTaskOptions={setselectedTaskOptions}
              selectAllTasksType={selectAllTasksType}
              setselectAllTasksType={setselectAllTasksType}
            />

            <Button
              variant={'default'}
              size={'lg'}
              className="text-lg"
              type="button"
              onClick={handleGenerateReport}
            >
              Generate Report
            </Button>
          </div>
        </div>

        {/* Report Preview */}
        <div className="w-full h-full">
          {reportData ? (
            <div className="flex flex-col w-full h-full rounded-xl bg-card justify-start items-center p-6 outline gap-4 overflow-y-auto">
              <h2 className="text-2xl font-bold">
                {reportData.reportType} Report
              </h2>
              <p>
                Generated at:{' '}
                {new Date(reportData.generatedAt).toLocaleString()}
              </p>

              <div className="w-full p-4 bg-white rounded-lg shadow-md">
                <h3 className="text-xl font-semibold mb-2">Report Summary</h3>
                <p>Total Tasks: {reportData.totalTasks}</p>
                <p>
                  Date Range:{' '}
                  {reportData.dateRange
                    ? `${new Date(
                        reportData.dateRange
                      ).toLocaleDateString()} to ${new Date(
                        reportData.endDate || ''
                      ).toLocaleDateString()}`
                    : 'Not specified'}
                </p>

                {reportData.user && (
                  <div className="mt-4">
                    <p>User: {reportData.user.name}</p>
                    <p>Role: {reportData.user.role}</p>
                  </div>
                )}

                {reportData.teamId && (
                  <div className="mt-4">
                    <p>Team Size: {reportData.teamSize}</p>
                  </div>
                )}
              </div>

              <div className="w-full mt-4 p-4 bg-white rounded-lg shadow-md">
                <h3 className="text-xl font-semibold mb-2">KPI Data</h3>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p>
                      Task Completion Rate:{' '}
                      {reportData.kpiData.taskCompletionRate.toFixed(2)}%
                    </p>
                    <p>
                      On-Time Completion:{' '}
                      {reportData.kpiData.onTimeCompletionRate.toFixed(2)}%
                    </p>
                    <p>
                      Overdue Tasks:{' '}
                      {reportData.kpiData.overdueTasksRate.toFixed(2)}%
                    </p>
                  </div>

                  <div>
                    <p>Planned Hours: {reportData.kpiData.plannedHours}</p>
                    <p>Worked Hours: {reportData.kpiData.workedHours}</p>
                    <p>
                      Hours Utilization:{' '}
                      {reportData.kpiData.hoursUtilizationPercent.toFixed(2)}%
                    </p>
                  </div>
                </div>
              </div>

              <div className="w-full mt-4 p-4 bg-white rounded-lg shadow-md">
                <h3 className="text-xl font-semibold mb-2">Task Details</h3>

                <div className="mt-2">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Title
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Status
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Due Date
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Assignee
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {reportData.tasks.map((task) => (
                        <tr key={task.id}>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                            {task.title}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {task.status}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {task.dueDate
                              ? new Date(task.dueDate).toLocaleDateString()
                              : 'N/A'}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {task.assignee ? task.assignee.name : 'Unassigned'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          ) : (
            <PdfDisplayer href={undefined} />
          )}
        </div>
      </div>
    </div>
  );
}
