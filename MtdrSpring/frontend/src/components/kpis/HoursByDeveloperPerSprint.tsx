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
  const colors = [
    'hsl(220, 70%, 60%)', // Blue
    'hsl(160, 70%, 55%)', // Green
    'hsl(30, 70%, 60%)', // Orange
    'hsl(280, 70%, 60%)', // Purple
    'hsl(350, 70%, 60%)', // Red
    'hsl(200, 70%, 60%)', // Cyan
    'hsl(50, 70%, 60%)', // Yellow
    'hsl(300, 70%, 60%)', // Magenta
  ];

  return members.reduce((config, member, index) => {
    config[member] = {
      label: member,
      color: colors[index % colors.length],
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
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-slate-200">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-blue-100 rounded-lg">
            <Clock className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-slate-800">
              Hours by Developer per Sprint
            </h3>
            <p className="text-sm text-slate-600">Oracle DevOps Requirement</p>
          </div>
        </div>
        <KPITitle title="" KPIObject={{ definition, example }} />
      </div>

      {/* Chart Content */}
      <div className="flex-1 p-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <LoadingSpinner />
          </div>
        ) : chartData.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center text-slate-500">
              <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p className="text-lg">No hours data available</p>
              <p className="text-sm">Ensure tasks have logged hours</p>
            </div>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <ChartContainer config={chartConfig}>
              <BarChart
                data={chartData}
                margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                <YAxis
                  label={{ value: 'Hours', angle: -90, position: 'insideLeft' }}
                  tick={{ fontSize: 12 }}
                />
                <XAxis
                  dataKey="sprint"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) =>
                    value.length > 10 ? value.substring(0, 10) + '...' : value
                  }
                />
                <ChartTooltip
                  cursor={{ fill: 'rgba(0, 0, 0, 0.1)' }}
                  labelStyle={{ color: '#1e293b', fontWeight: 'bold' }}
                  contentStyle={{
                    backgroundColor: 'white',
                    border: '1px solid #e2e8f0',
                    borderRadius: '8px',
                    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                  }}
                />
                <Legend
                  wrapperStyle={{ paddingTop: '10px', fontSize: '12px' }}
                />

                {Object.keys(chartConfig).map((memberName) => (
                  <Bar
                    key={memberName}
                    dataKey={memberName}
                    fill={chartConfig[memberName]?.color}
                    radius={[2, 2, 0, 0]}
                  />
                ))}
              </BarChart>
            </ChartContainer>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
