import { Member } from './ScopeSelection';

type Props = {
  isIndividual: boolean;
  setIsIndividual: (value: boolean) => void;
  selectedMemberProp: Member | null;
  setSelectedMemberProp: (member: Member | null) => void;
};

export default function MemberSelection({ isIndividual, selectedMemberProp, setSelectedMemberProp }: Props) {
  const members: Member[] = [
    { id: '1', name: 'Daniel Barreras' },
    { id: '2', name: 'Benjamin Ortiz' },
    { id: '3', name: 'Emiliano Nieto' },
  ];

  if (isIndividual)
    return (
      <div className="w-full">
        <p className="text-[#747276] text-[1.5625rem]">Select a member</p>
        <select
          style={{
            width: '100%',
            padding: '5px',
            borderRadius: '10px',
            border: '2px solid #DFDFE4',
            transition: 'box-shadow 0.2s ease-in-out',
            backgroundColor: 'white',
            fontSize: '20px',
          }}
          value={selectedMemberProp?.id || ''}
          onChange={(e) => {
            const memberId = e.target.value;
            const member = members.find((m) => m.id === memberId) || null;
            setSelectedMemberProp(member);
          }}
        >
          <option value="" disabled></option>
          {members.map((member) => (
            <option key={member.id} value={member.id}>
              {member.name}
            </option>
          ))}
        </select>
      </div>
    );
}
