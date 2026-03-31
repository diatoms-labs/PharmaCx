import type { ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  icon?: ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
  children: ReactNode;
  footer?: ReactNode;
}

const MAX_WIDTH_MAP = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl',
};

export function Modal({ open, onClose, title, icon, maxWidth = 'md', children, footer }: ModalProps) {
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      <div
        className={`bg-white rounded-lg shadow-xl w-full ${MAX_WIDTH_MAP[maxWidth]} mx-4`}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-brand-900 flex items-center gap-2">
            {icon} {title}
          </h3>
          <button
            onClick={onClose}
            className="text-brand-400 hover:text-brand-600 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="px-5 py-4">{children}</div>

        {/* Footer */}
        {footer && (
          <div className="px-5 py-3 border-t border-gray-200 flex justify-end gap-2">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
