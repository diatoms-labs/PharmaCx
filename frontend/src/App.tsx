import { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import AppShell from './components/layout/AppShell';
import PageLoadingFallback from './components/ui/PageLoadingFallback';
import { Loader2 } from 'lucide-react';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const ProductPresentationPage = lazy(() => import('./pages/ProductPresentationPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const DocumentListPage = lazy(() => import('./pages/DocumentListPage'));
const DocumentDetailPage = lazy(() => import('./pages/DocumentDetailPage'));
const PublishedDocumentsPage = lazy(() => import('./pages/PublishedDocumentsPage'));
const MyDocumentsPage = lazy(() => import('./pages/MyDocumentsPage'));
const TrainingDetailPage = lazy(() => import('./pages/TrainingDetailPage'));
const TemplateManagementPage = lazy(() => import('./pages/TemplateManagementPage'));
const TemplateViewPage = lazy(() => import('./pages/TemplateViewPage'));
const UsersPage = lazy(() => import('./pages/UsersPage'));
const SystemAdminPage = lazy(() => import('./pages/SystemAdminPage'));
const AISearchPage = lazy(() => import('./pages/AISearchPage'));

function HomeWrapper() {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <PageLoadingFallback />;
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : <ProductPresentationPage />;
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 size={32} className="animate-spin text-brand-500" />
      </div>
    );
  }
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={
        <Suspense fallback={<PageLoadingFallback />}>
          <HomeWrapper />
        </Suspense>
      } />
      <Route path="/login" element={
        <Suspense fallback={<PageLoadingFallback />}>
          <LoginPage />
        </Suspense>
      } />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppShell>
              <Suspense fallback={<PageLoadingFallback />}>
                <Routes>
                  {/* Dashboard (includes My Tasks, QA Prep, Training tabs) */}
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/" element={<Navigate to="/dashboard" replace />} />

                  {/* Workspace */}
                  <Route path="/documents/my" element={<MyDocumentsPage />} />
                  <Route path="/templates" element={<TemplateManagementPage />} />
                  <Route path="/templates/:id" element={<TemplateViewPage />} />
                  <Route path="/document/:id" element={<DocumentDetailPage />} />

                  {/* Shared: Department documents */}
                  <Route path="/documents/:type" element={<DocumentListPage />} />
                  <Route path="/documents/:type/:department" element={<DocumentListPage />} />
                  <Route path="/published" element={<PublishedDocumentsPage />} />
                  <Route path="/published/:type/:department" element={<PublishedDocumentsPage />} />

                  {/* Training detail (drill-down from dashboard) */}
                  <Route path="/training/:id" element={<TrainingDetailPage />} />

                  {/* AI Search */}
                  <Route path="/ai/search" element={<AISearchPage />} />

                  {/* Admin */}
                  <Route path="/users" element={<UsersPage />} />
                  <Route path="/admin" element={<SystemAdminPage />} />

                  {/* Catch-all */}
                  <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
              </Suspense>
            </AppShell>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}
