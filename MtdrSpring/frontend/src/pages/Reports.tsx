import { useState, useEffect } from 'react';
import { ChevronDown, Download, Sparkles } from 'lucide-react';
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
import sprintService from '@/services/sprintService';
import kpiGraphQLService from '@/services/kpiGraphQLService';
import { useAuth } from '@/hooks/useAuth';
import { jsPDF } from 'jspdf';
import 'jspdf-autotable';

declare module 'jspdf' {
  interface jsPDF {
    autoTable: (options: {
      startY?: number;
      head?: string[][];
      body: string[][];
      margin?: { top?: number; right?: number; bottom?: number; left?: number };
      theme?: string;
      styles?: Record<string, unknown>;
      headStyles?: Record<string, unknown>;
      bodyStyles?: Record<string, unknown>;
      alternateRowStyles?: Record<string, unknown>;
      columnStyles?: Record<string, unknown>;
    }) => void;
    lastAutoTable: {
      finalY: number;
    };
  }
}

type Member = {
  id: number;
  name: string;
};

type SprintData = {
  id: number;
  name: string;
  members: Member[];
};

type KpiData = {
  taskCompletionRate: number;
  onTimeCompletionRate: number;
  overdueTasksRate: number;
  workedHours: number;
  plannedHours: number;
  hoursUtilizationPercent: number;
};

type TaskInfo = {
  id: string | number;
  title: string;
  status: string;
  dueDate?: string;
  assigneeName?: string;
};

type SprintTaskInfo = {
  tasks: TaskInfo[];
};

type ReportCharts = {
  taskInformation: SprintTaskInfo[];
};

type ReportData = {
  isIndividual: boolean;
  reportType: string;
  generatedAt: string;
  kpiData: KpiData;
  charts: ReportCharts;
  insights: string;
  user: Member | null;
  teamId: number | null;
  dateRange: {
    startSprint: string;
    endSprint: string;
  };
};

