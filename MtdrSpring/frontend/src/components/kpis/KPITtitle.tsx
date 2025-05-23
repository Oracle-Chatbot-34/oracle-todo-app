import { useState } from 'react';
import { CircleHelp } from 'lucide-react';

type KPIObject = {
  definition: string;
  example: string;
};

type KPITitleProps = {
  title: string;
  KPIObject: KPIObject
};

export default function KPITitle({ title, KPIObject }: KPITitleProps) {
  const [isPopupOpen, setPopupOpen] = useState(false);

  return (
    <div className="flex flex-row w-full justify-between items-center text-2xl font-semibold ">
      <p>{title}</p>
      <CircleHelp
        className="w-6 text-gray-500 cursor-pointer hover:text-gray-700"
        onClick={() => setPopupOpen(true)}
      />

      {/* KPI Info Popup */}
      {isPopupOpen && (
        <div className="fixed inset-0 flex items-center justify-center w-full bg-black/70 z-20">
          <div className="bg-white p-6 rounded-lg shadow-lg max-w-md text-center flex flex-col gap-4">
            <h2 className="text-3xl font-bold mb-2">{title}</h2>
            <p className="text-gray-700">
              <strong>Definition:</strong> {KPIObject.definition || 'No definition available.'}
            </p>
            <p className="text-gray-700 mt-2">
              <strong>Example:</strong> {KPIObject.example || 'No example available.'}
            </p>
            <button
              className="mt-4 px-4 py-2 bg-gray-300 text-gray-800 rounded hover:bg-gray-600 hover:text-white"
              onClick={() => setPopupOpen(false)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}