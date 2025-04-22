type TeamSprintsProps = {
  team: number;
  name: string;
  status: string;
};

export default function TeamSprints({ name, status, }: TeamSprintsProps) {
  return (
    <div
      className={
        'p-5 rounded-lg flex flex-row gap-x-4 bg-greyie text-black justify-between'
      }
    >
      <div>
        <p className="text-3xl">{name}</p>
        <p className="text-xl italic">{status}</p>
      </div>
    </div>
  );
}
