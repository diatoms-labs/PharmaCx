import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="card p-8 flex flex-col items-center text-center">
      <div className="text-brand-300 mb-2">{icon}</div>
      <p className="text-sm text-brand-500 font-medium">{title}</p>
      {description && <p className="text-xs text-brand-400 mt-1">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
