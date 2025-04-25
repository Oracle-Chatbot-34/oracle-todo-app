import { useState, useEffect } from 'react';
import { CalendarClock, Plus } from 'lucide-react';
import { Sprint } from '@/services/sprintService';

import sprintService from '@/services/sprintService';
import LoadingSpinner from '@/components/LoadingSpinner';
import { Button } from '@/components/ui/button';

import SprintInfo from '@/components/sprints/SprintInfo';
import SprintCreateModal from '@/components/sprints/SprintCreateModal';

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
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [count, setCount] = useState(0);

  const [expandedId, setExpandedId] = useState<number | null>(null);

  const fetchSprints = async () => {
    try {
      setIsLoading(true);
      const sprintsData = await sprintService.getAllSprints();
      setSprints(sprintsData);
    } catch (err) {
      console.error('Error fetching sprints:', err);
      setError('Failed to load sprints. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSprints();
  }, []);

  const handleSprintCreated = (newSprint: Sprint) => {
    setSprints((prevSprints) => [...prevSprints, newSprint]);
  };

  const handleExpansion = () => {
    if (currentIndex > 0 && sprints.length > 0) {
      setIsExpanded(true);
      setExpandedId(currentIndex - 1);
    }
  };

  useEffect(() => {
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
        {/* Title and buttons */}
        <div className="flex flex-row items-center justify-between">
          <div className="flex flex-row items-center gap-[10px]">
            <CalendarClock className="w-8 h-8" />
            <p className="text-[24px] font-semibold">Sprints Management</p>
          </div>

          <Button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2"
          >
            <Plus size={18} /> New Sprint
          </Button>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="flex justify-center items-center h-64">
            <LoadingSpinner size={8} />
          </div>
        ) : isExpanded ? (
          <SprintInfo
            sprint={
              expandedId !== null && expandedId < sprints.length
                ? sprints[expandedId]
                : sprints[0]
            }
            setExpandedId={setExpandedId}
            setIsExpanded={setIsExpanded}
          />
        ) : (
          <div className="flex flex-col gap-x-40 w-full h-full justify-center items-center gap-6">
            {sprints.length > 0 ? (
              <>
                <Carousel className="flex justify-center" setApi={setApi}>
                  <CarouselContent>
                    {sprints.map((sprint) => (
                      <CarouselItem
                        key={sprint.id}
                        className="flex justify-center"
                      >
                        {sprint.name}
                      </CarouselItem>
                    ))}
                  </CarouselContent>
                  <CarouselPrevious />
                  <CarouselNext />
                </Carousel>

                <div>
                  {currentIndex}/{count}
                </div>

                <button
                  className="bg-greenie rounded-lg flex flex-row justify-center items-center h-12 w-110 shadow-lg"
                  onClick={handleExpansion}
                >
                  <p className="text-white text-2xl">More information</p>
                </button>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center h-64">
                <p className="text-2xl text-gray-500 mb-4">No sprints found</p>
                <Button
                  onClick={() => setShowCreateModal(true)}
                  className="flex items-center gap-2"
                >
                  <Plus size={18} /> Create your first sprint
                </Button>
              </div>
            )}
          </div>
        )}

        {/* Create Sprint Modal */}
        <SprintCreateModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          onSprintCreated={handleSprintCreated}
        />
      </div>
    </div>
  );
}
