import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import logo from '../assets/logo.png';
import { useAuth } from '@/hooks/useAuth';

const Navbar = () => {
  const location = useLocation();

  const pathSegment = location.pathname.split('/')[1];
  const locationText = pathSegment
    ? pathSegment.charAt(0).toUpperCase() + pathSegment.slice(1)
    : 'Home';

  const [active, setActive] = useState(locationText);
  const { isAuthenticated, fullName, logout } = useAuth();

  useEffect(() => {
    if (locationText !== active) {
      setActive(locationText);
    }
  }, [locationText, active]);

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <nav className="flex flex-row bg-background shrink-0 min-h-16 items-center justify-between px-10 shadow-md z-50">
      {/* Logo and Title */}
      <a href="/" className="text-4xl font-semibold ml-2 flex flex-row items-center gap-2 justify-center"> 
        <img src={logo} className="h-14 aspect-square" alt="Logo" />
        DashMaster
      </a>

      {/* Navigation Menu */}
      <div className="flex flex-row items-center justify-start">
        <ul className="flex font-light text-[24px]">
          <Link to="/">
            <li
              onClick={() => setActive('Home')}
              style={{ paddingLeft: '3rem', paddingRight: '3rem' }}
              className={`cursor-pointer relative after:absolute after:bottom-0 after:left-1/2 after:transform after:-translate-x-1/2 after:h-[2px] after:bg-black after:transition-all after:duration-100 ${
                active === 'Home'
                  ? 'after:w-[50px] font-semibold'
                  : 'after:w-0 hover:after:w-full'
              }`}
            >
              Home
            </li>
          </Link>
          {/* Pages */}
          {['Reports', 'Tasks', 'KPIs', 'Groups', 'Sprints'].map((item) => (
            <Link to={`/${item.toLowerCase()}`} key={item}>
              <li
                onClick={() => setActive(item)}
                style={{ paddingLeft: '3rem', paddingRight: '3rem' }}
                className={`cursor-pointer relative after:absolute after:bottom-0 after:left-1/2 after:transform after:-translate-x-1/2 after:h-[2px] after:bg-black after:transition-all after:duration-100 ${
                  active === item
                    ? 'after:w-[50px] font-semibold'
                    : 'after:w-0 hover:after:w-full'
                }`}
              >
                {item}
              </li>
            </Link>
          ))}
        </ul>
      </div>

      <div className="flex flex-row items-center gap-4">
        {isAuthenticated ? (
          <div className="flex items-center gap-4">
            <span className="text-lg">Welcome, {fullName}</span>
            <button
              onClick={handleLogout}
              className="bg-greenie text-white px-4 py-2 rounded-lg hover:opacity-90"
            >
              Logout
            </button>
          </div>
        ) : (
          <Link to="/login">
            <button className="bg-greenie text-white px-4 py-2 rounded-lg hover:opacity-90">
              Login
            </button>
          </Link>
        )}
        <img src={logo} className="h-14 aspect-square" alt="Logo" />
      </div>
    </nav>
  );
};

export default Navbar;