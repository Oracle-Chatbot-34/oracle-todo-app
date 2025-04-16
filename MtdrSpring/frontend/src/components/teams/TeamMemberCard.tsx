type TeamMemberCardProps = {
  name: string;
  role: string;
};

export default function TeamMemberCard({ name, role }: TeamMemberCardProps) {
  return (
    <div
      className={
        'p-5 rounded-lg flex flex-row gap-x-4 bg-greyie text-black justify-between items-center'
      }
    >
      <div>
        <p className="text-3xl">{name}</p>

        <p className="text-xl italic">{role}</p>
      </div>

      <div className="bg-redie rounded-lg flex flex-row justify-center items-center h-12 w-35 shadow-lg">
        <p className="text-2xl text-white">Remove</p>
      </div>
    </div>
  );
}
