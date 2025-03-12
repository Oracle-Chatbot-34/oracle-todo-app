import { MdOutlineAnalytics } from "react-icons/md";
import { BiTask } from "react-icons/bi";
import PieChart from "../components/PieChart";
import TasksTime from "../components/TasksTime";
import TaskDashCard from "../components/TaskDashCard";
import { useNavigate } from "react-router-dom";


export default function Dashboard() {
    const navigate = useNavigate();

    const navigateToTasks = () => {
        console.log("Navigating to /tasks");
        navigate('/tasks');
        // Your navigation logic here
    };

    const dataPie = [42, 5, 3];
    const dataLine = {
        data: [30, 40, 35, 50, 49, 60, 70, 35, 50],
        categories: ["Nov 24", "Dic 24", "Jan 25", "Feb 25", "Mar 25", "Apr 25", "May 25", "Jun 25"],
    };

    const tasks = [
        {
          id: 1,
          title: "React",
          dueDate: "12/Mar/2025",
          assignedTo: "Benjamin Ortiz"
        },
        {
          id: 2,
          title: "Vue",
          dueDate: "15/Mar/2025",
          assignedTo: "Alex Johnson"
        },
        {
          id: 3,
          title: "Angular",
          dueDate: "20/Mar/2025",
          assignedTo: "Emma Wilson"
        }
      ];
      

    return(
        <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[40px]">
            <div className="bg-whitie w-[650px] h-[800px] rounded-lg shadow-xl">
                <br/>
                <div className="flex flex-row items-center gap-[20px]">
                    <br/>
                    <div className="bg-greyie w-[40px] h-[40px] rounded-lg flex items-center justify-center"> 
                        <MdOutlineAnalytics className="w-[30px] h-[30px]"/>
                    </div>
                    <p className="text-[24px] font-semibold">Analytics</p>
                </div>

                <div className="flex flex-col items-center gap-[20px]">
                    {/* Chart goes here */}
                    <div className="shadow-xl rounded-lg">
                        <PieChart data={dataPie}/>

                    </div>
                    {/* Chart goes here */}
                    <div className="shadow-xl rounded-lg"> 
                        <TasksTime data={dataLine.data} categories={dataLine.categories}/>

                    </div>
                    {/* Active task */}
                    <div>

                    </div>

                </div>
                
            </div>
            <div className="flex flex-col items-center gap-[30px]">
                <div className="bg-whitie w-[650px] h-[600px] rounded-lg shadow-xl">
                <br/>
                    <div className="flex flex-row items-center gap-[20px]">
                        <br/>
                        <div className="bg-greyie w-[40px] h-[40px] rounded-lg flex items-center justify-center"> 
                            <BiTask className="w-[30px] h-[30px]"/>
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
                            <button 
                            type="button"
                            className="h-[40px] w-[320px] "
                            onClick={navigateToTasks}
                            >
                                Manage all tasks
                            </button>
                        </div>
                    </div>
                </div>
                <div className="bg-whitie w-[650px] h-[170px] rounded-lg shadow-xl">

                </div>
            </div>
        </div>  
    )
}
