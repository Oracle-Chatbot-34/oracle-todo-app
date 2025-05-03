import {
  ResponsiveContainer,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
  Legend,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';
import { CustomLegend } from './CustomLegend';
import { useEffect, useState } from 'react';
import { SquareCheckBig } from 'lucide-react';

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
  sprintData: SprintData[];
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

export default function CompletedTasksBySprint({
  sprintData,
}: CompletedTasksBySprintProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  useEffect(() => {
    if (!sprintData || sprintData.length === 0) return;

    const allChartData: ChartDataEntry[] = [];
    const memberSet = new Set<string>();

    sprintData.forEach((sprint) => {
      const sprintEntry: ChartDataEntry = { sprint: sprint.name };
      sprint.entries.forEach((entry) => {
        sprintEntry[entry.member] = entry.tasksCompleted;
        memberSet.add(entry.member);
      });
      allChartData.push(sprintEntry);
    });

    const members = Array.from(memberSet);
    const newChartConfig = generateChartConfig(members);

    setChartConfig(newChartConfig);
    setChartData(allChartData);
  }, [sprintData]);

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-1/2">
        <SquareCheckBig className="w-6 h-6" />
        <p className="font-semibold">Completed Tasks by Developer</p>
      </div>

      <ResponsiveContainer height="100%">
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
            <ChartTooltip
              cursor={true}
            />
    
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
    </div>
  );
}