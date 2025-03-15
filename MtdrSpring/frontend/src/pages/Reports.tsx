import { useState, useEffect } from 'react';
import { Sparkles } from 'lucide-react';
import StatusSelections from '../components/StatusSelections';
import ScopeSelection from '../components/ScopeSelection';
import { DateRange } from '@mui/x-date-pickers-pro';
import { Dayjs } from 'dayjs';
import DatePickerRange from '../components/DatePickerRange';
import { Button } from '@/components/ui/button';
import PdfDisplayer from '@/components/PdfDisplayer';
import userService from '../services/userService';
import teamService from '../services/teamService';
import reportService from '../services/reportService';

interface Member {
  id: number;
  name: string;
}

export default function Reports() {
  const [isIndividual, setIsIndividual] = useState(true);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [teams, setTeams] = useState<{ id: number; name: string }[]>([]);
  const [selectedTeam, setSelectedTeam] = useState<number | null>(null);

  const [selectedTaskOptions, setselectedTaskOptions] = useState<string[]>([]);
  const [selectAllTasksType, setselectAllTasksType] = useState(false);

  const [dateRange, setDateRange] = useState<DateRange<Dayjs>>([null, null]);

  const [, setLoading] = useState(false);
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
        const userMembers = usersResponse
          .filter(user => user.id !== undefined)
          .map((user) => ({
            id: user.id as number,
            name: user.fullName,
          }));
        setMembers(userMembers);

        const teamsResponse = await teamService.getAllTeams();
        setTeams(
          teamsResponse
            .filter(team => team.id !== undefined)
            .map((team) => ({
              id: team.id as number,
              name: team.name,
            }))
        );
      } catch (err) {
        console.error('Error fetching users and teams:', err);
        setError('Failed to load users and teams data');
      }
    };

    fetchData();
  }, []);

  const handleGenerateReport = async () => {
    try {
      setLoading(true);
      setError('');

      // Validate required fields
      if (isIndividual && !selectedMember) {
        setError('Please select a team member');
        setLoading(false);
        return;
      }

      if (!isIndividual && !selectedTeam) {
        setError('Please select a team');
        setLoading(false);
        return;
      }

      // Prepare dates
      const startDate = dateRange[0]?.toDate();
      const endDate = dateRange[1]?.toDate();

      // Create report request
      const reportRequest = {
        isIndividual,
        userId: isIndividual ? selectedMember?.id : undefined,
        teamId: !isIndividual ? selectedTeam || undefined : undefined,
        statuses:
          selectedTaskOptions.length > 0 ? selectedTaskOptions : undefined,
        startDate,
        endDate,
      };

      // Generate report
      const result = await reportService.generateReport(reportRequest);
      // Convert API result to match ReportData interface
      const formattedResult: ReportData = {
        reportType: result.reportType || '',
        generatedAt: result.generatedAt || new Date().toISOString(),
        totalTasks: result.totalTasks || '0',
        dateRange: result.dateRange,
        endDate: result.endDate,
        user: typeof result.user === 'string' 
          ? { name: result.user, role: '' } 
          : result.user as { name: string; role: string },
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
          <div className="flex flex-col items-center justify-around bg-whitiish2 w-full max-w-[37.5rem] lg:min-h-[50rem] space-y-3 h-full rounded-4xl shadow-xl p-10">
            <ScopeSelection
              isIndividual={isIndividual}
              setIsInidividual={setIsIndividual}
            />

            {isIndividual ? (
              <div className="w-full">
                <p className="text-[#747276] text-[1.5625rem]">
                  Select a member
                </p>
                <select
                  className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white text-[20px]"
                  value={selectedMember?.id || ''}
                  onChange={(e) => {
                    const memberId = parseInt(e.target.value);
                    const member =
                      members.find((m) => m.id === memberId) || null;
                    setSelectedMember(member);
                  }}
                >
                  <option value="" disabled></option>
                  {members.map((member) => (
                    <option key={member.id} value={member.id}>
                      {member.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="w-full">
                <p className="text-[#747276] text-[1.5625rem]">Select a team</p>
                <select
                  className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white text-[20px]"
                  value={selectedTeam || ''}
                  onChange={(e) => {
                    const teamId = parseInt(e.target.value);
                    setSelectedTeam(teamId);
                  }}
                >
                  <option value="" disabled></option>
                  {teams.map((team) => (
                    <option key={team.id} value={team.id}>
                      {team.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <StatusSelections
              selectedTaskOptions={selectedTaskOptions}
              setselectedTaskOptions={setselectedTaskOptions}
              selectAllTasksType={selectAllTasksType}
              setselectAllTasksType={setselectAllTasksType}
            />

            <DatePickerRange
              dateRangeProp={dateRange}
              setDateRangeProp={setDateRange}
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
                  {reportData.dateRange ? 
                    `${new Date(reportData.dateRange).toLocaleDateString()} to ${new Date(reportData.endDate || '').toLocaleDateString()}` :
                    'Not specified'
                  }
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
