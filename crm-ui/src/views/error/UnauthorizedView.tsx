import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/common/Button';
import { ShieldAlert } from 'lucide-react';

export const UnauthorizedView: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6">
      <div className="w-16 h-16 rounded-2xl bg-red-950/20 border border-red-800/40 flex items-center justify-center text-red-500 mb-4 shadow-xs">
        <ShieldAlert className="w-8 h-8" />
      </div>
      <h1 className="text-2xl font-bold text-app-primary">Access Denied</h1>
      <p className="text-sm text-app-secondary max-w-md mt-2 mb-6">
        You do not possess the required enterprise permissions (ROLE_ADMIN) to access this administrative section.
      </p>
      <Button variant="secondary" onClick={() => navigate('/dashboard')}>
        Return to Dashboard
      </Button>
    </div>
  );
};
