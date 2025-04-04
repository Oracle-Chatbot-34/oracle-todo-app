import { Routes, Route, useLocation } from 'react-router-dom';
import NavBar from './components/NavBar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import Tasks from './pages/Tasks';
import KPI from './pages/KPI';
import NotFound from './pages/NotFound';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

const App = () => {
  const location = useLocation();
  const hideNavRoutes = ['/login'];

  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex flex-col w-screen max-h-screen h-screen overflow-clip">
        {!hideNavRoutes.includes(location.pathname) && <NavBar />}
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/login" element={<Login />} />
          <Route path="/reports" element={<Reports />} />
          <Route path="/tasks" element={<Tasks />} />
          <Route path="/kpis" element={<KPI />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
    </QueryClientProvider>
  );
};

export default App;
