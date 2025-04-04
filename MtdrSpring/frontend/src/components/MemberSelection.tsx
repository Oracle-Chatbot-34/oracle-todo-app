import { Member } from './ScopeSelection';

type Props = {
  isIndividual: boolean;
  setIsIndividual: (value: boolean) => void;
  selectedMemberProp: Member | null;
  setSelectedMemberProp: (member: Member | null) => void;
};

export default function MemberSelection({ isIndividual, selectedMemberProp, setSelectedMemberProp }: Props) {
  const members: Member[] = [
    { id: 1 , name: 'Daniel Barreras' },
    { id: 2 , name: 'Benjamin Ortiz' },
    { id: 3 , name: 'Emiliano Nieto' },
  ];

  if (isIndividual)
    return (
      <div className="w-full">
        <p className="text-[#747276] text-[1.5625rem]">Select a member</p>
        <select
          className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white text-[20px]"
          value={selectedMemberProp?.id || ''}
          onChange={(e) => {
            const memberId = e.target.value;
            const member = members.find((m) => m.id === Number(memberId)) || null;
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
