import { useEffect, useState } from 'react';
import TaskCompletionRate from '@/components/kpis/TaskCompletionRate';
import TimeCompletionRate from '@/components/kpis/TimeCompletionRate';
import LineComponent from '@/components/kpis/LineComponent';
import RealHours from '@/components/kpis/RealHours';
import KPITitle from '@/components/kpis/KPITtitle';
import AvgHours from '@/components/kpis/AvgHoursEmpl';
import { dictionaryKPI } from '@/components/kpis/KPIDictionary';
import ScopeSelection from '@/components/ScopeSelection';
import { ChartPie } from 'lucide-react';
import LoadingSpinner from '@/components/LoadingSpinner';
import kpiService, { KpiData } from '@/services/kpiService';
import userService from '@/services/userService';
import teamService from '@/services/teamService';
import { useAuth } from '@/hooks/useAuth';

interface Member {
  id: number;
  name: string;
}

interface Team {
  id: number;
  name: string;
}

export default function KPI() {
  const { isAuthenticated } = useAuth();
  const [isIndividual, setIsIndividual] = useState(true);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [kpiData, setKpiData] = useState<KpiData | null>(null);

  // Load users and teams on component mount
  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchData = async () => {
      try {
        const [usersResponse, teamsResponse] = await Promise.all([
          userService.getAllUsers(),
          teamService.getAllTeams(),
        ]);

        const formattedUsers: Member[] = usersResponse
                  .filter((user) => 
                    typeof user.id === 'number' && user.id !== undefined && user.fullName !== undefined)
                  .map((user) => ({
                    id: user.id as number,
                    name: user.fullName,
                  }));

        const formattedTeams: Team[] = teamsResponse
          .filter((team) => typeof team.id === 'number' && team.id !== undefined)
          .map((team) => ({
            id: team.id as number,
            name: team.name,
          }));

        setMembers(formattedUsers);
        setTeams(formattedTeams);

        // Set default selections if available
        if (formattedUsers.length > 0) {
          setSelectedMember(formattedUsers[0]);
        }

        if (formattedTeams.length > 0) {
          setSelectedTeam(formattedTeams[0]);
        }
      } catch (err) {
        console.error('Error fetching users and teams:', err);
        setError('Failed to load users and teams data');
      }
    };

    fetchData();
  }, [isAuthenticated]);

  // Fetch KPI data when selections change
  useEffect(() => {
    if (!isAuthenticated) return;

    const fetchKpiData = async () => {
      // Don't fetch if we don't have valid selections
      if (
        (isIndividual && !selectedMember) ||
        (!isIndividual && !selectedTeam)
      ) {
        return;
      }

      try {
        setLoading(true);
        setError('');

        let data: KpiData;
        if (isIndividual && selectedMember) {
          data = await kpiService.getUserKpis(selectedMember.id);
        } else if (!isIndividual && selectedTeam) {
          data = await kpiService.getTeamKpis(selectedTeam.id);
        } else {
          return; // Should never reach here because of our guard condition
        }

        setKpiData(data);
      } catch (err) {
        console.error('Error fetching KPI data:', err);
        setError('Failed to load KPI data. Please try again.');
        setKpiData(null);
      } finally {
        setLoading(false);
      }
    };

    fetchKpiData();
  }, [isAuthenticated, isIndividual, selectedMember, selectedTeam]);

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center items-center p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-[10px]">
          <ChartPie className="w-8 h-8" />
          <p className="text-[24px] font-semibold">
            Key Performance Indicators
          </p>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
            {error}
          </div>
        )}

        <div className="flex lg:flex-row gap-x-3 w-full h-full p-6">
          {/* Task completion rate */}
          <div className="bg-whitiish2 w-1/3 h-full rounded-2xl shadow-xl p-5 gap-5 flex flex-col">
            <KPITitle
              title="Task completion rate"
              KPIObject={dictionaryKPI[1]}
            />
            <div className="flex flex-col gap-6 w-full p-2">
              {loading ? (
                <div className="h-60 flex items-center justify-center">
                  <LoadingSpinner size={8} />
                </div>
              ) : kpiData ? (
                <TaskCompletionRate
                  data={kpiData.taskCompletionTrend || []}
                  categories={kpiData.trendLabels || []}
                />
              ) : (
                <div className="h-60 flex items-center justify-center text-center">
                  <p>Select a member or team to view KPI data</p>
                </div>
              )}

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
                    <option value="" disabled>
                      Select a member
                    </option>
                    {members.map((member) => (
                      <option key={member.id} value={member.id}>
                        {member.name}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="w-full">
                  <p className="text-[#747276] text-[1.5625rem]">
                    Select a team
                  </p>
                  <select
                    className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white text-[20px]"
                    value={selectedTeam?.id || ''}
                    onChange={(e) => {
                      const teamId = parseInt(e.target.value);
                      const team = teams.find((t) => t.id === teamId) || null;
                      setSelectedTeam(team);
                    }}
                  >
                    <option value="" disabled>
                      Select a team
                    </option>
                    {teams.map((team) => (
                      <option key={team.id} value={team.id}>
                        {team.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>

          <div className="flex flex-col gap-5 w-1/3 h-full">
            {/* Time completion rate */}
            <div className="bg-whitiish2 rounded-2xl shadow-xl p-2 gap-5 flex flex-col h-1/2 justify-center items-center">
              <KPITitle
                title="Time Completion Rate Over Time %"
                KPIObject={dictionaryKPI[3]}
              />
              {loading ? (
                <LoadingSpinner size={8} />
              ) : kpiData ? (
                <TimeCompletionRate
                  data={[
                    kpiData.onTimeCompletionRate || 0,
                    kpiData.overdueTasksRate || 0,
                    kpiData.inProgressRate || 0,
                  ]}
                />
              ) : (
                <div className="h-40 flex items-center justify-center">
                  <p>No data available</p>
                </div>
              )}
            </div>

            {/* Percentages */}
            <div className="bg-whitiish2 rounded-2xl shadow-xl px-6 py-8 gap-5 flex flex-col justify-around items-center h-1/2 ">
              <div className="w-full flex flex-col gap-4">
                <KPITitle
                  title="OCI Resources Utilization"
                  KPIObject={dictionaryKPI[5]}
                />
                {loading ? (
                  <LoadingSpinner />
                ) : kpiData ? (
                  <LineComponent
                    percentage={kpiData.ociResourcesUtilization || 0}
                  />
                ) : (
                  <div className="h-10 flex items-center justify-center">
                    <p>No data available</p>
                  </div>
                )}
              </div>

              <div className="w-full flex flex-col gap-4">
                <KPITitle
                  title="Tasks Completed per Week"
                  KPIObject={dictionaryKPI[6]}
                />
                {loading ? (
                  <LoadingSpinner />
                ) : kpiData ? (
                  <LineComponent
                    percentage={kpiData.tasksCompletedPerWeek || 0}
                  />
                ) : (
                  <div className="h-10 flex items-center justify-center">
                    <p>No data available</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Real worked hours */}
          <div className="flex flex-col w-1/3 h-full gap-5">
            <div className="bg-whitiish2 rounded-2xl shadow-xl p-2 gap-5 flex flex-col h-full justify-center">
              <KPITitle
                title="Real Hours Worked"
                KPIObject={dictionaryKPI[4]}
              />
              {loading ? (
                <LoadingSpinner size={8} />
              ) : kpiData ? (
                <RealHours
                  percentage={kpiData.hoursUtilizationPercent || 0}
                  workedHours={kpiData.workedHours || 0}
                  plannedHours={kpiData.plannedHours || 0}
                />
              ) : (
                <div className="h-40 flex items-center justify-center">
                  <p>No data available</p>
                </div>
              )}
            </div>

            <div className="bg-whitiish2 rounded-2xl shadow-xl h-full p-2 gap-5 flex flex-col justify-center items-center">
              <KPITitle
                title="Average Tasks by Employee"
                KPIObject={dictionaryKPI[2]}
              />
              {loading ? (
                <LoadingSpinner size={8} />
              ) : kpiData ? (
                <AvgHours average={kpiData.averageTasksPerEmployee || 0} />
              ) : (
                <div className="h-40 flex items-center justify-center">
                  <p>No data available</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
