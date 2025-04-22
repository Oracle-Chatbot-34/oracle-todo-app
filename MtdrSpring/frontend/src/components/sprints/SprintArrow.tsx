import { Sprint } from '@/services/sprintService';
import { dummyTasks } from './tasksdummy';

type SprintArrowProps = {
  sprint: Sprint,
  setIsExpanded: (value: boolean) => void,
  setExpandedId: (value: number) => void
  
};

export default function SprintArrow({ sprint, setIsExpanded, setExpandedId }: SprintArrowProps) {
  function handleOnClick() {
    setIsExpanded(true)

    if (sprint.id !== undefined) {
      setExpandedId(sprint.id);
    }
  }

  return (
    <div>
      Sprint Name: {sprint.name}
      <br />
      Sprint ID: {sprint.id}
      <br />
      Sprint Start: {sprint.startDate}
      <br />
      Sprint End: {sprint.endDate}

      <button className="bg-greenie p-2 rounded-xl text-white" onClick={handleOnClick}>Expand this sprint</button>
    </div>
  );
}
