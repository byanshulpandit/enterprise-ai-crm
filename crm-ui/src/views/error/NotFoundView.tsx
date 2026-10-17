import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/common/Button';
import { Compass } from 'lucide-react';

export const NotFoundView: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6">
      <div className="w-16 h-16 rounded-2xl bg-app-surface-subtle border border-app flex items-center justify-center text-app-muted mb-4 shadow-xs">
        <Compass className="w-8 h-8" />
      </div>
      <h1 className="text-2xl font-bold text-app-primary">Page Not Found</h1>
      <p className="text-sm text-app-secondary max-w-md mt-2 mb-6">
        The requested resource or page route could not be found within this AI-CRM console.
      </p>
      <Button onClick={() => navigate('/dashboard')}>Return to Dashboard</Button>
    </div>
  );
};
