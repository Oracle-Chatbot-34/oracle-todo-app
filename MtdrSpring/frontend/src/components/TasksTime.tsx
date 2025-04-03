import React from "react";
import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";

// Define the props type
interface LineChartProps {
    data: number[];
    categories: string[]; // Time span (e.g., years)
}

const TasksTime: React.FC<LineChartProps> = ({ data, categories }) => {
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
      categories: categories, // Time span (years)
      labels: { rotate: -45 }, // Tilt labels for readability
    },
    yaxis: {
      title: { text: "Tasks" },
    },
    stroke: {
      curve: "smooth", // Smooth line
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
    <div>
      <Chart options={options} series={[{ name: "Data", data }]} type="line" width="500" />
    </div>
  );
};

export default TasksTime;
