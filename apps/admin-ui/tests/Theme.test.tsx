// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { ThemePicker } from '../src/Theme';

afterEach(() => { cleanup(); localStorage.clear(); delete document.documentElement.dataset.theme; vi.restoreAllMocks(); });
it('defaults to dark without storing session data and remembers an explicit selection', () => {
  const view = render(<ThemePicker />);
  expect(document.documentElement.dataset.theme).toBe('dark');
  expect(localStorage.length).toBe(0);
  fireEvent.change(screen.getByLabelText('Theme'), {target:{value:'light'}});
  expect(document.documentElement.dataset.theme).toBe('light');
  expect(localStorage.getItem('toolgate.theme')).toBe('light');
  view.unmount(); render(<ThemePicker />);
  expect((screen.getByLabelText('Theme') as HTMLSelectElement).value).toBe('light');
});
it('follows system changes only when System is selected', () => {
  let change: (() => void) | undefined;
  const media = {matches:true,addEventListener:vi.fn((_event,handler) => {change=handler;}),removeEventListener:vi.fn()};
  vi.stubGlobal('matchMedia', vi.fn(() => media));
  const view=render(<ThemePicker />);
  expect(document.documentElement.dataset.theme).toBe('dark');
  fireEvent.change(screen.getByLabelText('Theme'), {target:{value:'system'}});
  expect(document.documentElement.dataset.theme).toBe('light');
  media.matches=false; change?.();
  expect(document.documentElement.dataset.theme).toBe('dark');
  view.unmount(); expect(media.removeEventListener).toHaveBeenCalled();
  vi.unstubAllGlobals();
});
it('works when browser storage is unavailable', () => {
  vi.spyOn(Storage.prototype,'getItem').mockImplementation(() => {throw new Error('Storage denied');});
  vi.spyOn(Storage.prototype,'setItem').mockImplementation(() => {throw new Error('Storage denied');});
  render(<ThemePicker />);
  fireEvent.change(screen.getByLabelText('Theme'), {target:{value:'light'}});
  expect(document.documentElement.dataset.theme).toBe('light');
});
