import { useState, useEffect } from 'react';
import { CalendarClock } from 'lucide-react';
import { Sprint } from '@/services/sprintService';
import sprintService from '@/services/sprintService';
import { dummySprints } from '@/components/sprints/sprintdummy';

import SprintArrow from '@/components/sprints/SprintArrow';

export default function Sprints() {
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [isExpanded, setIsExpanded] = useState(false);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const handlePrev = () => {
    setCurrentIndex((prev) => Math.max(prev - 1, 0))
  };
  const handleNext = () =>{
    setCurrentIndex((prev) => Math.min(prev + 1, sprints.length - 1))
  
  };

  const handleExpand = (id: any) => {
    setExpandedId(expandedId === id ? null : id);

  };

  const currentSprint = sprints[currentIndex];

  /*
  useEffect(() => {
    const fetchSprints = async () => {
      try {
        const sprints = await sprintService.getAllSprints();
        setSprints(sprints);
      } catch (error) {
        console.error('Failed to fetch tasks:', error);
        // Optionally handle the error in your UI
      }
    };

    fetchSprints();
  }, []);
  
  */

  useEffect(() => {
    const fetchSprints = async () => {
      setSprints(dummySprints);
    };
    fetchSprints();
  }, []);

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-[10px]">
          <CalendarClock className="w-8 h-8" />
          <p className="text-[24px] font-semibold">Sprints Management</p>
        </div>

        <div className="flex justify-between w-full px-10">
          <button onClick={handlePrev} disabled={currentIndex === 0}>
            ←
          </button>
          <button
            onClick={handleNext}
            disabled={currentIndex === sprints.length - 1}
          >
            →
          </button>
        </div>

        <div className="flex flex-row gap-x-40 w-full h-full">
          {sprints.map((sprint) => (
            <SprintArrow sprint={sprint} isExpanded={isExpanded}
            onToggle={() => handleExpand(currentSprint.id)}
            />
            
          ))}
        </div>
      </div>
    </div>
  );
}
