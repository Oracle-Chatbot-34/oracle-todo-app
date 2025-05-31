import { useEffect, useState } from 'react';
import KPIScopeSelection from '@/components/kpis/KPIScopeSelection';
import { BarChart3, TrendingUp } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';
import CompletedTasksBySprint from '@/components/kpis/CompletedTasksBySprint';
import HoursByTeam from '@/components/kpis/HoursByTeam';
import HoursByDeveloperPerSprint from '@/components/kpis/HoursByDeveloperPerSprint';
import TotalHoursBySprint from '@/components/kpis/TotalHoursBySprint';
import TaskInformationBySprint from '@/components/kpis/TaskInformationBySprint';

// Services
import sprintService from '@/services/sprintService';
import kpiGraphQLService, {
  KpiResult,
  SprintData,
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

        setFilteredSprints(processedSprintData);
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

  const sprintRangeText = endSprint
    ? `${startSprint?.name} to ${endSprint.name}`
    : startSprint?.name || 'No Sprint Selected';

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 overflow-y-auto">
      {/* Main container with responsive padding that adapts to screen size */}
      <div className="w-full px-4 py-6 sm:px-6 lg:px-8 xl:px-12 2xl:px-16">
        {/* Responsive content wrapper that provides max-width constraints */}
        <div className="max-w-8xl mx-auto space-y-6 pb-8">
          {/* Header Section - Responsive layout that stacks on mobile */}
          <div className="bg-white rounded-2xl shadow-lg p-4 sm:p-6 border border-slate-200">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 mb-6">
              <div className="flex items-center space-x-4">
                <div className="p-3 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex-shrink-0">
                  <BarChart3 className="w-6 h-6 sm:w-8 sm:h-8 text-white" />
                </div>
                <div className="min-w-0 flex-1">
                  <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-slate-800 truncate">
                    Key Performance Indicators
                  </h1>
                  <p className="text-sm sm:text-base text-slate-600 mt-1 truncate">
                    Sprint Range: {sprintRangeText}
                  </p>
                </div>
              </div>

              {/* Error display that adapts to available space */}
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg w-full lg:max-w-md">
                  <div className="flex items-center">
                    <span className="text-sm">{error}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Sprint Selection - Responsive design that works on all screen sizes */}
            <div className="bg-slate-50 rounded-xl p-4">
              <div className="flex items-center space-x-4 mb-3">
                <TrendingUp className="w-5 h-5 text-slate-600 flex-shrink-0" />
                <span className="text-base sm:text-lg font-semibold text-slate-700">
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

          {/* Main Charts Grid - 2x2 Grid Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
            {/* Top Left: Hours by Team Member */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200">
              <HoursByTeam
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[1].definition}
                example={dictionaryKPI[1].example}
              />
            </div>

            {/* Top Right: Total Hours by Sprint */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200">
              <TotalHoursBySprint
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[7].definition}
                example={dictionaryKPI[7].example}
              />
            </div>

            {/* Bottom Left: Hours by Developer per Sprint */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200">
              <HoursByDeveloperPerSprint
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[6].definition}
                example={dictionaryKPI[6].example}
              />
            </div>

            {/* Bottom Right: Completed Tasks by Sprint */}
            <div className="bg-white rounded-xl shadow-lg border border-slate-200">
              <CompletedTasksBySprint
                isLoading={loading}
                sprintData={filteredSprints}
                definition={dictionaryKPI[2].definition}
                example={dictionaryKPI[2].example}
              />
            </div>
          </div>

          {/* Task Information Section - Full width below the 2x2 grid */}
          <div className="bg-white rounded-xl shadow-lg border border-slate-200 max-h-[60vh] overflow-y-auto">
            <TaskInformationBySprint
              sprints={sprintsForTasks}
              definition={dictionaryKPI[5].definition}
              example={dictionaryKPI[5].example}
            />
          </div>

          {/* Footer Information - Responsive padding and text sizing */}
          <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-4 sm:p-6">
            <div className="text-center text-slate-600">
              <p className="text-xs sm:text-sm">
                This dashboard fulfills Oracle DevOps documentation requirements
                for sprint performance visualization. Data refreshes
                automatically when sprint range is modified.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
