import React, { useState, useEffect } from 'react';
import authService from '../services/authService';
import { AuthContext } from '@/hooks/useAuth';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [username, setUsername] = useState<string | null>(null);
  const [fullName, setFullName] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Check authentication status on mount
    const storedToken = localStorage.getItem('token');
    const storedUsername = localStorage.getItem('username');
    const storedFullName = localStorage.getItem('fullName');

    if (storedToken && storedUsername) {
      setIsAuthenticated(true);
      setUsername(storedUsername);
      setFullName(storedFullName);
    }

    setLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    try {
      setLoading(true);
      const response = await authService.login({ username, password });

      localStorage.setItem('token', response.token);
      localStorage.setItem('username', response.username);
      localStorage.setItem('fullName', response.fullName);

      setIsAuthenticated(true);
      setUsername(response.username);
      setFullName(response.fullName);
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setUsername(null);
    setFullName(null);
  };

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, username, fullName, login, logout, loading }}
    >
      {children}
    </AuthContext.Provider>
  );
};
