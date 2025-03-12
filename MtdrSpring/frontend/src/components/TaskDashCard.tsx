import React from 'react';

interface TaskDashCardProps {
    id?: number;
    title?: string;
    dueDate?: string;
    assignedTo?: string;
}

const TaskDashCard: React.FC<TaskDashCardProps> = ({ id, title, dueDate, assignedTo }) => {
    const handleEdit = (): void => {
        console.log("Editing task", id);
        // Your edit logic here
    };

    const handleDelete = (): void => {
        console.log("Deleting task", id);
        // Your delete logic here
    };
    return (
        <div className="flex flex-row bg-greyie rounded-lg shadow-xl w-[540px] h-[105px]" style={{ marginTop: '30px' }}>
            <div className="flex flex-col items-start" style={{ marginLeft: '20px', marginTop: '5px' }}>
                {/* Task type */}
                <p className="text-[24px] font-bold" style={{ marginTop: '10px' }}>{title}</p>
                {/* Assigned to */}
                <p className="text-[16px] italic">
                    Assigned to: <span className="font-semibold underline">{assignedTo}</span>
                </p>
            </div>

            <div className="flex flex-row items-start" style={{ marginLeft: '90px', marginTop: '10px' }}>
                <p className="text-[16px] italic">
                    Due date: <span className="font-semibold">{dueDate}</span>
                </p>
            </div>
            
        </div>
    );
};

export default TaskDashCard;