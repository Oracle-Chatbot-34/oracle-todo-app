import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
} from '@/components/ui/chart';
import KPITitle from './KPITtitle';
import { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import LoadingSpinner from '@/components/LoadingSpinner';

type MemberEntry = {
  member: string;
  hours: number;
};

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
};

type ChartConfig = Record<
  string,
  {
    label: string;
    color: string;
  }
>;

type ChartDataEntry = {
  member: string;
  workedHours: number;
  // Add sprint breakdown for detailed view
  sprintBreakdown: { [sprintName: string]: number };
};

type HoursByTeamProps = {
  isLoading: boolean;
  sprintData: SprintData[];
  definition: string;
  example: string;
};

const generateChartConfig = (chartData: ChartDataEntry[]): ChartConfig => {
  return chartData.reduce((config, entry, index) => {
    config[entry.member] = {
      label: entry.member,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`,
    };
    return config;
  }, {} as ChartConfig);
};

// Helper function to truncate member names for better display
const truncateName = (name: string): string => {
  const nameParts = name.split(' ');
  // Get only first name and first last name
  return nameParts.length > 1 ? `${nameParts[0]} ${nameParts[1]}` : name;
};

export default function HoursByTeam({
  isLoading,
  sprintData,
  definition,
  example,
}: HoursByTeamProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  useEffect(() => {
    // This implementation addresses the Oracle requirement:
    // "Total Hours worked each developer on each Sprint"
    // by aggregating hours across all sprints while maintaining sprint-level detail

    console.log('Processing sprint data for hours by team:', sprintData);

    const memberMap = new Map<string, string>(); // Original name to truncated name mapping
    const memberHoursMap: Record<
      string,
      {
        totalHours: number;
        sprintBreakdown: { [sprintName: string]: number };
      }
    > = {};

    // First, create mappings of original to truncated names
    sprintData.forEach((sprint) => {
      sprint.entries.forEach(({ member }) => {
        const truncatedName = truncateName(member);
        memberMap.set(member, truncatedName);
      });
    });

    // Then aggregate hours by truncated name and track sprint-level breakdown
    sprintData.forEach((sprint) => {
      sprint.entries.forEach(({ member, hours }) => {
        const truncatedName = memberMap.get(member) || member;

        if (!memberHoursMap[truncatedName]) {
          memberHoursMap[truncatedName] = {
            totalHours: 0,
            sprintBreakdown: {},
          };
        }

        // Add to total hours
        memberHoursMap[truncatedName].totalHours += hours;

        // Track hours per sprint for detailed breakdown
        const sprintName = sprint.name;
        if (!memberHoursMap[truncatedName].sprintBreakdown[sprintName]) {
          memberHoursMap[truncatedName].sprintBreakdown[sprintName] = 0;
        }
        memberHoursMap[truncatedName].sprintBreakdown[sprintName] += hours;
      });
    });

    // Convert to chart data format
    const formattedChartData: ChartDataEntry[] = Object.entries(memberHoursMap)
      .map(([member, data]) => ({
        member,
        workedHours: data.totalHours,
        sprintBreakdown: data.sprintBreakdown,
      }))
      .sort((a, b) => b.workedHours - a.workedHours); // Sort by total hours descending

    console.log('Formatted chart data for hours by team:', formattedChartData);

    setChartData(formattedChartData);
    setChartConfig(generateChartConfig(formattedChartData));
  }, [sprintData]);

  // Custom tooltip to show sprint breakdown
  type CustomTooltipProps = {
    active?: boolean;
    payload?: Array<{
      value: number;
      name: string;
      dataKey: string;
    }>;
    label?: string;
  };

  const CustomTooltip = ({ active, payload, label }: CustomTooltipProps) => {
    if (active && payload && payload.length) {
      const data = chartData.find((item) => item.member === label);
      if (data) {
        return (
          <div className="bg-white p-3 border rounded shadow-lg">
            <p className="font-semibold">{label}</p>
            <p className="text-blue-600">Total Hours: {data.workedHours}</p>
            <div className="mt-2">
              <p className="font-medium text-sm">Sprint Breakdown:</p>
              {Object.entries(data.sprintBreakdown).map(([sprint, hours]) => (
                <p key={sprint} className="text-xs text-gray-600">
                  {sprint}: {hours} hours
                </p>
              ))}
            </div>
          </div>
        );
      }
    }
    return null;
  };

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <Clock className="w-6 h-6" />
        <KPITitle
          title="Worked hours by team member (across all sprints)"
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
          <p className="text-xl">No worked hours data available</p>
        </div>
      ) : (
        <ResponsiveContainer height="100%">
          <ChartContainer config={chartConfig}>
            <BarChart data={chartData}>
              <CartesianGrid vertical={false} />
              <YAxis />
              <XAxis
                dataKey="member"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
              />
              <ChartTooltip cursor={false} content={<CustomTooltip />} />
              <Bar dataKey="workedHours" radius={8}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={chartConfig[entry.member]?.color || '#ccc'}
                  />
                ))}
                <LabelList
                  position="top"
                  offset={12}
                  className="fill-foreground"
                  fontSize={12}
                />
              </Bar>
            </BarChart>
          </ChartContainer>
        </ResponsiveContainer>
      )}
    </div>
  );
}
