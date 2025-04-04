import Chart from 'react-apexcharts';
import { ApexOptions } from 'apexcharts';

interface PieChartProps {
  data: number[];
}

export default function TimeCompletionRate({ data }: PieChartProps) {
  const options: ApexOptions = {
    chart: {
      id: 'time-completion-chart',
    },
    labels: ['OTCR', 'OTR', 'In-Progress'],
    colors: ['#F68121', '#265599', '#8C57B2'],
    legend: {
      position: 'right',
      fontSize: '20px',
    },
    dataLabels: {
      enabled: true,
      formatter: (val: number) => `${val.toFixed(2)}%`, // Formats as percentage
    },
    tooltip: {
      enabled: true,
      y: {
        formatter: (val: number) => `${val.toFixed(2)} tasks`, // Custom tooltip formatting
      },
    },
  };

  return (
    <div className='flex h-full w-full'>
      <Chart options={options} series={data} type="pie" width="200%" height={"80%"} />
    </div>
  );
}
