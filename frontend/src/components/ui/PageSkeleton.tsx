import { Skeleton } from './Skeleton';

/** Skeleton for pages with a table layout (most pages) */
export function TablePageSkeleton() {
  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-9 w-9 rounded-md" />
      </div>

      {/* Table card */}
      <div className="card overflow-hidden">
        {/* Table header */}
        <div className="bg-gray-50 border-b border-gray-200 px-4 py-3 flex gap-6">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-3 w-32" />
          <Skeleton className="h-3 w-20" />
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-3 w-16" />
        </div>
        {/* Table rows */}
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="px-4 py-3.5 border-b border-gray-100 flex gap-6 items-center">
            <Skeleton className="h-3.5 w-20" />
            <Skeleton className="h-3.5 w-40" />
            <Skeleton className="h-3.5 w-16" />
            <Skeleton className="h-5 w-20 rounded-full" />
            <Skeleton className="h-3.5 w-24" />
          </div>
        ))}
      </div>
    </div>
  );
}

/** Skeleton for the dashboard with KPI cards + tabs */
export function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-7 w-32" />

      {/* KPI cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="card p-4">
            <div className="flex items-center gap-3">
              <Skeleton className="w-10 h-10 rounded-lg" />
              <div className="space-y-2">
                <Skeleton className="h-6 w-12" />
                <Skeleton className="h-3 w-20" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Tab bar + content */}
      <div className="card overflow-hidden">
        <div className="border-b border-gray-200 bg-gray-50 px-4 py-3 flex gap-4">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-4 w-24" />
        </div>
        <div className="p-4 space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex items-center gap-4 py-2">
              <Skeleton className="h-3.5 w-20" />
              <Skeleton className="h-3.5 w-48" />
              <Skeleton className="h-3.5 w-16" />
              <Skeleton className="h-5 w-20 rounded-full" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/** Skeleton for document detail / template view (header + editor area) */
export function DocumentDetailSkeleton() {
  return (
    <div className="space-y-4">
      {/* Back link + title */}
      <div className="flex items-center gap-3">
        <Skeleton className="h-4 w-4" />
        <Skeleton className="h-6 w-64" />
      </div>

      {/* Status bar + badges */}
      <div className="card p-4">
        <div className="flex items-center gap-4">
          <Skeleton className="h-5 w-24 rounded-full" />
          <Skeleton className="h-3 w-32" />
          <Skeleton className="h-3 w-20" />
        </div>
      </div>

      {/* Workflow steps */}
      <div className="card p-4">
        <div className="flex items-center gap-2">
          {Array.from({ length: 7 }).map((_, i) => (
            <div key={i} className="flex items-center gap-2">
              <Skeleton className="w-7 h-7 rounded-full" />
              <Skeleton className="h-3 w-16" />
            </div>
          ))}
        </div>
      </div>

      {/* Editor area */}
      <div className="card overflow-hidden">
        <Skeleton className="h-[500px] w-full rounded-none" />
      </div>
    </div>
  );
}
