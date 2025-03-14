import React from 'react';

interface TaskCardProps {
    id?: number;
    title?: string;
    dueDate?: string;
    assignedTo?: string;
}

const TaskCard: React.FC<TaskCardProps> = ({ id, title, dueDate, assignedTo }) => {
    const handleEdit = (): void => {
        console.log("Editing task", id);
        // Your edit logic here
    };

    const handleDelete = (): void => {
        console.log("Deleting task", id);
        // Your delete logic here
    };
    return (
        <div className="flex flex-row bg-greyie rounded-lg shadow-xl md: w-[650px] h-[105px] justify-between" style={{ marginTop: '30px' }}>
            <div className="flex flex-col items-start" style={{ marginLeft: '20px', marginTop: '5px' }}>
                {/* Task type */}
                <p className="text-[24px] font-bold" style={{ marginTop: '10px' }}>{title}</p>
                {/* Assigned to */}
                <p className="text-[16px] italic">
                    Assigned to: <span className="font-semibold underline">{assignedTo}</span>
                </p>
            </div>

            <div className="flex flex-col items-start gap-[20px] pr-3">
                <div className="flex flex-row items-start mr-3">
                    <p className="text-[16px] italic ">
                        Due date: <span className="font-semibold">{dueDate}</span>
                    </p>
                </div>
                {/* Edit and delete buttons */}
                <div className="flex flex-row items-start gap-[20px]">

                    <div className="bg-whitiish rounded-lg flex flex-row justify-center items-center w-[54px] h-[40px] shadow-lg" >
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

export default TaskCard;