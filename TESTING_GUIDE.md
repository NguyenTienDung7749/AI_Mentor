# Hướng dẫn kiểm thử toàn bộ AI Study Mentor

Tài liệu này dùng để kiểm tra bản nộp Assignment theo một quy trình có thể lặp lại. Mỗi mục cần ghi `PASS`, `FAIL` hoặc `BLOCKED`, kèm ảnh chụp và ghi chú nếu kết quả khác mong đợi.

## 1. Chuẩn bị

### Môi trường khuyến nghị

- Android Studio và Android SDK 36.
- Pixel 7 AVD, Android 15 (API 35), độ phân giải 1080 × 2400.
- Một máy ảo Android 7.0/API 24 nếu cần kiểm tra minSdk.
- JDK: `C:\Program Files\Android\Android Studio\jbr`.
- Internet ổn định, sau đó có khả năng bật/tắt Wi-Fi hoặc Airplane mode.
- `local.properties` có `GROQ_API_KEY`; thêm `MISTRAL_API_KEY` để kiểm tra câu hỏi ảnh.
- Một ứng dụng Authenticator hỗ trợ TOTP để kiểm tra xác thực hai bước.

### Dữ liệu kiểm thử gợi ý

- Họ tên: `ASM Tester`
- Email: `asm.test.<ngaygio>@example.com` để tránh trùng tài khoản
- Mật khẩu hợp lệ: dùng ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
- Câu hỏi AI: `Explain why the derivative of x squared is 2x.`
- Câu hỏi offline: `Explain the offline-first pattern in Android.`
- Ảnh: một bài toán rõ nét, không chứa dữ liệu cá nhân

Trước một vòng kiểm thử sạch, xóa dữ liệu ứng dụng bằng Android Settings hoặc:

```powershell
adb shell pm clear com.example.aimentor
```

## 2. Kiểm thử tự động

Tại thư mục gốc dự án:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Kết quả đạt yêu cầu:

- Tất cả unit test và instrumented test đều pass, không skip.
- Lint không có error; warning thư viện cũ hoặc overdraw đã biết phải được đọc và ghi nhận.
- Có `app-debug.apk` và APK release thu gọn trong `app/build/outputs/apk/`.
- Release build sẽ fail có chủ đích nếu thiếu hoặc sai định dạng `GROQ_API_KEY`.

Lưu ý: instrumented test có thể gỡ/cài lại ứng dụng và làm mất dữ liệu đang có trên máy thử.

## 3. Cài đặt và smoke test

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| SM-01 | Cài `app-debug.apk` và mở ứng dụng | Mở màn hình Log in, không crash, không màn hình trắng |
| SM-02 | Xoay dọc/ngang rồi quay lại dọc | Nội dung còn đọc được, không bị cắt CTA chính |
| SM-03 | Nhấn Back ở màn hình đăng nhập | Ứng dụng thoát bình thường |
| SM-04 | Đóng và mở lại ứng dụng | Trạng thái phiên được giữ đúng: chưa đăng nhập vẫn ở Log in |

## 4. Giao diện xác thực

Kiểm tra ở light mode và dark mode nếu thiết bị đang kế thừa giao diện hệ thống.

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| AU-01 | Quan sát màn hình Log in | Logo BTEC rõ, không méo; biểu tượng AI đồng bộ; bố cục thoáng, không còn khung biểu mẫu lớn |
| AU-02 | Quan sát Email, Password | Nhãn, icon đầu dòng và password toggle rõ; vùng chạm tối thiểu 48dp |
| AU-03 | Nhấn `Create account` | Mở Sign up; Sign up cũng có logo BTEC và nhận diện giống Log in |
| AU-04 | Tăng font hệ thống lên 130% | Tiêu đề, trường nhập và hai nút vẫn đọc/chạm được sau khi cuộn |
| AU-05 | Bật TalkBack hoặc Accessibility Scanner | Heading được nhận diện; logo có mô tả; icon trang trí không bị đọc thừa |

