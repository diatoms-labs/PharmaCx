import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  icon?: ReactNode;
  subtitle?: string;
  action?: ReactNode;
}

export function PageHeader({ title, icon, subtitle, action }: PageHeaderProps) {
  return (
    <div className="flex items-center justify-between">
      <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
        {icon} {title}
        {subtitle && <span className="text-sm font-normal text-gray-400 ml-1">{subtitle}</span>}
      </h2>
      {action}
    </div>
  );
}
