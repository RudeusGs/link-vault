const { test, expect } = require('@playwright/test');

test('S-01: API health trả trạng thái thành công', async ({ request }) => {
  const apiURL = process.env.E2E_API_URL;
  test.skip(!apiURL, 'Chưa cấu hình E2E_API_URL');

  const response = await request.get(`${apiURL.replace(/\/$/, '')}/api/health`);
  expect(response.ok()).toBeTruthy();
  const body = await response.json().catch(() => ({}));
  expect(String(body.status || body.message || 'UP')).toMatch(/UP|OK|healthy/i);
});
