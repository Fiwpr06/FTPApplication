# FTPApplication — Hệ thống FTP Server & Client (Chuẩn FileZilla + MySQL)
### Báo cáo Thực hiện Bài Lab 2 — Môn Học Lập Trình Mạng

Dự án **FTPApplication** được xây dựng toàn diện đáp ứng 100% các yêu cầu của **Bài Lab 2**:
* **Giao thức chuẩn FTP RFC 959:** Tách biệt 2 kênh kết nối: Kênh Điều khiển (Control Connection) trên cổng `2121` và Kênh Dữ liệu Chế độ Bị động (Passive Mode `PASV`).
* **Cơ sở dữ liệu MySQL:** Tự động kết nối, tự động khởi tạo cơ sở dữ liệu `ftp_db` cùng 2 bảng `users` (quản lý tài khoản, không hash mật khẩu để dễ demo) và `transfer_logs` (ghi nhận lịch sử truyền tải file thời gian thực).
* **Giao diện Client chuẩn FileZilla:** Bố cục Dual-Pane (Local Site & Remote Site), Quickconnect Bar, Status Console Log stream với mã phản hồi FTP (`220`, `331`, `230`, `257`, `250`, `227`, `150`, `226`,...), và hàng đợi truyền tải Transfer Queue với Progress Bar.
* **Giao diện Server Dashboard:** Quản lý bật/tắt Server, giám sát Client đang kết nối, thêm/xóa/phân quyền tài khoản trong MySQL và xem lịch sử truyền file.
* **Tương thích 100% với ứng dụng FileZilla thật:** Có thể mở phần mềm FileZilla trên máy và kết nối trực tiếp vào `127.0.0.1:2121`.

---

## 1. Cấu trúc Cây Thư mục (Chuẩn Default-Tree & Clean Architecture)

```text
FTPApplication/
├── db.properties               # Cấu hình kết nối MySQL (host, port, user, pass)
├── pom.xml                     # Cấu hình Maven (JavaFX 21, MySQL Connector/J 8.3.0)
├── START-SERVER.bat / .vbs     # 1-Click khởi chạy FTP Server Dashboard
├── START-CLIENT.bat / .vbs     # 1-Click khởi chạy FTP Client Dual-Pane
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
    │   │       │   ├── controller/ # FtpClientController.java
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
    │           ├── css/            # client.css, server.css (Cyberpunk Slate Theme)
    │           └── fxml/           # client_view.fxml, server_view.fxml
    └── test/
        └── java/com/fiwpr06/ftpapplication/
            ├── DatabaseTest.java           # Kiểm thử kết nối và CRUD MySQL
            ├── FtpProtocolTest.java        # Kiểm thử Handshake RFC 959
            └── FtpClientIntegrationTest.java # Kiểm thử trọn vẹn Upload/Download/List/Delete
```

---

## 2. Hướng dẫn Khởi chạy & Vận hành

### Cách 1: Khởi chạy 1-Click bằng Script (Khuyên dùng)
1. **Bật FTP Server:** Nhấn đúp chuột vào `START-SERVER.bat` (hoặc `START-SERVER.vbs` để chạy ngầm).
   - Nhấn nút **▶ Start Server** trên giao diện Server.
   - Trạng thái sẽ chuyển sang màu xanh: `RUNNING (PORT 2121)`.
2. **Bật FTP Client:** Nhấn đúp chuột vào `START-CLIENT.bat` (hoặc `START-CLIENT.vbs`).
   - Nhập thông tin Quickconnect (đã điền sẵn mặc định):
     - Host: `127.0.0.1`
     - Username: `admin` (hoặc `user`)
     - Password: `admin` (hoặc `123456`)
     - Port: `2121`
   - Nhấn **⚡ Quickconnect**.
   - Giao diện Dual-Pane sẽ tải danh sách file trên máy và trên Server.
   - Thử nghiệm:
     - Chọn file bên **Local Site** -> Nhấn **Upload ➡** để đẩy file lên Server.
     - Chọn file bên **Remote Site** -> Nhấn **⬅ Download** để tải file về máy.
     - Nhấn **➕ Dir** để tạo thư mục mới.
     - Nhấn **🗑 Delete** để xóa file.

### Cách 2: Khởi chạy bằng lệnh Maven
* Khởi chạy Server:
  ```powershell
  .\mvnw.cmd javafx:run -Pserver
  ```
* Khởi chạy Client:
  ```powershell
  .\mvnw.cmd javafx:run -Pclient
  ```

---

## 3. Tài khoản Đăng nhập MySQL Mặc định

Cơ sở dữ liệu tự động khởi tạo 2 tài khoản mẫu (không mã hóa mật khẩu):
| Username | Password | Quyền Đọc (Read) | Quyền Ghi (Write) | Thư mục Home |
| :--- | :--- | :---: | :---: | :--- |
| `admin` | `admin` | Có | Có | `server_storage/files` |
| `user` | `123456` | Có | Có | `server_storage/files` |

*Người dùng có thể thêm/xóa tài khoản trực tiếp trên Tab **MySQL Accounts** của Server Dashboard.*

---

## 4. Kết quả Kiểm thử Tự động (Automated Tests)

Chạy toàn bộ test kiểm thử:
```powershell
.\mvnw.cmd clean test
```
Kết quả kiểm thử thực tế:
* `DatabaseTest`: Pass kết nối MySQL `localhost:3306`, xác thực tài khoản và ghi nhận log.
* `FtpProtocolTest`: Pass toàn bộ quy trình bắt tay giao thức FTP RFC 959 (`USER`, `PASS`, `PWD`, `TYPE`, `PASV`, `QUIT`).
* `FtpClientIntegrationTest`: Pass toàn bộ kịch bản upload file, tải file về máy, kiểm tra checksum toàn vẹn, duyệt danh sách và xóa file.
* **Tổng kết:** `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` (100% Passed).
