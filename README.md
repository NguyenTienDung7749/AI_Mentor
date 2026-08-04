# AI Study Mentor

AI Study Mentor là ứng dụng Android hỗ trợ học tập cá nhân hóa, được xây dựng cho bài Assignment BTEC Unit 22 – Application Development. Ứng dụng kết hợp trợ lý AI, câu hỏi có ảnh, bài quiz nhiều định dạng, thư viện ôn tập, theo dõi tiến độ và nhắc lịch học trong một trải nghiệm Material 3 trẻ trung nhưng chuyên nghiệp.

## Trạng thái dự án

- Phiên bản: `1.0`
- Application ID: `com.example.aimentor`
- Nền tảng: Android 7.0 trở lên (`minSdk 24`, `targetSdk 36`)
- Ngôn ngữ giao diện: tiếng Anh
- Kiến trúc dữ liệu: local-first với Room; tài khoản và dữ liệu học tập nằm riêng theo từng người dùng
- Trạng thái nộp bài: sẵn sàng trình diễn và đưa vào báo cáo Assignment

Lần kiểm tra đầy đủ gần nhất đạt 75 unit test, 31 instrumented test, Android Lint không có lỗi và build release thu gọn thành công. Chi tiết đối chiếu yêu cầu nằm trong [ASSIGNMENT_VERIFICATION.md](ASSIGNMENT_VERIFICATION.md).

## Chức năng chính

### Tài khoản và cá nhân hóa

- Đăng ký, đăng nhập và duy trì phiên đăng nhập.
- Kiểm tra độ mạnh mật khẩu và xác nhận mật khẩu.
- Onboarding chọn trình độ học vấn, môn học quan tâm và phong cách giải thích.
- Xác thực hai bước TOTP tùy chọn; khóa thiết lập được mã hóa bằng Android Keystore.
- Đăng xuất, xóa tài khoản bằng xác nhận mật khẩu và cô lập dữ liệu giữa các tài khoản.

### Trợ lý học tập AI

- Gửi câu hỏi văn bản theo môn học và phong cách giải thích.
- Chọn ảnh từ thư viện hoặc chụp bằng camera để hỏi bài; hiển thị thông báo đồng ý trước khi chuyển ảnh cho nhà cung cấp AI.
- Trả lời có cấu trúc gồm đáp án trực tiếp, các bước giải, giải thích đơn giản, khái niệm chính, lỗi thường gặp và câu hỏi tiếp nối.
- Hiển thị Markdown và công thức toán học.
- Dùng Groq cho câu hỏi văn bản và Mistral Vision cho câu hỏi có ảnh.
- Có phương án trả lời cục bộ/fallback khi phản hồi từ xa không khả dụng hoặc không hợp lệ.

### Offline, lưu trữ và ôn tập

- Xếp hàng câu hỏi văn bản khi mất mạng; WorkManager tự gửi lại khi có kết nối.
- Chống tạo đáp án hoặc XP trùng lặp khi một yêu cầu được thử lại.
- Tùy chọn dùng lại đáp án đã lưu chỉ khi câu hỏi và hồ sơ học tập khớp chính xác.
- Thư viện hỗ trợ tìm kiếm, lọc môn học, bookmark và mở lại đáp án.
- Phân loại nội dung theo `All`, `Due`, `Learning`, `Mastered` và tạo kế hoạch học trong ngày.

### Quiz và tiến độ

- Quiz cá nhân hóa gồm bốn dạng: trắc nghiệm, đúng/sai, trả lời ngắn và điền vào chỗ trống.
- Phản hồi đáp án ngay lập tức, giải thích, tính điểm, XP và làm lại câu sai.
- Kiểm tra tính nhất quán giữa đáp án và lời giải từ AI; phản hồi lỗi sẽ được thử lại hoặc thay bằng quiz an toàn cục bộ.
- Theo dõi XP, cấp độ, huy hiệu, streak, hoạt động theo tuần, mastery theo môn và lịch sử quiz.
- Bảng xếp hạng cục bộ chỉ dành cho tài khoản đã chủ động tham gia và không hiển thị email.

### Cài đặt và quyền riêng tư

- Chế độ sáng/tối/theo hệ thống và bốn bảng màu: Scholar, Ocean, Forest, Sunset.
- Ảnh đại diện, nhắc lịch học, thông báo đến hạn ôn tập, chủ đề lặp lại, tổng kết tuần và đáp án sẵn sàng.
- Xuất bản tóm tắt dữ liệu học tập, xóa lịch sử nhưng giữ tài khoản, hoặc xóa toàn bộ tài khoản.
- Tắt Android backup; dữ liệu Room nằm trong vùng lưu trữ riêng của ứng dụng.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 11 |
| Giao diện | Material 3, ConstraintLayout, RecyclerView, ViewPager2 |
| Dữ liệu | Room 2.6.1 |
| Tác vụ nền | WorkManager 2.11.2 |
| Mạng | Retrofit 3, OkHttp 4, Gson |
| Nội dung | Markwon và LaTeX extension |
| Kiểm thử | JUnit 4, MockWebServer, AndroidX Test, Espresso |
| Build | Gradle 9.5, Android Gradle Plugin 9.3.1 |

