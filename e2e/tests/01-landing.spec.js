const { test, expect } = require('@playwright/test');
const { LandingPage } = require('../pages/landing.page');

test.describe('Landing page - người dùng chưa đăng nhập', () => {
  test('L-01: route / hiển thị công khai và không tự chuyển sang login', async ({ page }) => {
    const landing = new LandingPage(page);
    await landing.open();
    await landing.expectPublicPage();
  });

  test('L-02: nút đăng nhập điều hướng đến /login', async ({ page }) => {
    const landing = new LandingPage(page);
    await landing.open();
    await landing.goToLogin();
    await expect(page).toHaveURL(/\/login(?:\?|$)/);
  });

  test('L-03: CTA đăng ký/bắt đầu điều hướng đúng auth flow', async ({ page }) => {
    const landing = new LandingPage(page);
    await landing.open();
    await landing.goToRegister();
    await expect(page).toHaveURL(/\/(register|signup|login)(?:\?|$)/);
  });
});
