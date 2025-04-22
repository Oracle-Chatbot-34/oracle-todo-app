import { Team } from '@/services/teamService';
import {User} from '@/services/userService';


export const dummyTeams: Team[] = [
  {
    id: 1,
    name: 'Alpha Team',
    description: 'Frontend development team',
    managerId: 1,
    createdAt: '2023-01-15T09:30:00Z',
  },
  {
    id: 2,
    name: 'Bravo Team',
    description: 'Backend services team',
    managerId: 1,
    createdAt: '2023-02-20T14:15:00Z',
  },
  {
    id: 3,
    name: 'Gamma Team',
    description: 'Quality assurance specialists',
    managerId: 1,
    createdAt: '2023-03-10T11:00:00Z',
  },
  {
    id: 4,
    name: 'Delta Team',
    managerId: 1,
    createdAt: '2023-04-05T16:45:00Z',
  },
  {
    id: 5,
    name: 'Epsilon Team',
    description: 'DevOps and infrastructure',
    managerId: 1,
    createdAt: '2023-05-12T10:20:00Z',
  },
  {
    id: 6,
    name: 'Omega Team',
    description: 'Cross-functional product team',
    managerId: 1,
    createdAt: '2023-06-18T13:10:00Z',
  },
];

export const dummyUsers: User[] = [
  {
    id: 1,
    username: "john.doe",
    password: "secure123",
    fullName: "John Doe",
    role: "Manager",
    employeeId: "EMP-1001",
    telegramId: 123456789,
    createdAt: "2023-01-10T08:00:00Z",
    updatedAt: "2023-06-01T14:30:00Z"
  },
  {
    id: 2,
    username: "jane.smith",
    fullName: "Jane Smith",
    role: "Developer",
    employeeId: "EMP-1002",
    telegramId: 987654321,
    createdAt: "2023-01-15T09:15:00Z"
  },
  {
    id: 3,
    username: "mike.johnson",
    fullName: "Mike Johnson",
    role: "QA Engineer",
    employeeId: "EMP-1003",
    createdAt: "2023-02-01T10:00:00Z"
  },
  {
    id: 4,
    username: "sarah.williams",
    fullName: "Sarah Williams",
    role: "UX Designer",
    employeeId: "EMP-1004",
    telegramId: 555666777,
    createdAt: "2023-02-10T11:20:00Z",
    updatedAt: "2023-05-15T16:45:00Z"
  },
  {
    id: 5,
    username: "david.brown",
    fullName: "David Brown",
    role: "DevOps",
    createdAt: "2023-03-05T13:10:00Z"
  },
  {
    id: 6,
    username: "lisa.taylor",
    fullName: "Lisa Taylor",
    role: "Product Owner",
    employeeId: "EMP-1006",
    createdAt: "2023-03-20T14:00:00Z",
    updatedAt: "2023-06-10T10:15:00Z"
  }
];
