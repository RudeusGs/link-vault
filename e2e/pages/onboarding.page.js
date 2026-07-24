const { fillFirst, clickFirst, firstVisible } = require('../helpers/locator-helpers');

class OnboardingPage {
  constructor(page) {
    this.page = page;
  }

  async open() {
    await this.page.goto('/onboarding');
  }

  nameCandidates() {
    return [
      this.page.getByLabel(/workspace name|tên workspace|tên không gian|tên nhóm/i),
      this.page.getByPlaceholder(/workspace name|tên workspace|tên không gian|tên nhóm/i),
      this.page.locator('[data-testid="workspace-name"]'),
      this.page.locator('input[name*="workspace" i]'),
      this.page.locator('form input[type="text"]').first(),
    ];
  }

  createCandidates() {
    return [
      this.page.getByRole('button', { name: /create workspace|tạo workspace|tạo không gian|bắt đầu/i }),
      this.page.locator('[data-testid="create-workspace"]'),
      this.page.locator('button[type="submit"]'),
    ];
  }

  async expectFormVisible() {
    await firstVisible(this.nameCandidates(), 'ô tên workspace');
    await firstVisible(this.createCandidates(), 'nút tạo workspace');
  }

  async createWorkspace(name) {
    await fillFirst(this.nameCandidates(), name, 'ô tên workspace');
    await clickFirst(this.createCandidates(), 'nút tạo workspace');
  }
}

module.exports = { OnboardingPage };
