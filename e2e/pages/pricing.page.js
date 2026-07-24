const { clickFirst, expectBodyContains } = require('../helpers/locator-helpers');

class PricingPage {
  constructor(page) {
    this.page = page;
  }

  async open() {
    await this.page.goto('/pricing');
  }

  async expectFreePlan() {
    await expectBodyContains(this.page, [/\bFREE\b/i, /gói miễn phí/i, /free plan/i]);
  }

  async upgradeToPro() {
    await clickFirst([
      this.page.getByRole('button', { name: /upgrade.*pro|nâng cấp.*pro|chọn.*pro|pro/i }),
      this.page.getByRole('link', { name: /upgrade.*pro|nâng cấp.*pro|chọn.*pro|pro/i }),
      this.page.locator('[data-testid="upgrade-pro"]'),
      this.page.locator('[data-plan="PRO"] button'),
    ], 'nút nâng cấp gói Pro');
  }
}

module.exports = { PricingPage };
