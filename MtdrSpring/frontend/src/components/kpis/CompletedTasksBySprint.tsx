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

  useEffect(() => {
    if (!sprintData || sprintData.length === 0) return;

    const allChartData: ChartDataEntry[] = [];
    const memberMap = new Map<string, string>(); // Original name to truncated name mapping
    const truncatedMemberData = new Map<string, number>(); // For combining if truncated names match

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

      // Clear accumulated task counts for this sprint
      truncatedMemberData.clear();

      // Combine task counts for members with same truncated name
      sprint.entries.forEach((entry) => {
        const truncatedName = memberMap.get(entry.member) || entry.member;
        const currentCount = truncatedMemberData.get(truncatedName) || 0;
        truncatedMemberData.set(
          truncatedName,
          currentCount + entry.tasksCompleted
        );
      });

      // Add combined data to sprint entry
      truncatedMemberData.forEach((count, name) => {
        sprintEntry[name] = count;
      });

      allChartData.push(sprintEntry);
    });

    // Get unique truncated member names for the chart config
    const uniqueTruncatedMembers = Array.from(
      new Set(Array.from(memberMap.values()))
    );
    const newChartConfig = generateChartConfig(uniqueTruncatedMembers);

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
              <YAxis />
              <XAxis
                dataKey="sprint"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
                tickFormatter={(value) => value}
              />
              <ChartTooltip cursor={true} />

              {chartData.length > 0 &&
                Object.keys(chartData[0])
                  .filter((key) => key !== 'sprint')
                  .map((memberName) => (
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
