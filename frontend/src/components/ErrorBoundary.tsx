import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // You can also log the error to an error reporting service here
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen w-full flex items-center justify-center bg-background text-foreground p-4">
          <div className="max-w-md w-full bg-card border border-destructive/20 rounded-2xl p-6 shadow-xl elevation-5 animate-in zoom-in-95 duration-300">
            <div className="flex flex-col items-center text-center gap-4">
              <div className="w-16 h-16 rounded-full bg-destructive/10 flex items-center justify-center text-destructive">
                <AlertTriangle size={32} />
              </div>
              <div>
                <h1 className="text-2xl font-bold tracking-tight mb-2">Something went wrong</h1>
                <p className="text-muted-foreground text-sm mb-6">
                  An unexpected error occurred in the application. Our team has been notified.
                </p>
                {this.state.error && (
                  <div className="bg-muted/50 rounded-lg p-3 text-left mb-6 overflow-x-auto">
                    <pre className="text-xs font-mono text-muted-foreground">
                      {this.state.error.message}
                    </pre>
                  </div>
                )}
              </div>
              <button
                onClick={() => window.location.reload()}
                className="w-full h-11 bg-primary text-primary-foreground font-semibold rounded-xl hover:opacity-90 transition-opacity"
              >
                Reload application
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
