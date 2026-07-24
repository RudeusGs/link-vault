const { expect } = require('@playwright/test');

async function firstVisible(candidates, description) {
  for (const candidate of candidates) {
    const locator = candidate.first();
    const count = await locator.count().catch(() => 0);
    if (!count) continue;
    const visible = await locator.isVisible().catch(() => false);
    if (visible) return locator;
  }

  throw new Error(`Không tìm thấy phần tử hiển thị: ${description}`);
}

async function fillFirst(candidates, value, description) {
  const locator = await firstVisible(candidates, description);
  await locator.fill(value);
  return locator;
}

async function clickFirst(candidates, description) {
  const locator = await firstVisible(candidates, description);
  await expect(locator).toBeEnabled();
  await locator.click();
  return locator;
}

async function expectBodyContains(page, patterns) {
  const bodyText = await page.locator('body').innerText();
  const matched = patterns.some((pattern) => pattern.test(bodyText));
  expect(matched, `Nội dung trang không khớp các mẫu: ${patterns.join(', ')}`).toBeTruthy();
}

module.exports = {
  firstVisible,
  fillFirst,
  clickFirst,
  expectBodyContains,
};