## 5. Đăng ký và onboarding

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| RG-01 | Bỏ trống các trường rồi nhấn Sign up | Không tạo tài khoản; thông báo lỗi dễ hiểu |
| RG-02 | Nhập email sai định dạng | Không tạo tài khoản |
| RG-03 | Nhập mật khẩu yếu | Chỉ báo strength đổi màu/nội dung và giải thích yêu cầu |
| RG-04 | Nhập hai mật khẩu khác nhau | Hiển thị lỗi không khớp |
| RG-05 | Dùng email đã tồn tại | Không tạo bản ghi người dùng trùng |
| RG-06 | Nhập dữ liệu hợp lệ | Tạo tài khoản, tự đăng nhập và chuyển đến Onboarding |
| OB-01 | Thử tiếp tục khi thiếu lựa chọn bắt buộc | Không chuyển màn hình, có hướng dẫn hoàn thiện |
| OB-02 | Chọn education level, ít nhất một subject và explanation style | Lưu thành công, chuyển đến Home |
| OB-03 | Nhấn Back tại Home ngay sau onboarding | Không quay lại Login hoặc Onboarding |

## 6. App bar và điều hướng chính

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| NV-01 | Quan sát thẻ đầu trang | Card bo góc, có chiều sâu nhẹ, logo AI cân đối, không dính mép màn hình |
| NV-02 | Chuyển lần lượt Home, Library, Quiz, Progress, Settings | Chip bên phải cập nhật đúng tên tab; không nhấp nháy hoặc chồng chữ |
| NV-03 | Kiểm tra bottom navigation | Icon/label active rõ ràng; mỗi lần chạm mở đúng một tab |
| NV-04 | Thử font 130% và dark mode | Tên app, dòng phụ và chip còn đọc được; màu có độ tương phản tốt |
| NV-05 | Chuyển tab liên tục 20 lần | Không crash, không tạo fragment lỗi hoặc mất trạng thái bất thường |

## 7. Home và câu hỏi AI văn bản

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| HM-01 | Mở Home sau onboarding | Hiện lời chào, môn/phong cách đã chọn và kế hoạch học hôm nay |
| HM-02 | Gửi câu hỏi trống | Không gửi; hiển thị hướng dẫn nhập câu hỏi |
| HM-03 | Chọn subject/style và gửi câu hỏi hợp lệ khi online | Có trạng thái loading, chống nhấn gửi lặp, sau đó mở Answer |
| HM-04 | Kiểm tra Answer | Có đáp án trực tiếp, bước giải, giải thích, khái niệm/lỗi thường gặp hoặc phần tương đương; Markdown/công thức không lộ ký tự rác |
| HM-05 | Quay lại Home | Câu hỏi vừa hỏi xuất hiện trong dữ liệu Library/Progress; XP tăng đúng một lần |
| HM-06 | Bật tùy chọn dùng đáp án đã lưu rồi hỏi lại chính xác cùng câu/profile | Mở đáp án cache; không tạo lịch sử/XP trùng không hợp lý |
| HM-07 | Tắt dùng cache hoặc thay đổi nội dung/profile | Gửi yêu cầu mới, không tái dùng đáp án gần giống |
| HM-08 | Nhấn gửi nhiều lần hoặc xoay màn hình lúc đang tải | Không tạo nhiều đáp án cho cùng một thao tác |

## 8. Câu hỏi có ảnh

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| IM-01 | Nhấn Add image lần đầu | Hiện thông báo về chuyển ảnh cho provider và lưu pending khi offline |
| IM-02 | Từ chối/đóng consent | Không mở picker và không gửi ảnh |
| IM-03 | Đồng ý rồi chọn Gallery | Mở photo picker; ảnh đã chọn có preview/trạng thái phù hợp |
| IM-04 | Chọn Camera trên thiết bị có camera | Mở camera, trả ảnh về ứng dụng và không lỗi FileProvider |
| IM-05 | Hủy picker/camera | Trở lại Home ổn định, không crash |
| IM-06 | Gửi ảnh bài tập cùng câu hỏi khi có Mistral key | Nhận Answer phù hợp với nội dung ảnh |
| IM-07 | Thử ảnh quá lớn/sai hoặc thiếu Mistral key | Có lỗi rõ ràng/fallback an toàn; không treo vô hạn |

Không dùng ảnh chứa email, số điện thoại, giấy tờ hoặc dữ liệu nhạy cảm.

## 9. Offline queue và đồng bộ lại

