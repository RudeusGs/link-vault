const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const projectRoot = process.cwd();
const packagePath = path.join(projectRoot, 'package.json');
const noInstall = process.argv.includes('--no-install');

if (!fs.existsSync(packagePath)) {
  console.error('Không tìm thấy package.json. Hãy chạy script này tại thư mục gốc frontend/project Node.js.');
  process.exit(1);
}

const backupPath = path.join(projectRoot, 'package.json.before-playwright.bak');
if (!fs.existsSync(backupPath)) fs.copyFileSync(packagePath, backupPath);

const packageJson = JSON.parse(fs.readFileSync(packagePath, 'utf8'));
packageJson.scripts = packageJson.scripts || {};
Object.assign(packageJson.scripts, {
  'test:e2e': 'playwright test',
  'test:e2e:headed': 'playwright test --headed',
  'test:e2e:ui': 'playwright test --ui',
  'test:e2e:debug': 'playwright test --debug',
  'test:e2e:report': 'playwright show-report',
  'test:e2e:install': 'playwright install chromium',
});

fs.writeFileSync(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`);
console.log('Đã cập nhật scripts trong package.json. Bản sao lưu:', backupPath);

if (noInstall) {
  console.log('Đã bỏ qua cài package do có cờ --no-install.');
  process.exit(0);
}

function run(command, args) {
  const result = spawnSync(command, args, { stdio: 'inherit', shell: process.platform === 'win32' });
  if (result.status !== 0) process.exit(result.status || 1);
}

run('npm', ['install', '--save-dev', '@playwright/test']);
run('npx', ['playwright', 'install', 'chromium']);
console.log('\nHoàn tất. Tiếp theo:');
console.log('1) Sao chép .env.playwright.example thành .env.playwright');
console.log('2) Chỉnh URL/command theo dự án');
console.log('3) Chạy npm run test:e2e');
