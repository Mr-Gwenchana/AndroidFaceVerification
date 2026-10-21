# Tiến độ tuần 1 + 2

---

## 1. Các công việc đã hoàn thành

Trong giai đoạn 2 tuần đầu tiên (Phase 1), dự án đã hoàn thành toàn bộ nền tảng cốt lõi về cấu hình dự án, giao diện người dùng, điều hướng và hệ thống quản lý Camera phần cứng:

### 1.1. Khởi tạo Môi trường & Thiết lập Kiến trúc Dự án
- **Cấu hình Build System:** Khởi tạo dự án Android Native sử dụng Gradle `9.6.0`, Android Gradle Plugin (AGP) `9.4.1`, Kotlin `2.2.10` và Gradle Version Catalog ([`gradle/libs.versions.toml`](file:///E:/Project-3/gradle/libs.versions.toml)) giúp quản lý tập trung toàn bộ thư viện dependencies.
- **Tối ưu cấu hình mục tiêu:** Ứng dụng nhắm tới phần cứng tablet nhúng thực tế có cấu hình hạn chế (Android 10 - API 29, 1 GB RAM, 8 GB bộ nhớ trong). Khóa cứng định hướng màn hình ngang (`sensorLandscape`) trong [`AndroidManifest.xml`](file:///E:/Project-3/app/src/main/AndroidManifest.xml) phù hợp với quy cách lắp đặt tablet.
- **Kiến trúc Single-Activity:** Sử dụng [`MainActivity.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/MainActivity.kt) duy nhất kết hợp hoàn toàn với Jetpack Compose (Edge-to-Edge), loại bỏ hoàn toàn các layout XML truyền thống.

### 1.2. Thiết kế & Xây dựng Giao diện Người dùng (UI Framework)
- **Màn hình chính ([`HomeScreen.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/HomeScreen.kt)):**
  - Bố cục chia đôi theo chiều ngang (`Row` Layout).
  - Thanh điều hướng cạnh bên ([`SideNavBar.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/components/SideNavBar.kt), [`SideNavItem.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/components/SideNavItem.kt)) với icon vector tối giản, độ phản hồi cảm ứng nhạy, tối ưu cho thao tác một chạm trên tablet.
  - Đồng hồ kỹ thuật số ([`DigitalClock.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/components/DigitalClock.kt)) cập nhật thời gian thực mỗi giây với kích thước chữ lớn (88sp) và hiển thị ngày tháng đầy đủ bằng tiếng Việt.
- **Màn hình Cài đặt ([`SettingsScreen.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/SettingsScreen.kt)):**
  - Biểu mẫu cấu hình các tham số kết nối MQTT Broker (Host, Port, Topic, Client ID).
  - Tích hợp Scaffold, TopAppBar, bàn phím số/chữ tương ứng từng trường thông tin và nút lưu cấu hình.
- **Hệ thống Điều hướng ([`AppNavigation.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/AppNavigation.kt)):**
  - Cấu hình luồng điều hướng tập trung sử dụng `NavHost` và `rememberNavController`.
  - Định nghĩa các Route chuẩn hóa tại [`MainNavRoutes`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/const/MainNavItemConst.kt) (`HOME`, `FACE_RECOGNITION`, `SETTINGS`).
- **Màn hình Quét khuôn mặt ([`FaceRecognitionScreen.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/FaceRecognitionScreen.kt)):**
  - Thiết kế layout chia đôi màn hình (Dual-view) tỉ lệ cân đối 50:50 với khoảng cách đệm 20dp và bo góc 20dp hiện đại, sẵn sàng hiển thị luồng hình ảnh từ 2 camera vật lý (Camera RGB và Camera Hồng ngoại IR).
  - Tích hợp nút quay lại (`IconButton`) có nền mờ bán trong suốt ở góc trên cùng bên trái.

### 1.3. Triển khai Hệ thống Quản lý Camera & Quyền truy cập
- **Quản lý Quyền theo mô hình State-Holder ([`CameraPermission.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraPermission.kt)):**
  - Đóng gói toàn bộ logic xin quyền `Manifest.permission.CAMERA`, đăng ký `rememberLauncherForActivityResult`, và theo dõi trạng thái phản ứng `hasCameraPermission`.
  - Cung cấp Composable factory [`rememberInstOfCameraPermission()`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraPermission.kt#L40-L61) giúp tách biệt hoàn toàn boilerplate xin quyền ra khỏi code giao diện.
- **Bộ điều khiển CameraX tập trung ([`CameraManager.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraManager.kt)):**
  - Quản lý vòng đời `ProcessCameraProvider` thông qua coroutine bất đồng bộ (`await()`).
  - Hỗ trợ luồng `ConcurrentCamera` để mở và truyền tín hiệu hình ảnh đồng thời lên 2 `PreviewView` riêng biệt khi thiết bị có 2 camera vật lý trở lên.
  - Cơ chế fallback tự động: nếu thiết bị chỉ có 1 camera hoặc không hỗ trợ concurrent stream ở mức phần cứng, hệ thống tự động unbind và fallback an toàn sang Single Camera thay vì làm crash ứng dụng.
- **Quản lý Vòng đời & Giải phóng Tài nguyên:**
  - Kết hợp chặt chẽ với `LifecycleOwner` của Compose.
  - Sử dụng `DisposableEffect(Unit)` để tự động gọi `cameraManager.unbindAll()` ngay khi người dùng bấm quay lại hoặc màn hình bị unmount, lập tức trả lại tài nguyên phần cứng camera cho hệ điều hành.

---

## 2. Các công nghệ đã sử dụng (Thư viện, Tool, Dependency)

Toàn bộ thư viện được khai báo chuẩn hóa trong file [`gradle/libs.versions.toml`](file:///E:/Project-3/gradle/libs.versions.toml) và tích hợp vào [`app/build.gradle.kts`](file:///E:/Project-3/app/build.gradle.kts):

| Nhóm công nghệ | Tên công cụ / Thư viện | Phiên bản | Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| **Hệ thống Build** | **Gradle** | `9.6.0` | Công cụ tự động hóa quá trình biên dịch dự án. |
| | **Android Gradle Plugin (AGP)** | `9.4.1` | Plugin Gradle chính thức của Android. |
| | **Kotlin Compiler & Plugin** | `2.2.10` | Trình biên dịch ngôn ngữ Kotlin thế hệ mới kèm Compose Compiler Plugin. |
| | **Java Compatibility** | `Java 11` | Cấu hình tương thích mã bytecode trên thiết bị Android 10. |
| **Giao diện Người dùng (UI)** | **Jetpack Compose BOM** | `2026.02.01` | Quản lý đồng bộ phiên bản các thư viện Compose UI. |
| | **Compose Material 3** | BOM | Hệ thống thành phần giao diện theo chuẩn thiết kế Material 3. |
| | **Compose Material Icons Extended** | BOM | Bộ sưu tập đầy đủ các vector icon hệ thống. |
| | **Activity Compose** | `1.8.0` | Cầu nối giữa Activity truyền thống (`ComponentActivity`) và `setContent { }` của Compose. |
| **Điều hướng & Kiến trúc** | **Navigation Compose** | `2.7.7` | Định tuyến và chuyển đổi màn hình trong ứng dụng Single-Activity. |
| | **Lifecycle ViewModel Compose** | `2.6.1` | Tích hợp vòng đời `LifecycleOwner` và quản lý trạng thái theo vòng đời. |
| **Phần cứng Camera** | **CameraX Core & Camera2** | `1.3.4` | Tầng trừu tượng điều khiển phần cứng camera ở cấp độ cao, hỗ trợ Camera2 interop. |
| | **CameraX Lifecycle & View** | `1.3.4` | Tự động đồng bộ camera theo vòng đời và cung cấp view hiển thị `PreviewView`. |
| | **Concurrent Futures KTX** | `1.2.0` | Cung cấp hàm mở rộng `await()` chuyển đổi `ListenableFuture` sang Kotlin Coroutines. |
| **Lưu trữ Cấu hình** | **DataStore Preferences** | `1.1.1` | Thư viện lưu trữ key-value bất đồng bộ (chuẩn bị lưu thông số MQTT ở Phase sau). |

---

## 3. Các đặc tính kỹ thuật chi tiết & Lý do thiết kế

### 3.1. Mô hình State-Holder Pattern trong Quản lý Quyền Camera

#### Lớp và Hàm triển khai:
- **Class [`CameraPermission`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraPermission.kt#L16-L38):**
  - **Thuộc tính & Constructor:**
    - `private val context: Context`: Dùng để gọi API kiểm tra quyền hệ thống.
    - `private val launcher: ActivityResultLauncher<String>`: Kênh kích hoạt hộp thoại xin quyền của Android OS.
    - `private val hasPermissionState: MutableState<Boolean>`: Nắm giữ Snapshot State của Compose, lưu trữ trạng thái cấp quyền hiện tại.
  - **Thuộc tính phản ứng `hasCameraPermission`:**
    ```kotlin
    val hasCameraPermission: Boolean
        get() = hasPermissionState.value
    ```
    Mỗi khi một Composable (như [`FaceRecognitionScreen`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/FaceRecognitionScreen.kt)) đọc getter này, Compose runtime sẽ ngầm đăng ký Composable đó theo dõi `hasPermissionState`. Khi quyền thay đổi, màn hình sẽ tự động được recompose.
  - **Hàm `isGranted(): Boolean`:** Gọi `ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED` để truy vấn trực tiếp từ hệ thống.
  - **Hàm `grant(): Unit`:** Kiểm tra nếu `!isGranted()` thì gọi `launcher.launch(permission)` để bật hộp thoại yêu cầu người dùng cấp quyền.

- **Composable Factory [`rememberInstOfCameraPermission()`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraPermission.kt#L40-L61):**
  - Khởi tạo giá trị ban đầu cho `hasPermissionState` bằng `ContextCompat.checkSelfPermission`.
  - Khởi tạo `launcher` thông qua `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. Khi người dùng nhấn "Cho phép" (Allow), callback được kích hoạt và gán `hasPermissionState.value = isGranted`.
  - Trả về đối tượng `CameraPermission` được bọc trong `remember(context, launcher) { ... }` để giữ nguyên instance qua các lần recompose.

#### Lý do thiết kế:
- **Tuân thủ nguyên tắc Đơn nhiệm (Single Responsibility Principle - SRP):** Tách toàn bộ các chi tiết cấp thấp của Android Framework (`ActivityResultLauncher`, hợp đồng xin quyền, `ContextCompat`) ra khỏi UI. Màn hình chỉ nhận một đối tượng điều khiển duy nhất và thuộc tính `hasCameraPermission`.
- **Dễ tái sử dụng và kiểm thử:** Bất kỳ màn hình nào cần sử dụng Camera sau này chỉ cần gọi đúng một dòng: `val cameraPermission = rememberInstOfCameraPermission()`.

---

### 3.2. Điều khiển Vòng đời Giao diện với Compose Effects và AndroidView

#### Lớp và Hàm triển khai trong [`FaceRecognitionScreen.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/FaceRecognitionScreen.kt):
1. **Khởi tạo và Xin quyền (`LaunchedEffect(Unit)` - L53-L56):**
   ```kotlin
   LaunchedEffect(Unit) {
       cameraPermission.grant()
       isCameraInitialized = cameraManager.initialize()
   }
   ```
   - **Đặc tính:** Key `Unit` đảm bảo coroutine này chỉ chạy **duy nhất một lần** khi màn hình lần đầu đi vào cây Composition (Initial Composition). Không bao giờ chạy lại khi recompose.
   - **Tác vụ:** Tự động kích hoạt luồng xin quyền camera và khởi tạo sẵn `ProcessCameraProvider` trong bộ nhớ.
2. **Kích hoạt Camera Đa điều kiện (`LaunchedEffect(keys)` - L58-L71):**
   ```kotlin
   LaunchedEffect(hasCameraPermission, isCameraInitialized, previewView1, previewView2) {
       val view1 = previewView1 ?: return@LaunchedEffect
       val view2 = previewView2 ?: return@LaunchedEffect
       if (!hasCameraPermission || !isCameraInitialized) return@LaunchedEffect

       cameraManager.startCameras(...)
   }
   ```
   - **Đặc tính:** Khối Effect nhận 4 keys điều kiện. Khi bất kỳ key nào thay đổi giá trị (ví dụ: quyền chuyển từ `false` sang `true`, hoặc cả 2 `PreviewView` vừa hoàn tất khởi tạo layout), khối code sẽ được kích hoạt lại.
   - **Bảo vệ luồng (Guard Clauses):** Bốn dòng đầu kiểm tra tính toàn vẹn (khác null và bằng true). Nếu chưa đủ điều kiện, coroutine thoát sớm (`return@LaunchedEffect`), ngăn chặn triệt để lỗi Null Pointer hoặc bind camera khi chưa có quyền.
3. **Giải phóng Phần cứng Tức thì (`DisposableEffect(Unit)` - L73-L77):**
   ```kotlin
   DisposableEffect(Unit) {
       onDispose {
           cameraManager.unbindAll()
       }
   }
   ```
   - **Đặc tính:** Gắn kết chặt chẽ với vòng đời Composable. Khi người dùng bấm nút back để rời màn hình, khối `onDispose` ngay lập tức được gọi, ngắt kết nối pipeline camera và giải phóng tài nguyên phần cứng.
4. **Hiển thị Camera bằng `AndroidView` với chế độ `COMPATIBLE` (L97-L126):**
   - **Đặc tính:** Khởi tạo `PreviewView` của CameraX trong lambda `factory` và gán cấu hình:
     ```kotlin
     scaleType = PreviewView.ScaleType.FILL_CENTER
     implementationMode = PreviewView.ImplementationMode.COMPATIBLE
     ```
   - **Lý do thiết kế:** Mặc định `PreviewView` dùng `PERFORMANCE` (dựa trên `SurfaceView`). Trên Android 10 nhúng với GPU yếu, `SurfaceView` nằm ở Z-layer độc lập nên không thể bo góc bằng `Modifier.clip(RoundedCornerShape(20.dp))` và thường xuyên bị lỗi đen màn hình khi mở 2 view đồng thời. Cấu hình `COMPATIBLE` buộc CameraX dùng `TextureView`, tích hợp hoàn toàn vào hệ thống đồ họa của Compose, cho phép bo góc 20dp mượt mà và hiển thị đồng thời 2 luồng camera ổn định.

---

### 3.3. Mẫu thiết kế Singleton (Double-Checked Locking & Quản lý Tài nguyên Phần cứng)

#### Lớp và Hàm triển khai trong [`CameraManager.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraManager.kt):
```kotlin
class CameraManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val TAG = "CameraManager"

        @Volatile
        private var instance: CameraManager? = null

        fun getInstance(context: Context): CameraManager {
            return instance ?: synchronized(this) {
                instance ?: CameraManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
```

#### Phân tích chi tiết và Lý do thiết kế:
- **Private Constructor:** Ngăn chặn việc tùy tiện khởi tạo đối tượng từ bên ngoài bằng toán tử `CameraManager(context)`.
- **Double-Checked Locking với `@Volatile`:**
  - `@Volatile` đảm bảo các thao tác ghi lên biến `instance` sẽ hiển thị ngay lập tức (visibility) tới tất cả các CPU Core khác nhau, tránh hiện tượng Instruction Reordering của trình biên dịch.
  - Khối `synchronized(this)` đảm bảo chỉ một luồng duy nhất được phép khởi tạo đối tượng trong trường hợp nhiều luồng gọi `getInstance` cùng lúc. Lần check thứ hai bên trong block ngăn chặn việc tạo trùng instance.
- **Bảo vệ tài nguyên phần cứng độc quyền (Exclusive Physical Resource):** Camera vật lý là tài nguyên đơn điểm của hệ điều hành. Nếu tồn tại nhiều instance quản lý `ProcessCameraProvider`, chúng sẽ tranh chấp quyền điều khiển lens và surface, dẫn đến ngoại lệ `CameraAccessException` hoặc treo cảm biến phần cứng.
- **Chống rò rỉ bộ nhớ (Memory Leak Prevention):** Biến `appContext` luôn lưu `context.applicationContext`. Nhờ đó, instance Singleton tồn tại theo vòng đời Application mà không bao giờ giữ tham chiếu tới `Activity Context` (nguyên nhân hàng đầu gây memory leak trên Android khi xoay màn hình hoặc đóng mở Activity).

---

### 3.4. Nguyên lý Đảo ngược Phụ thuộc (Dependency Inversion Principle - DIP) & Phân tách Module

#### Hiện thực chi tiết trong mã nguồn:
1. **Ở Tầng Giao diện (Presentation Layer):**
   - [`FaceRecognitionScreen`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/FaceRecognitionScreen.kt) nhận tham số `onNavigateBack: () -> Unit`. Màn hình hoàn toàn không biết đến sự tồn tại của `NavController` hay `NavHost`.
   - [`HomeScreen`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/HomeScreen.kt) nhận tham số `onNavigate: (String) -> Unit` và danh sách `navItems: List<NavItemModel>`.
   - Toàn bộ việc quyết định chuyển trang hay lùi trang được đảo ngược quyền điều khiển (Inversion of Control) cho [`AppNavigation.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/screens/AppNavigation.kt) đảm nhận.
   - **Lợi ích:** Màn hình trở thành thành phần thuần túy (Stateless/Decoupled), có thể chạy độc lập trong Compose Preview hoặc viết UI Test tự động mà không cần mock `NavController`.
2. **Ở Tầng Dịch vụ Camera (Service/Data Layer):**
   - Hàm [`CameraManager.startCameras(...)`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraManager.kt#L33-L121) đóng vai trò là một **Facade Pattern**:
     - Che giấu toàn bộ sự phức tạp của Android Camera2 API (`SystemCameraManager.cameraIdList`), bộ lọc `CameraSelector.addCameraFilter`, cấu hình `ConcurrentCamera.SingleCameraConfig`, và nhóm ca sử dụng `UseCaseGroup`.
     - Cung cấp API cấp cao với các cờ điều khiển: `enablePreview: Boolean`, `enableDualCamera: Boolean`, `enableImageAnalysis: Boolean`, `analyzer: ImageAnalysis.Analyzer?`.
   - **Lợi ích:** Khi bước sang Phase xử lý Face Detection, ta chỉ cần truyền implementation của `ImageAnalysis.Analyzer` vào `analyzer` mà không cần sửa đổi bất kỳ logic nội bộ nào của `CameraManager` hay `FaceRecognitionScreen`.

---

### 3.5. Kiến trúc Đa luồng (Threading, Concurrency & Tối ưu RAM 1 GB)

#### Phân chia luồng xử lý:
1. **Luồng Giao diện (Main / UI Thread):**
   - Chịu trách nhiệm render Composable, xử lý cử chỉ chạm và điều hướng.
   - Không chứa bất kỳ câu lệnh I/O hay blocking nào.
2. **Luồng Khởi tạo Bất đồng bộ (Coroutine Suspension):**
   - Hàm [`CameraManager.initialize()`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/camera/CameraManager.kt#L22-L31) là một `suspend fun`:
     ```kotlin
     suspend fun initialize(): Boolean {
         if (cameraProvider != null) return true
         return try {
             cameraProvider = ProcessCameraProvider.getInstance(appContext).await()
             true
         } catch (e: Exception) {
             Log.e(TAG, "Lỗi khởi tạo ProcessCameraProvider", e)
             false
         }
     }
     ```
     Nhờ extension `await()` của thư viện `androidx.concurrent:concurrent-futures-ktx`, luồng Main được tạm hoãn (suspend) một cách tự nhiên trong khi CameraProvider khởi tạo trên background thread. Khi hoàn tất, kết quả trả về mà không làm đơ giao diện hay gây lỗi ANR (Application Not Responding).
3. **Luồng Cập nhật Thời gian Thực trong [`DigitalClock.kt`](file:///E:/Project-3/app/src/main/java/com/bachelor/projectiii/components/DigitalClock.kt#L34-L39):**
   ```kotlin
   LaunchedEffect(Unit) {
       while (true) {
           currentTime = Date()
           delay(1000L)
       }
   }
   ```
   - Chạy trên một coroutine độc lập, tạm dừng không chặn luồng (`delay(1000L)` thay vì `Thread.sleep`) để cập nhật state đồng hồ đúng chu kỳ 1 giây.
   - Các đối tượng định dạng `SimpleDateFormat` được bọc trong `remember { ... }` (L41-L42), triệt tiêu việc cấp phát lại bộ nhớ sau mỗi giây, bảo vệ tối đa heap memory.
4. **Chiến lược Backpressure Chống tràn Bộ nhớ trong `ImageAnalysis`:**
   - Trong `CameraManager`, pipeline phân tích ảnh sử dụng:
     ```kotlin
     ImageAnalysis.Builder()
         .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
         .build()
     ```
   - **Lý do thiết kế:** Phần cứng chỉ có 1 GB RAM. Nếu bộ nhận diện khuôn mặt xử lý một frame mất 50ms trong khi camera nạp về 30fps (33ms/frame), hàng đợi sẽ bị dồn ứ nếu dùng chiến lược đệm thông thường (BUFFER). Chiến lược `STRATEGY_KEEP_ONLY_LATEST` lập tức hủy bỏ các frame cũ và chỉ giữ frame mới nhất, giữ mức sử dụng RAM cố định và tránh tuyệt đối hiện tượng Out-Of-Memory (OOM).
