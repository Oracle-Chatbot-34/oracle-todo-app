import { useState, useEffect } from 'react';
import { FileText, FileSearch } from 'lucide-react';
import { Button } from '@/components/ui/button';
import StatusSelections from '@/components/StatusSelections';
import MemberSelection from '@/components/MemberSelection';
import sprintService from '@/services/sprintService';

export type Member = {
  id: number;
  name: string;
};

const mockTasks = [
  {
    id: 1,
    title: 'Maquetar layout base y sistema de rutas',
    assignee: 'José Benjamín Ortiz Badillo',
    sprint: 'Sprint 1',
    status: 'COMPLETED',
    estimatedHours: 2.67,
    actualHours: 3.0,
    comment:
      'AI: Benjamin completó la tarea con un 12% de sobre tiempo, recomendado revisar complejidad de estimaciones en próximos sprints',
  },
  {
    id: 4,
    title: 'Crear API REST y controladores Spring Boot',
    assignee: 'Daniel Alfredo Barreras Meraz',
    sprint: 'Sprint 1',
    status: 'COMPLETED',
    estimatedHours: 4,
    actualHours: 4.5,
    comment:
      'AI: Daniel mostró buen desempeño en backend pero con sobre tiempo, considerar pair programming para próximas tareas complejas',
  },
  {
    id: 23,
    title: 'Orquestar secuencia E2E y actualizar README',
    assignee: 'Daniel Alfredo Barreras Meraz',
    sprint: 'Sprint 2',
    status: 'COMPLETED',
    estimatedHours: 4,
    actualHours: 5,
    comment:
      'AI: Tarea crítica completada con éxito, aunque con 25% más tiempo del estimado. Destacar liderazgo técnico',
  },
  {
    id: 6,
    title: 'Tabla ordenable en frontend',
    assignee: 'José Benjamín Ortiz Badillo',
    sprint: 'Sprint 3',
    status: 'COMPLETED',
    estimatedHours: 1.5,
    actualHours: 1.8,
    comment:
      'AI: Benjamin implementó feature clave con ligero sobre tiempo, buen manejo de React',
  },
  {
    id: 24,
    title: 'Desplegar Release 1.2 con Helm',
    assignee: 'Daniel Alfredo Barreras Meraz',
    sprint: 'Sprint 3',
    status: 'COMPLETED',
    estimatedHours: 3,
    actualHours: 3.6,
    comment:
      'AI: Despliegue exitoso con 20% sobre tiempo, considerar capacitación en Kubernetes para equipo',
  },
];

