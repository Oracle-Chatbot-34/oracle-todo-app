
type TaskDashCardProps = {
  id: number;
  title: string;
  dueDate: string;
  assignedTo: number;
};

export default function TaskDashCard({
  id,
  title,
  dueDate,
  assignedTo,
}: TaskDashCardProps) {
  return (
    <div key={id} className="flex flex-row bg-whitie rounded-lg shadow-md w-full h-full p-3 justify-between">
      <div className="flex flex-col items-start gap-3">
        {/* Task type */}
        <p className="text-2xl font-bold mt-2">{title}</p>
        {/* Assigned to */}
        <div className="text-xl italic">
          Assigned to:{' '}
          <span className="font-semibold underline">{assignedTo}</span>
        </div>
      </div>

      <div className="flex flex-col items-start gap-5 text-right">
        <div className="flex flex-row items-start">
          <div className="italic">
            Due date: <p className="font-semibold">{dueDate || 'No due date'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
