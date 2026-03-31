import toast from 'react-hot-toast';
import { AlertCircle, ShieldX, SearchX, Ban, XCircle, LogOut } from 'lucide-react';
import type { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types/errors';
import type { ReactNode } from 'react';

/**
 * Extracts the user-friendly message from an Axios error.
 * Prefers the backend's structured `message` field over the generic fallback.
 */
export function getErrorMessage(error: unknown, fallback: string): string {
  const axiosErr = error as AxiosError<ApiErrorResponse>;
  const backendMessage = axiosErr?.response?.data?.message;
  if (backendMessage && typeof backendMessage === 'string') {
    return backendMessage;
  }
  return fallback;
}

/**
 * Gets the HTTP status code from an error, if available.
 */
export function getErrorStatus(error: unknown): number | undefined {
  const axiosErr = error as AxiosError;
  return axiosErr?.response?.status;
}

/** Returns true if the error is a 404 Not Found — useful for list endpoints
 *  that return 404 when no items exist instead of an empty array. */
export function isNotFoundError(error: unknown): boolean {
  return getErrorStatus(error) === 404;
}

/** Returns true if the error is a 401 Unauthorized (session expired). */
export function isUnauthorizedError(error: unknown): boolean {
  return getErrorStatus(error) === 401;
}

/** Status-aware config for the toast */
function getErrorConfig(status: number | undefined): {
  title: string;
  icon: ReactNode;
  accent: string;
  bg: string;
} {
  switch (status) {
    case 400:
      return { title: 'Validation Error', icon: <AlertCircle size={18} />, accent: '#f59e0b', bg: '#fffbeb' };
    case 403:
      return { title: 'Access Denied', icon: <ShieldX size={18} />, accent: '#ef4444', bg: '#fef2f2' };
    case 404:
      return { title: 'Not Found', icon: <SearchX size={18} />, accent: '#6b7280', bg: '#f9fafb' };
    case 409:
      return { title: 'Action Not Allowed', icon: <Ban size={18} />, accent: '#f59e0b', bg: '#fffbeb' };
    default:
      return { title: 'Error', icon: <XCircle size={18} />, accent: '#ef4444', bg: '#fef2f2' };
  }
}

/**
 * Displays an enterprise-grade error toast with status-aware icon, accent color,
 * and close button. Shows both the page context (what was attempted) and the
 * backend's specific message when available.
 *
 * @param error    The caught error (typically AxiosError)
 * @param context  What the user was doing, e.g. "Failed to load published documents"
 */
export function showErrorToast(error: unknown, context: string): void {
  const backendMessage = getErrorMessage(error, '');
  const status = getErrorStatus(error);
  const { title, icon, accent, bg } = getErrorConfig(status);

  // Show context as the primary line, backend message as secondary detail
  const hasBackendDetail = backendMessage && backendMessage !== context;

  toast.custom(
    (t) => (
      <div
        className={`${t.visible ? 'animate-enter' : 'animate-leave'} pointer-events-auto`}
        style={{
          background: bg,
          border: `1px solid ${accent}33`,
          borderLeft: `4px solid ${accent}`,
          borderRadius: '8px',
          padding: '12px 16px',
          maxWidth: '480px',
          width: '100%',
          boxShadow: '0 4px 12px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.06)',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px',
        }}
      >
        <div style={{ color: accent, flexShrink: 0, marginTop: '1px' }}>
          {icon}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ fontSize: '13px', fontWeight: 600, color: '#111827', lineHeight: '1.4', margin: 0 }}>
            {title}
          </p>
          <p style={{ fontSize: '13px', color: '#4b5563', lineHeight: '1.4', margin: '2px 0 0 0' }}>
            {context}
          </p>
          {hasBackendDetail && (
            <p style={{ fontSize: '12px', color: '#6b7280', lineHeight: '1.4', margin: '4px 0 0 0', fontStyle: 'italic' }}>
              {backendMessage}
            </p>
          )}
        </div>
        <button
          onClick={() => toast.dismiss(t.id)}
          style={{
            flexShrink: 0,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '2px',
            color: '#9ca3af',
            lineHeight: 1,
          }}
          onMouseEnter={(e) => { e.currentTarget.style.color = '#4b5563'; }}
          onMouseLeave={(e) => { e.currentTarget.style.color = '#9ca3af'; }}
          aria-label="Dismiss"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 4l8 8M12 4l-8 8" />
          </svg>
        </button>
      </div>
    ),
    { duration: status === 403 || status === 409 ? 8000 : 5000 }
  );
}

/**
 * Shows a session-expired toast prompting the user to log in again.
 */
export function showSessionExpiredToast(): void {
  toast.custom(
    (t) => (
      <div
        className={`${t.visible ? 'animate-enter' : 'animate-leave'} pointer-events-auto`}
        style={{
          background: '#eff6ff',
          border: '1px solid #3b82f633',
          borderLeft: '4px solid #3b82f6',
          borderRadius: '8px',
          padding: '12px 16px',
          maxWidth: '480px',
          width: '100%',
          boxShadow: '0 4px 12px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.06)',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px',
        }}
      >
        <div style={{ color: '#3b82f6', flexShrink: 0, marginTop: '1px' }}>
          <LogOut size={18} />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ fontSize: '13px', fontWeight: 600, color: '#111827', lineHeight: '1.4', margin: 0 }}>
            Session Expired
          </p>
          <p style={{ fontSize: '13px', color: '#4b5563', lineHeight: '1.4', margin: '2px 0 0 0' }}>
            Your session has ended. Please log in again to continue.
          </p>
        </div>
        <button
          onClick={() => toast.dismiss(t.id)}
          style={{
            flexShrink: 0,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '2px',
            color: '#9ca3af',
            lineHeight: 1,
          }}
          onMouseEnter={(e) => { e.currentTarget.style.color = '#4b5563'; }}
          onMouseLeave={(e) => { e.currentTarget.style.color = '#9ca3af'; }}
          aria-label="Dismiss"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 4l8 8M12 4l-8 8" />
          </svg>
        </button>
      </div>
    ),
    { duration: 6000 }
  );
}