export default function Reports() {
  const [isIndividual, setIsIndividual] = useState(false);
  const [selectAllTasksType, setSelectAllTasksType] = useState(false);
  const [selectedTaskOptions, setSelectedTaskOptions] = useState<string[]>([]);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);
  const [reportData, setReportData] = useState<Array<any>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  interface Sprint {
    id: number | undefined;
    name: string;
  }

  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [startSprint, setStartSprint] = useState<Sprint | null>(null);
  const [endSprint, setEndSprint] = useState<Sprint | null>(null);

  useEffect(() => {
    const loadTeamsAndSprints = async () => {
      try {
        // Load sprints
        const sprintsData = await sprintService.getAllSprints();
        // Convert to Sprint type to ensure id is not optional
        const typedSprints: Sprint[] = sprintsData.map((sprint) => ({
          id: sprint.id || 0, // Provide a default value if id is undefined
          name: sprint.name,
        }));
        setSprints(typedSprints);
        if (typedSprints.length > 0) {
          setStartSprint(typedSprints[0]);
        }
      } catch (err) {
        console.error('Error loading teams and sprints:', err);
        setError('Failed to load required data');
      }
    };

    loadTeamsAndSprints();
  }, []);

  const handleReportGeneration = async () => {
    setReportData(mockTasks);
    setError('');
    setLoading(false);
  };

  return (
    <div className="bg-background h-full w-full p-6 lg:px-10 py-10 flex items-start justify-center overflow-clip">
      <div className="flex flex-col justify-between p-4 lg:p-10 gap-y-4 bg-whitie w-full h-full rounded-lg shadow-xl ">
        <div className="flex items-center gap-8">
          <div className="flex flex-row items-center gap-3">
            <FileText className="w-8 h-8" />
            <p className="text-[24px] font-semibold">Reports</p>
          </div>
          <div className="flex flex-col sm:flex-row gap-4">
            <button
              className={`px-6 py-3 rounded-xl font-bold ${
                !isIndividual
                  ? 'bg-greenie text-white'
                  : 'bg-gray-300 text-black hover:bg-gray-400'
              }`}
              onClick={() => setIsIndividual(false)}
            >
              Team Report
            </button>
            <button
              className={`px-6 py-3 rounded-xl font-bold ${
                isIndividual
                  ? 'bg-greenie text-white'
                  : 'bg-gray-300 text-black hover:bg-gray-400'
              }`}
              onClick={() => setIsIndividual(true)}
            >
              Individual Report
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <div className="flex flex-grow gap-4 pt-4">
          <div className="flex flex-col gap-6 w-2/5">
            {isIndividual && (
              <MemberSelection
                isIndividual={isIndividual}
                setIsIndividual={setIsIndividual}
                selectedMemberProp={selectedMember}
                setSelectedMemberProp={setSelectedMember}
              />
            )}

            {/* Sprint selection */}
            <div className="flex flex-col gap-4">
              <p className="text-[#747276] text-[1.5625rem]">Sprint Range</p>
              <div className="flex gap-4">
                <div className="w-1/2">
                  <p className="text-[#747276] text-lg">Start Sprint</p>
                  <select
                    className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white"
                    value={startSprint?.id || ''}
                    onChange={(e) => {
                      const sprintId = e.target.value;
                      const sprint =
                        sprints.find((s) => s.id === Number(sprintId)) || null;
                      setStartSprint(sprint);

                      // Reset end sprint if it's before start sprint
                      if (
                        endSprint &&
                        sprint &&
                        endSprint.id! < (sprint.id || 0)
                      ) {
                        setEndSprint(null);
                      }
                    }}
                  >
                    <option value="" disabled>
                      Select start sprint
                    </option>
                    {sprints.map((sprint) => (
                      <option key={sprint.id} value={sprint.id}>
                        {sprint.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="w-1/2">
                  <p className="text-[#747276] text-lg">
                    End Sprint (optional)
                  </p>
                  <select
                    className="w-full pl-4 pr-2 rounded-xl h-12 border-2 border-[#DFDFE4] transition-shadow duration-200 ease-in-out bg-white"
                    value={endSprint?.id || ''}
                    onChange={(e) => {
                      const sprintId = e.target.value;
                      if (sprintId === '') {
                        setEndSprint(null);
                      } else {
                        const sprint =
                          sprints.find((s) => s.id === Number(sprintId)) ||
                          null;
                        setEndSprint(sprint);
                      }
                    }}
                  >
                    <option value="">None</option>
                    {sprints
                      .filter((sprint) => sprint.id! > (startSprint?.id || 0))
                      .map((sprint) => (
                        <option key={sprint.id} value={sprint.id}>
                          {sprint.name}
                        </option>
                      ))}
                  </select>
                </div>
              </div>
            </div>

            <StatusSelections
              selectedTaskOptions={selectedTaskOptions}
              setselectedTaskOptions={setSelectedTaskOptions}
              selectAllTasksType={selectAllTasksType}
              setselectAllTasksType={setSelectAllTasksType}
            />

            <div className="flex flex-col w-full items-end pt-3">
              <Button
                onClick={handleReportGeneration}
                className="bg-greenie text-white px-6 py-8 rounded-md flex items-center gap-2 text-xl"
                disabled={loading}
              >
                <FileSearch className="w-6 h-6" />
                {loading ? 'Generating...' : 'Generate Report'}
              </Button>
            </div>
          </div>

          <div className="w-3/5 h-full bg-white p-4 rounded-lg shadow">
            {reportData.length > 0 ? (
              <div className="space-y-4">
                <h2 className="text-xl font-bold mb-4">
                  Reporte de Tareas - Vista Previa
                </h2>
                <div className="grid grid-cols-1 gap-4">
                  {reportData.map((task) => (
                    <div key={task.id} className="border p-4 rounded-lg">
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="font-semibold text-lg">{task.title}</h3>
                        <span className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-sm">
                          {task.sprint}
                        </span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-sm">
                        <div>
                          <span className="font-medium">Asignado:</span>{' '}
                          {task.assignee}
                        </div>
                        <div>
                          <span className="font-medium">Estado:</span>
                          <span className="ml-2 px-2 py-1 rounded bg-green-100 text-green-800">
                            {task.status}
                          </span>
                        </div>
                        <div>
                          <span className="font-medium">Horas estimadas:</span>{' '}
                          {task.estimatedHours}
                        </div>
                        <div>
                          <span className="font-medium">Horas reales:</span>{' '}
                          {task.actualHours}
                        </div>
                      </div>
                      {task.comment && (
                        <div className="mt-3 p-2 bg-yellow-50 border-l-4 border-yellow-400">
                          <p className="text-sm text-yellow-700">
                            {task.comment}
                          </p>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600">
                    * Informe generado con datos de demostración - Integración
                    con IA en desarrollo
                  </p>
                </div>
              </div>
            ) : (
              <div className="h-full flex items-center justify-center text-gray-500">
                Seleccione opciones y genere el reporte para ver la vista previa
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
