import { Routes, Route, useLocation } from 'react-router-dom';
import NavBar from './components/NavBar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import Tasks from './pages/Tasks';
import NotFound from './pages/NotFound';

const App = () => {
  const location = useLocation();
  const hideNavRoutes = ['/login'];


  return (
    <div className="flex flex-col w-screen max-h-screen h-screen overflow-clip">
      {!hideNavRoutes.includes(location.pathname) && <NavBar />}
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/login" element={<Login />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/tasks" element={<Tasks />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </div>
  );
};

export default App;
