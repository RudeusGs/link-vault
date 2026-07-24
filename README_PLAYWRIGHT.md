# Playwright automation test cho LinkVault

Bộ file này được thiết kế dưới dạng **overlay**: giải nén và chép toàn bộ nội dung vào thư mục gốc của frontend/dự án Node.js. Bộ test không ghi đè `package.json`; script cài đặt sẽ sao lưu rồi bổ sung các lệnh Playwright an toàn.

## 1. Cài đặt

```bash
node scripts/setup-playwright.cjs
```

Script sẽ:

- Sao lưu `package.json` thành `package.json.before-playwright.bak`.
- Bổ sung các script `test:e2e`, `test:e2e:headed`, `test:e2e:ui`, `test:e2e:debug` và `test:e2e:report`.
- Cài `@playwright/test` và trình duyệt Chromium.

Chỉ muốn cập nhật `package.json` mà chưa cài package:

```bash
node scripts/setup-playwright.cjs --no-install
```

## 2. Cấu hình môi trường

Sao chép file mẫu:

```bash
cp .env.playwright.example .env.playwright
```

Trên Windows PowerShell:

```powershell
Copy-Item .env.playwright.example .env.playwright
```

Các biến quan trọng:

- `E2E_BASE_URL`: URL frontend.
- `E2E_START_COMMAND`: lệnh khởi động frontend; bỏ trống khi frontend đã chạy sẵn.
- `E2E_API_URL`: URL backend cho API health test.
- `E2E_USERNAME`, `E2E_PASSWORD`: tài khoản test thật; thiếu thì case login thành công tự skip.
- `E2E_AUTH_STORAGE_KEY`: key token trong localStorage. Mặc định là `token`.
- `E2E_ALL_BROWSERS=true`: chạy thêm Firefox và WebKit.

## 3. Chạy test

```bash
npm run test:e2e
npm run test:e2e:headed
npm run test:e2e:ui
npm run test:e2e:debug
npm run test:e2e:report
```

Chạy riêng một nhóm:

```bash
npx playwright test e2e/tests/03-onboarding.mock.spec.js
npx playwright test --grep "nâng cấp"
```

## 4. Các chức năng đã có automation

| Nhóm | Chức năng |
|---|---|
| Landing | Trang `/` mở được khi chưa đăng nhập; Login và Register CTA điều hướng đúng |
| Authentication | Kiểm tra form đăng nhập, dữ liệu trống, đăng nhập bằng tài khoản thật khi có biến môi trường |
| Onboarding | User mới tạo workspace đầu tiên; kiểm tra không gửi dữ liệu trống |
| Pricing | Hiển thị plan hiện tại và nâng cấp FREE lên PRO |
| Invitation | Mở token, chấp nhận và từ chối lời mời workspace |
| Workspace Settings | Owner mời thành viên bằng email |
| API smoke | Kiểm tra `/api/health` |

## 5. Cấu trúc

```text
playwright.config.js
.env.playwright.example
e2e/
  helpers/
    auth.js
    linkvault-api-mock.js
    locator-helpers.js
  pages/
    landing.page.js
    login.page.js
    onboarding.page.js
    pricing.page.js
    invitation.page.js
    workspace-settings.page.js
  tests/
    01-landing.spec.js
    02-login.spec.js
    03-onboarding.mock.spec.js
    04-pricing.mock.spec.js
    05-invitations.mock.spec.js
    06-workspace-settings.mock.spec.js
    07-api-health.spec.js
scripts/setup-playwright.cjs
.github/workflows/playwright-e2e.yml
```

## 6. Lưu ý khi ghép vào code thật

Bộ test được xây dựng theo các route đã mô tả trong tài liệu LinkVault: `/`, `/login`, `/onboarding`, `/pricing`, `/workspaces/:id/settings`, `/invitations/:token` và các API `/api/workspaces/...`.

Các page object đã có nhiều locator dự phòng bằng role, label, placeholder, `data-testid` và selector input phổ biến. Nếu tên nút hoặc API trong dự án khác, chỉ cần sửa file tương ứng trong `e2e/pages/` hoặc `e2e/helpers/linkvault-api-mock.js`, không phải sửa từng test case.

Nên bổ sung các `data-testid` ổn định sau vào giao diện để test ít phụ thuộc câu chữ:

- `login-username`, `login-password`, `login-submit`
- `workspace-name`, `create-workspace`
- `upgrade-pro`
- `accept-invitation`, `decline-invitation`
- `invite-email`, `invite-submit`

## 7. Dữ liệu test và an toàn

- Không dùng tài khoản production.
- Tạo workspace và email riêng cho môi trường test.
- Không commit `.env.playwright` chứa mật khẩu.
- Các file `*.mock.spec.js` mock API trên trình duyệt, phù hợp để kiểm tra frontend độc lập với backend.
- Case login thật và API health sẽ tự skip nếu chưa cấu hình thông tin tương ứng.