1. Tắt mạng hoặc bật Airplane mode.
2. Ở Home, gửi câu hỏi văn bản ở mục dữ liệu kiểm thử.
3. Xác nhận ứng dụng báo câu hỏi đang chờ kết nối.
4. Mở Library và xác nhận số item pending đúng.
5. Force-stop ứng dụng, mở lại trong khi vẫn offline; pending phải còn vì được lưu trong Room.
6. Bật mạng, chờ WorkManager chạy và theo dõi notification/trạng thái.
7. Mở Library: pending biến mất và đáp án đã lưu xuất hiện.
8. Mở Answer và Progress: nội dung hợp lệ, XP chỉ cộng một lần.
9. Bật/tắt mạng thêm một lần trong lúc đồng bộ để kiểm tra retry và chống trùng.

FAIL nếu câu hỏi mất sau restart, tạo hai đáp án, cộng XP hai lần hoặc pending bị kẹt khi mạng đã ổn định.

## 10. Library và review

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| LB-01 | Tìm theo từ khóa trong câu hỏi/đáp án | Chỉ hiện mục phù hợp; xóa từ khóa khôi phục danh sách |
| LB-02 | Lọc theo subject | Kết quả đúng môn; empty state rõ khi không có dữ liệu |
| LB-03 | Chuyển All/Due/Learning/Mastered | Các chip dùng cùng trạng thái review, số lượng hợp lý |
| LB-04 | Bookmark một Answer | Icon/trạng thái đổi và được giữ sau khi đóng/mở app |
| LB-05 | Mark reviewed | Trạng thái review/mastery và XP cập nhật đúng, không cộng lặp khi thao tác lại |
| LB-06 | Mở item trong Library | Mở đúng Answer của đúng tài khoản |

## 11. Quiz

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| QZ-01 | Chọn subject, độ khó và số câu rồi tạo quiz online | Loading rõ; quiz mở khi hoàn tất |
| QZ-02 | Kiểm tra cả bốn loại câu | Multiple choice, true/false, short answer, fill blank đều nhập/trả lời được |
| QZ-03 | Trả lời đúng và sai | Feedback tức thì, màu/icon không phải dấu hiệu duy nhất; explanation khớp đáp án |
| QZ-04 | Để timer hết | Câu được xử lý nhất quán, không treo |
| QZ-05 | Hoàn thành quiz | Hiện điểm, XP, mastery movement và khuyến nghị tiếp theo |
| QZ-06 | Chọn retry mistakes | Chỉ các câu sai cần làm lại |
| QZ-07 | Mất mạng hoặc provider trả dữ liệu lỗi | Retry/fallback quiz cục bộ; không hiển thị quiz malformed |
| QZ-08 | Đóng/mở hoặc xoay trong quiz | Không crash; trạng thái không bị nhân đôi bất thường |

## 12. Progress và gamification

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| PG-01 | So sánh XP trước/sau hỏi bài, review và quiz | XP tăng theo đúng nghiệp vụ, không nhân đôi |
| PG-02 | Kiểm tra level, badge và streak | Phản ánh dữ liệu hoạt động hiện có |
| PG-03 | Kiểm tra weekly activity và subject mastery | Số liệu/biểu đồ có nhãn dễ hiểu và đúng subject |
| PG-04 | Bật tham gia local leaderboard | Chỉ hiện tên và XP của tài khoản opt-in trên thiết bị; không có email |
| PG-05 | Tắt opt-in | Tài khoản không còn được đưa vào bảng xếp hạng |

## 13. Settings, nhắc lịch và bảo mật

| Mã | Thao tác | Kết quả mong đợi |
|---|---|---|
| ST-01 | Đổi avatar, theme Scholar/Ocean/Forest/Sunset và light/dark/system | Giao diện cập nhật, giữ sau restart, không giảm khả năng đọc |
| ST-02 | Bật reminder, chọn giờ và từng loại nội dung | Cấu hình được giữ; WorkManager được lên lịch đúng |
| ST-03 | Android 13+ từ chối quyền notification | App không crash; Settings giải thích được trạng thái |
| ST-04 | Cho phép và gửi notification test | Notification xuất hiện đúng channel; chạm mở đúng màn hình |
| ST-05 | Export study summary | Android share sheet mở với nội dung tóm tắt, không tự gửi đi |
| ST-06 | Clear study history | Có dialog xác nhận; xóa câu hỏi/quiz/XP nhưng giữ tài khoản |
| ST-07 | Logout | Về Login; Back không mở lại Menu; đăng nhập lại vẫn đúng tài khoản |