## Cấu trúc dự án

```text
app/src/main/java/com/example/aimentor/
├── activities/   Màn hình đăng nhập, onboarding, menu, đáp án và quiz
├── Fragments/    Home, Library, Quiz, Progress và Settings
├── ai/           Mô hình dữ liệu, parser và AI engine từ xa/cục bộ
├── data/         Room database, entity và DAO
├── network/      Retrofit service cho Groq và Mistral
├── repo/         Nghiệp vụ tài khoản và học tập
├── util/         Bảo mật, giao diện, thông báo và tính toán tiến độ
└── worker/       Đồng bộ câu hỏi offline và nhắc học
```

Các tài liệu quan trọng:

- [ASSIGNMENT_VERIFICATION.md](ASSIGNMENT_VERIFICATION.md): đối chiếu yêu cầu, bằng chứng và giới hạn của sản phẩm.
- [IMPLEMENTATION_BATCHES.md](IMPLEMENTATION_BATCHES.md): lịch sử các batch nâng cấp.
- [TESTING_GUIDE.md](TESTING_GUIDE.md): quy trình kiểm thử tự động và thủ công toàn bộ ứng dụng.
- [`docs/evidence`](docs/evidence): ảnh bằng chứng dùng cho báo cáo.

## Chuẩn bị môi trường

Yêu cầu:

- Android Studio có Android SDK 36.
- JDK đi kèm Android Studio hoặc JDK tương thích với Gradle 9.5.
- Thiết bị thật hoặc máy ảo Android 7.0 trở lên; cấu hình kiểm tra chính là Pixel 7, Android 15 (API 35).
- Kết nối Internet để gọi AI.

Tạo hoặc cập nhật `local.properties` ở thư mục gốc:

```properties
sdk.dir=C\:\\Users\\<ten-nguoi-dung>\\AppData\\Local\\Android\\Sdk
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxx
MISTRAL_API_KEY=xxxxxxxxxxxxxxxxxxxx
```

`GROQ_API_KEY` là bắt buộc cho build release và luồng AI văn bản. `MISTRAL_API_KEY` cần cho câu hỏi có ảnh. Với phạm vi bài Assignment, khóa được đưa vào `BuildConfig` để dễ chạy demo. Không dùng cách này cho sản phẩm phát hành công khai; bản production thực tế cần backend proxy, giới hạn quota và cơ chế xoay khóa.

## Build và chạy

Mở thư mục dự án bằng Android Studio, chờ Gradle Sync hoàn tất, chọn thiết bị rồi chạy cấu hình `app`.

Có thể build bằng PowerShell tại thư mục gốc:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

APK debug nằm tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build release thu gọn:

```powershell
.\gradlew.bat assembleRelease
```

APK release mặc định chưa được ký để phát hành. Hãy cấu hình keystore riêng nếu cần cài bản release hoặc đưa lên cửa hàng.

## Kiểm thử nhanh

```powershell
# Unit test
.\gradlew.bat testDebugUnitTest

# Kiểm thử trên máy ảo/thiết bị đang kết nối
.\gradlew.bat connectedDebugAndroidTest

# Android Lint
.\gradlew.bat lintDebug

# Kiểm tra toàn bộ và build APK
.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest lintDebug assembleDebug assembleRelease
```

Lưu ý: `connectedDebugAndroidTest` có thể cài lại hoặc xóa dữ liệu ứng dụng trên thiết bị kiểm thử. Không chạy lệnh này trên thiết bị đang chứa dữ liệu cần giữ. Quy trình chi tiết và kết quả mong đợi có trong [TESTING_GUIDE.md](TESTING_GUIDE.md).

## Giới hạn đã biết

- API key trong APK có thể bị trích xuất; thiết kế hiện tại chỉ phù hợp cho bài tập và demo riêng tư.
- Bảng xếp hạng là cục bộ trên một thiết bị, không phải dịch vụ nhiều người dùng trực tuyến.
- Chất lượng và thời gian trả lời phụ thuộc nhà cung cấp AI và kết nối mạng.
- Bản release tạo từ dự án chưa có chữ ký của chủ sở hữu.
- Ứng dụng chưa có đồng bộ tài khoản/dữ liệu giữa nhiều thiết bị.

## Phạm vi sử dụng

Dự án phục vụ học tập và đánh giá Assignment BTEC. Không nhập dữ liệu cá nhân nhạy cảm hoặc dùng khóa API production có quota lớn trong bản demo.
