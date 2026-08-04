import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorBoundary } from '../ErrorBoundary';

const Bomb = ({ shouldThrow }: { shouldThrow: boolean }) => {
  if (shouldThrow) {
    throw new Error('Test Error Boundary Bomb!');
  }
  return <div>Safe Child</div>;
};

describe('ErrorBoundary', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={false} />
      </ErrorBoundary>
    );

    expect(screen.getByText('Safe Child')).toBeInTheDocument();
  });

  it('renders fallback UI when a child throws', () => {
    // Hide expected console.error from the test output
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText('An unexpected error occurred in the application. Our team has been notified.')).toBeInTheDocument();
    expect(screen.getByText('Test Error Boundary Bomb!')).toBeInTheDocument();

    consoleSpy.mockRestore();
  });
});
