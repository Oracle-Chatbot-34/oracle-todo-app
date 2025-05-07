import { memo } from 'react';
import { Task } from '../services/tasksService';

interface TaskCardProps {
  task: Task;
  onEdit: (task: Task) => void;
  onDelete: (taskId: number) => void;
  userName: string;
}

// Using memo to prevent unnecessary re-renders
const TaskCard = memo(({ task, onEdit, onDelete, userName }: TaskCardProps) => {
  const getStatusColor = () => {
    switch (task.status) {
      case 'COMPLETED':
      case 'DONE':
        return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'DELAYED':
        return 'bg-yellow-100 text-yellow-800';
      case 'SELECTED_FOR_DEVELOPMENT':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  // Format date if it exists
  const formatDate = (dateString?: string) => {
    if (!dateString) return 'No date';
    
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString();
    } catch {
      return 'Invalid date';
    }
  };

  const handleEdit = () => {
    onEdit(task);
  };

  const handleDelete = () => {
    if (task.id) {
      onDelete(task.id);
    } else if (task.ID) {
      onDelete(task.ID);
    }
  };


  return (
    <div className="flex bg-card flex-col shrink-0 shadow-md justify-start w-full items-center p-6 rounded-xl gap-2 min-h-32">
      <div className="flex justify-between w-full items-center h-fit">
        <div className="flex items-center gap-3">
          <p className="font-bold text-2xl">{task.title}</p>
          {task.status && (
            <span
              className={`px-2 py-1 rounded text-sm font-medium ${getStatusColor()}`}
            >
              {task.status.replace(/_/g, ' ')}
            </span>
          )}
        </div>
        <p className="text-lg text-slate-700">{userName}</p>
      </div>

      {task.description && <p className="text-lg text-left w-full">{task.description}</p>}

      <div className="flex justify-between w-full h-fit items-center">
        <div className="flex flex-col gap-2 text-slate-800">
          {task.creation_ts && <p>Created: {formatDate(task.creation_ts)}</p>}
          {task.dueDate && (
            <p className="text-slate-900">Due: {formatDate(task.dueDate)}</p>
          )}
        </div>

        <div className="flex flex-col lg:flex-row w-fit min-w-[6.25rem] h-fit gap-2">
          <button 
            onClick={handleEdit}
            className="bg-primary text-white px-4 py-2 rounded hover:bg-primary/90 transition-colors"
          >
            Edit
          </button>
          <button 
            onClick={handleDelete}
            className="bg-destructive text-white px-4 py-2 rounded hover:bg-destructive/90 transition-colors"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
});

TaskCard.displayName = 'TaskCard';

export default TaskCard;