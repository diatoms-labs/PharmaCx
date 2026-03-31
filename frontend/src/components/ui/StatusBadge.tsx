import { fmt } from '../../utils/format';

type BadgeVariant = 'document' | 'training' | 'task';

const DOCUMENT_STATUS_MAP: Record<string, string> = {
  REQUESTED: 'badge-dark',
  QA_PREPARATION: 'badge-dark',
  AUTHOR_DRAFT: 'badge-dark',
  PEER_REVIEW: 'badge-dark',
  QA_REVIEW: 'badge-dark',
  APPROVAL: 'badge-yellow',
  PUBLISHED: 'badge-gray',
  RETIRED: 'badge-gray',
};

const TRAINING_STATUS_MAP: Record<string, string> = {
  ASSIGNED: 'badge-dark',
  IN_PROGRESS: 'badge-yellow',
  READ: 'badge-yellow',
  QUIZ_PASSED: 'badge-green',
  COMPLETED: 'badge-green',
  FAILED: 'badge-red',
  OVERDUE: 'badge-red',
};

const TASK_STATUS_MAP: Record<string, string> = {
  PUBLISHED: 'badge-gray',
  RETIRED: 'badge-gray',
};

function resolveBadgeClass(status: string, variant: BadgeVariant): string {
  switch (variant) {
    case 'document':
      return DOCUMENT_STATUS_MAP[status] ?? 'badge-dark';
    case 'training':
      return TRAINING_STATUS_MAP[status] ?? 'badge-dark';
    case 'task':
      return TASK_STATUS_MAP[status] ?? 'badge-dark';
  }
}

interface StatusBadgeProps {
  status: string;
  variant?: BadgeVariant;
  label?: string;
}

export function StatusBadge({ status, variant = 'document', label }: StatusBadgeProps) {
  return (
    <span className={resolveBadgeClass(status, variant)}>
      {label ?? fmt(status)}
    </span>
  );
}
