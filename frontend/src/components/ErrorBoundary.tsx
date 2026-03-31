import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface Props {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  handleReload = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
          <div className="bg-white rounded-lg border border-gray-200 shadow-sm max-w-md w-full p-8 text-center space-y-4">
            <AlertCircle size={48} className="mx-auto text-red-400" />
            <h1 className="text-lg font-semibold text-gray-900">Something went wrong</h1>
            <p className="text-sm text-gray-500">
              An unexpected error occurred. Please reload the page to continue.
            </p>
            {this.state.error && (
              <p className="text-xs text-gray-400 bg-gray-50 rounded p-3 font-mono break-all">
                {this.state.error.message}
              </p>
            )}
            <button
              onClick={this.handleReload}
              className="inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium bg-gray-800 text-white hover:bg-gray-900 transition-colors"
            >
              <RefreshCw size={14} /> Reload Page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
