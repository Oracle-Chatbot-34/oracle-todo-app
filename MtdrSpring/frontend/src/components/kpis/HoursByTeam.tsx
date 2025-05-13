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
  ChartTooltipContent,
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

type ChartDataEntree = {
  member: string;
  workedHours: number;
};

type HoursByTeamProps = {
  isLoading: boolean;
  sprintData: SprintData[];
  definition: string;
  example: string;
};

const generateChartConfig = (chartData: ChartDataEntree[]): ChartConfig => {
  return chartData.reduce((config, entry, index) => {
    config[entry.member] = {
      label: entry.member,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`,
    };
    return config;
  }, {} as ChartConfig);
};

export default function HoursByTeam({
  isLoading,
  sprintData,
  definition,
  example,
}: HoursByTeamProps) {
  const [chartData, setChartData] = useState<ChartDataEntree[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  useEffect(() => {
    // Aggregate hours by member from the passed sprint data
    const memberHoursMap: Record<string, number> = {};

    sprintData.forEach((sprint) => {
      sprint.entries.forEach(({ member, hours }) => {
        if (!memberHoursMap[member]) {
          memberHoursMap[member] = 0;
        }
        memberHoursMap[member] += hours;
      });
    });

    const formattedChartData: ChartDataEntree[] = Object.entries(
      memberHoursMap
    ).map(([member, workedHours]) => ({
      member,
      workedHours,
    }));

    setChartData(formattedChartData);
    setChartConfig(generateChartConfig(formattedChartData));
  }, [sprintData]);

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <Clock className="w-6 h-6" />
        <KPITitle
          title="Worked hours by team member"
          KPIObject={{ definition, example }}
        />
      </div>
      {isLoading ? (
        <div className="flex items-center justify-center">
          <div className='h-28/50 w-28/50'>
            <LoadingSpinner />
          </div>
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
              <ChartTooltip
                cursor={false}
                content={<ChartTooltipContent hideLabel />}
              />
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
