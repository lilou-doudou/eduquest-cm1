import { defineConfig, devices } from '@playwright/test';

// Pile isolée lancée par scripts/e2e.sh : backend sur 8081 (base PostgreSQL jetable), Vite sur 5174.
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5174';
const chromium = process.env.PLAYWRIGHT_CHROMIUM_PATH;

export default defineConfig({
  testDir: './tests',
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  workers: 3,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: BASE_URL,
    locale: 'fr-FR',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: chromium ? { executablePath: chromium } : undefined,
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] }, testIgnore: /mobile|no-webgl/ },
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /mobile/ },
    { name: 'no-webgl', use: { ...devices['Desktop Chrome'] }, testMatch: /no-webgl/ },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev -- --host 127.0.0.1 --port 5174',
        cwd: '../frontend',
        url: BASE_URL,
        reuseExistingServer: !process.env.CI,
        env: { API_PROXY_TARGET: process.env.API_PROXY_TARGET ?? 'http://127.0.0.1:8081' },
        timeout: 60_000,
      },
});
