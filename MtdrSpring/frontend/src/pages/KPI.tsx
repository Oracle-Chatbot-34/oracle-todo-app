import { useEffect, useState } from 'react';
import KPIScopeSelection from '@/components/kpis/KPIScopeSelection';
import { ChartPie } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';

import CompletedTasksBySprint from '@/components/kpis/CompletedTasksBySprint';
import HoursByTeam from '@/components/kpis/HoursByTeam';
import HoursBySprints from '@/components/kpis/HoursBySprint';
import CountLegend from '@/components/kpis/CountLegend';
import TaskInformationBySprint from '@/components/kpis/TaskInformationBySprint';
import LoadingSpinner from '@/components/LoadingSpinner';
import kpiGraphQLService from '@/services/kpiGraphQLService';
import sprintService from '@/services/sprintService';
import userService from '@/services/userService';

type MemberEntry = {
  member: string;
  hours: number;
  tasksCompleted: number;
};

type SprintDataForPie = {
  id: number;
  name: string;
  count: number;
};

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
  totalHours?: number;
  totalTasks?: number;
};

type UserInfo = {
  id: number;
  name: string;
};

export default function KPI() {
  const { isAuthenticated, username } = useAuth();
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

  const [currentUser, setCurrentUser] = useState<UserInfo | null>(null);
  const [currentTeam, setCurrentTeam] = useState<number | null>(null);
  const [insights, setInsights] = useState<string>('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Load user information and team details on component mount
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!isAuthenticated || !username) return;
      
      try {
        const users = await userService.getAllUsers();
        const currentUserData = users.find(user => user.username === username);
        
        if (currentUserData) {
          setCurrentUser({
            id: currentUserData.id!,
            name: currentUserData.fullName
          });
          
          if (currentUserData.teamId) {
            setCurrentTeam(currentUserData.teamId);
          }
        }
      } catch (err) {
        console.error('Error fetching user info:', err);
        setError('Failed to load user information');
      }
    };
    
    fetchUserInfo();
  }, [isAuthenticated, username]);

  // Load sprints on component mount
  useEffect(() => {
    if (!isAuthenticated) return;
    
    const fetchSprints = async () => {
      try {
        setLoading(true);
        const sprintsData = await sprintService.getAllSprints();
        
        if (!sprintsData || sprintsData.length === 0) {
          setError('No sprints available');
          setLoading(false);
          return;
        }
        
        // Convert to SprintData format
        const formattedSprints: SprintData[] = sprintsData.map(sprint => ({
          id: sprint.id!,
          name: sprint.name,
          entries: [],
          totalHours: 0,
          totalTasks: 0
        }));
        
        setSprints(formattedSprints);
        setStartSprint(formattedSprints[0]);
      } catch (err) {
        console.error('Error fetching sprints:', err);
        setError('Failed to load sprints');
      } finally {
        setLoading(false);
      }
    };
    
    fetchSprints();
  }, [isAuthenticated]);

  // Fetch KPI data when sprints are selected
  useEffect(() => {
    if (!startSprint) return;
    
    const fetchKpiData = async () => {
      try {
        setLoading(true);
        setError('');
        
        // Determine if we're showing individual or team data
        const userId = currentUser?.id;
        const teamId = currentTeam;

        
        // Call GraphQL service
        const kpiResult = await kpiGraphQLService.getKpiData(
          userId,
          teamId || undefined,
        );
        
        const { charts } = kpiResult.data.getKpiData;
        
        // Process developer hours and tasks data
        const processedSprints: SprintData[] = [];
        
        // Process each sprint in the range
        const sprintsInRange = charts.hoursBySprint.map(sprint => sprint.sprintId);
        
        const sprintNames = new Map<number, string>();
        charts.hoursBySprint.forEach(sprint => {
          sprintNames.set(sprint.sprintId, sprint.sprintName);
        });
        
        // Create sprint data objects
        sprintsInRange.forEach(sprintId => {
          // Create entries for each developer
          const entries: MemberEntry[] = [];
          
          // Find developer data for this sprint
          charts.hoursByDeveloper.forEach(devHours => {
            const devTasks = charts.tasksByDeveloper.find(
              dt => dt.developerId === devHours.developerId
            );
            
            if (devHours && devTasks) {
              // Find sprint index in the developer's data
              const sprintIndex = devHours.sprints.findIndex(
                sprint => sprint === sprintNames.get(sprintId)
              );
              
              if (sprintIndex >= 0) {
                entries.push({
                  member: devHours.developerName,
                  hours: devHours.values[sprintIndex],
                  tasksCompleted: devTasks.values[sprintIndex]
                });
              }
            }
          });
          
          // Calculate totals for this sprint
          const totalHours = entries.reduce((sum, entry) => sum + entry.hours, 0);
          const totalTasks = entries.reduce((sum, entry) => sum + entry.tasksCompleted, 0);
          
          processedSprints.push({
            id: sprintId,
            name: sprintNames.get(sprintId) || `Sprint ${sprintId}`,
            entries,
            totalHours,
            totalTasks
          });
        });
        
        // Set filtered sprints based on date range
        setFilteredSprints(processedSprints);
        
        // Map sprint data for pie charts
        const hoursBySprint: SprintDataForPie[] = charts.hoursBySprint.map(sprint => ({
          id: sprint.sprintId,
          name: sprint.sprintName,
          count: sprint.value
        }));
        
        const tasksBySprint: SprintDataForPie[] = charts.tasksBySprint.map(sprint => ({
          id: sprint.sprintId,
          name: sprint.sprintName,
          count: sprint.value
        }));
        
        setFilteredSprintHours(hoursBySprint);
        setFilteredSprintTasks(tasksBySprint);
        
        // Map sprint data for tasks information
        const tasksInfo = charts.taskInformation.map(info => ({
          sprintId: info.sprintId,
          sprintName: info.sprintName
        }));
        
        setSprintsForTasks(tasksInfo);
        
        // Set insights from AI
        setInsights(insights);
        
      } catch (err) {
        console.error('Error fetching KPI data:', err);
        setError('Failed to load KPI data');
      } finally {
        setLoading(false);
      }
    };
    
    fetchKpiData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startSprint, endSprint, currentUser, currentTeam]);

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
            <KPIScopeSelection
              sprints={sprints}
              startSprint={startSprint!}
              endSprint={endSprint}
              setStartSprint={setStartSprint}
              setEndSprint={setEndSprint}
            />
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center items-center h-full">
            <LoadingSpinner />
          </div>
        ) : (
          <>
            <div className="flex flex-row w-full h-11/13 gap-4">
              <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
                <HoursByTeam
                  sprintData={filteredSprints}
                  definition={dictionaryKPI[1].definition}
                  example={dictionaryKPI[1].example}
                />
                <div className="flex flex-row w-full items-center justify-center gap-4">
                  {filteredSprintHours.length > 1 ? (
                    <HoursBySprints
                      isHours={true}
                      chartData={filteredSprintHours}
                      definition={dictionaryKPI[3].definition}
                      example={dictionaryKPI[3].example}
                    />
                  ) : (
                    <CountLegend 
                      isHours={true} 
                      count={startSprint?.totalHours || 0} 
                    />
                  )}
                </div>
              </div>
              <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
                <CompletedTasksBySprint
                  sprintData={filteredSprints}
                  definition={dictionaryKPI[2].definition}
                  example={dictionaryKPI[2].example}
                />
                <div className="flex flex-row w-full items-center justify-center gap-4">
                  {filteredSprintTasks.length > 1 ? (
                    <HoursBySprints
                      isHours={false}
                      chartData={filteredSprintTasks}
                      definition={dictionaryKPI[4].definition}
                      example={dictionaryKPI[4].example}
                    />
                  ) : (
                    <CountLegend 
                      isHours={false} 
                      count={startSprint?.totalTasks || 0} 
                    />
                  )}
                </div>
              </div>

              <div className="flex flex-col w-1/3 h-full gap-4">
                <TaskInformationBySprint
                  sprints={sprintsForTasks}
                  definition={dictionaryKPI[5].definition}
                  example={dictionaryKPI[5].example}
                />
                
                {/* AI Insights Panel */}
                {insights && (
                  <div className="w-full flex flex-col p-5 bg-white rounded-xl shadow-lg">
                    <h3 className="text-2xl font-semibold mb-4">AI Insights</h3>
                    <div className="prose max-w-none">
                      {insights.split('\n\n').map((paragraph, idx) => (
                        <p key={idx} className="mb-2">{paragraph}</p>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}