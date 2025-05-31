import {
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartContainer, ChartTooltip } from '@/components/ui/chart';
import { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import KPITitle from './KPITtitle';
import LoadingSpinner from '@/components/LoadingSpinner';

type ChartConfig = {
  totalHours: {
    label: string;
    color: string;
  };
};

type ChartDataEntry = {
  sprint: string;
  totalHours: number;
  sprintId: number;
};

type MemberEntry = {
  member: string;
  hours: number;
  tasksCompleted: number;
};

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
  totalHours: number;
  totalTasks: number;
};

type TotalHoursBySprintProps = {
  isLoading: boolean;
  sprintData: SprintData[];
  definition: string;
  example: string;
};

const chartConfig: ChartConfig = {
  totalHours: {
    label: 'Total Hours',
    color: '#3b82f6', // Blue color
  },
};

export default function TotalHoursBySprint({
  isLoading,
  sprintData,
  definition,
  example,
}: TotalHoursBySprintProps) {
  const [chartData, setChartData] = useState<ChartDataEntry[]>([]);

  useEffect(() => {
    if (!sprintData || sprintData.length === 0) return;

    console.log('Processing sprint data for total hours by sprint:', sprintData);

    const processedData: ChartDataEntry[] = sprintData.map((sprint) => {
      // Calculate total hours for this sprint by summing all member hours
      const totalSprintHours = sprint.entries.reduce((total, entry) => {
        return total + (entry.hours || 0);
      }, 0);

      return {
        sprint: sprint.name,
        totalHours: totalSprintHours,
        sprintId: sprint.id,
      };
    });

    // Sort by sprint ID to maintain chronological order
    processedData.sort((a, b) => a.sprintId - b.sprintId);

    console.log('Chart data for total hours by sprint:', processedData);
    setChartData(processedData);
  }, [sprintData]);

  // Custom tooltip component
  interface CustomTooltipProps {
    active?: boolean;
    payload?: Array<{
      value: number;
      name?: string;
      dataKey?: string;
      color?: string;
    }>;
    label?: string;
  }
  
  const CustomTooltip = ({ active, payload, label }: CustomTooltipProps) => {
    if (active && payload && payload.length) {
      const data = payload[0];
      return (
        <div className="bg-white p-3 border rounded shadow-lg">
          <p className="font-semibold">{label}</p>
          <p className="text-blue-600">
            Total Hours: {data.value} hours
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="w-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      <div className="flex flex-row text-2xl gap-4 w-full items-center">
        <Clock className="w-6 h-6" />
        <KPITitle
          title="Total Hours Worked by Sprint"
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
          <p className="text-xl">No hours data available</p>
        </div>
      ) : (
        <ResponsiveContainer height="100%" width="100%">
          <ChartContainer config={chartConfig}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <YAxis 
                domain={[0, 'dataMax + 10']}
                label={{ value: 'Hours', angle: -90, position: 'insideLeft' }}
              />
              <XAxis
                dataKey="sprint"
                tickLine={false}
                tickMargin={10}
                axisLine={false}
                tickFormatter={(value) => value}
              />
              <ChartTooltip cursor={true} content={<CustomTooltip />} />
              <Line
                type="monotone"
                dataKey="totalHours"
                stroke={chartConfig.totalHours.color}
                strokeWidth={3}
                dot={{
                  fill: chartConfig.totalHours.color,
                  strokeWidth: 2,
                  r: 6,
                }}
                activeDot={{
                  r: 8,
                  stroke: chartConfig.totalHours.color,
                  strokeWidth: 2,
                }}
              />
            </LineChart>
          </ChartContainer>
        </ResponsiveContainer>
      )}
    </div>
  );
}