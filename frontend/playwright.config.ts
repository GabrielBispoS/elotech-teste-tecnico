import { defineConfig, devices } from '@playwright/test';

/**
 * E2E roda contra a aplicacao ja no ar (docker compose up -d, ou ng serve + backend local).
 * A URL base pode ser trocada por E2E_BASE_URL.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
