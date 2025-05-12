import { useState } from 'react';

type Member = {
  id: number;
  name: string;
};

type ReportUserSelectionProps = {
  members: Member[];
};

export default function ReportUserSelection({
  members,
}: ReportUserSelectionProps) {
  const [selectedMembers, setSelectedMembers] = useState<Member[]>([]);

  const allSelected =
    members.length > 0 && selectedMembers.length === members.length;

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelectedMembers([]);
    } else {
      setSelectedMembers(members);
    }
  };

  const handleMemberChange = (member: Member) => {
    if (selectedMembers.some((m) => m.id === member.id)) {
      setSelectedMembers(selectedMembers.filter((m) => m.id !== member.id));
    } else {
      setSelectedMembers([...selectedMembers, member]);
    }
  };

  return (
    <div className="px-4 text-2xl text-gray-700 w-[50vh] max-h-[20vh] overflow-y-scroll ">
      <div className="flex items-center mb-3">
        <input
          id="selectAll"
          type="checkbox"
          checked={allSelected}
          onChange={toggleSelectAll}
          className="w-5 h-5 accent-redie"
        />
        <label htmlFor="selectAll" className="ml-2">
          Select All
        </label>
      </div>

      {/* Scrollable checkbox list */}
      <div className="flex flex-col gap-2 overflow-y-auto max-h-[30vh] pr-1">
        {members.map((member) => (
          <div key={member.id} className="flex items-center space-x-2">
            <input
              type="checkbox"
              id={`member-${member.id}`}
              checked={selectedMembers.some((m) => m.id === member.id)}
              onChange={() => handleMemberChange(member)}
              className="w-5 h-5 rounded-lg  accent-redie"
            />
            <label htmlFor={`member-${member.id}`}>{member.name}</label>
          </div>
        ))}
      </div>
    </div>
  );
}
