import { MdOutlineAnalytics } from 'react-icons/md';
import { BiTask } from 'react-icons/bi';
import PieChart from '../components/PieChart';
import TasksTime from '../components/TasksTime';
import TaskDashCard from '../components/TaskDashCard';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ChartArea, CheckCheck } from 'lucide-react';

export default function Dashboard() {
  const navigate = useNavigate();

  const navigateToTasks = () => {
    console.log('Navigating to /tasks');
    navigate('/tasks');
    // Your navigation logic here
  };

  const dataPie = [42, 5, 3];
  const dataLine = {
    data: [30, 40, 35, 50, 49, 60, 70, 35, 50],
    categories: [
      'Nov 24',
      'Dic 24',
      'Jan 25',
      'Feb 25',
      'Mar 25',
      'Apr 25',
      'May 25',
      'Jun 25',
    ],
  };

  const tasks = [
    {
      id: 1,
      title: 'React',
      dueDate: '12/Mar/2025',
      assignedTo: 'Benjamin Ortiz',
    },
    {
      id: 2,
      title: 'Vue',
      dueDate: '15/Mar/2025',
      assignedTo: 'Alex Johnson',
    },
    {
      id: 3,
      title: 'Angular',
      dueDate: '20/Mar/2025',
      assignedTo: 'Emma Wilson',
    },
  ];

  return (
    <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[40px]">
      <div className="bg-whitie w-[650px] h-[800px] rounded-lg shadow-xl">
        <br />
        <div className="flex flex-row items-center gap-[20px]">
          <br />
          <div className=" w-[40px] h-[40px] rounded-lg flex items-center justify-center">
            <ChartArea className="w-[30px] h-[30px]" />
          </div>
          <p className="text-[24px] font-semibold">Analytics</p>
        </div>

        <div className="flex flex-col items-center gap-[20px]">
          {/* Chart goes here */}
          <div className="shadow-xl rounded-lg">
            <PieChart data={dataPie} />
          </div>
          {/* Chart goes here */}
          <div className="shadow-xl rounded-lg">
            <TasksTime data={dataLine.data} categories={dataLine.categories} />
          </div>
        </div>
      </div>
      <div className="flex flex-col items-center gap-[20px]">
        <div className="bg-whitie w-[650px] h-[570px] rounded-lg shadow-xl">
          <br />
          <div className="flex flex-row items-center gap-[20px]">
            <br />
            <div className=" w-[40px] h-[40px] rounded-lg flex items-center justify-center">
              <CheckCheck className="w-[30px] h-[30px]" />
            </div>
            <p className="text-[24px] font-semibold">Latest tasks</p>
          </div>
          <div className="flex flex-col items-center gap-[30px]">
            {/* Task list */}
            <div>
              {tasks.map((task) => (
                <TaskDashCard
                  key={task.id}
                  title={task.title}
                  dueDate={task.dueDate}
                  assignedTo={task.assignedTo}
                />
              ))}
            </div>
            <div className="bg-greyie rounded-lg text-black text-[20px] font-semibold shadow-xl">
              <Button
                className="h-[40px] w-54 text-xl"
                size={'lg'}
                onClick={navigateToTasks}
              >
                Manage all tasks
              </Button>
            </div>
          </div>
        </div>
        <div className="bg-whitie w-[650px] h-[205px] rounded-lg shadow-xl">
          {/* Active task */}
          <div></div>
        </div>
      </div>
    </div>
  );
}
