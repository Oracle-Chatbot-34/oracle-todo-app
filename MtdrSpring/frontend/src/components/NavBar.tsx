import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import logo from '../assets/logo.png';

const Navbar = () => {
  var location = useLocation();
  var locationText =
    location.pathname.split('/')[1].charAt(0).toUpperCase() +
    location.pathname.split('/')[1].slice(1);

  const [active, setActive] = useState(locationText);

  useEffect(() => {
    setActive(locationText);
  }, [locationText]);

  return (
    <nav className="flex flex-row bg-background shrink-0 min-h-16 items-center justify-between px-10 shadow-md z-50">
      {/* Logo and Title */}
      <div className="flex flex-row items-center gap-[10px]">
        <img src={logo} className="h-14 aspect-square" alt="Logo" />
        <div />
        <a href="/" className="text-[36px] font-semibold ml-2">
          DashMaster
        </a>
      </div>

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
          {['Reports', 'Tasks'].map((item) => (
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

      <div className="flex flex-row items-center gap-[370px]">
        <div />
        <img src={logo} className="h-14 aspect-square" alt="Logo" />
      </div>
    </nav>
  );
};

export default Navbar;
