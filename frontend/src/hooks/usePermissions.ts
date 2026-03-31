import { useAppSelector } from '../store/hooks';

/**
 * Returns the current user's editor-level permissions.
 * These are set by SYSTEM_ADMIN and control whether the user
 * can download, print, or upload documents via OnlyOffice.
 */
export function usePermissions() {
  const user = useAppSelector((s) => s.auth.user);
  const perms = user?.editorPermissions;

  return {
    canDownload: perms?.canDownload ?? false,
    canPrint: perms?.canPrint ?? false,
    canUpload: perms?.canUpload ?? false,
  };
}
