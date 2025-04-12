import { useEffect, useState } from 'react';
import KPIScopeSelection from '@/components/kpis/KPIScopeSelection';
import { ChartPie } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
// KPI dictionary
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';

// Components
import CompletedTasksBySprint from '@/components/kpis/CompletedTasksBySprint';
import HoursByTeam from '@/components/kpis/HoursByTeam';
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
        // Get all sprints from backend
        const sprintsResponse = await sprintService.getAllSprints();

        if (Array.isArray(sprintsResponse) && sprintsResponse.length > 0) {
          // Convert to our SprintData format (just to get the list initially)
          const convertedSprints: SprintData[] = sprintsResponse.map(
            (sprint) => ({
              id: sprint.id || 0,
              name: sprint.name,
              entries: [], // Will be populated by KPI data
              totalHours: 0,
              totalTasks: 0,
            })
          );

          setSprints(convertedSprints);
          setStartSprint(convertedSprints[0]);

          // Once we have the sprints, fetch KPI data for the first sprint
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

      // Check if the response has the expected structure
      if (!response?.data?.getKpiData) {
        console.error('Invalid KPI data response:', response);
        setError('Failed to load KPI data. Invalid response format.');
        setLoading(false);
        return;
      }

      const kpiResult: KpiResult = response.data.getKpiData;

      // Update sprints with the full data from the KPI service
      if (kpiResult.sprintData && kpiResult.sprintData.length > 0) {
        // Process sprint data to ensure each entry has entries, totalHours, and totalTasks
        const processedSprintData = kpiResult.sprintData.map((sprint) => {
          // Calculate total hours and tasks if not already provided
          const totalHours = sprint.entries.reduce(
            (sum, entry) => sum + (entry.hours || 0),
            0
          );
          const totalTasks = sprint.entries.reduce(
            (sum, entry) => sum + (entry.tasksCompleted || 0),
            0
          );

          return {
            ...sprint,
            totalHours: sprint.totalHours || totalHours,
            totalTasks: sprint.totalTasks || totalTasks,
          };
        });

        setSprints((prevSprints) => {
          // Update the existing sprints with the full data
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

        // Process sprint hours and tasks data to ensure no zero values
        const processedHours = (kpiResult.sprintHours || []).filter(
          (item) => item.count > 0
        );
        const processedTasks = (kpiResult.sprintTasks || []).filter(
          (item) => item.count > 0
        );

        // Set filtered data
        setFilteredSprints(processedSprintData);
        setFilteredSprintHours(processedHours);
        setFilteredSprintTasks(processedTasks);
        setSprintsForTasks(kpiResult.sprintsForTasks || []);

        // Update current sprint selection if needed
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

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col p-4 lg:p-6 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-4 w-full h-1/13">
          <ChartPie className="w-8 h-8" />
          <p className="text-3xl font-semibold mr-20">
            Key Performance Indicators
          </p>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-6/10">
              {error}
            </div>
          )}
        </div>
        <div className="flex flex-row items-center justify-center gap-4 w-full h-1/13 bg-white rounded-xl shadow-lg pl-7">
          <div className="text-2xl font-semibold w-1/4">Select a scope:</div>
          <div className="w-3/4">
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

        <div className="flex flex-row w-full h-11/13 gap-4">
          <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
            <HoursByTeam
              isLoading={loading}
              sprintData={filteredSprints}
              definition={dictionaryKPI[1].definition}
              example={dictionaryKPI[1].example}
            />
            <div className="flex flex-row w-full items-center justify-center gap-4">
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
          </div>
          <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
            <CompletedTasksBySprint
              isLoading={loading}
              sprintData={filteredSprints}
              definition={dictionaryKPI[2].definition}
              example={dictionaryKPI[2].example}
            />
            <div className="flex flex-row w-full items-center justify-center gap-4">
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

          <div className="flex flex-row w-1/3 h-full items-center justify-center">
            <TaskInformationBySprint
              sprints={sprintsForTasks}
              definition={dictionaryKPI[5].definition}
              example={dictionaryKPI[5].example}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
