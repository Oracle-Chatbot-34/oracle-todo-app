import React from "react";
import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";

// New type
type LineChartType = {
  month: string;
  avg: number;
};

// Props now expects an array of LineChartType
interface LineChartProps {
  data: LineChartType[];
}

const TasksTime: React.FC<LineChartProps> = ({ data }) => {
  // Extract months and averages
  const categories = data.map(item => item.month);
  const seriesData = data.map(item => item.avg);

  const options: ApexOptions = {
    chart: {
      id: "line-chart",
    },
    title: {
      text: "Tasks over time (months)",
      align: "center",
      margin: 10,
    },
    xaxis: {
      categories: categories, // extracted months
      labels: { rotate: -45 },
    },
    yaxis: {
      title: { text: "Tasks" },
    },
    stroke: {
      curve: "smooth",
      width: 3,
    },
    markers: {
      size: 5,
    },
    grid: {
      borderColor: "#ccc",
      strokeDashArray: 5,
    },
  };

  return (
    <div className="p-4 w-full h-full">
      <Chart
        options={options}
        series={[{ name: "Average Tasks", data: seriesData }]}
        type="line"
        width="100%"
        height="100%"
      />
    </div>
  );
};

export default TasksTime;