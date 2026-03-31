import { useEffect, useState } from 'react';
import api from '../api/client';

export interface BrandingValues {
  orgName: string;
  orgSubtitle: string;
  logoUrl: string | null;
  sidebarColor: string;
  accentColor: string;
  headerColor: string | null;
}

export const BRANDING_DEFAULTS: BrandingValues = {
  orgName: 'PharmaCX',
  orgSubtitle: 'Compliance Execution',
  logoUrl: '/logo.svg',
  sidebarColor: '#0F3D6E',
  accentColor: '#1E7FC4',
  headerColor: '#1E7FC4',
};

// Module-level cache so multiple components share a single fetch
let cachedBranding: BrandingValues | null = null;
let listeners: Array<(b: BrandingValues) => void> = [];

export function applyBrandingCssVars(b: BrandingValues) {
  const root = document.documentElement;
  root.style.setProperty('--brand-sidebar-bg', b.sidebarColor);
  root.style.setProperty('--brand-accent', b.accentColor);
  if (b.headerColor) {
    root.style.setProperty('--brand-header-bg', b.headerColor);
  } else {
    root.style.removeProperty('--brand-header-bg');
  }
}

/** Call this after saving branding to update all subscribers live. */
export function notifyBrandingUpdate(b: BrandingValues) {
  cachedBranding = b;
  applyBrandingCssVars(b);
  listeners.forEach(fn => fn(b));
}

export function useBranding() {
  const [branding, setBranding] = useState<BrandingValues>(
    cachedBranding ?? BRANDING_DEFAULTS,
  );

  useEffect(() => {
    listeners.push(setBranding);

    if (!cachedBranding) {
      api
        .get<{ settings: BrandingValues }>('/system-settings')
        .then(res => {
          if (res.data?.settings) {
            const merged = { ...BRANDING_DEFAULTS, ...res.data.settings };
            notifyBrandingUpdate(merged);
          } else {
            applyBrandingCssVars(BRANDING_DEFAULTS);
          }
        })
        .catch(() => {
          applyBrandingCssVars(BRANDING_DEFAULTS);
        });
    } else {
      applyBrandingCssVars(cachedBranding);
    }

    return () => {
      listeners = listeners.filter(fn => fn !== setBranding);
    };
  }, []);

  return branding;
}
