import React from "react";
import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";


// Define the props type
interface PieChartProps {
    data: number[];
}

const PieChart: React.FC<PieChartProps> = ({ data }) => {
  const options: ApexOptions = {
    chart: {
      id: "pie-chart",
    },
    title: {
        text: "Active Tasks",
        align: "center",
        margin: 10,
    },
    labels: ["On time", "Might not make it", "Beyond the deadline"],
    colors:['#00A884', '#EED202', '#ff4545'],
    legend: {
        position: "bottom",
        horizontalAlign: "center",
    },
    dataLabels: {
        enabled: true,
        formatter: (_, { seriesIndex, w }) => w.config.series[seriesIndex], // Show raw numbers
    },
    tooltip: {
        y: {
            formatter: (val) => val.toString(), // Ensure raw numbers in tooltip
        },
    },
    
  };

  return (
    <div className="chart-container">
      <Chart options={options} series={data} type="donut" width="350" />
    </div>
  );
};

export default PieChart;
