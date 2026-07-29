# AI Study Mentor

AI Study Mentor là một ứng dụng Android bằng Java được tạo ra cho kịch bản học tập BrightPath trong bài tập BTEC Unit 22: Application Development (Phát triển Ứng dụng). Ứng dụng kết hợp trợ lý học tập được hỗ trợ bởi Groq với tính năng OCR trên thiết bị, các bài trắc nghiệm được cá nhân hóa, lịch sử cục bộ, theo dõi tiến độ và nhắc nhở học tập theo lịch trình.

Kho lưu trữ này chứa mã nguồn ứng dụng. Các phần phân tích bài tập, minh chứng kiểm thử, ảnh chụp màn hình, đánh giá chéo và tự đánh giá thuộc về báo cáo bài tập nộp riêng thay vì nằm trong cây mã nguồn này.

## Tính năng

| Khu vực | Triển khai hiện tại |
|---|---|
| Tài khoản | Đăng ký, đăng nhập, hướng dẫn người dùng mới (onboarding) và quản lý phiên cục bộ |
| Câu trả lời từ AI | Câu trả lời có cấu trúc bằng ngôn ngữ của câu hỏi với phân loại môn học và độ khó |
| Định tuyến mô hình (Model routing) | Lựa chọn cục bộ giữa `llama-3.1-8b-instant` và `llama-3.3-70b-versatile` |
| Độ tin cậy | Cho phép thử lại mô hình thay thế có giới hạn, thời hạn phản hồi tổng cộng là 60 giây và nội dung dự phòng khi ngoại tuyến dựa trên quy tắc |
| OCR | Nhập ảnh từ thư viện hoặc camera, sử dụng ML Kit OCR trên thiết bị và có thể chỉnh sửa văn bản được trích xuất |
| Lịch sử | Tìm kiếm, bộ lọc môn học, đánh dấu (bookmark) và trạng thái "đã ôn tập" được lưu trữ bằng Room |
| Trắc nghiệm (Quiz) | Cá nhân hóa theo chủ đề/lịch sử, độ khó thích ứng, trắc nghiệm nhiều lựa chọn, đúng/sai, trả lời ngắn, điền vào chỗ trống, giải thích và thử lại các câu sai |
| Tiến độ | Thống kê thực tế qua Room, thời gian ôn tập, biểu đồ độ chính xác 7/30 ngày, các chủ đề lặp lại, tổng số theo môn học, XP, cấp độ và huy hiệu |
| Thông báo | Nhắc nhở hàng ngày bằng WorkManager do người dùng kiểm soát, có thể chọn thời gian và có thông báo chạy thử nghiệm |
| Giao diện | Material UI, chế độ sáng/tối, mở khóa avatar/chủ đề (theme), các trạng thái đang tải/lỗi/trống và phục hồi trạng thái |

Chỉ những câu trả lời thành công qua mạng (remote) mới được lưu vào Room. Nội dung dự phòng ngoại tuyến là tạm thời và không được tính vào lịch sử, tiến độ hay điểm XP. Mỗi khi gửi một câu hỏi luôn bắt đầu một yêu cầu hoàn toàn mới thay vì sử dụng lại câu trả lời cũ.

## Công nghệ

- Tương thích với mã nguồn Java 11
- Android SDK: tối thiểu (min) 24, compile/target 36
- Android Gradle Plugin 8.11.2 và Gradle 8.13
- AndroidX, Material Components và ViewPager2
- Room để lưu trữ dữ liệu cục bộ
- Retrofit, OkHttp và Gson cho API tương thích với OpenAI của Groq
- ML Kit Text Recognition cho tính năng OCR trên thiết bị
- WorkManager cho các nhắc nhở hàng ngày chạy ngầm
- JUnit, MockWebServer, AndroidX Test và Espresso

## Cấu trúc dự án

```text
app/src/main/java/com/example/aimentor/
|-- activities/    Màn hình đăng nhập, đăng ký, hướng dẫn, trả lời và trắc nghiệm
|-- Fragments/     Trang chủ, lịch sử, thiết lập trắc nghiệm và cài đặt
|-- adapters/      Adapter cho ViewPager và danh sách câu hỏi
|-- ai/            Các engine kết nối mạng/cục bộ, phân tích cú pháp, phân loại và mô hình trắc nghiệm
|-- data/          Room entity, DAO, cơ sở dữ liệu và migration
|-- network/       Service Retrofit cho Groq và các mô hình request/response
|-- repo/          Repository bất đồng bộ cho người dùng và tiến độ học
|-- util/          Các hàm tiện ích cho bảo mật, xác thực, game hóa và nhắc nhở
`-- worker/        Worker chạy lịch nhắc nhở học tập
```

## Cấu hình cục bộ

Ứng dụng không yêu cầu sinh viên cung cấp API key. Nhà phát triển tự cung cấp key trên máy cục bộ khi build.

Tạo hoặc cập nhật file `local.properties` ở thư mục gốc:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GROQ_API_KEY=thay-bang-groq-key-cua-ban
MISTRAL_API_KEY=thay-bang-mistral-key-cua-ban
```

File `local.properties` được Git bỏ qua (ignore). Tuyệt đối không đặt key thật trong code Java, XML, ảnh chụp màn hình, issue, báo cáo hay các commit.

Bản mẫu (prototype) debug đọc các key này vào `BuildConfig`. Các câu hỏi chỉ có văn bản sẽ sử dụng:

```text
https://api.groq.com/openai/v1/chat/completions
```

Các câu hỏi có đính kèm 1 hình ảnh sử dụng:

```text
https://api.mistral.ai/v1/chat/completions
```

