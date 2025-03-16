# DashMaster - Task Management System

DashMaster is a comprehensive task management application designed to help teams organize their work, track progress, and visualize performance metrics. The application integrates sprint planning, team management, key performance indicators, and reporting capabilities to provide a complete solution for modern agile teams.

## Features

### User Authentication and Account Management

- Secure JWT-based authentication system
- User registration with role assignment
- Role-based access control with Manager, Developer, and Employee roles
- Session management and token-based authentication

### Task Management

- Create, read, update, and delete tasks
- Task status tracking across the development lifecycle
- Task prioritization and deadline management
- Task assignment to team members
- Estimation tracking for better project planning
- Actual vs. estimated hours comparison

### Sprint Planning

- Sprint creation and management
- Sprint backlog management
- Sprint board with task visualization
- Sprint status tracking (Planning, Active, Completed)
- Team assignment for sprints

### Team Management Best Practices

- Team creation and organization
- Team member assignment
- Team performance tracking
- Manager designation for each team
- Cross-team collaboration support

### Performance Analytics (KPIs)

- Task completion rate tracking
- On-time completion rate measurement
- Resource utilization monitoring
- Real-time hours worked vs. planned
- Average tasks per employee calculation
- Trend analysis for team and individual performance

### Reporting

- Custom report generation based on various parameters
- Team and individual performance reports
- Time period filtering for trend analysis
- Status-based filtering for targeted insights
- Exportable report formats

### Telegram Bot Integration

- Task management via Telegram- Secure JWT-based authentication system
- Notifications for important events
- Task status updates through messaging
- Quick task creation through chat interface

## Technology Stack

### Backend

- **Language**: Java 17
- **Framework**: Spring Boot 3.1.5
- **Security**: Spring Security with JWT
- **Database Access**: Spring Data JPA
- **Database**: Oracle Database with autonomous capabilities
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Testing**: Spring Boot Test framework

### Frontend

- **Library**: React 19 with TypeScript
- **Styling**: Tailwind CSS with custom theming
- **State Management**: React Query for server state
- **Routing**: React Router for navigation
- **Form Handling**: React Hook Form with Zod validation
- **UI Components**: Custom components with Radix UI primitives
- **Data Visualization**: ApexCharts and Recharts
- **HTTP Client**: Axios for API communication

## Architecture

DashMaster follows a modern client-server architecture with a clear separation of concerns:

### Backend Architecture

The Spring Boot backend is structured using a layered architecture:

1. **Controller Layer**: Handles HTTP requests and responses
2. **Service Layer**: Contains business logic and process management
3. **Repository Layer**: Manages data access and persistence
4. **Model Layer**: Defines the data structures and relationships
5. **Security Layer**: Manages authentication and authorization
6. **Utility Layer**: Provides common functionality across the application

### Frontend Architecture

The React frontend is organized using a feature-based structure:

1. **Pages**: Major application views like Dashboard, Tasks, Reports, etc.
2. **Components**: Reusable UI elements shared across pages
3. **Services**: API client functionality for backend communication
4. **Contexts**: State management for application-wide state
5. **Hooks**: Custom logic for component behavior and data fetching
6. **Types**: TypeScript type definitions for type safety

### Data Flow

1. User interacts with the frontend React application
2. React components use service modules to make API requests
3. Spring controllers receive and process the requests
4. Service layer applies business logic and interacts with repositories
5. Data is persisted in Oracle Database
6. Responses flow back to the frontend for rendering

## API Overview

DashMaster exposes a comprehensive RESTful API for all functionality:

### Authentication Endpoints

- `POST /auth/login` - Authenticate users and receive JWT token
- `POST /auth/register` - Register new users

### Task Management Endpoints

- `GET /todolist` - Retrieve all tasks
- `GET /todolist/{id}` - Get a specific task by ID
- `POST /todolist` - Create a new task
- `PUT /todolist/{id}` - Update an existing task
- `DELETE /todolist/{id}` - Delete a task
- `POST /tasks/{id}/assign-to-sprint/{sprintId}` - Assign task to sprint
- `POST /tasks/{id}/start?userId={userId}` - Start working on a task
- `POST /tasks/{id}/complete` - Complete a task

### Team Management Endpoints

- `GET /api/teams` - List all teams
- `GET /api/teams/{id}` - Get team details
- `POST /api/teams` - Create a new team
- `PUT /api/teams/{id}` - Update team information
- `DELETE /api/teams/{id}` - Delete a team
- `GET /api/teams/{id}/members` - Get team members
- `POST /api/teams/{teamId}/members/{userId}` - Add member to team
- `DELETE /api/teams/{teamId}/members/{userId}` - Remove member from team
- `POST /api/teams/{teamId}/manager/{userId}` - Assign manager to team

### Sprint Management Endpoints

- `GET /api/sprints` - List all sprints
- `GET /api/sprints/{id}` - Get sprint details
- `POST /api/sprints` - Create a new sprint
- `PUT /api/sprints/{id}` - Update sprint information
- `DELETE /api/sprints/{id}` - Delete a sprint
- `POST /api/sprints/{id}/start` - Start a sprint
- `POST /api/sprints/{id}/complete` - Complete a sprint
- `GET /api/sprints/{id}/board` - Get sprint board (tasks)
- `GET /api/teams/{teamId}/sprints` - Get sprints for a team
- `GET /api/teams/{teamId}/active-sprint` - Get active sprint for a team

### KPI and Reporting Endpoints

