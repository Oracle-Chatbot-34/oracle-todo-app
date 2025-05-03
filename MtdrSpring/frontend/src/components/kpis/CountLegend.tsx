type CountLegendProps = {
  isHours: boolean;
  count: number;
};

export default function CountLegend({ isHours, count }: CountLegendProps) {
  return (
    <div className="w-full h-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      {isHours ? (
        <div className="w-full h-full flex flex-col gap-4 p-5 items-center justify-center text-center">
          <div className="text-4xl flex flex-col gap-5">
            We've worked<p className="text-6xl font-semibold">{count}</p>
            hours in this sprint
          </div>
        </div>
      ) : (
        <div className="w-full h-full flex flex-col gap-4 p-5 items-center justify-center text-center">
          <div className="text-4xl flex flex-col gap-5">
            We've completed<p className="text-6xl font-semibold">{count}</p>
            tasks in this sprint
          </div>
        </div>
      )}
    </div>
  );
}
