// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState } from 'react';

type Theme = 'dark' | 'light' | 'system';
const storageKey = 'toolgate.theme';
export function themePreference(): Theme {
  try {
    const saved = localStorage.getItem(storageKey);
    if (saved === 'light' || saved === 'system') return saved;
  } catch { /* Storage is optional; dark remains the default. */ }
  return 'dark';
}
export function applyTheme(theme: Theme) {
  const light = theme === 'light' || (theme === 'system' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: light)').matches);
  document.documentElement.dataset.theme = light ? 'light' : 'dark';
}
export function ThemePicker() {
  const [theme, setTheme] = useState<Theme>(themePreference);
  useEffect(() => {
    applyTheme(theme);
    if (theme !== 'system' || typeof window.matchMedia !== 'function') return;
    const media = window.matchMedia('(prefers-color-scheme: light)');
    const update = () => applyTheme('system');
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, [theme]);
  return <label className="theme-picker">Theme
    <select value={theme} onChange={event => {
      const value = event.target.value as Theme;
      setTheme(value);
      try { localStorage.setItem(storageKey, value); } catch { /* Session selection still works. */ }
    }}>
      <option value="dark">Dark</option><option value="light">Light</option><option value="system">System</option>
    </select>
  </label>;
}