- `GET /api/kpi/users/{userId}` - Get KPIs for a user
- `GET /api/kpi/teams/{teamId}` - Get KPIs for a team
- `POST /api/reports/generate` - Generate custom report

### User Management Endpoints

- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user details
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update user information
- `DELETE /api/users/{id}` - Delete a user
- `GET /api/users/roles/{role}` - Get users by role
- `PUT /api/users/{id}/telegram/{telegramId}` - Update a user's Telegram ID

## Development Setup

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Node.js 20.x or higher
- npm or bun package manager
- Oracle Database instance or connection details
- Git

### Backend Setup

1. Clone the repository:

   ```bash
   clone repository 
   cd MtdrSpring
   ```

2. Configure database connection in `backend/src/main/resources/application.properties` or via environment variables:

   ```properties
   spring.datasource.url=jdbc:oracle:thin:@your_connection_string
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
   ```

3. Optional: Configure JWT settings for authentication:

   ```properties
   jwt.secret=your_jwt_secret_key
   jwt.expiration=86400000
   ```

4. Optional: Configure Telegram bot integration:

   ```properties
   telegram.bot.token=your_telegram_bot_token
   telegram.bot.name=your_bot_name
   ```

5. Run the backend application:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

### Frontend Setup

1. Navigate to the frontend directory:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Configure API URL in `.env.development`:

   ```bash
   VITE_API_BASE_URL=http://localhost:8080
   VITE_AUTH_ENDPOINT=/auth
   VITE_API_ENDPOINT=/api
   ```

4. Start the development server:

   ```bash
   bun run dev
   ```

5. Access the application at `http://localhost:5173`

## User Guide

### Getting Started

1. **Registration**: Create an account via the registration page
2. **Login**: Use your credentials to log in to the system
3. **Dashboard**: View key metrics and recent tasks on the dashboard

### Task Management Best Practices

1. **Create Task**: Use the "+ Create Task" button on the Tasks page
2. **View Tasks**: Browse tasks on the Tasks page with filtering options
3. **Update Task**: Click "Edit" on any task to modify its details
4. **Delete Task**: Use the "Delete" button to remove a task
5. **Task Assignment**: Assign tasks to team members during creation or update

### Sprint Management

1. **Create Sprint**: Navigate to Sprints and create a new sprint
2. **Sprint Planning**: Add tasks to the sprint backlog
3. **Start Sprint**: Activate a sprint to begin work
4. **Track Progress**: Use the sprint board to see task status
5. **Complete Sprint**: Mark a sprint as completed when all work is done

### Team Management

1. **Create Team**: Set up a new team with a designated manager
2. **Add Members**: Add team members from the user pool
3. **Assign Roles**: Define roles for team members (Developer, Employee)
4. **Team Performance**: View team KPIs and performance metrics

### KPI Dashboards

1. **Individual Performance**: View personal KPIs on the KPI page
2. **Team Performance**: Switch to team view for collective metrics
3. **Date Ranges**: Adjust time periods for trend analysis
4. **Specific Metrics**: Focus on particular KPIs for detailed insights

### Reports

1. **Generate Report**: Use the Reports page to create custom reports
2. **Set Parameters**: Choose scope, status, and time period
3. **View Report**: Analyze the generated report with visualizations
4. **Apply Insights**: Use report findings to optimize workflows

## Best Practices

### For Task Management

- Break down larger tasks into smaller, manageable units
- Maintain consistent estimation practices (4 hours maximum per task)
- Use the status workflow consistently for accurate reporting
- Include detailed descriptions for better team understanding
- Set realistic deadlines to maintain on-time completion metrics

### For Sprint Planning

- Limit sprint scope to achievable goals
- Include buffer time for unexpected challenges
- Assign tasks to team members with appropriate skills
- Review sprint capacity before committing to work
- Conduct thorough sprint reviews for continuous improvement

### KPI Monitoring

- Review KPIs regularly to identify trends
- Address performance outliers promptly
- Use comparative analysis between teams for benchmarking
- Focus on improving problem areas identified in metrics
- Celebrate improvements and achievements

## Extending DashMaster

The modular architecture of DashMaster makes it easily extensible:

### Adding New Features

1. Define new models in the backend
2. Create corresponding repositories and service methods
3. Expose functionality through controller endpoints
4. Implement frontend services to communicate with new endpoints
5. Create UI components to interact with the new functionality

### Customizing Workflows

1. Modify task status enums to match your team's workflow
2. Adjust service methods to handle custom business logic
3. Update frontend components to reflect workflow changes

### Integration with Other Systems

1. Implement new service methods for external API communication
2. Create gateway classes for external system integration
3. Configure authentication for third-party services
4. Add UI components for integrated functionality

## Troubleshooting

### Common Issues

#### Authentication Problems

- Ensure JWT token is properly configured
- Check that credentials are correct
- Verify token expiration settings

#### Database Connection Issues

- Confirm database credentials and connection string
- Check network connectivity to database server
- Verify Oracle wallet configuration (if applicable)

#### API Communication Errors

- Check CORS configuration in the backend
- Verify API endpoint URLs in frontend services
- Inspect network requests in browser developer tools

#### Performance Concerns

- Monitor database query performance
- Check for N+1 query problems in JPA repositories
- Optimize frontend rendering with proper React patterns

## Getting Help

If you encounter issues or have questions about DashMaster:

- Review documentation thoroughly
- Check issue tracking system for known problems
- Reach out to the development team for assistance

## License

DashMaster is proprietary software. All rights reserved.
