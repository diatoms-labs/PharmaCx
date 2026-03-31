import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store';
import { initAuth } from './store/slices/authSlice';
import App from './App';
import ErrorBoundary from './components/ErrorBoundary';
import { Toaster } from 'react-hot-toast';
import './index.css';

// Hydrate auth state from localStorage before first render
store.dispatch(initAuth());

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <ErrorBoundary>
          <App />
        </ErrorBoundary>
        <Toaster
          position="top-center"
          toastOptions={{
            style: {
              background: '#343a40',
              color: '#f8f9fa',
              fontSize: '0.875rem',
              maxWidth: '480px',
            },
            success: { duration: 3000 },
            error: { duration: 6000 },
          }}
        />
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
);
