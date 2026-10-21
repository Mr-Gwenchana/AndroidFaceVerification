# Nhật ký Tiến độ Dự án (Project Progress Log)

---

### Tuần 1: Khởi tạo Kiến trúc & Hoàn thiện Giao diện Ứng dụng
- **07/10/2026:** Khởi tạo repository, cấu hình Gradle 9.6, AGP 9.4, Kotlin 2.2, Android 10 (API 29) và khóa cứng màn hình ngang `sensorLandscape`.
- **10/10/2026:** Chuẩn hóa thư viện qua Gradle Version Catalog `libs.versions.toml` (Compose BOM 2026.02.01, CameraX 1.3.4, Navigation).
- **14/10/2026:** Hoàn thiện kiến trúc Single-Activity với `MainActivity.kt`, thiết lập bảng màu Material 3 và khung điều hướng tổng thể.
- **16/10/2026:** Hoàn thiện UI `HomeScreen` với thanh điều hướng dọc `SideNavBar`, `SideNavItem` và đồng hồ kỹ thuật số `DigitalClock` thời gian thực.
- **17/10/2026:** Xây dựng màn hình `SettingsScreen` (form cấu hình MQTT) và hoàn thiện hệ thống định tuyến tập trung `AppNavigation`.
- **18/10/2026:** Thiết kế layout chia đôi 50:50 cho `FaceRecognitionScreen` với 2 khung `PreviewView` bo góc 20dp và nút back tiện lợi.

---

### Tuần 2: Tích hợp Đấu nối Camera & Tối ưu Hóa Vòng đời
- **19/10/2026 (Sáng):** Tích hợp CameraX, đấu nối thành công luồng Dual Camera Preview (`ConcurrentCamera`) mở đồng thời 2 ống kính RGB và IR.
- **19/10/2026 (Chiều):** Bổ sung cơ chế Fallback tự động sang Single Camera trong `CameraManager` nếu phần cứng thiết bị không hỗ trợ concurrent stream.
- **19/10/2026 (Tối):** Xử lý race condition trong `LaunchedEffect` của `FaceRecognitionScreen` và nạp bất đồng bộ `ProcessCameraProvider` qua `await()`.
- **20/10/2026:** Hoàn thiện cơ chế giải phóng phần cứng tức thì khi unmount qua `DisposableEffect`, nghiên cứu xử lý vòng đời khi OS thu hồi quyền.
- **21/10/2026 (Sáng):** Tái cấu trúc logic quyền Camera theo State-Holder pattern (`CameraPermission` + `rememberInstOfCameraPermission`), tách sạch khỏi UI.
- **21/10/2026 (Chiều):** Cập nhật tài liệu `prj-structure-now.md`, biên soạn báo cáo kỹ thuật `Report_1.md` và chuẩn hóa kế hoạch 12 tuần `main-plan.md`.