Điều này chỉ được chấp nhận đối với một nguyên mẫu được kiểm soát trong môi trường lớp học bằng cách sử dụng dữ liệu giả. Bất kỳ giá trị nào biên dịch vào APK đều có thể bị trích xuất. Phiên bản production bắt buộc phải giữ credential của nhà cung cấp trên một backend/proxy đáng tin cậy và xác thực người dùng ứng dụng trước khi chuyển tiếp yêu cầu.

Nếu `GROQ_API_KEY` trống, ứng dụng sẽ sử dụng `LocalAiEngine`. Khi một request từ xa được cấu hình bị thất bại do lỗi tạm thời (transient error) đủ điều kiện, ứng dụng có thể thử lại bằng mô hình Groq thay thế một lần trong cùng thời hạn 60 giây, và sau đó hiển thị dự phòng ngoại tuyến mà không bị crash.

## Build và chạy

Yêu cầu (Prerequisites):

- Android Studio với Android SDK/platform 36
- JDK 17 hoặc JBR đi kèm với Android Studio
- Thiết bị thật hoặc máy ảo Android chạy API 24 trở lên

Trên Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat verifyReleaseConfiguration assembleRelease
```

Trên macOS/Linux:

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Tệp APK debug sẽ được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Các bản build Release yêu cầu phải có một `GROQ_API_KEY` hợp lệ cục bộ, kích hoạt tối ưu hóa/làm rối mã code bằng R8 và loại bỏ các resource không sử dụng. APK release được tạo ra chưa được ký (unsigned) trừ khi nhà phát triển cung cấp cấu hình ký riêng tư bên ngoài repository.

Mở dự án trong Android Studio, chọn thiết bị và chạy cấu hình `app`. Luồng sử dụng lần đầu thông thường là:

```text
Đăng ký -> Hướng dẫn (Onboarding) -> Trang chủ
```

## Kiểm thử (Testing)

Các bài test JVM bao phủ:

- Phân tích cú pháp phản hồi (response parsing) và kết quả có cấu trúc song ngữ
- Lựa chọn mô hình cục bộ và ánh xạ các lần thử lại/lỗi
- Thời hạn dự phòng chung 60 giây
- Xác thực, mã hóa và kiểm tra mật khẩu
- Phân loại môn học, chấm điểm trắc nghiệm, XP, cấp độ và huy hiệu
- Giới hạn nhập liệu/OCR và tính toán thời gian nhắc nhở

Các instrumented test bao phủ:

- Các thao tác với Room repository và cách ly người dùng
- Lưu trữ câu trả lời qua mạng và không lưu trữ khi ngoại tuyến
- Cập nhật (mutations) bất đồng bộ và nguyên tử đối với điểm XP/ôn tập/trắc nghiệm
- Các callback bất đồng bộ của repository
- Phục hồi trạng thái ViewModel và ngăn chặn gửi trùng lặp yêu cầu
- Lập lịch, hủy và thiết lập kênh thông báo bằng WorkManager độc nhất

Chạy instrumented tests với thiết bị đã kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Sử dụng thiết bị hoặc profile kiểm thử chuyên dụng: task test của Android Gradle có thể cài đặt hoặc gỡ cài đặt ứng dụng debug và do đó sẽ xóa các tài khoản cục bộ cũng như lịch sử Room.

## Ghi chú về dữ liệu và bảo mật

- Mật khẩu được lưu dưới dạng băm PBKDF2 có salt cho từng người dùng; các hàm băm cũ (legacy hashes) sẽ được tự động nâng cấp sau khi đăng nhập thành công.
- Truy cập vào dữ liệu Room trên luồng chính (main-thread) bị vô hiệu hóa. Hoạt động của Repository chạy trên các executor nền (background executors) và trả về kết quả cho luồng chính.
- Ảnh được xử lý hoàn toàn trên thiết bị. Việc nhận dạng văn bản (OCR) chạy cục bộ và chỉ phần văn bản sau khi đã chỉnh sửa mới được gửi tới nhà cung cấp AI.
- Câu hỏi nhập vào được giới hạn trong 6,000 ký tự.
- Tính năng sao lưu ứng dụng bị vô hiệu hóa và màn hình Cài đặt (Settings) cung cấp tính năng xóa tài khoản/dữ liệu triệt để.
- Các nhắc nhở hàng ngày là tùy chọn, sử dụng một job định kỳ duy nhất và sẽ bị hủy bỏ khi đăng xuất bình thường hoặc khi xóa tài khoản.
- Ứng dụng này là một bản nguyên mẫu (prototype) giáo dục và nên được sử dụng với dữ liệu giả định.

## Những hạn chế đã biết của nguyên mẫu (prototype limitations)

- Quá trình xác thực và mọi dữ liệu người dùng chỉ được lưu trên thiết bị (device-local); không có dịch vụ tài khoản đám mây hay tính năng đồng bộ hóa giữa các thiết bị.
- Groq key có thể bị trích xuất từ file APK đã build vì không có proxy backend bảo vệ.
- Các nhắc nhở của WorkManager giúp tiết kiệm pin và có tính bền bỉ nhưng không phải là báo thức chính xác; hệ thống Android có thể gửi chúng muộn hơn so với thời gian đã chọn.
- Các câu trả lời khi ngoại tuyến mang tính hướng dẫn học tập cố định, không phải là sự thay thế cho các mô hình ngôn ngữ trí tuệ nhân tạo trực tuyến.
- Quá trình ký phát hành (release signing), giám sát trong môi trường production và triển khai lên store ứng dụng nằm ngoài phạm vi nguyên mẫu của bài tập.
