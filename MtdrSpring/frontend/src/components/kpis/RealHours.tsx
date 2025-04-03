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
    <div className="flex flex-row p-5">
      {/* Water Fill Effect */}
      <div className="relative w-[12rem] h-[12.5rem] border rounded-lg overflow-hidden">
        <div
          className="absolute bottom-0 left-0 w-full bg-blue-500 transition-all duration-1000 ease-in-out"
          style={{ height: `${percentage}%` }}
        ></div>
        <div className="absolute inset-0 flex justify-center items-center text-white font-bold">
          {percentage}%
        </div>
      </div>

      {/* Hours Data */}
      <div className="relative flex items-center justify-between">
        <div className="absolute top-12 left-4 text-2xl font-bold">{workedHours}</div>
        <div className="w-30 border-b-4 border-black transform rotate-135"></div>
        <div className="absolute bottom-12 right-2 text-2xl font-bold">{plannedHours}</div>
        <p className="absolute bottom-0 left-10 text-center font-semibold">Hours</p>
      </div>
    </div>
  );
}
