import taskService from '@/services/tasksService';
import { useState, useEffect } from 'react';
type Prop = {
  selectedMemberId: number;
};

export default function AvgHours({ selectedMemberId }: Prop) {
  const [userAvgTasks, setUserAvgTasks] = useState(0);

  useEffect(() => {
    const fetchUserTasks = async () => {
      if (selectedMemberId != 0) {
        try {
          // We could also fetch all and get the length, but I think this could be more slick
          const taskResponse = await taskService.getUserAverageOfTasks(
            selectedMemberId
          );
          setUserAvgTasks(taskResponse);
        } catch (err) {}
      }
    };
    fetchUserTasks()
  }, [selectedMemberId != 0 && userAvgTasks != 0]);

  return (
    <div className="text-center">
      {selectedMemberId == 0 ? (
        <div>
          <p className="text-gray-700 text-3xl font-semibold mt-2">
            There is an average of
          </p>
          <p className="text-black text-9xl font-bold mt-1">{userAvgTasks}</p>
          <p className="text-gray-700 text-3xl font-semibold">
            task(s) per employee.
          </p>
        </div>
      ) : (
        <div>
          <p className="text-gray-700 text-3xl font-semibold mt-2">
            This user has
          </p>
          <p className="text-black text-9xl font-bold mt-1">{userAvgTasks}</p>
          <p className="text-gray-700 text-3xl font-semibold">tasks.</p>
        </div>
      )}
    </div>
  );
}
