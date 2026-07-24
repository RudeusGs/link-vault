async function seedAuthenticatedUser(page, overrides = {}) {
  const authStorageKey = process.env.E2E_AUTH_STORAGE_KEY || 'token';
  const userStorageKey = process.env.E2E_USER_STORAGE_KEY || 'currentUser';
  const token = process.env.E2E_FAKE_TOKEN || 'e2e.mock.jwt.token';
  const user = {
    id: 'user-e2e-001',
    username: 'playwright.user',
    email: 'playwright@example.com',
    displayName: 'Playwright User',
    ...overrides,
  };

  await page.addInitScript(
    ({ authStorageKey, userStorageKey, token, user }) => {
      window.localStorage.setItem(authStorageKey, token);
      window.localStorage.setItem(userStorageKey, JSON.stringify(user));
    },
    { authStorageKey, userStorageKey, token, user },
  );
}

module.exports = { seedAuthenticatedUser };
