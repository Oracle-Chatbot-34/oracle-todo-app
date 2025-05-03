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
import LineComponent from '@/components/kpis/LineComponent';
import LoadingSpinner from '@/components/LoadingSpinner';



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
/*
  Example sprint data structure:
  [
    {
      id: 1,
      name: "Sprint 1",
      entries: [
        { member: "Member 1", hours: 10, tasksCompleted: 2 },
        { member: "Member 2", hours: 20, tasksCompleted: 3 },
        { member: "Member 3", hours: 15, tasksCompleted: 1 },
      ],
    },
    {
      id: 2,
      name: "Sprint 2",
      entries: [
        // Notice how member 4 is here but not in Sprint 1 so it varies from sprint to sprint
        { member: "Member 1", hours: 12, tasksCompleted: 4 },
        { member: "Member 2", hours: 18, tasksCompleted: 2 },
        { member: "Member 4", hours: 22, tasksCompleted: 5 },
      ],
    },
    // Add more sprints as needed
  ]

*/

export default function KPI() {
  const { isAuthenticated } = useAuth();
  const [sprints, setSprints] = useState<SprintData[]>([]);
  const [startSprint, setStartSprint] = useState<SprintData | null>(null);
  const [endSprint, setEndSprint] = useState<SprintData | null>(null);

  const [sprintsForTasks, setSprintsForTasks] = useState<{sprintId: number, sprintName: string}[]>([]);
  const [filteredSprints, setFilteredSprints] = useState<SprintData[]>([]);
  const [filteredSprintHours, setFilteredSprintHours] = useState<SprintDataForPie[]>(
    []
  );
  const [filteredSprintTasks, setFilteredSprintTasks] = useState<SprintDataForPie[]>(
    []
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Load users and teams on component mount
  useEffect(() => {
    if (!isAuthenticated) return;
    // Fetch all sprints
    const fetchAllSprints = async () => {
      // Simulate fetching sprints
      const sprints: SprintData[] = Array.from({ length: 6 }, (_, i) => {
        const memberCount = Math.floor(Math.random() * 3) + 3; // 3 to 5 members
        const entries = Array.from({ length: memberCount }, (_, j) => ({
          member: `Member ${j + 1}`,
          hours: Math.floor(Math.random() * 40) + 10, // 10–50 hours
          tasksCompleted: Math.floor(Math.random() * 8) + 1, // 1–8 tasks
        }));

        // Calculate total hours and tasks for this sprint
        const totalHours = entries.reduce((sum, e) => sum + e.hours, 0);
        const totalTasks = entries.reduce(
          (sum, e) => sum + e.tasksCompleted,
          0
        );

        return {
          id: i + 1,
          name: `Sprint ${i + 1}`,
          entries,
          totalHours, // Add this field if your type allows
          totalTasks, // Add this field if your type allows
        };
      });

      setSprints(sprints);
      setStartSprint(sprints[0]);
    };
    fetchAllSprints();
  }, []);

  useEffect(() => {
    if (!startSprint) return;

    const startId = startSprint.id;
    const endId = endSprint?.id || startId;

    const span = sprints.filter((s) => s.id >= startId && s.id <= endId);
    setFilteredSprints(span);
  }, [startSprint, endSprint, sprints]);

  useEffect(() => {
    if (filteredSprints.length === 0) return;
    // Sprint data for Task Information
    const sprintsForTasks = filteredSprints.map((sprint) => ({
      sprintId: sprint.id,
      sprintName: sprint.name,
    }));

    // Calculate total hours and tasks for each sprint
    const sprintHours = filteredSprints.map((sprint) => ({
      id: sprint.id,
      name: sprint.name,
      count: sprint.totalHours || 0,
    }));
    const sprintTasks = filteredSprints.map((sprint) => ({
      id: sprint.id,
      name: sprint.name,
      count: sprint.totalTasks || 0,
    }));

    setSprintsForTasks(sprintsForTasks);
    setFilteredSprintHours(sprintHours);
    setFilteredSprintTasks(sprintTasks);

  }, [filteredSprints]);

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col p-4 lg:p-6 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-4 w-full h-1/10">
          <ChartPie className="w-8 h-8" />
          <p className="text-3xl font-semibold mr-20">Key Performance Indicators</p>
          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-6/10">
              {error}
            </div>
          )}
        </div>

        <div className="flex flex-row items-center justify-center gap-4 w-full h-1/10 bg-white rounded-xl shadow-lg pl-7">
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

        <div className="flex flex-row w-full h-8/10 gap-4">
          <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
            <HoursByTeam sprintData={filteredSprints} />
            <div className="flex flex-row w-full items-center justify-center gap-4">
              {filteredSprintHours.length > 1 ? (
                <HoursBySprints
                  isHours={true}
                  chartData={filteredSprintHours}
                />
              ) : (
                <CountLegend isHours={true} count={startSprint?.totalHours!} />
              )}
              {filteredSprintTasks.length > 1 ? (
                <HoursBySprints
                  isHours={false}
                  chartData={filteredSprintTasks}
                />
              ) : (
                <CountLegend isHours={false} count={startSprint?.totalTasks!} />
              )}
            </div>
          </div>
          <div className="flex flex-col w-1/3 h-full items-center justify-center gap-4">
            <CompletedTasksBySprint sprintData={filteredSprints} />
          </div>

          <div className="flex flex-row w-1/3 h-9/10 items-center justify-center bg-white rounded-xl shadow-lg">
            <TaskInformationBySprint sprints={sprintsForTasks} />
          </div>
        </div>
      </div>
    </div>
  );
}
