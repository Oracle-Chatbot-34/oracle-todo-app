import taskService from '@/services/tasksService';
import userService from '@/services/userService';
import { useState, useEffect } from 'react';
import {
  ResponsiveContainer,
  Bar,
  BarChart,
  XAxis,
  YAxis,
  Cell,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';

type ChartConfig = Record<
  string,
  {
    label: string;
    color: string;
  }
>;

type BarChartData = {
  member: string;
  taskBySprint: number;
};

type Sprint = {
  id: number;
  name: string;
};

type Prop = {
  selectedMemberId: number;
};

type MemberResponse = {
  id: number;
  name: string;
  role: string;
};

type MemberSprintData = {
  memberId: number;
  sprintId: number;
  avgTasks: number;
};

const generateChartConfig = (members: string[]): ChartConfig => {
  return members.reduce((config, member, index) => {
    config[member] = {
      label: member,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`,
    };
    return config;
  }, {} as ChartConfig);
};

export default function AvgHours({ selectedMemberId }: Prop) {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [selectedSprint, setSelectedSprint] = useState<Sprint | null>(null);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});
  const [chartData, setChartData] = useState<BarChartData[]>([]);
  const [userAvgTasks, setUserAvgTasks] = useState(0);
  const [teamMembers, setTeamMembers] = useState<MemberResponse[]>([]);
  const [memberSprintData, setMemberSprintData] = useState<MemberSprintData[]>(
    []
  );
  const [selectedMemberSprintData, setSelectedMemberSprintData] = useState<
    MemberSprintData[]
  >([]);

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const teamId = 1; // Simulated teamId from selectedMemberId
        const sprints = Array.from({ length: 6 }, (_, i) => ({
          id: i + 1,
          name: `Sprint ${i + 1}`,
        }));
        const members: MemberResponse[] = [
          { id: 4, name: 'John Doe', role: 'Developer' },
          { id: 5, name: 'Jane Smith', role: 'Designer' },
          { id: 8, name: 'Alice Johnson', role: 'Manager' },
        ];

        const simulatedMemberSprintData: MemberSprintData[] = [];
        const simulatedSelectedMemberData: MemberSprintData[] = [];

        for (let sprint of sprints) {
          for (let member of members) {
            simulatedMemberSprintData.push({
              memberId: member.id,
              sprintId: sprint.id,
              avgTasks: Math.floor(Math.random() * 10) + 1,
            });
          }

          // Add data for selected member as well
          simulatedSelectedMemberData.push({
            memberId: selectedMemberId,
            sprintId: sprint.id,
            avgTasks: Math.floor(Math.random() * 10) + 1,
          });
        }

        setTeamMembers(members);
        setSprints(sprints);
        setSelectedSprint(sprints[0]);
        setMemberSprintData(simulatedMemberSprintData);
        setSelectedMemberSprintData(simulatedSelectedMemberData);

        const config = generateChartConfig(members.map((m) => m.name));
        setChartConfig(config);
      } catch (err) {
        console.error(err);
      }
    };

    const fetchUserTasks = async () => {
      if (selectedMemberId !== 0) {
        try {
          const taskResponse = await taskService.getUserAverageOfTasks(
            selectedMemberId
          );
          setUserAvgTasks(taskResponse);
        } catch (err) {
          console.error(err);
        }
      }
    };

    fetchInitialData();
    fetchUserTasks();
  }, [selectedMemberId]);

  useEffect(() => {
    if (!selectedSprint) return;

    // Update chart
    const filteredData = teamMembers.map((member) => {
      const entry = memberSprintData.find(
        (d) => d.sprintId === selectedSprint.id && d.memberId === member.id
      );
      return {
        member: member.name,
        taskBySprint: entry?.avgTasks ?? 0,
      };
    });
    setChartData(filteredData);

    // Update selected user avg task
    const selectedUserData = selectedMemberSprintData.find(
      (d) => d.sprintId === selectedSprint.id && d.memberId === selectedMemberId
    );
    setUserAvgTasks(selectedUserData?.avgTasks ?? 0);
  }, [selectedSprint, memberSprintData, selectedMemberSprintData, teamMembers]);

  return (
    <div className="flex flex-col h-full w-full items-center gap-7">
      <div className="w-full h-1/6 flex flex-row items-center justify-center gap-4 text-2xl">
        <p>Select a sprint:</p>
        <select
          value={selectedSprint?.id || ''}
          onChange={(e) =>
            setSelectedSprint(
              sprints.find((s) => s.id === Number(e.target.value)) || null
            )
          }
        >
          <option value="">Select a sprint</option>
          {sprints.map((sprint) => (
            <option key={sprint.id} value={sprint.id}>
              {sprint.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-row w-full h-1/2 items-center justify-center gap-2">
        <div className="w-1/3 h-full flex flex-col items-center justify-center gap-2">
          <p className="text-gray-700 text-3xl font-semibold mt-2">
            This user has
          </p>
          <p className="text-black text-9xl font-bold mt-1">{userAvgTasks}</p>
          <p className="text-gray-700 text-3xl font-semibold">tasks.</p>
        </div>

        <div className="w-2/3 h-full flex flex-col items-center justify-center">
          <p className="text-2xl">Compared to members of the same team</p>
          <ResponsiveContainer width="100%" height="100%">
            <ChartContainer config={chartConfig}>
              <BarChart
                accessibilityLayer
                data={chartData}
                layout="vertical"
                margin={{ left: 10 }}
              >
                <YAxis
                  dataKey="member"
                  type="category"
                  tickLine={false}
                  axisLine={false}
                />
                <XAxis dataKey="taskBySprint" type="number" hide />
                <ChartTooltip
                  cursor={false}
                  content={<ChartTooltipContent hideLabel />}
                />
                <Bar dataKey="taskBySprint" radius={8} fill="#8884d8">
                  {chartData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={chartConfig[entry.member]?.color || '#8884d8'}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ChartContainer>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
