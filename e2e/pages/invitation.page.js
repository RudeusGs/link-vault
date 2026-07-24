const { clickFirst, expectBodyContains } = require('../helpers/locator-helpers');

class InvitationPage {
  constructor(page, token = 'e2e-valid-token') {
    this.page = page;
    this.token = token;
  }

  async open() {
    await this.page.goto(`/invitations/${this.token}`);
  }

  async expectInvitationVisible() {
    await expectBodyContains(this.page, [/Demo Workspace/i, /lời mời|invitation/i, /Workspace Owner/i]);
  }

  async accept() {
    await clickFirst([
      this.page.getByRole('button', { name: /accept|chấp nhận|tham gia/i }),
      this.page.locator('[data-testid="accept-invitation"]'),
    ], 'nút chấp nhận lời mời');
  }

  async decline() {
    await clickFirst([
      this.page.getByRole('button', { name: /decline|từ chối/i }),
      this.page.locator('[data-testid="decline-invitation"]'),
    ], 'nút từ chối lời mời');
  }
}

module.exports = { InvitationPage };