### Xác thực hai bước

1. Trong Settings, bật authenticator verification.
2. Nhập setup key vào ứng dụng Authenticator và xác nhận bằng mã sáu số hiện tại.
3. Logout rồi đăng nhập bằng email + password.
4. Xác nhận chưa nhập mã thì không thể tạo session.
5. Nhập mã sai/hết hạn: ở lại dialog và báo lỗi.
6. Nhập mã đúng: vào Home.
7. Quay lại Settings, tắt 2FA bằng một mã hợp lệ.
8. Logout/login lần nữa: không còn dialog 2FA.

### Xóa tài khoản

1. Tạo một tài khoản thử riêng và phát sinh ít nhất một Answer, bookmark, quiz, pending hoặc cài đặt.
2. Chọn Delete account, thử mật khẩu sai rồi hủy: dữ liệu phải còn.
3. Thực hiện lại với mật khẩu đúng và xác nhận.
4. Ứng dụng về Login; đăng nhập cũ thất bại.
5. Dùng tài khoản khác trên cùng thiết bị: dữ liệu của tài khoản đã xóa không xuất hiện.

## 14. Chất lượng giao diện và khả năng truy cập

Chạy danh sách này trên Login, Sign up, Home, Answer, Library, Quiz, Progress và Settings:

- Light mode, dark mode và từng bảng màu đều có độ tương phản tốt.
- Font 100% và 130%; không cắt tiêu đề, CTA hoặc nội dung quan trọng.
- Màn hình 1080 × 2400 và một màn hình nhỏ/API 24 nếu có.
- Không có text đè nhau, card sát mép, icon méo, khoảng trắng bất thường hoặc thanh cuộn ngang.
- Loading, empty, error, offline, disabled, selected và success state đều nhận biết được.
- Vùng chạm chính tối thiểu 48dp; bàn phím không che nút mà không thể cuộn tới.
- TalkBack đọc heading, nhãn trường, trạng thái password strength, button và ảnh nội dung hợp lý.
- Back stack nhất quán; không có màn hình cũ bị lộ sau login/logout/onboarding.

## 15. Ổn định, bảo mật và kích thước APK

| Mã | Kiểm tra | Kết quả mong đợi |
|---|---|---|
| NF-01 | Không mạng, DNS lỗi, timeout, HTTP lỗi | Có thông báo/fallback; không crash hoặc loading vô hạn |
| DT-01 | Hai tài khoản trên cùng thiết bị | Không đọc được lịch sử, bookmark, quiz, pending hoặc 2FA của nhau |
| DT-02 | Kiểm tra Android backup | Manifest chặn backup; dữ liệu không tự đồng bộ qua cloud backup |
| DT-03 | Kiểm tra logcat khi nhập mật khẩu/API | Không log password, TOTP seed, ảnh hoặc API key |
| PF-01 | Cold start, chuyển tab và mở Library có dữ liệu | Không ANR; thao tác cơ bản phản hồi mượt trên máy ảo mục tiêu |
| SZ-01 | So kích thước release với mốc đã xác minh | Không tăng bất thường; resource shrinking và minify vẫn hoạt động |
| SG-01 | Chuẩn bị phát hành thật | APK/AAB phải dùng keystore của chủ dự án; không dùng bản unsigned |

## 16. Biểu mẫu ghi kết quả

```text
Build/commit:
Thiết bị và API:
Kích thước màn hình:
Theme / font scale:
Thời gian bắt đầu - kết thúc:

Nhóm test | PASS | FAIL | BLOCKED | Ghi chú / ảnh bằng chứng
Smoke      |      |      |         |
Auth       |      |      |         |
Onboarding |      |      |         |
Home/AI    |      |      |         |
Image      |      |      |         |
Offline    |      |      |         |
Library    |      |      |         |
Quiz       |      |      |         |
Progress   |      |      |         |
Settings   |      |      |         |
2FA/Data   |      |      |         |
UI/A11y    |      |      |         |

Lỗi còn mở:
Rủi ro chấp nhận:
Kết luận GO / NO-GO:
Người kiểm thử:
```

Chỉ chọn `GO` khi toàn bộ luồng bắt buộc pass, không còn crash/ANR, không có lỗi Lint, build release thành công và mọi giới hạn prototype đã được ghi rõ trong báo cáo.
