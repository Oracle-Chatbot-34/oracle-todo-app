import { useEffect, useState } from 'react';
import { Task } from '@/services/tasksService';
import { ArrowLeft } from 'lucide-react';
import { Sprint } from '@/services/sprintService';

import taskService from '@/services/tasksService';
import LoadingSpinner from '@/components/LoadingSpinner';
import SprintTaskCard from './SprintTaskCard';
import { Link } from 'react-router-dom';

type SprintInfoProps = {
  sprint: Sprint;
  setIsExpanded: (value: boolean) => void;
  setExpandedId: (value: number) => void;
};

export default function SprintInfo({
  sprint,
  setIsExpanded,
  setExpandedId,
}: SprintInfoProps) {
  const [tasksInSprint, setTasksInSprint] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  function handleBack() {
    setExpandedId(0);
    setIsExpanded(false);
  }

  useEffect(() => {
    const fetchTasksInThisSprint = async () => {
      if (sprint?.id) {
        try {
          setIsLoading(true);
          const tasks = await taskService.getSprintTasks(sprint.id);
          console.log('Tasks in this sprint:', tasks);
          setTasksInSprint(tasks);
        } catch (err) {
          console.error(`Error fetching tasks for sprint ${sprint.id}:`, err);
          setError('Failed to load sprint tasks. Please try again.');
        } finally {
          setIsLoading(false);
        }
      }
    };

    fetchTasksInThisSprint();
  }, [sprint]);

  if (!sprint) {
    return <div>No sprint selected</div>;
  }

  return (
    <div className="flex flex-col w-full h-full gap-3">
      <div className="flex flex-row items-center gap-7">
        <button
          className="flex flex-row items-center gap-[10px]"
          onClick={handleBack}
        >
          <ArrowLeft className="w-8 h-8" />
          <p className="text-[23px] font-semibold">Go Back</p>
        </button>
        <p className="text-4xl font-semibold">{sprint.name}</p>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
          {error}
        </div>
      )}

      <div className="flex w-full h-full flex-row gap-3">
        <div className="flex w-1/4 h-full flex-col gap-4">
          <div className="flex w-full bg-white h-1/2 flex-col rounded-xl shadow-xl p-4 gap-5">
            <p className="text-3xl">Important Dates</p>
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="text-2xl">
                Sprint Start Date:{' '}
                <p className="font-bold">{sprint.startDate || 'Not set'}</p>
              </div>
              <div className="text-2xl">
                Sprint End Date:{' '}
                <p className="font-bold">{sprint.endDate || 'Not set'}</p>
              </div>
            </div>
          </div>

          <div className="flex w-full bg-white h-1/2 flex-col rounded-xl shadow-xl p-4">
            <p className="text-3xl">Assigned Team</p>
            <p>{sprint.teamId || 'No team assigned'}</p>
          </div>
        </div>

        <div className="flex flex-col h-full w-3/4 bg-white rounded-xl shadow-xl p-4 gap-5">
          <p className="text-3xl">Tasks in this Sprint</p>
          {isLoading ? (
            <div className="flex justify-center items-center h-40">
              <LoadingSpinner size={6} />
            </div>
          ) : (
            <div className="overflow-y-auto max-h-200 flex flex-col gap-9">
              {tasksInSprint.length > 0 ? (
                tasksInSprint.map((task) => (
                  <SprintTaskCard
                    key={task.id}
                    id={task.id || 0}
                    title={task.title}
                    dueDate={task.dueDate || ''}
                    assignedTo={task.assigneeId?.toString() || ''}
                  />
                ))
              ) : (
                <p className="text-center text-gray-500">
                  No tasks in this sprint
                </p>
              )}
            </div>
          )}
          <div>
            This view is read only, if you wish to edit tasks, go to:
            <Link to="/tasks">
              <p className="underline">Task Management</p>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
