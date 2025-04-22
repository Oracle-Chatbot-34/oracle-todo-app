import { useState, useEffect } from 'react';
import { CalendarClock } from 'lucide-react';
import { Sprint } from '@/services/sprintService';

import { dummySprints } from '@/components/sprints/sprintdummy';

import SprintInfo from '@/components/sprints/SprintInfo';

import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from '@/components/ui/carousel';

import { type CarouselApi } from '@/components/ui/carousel';

export default function Sprints() {
  const [api, setApi] = useState<CarouselApi>();

  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [isExpanded, setIsExpanded] = useState(false);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [count, setCount] = useState(0);

  const [expandedId, setExpandedId] = useState<number | null>(null);

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

  const handleExpansion = () =>{
    setIsExpanded(true)
    setExpandedId(currentIndex-1)


  }

  useEffect(() => {
    const fetchSprints = async () => {
      setSprints(dummySprints);
    };

    fetchSprints();

    if (!api) {
      return;
    }

    setCount(api.scrollSnapList().length);
    setCurrentIndex(api.selectedScrollSnap() + 1);

    api.on('select', () => {
      setCurrentIndex(api.selectedScrollSnap() + 1);
    });
  }, [api]);

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-center p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        {/* Title */}
        <div className="flex flex-row items-center gap-[10px]">
          <CalendarClock className="w-8 h-8" />
          <p className="text-[24px] font-semibold">Sprints Management</p>
        </div>

        {isExpanded ? (
          <SprintInfo
            sprint={expandedId !== null ? sprints[expandedId] : undefined}
            setExpandedId={setExpandedId}
            setIsExpanded={setIsExpanded}
          />
        ) : (
          <div className="flex flex-col gap-x-40 w-full h-full justify-center items-center gap-6">
            <Carousel className="flex justify-center" setApi={setApi}>
              <CarouselContent>
                {sprints.map((sprint) => (
                  <CarouselItem key={sprint.id} className="flex justify-center">
                    {sprint.name}
                  </CarouselItem>
                ))}
              </CarouselContent>
              <CarouselPrevious />
              <CarouselNext />
            </Carousel>

            <div>{currentIndex}/{count}</div>
            
            <button
              className="bg-greenie rounded-lg flex flex-row justify-center items-center h-12 w-110 shadow-lg"
              onClick={handleExpansion}
            >
              <p className="text-white text-2xl">More information</p>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
