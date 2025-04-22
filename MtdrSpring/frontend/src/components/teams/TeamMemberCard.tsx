import { useState } from 'react';

type TeamMemberCardProps = {
  id: number;
  name: string;
  role: string;
};

export default function TeamMemberCard({
  id,
  name,
  role,
}: TeamMemberCardProps) {
  const [isPopupOpen, setIsPopupOpen] = useState(false);

  return (
    <div
      className={
        'p-5 rounded-lg flex flex-row gap-x-4 bg-greyie text-black justify-between items-center'
      }
      key={id}
    >
      {isPopupOpen && (
        <div className="fixed inset-0 flex items-center justify-center w-full bg-black/70 z-20">
          <div className="bg-white p-6 rounded-lg shadow-lg max-w-md">
            <p className="text-gray-700">
              Are you sure you want to remove this person from the team?
              You can read them again if you change your mind.
            </p>
            <div className="flex flex-row justify-between">
              <button
                className="mt-4 px-4 py-2 bg-redie text-white rounded hover:bg-red-700"
                onClick={() => setIsPopupOpen(false)}
              >
                Remove
              </button>
              <button
                className="mt-4 px-4 py-2 bg-black/50 text-white rounded hover:bg-black/40"
                onClick={() => setIsPopupOpen(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
      <div>
        <p className="text-3xl">{name}</p>

        <p className="text-xl italic">{role}</p>
      </div>

      <button
        className="bg-redie rounded-lg flex flex-row justify-center items-center h-12 w-35 shadow-lg"
        onClick={() => setIsPopupOpen(true)}
      >
        <span className="text-2xl text-white">Remove</span>
      </button>
    </div>
  );
}
