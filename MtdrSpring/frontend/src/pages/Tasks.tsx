import { useState, useEffect } from 'react';
import { MdOutlineAnalytics, MdFilterList } from "react-icons/md";
import { BiTask } from "react-icons/bi";
import { FiPlus, FiEdit, FiTrash2 } from "react-icons/fi";
import TaskDashCard from "../components/TaskCard";

export interface Task{
    id: number;
    title: string;
    dueDate: string;
    assignedTo: string;
    description: string;
    priority: string;
    status: string;
}

// Main TaskManager component
export default function TaskManager() {
    const taskExamples= [ {
        id: 1,
        title: "NOTHING AI",
        description: "Develop Nothing AI neural network, please make okay",
        createdAt: "8/Mar/2025",
        dueDate: "15/Mar/2025",
        assignedTo: "Liam Carrasco",
        priority: "High",
        status: "In Progress"
    },
        {
            id: 2,
            title: "React Components",
            description: "Create reusable card components for the dashboard",
            createdAt: "10/Mar/2025",
            dueDate: "18/Mar/2025",
            assignedTo: "Benjamin Ortiz",
            priority: "Medium",
            status: "To Do"
        },
        {
            id: 3,
            title: "API Integration",
            description: "Connect frontend with backend REST API endpoints",
            createdAt: "5/Mar/2025",
            dueDate: "20/Mar/2025",
            assignedTo: "Alex Johnson",
            priority: "High",
            status: "Not Started"
        }]
    const [tasks, setTasks] = useState<Task[]>([]);
    useEffect(() => {
        setTasks(taskExamples);
    }, []);

    const [selectedTask, setSelectedTask] = useState(tasks[0]);
    const [filterVisible, setFilterVisible] = useState(true);

    // Task stats for the summary cards
    const taskStats = {
        totalActiveTasks: 50,
        tasksOnTime: 40,
        tasksAtRisk: 7,
        tasksPastDue: 3
    };

    return (
        <div className="bg-background min-h-screen w-full p-6 place-items-center">
            <div className="max-w-7xl h-[90%] w-[90%] mx-auto bg-gray-50 rounded-lg ">
                {/* Added Filter Bar */}
                <div className="bg-white rounded-lg shadow-lg p-4 mb-6">
                    <div className="flex items-center mb-6">
                        <div className="bg-gray-200 p-2 rounded-lg mr-3">
                            <BiTask className="w-6 h-6 text-gray-700" />
                        </div>
                        <h1 className="text-xl font-semibold">Task Manager</h1>
                    </div>

                    <div className="flex flex-wrap gap-4 items-center">
                        <div className="text-sm text-gray-500">Order by:</div>

                        <SelectCard
                            options={["Due date", "Created at", "Priority"]}
                            defaultValue="Due date"
                        />

                        <SelectCard
                            options={["Priority", "High", "Medium", "Low"]}
                            defaultValue="Priority"
                        />

                        <SelectCard
                            options={["Assignee", "Liam Carrasco", "Benjamin Ortiz", "Alex Johnson"]}
                            defaultValue="Assignee"
                        />

                        <div className="flex items-center ml-auto">
                            <input type="checkbox" id="showPastTasks" className="rounded text-blue-500" />
                            <label htmlFor="showPastTasks" className="ml-2 text-sm text-gray-700">Show past tasks</label>
                        </div>
                    </div>
                </div>

                <div className=" flex flex-col  md:flex-row gap-6 ">
                    {/* Main task area */}
                    <div className="w-[70%] flex flex-col justify-center items-center  w-[700px]">
                        {tasks.map((task) => (
                            <TaskDashCard
                                id={task.id}
                                title={task.title}
                                dueDate={task.dueDate}
                                assignedTo={task.assignedTo}
                                key={task.id}

                            />
                        ))}
                    </div>

                    <div className="w-[30%] flex flex-col gap-4  rounded-lg mt-9">
                        <Card>
                            <button className=" px-4 py-2 flex items-center justify-center w-full p-2">
                            Create new task
                            </button>
                        </Card>

                        <StatsCardsContainer stats={taskStats} />
                    </div>

                </div>
            </div>
        </div>
    );
}

// Main Task Manager Card
function TaskManagerCard({ tasks, selectedTask, setSelectedTask, filterVisible, setFilterVisible }) {
    return (
        <Card>
            <div className="flex justify-between items-center mb-6">
                <div className="flex items-center gap-3">
                    <div className="bg-gray-100 w-10 h-10 rounded-lg flex items-center justify-center">
                        <BiTask className="w-6 h-6"/>
                    </div>
                    <h1 className="text-2xl font-semibold">Task Manager</h1>
                </div>

                {/* Removed the Create new task button from here */}
            </div>

            {filterVisible && <FiltersCard />}

            <div className="mt-6">
                <TaskDetailsCard task={selectedTask} />
            </div>

            <div className="mt-6">
                <TaskListCard tasks={tasks} selectedTask={selectedTask} setSelectedTask={setSelectedTask} />
            </div>
        </Card>
    );
}

