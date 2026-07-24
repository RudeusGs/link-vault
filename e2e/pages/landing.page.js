const { expect } = require('@playwright/test');
const { clickFirst, expectBodyContains } = require('../helpers/locator-helpers');

class LandingPage {
  constructor(page) {
    this.page = page;
  }

  async open() {
    await this.page.goto('/');
    await this.page.waitForLoadState('domcontentloaded');
  }

  async expectPublicPage() {
    await expect(this.page).not.toHaveURL(/\/login(?:\?|$)/);
    await expectBodyContains(this.page, [
      /LinkVault/i,
      /quản lý.*(link|file|tài nguyên)/i,
      /organize.*(link|file|resource)/i,
    ]);
  }

  async goToLogin() {
    await clickFirst([
      this.page.getByRole('link', { name: /đăng nhập|login|sign in/i }),
      this.page.getByRole('button', { name: /đăng nhập|login|sign in/i }),
      this.page.locator('[data-testid="login-link"]'),
      this.page.locator('a[href*="login"]'),
    ], 'nút/link đăng nhập');
  }

  async goToRegister() {
    await clickFirst([
      this.page.getByRole('link', { name: /đăng ký|get started|register|sign up|bắt đầu/i }),
      this.page.getByRole('button', { name: /đăng ký|get started|register|sign up|bắt đầu/i }),
      this.page.locator('[data-testid="register-link"]'),
      this.page.locator('a[href*="register"]'),
    ], 'nút/link đăng ký');
  }
}

module.exports = { LandingPage };
