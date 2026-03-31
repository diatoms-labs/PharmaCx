import { useAppSelector } from '../../store/hooks';
import { selectIsLoading } from '../../store/slices/uiSlice';

export function GlobalLoadingBar() {
  const isLoading = useAppSelector(selectIsLoading);

  if (!isLoading) return null;

  return (
    <div className="fixed top-0 left-0 right-0 z-[60] h-[3px] bg-gray-200/50">
      <div
        className="h-full bg-gray-700 rounded-r"
        style={{
          animation: 'global-loading 1.8s ease-in-out infinite',
        }}
      />
    </div>
  );
}
