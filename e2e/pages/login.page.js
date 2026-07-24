const { fillFirst, clickFirst, firstVisible } = require('../helpers/locator-helpers');

class LoginPage {
  constructor(page) {
    this.page = page;
  }

  async open() {
    await this.page.goto('/login');
  }

  usernameCandidates() {
    return [
      this.page.getByLabel(/email|username|tên đăng nhập|tài khoản/i),
      this.page.getByPlaceholder(/email|username|tên đăng nhập|tài khoản/i),
      this.page.locator('[data-testid="login-username"]'),
      this.page.locator('input[type="email"]'),
      this.page.locator('input[name*="user" i]'),
      this.page.locator('input').first(),
    ];
  }

  passwordCandidates() {
    return [
      this.page.getByLabel(/password|mật khẩu/i),
      this.page.getByPlaceholder(/password|mật khẩu/i),
      this.page.locator('[data-testid="login-password"]'),
      this.page.locator('input[type="password"]'),
    ];
  }

  submitCandidates() {
    return [
      this.page.getByRole('button', { name: /đăng nhập|login|sign in/i }),
      this.page.locator('[data-testid="login-submit"]'),
      this.page.locator('button[type="submit"]'),
    ];
  }

  async expectFormVisible() {
    await firstVisible(this.usernameCandidates(), 'ô email/tên đăng nhập');
    await firstVisible(this.passwordCandidates(), 'ô mật khẩu');
    await firstVisible(this.submitCandidates(), 'nút đăng nhập');
  }

  async submitEmpty() {
    await clickFirst(this.submitCandidates(), 'nút đăng nhập');
  }

  async login(username, password) {
    await fillFirst(this.usernameCandidates(), username, 'ô email/tên đăng nhập');
    await fillFirst(this.passwordCandidates(), password, 'ô mật khẩu');
    await clickFirst(this.submitCandidates(), 'nút đăng nhập');
  }
}

module.exports = { LoginPage };
