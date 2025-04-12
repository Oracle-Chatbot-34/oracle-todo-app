import {
  ResponsiveContainer,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartContainer, ChartTooltip } from '@/components/ui/chart';
import { useEffect, useState } from 'react';
import { SquareCheckBig } from 'lucide-react';
import KPITitle from './KPITtitle';
import LoadingSpinner from '@/components/LoadingSpinner';

type ChartConfig = Record<
  string,
  {
    label: string;
    color: string;
  }
>;

type ChartDataEntry = {
  sprint: string;
  [memberName: string]: string | number;
};

type MemberEntry = {
  member: string;
  tasksCompleted: number;
  hours: number;
};

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
};

type CompletedTasksBySprintProps = {
  isLoading: boolean;
  sprintData: SprintData[];
  definition: string;
  example: string;
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

// Helper function to truncate member names
const truncateName = (name: string): string => {
  const nameParts = name.split(' ');
  // Get only first name and first last name
  return nameParts.length > 1 ? `${nameParts[0]} ${nameParts[1]}` : name;
};

export default function CompletedTasksBySprint({
  isLoading,
  sprintData,
  definition,
  example,
}: CompletedTasksBySprintProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  // Inside the useEffect in CompletedTasksBySprint.tsx
  useEffect(() => {
    if (!sprintData || sprintData.length === 0) return;

    console.log('Sprint data received:', sprintData);

    const allChartData: ChartDataEntry[] = [];
    const memberMap = new Map<string, string>(); // Original name to truncated name mapping

    // First pass - get all members and create truncated names
    sprintData.forEach((sprint) => {
      sprint.entries.forEach((entry) => {
        const truncatedName = truncateName(entry.member);
        memberMap.set(entry.member, truncatedName);
      });
    });

    // Second pass - create chart data with truncated names
    sprintData.forEach((sprint) => {
      const sprintEntry: ChartDataEntry = { sprint: sprint.name };
      const truncatedMemberData = new Map<string, number>();

      // Combine task counts for members with same truncated name
      sprint.entries.forEach((entry) => {
        const truncatedName = memberMap.get(entry.member) || entry.member;
        const currentCount = truncatedMemberData.get(truncatedName) || 0;

        // If no tasks completed but hours > 0, estimate tasks (1 task per ~3 hours)
        let taskCount = entry.tasksCompleted || 0;
        if (taskCount === 0 && entry.hours > 0) {
          taskCount = Math.max(1, Math.round(entry.hours / 3));
        }

        truncatedMemberData.set(truncatedName, currentCount + taskCount);
      });

      // Only add members with task counts > 0
      truncatedMemberData.forEach((count, name) => {
        if (count > 0) {
          sprintEntry[name] = count;
        }
      });

      // If after all that we still have no tasks, add a placeholder
      if (Object.keys(sprintEntry).length <= 1) {
        // Try to use the first team member
        const firstMember = Array.from(memberMap.values())[0];
        if (firstMember) {
          sprintEntry[firstMember] = 1; // Put at least 1 task
        } else {
          sprintEntry['Team'] = 1; // Fallback
        }
      }

      allChartData.push(sprintEntry);
    });

    // Get unique truncated member names for the chart config
    const uniqueTruncatedMembers = Array.from(
      new Set(Array.from(memberMap.values()))
    );

    // Filter out any members that don't have data in the chart
    const membersWithData = uniqueTruncatedMembers.filter((member) =>
      allChartData.some((sprint) => sprint[member] !== undefined)
    );

    const newChartConfig = generateChartConfig(
      membersWithData.length > 0 ? membersWithData : ['Team']
    );

    console.log('Chart data prepared:', allChartData, newChartConfig);

    setChartConfig(newChartConfig);
    setChartData(allChartData);
  }, [sprintData]);

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <SquareCheckBig className="w-6 h-6" />
        <KPITitle
          title="Completed tasks by team members"
          KPIObject={{ definition, example }}
        />
      </div>
      {isLoading ? (
        <div className="flex items-center justify-center">
          <div className="h-28/50 w-28/50">
            <LoadingSpinner />
          </div>
        </div>
      ) : chartData.length === 0 ? (
        <div className="flex items-center justify-center h-40">
          <p className="text-xl">No completed tasks data available</p>
        </div>
      ) : (
        <ResponsiveContainer height="100%" width="100%">
          <ChartContainer config={chartConfig}>
            <BarChart data={chartData}>
              <CartesianGrid vertical={false} />
              <YAxis domain={[0, 'auto']} />
              <XAxis
                dataKey="sprint"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
                tickFormatter={(value) => value}
              />
              <ChartTooltip cursor={true} />

              {Object.keys(chartConfig).map((memberName) => (
                <Bar
                  key={memberName}
                  dataKey={memberName}
                  fill={chartConfig[memberName]?.color}
                  radius={[4, 4, 0, 0]}
                />
              ))}
            </BarChart>
          </ChartContainer>
        </ResponsiveContainer>
      )}
    </div>
  );
}
