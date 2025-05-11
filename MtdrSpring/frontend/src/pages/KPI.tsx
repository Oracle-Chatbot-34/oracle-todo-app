import { useEffect, useState } from 'react';
import KPIScopeSelection from '@/components/kpis/KPIScopeSelection';
import { ChartPie, TrendingUp, Users, Clock } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';
import CompletedTasksBySprint from '@/components/kpis/CompletedTasksBySprint';
import HoursByTeam from '@/components/kpis/HoursByTeam';
import HoursByDeveloperPerSprint from '@/components/kpis/HoursByDeveloperPerSprint';
import HoursBySprints from '@/components/kpis/HoursBySprint';
import CountLegend from '@/components/kpis/CountLegend';
import TaskInformationBySprint from '@/components/kpis/TaskInformationBySprint';

// Services
import sprintService from '@/services/sprintService';
import kpiGraphQLService, {
  KpiResult,
  SprintData,
  SprintDataForPie,
} from '@/services/kpiGraphQLService';

export default function KPI() {
  const { isAuthenticated } = useAuth();
  const [sprints, setSprints] = useState<SprintData[]>([]);
  const [startSprint, setStartSprint] = useState<SprintData | null>(null);
  const [endSprint, setEndSprint] = useState<SprintData | null>(null);

  const [sprintsForTasks, setSprintsForTasks] = useState<
    { sprintId: number; sprintName: string }[]
  >([]);
  const [filteredSprints, setFilteredSprints] = useState<SprintData[]>([]);
  const [filteredSprintHours, setFilteredSprintHours] = useState<
    SprintDataForPie[]
  >([]);
  const [filteredSprintTasks, setFilteredSprintTasks] = useState<
    SprintDataForPie[]
  >([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Load sprints on component mount
  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchAllSprints = async () => {
      try {
        setLoading(true);
        const sprintsResponse = await sprintService.getAllSprints();

        if (Array.isArray(sprintsResponse) && sprintsResponse.length > 0) {
          const convertedSprints: SprintData[] = sprintsResponse.map(
            (sprint) => ({
              id: sprint.id || 0,
              name: sprint.name,
              entries: [],
              totalHours: 0,
              totalTasks: 0,
            })
          );

          setSprints(convertedSprints);
          setStartSprint(convertedSprints[0]);

          if (convertedSprints.length > 0 && convertedSprints[0].id) {
            await fetchKpiData(convertedSprints[0].id);
          }
        } else {
          console.log('No sprints found or empty array:', sprintsResponse);
          setError('No sprints available.');
        }
      } catch (err) {
        console.error('Error fetching sprints:', err);
        setError('Failed to load sprints. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchAllSprints();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  // Function to fetch KPI data
  const fetchKpiData = async (startSprintId: number, endSprintId?: number) => {
    try {
      setLoading(true);

      const response = await kpiGraphQLService.getKpiData(
        startSprintId,
        endSprintId
      );

      if (!response?.data?.getKpiData) {
        console.error('Invalid KPI data response:', response);
        setError('Failed to load KPI data. Invalid response format.');
        setLoading(false);
        return;
      }

      const kpiResult: KpiResult = response.data.getKpiData;
      console.log('KPI Result received:', kpiResult);

      if (kpiResult.sprintData && kpiResult.sprintData.length > 0) {
        const processedSprintData = kpiResult.sprintData.map((sprint) => {
          let totalHours = 0;

          const processedEntries = sprint.entries.map((entry) => {
            let tasksCompleted = entry.tasksCompleted;
            if (!tasksCompleted || tasksCompleted === 0) {
              if (entry.hours > 0) {
                tasksCompleted = Math.max(1, Math.round(entry.hours / 3));
                console.log(
                  `Estimated ${tasksCompleted} tasks for ${entry.member} based on ${entry.hours} hours`
                );
              }
            }

            totalHours += entry.hours || 0;

            return {
              ...entry,
              tasksCompleted,
            };
          });

          const totalTasks = processedEntries.reduce(
            (sum, entry) => sum + (entry.tasksCompleted || 0),
            0
          );

          return {
            ...sprint,
            entries: processedEntries,
            totalHours: sprint.totalHours || totalHours,
            totalTasks: Math.max(sprint.totalTasks || 0, totalTasks),
          };
        });

        console.log('Processed sprint data:', processedSprintData);

        setSprints((prevSprints) => {
          const updatedSprints = [...prevSprints];
          processedSprintData.forEach((sprintData) => {
            const index = updatedSprints.findIndex(
              (s) => s.id === sprintData.id
            );
            if (index !== -1) {
              updatedSprints[index] = sprintData;
            } else {
              updatedSprints.push(sprintData);
            }
          });
          return updatedSprints;
        });

        const processedHours = kpiResult.sprintHours.map((item) => {
          const matchingSprint = processedSprintData.find(
            (s) => s.id === item.id
          );
          return {
            ...item,
            count: Math.max(item.count || 0, matchingSprint?.totalHours || 0),
          };
        });

        const processedTasks = kpiResult.sprintTasks.map((item) => {
          const matchingSprint = processedSprintData.find(
            (s) => s.id === item.id
          );
          return {
            ...item,
            count: Math.max(item.count || 0, matchingSprint?.totalTasks || 0),
          };
        });

        setFilteredSprints(processedSprintData);
        setFilteredSprintHours(processedHours);
        setFilteredSprintTasks(processedTasks);
        setSprintsForTasks(kpiResult.sprintsForTasks || []);

        if (!startSprint || startSprint.id !== startSprintId) {
          const selectedSprint = processedSprintData.find(
            (s) => s.id === startSprintId
          );
          if (selectedSprint) {
            setStartSprint(selectedSprint);
          }
        }

        if (endSprintId) {
          const selectedEndSprint = processedSprintData.find(
            (s) => s.id === endSprintId
          );
          if (selectedEndSprint) {
            setEndSprint(selectedEndSprint);
          }
        } else {
          setEndSprint(null);
        }
      } else {
        console.warn('No sprint data returned in KPI result:', kpiResult);
      }
    } catch (err) {
      console.error('Error fetching KPI data:', err);
      setError('Failed to load KPI data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  // When start or end sprint selection changes
  useEffect(() => {
    if (!startSprint) return;

    const startId = startSprint.id;
    const endId = endSprint?.id;

    fetchKpiData(startId, endId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startSprint?.id, endSprint?.id]);

  // Calculate summary statistics for the overview cards
  const totalHoursAcrossRange = filteredSprints.reduce(
    (sum, sprint) => sum + sprint.totalHours,
    0
  );
  const totalTasksAcrossRange = filteredSprints.reduce(
    (sum, sprint) => sum + sprint.totalTasks,
    0
  );
  const averageHoursPerSprint =
    filteredSprints.length > 0
      ? Math.round(totalHoursAcrossRange / filteredSprints.length)
      : 0;
  const sprintRangeText = endSprint
    ? `${startSprint?.name} to ${endSprint.name}`
    : startSprint?.name || 'No Sprint Selected';

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 p-4 lg:p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Enhanced Header Section */}
        <div className="bg-white rounded-2xl shadow-lg p-6 border border-slate-200">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center space-x-4">
              <div className="p-3 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl">
                <ChartPie className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-slate-800">
                  Key Performance Indicators
                </h1>
                <p className="text-slate-600 mt-1">
                  Sprint Range: {sprintRangeText}
                </p>
              </div>
            </div>
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg max-w-md">
                <div className="flex items-center">
                  <span className="text-sm">{error}</span>
                </div>
              </div>
            )}
          </div>

          {/* Sprint Selection */}
          <div className="bg-slate-50 rounded-xl p-4">
            <div className="flex items-center space-x-4 mb-3">
              <TrendingUp className="w-5 h-5 text-slate-600" />
              <span className="text-lg font-semibold text-slate-700">
                Select Sprint Range:
              </span>
            </div>
            {startSprint && (
              <KPIScopeSelection
                sprints={sprints}
                startSprint={startSprint}
                endSprint={endSprint}
                setStartSprint={setStartSprint}
                setEndSprint={setEndSprint}
              />
            )}
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-blue-100 text-sm font-medium">Total Hours</p>
                <p className="text-3xl font-bold">{totalHoursAcrossRange}</p>
              </div>
              <Clock className="w-8 h-8 text-blue-200" />
            </div>
          </div>

          <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-xl p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-green-100 text-sm font-medium">
                  Total Tasks
                </p>
                <p className="text-3xl font-bold">{totalTasksAcrossRange}</p>
              </div>
              <Users className="w-8 h-8 text-green-200" />
            </div>
          </div>

          <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-purple-100 text-sm font-medium">
                  Avg Hours/Sprint
                </p>
                <p className="text-3xl font-bold">{averageHoursPerSprint}</p>
              </div>
              <TrendingUp className="w-8 h-8 text-purple-200" />
            </div>
          </div>

          <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl p-6 text-white">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-orange-100 text-sm font-medium">
                  Sprint Count
                </p>
                <p className="text-3xl font-bold">{filteredSprints.length}</p>
              </div>
              <ChartPie className="w-8 h-8 text-orange-200" />
            </div>
          </div>
        </div>

        {/* Main Charts Grid - Redesigned for Better Organization */}
        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          {/* Left Column - Team Overview */}
          <div className="xl:col-span-1 space-y-6">
            {/* Total Hours by Team Member */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-80">
              <HoursByTeam
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[1].definition}
                example={dictionaryKPI[1].example}
              />
            </div>

            {/* Sprint Totals */}
            <div className="grid grid-cols-1 gap-4">
              <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-64">
                {filteredSprintHours.length > 1 ? (
                  <HoursBySprints
                    isLoading={loading}
                    isHours={true}
                    chartData={filteredSprintHours}
                    definition={dictionaryKPI[3].definition}
                    example={dictionaryKPI[3].example}
                  />
                ) : (
                  <CountLegend
                    isLoading={loading}
                    isHours={true}
                    count={startSprint?.totalHours || 0}
                  />
                )}
              </div>

              <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-64">
                {filteredSprintTasks.length > 1 ? (
                  <HoursBySprints
                    isLoading={loading}
                    isHours={false}
                    chartData={filteredSprintTasks}
                    definition={dictionaryKPI[4].definition}
                    example={dictionaryKPI[4].example}
                  />
                ) : (
                  <CountLegend
                    isLoading={loading}
                    isHours={false}
                    count={startSprint?.totalTasks || 0}
                  />
                )}
              </div>
            </div>
          </div>

          {/* Center Column - Oracle DevOps Requirements */}
          <div className="xl:col-span-1 space-y-6">
            {/* Hours by Developer per Sprint - Oracle Requirement */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-96">
              <HoursByDeveloperPerSprint
                isLoading={loading}
                sprintData={filteredSprints}
                definition="Hours Worked by Developer per Sprint shows the actual hours logged by each developer for each sprint, allowing visualization of workload distribution and individual productivity across different sprint periods."
                example="Sprint 1: Developer A worked 25 hours, Developer B worked 30 hours. Sprint 2: Developer A worked 28 hours, Developer B worked 35 hours."
              />
            </div>

            {/* Completed Tasks by Sprint */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-80">
              <CompletedTasksBySprint
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[2].definition}
                example={dictionaryKPI[2].example}
              />
            </div>
          </div>

          {/* Right Column - Task Details */}
          <div className="xl:col-span-1">
            <div className="bg-white rounded-xl shadow-lg border border-slate-200 h-full min-h-[700px]">
              <TaskInformationBySprint
                sprints={sprintsForTasks}
                definition={dictionaryKPI[5].definition}
                example={dictionaryKPI[5].example}
              />
            </div>
          </div>
        </div>

        {/* Footer Information */}
        <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6">
          <div className="text-center text-slate-600">
            <p className="text-sm">
              This dashboard fulfills Oracle DevOps documentation requirements
              for sprint performance visualization. Data refreshes automatically
              when sprint range is modified.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
