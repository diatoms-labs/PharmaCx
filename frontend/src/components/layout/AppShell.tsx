import React from 'react';
import { useLocation } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import { GlobalLoadingBar } from '../ui/GlobalLoadingBar';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { toggleSidebar } from '../../store/slices/uiSlice';
import { useBranding } from '../../hooks/useBranding';

interface AppShellProps {
  children: React.ReactNode;
}

export default function AppShell({ children }: AppShellProps) {
  const dispatch = useAppDispatch();
  const location = useLocation();
  const sidebarOpen = useAppSelector((s) => s.ui.sidebarOpen);
  const branding = useBranding();

  const isFullFit = location.pathname.startsWith('/document/') || location.pathname.startsWith('/ai/search');

  return (
    <div className="flex h-screen overflow-hidden bg-gray-100">
      <GlobalLoadingBar />
      <Sidebar open={sidebarOpen} onToggle={() => dispatch(toggleSidebar())} branding={branding} />
      <div className="flex flex-1 flex-col overflow-hidden">
        {!isFullFit && (
          <Header onMenuToggle={() => dispatch(toggleSidebar())} headerColor={branding.headerColor} />
        )}
        <main className={`flex-1 overflow-y-auto ${isFullFit ? 'p-0' : 'p-6'}`}>
          {children}
        </main>
      </div>
    </div>
  );
}
