const { test, expect } = require('@playwright/test');

test.describe('E2E Smoke Test', () => {
  // Use a unique email for each run
  const uniqueId = Date.now();
  const testUser = {
    firstName: 'Smoke',
    lastName: 'Tester',
    email: `smoke.${uniqueId}@example.com`,
    password: 'Password123!',
  };

  test('Full flow: Register, Create Vault, Create Resource, Share Link', async ({ page }) => {
    // 1. Register
    await page.goto('/register');
    
    // Fill in registration form
    await page.getByLabel(/first name|tên/i).first().fill(testUser.firstName);
    await page.getByLabel(/last name|họ/i).first().fill(testUser.lastName);
    await page.getByLabel(/email/i).first().fill(testUser.email);
    
    // Try both label and placeholder for password
    const pwdInput = page.getByLabel(/password|mật khẩu/i).first();
    if (await pwdInput.isVisible()) {
      await pwdInput.fill(testUser.password);
    } else {
      await page.locator('input[type="password"]').first().fill(testUser.password);
    }

    // Confirm password if exists
    const confirmPwdInput = page.getByLabel(/confirm password|xác nhận mật khẩu/i).first();
    if (await confirmPwdInput.isVisible()) {
        await confirmPwdInput.fill(testUser.password);
    }
    
    // Submit registration
    await page.getByRole('button', { name: /register|đăng ký/i }).click();

    // Verify registration success (should navigate to login or dashboard)
    await expect(page).not.toHaveURL(/\/register/, { timeout: 15_000 });

    // Login if redirected to login
    if (page.url().includes('/login')) {
      await page.getByLabel(/email|username|tên đăng nhập/i).first().fill(testUser.email);
      await page.getByLabel(/password|mật khẩu/i).first().fill(testUser.password);
      await page.getByRole('button', { name: /đăng nhập|login/i }).click();
      await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 });
    }

    // 2. Create Vault
    // Navigate to vaults page or find 'New Vault' button
    await page.goto('/dashboard');
    const newVaultBtn = page.getByRole('button', { name: /new vault|thêm vault|tạo vault/i }).first();
    
    // If not found, try to go to /vaults directly
    if (!(await newVaultBtn.isVisible())) {
      await page.goto('/vaults');
    }
    
    await page.getByRole('button', { name: /new vault|thêm vault|tạo vault/i }).first().click();
    
    // Fill vault form
    const vaultName = `Smoke Vault ${uniqueId}`;
    await page.getByLabel(/name|tên/i).first().fill(vaultName);
    await page.getByLabel(/description|mô tả/i).first().fill('Created by automated smoke test');
    await page.getByRole('button', { name: /create|lưu|tạo/i }).first().click();
    
    // Verify vault creation
    await expect(page.getByText(vaultName).first()).toBeVisible({ timeout: 10_000 });

    // 3. Create Resource
    // Click on the created vault
    await page.getByText(vaultName).first().click();
    
    // Click Add Resource
    await page.getByRole('button', { name: /add resource|thêm tài nguyên|tạo tài nguyên/i }).first().click();
    
    // Fill resource form (Link type as it's easier than file upload)
    const resourceName = `Smoke Resource ${uniqueId}`;
    await page.getByLabel(/title|tên/i).first().fill(resourceName);
    await page.getByLabel(/url|đường dẫn/i).first().fill('https://example.com');
    await page.getByRole('button', { name: /create|lưu|tạo/i }).first().click();
    
    // Verify resource creation
    await expect(page.getByText(resourceName).first()).toBeVisible({ timeout: 10_000 });

    // 4. Generate Share Link
    // Find the share button for the resource
    // Assume there is a menu or share button on the resource item
    const resourceItem = page.locator(`text=${resourceName}`).first().locator('..');
    const shareBtn = resourceItem.getByRole('button', { name: /share|chia sẻ/i }).first();
    
    if (await shareBtn.isVisible()) {
        await shareBtn.click();
    } else {
        // Alternatively, maybe it's in a dropdown
        await resourceItem.getByRole('button', { name: /more|thêm/i }).first().click();
        await page.getByRole('menuitem', { name: /share|chia sẻ/i }).first().click();
    }
    
    // In share modal, create link
    await page.getByRole('button', { name: /create link|tạo link/i }).first().click();
    
    // Verify link is created (usually a copy button appears)
    await expect(page.getByRole('button', { name: /copy|sao chép/i }).first()).toBeVisible({ timeout: 10_000 });
  });
});