export default function Reports() {

  const { isAuthenticated } = useAuth();

  const [sprints, setSprints] = useState<SprintData[]>([]);

  const [startSprint, setStartSprint] = useState<SprintData | null>(null);
  const [endSprint, setEndSprint] = useState<SprintData | null>(null);

  const [users, setUsers] = useState<Member[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);

  const [selectedTaskOptions, setselectedTaskOptions] = useState<string[]>([]);
  const [selectAllTasksType, setselectAllTasksType] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [reportData, setReportData] = useState<ReportData | null>(null);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);

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

    const fetchAllSprints = async () => {
      try {
        setLoading(true);
        const sprintsResponse = await sprintService.getAllSprints();

        if (!sprintsResponse || sprintsResponse.length === 0) {
          setError('No sprints available');
          setLoading(false);
          return;
        }

        // Format sprints
        const formattedSprints: SprintData[] = sprintsResponse.map(
          (sprint) => ({
            id: sprint.id!,
            name: sprint.name,
            members: [],
          })
        );

        setSprints(formattedSprints);
        setStartSprint(formattedSprints[0]);
      } catch (err) {
        console.error('Error fetching sprints:', err);
        setError('Failed to load sprints');
      } finally {
        setLoading(false);
      }
    };

    fetchAllSprints();
  }, [isAuthenticated]);

  const generatePdf = () => {
    if (!reportData) return;

    const doc = new jsPDF();

    // Add title
    doc.setFontSize(18);
    doc.text('Performance Report', 20, 20);

    // Add metadata
    doc.setFontSize(12);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 20, 30);
    doc.text(
      `Report Type: ${reportData.isIndividual ? 'Individual' : 'Team'}`,
      20,
      40
    );
    doc.text(
      `Sprint Range: ${startSprint?.name} ${
        endSprint ? `to ${endSprint.name}` : ''
      }`,
      20,
      50
    );

    // Add KPI summary
    doc.setFontSize(14);
    doc.text('KPI Summary', 20, 65);

    // Add KPI table
    const kpiData = [
      [
        'Task Completion Rate',
        `${reportData.kpiData.taskCompletionRate.toFixed(2)}%`,
      ],
      [
        'On-Time Completion',
        `${reportData.kpiData.onTimeCompletionRate.toFixed(2)}%`,
      ],
      ['Overdue Tasks', `${reportData.kpiData.overdueTasksRate.toFixed(2)}%`],
      ['Worked Hours', `${reportData.kpiData.workedHours.toFixed(1)} hrs`],
      ['Planned Hours', `${reportData.kpiData.plannedHours.toFixed(1)} hrs`],
      [
        'Hours Utilization',
        `${reportData.kpiData.hoursUtilizationPercent.toFixed(2)}%`,
      ],
    ];

    doc.autoTable({
      startY: 70,
      head: [['Metric', 'Value']],
      body: kpiData,
    });

    // Add AI insights
    doc.setFontSize(14);
    doc.text('AI Insights', 20, doc.lastAutoTable.finalY + 15);

    // Add insights content with text wrapping
    doc.setFontSize(10);
    const insightsText = reportData.insights;
    const splitInsights = doc.splitTextToSize(insightsText, 170);
    doc.text(splitInsights, 20, doc.lastAutoTable.finalY + 25);

    // Save PDF
    const pdfBlob = doc.output('blob');
    const pdfUrl = URL.createObjectURL(pdfBlob);
    setPdfUrl(pdfUrl);

    return pdfUrl;
  };

  const handleGenerateReport = async () => {
    try {
      setLoading(true);
      setError('');
      setPdfUrl(null);

      if (!startSprint) {
        setError('Please select a start sprint');
        setLoading(false);
        return;
      }

      if (!selectedUserId && !selectedTeamId) {
        setError('Please select either a user or a team');
        setLoading(false);
        return;
      }

      // Call GraphQL service
      const kpiResult = await kpiGraphQLService.getKpiData(
        startSprint.id,
        endSprint?.id
      );

      // Format report data
      const formattedResult: ReportData = {
        isIndividual: selectedUserId !== null,
        reportType: 'Performance',
        generatedAt: new Date().toISOString(),
        kpiData: kpiResult.data.getKpiData.data,
        charts: kpiResult.data.getKpiData.charts,
        insights: "AI-generated insights here",
        user: selectedUserId
          ? users.find((u) => u.id === selectedUserId) || null
          : null,
        teamId: selectedTeamId,
        dateRange: {
          startSprint: startSprint.name,
          endSprint: endSprint?.name || startSprint.name,
        },
      };

      setReportData(formattedResult);

      // Generate PDF
      setTimeout(() => {
        generatePdf();
      }, 500);
    } catch (err) {
      console.error('Error generating report:', err);
      setError('Failed to generate report. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleUserSelect = (userId: number) => {
    setSelectedUserId(userId);
    setSelectedTeamId(null); // Clear team selection when user is selected
  };

  const handleTeamSelect = (teamId: number) => {
    setSelectedTeamId(teamId);
    setSelectedUserId(null); // Clear user selection when team is selected
  };

  return (
    <div className="bg-background w-full p-6 lg:px-10 py-10 flex items-start justify-center">
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
          <div className="flex flex-col items-center justify-around text-2xl bg-whitiish2 w-full max-w-[70vh] lg:min-h-[50rem] space-y-3 h-full rounded-4xl shadow-xl p-10">
            <ReportScopeSelection
              sprints={sprints}
              startSprint={startSprint!}
              endSprint={endSprint}
              setStartSprint={setStartSprint}
              setEndSprint={setEndSprint}
            />

            <div className="w-full">
              <Popover>
                <PopoverTrigger className="w-full font-semibold p-4 border-2 border-gray-300 rounded-lg text-left flex justify-between items-center">
                  Report Type Selection
                  <ChevronDown className="w-8 h-8" />
                </PopoverTrigger>
                <PopoverContent className="w-full">
                  <div className="p-4 space-y-6">
                    <div>
                      <h3 className="font-semibold mb-3 text-2xl">
                        Individual Report
                      </h3>
                      <select
                        className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] bg-white text-[20px]"
                        value={selectedUserId || ''}
                        onChange={(e) =>
                          handleUserSelect(Number(e.target.value))
                        }
                      >
                        <option value="">Select a user</option>
                        {users.map((user) => (
                          <option key={user.id} value={user.id}>
                            {user.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="text-center font-semibold">- OR -</div>

                    <div>
                      <h3 className="font-semibold mb-3 text-2xl">
                        Team Report
                      </h3>
                      <select
                        className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] bg-white text-[20px]"
                        value={selectedTeamId || ''}
                        onChange={(e) =>
                          handleTeamSelect(Number(e.target.value))
                        }
                      >
                        <option value="">Select a team</option>
                        <option value="1">Alpha Team</option>
                        <option value="2">Beta Team</option>
                      </select>
                    </div>
                  </div>
                </PopoverContent>
              </Popover>
            </div>

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
              disabled={loading}
            >
              {loading ? 'Generating...' : 'Generate Report'}
            </Button>
          </div>
        </div>

        {/* Report Preview */}
        <div className="w-full h-full">
          {reportData ? (
            <div className="flex flex-col w-full h-full rounded-xl bg-card justify-start items-center p-6 outline gap-4 overflow-y-auto">
              <div className="flex justify-between w-full">
                <h2 className="text-2xl font-bold">
                  {reportData.reportType} Report
                </h2>

                {pdfUrl && (
                  <a
                    href={pdfUrl}
                    download="performance_report.pdf"
                    className="flex items-center gap-2 bg-greenie text-white px-4 py-2 rounded-lg"
                  >
                    <Download size={18} />
                    Download PDF
                  </a>
                )}
              </div>

              <p>
                Generated at:{' '}
                {new Date(reportData.generatedAt).toLocaleString()}
              </p>

              <div className="w-full p-4 bg-white rounded-lg shadow-md">
                <h3 className="text-xl font-semibold mb-2">Report Summary</h3>
                <p>
                  Date Range: {reportData.dateRange.startSprint}
                  {reportData.dateRange.endSprint !==
                    reportData.dateRange.startSprint &&
                    ` to ${reportData.dateRange.endSprint}`}
                </p>
                <p>
                  Report Type: {reportData.isIndividual ? 'Individual' : 'Team'}
                </p>

                {reportData.isIndividual && reportData.user && (
                  <div className="mt-4">
                    <p>User: {reportData.user.name}</p>
                  </div>
                )}

                {!reportData.isIndividual && reportData.teamId && (
                  <div className="mt-4">
                    <p>Team ID: {reportData.teamId}</p>
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
                    <p>
                      Planned Hours:{' '}
                      {reportData.kpiData.plannedHours.toFixed(1)}
                    </p>
                    <p>
                      Worked Hours: {reportData.kpiData.workedHours.toFixed(1)}
                    </p>
                    <p>
                      Hours Utilization:{' '}
                      {reportData.kpiData.hoursUtilizationPercent.toFixed(2)}%
                    </p>
                  </div>
                </div>
              </div>

              <div className="w-full mt-4 p-4 bg-white rounded-lg shadow-md">
                <h3 className="text-xl font-semibold mb-2">AI Insights</h3>
                <div className="prose max-w-none">
                  {reportData.insights
                    .split('\n\n')
                    .map((paragraph: string, idx: number) => (
                      <p key={idx} className="mb-2">
                        {paragraph}
                      </p>
                    ))}
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
                      {reportData.charts.taskInformation.flatMap(
                        (sprintInfo: SprintTaskInfo) =>
                          sprintInfo.tasks.map((task) => (
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
                                {task.assigneeName || 'Unassigned'}
                              </td>
                            </tr>
                          ))
                      )}
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
