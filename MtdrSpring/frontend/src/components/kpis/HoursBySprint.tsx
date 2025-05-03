import { Cell, Label, Pie, PieChart, ResponsiveContainer } from 'recharts';
import KPITitle from '@/components/kpis/KPITtitle';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';
import { AlarmClockCheck, ListChecks } from 'lucide-react';
import React from 'react';

/*
This component could receive two types of data:
1. Sprint data with total hours for each sprint:
[
  {
    id: 1,
    name: "Sprint 1",
    totalHours: 100,
  },
  {
    id: 2,
    name: "Sprint 2",
    totalHours: 150,
  },
  // Add more sprints as needed
]
2. Sprint data with total tasks for each sprint:
[
  {
    id: 1,
    name: "Sprint 1",
    totalTasks: 10,
  },
  {
    id: 2,
    name: "Sprint 2",
    totalTasks: 15,
  },
  // Add more sprints as needed
 */

type SprintData = {
  id: number;
  name: string;
  count: number;
};

type HoursBySprintProps = {
  isHours: boolean;
  chartData: SprintData[];
  definition: string;
  example: string;
};

type ChartConfig = {
  [key: string]: {
    label: string;
    color: string;
  };
};

const generateChartConfig = (labels: string[]): ChartConfig => {
  return labels.reduce((config, label, index) => {
    config[label] = {
      label,
      color: `hsl(${(index * 50) % 360}, 70%, 60%)`,
    };
    return config;
  }, {} as ChartConfig);
};

export default function HoursBySprints({
  isHours,
  chartData,
  definition,
  example,
}: HoursBySprintProps) {
  // Calculate the total count from chartData
  const totalCount = React.useMemo(() => {
    return chartData.reduce((acc, entry) => acc + entry.count, 0);
  }, []);

  console.log('chartData', chartData, isHours);
  const chartConfig = generateChartConfig(chartData.map((s) => s.name));
  return (
    <div className="w-1/2 h-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        {isHours ? (
          <AlarmClockCheck className="w-6 h-6" />
        ) : (
          <ListChecks className="w-6 h-6" />
        )}
        <KPITitle
          title={
            isHours
              ? 'Worked Hours in Range'
              : 'Completed Tasks in Range'
          }
          KPIObject={{definition, example}}
        />
      </div>

      <ResponsiveContainer height="100%" width="100%">
        <ChartContainer config={chartConfig}>
          <PieChart>
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent hideLabel />}
            />
            <Pie
              data={chartData}
              dataKey="count"
              nameKey="name"
              innerRadius={60}
              strokeWidth={5}
            >
              <Label
                content={({ viewBox }) => {
                  if (viewBox && 'cx' in viewBox && 'cy' in viewBox) {
                    return (
                      <text
                        x={viewBox.cx}
                        y={viewBox.cy}
                        textAnchor="middle"
                        dominantBaseline="middle"
                      >
                        <tspan
                          x={viewBox.cx}
                          y={viewBox.cy}
                          className="fill-foreground text-3xl font-bold"
                        >
                          {totalCount.toLocaleString()}
                        </tspan>
                        <tspan
                          x={viewBox.cx}
                          y={(viewBox.cy || 0) + 24}
                          className="fill-muted-foreground"
                        >
                          {isHours ? 'Hours' : 'Tasks'}
                        </tspan>
                      </text>
                    );
                  }
                }}
              />
              {chartData.map((entry) => (
                <Cell
                  key={entry.id}
                  fill={chartConfig[entry.name]?.color || '#ccc'}
                />
              ))}
            </Pie>
          </PieChart>
        </ChartContainer>
      </ResponsiveContainer>
    </div>
  );
}
