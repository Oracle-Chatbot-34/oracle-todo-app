import React from 'react';

interface TaskDashCardProps {
  id?: number;
  title?: string;
  dueDate?: string;
  assignedTo?: string;
}

const TaskDashCard: React.FC<TaskDashCardProps> = ({
  id,
  title,
  dueDate,
  assignedTo,
}) => {
  const handleEdit = (): void => {
    console.log('Editing task', id);
    // Your edit logic here
  };

  const handleDelete = (): void => {
    console.log('Deleting task', id);
    // Your delete logic here
  };
  return (
    <div className="flex flex-row bg-whitie rounded-lg shadow-md w-full h-full p-3 justify-between">
      <div className="flex flex-col items-start gap-3">
        {/* Task type */}
        <p className="text-2xl font-bold mt-2">{title}</p>
        {/* Assigned to */}
        <p className="text-xl italic">
          Assigned to:{' '}
          <span className="font-semibold underline">{assignedTo}</span>
        </p>
      </div>

      <div className="flex flex-col items-start gap-5">
        <div className="flex flex-row items-start">
          <p className="italic">
            Due date: <span className="font-semibold">{dueDate}</span>
          </p>
        </div>
        {/* Edit and delete buttons */}
        <div className="flex flex-row items-start gap-5 ml-7">
          <button
            type="button"
            className="bg-whitiish rounded-lg flex flex-row justify-center items-center w-[54px] h-[40px] shadow-lg"
            onClick={handleEdit}
          >
            <p>Edit</p>
          </button>

          <button
            type="button"
            className="rounded-lg flex flex-row justify-center items-center h-[40px] w-[73px] shadow-lg bg-redie"
            onClick={handleDelete}
          >
            <p className="text-white">Delete</p>
          </button>
        </div>
      </div>
    </div>
  );
};

export default TaskDashCard;
