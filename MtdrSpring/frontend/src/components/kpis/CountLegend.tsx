import LoadingSpinner from '@/components/LoadingSpinner';

type CountLegendProps = {
  isLoading: boolean;
  isHours: boolean;
  count: number;
};

export default function CountLegend({
  isLoading,
  isHours,
  count,
}: CountLegendProps) {
  if (isLoading) {
    return (
      <div className="w-2/3 h-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg items-center justify-center">
        <div className="h-28/50 w-28/50">
          <LoadingSpinner />
        </div>
      </div>
    );
  }

  // Format the count number for better display
  const formattedCount =
    typeof count === 'number' ? count.toLocaleString() : '0';

  return (
    <div className="w-2/3 h-full flex flex-col gap-4 p-5 bg-white rounded-xl shadow-lg">
      {isHours ? (
        <div className="w-full h-full flex flex-col gap-4 p-5 items-center justify-center text-center">
          <div className="text-4xl flex flex-col gap-5">
            We've worked
            <p className="text-6xl font-semibold">{formattedCount}</p>
            hours in this sprint
          </div>
        </div>
      ) : (
        <div className="w-full h-full flex flex-col gap-4 p-5 items-center justify-center text-center">
          <div className="text-4xl flex flex-col gap-5">
            We've completed
            <p className="text-6xl font-semibold">{formattedCount}</p>
            tasks in this sprint
          </div>
        </div>
      )}
    </div>
  );
}
