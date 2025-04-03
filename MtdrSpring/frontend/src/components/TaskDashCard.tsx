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
    <div
      className="flex flex-row bg-card rounded-lg shadow-xl w-[540px] h-[105px] mt-6"
    >
      <div
        className="flex flex-col items-start ml-5 mt-2"
      >
        {/* Task type */}
        <p className="text-[24px] font-bold mt-2" >
          {title}
        </p>
        {/* Assigned to */}
        <p className="text-[16px] italic">
          Assigned to:{' '}
          <span className="font-semibold underline">{assignedTo}</span>
        </p>
      </div>

      <div
        className="flex flex-col items-start gap-[20px] mt-2 ml-24"
        style={{ marginLeft: '100px', marginTop: '10px' }}
      >
        <div className="flex flex-row items-start">
          <p className="italic">
            Due date: <span className="font-semibold">{dueDate}</span>
          </p>
        </div>
        {/* Edit and delete buttons */}
        <div
          className="flex flex-row items-start gap-[20px] ml-7"
        >
          <div
            className="bg-whitiish rounded-lg flex flex-row justify-center items-center w-[54px] h-[40px] shadow-lg"
            style={{ border: '2px #767676 solid' }}
          >
            <button
              type="button"
              className="w-[54px] h-[40px]"
              onClick={handleEdit}
            >
              <p>Edit</p>
            </button>
          </div>

          <div className="bg-redie rounded-lg flex flex-row justify-center items-center h-[40px] w-[73px] shadow-lg">
            <button
              type="button"
              className="w-[73px] h-[40px]"
              onClick={handleDelete}
            >
              <p className="text-white">Delete</p>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TaskDashCard;
