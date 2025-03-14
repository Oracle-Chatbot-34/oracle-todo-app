import logo from '../assets/logo.png';
import { useNavigate, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Get the return URL from location state or default to home
  const from =
    (location.state as { from: { pathname: string } } | null)?.from?.pathname ||
    '/';

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!username || !password) {
      setError('Please enter both username and password');
      return;
    }

    try {
      setLoading(true);
      setError('');

      await login(username, password);

      // Navigate to the page the user was trying to access or home
      navigate(from, { replace: true });
    } catch (err: unknown) {
      console.error('Login error:', err);
      const errorMessage =
        err instanceof Error
          ? err.message
          : (err as { response?: { data?: { error?: string } } })?.response
              ?.data?.error;
      setError(errorMessage || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-background h-screen w-full flex flex-row items-center justify-center gap-[250px]">
      <div className="flex flex-col items-center">
        <p className="text-[80px]">DashMaster</p>
        <img src={logo} className="w-[500px] h-[460px]" alt="Logo" />
      </div>

      <div className="w-[440px] h-[460px] bg-white rounded-lg shadow-xl">
        <form
          onSubmit={handleLogin}
          className="h-full flex flex-col items-center justify-center gap-[20px] p-6"
        >
          <div className="justify-center flex flex-col items-center">
            <p className="text-[40px] w-[230px] text-sm/12 text-center">
              Go to your Dashboard!
            </p>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded w-full">
              {error}
            </div>
          )}

          <div className="flex flex-col items-start relative gap-[50px] w-full">
            {/* Username */}
            <div className="w-full">
              <p className="text-[20px] text-[#747276] text-left">Username</p>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                style={{
                  border: '2px #9B9B9B solid',
                  borderRadius: '8px',
                }}
                className="w-full h-[40px] border-2 border-solid border-black rounded-lg p-4"
              />
            </div>
            {/* Password */}
            <div className="w-full">
              <p className="text-[20px] text-[#747276] text-left">Password</p>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{
                  border: '2px #9B9B9B solid',
                  borderRadius: '8px',
                }}
                className="w-full h-[40px] border-2 border-gray-500 rounded-lg p-4"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className={`bg-greenie rounded-lg text-white text-[25px] h-[40px] w-[320px] ${
              loading ? 'opacity-50 cursor-not-allowed' : ''
            }`}
          >
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
      </div>
    </div>
  );
}
