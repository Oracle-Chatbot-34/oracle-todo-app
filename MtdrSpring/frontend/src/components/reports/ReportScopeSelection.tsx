import { useEffect } from 'react';

interface Member {
  id: number;
  name: string;
}
type SprintData = {
  id: number;
  name: string;
  members: Member[];
}

type Props = {
  sprints: SprintData[];
  startSprint: SprintData;
  endSprint: SprintData | null;
  setStartSprint: (sprint: SprintData) => void;
  setEndSprint: (sprint: SprintData | null) => void;
};


export default function ReportScopeSelection({
  sprints,
  startSprint,
  endSprint,
  setStartSprint,
  setEndSprint,
}: Props) {
  // Ensure valid end sprint on start change
  useEffect(() => {
    if (endSprint && endSprint.id! < startSprint.id!) {
      setEndSprint(null);
    }
  }, [startSprint]);

  return (
    <div className="w-full flex flex-row items-center justify-center">
      <div className="flex flex-row items-center gap-2 w-1/2">
        <label className=" font-semibold">Start Sprint: </label>
        <select
          className="border px-2 py-1 rounded-lg w-1/2"
          value={startSprint?.id || ''}
          onChange={(e) => {
            const selected = sprints.find(
              (s) => s.id === Number(e.target.value)
            );
            if (selected) setStartSprint(selected);
          }}
        >
          {sprints.map((sprint) => (
            <option key={sprint.id} value={sprint.id}>
              {sprint.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-row items-center gap-2 w-1/2">
        <div className=" font-semibold">End Sprint: (optional)</div>
        <select
          className="border px-2 py-1 rounded-lg w-1/2"
          value={endSprint?.id || ''}
          onChange={(e) => {
            const value = e.target.value;
            if (value === '') {
              setEndSprint(null);
            } else {
              const selected = sprints.find((s) => s.id === Number(value));
              if (selected) setEndSprint(selected);
            }
          }}
        >
          <option value="">None</option>
          {sprints
            .filter((sprint) => sprint.id! > startSprint.id!)
            .map((sprint) => (
              <option key={sprint.id} value={sprint.id}>
                {sprint.name}
              </option>
            ))}
        </select>
      </div>
    </div>
  );
}
