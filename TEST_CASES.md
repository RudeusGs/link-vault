# Danh sách test case đã triển khai

| Mã | Chức năng | Loại | Kết quả mong muốn |
|---|---|---|---|
| L-01 | Mở landing khi chưa đăng nhập | E2E smoke | Hiển thị landing, không redirect login |
| L-02 | CTA Login | E2E smoke | Điều hướng `/login` |
| L-03 | CTA Register/Get Started | E2E smoke | Điều hướng auth flow |
| A-01 | Form đăng nhập | E2E | Có username/email, password, submit |
| A-02 | Login dữ liệu trống | E2E | Vẫn ở `/login`, không đăng nhập |
| A-03 | Login tài khoản test | E2E thật | Đăng nhập và rời `/login` |
| O-01 | Tạo workspace đầu tiên | E2E + mock API | POST workspace với đúng tên |
| O-02 | Workspace name trống | E2E + mock API | Không tạo workspace |
| P-01 | Hiển thị plan hiện tại | E2E + mock API | Hiển thị FREE |
| P-02 | Upgrade FREE -> PRO | E2E + mock API | Gửi API upgrade target PRO |
| I-01 | Mở invitation token | E2E + mock API | Hiển thị workspace/inviter |
| I-02 | Accept invitation | E2E + mock API | Gửi API accept |
| I-03 | Decline invitation | E2E + mock API | Gửi API decline |
| W-01 | Invite member | E2E + mock API | Gửi đúng email người được mời |
| S-01 | Backend health | API smoke | HTTP thành công, status UP/OK |
