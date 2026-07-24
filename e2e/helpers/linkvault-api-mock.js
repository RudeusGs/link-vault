function json(route, status, body) {
  return route.fulfill({
    status,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify(body),
  });
}

async function mockLinkVaultApi(page, options = {}) {
  const state = {
    plan: options.plan || 'FREE',
    createdWorkspace: null,
    upgradedPlan: null,
    acceptedInvitation: false,
    declinedInvitation: false,
    invitedEmail: null,
  };

  const user = options.user || {
    id: 'user-e2e-001',
    username: 'playwright.user',
    email: 'playwright@example.com',
    displayName: 'Playwright User',
  };

  const workspace = options.workspace || {
    id: 'ws-e2e-001',
    name: 'Demo Workspace',
    description: 'Workspace dùng cho Playwright automation test',
    role: 'OWNER',
    plan: state.plan,
  };

  const invitation = options.invitation || {
    token: 'e2e-valid-token',
    workspaceId: workspace.id,
    workspaceName: workspace.name,
    inviterName: 'Workspace Owner',
    invitedEmail: user.email,
    role: 'MEMBER',
    status: 'PENDING',
    expiresAt: '2026-12-31T23:59:59Z',
  };

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const method = request.method().toUpperCase();
    const url = new URL(request.url());
    const path = url.pathname.replace(/\/$/, '');

    if (method === 'OPTIONS') return route.fulfill({ status: 204, body: '' });

    if (method === 'GET' && /\/api\/(auth\/me|users\/me|profile)$/.test(path)) {
      return json(route, 200, user);
    }

    if (method === 'GET' && path === '/api/health') {
      return json(route, 200, { status: 'UP' });
    }

    if (method === 'GET' && path === '/api/workspaces') {
      const workspaces = options.noWorkspace ? [] : [
        { ...workspace, plan: state.plan },
      ];
      return json(route, 200, workspaces);
    }

    if (method === 'POST' && path === '/api/workspaces') {
      const body = request.postDataJSON?.() || {};
      state.createdWorkspace = {
        id: 'ws-created-e2e',
        name: body.name || body.workspaceName || 'E2E Workspace',
        description: body.description || '',
        role: 'OWNER',
        plan: 'FREE',
      };
      return json(route, 201, state.createdWorkspace);
    }

    if (method === 'GET' && path === `/api/workspaces/${workspace.id}`) {
      return json(route, 200, { ...workspace, plan: state.plan });
    }

    if (method === 'GET' && path === `/api/workspaces/${workspace.id}/plan`) {
      return json(route, 200, {
        currentPlan: state.plan,
        plan: state.plan,
        limits: { maxVaults: state.plan === 'FREE' ? 3 : 50, maxResources: state.plan === 'FREE' ? 100 : 5000, maxMembers: state.plan === 'FREE' ? 2 : 25 },
        usage: { vaults: 1, resources: 12, members: 1 },
      });
    }

    if (method === 'GET' && path === `/api/workspaces/${workspace.id}/usage`) {
      return json(route, 200, { vaults: 1, resources: 12, members: 1 });
    }

    if (method === 'POST' && path === `/api/workspaces/${workspace.id}/plan/upgrade`) {
      const body = request.postDataJSON?.() || {};
      const targetPlan = body.targetPlan || body.plan || 'PRO';
      state.plan = String(targetPlan).toUpperCase();
      state.upgradedPlan = state.plan;
      return json(route, 200, { ...workspace, plan: state.plan, currentPlan: state.plan });
    }

    if (method === 'GET' && path === `/api/workspaces/${workspace.id}/invitations`) {
      return json(route, 200, []);
    }

    if (method === 'POST' && path === `/api/workspaces/${workspace.id}/invitations`) {
      const body = request.postDataJSON?.() || {};
      state.invitedEmail = body.email || body.username || body.invitedEmail || null;
      return json(route, 201, {
        id: 'inv-e2e-new',
        token: 'e2e-new-token',
        invitedEmail: state.invitedEmail,
        role: body.role || 'MEMBER',
        status: 'PENDING',
      });
    }

    if (method === 'GET' && path === `/api/workspace-invitations/${invitation.token}`) {
      return json(route, 200, invitation);
    }

    if (method === 'POST' && path === `/api/workspace-invitations/${invitation.token}/accept`) {
      state.acceptedInvitation = true;
      return json(route, 200, { ...invitation, status: 'ACCEPTED', workspace });
    }

    if (method === 'POST' && path === `/api/workspace-invitations/${invitation.token}/decline`) {
      state.declinedInvitation = true;
      return json(route, 200, { ...invitation, status: 'DECLINED' });
    }

    if (method === 'GET') return json(route, 200, []);
    return json(route, 404, { code: 'E2E_MOCK_NOT_FOUND', message: `Chưa mock API ${method} ${path}` });
  });

  return state;
}

module.exports = { mockLinkVaultApi };
