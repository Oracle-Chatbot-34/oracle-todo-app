type SprintTaskCardProps = {
  id: number;
  title: string;
  dueDate: string;
  assignedTo: string;
};

export default function SprintTaskCard({
  id,
  title,
  dueDate,
  assignedTo,
}: SprintTaskCardProps) {
  return (
    <div
      className="flex flex-row bg-greyie rounded-lg shadow-xl w-full justify-between"
      key={id}
    >
      <div className="flex flex-col items-start p-4">
        {/* Task type */}
        <p className="text-3xl font-semibold">{title}</p>
        {/* Assigned to */}
        <p className="text-xl italic">
          Assigned to:{' '}
          <span className="font-semibold underline">{assignedTo}</span>
        </p>
      </div>

      <div className="flex flex-col p-4 gap-4">
        <p className="text-xl italic">
          Due date: <span className="font-semibold">{dueDate}</span>
        </p>
      </div>
    </div>
  );
}
