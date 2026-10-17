import { describe, it, expect } from 'vitest';
import { formatCurrency, formatDate, formatDuration, formatPercent, cn } from '@/lib/utils';

describe('Utility Formatters', () => {
  it('formats currency correctly', () => {
    expect(formatCurrency(1250.5)).toBe('$1,250.50');
    expect(formatCurrency(0)).toBe('$0.00');
    expect(formatCurrency(null)).toBe('$0.00');
    expect(formatCurrency(undefined)).toBe('$0.00');
  });

  it('formats dates consistently', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate('')).toBe('—');
    const formatted = formatDate('2026-10-15');
    expect(formatted).toContain('2026');
  });

  it('formats duration', () => {
    expect(formatDuration(45)).toBe('45s');
    expect(formatDuration(125)).toBe('2m 5s');
    expect(formatDuration(null)).toBe('0s');
  });

  it('formats percentages', () => {
    expect(formatPercent(99.45)).toBe('99.5%');
    expect(formatPercent(100)).toBe('100.0%');
    expect(formatPercent(null)).toBe('0.0%');
  });

  it('merges class names cleanly', () => {
    expect(cn('px-2', 'py-1', { 'text-red-500': true, 'text-blue-500': false })).toBe(
      'px-2 py-1 text-red-500'
    );
  });
});
