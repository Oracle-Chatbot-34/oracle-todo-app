import { useState } from 'react';
type Member = {
  id: number;
  name: string;
}

type ReportUserSelectionProps = {
  members: Member[];
};


export default function ReportUserSelection({members}: ReportUserSelectionProps){
  const [selectedMembers, setSelectedMembers] = useState<Member[]>([]);

  const handleMemberChange = (member: Member) => {
    if (selectedMembers.some((m) => m.id === member.id)) {
      setSelectedMembers(selectedMembers.filter((m) => m.id !== member.id));
    } else {
      setSelectedMembers([...selectedMembers, member]);
    }
  };

  return (
    <div>
      <h2>Select Members</h2>
      {members.map((member) => (
        <div key={member.id}>
          <input
            type="checkbox"
            checked={selectedMembers.some((m) => m.id === member.id)}
            onChange={() => handleMemberChange(member)}
          />
          {member.name}
        </div>
      ))}
    </div>
  );
}