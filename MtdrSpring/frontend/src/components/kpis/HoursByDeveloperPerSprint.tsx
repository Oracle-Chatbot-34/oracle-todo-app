import {
  ResponsiveContainer,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
  Legend,
} from 'recharts';
import { ChartContainer, ChartTooltip } from '@/components/ui/chart';
import { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
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
  hours: number;
};

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
};

type HoursByDeveloperPerSprintProps = {
  isLoading: boolean;
  sprintData: SprintData[];
  definition: string;
  example: string;
};

const generateChartConfig = (members: string[]): ChartConfig => {
  return members.reduce((config, member, index) => {
    config[member] = {
      label: member,
      color: `hsl(${(index * 45) % 360}, 70%, 55%)`,
    };
    return config;
  }, {} as ChartConfig);
};

// Helper function to truncate member names
const truncateName = (name: string): string => {
  const nameParts = name.split(' ');
  return nameParts.length > 1 ? `${nameParts[0]} ${nameParts[1]}` : name;
};

export default function HoursByDeveloperPerSprint({
  isLoading,
  sprintData,
  definition,
  example,
}: HoursByDeveloperPerSprintProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  useEffect(() => {
    if (!sprintData || sprintData.length === 0) return;

    console.log(
      'Processing sprint data for hours per developer per sprint:',
      sprintData
    );

    // This implementation directly addresses the Oracle requirement:
    // "Show Worked Hours by Developer per Sprint"
    // Each bar represents a sprint, with stacked segments showing each developer's hours

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

      // Combine hours for members with same truncated name
      sprint.entries.forEach((entry) => {
        const truncatedName = memberMap.get(entry.member) || entry.member;
        const currentHours = truncatedMemberData.get(truncatedName) || 0;
        truncatedMemberData.set(truncatedName, currentHours + entry.hours);
      });

      // Add each developer's hours to the sprint entry
      truncatedMemberData.forEach((hours, name) => {
        if (hours > 0) {
          sprintEntry[name] = hours;
        }
      });

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
      membersWithData.length > 0 ? membersWithData : ['No Data']
    );

    console.log(
      'Chart data for hours per developer per sprint:',
      allChartData,
      newChartConfig
    );

    setChartConfig(newChartConfig);
    setChartData(allChartData);
  }, [sprintData]);

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <Clock className="w-6 h-6" />
        <KPITitle
          title="Hours worked by developer per sprint"
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
          <p className="text-xl">No hours data available per sprint</p>
        </div>
      ) : (
        <ResponsiveContainer height="100%" width="100%">
          <ChartContainer config={chartConfig}>
            <BarChart data={chartData}>
              <CartesianGrid vertical={false} />
              <YAxis
                domain={[0, 'auto']}
                label={{ value: 'Hours', angle: -90, position: 'insideLeft' }}
              />
              <XAxis
                dataKey="sprint"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
                tickFormatter={(value) => value}
              />
              <ChartTooltip cursor={true} />
              <Legend />

              {Object.keys(chartConfig).map((memberName) => (
                <Bar
                  key={memberName}
                  dataKey={memberName}
                  stackId="hours"
                  fill={chartConfig[memberName]?.color}
                  radius={[2, 2, 0, 0]}
                />
              ))}
            </BarChart>
          </ChartContainer>
        </ResponsiveContainer>
      )}
    </div>
  );
}
