import { useEffect } from 'react';

type MemberEntry = {
  member: string;
  hours: number;
  tasksCompleted: number;
}

type SprintData = {
  id: number;
  name: string;
  entries: MemberEntry[];
}

type Props = {
  sprints: SprintData[];
  startSprint: SprintData;
  endSprint: SprintData | null;
  setStartSprint: (sprint: SprintData) => void;
  setEndSprint: (sprint: SprintData | null) => void;
};

export default function KPIScopeSelection({
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
    <div className="w-full flex gap-4 items-center text-2xl justify-between">
      <div className="flex flex-row items-center gap-3 w-1/2">
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

      <div className="flex flex-row items-center gap-3 w-1/2">
        <label className=" font-semibold">End Sprint: (optional)</label>
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
