import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Logo } from '@/components/common/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';
import { User, Lock, AlertCircle, Shield } from 'lucide-react';

export const LoginView: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // If already logged in, redirect to dashboard or intended route
  const from = (location.state as any)?.from?.pathname || '/dashboard';
  React.useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, from]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Please enter both username and password.');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      await login(username.trim(), password);
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(err?.message || 'Invalid username or password');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col justify-between bg-app-main p-4 sm:p-6 lg:p-8">
      {/* Top Header with Theme Switcher */}
      <header className="w-full max-w-5xl mx-auto flex items-center justify-between">
        <Logo size="md" showSubtitle />
        <ThemeToggle />
      </header>

      {/* Center Auth Card */}
      <main className="w-full max-w-md mx-auto my-auto py-8">
        <div className="bg-app-surface border border-app rounded-2xl p-6 sm:p-8 shadow-xl">
          <div className="mb-6 text-left">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-app-primary">
              Enterprise Sign In
            </h1>
            <p className="text-xs sm:text-sm text-app-secondary mt-1">
              Enter your corporate credentials to access the AI-CRM platform.
            </p>
          </div>

          {error && (
            <div
              className="mb-5 p-3 rounded-lg bg-red-950/20 border border-red-800/40 text-red-500 text-xs flex items-start gap-2.5"
              role="alert"
            >
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <Input
              label="Username or Email"
              id="login-username"
              type="text"
              placeholder="e.g. admin or marketer"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              leftIcon={<User className="w-4 h-4" />}
              autoComplete="username"
              autoFocus
              required
              disabled={isLoading}
            />

            <Input
              label="Password"
              id="login-password"
              type="password"
              placeholder="Enter your account password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              leftIcon={<Lock className="w-4 h-4" />}
              autoComplete="current-password"
              required
              disabled={isLoading}
            />

            <Button
              type="submit"
              className="w-full mt-2"
              size="lg"
              isLoading={isLoading}
            >
              {isLoading ? 'Authenticating...' : 'Sign In'}
            </Button>
          </form>

          <div className="mt-6 pt-5 border-t border-app flex items-center justify-center gap-1.5 text-app-muted text-[11px]">
            <Shield className="w-3.5 h-3.5 shrink-0" />
            <span>Authorized enterprise access only. Sessions are logged.</span>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full max-w-5xl mx-auto text-center text-xs text-app-muted">
        AI-CRM Platform &copy; {new Date().getFullYear()}. All rights reserved.
      </footer>
    </div>
  );
};
