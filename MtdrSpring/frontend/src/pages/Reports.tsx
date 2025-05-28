import { useState, useEffect } from 'react';
import { FileText, RefreshCcw, ChevronDown, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import MemberSelection from '@/components/MemberSelection';
import reportService, {
  SprintTasksReport,
  DeveloperTaskGroup,
} from '@/services/reportService';
import userService from '@/services/userService';
import LoadingSpinner from '@/components/LoadingSpinner';

export type Member = {
  id: number;
  name: string;
};

export default function Reports() {
  const [isIndividual, setIsIndividual] = useState(false);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [, setMembers] = useState<Member[]>([]);

  // For the sprint report
  const [sprintReport, setSprintReport] = useState<SprintTasksReport | null>(
    null
  );
  const [expandedDevelopers, setExpandedDevelopers] = useState<number[]>([]);

  useEffect(() => {
    const loadUsers = async () => {
      try {
        const users = await userService.getAllUsers();
        setMembers(
          users.map((user) => ({
            id: user.id || 0,
            name: user.fullName,
          }))
        );
      } catch (err) {
        console.error('Error loading users:', err);
        setError('Failed to load users');
      }
    };

    loadUsers();
    fetchSprintReport();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchSprintReport = async () => {
    try {
      setLoading(true);
      setError('');

      // If selected member, pass userId
      const params: { userId?: number } = {};
      if (isIndividual && selectedMember) {
        params.userId = selectedMember.id;
      }

      const reportData = await reportService.getLastSprintTasksReport(params);
      setSprintReport(reportData);
    } catch (err) {
      console.error('Error loading sprint report:', err);
      setError('Failed to load sprint reports');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = () => {
    fetchSprintReport();
  };

  const toggleDeveloperExpand = (userId: number) => {
    setExpandedDevelopers((prev) => {
      if (prev.includes(userId)) {
        return prev.filter((id) => id !== userId);
      } else {
        return [...prev, userId];
      }
    });
  };

  const handleMemberChange = (member: Member | null) => {
    setSelectedMember(member);
    // Fetch report for this member if in individual mode
    if (isIndividual && member) {
      fetchSprintReport();
    }
  };

  const handleModeChange = (individual: boolean) => {
    setIsIndividual(individual);
    // Reset selected member if switching to team mode
    if (!individual) {
      setSelectedMember(null);
    }
    fetchSprintReport();
  };

  return (
    <div className="bg-background w-full h-full overflow-y-auto flex flex-col">
      <div className="p-6 lg:px-10 py-8 flex-grow">
        <div className="bg-whitie w-full h-full rounded-lg shadow-xl p-6 flex flex-col">
          <div className="flex items-center gap-8 mb-4">
            <div className="flex flex-row items-center gap-3">
              <FileText className="w-8 h-8" />
              <p className="text-[24px] font-semibold">Sprint Tasks Report</p>
            </div>
            <div className="flex flex-col sm:flex-row gap-4">
              <button
                className={`px-6 py-3 rounded-xl font-bold ${
                  !isIndividual
                    ? 'bg-greenie text-white'
                    : 'bg-gray-300 text-black hover:bg-gray-400'
                }`}
                onClick={() => handleModeChange(false)}
              >
                Team Report
              </button>
              <button
                className={`px-6 py-3 rounded-xl font-bold ${
                  isIndividual
                    ? 'bg-greenie text-white'
                    : 'bg-gray-300 text-black hover:bg-gray-400'
                }`}
                onClick={() => handleModeChange(true)}
              >
                Individual Report
              </button>
            </div>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          <div className="flex items-center gap-4 mb-4">
            {isIndividual && (
              <div className="w-64">
                <MemberSelection
                  isIndividual={isIndividual}
                  setIsIndividual={setIsIndividual}
                  selectedMemberProp={selectedMember}
                  setSelectedMemberProp={handleMemberChange}
                />
              </div>
            )}

            <Button
              onClick={handleGenerateReport}
              className="bg-greenie text-white px-4 py-2 rounded-md flex items-center gap-2 ml-auto"
              disabled={loading}
            >
              {loading ? (
                <LoadingSpinner />
              ) : (
                <>
                  <RefreshCcw className="w-5 h-5" />
                  Refresh Report
                </>
              )}
            </Button>
          </div>

          <div className="flex-grow overflow-y-auto">
            {loading ? (
              <div className="flex justify-center items-center h-64">
                <LoadingSpinner />
              </div>
            ) : sprintReport ? (
              <div className="bg-white p-6 rounded-lg shadow-md">
                <div className="mb-8">
                  <h2 className="text-2xl font-bold mb-2">
                    {sprintReport.sprintName} Tasks Report
                  </h2>
                  <div className="flex gap-4 text-sm text-gray-600">
                    <p>
                      Start Date:{' '}
                      {new Date(
                        sprintReport.startDate || ''
                      ).toLocaleDateString()}
                    </p>
                    <p>
                      End Date:{' '}
                      {new Date(
                        sprintReport.endDate || ''
                      ).toLocaleDateString()}
                    </p>
                  </div>
                </div>

                <div className="mb-8">
                  <h3 className="text-xl font-semibold mb-4">Team Overview</h3>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="bg-blue-50 p-4 rounded-lg">
                      <p className="text-sm text-blue-800">Total Tasks</p>
                      <p className="text-3xl font-bold">
                        {sprintReport.teamStats.totalTasks}
                      </p>
                    </div>
                    <div className="bg-green-50 p-4 rounded-lg">
                      <p className="text-sm text-green-800">Completion Rate</p>
                      <p className="text-3xl font-bold">
                        {sprintReport.teamStats.completionRate.toFixed(0)}%
                      </p>
                    </div>
                    <div className="bg-purple-50 p-4 rounded-lg">
                      <p className="text-sm text-purple-800">Time Efficiency</p>
                      <p className="text-3xl font-bold">
                        {sprintReport.teamStats.timeEfficiency.toFixed(0)}%
                      </p>
                    </div>
                    <div className="bg-amber-50 p-4 rounded-lg">
                      <p className="text-sm text-amber-800">On-Time Rate</p>
                      <p className="text-3xl font-bold">
                        {sprintReport.teamStats.onTimeRate.toFixed(0)}%
                      </p>
                    </div>
                  </div>

                  <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                    <h4 className="font-semibold mb-2">AI Analysis</h4>
                    <p className="text-blue-800">{sprintReport.teamInsights}</p>
                  </div>
                </div>

                <div>
                  <h3 className="text-xl font-semibold mb-4">
                    Developer Contributions
                  </h3>

                  <div className="space-y-6 pb-6">
                    {isIndividual && selectedMember ? (
                      // Individual report
                      <DeveloperSection
                        developer={
                          sprintReport.developerGroups.find(
                            (d) => d.userId === selectedMember.id
                          ) || sprintReport.developerGroups[0]
                        }
                        isExpanded={true}
                        toggleExpand={() => {}}
                      />
                    ) : (
                      // Team report
                      sprintReport.developerGroups.map((developer) => (
                        <DeveloperSection
                          key={developer.userId}
                          developer={developer}
                          isExpanded={expandedDevelopers.includes(
                            developer.userId
                          )}
                          toggleExpand={() =>
                            toggleDeveloperExpand(developer.userId)
                          }
                        />
                      ))
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex justify-center items-center h-64 bg-gray-100 rounded-lg">
                <p className="text-lg text-gray-500">
                  Select options and generate a report
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

interface DeveloperSectionProps {
  developer: DeveloperTaskGroup;
  isExpanded: boolean;
  toggleExpand: () => void;
}

function DeveloperSection({
  developer,
  isExpanded,
  toggleExpand,
}: DeveloperSectionProps) {
  return (
    <div className="border rounded-lg overflow-hidden">
      <div
        className="flex items-center justify-between p-4 bg-gray-50 cursor-pointer"
        onClick={toggleExpand}
      >
        <div className="flex items-center gap-3">
          {isExpanded ? (
            <ChevronDown className="h-5 w-5 text-gray-500" />
          ) : (
            <ChevronRight className="h-5 w-5 text-gray-500" />
          )}
          <div>
            <h4 className="font-semibold text-lg">{developer.fullName}</h4>
            <p className="text-sm text-gray-600">{developer.role}</p>
          </div>
        </div>

        <div className="flex gap-4 text-sm">
          <div className="text-center">
            <p className="text-gray-500">Tasks</p>
            <p className="font-semibold">{developer.stats.totalTasks}</p>
          </div>
          <div className="text-center">
            <p className="text-gray-500">Hours</p>
            <p className="font-semibold">{developer.stats.totalActualHours}</p>
          </div>
          <div className="text-center">
            <p className="text-gray-500">Efficiency</p>
            <p className="font-semibold">
              {developer.stats.timeEfficiency.toFixed(0)}%
            </p>
          </div>
        </div>
      </div>

      {isExpanded && (
        <div className="p-4">
          <div className="mb-4 p-3 bg-yellow-50 rounded">
            <p className="italic text-yellow-800">{developer.aiInsights}</p>
          </div>

          <h5 className="font-medium mb-3">Completed Tasks</h5>
          <div className="space-y-3">
            {developer.tasks.map((task) => (
              <div key={task.id} className="border p-3 rounded">
                <div className="flex justify-between">
                  <h6 className="font-semibold">{task.title}</h6>
                  <span
                    className={`px-2 py-1 rounded text-xs ${
                      task.status === 'COMPLETED' || task.status === 'DONE'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-blue-100 text-blue-800'
                    }`}
                  >
                    {task.status}
                  </span>
                </div>
                {task.description && (
                  <p className="text-sm text-gray-600 mt-1">
                    {task.description}
                  </p>
                )}
                <div className="flex justify-between mt-2 text-sm">
                  <div>
                    <span className="text-gray-500">Estimated:</span>{' '}
                    {task.estimatedHours} hours
                  </div>
                  <div>
                    <span className="text-gray-500">Actual:</span>{' '}
                    {task.actualHours} hours
                  </div>
                  <div>
                    <span className="text-gray-500">Priority:</span>{' '}
                    {task.priority}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
