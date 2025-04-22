import { useEffect, useState } from 'react';
import { Task } from '@/services/tasksService';
import { ArrowLeft } from 'lucide-react';
import { Sprint } from '@/services/sprintService';

import { dummyTasks } from './tasksdummy';
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

  function handleBack() {
    setExpandedId(0);
    setIsExpanded(false);
  }

  useEffect(() => {
    const fetchTasksInThisSprint = async () => {
      setTasksInSprint(dummyTasks);
      console.log(tasksInSprint);
    };

    fetchTasksInThisSprint();
  }, []);

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

      <div className="flex w-full h-full flex-row gap-3">
        <div className="flex w-1/4 h-full flex-col gap-4">
          <div className="flex w-full bg-white h-1/2 flex-col rounded-xl shadow-xl p-4 gap-5">
            <p className="text-3xl">Important Dates</p>
            <div className="flex flex-col items-center gap-4 text-center">
              <p className="text-2xl">
                Sprint Start Date:{' '}
                <p className="font-bold">{sprint.startDate}</p>
              </p>
              <p className="text-2xl">
                Sprint End Date: <p className="font-bold">{sprint.endDate}</p>
              </p>
            </div>
          </div>

          <div className="flex w-full bg-white h-1/2 flex-col rounded-xl shadow-xl p-4">
            <p className="text-3xl">Assigned Team</p>
            <p>{sprint.teamId}</p>
          </div>
        </div>

        <div className="flex flex-col h-full w-3/4 bg-white rounded-xl shadow-xl p-4 gap-5">
          <p className="text-3xl">Tasks in this Sprint</p>
          <div className="overflow-y-auto max-h-200 flex flex-col gap-9">
            {tasksInSprint.map((task) => (
              <SprintTaskCard
                key={task.ID}
                id={task.ID || 0}
                title={task.title}
                dueDate={task.dueDate || ''}
                assignedTo={task.assigneeId?.toString() || ''}
              />
            ))}
          </div>
          <p>
            This view is read only, if you wish to edit tasks, go to:
            <Link to="/tasks">
              <p className="underline">Task Management</p>
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
