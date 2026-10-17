import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Badge } from '@/components/common/Badge';

describe('Badge Component', () => {
  it('renders badge with correct text and variant styles', () => {
    render(<Badge variant="admin">ADMIN</Badge>);
    const badge = screen.getByText('ADMIN');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('uppercase');
  });

  it('renders different statuses properly', () => {
    const { rerender } = render(<Badge variant="running">RUNNING</Badge>);
    expect(screen.getByText('RUNNING')).toBeInTheDocument();

    rerender(<Badge variant="completed">COMPLETED</Badge>);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });
});
