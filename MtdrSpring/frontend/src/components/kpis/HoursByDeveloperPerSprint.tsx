import {
  ResponsiveContainer,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis
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
  // Color mapping that matches the reference design exactly
  const colorMap: { [key: string]: string } = {
    'Daniel Alfredo': '#3b82f6', // Blue
    'Hanna Karina': '#10b981', // Teal/Green
    'José Benjamin': '#f59e0b', // Orange/Amber
    'Yair Salvador': '#8b5cf6', // Purple
  };

  // Fallback colors for additional members
  const fallbackColors = [
    '#ef4444', // Red
    '#06b6d4', // Cyan
    '#84cc16', // Lime
    '#f97316', // Orange
  ];

  return members.reduce((config, member, index) => {
    // First try to match by full name, then by first name
    let color = colorMap[member];

    if (!color) {
      // Try matching by first name
      const firstName = member.split(' ')[0];
      const matchingKey = Object.keys(colorMap).find(
        (key) => key.split(' ')[0] === firstName
      );
      color = matchingKey
        ? colorMap[matchingKey]
        : fallbackColors[index % fallbackColors.length];
    }

    config[member] = {
      label: member,
      color: color,
    };
    return config;
  }, {} as ChartConfig);
};

// Helper function to truncate member names for better display
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

    const allChartData: ChartDataEntry[] = [];
    const memberMap = new Map<string, string>();

    // First pass - collect all unique member names and create truncated versions
    sprintData.forEach((sprint) => {
      sprint.entries.forEach((entry) => {
        const truncatedName = truncateName(entry.member);
        memberMap.set(entry.member, truncatedName);
      });
    });

    // Second pass - build chart data
    sprintData.forEach((sprint) => {
      const sprintEntry: ChartDataEntry = { sprint: sprint.name };
      const truncatedMemberData = new Map<string, number>();

      // Aggregate hours by truncated member name
      sprint.entries.forEach((entry) => {
        const truncatedName = memberMap.get(entry.member) || entry.member;
        const currentHours = truncatedMemberData.get(truncatedName) || 0;
        truncatedMemberData.set(truncatedName, currentHours + entry.hours);
      });

      // Add member hours to sprint entry
      truncatedMemberData.forEach((hours, name) => {
        if (hours > 0) {
          sprintEntry[name] = hours;
        }
      });

      allChartData.push(sprintEntry);
    });

    // Get unique member names that have data
    const uniqueMembers = Array.from(new Set(Array.from(memberMap.values())));
    const membersWithData = uniqueMembers.filter((member) =>
      allChartData.some((sprint) => sprint[member] !== undefined)
    );

    const newChartConfig = generateChartConfig(membersWithData);

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
      {/* Header - now horizontal layout matching the reference */}
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <Clock className="w-6 h-6" />
        <KPITitle
          title="Hours by Developer per Sprint"
          KPIObject={{ definition, example }}
        />
      </div>

      {/* Chart Content */}
      {isLoading ? (
        <div className="flex items-center justify-center">
          <div className="h-28/50 w-28/50">
            <LoadingSpinner />
          </div>
        </div>
      ) : chartData.length === 0 ? (
        <div className="flex items-center justify-center h-40">
          <p className="text-xl">No hours data available</p>
        </div>
      ) : (
        <ResponsiveContainer height="100%" width="100%">
          <ChartContainer config={chartConfig}>
            <BarChart data={chartData}>
              {/* Updated grid styling to match reference */}
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

              {/* Render bars with updated styling */}
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
