# ⚡ FTPApplication — Hệ thống Truyền Tệp FTP Server & Client (FileZilla Style + MySQL)

Dự án **FTPApplication** là hệ thống máy chủ và ứng dụng máy khách truyền tệp tin hoàn chỉnh, tuân thủ chuẩn giao thức **FTP RFC 959**, tích hợp cơ sở dữ liệu **MySQL** và hỗ trợ cơ chế xác thực kết nối nhanh theo phong cách **PeerChat** (kết nối bằng Mã Passcode và Tên hiển thị, không cần đăng ký tài khoản rườm rà).

---

## 🌟 1. Điểm nổi bật & Tính năng chính

1. **Cơ chế Kết nối Kiểu PeerChat (Passcode & Bí danh):**
   - Người dùng không cần tạo tài khoản hay đăng ký mật khẩu phức tạp.
   - Khi mở FTP Client, màn hình đăng nhập yêu cầu: `Địa chỉ Máy chủ (IP)`, `Cổng (Port)`, `Mã kết nối (Passcode - mặc định: FTP888)` và `Tên hiển thị (Bí danh)`.
   - Máy chủ FTP có hiển thị và cho phép tùy biến Mã kết nối (Passcode). Khi nhập đúng mã, người dùng được cấp quyền truy cập ngay lập tức.
   - Máy chủ tự động ghi nhận phiên đăng nhập vào bảng `active_sessions` trong CSDL MySQL.
2. **Giao diện Light Modern High-Legibility (Chuẩn Design-Kit):**
   - Thiết kế tông màu sáng sạch sẽ (`#F8FAFC`, `#FFFFFF`), chữ đen sắc nét (`#0F172A`), tương phản tuyệt đối, dễ nhìn khi thuyết trình.
   - Triệt tiêu hoàn toàn hiện tượng text bị cắt thành `...`: các cột tên tệp, dung lượng, quyền hạn, thời gian đều có độ rộng tối ưu và tự động co giãn (`TableView.CONSTRAINED_RESIZE_POLICY`).
3. **Giao thức chuẩn FTP RFC 959:**
   - Hỗ trợ đầy đủ kênh điều khiển (Control Port `2121`) và kênh truyền dữ liệu bị động (Passive Mode `PASV`).
   - Các lệnh FTP: `USER`, `PASS`, `PASV`, `LIST`, `CWD`, `PWD`, `CDUP`, `TYPE`, `RETR` (Tải về), `STOR` (Tải lên), `DELE`, `MKD`, `RMD`, `SIZE`, `QUIT`.
4. **Giao diện Client Dual-Pane (Chuẩn FileZilla):**
   - Khung duyệt thư mục máy tính (Local Site) và thư mục máy chủ (Remote Site) song song.
   - Hàng đợi truyền tải (Transfer Queue) với thanh ProgressBar, hiển thị dung lượng và tốc độ truyền.
   - Console log hiển thị luồng dữ liệu giao thức FTP theo thời gian thực.
5. **Máy chủ FTP Server Dashboard:**
   - Quản lý bật/tắt máy chủ, cấu hình cổng port và mã passcode.
   - Giám sát danh sách Client đang kết nối thời gian thực (`active_sessions`).
   - Nhật ký truyền tệp tự động lưu vào MySQL (`transfer_logs`).

---

## 🛠️ 2. Cấu trúc Cây Thư mục

```text
FTPApplication/
├── db.properties               # Cấu hình kết nối MySQL (host, port, user, pass)
├── pom.xml                     # Cấu hình Maven (JavaFX 21, MySQL Connector/J 8.3.0)
├── START-SERVER.bat / .vbs     # 1-Click khởi chạy FTP Server Dashboard
├── START-CLIENT.bat / .vbs     # 1-Click khởi chạy FTP Client
├── server_storage/files/       # Thư mục lưu trữ tệp tin gốc của Server
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/fiwpr06/ftpapplication/
    │   │       ├── common/
    │   │       │   ├── model/      # FileInfo, FtpReplyCode, TransferTask, TransferStatus
    │   │       │   └── util/       # AlertHelper, NetworkUtil, DateTimeUtil, FileUtils
    │   │       ├── client/
    │   │       │   ├── ClientApp.java, ClientLauncher.java
    │   │       │   ├── controller/ # ConnectController.java, FtpClientController.java
    │   │       │   ├── model/      # LocalFileItem.java, RemoteFileItem.java
    │   │       │   └── network/    # FtpClient.java (RFC 959 Socket Engine)
    │   │       └── server/
    │   │           ├── ServerApp.java, ServerLauncher.java
    │   │           ├── command/    # FtpCommand interface & các lệnh FTP
    │   │           ├── controller/ # FtpServerController.java (Dashboard GUI)
    │   │           ├── core/       # FtpServer.java, FtpSessionHandler.java, PassiveDataChannel.java
    │   │           └── database/   # DatabaseConfig, FtpDatabaseManager, UserDao, TransferLogDao
    │   └── resources/
    │       └── com/fiwpr06/ftpapplication/
    │           ├── css/            # client.css, server.css (Light Modern Theme)
    │           └── fxml/           # connect_view.fxml, client_view.fxml, server_view.fxml
    └── test/
        └── java/com/fiwpr06/ftpapplication/
            ├── DatabaseTest.java           # Kiểm thử kết nối và CRUD MySQL
            ├── FtpProtocolTest.java        # Kiểm thử Handshake RFC 959
            └── FtpClientIntegrationTest.java # Kiểm thử Upload/Download/List/Delete
```

---

## 🚀 3. Hướng dẫn Khởi chạy Nhanh

### Bước 1: Khởi động FTP Server
1. Chạy `START-SERVER.bat` (hoặc `mvnw javafx:run -Pserver`).
2. Giao diện Server hiển thị: Cổng lắng nghe `2121`, Mã Passcode mặc định `FTP888`.
3. Bấm **▶ Bật Server**.

### Bước 2: Khởi động FTP Client
1. Chạy `START-CLIENT.bat` (hoặc `mvnw javafx:run -Pclient`).
2. Màn hình đăng nhập hiển thị:
   - IP: `127.0.0.1` (hoặc IP mạng LAN của máy chủ)
   - Port: `2121`
   - Passcode: `FTP888`
   - Bí danh: Nhập tên bạn (ví dụ: `Alice` hoặc `Bình Minh`)
3. Bấm **KẾT NỐI MÁY CHỦ FTP** -> Giao diện FileZilla mở ra và tự động tải thư mục của máy chủ.
4. Kéo thả hoặc chọn tệp rồi bấm **Tải lên** / **Tải về** để truyền tệp.
