import { TablePageSkeleton } from './PageSkeleton';

/** Route-level Suspense fallback — top progress bar + skeleton placeholder */
export default function PageLoadingFallback() {
  return (
    <div>
      <div className="fixed top-0 left-0 right-0 h-0.5 bg-gray-200 z-50">
        <div className="h-full bg-gray-800 rounded-r animate-loading-bar" />
      </div>
      <TablePageSkeleton />
    </div>
  );
}
