import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { ThemeProvider } from '@/context/ThemeContext';
import { LoginView } from '@/views/auth/LoginView';

describe('LoginView Component', () => {
  const renderLogin = () => {
    return render(
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <LoginView />
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    );
  };

  it('renders AI-CRM branding and form controls', () => {
    renderLogin();
    expect(screen.getAllByText('AI-CRM').length).toBeGreaterThan(0);
    expect(screen.getByText('Enterprise Sign In')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('e.g. admin or marketer')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter your account password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('displays validation error if submitted empty', async () => {
    renderLogin();
    const submitBtn = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Please enter both username and password.')).toBeInTheDocument();
    });
  });

  it('does NOT contain demo credentials or quick role preset buttons', () => {
    renderLogin();
    expect(screen.queryByText('Quick Credential Presets')).not.toBeInTheDocument();
    expect(screen.queryByText('1-Click Select')).not.toBeInTheDocument();
    expect(screen.queryByText('Administrator')).not.toBeInTheDocument();
    expect(screen.queryByText('Marketing Lead')).not.toBeInTheDocument();
  });
});
