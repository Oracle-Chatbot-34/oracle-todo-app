interface RealHoursProps {
  percentage: number;
  workedHours: number;
  plannedHours: number;
}

export default function RealHours({
  percentage,
  workedHours,
  plannedHours,
}: RealHoursProps) {
  return (
    <div className="bg-whitiish2 rounded-2xl shadow-lg flex flex-row p-6">
      {/* Water Fill Effect */}
      <div className="relative w-[12rem] h-[12rem] mx-auto border rounded-lg overflow-hidden">
        <div
          className="absolute bottom-0 left-0 w-full bg-blue-500 transition-all duration-1000 ease-in-out"
          style={{ height: `${percentage}%` }}
        ></div>
        <div className="absolute inset-0 flex justify-center items-center text-white font-bold">
          {percentage}%
        </div>
      </div>

      {/* Hours Data */}
      <div className="flex items-center justify-between">
        <div className="text-2xl font-bold">{workedHours}</div>
        <div className="w-10 border-b-4 border-black transform rotate-135"></div>
        <div className="text-2xl font-bold">{plannedHours}</div>
      </div>
      <p className="text-center font-semibold">Hours</p>
    </div>
  );
}
