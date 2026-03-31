import { GraduationCap } from 'lucide-react';

export default function TrainingAdminPage() {
  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-brand-900 flex items-center gap-2">
        <GraduationCap size={22} /> Training Administration
      </h2>
      <div className="card p-6">
        <p className="text-sm text-brand-500">
          Training assignment is available from the Published Documents page under Shared departments.
        </p>
      </div>
    </div>
  );
}
