const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/login.page');

test.describe('Đăng nhập', () => {
  test('A-01: form đăng nhập có đủ trường cần thiết', async ({ page }) => {
    const login = new LoginPage(page);
    await login.open();
    await login.expectFormVisible();
  });

  test('A-02: không cho đăng nhập với dữ liệu trống', async ({ page }) => {
    const login = new LoginPage(page);
    await login.open();
    await login.submitEmpty();
    await expect(page).toHaveURL(/\/login(?:\?|$)/);
  });

  test('A-03: đăng nhập bằng tài khoản test khi đã cấu hình biến môi trường', async ({ page }) => {
    test.skip(!process.env.E2E_USERNAME || !process.env.E2E_PASSWORD, 'Chưa cấu hình E2E_USERNAME/E2E_PASSWORD');
    const login = new LoginPage(page);
    await login.open();
    await login.login(process.env.E2E_USERNAME, process.env.E2E_PASSWORD);
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 20_000 });
  });
});
