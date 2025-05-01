import {
  ResponsiveContainer,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';
import sprintService from '@/services/sprintService';
import { useState, useEffect } from 'react';

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

type CompletedTasksBySprintProps = {
  teamId: number;
};

const generateChartConfig = (members: string[]): ChartConfig => {
  return members.reduce((config, member, index) => {
    config[member] = {
      label: member,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`, // consistent unique color
    };
    return config;
  }, {} as ChartConfig);
};

export default function CompletedTasksBySprint({
  teamId,
}: CompletedTasksBySprintProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);
  const [chartConfig, setChartConfig] = useState<ChartConfig>({});

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Fetch team sprints (replace with real API call later)
        // const sprints = await sprintService.getTeamSprints(teamId);
        const sprints = Array.from({ length: 6 }, (_, i) => ({
          id: i + 1,
          name: `Sprint ${i + 1}`,
        }));

        const allChartData: ChartDataEntry[] = [];

        // 2. Loop through each sprint
        for (const sprint of sprints) {
          // Simulate team members (replace with real API call later)
          const members = ['Rober', 'Benjamin', 'Hannia'];
          const newChartConfig = generateChartConfig(members); // Example static list
          setChartConfig(newChartConfig);
          const sprintEntry: ChartDataEntry = { sprint: sprint.name };

          members.forEach((member) => {
            // Generate a random number of tasks (simulate fetching)
            const tasksCompleted = Math.floor(Math.random() * 100) + 1;
            sprintEntry[member] = tasksCompleted;
          });

          allChartData.push(sprintEntry);
        }

        setChartData(allChartData);
      } catch (error) {
        console.error('Error fetching chart data:', error);
      }
    };

    fetchData();
  }, []);

  return (
    <div className="w-full h-full">
      <ResponsiveContainer>
        <ChartContainer config={chartConfig as ChartConfig}>
          <BarChart accessibilityLayer data={chartData}>
            <CartesianGrid vertical={false} />
            <YAxis/>
            <XAxis
              dataKey="sprint"
              tickLine={false}
              tickMargin={10}
              axisLine={false}
              tickFormatter={(value) => value.slice(0, 3)}
            />
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent indicator="dashed" />}
            />
            {chartData.length > 0 &&
              Object.keys(chartData[0])
                .filter((key) => key !== 'sprint')
                .map((memberName, index) => (
                  <Bar
                    key={memberName}
                    dataKey={memberName}
                    fill={`hsl(${(index * 50) % 360}, 70%, 60%)`} // Different color per member
                    radius={[4, 4, 0, 0]}
                  />
                ))}
          </BarChart>
        </ChartContainer>
      </ResponsiveContainer>
    </div>
  );
}