// Task filtering options card
function FiltersCard() {
    return (
        <Card className="bg-gray-50 p-4">
            <div className="flex flex-wrap gap-4 items-center">
                <div className="text-sm text-gray-500">Order by:</div>

                <SelectCard
                    options={["Due date", "Created at", "Priority"]}
                    defaultValue="Due date  "
                />

                <SelectCard
                    options={["Priority", "High", "Medium", "Low"]}
                    defaultValue="Priority  "
                />

                <SelectCard
                    options={["Assignee", "Liam Carrasco", "Benjamin Ortiz", "Alex Johnson"]}
                    defaultValue="Assignee  "
                />

                <div className="flex items-center ml-auto">
                    <input type="checkbox" id="showPastTasks" className="rounded text-blue-500" />
                    <label htmlFor="showPastTasks" className="ml-2 text-sm text-gray-700">Show past tasks</label>
                </div>
            </div>
        </Card>
    );
}

// Reusable select component
function SelectCard({ options, defaultValue }) {
    return (
        <div className="relative">
            <select className="appearance-none bg-white border border-gray-300 rounded-md py-2 pl-3 pr-10 text-sm">
                {options.map((option, index) => (
                    <option key={index} selected={option === defaultValue}>{option}</option>
                ))}
            </select>
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-700">
                <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"></path>
                </svg>
            </div>
        </div>
    );
}

// Detailed view of selected task
function TaskDetailsCard({ task }) {
    if (!task) return null;

    return (
        <Card className="bg-gray-50">
            <div className="flex justify-between">
                <div>
                    <h2 className="text-xl font-semibold">{task.title}</h2>
                    <div className="text-sm text-gray-500 mt-1">Created at: {task.createdAt}</div>
                    <div className="text-sm text-gray-500">Due date: {task.dueDate}</div>
                </div>

                <div>
                    <div className="text-sm text-gray-600">Assigned to: <span className="font-medium">{task.assignedTo}</span></div>
                    <div className="flex gap-2 mt-2">
                        <div className="flex gap-2 mt-2 justify-end">
                        <button className="px-3 py-1 bg-gray-200 text-gray-700 rounded flex items-center gap-1">
                            <FiEdit size={14} /> Edit
                        </button>
                        <button className="px-3 py-1 bg-red-100 text-red-600 rounded flex items-center gap-1">
                            <FiTrash2 size={14} /> Delete
                        </button>
                        </div>
                    </div>
                </div>
            </div>

            <div className="mt-4">
                <div className="text-sm text-gray-600">Description:</div>
                <p className="text-gray-700 mt-1">{task.description}</p>
            </div>
        </Card>
    );
}

// Task list component (was missing in original code)
function TaskListCard({ tasks, selectedTask, setSelectedTask }) {
    return (
        <div className="space-y-2">
            {tasks.map((task) => (
                <TaskItemCard
                    key={task.id}
                    task={task}
                    isSelected={selectedTask && selectedTask.id === task.id}
                    onClick={() => setSelectedTask(task)}
                />
            ))}
        </div>
    );
}

// Individual task item card
function TaskItemCard({ task, isSelected, onClick }) {
    return (
        <Card
            className={`cursor-pointer hover:bg-gray-50 ${isSelected ? 'border-blue-400 bg-blue-50' : 'border-gray-200'}`}
            onClick={onClick}
        >
            <div className="flex justify-between">
                <div>
                    <h3 className="font-medium">{task.title}</h3>
                    <div className="text-sm text-gray-500 mt-1">Due: {task.dueDate}</div>
                </div>
                <div className="text-sm text-gray-600">
                    {task.assignedTo}
                </div>
            </div>
        </Card>
    );
}

// Container for all stats cards
function StatsCardsContainer({ stats }) {
    const { totalActiveTasks, tasksOnTime, tasksAtRisk, tasksPastDue } = stats;

    return (
        <div className="mt-6 space-y-4">
            <TotalTasksCard count={totalActiveTasks} />
            <TaskStatusBreakdownCard className="mr-6 bg-yellow-500"
                onTime={tasksOnTime}
                atRisk={tasksAtRisk}
                pastDue={tasksPastDue}
            />
        </div>
    );
}

// Card showing total tasks count
function TotalTasksCard({ count }) {
    return (
        <Card>
            <div className="text-center p-6">
                <div className="text-gray-500 text-sm">There are</div>
                <div className="text-6xl font-bold text-gray-800 my-2">{count}</div>
                <div className="text-gray-500 text-sm">active tasks</div>
            </div>
        </Card>
    );
}

// Card showing status breakdown
function TaskStatusBreakdownCard({ onTime, atRisk, pastDue }) {
    return (
        <Card>
            <div className="space-y-4 p-6">
                <StatusItemCard
                    label={`${onTime} tasks are on time`}
                    color="bg-green-500"
                />

                <StatusItemCard
                    label={`${atRisk} tasks may not be ready on time`}
                    color="bg-yellow-500"
                />

                <StatusItemCard
                    label={`${pastDue} tasks are beyond the deadline`}
                    color="bg-red-500"
                />
            </div>
        </Card>
    );
}


function StatusItemCard({ label, color }) {
    return (
        <div className="flex justify-between items-center">
            <div className="text-gray-600">{label}</div>
            <div className={`w-2 h-2 rounded-full ${color}`}></div>
        </div>
    );
}


function Card({ children, className = "" }) {
    return (
        <div className={`bg-white rounded-lg shadow-lg p-6 ${className}`}>
            {children}
        </div>
    );
}