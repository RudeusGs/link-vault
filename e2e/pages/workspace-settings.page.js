const { fillFirst, clickFirst, firstVisible } = require('../helpers/locator-helpers');

class WorkspaceSettingsPage {
  constructor(page, workspaceId = 'ws-e2e-001') {
    this.page = page;
    this.workspaceId = workspaceId;
  }

  async open() {
    await this.page.goto(`/workspaces/${this.workspaceId}/settings`);
  }

  emailCandidates() {
    return [
      this.page.getByLabel(/member email|email thành viên|email người được mời|email/i),
      this.page.getByPlaceholder(/member email|email thành viên|email người được mời|email/i),
      this.page.locator('[data-testid="invite-email"]'),
      this.page.locator('input[type="email"]'),
    ];
  }

  inviteCandidates() {
    return [
      this.page.getByRole('button', { name: /invite member|mời thành viên|gửi lời mời|invite/i }),
      this.page.locator('[data-testid="invite-submit"]'),
      this.page.locator('form button[type="submit"]'),
    ];
  }

  async expectInviteFormVisible() {
    await firstVisible(this.emailCandidates(), 'ô email mời thành viên');
    await firstVisible(this.inviteCandidates(), 'nút gửi lời mời');
  }

  async invite(email) {
    await fillFirst(this.emailCandidates(), email, 'ô email mời thành viên');
    await clickFirst(this.inviteCandidates(), 'nút gửi lời mời');
  }
}

module.exports = { WorkspaceSettingsPage };
