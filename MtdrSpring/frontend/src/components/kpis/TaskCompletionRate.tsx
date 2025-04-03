import Chart from 'react-apexcharts';
import { ApexOptions } from 'apexcharts';

// Define the props type
type LineChartProps = {
  data: number[];
  categories: string[]; // Time span. (e.g., two weeks)
};

export default function TaskCompletionRate({
  data,
  categories,
}: LineChartProps) {
  const options: ApexOptions = {
    chart: {
      id: 'kpi-line-chart',
    },
    xaxis: {
      title: { text: 'Time Span' },
      categories: categories, // Time span (sprints/two weeks)
      labels: { rotate: -45 },
    },
    yaxis: {
      title: { text: 'Task Completion Rate' },
    },
    stroke: {
      curve: 'smooth', // Smooth line
      width: 3,
    },
    markers: {
      size: 5,
    },
    grid: {
      borderColor: '#ccc',
      strokeDashArray: 5,
    },
  };

  return (
    <div>
      <Chart
        options={options}
        series={[{ name: 'Data', data }]}
        type="line"
      />
    </div>
  );
}
