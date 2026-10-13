# FCM Keeper iQOO

App giúp giữ kết nối thông báo Google (FCM) trên iQOO / vivo chạy OriginOS (ROM nội địa Trung Quốc, GMS cài thêm).

## Build
1. Mở thư mục này bằng Android Studio (Koala trở lên), chờ Gradle sync (đồng ý dùng Gradle 8.7 nếu được hỏi).
2. Build > Build APK(s) rồi cài file `app/build/outputs/apk/debug/app-debug.apk` lên máy.

## Sau khi cài
1. Mở app, bấm lần lượt nút 1 → 5.
2. Với Google Play Services, Google Services Framework và app đang mất thông báo:
   - Pin: cho phép chạy nền tiêu thụ cao / không giới hạn
   - Quyền: bật Tự khởi động
   - Khóa app trong màn hình đa nhiệm
3. Không vuốt tắt (force-stop) các app đó.
4. Nếu dùng VPN/proxy, app VPN cũng cần các quyền ở bước 2.

## Bản 1.1.0
- Chu kỳ heartbeat tự điều chỉnh: sạc ~10 phút, bình thường ~15 phút, pin yếu / tiết kiệm pin / mất mạng ~30 phút.
- Thêm intent MCS_HEARTBEAT; chỉ gửi heartbeat khi có mạng.
- Mở màn hình tự khởi động của vivo/iQOO, Xiaomi, OPPO/realme/OnePlus, Honor.
- Hiển thị lần heartbeat tiếp theo và trạng thái mạng.

## Bản 1.3.0
- Đọc trạng thái FCM trực tiếp từ Google Play Services (yêu cầu "local/status", không có tài liệu chính thức; hiện chỉ biết mã 3 = mất kết nối).
- Mỗi 3 phút:
  - Đang kết nối: chỉ gửi heartbeat, không reconnect.
  - Mất kết nối: gửi heartbeat, chờ 2 giây, đọc lại; vẫn mất kết nối thì mới gửi reconnect.
  - GMS không trả lời: dùng kiểm tra TCP tới mtalk.google.com (cổng 5228/443) làm phương án dự phòng.
  - Không có mạng: không gửi gì, chờ mạng trở lại rồi kiểm tra lại.
- Màn hình hiển thị mã trạng thái thô mà GMS trả về để bạn đối chiếu.

## Bản 1.4.0
- Thêm cơ chế thử nghiệm giữ liên kết (bind) từ app tới một service của Google Play Services trong lúc foreground service đang chạy, để GMS ít bị hệ thống coi là app nền có thể đóng băng.
- Bật/tắt bằng nút 8; màn hình hiển thị trạng thái liên kết (đã liên kết / đang kết nối / không liên kết được / tắt).
- Tên service dùng để liên kết là nội bộ của GMS, có thể khác giữa các phiên bản; nếu không liên kết được thì app vẫn chạy bình thường.
- Chưa có bằng chứng cơ chế này vượt được bộ đóng băng của OriginOS; hãy kiểm chứng trên máy.

## Giới hạn
- Yêu cầu đọc trạng thái FCM là cơ chế không có tài liệu chính thức; bản GMS mới có thể đổi hoặc chặn. Khi đó app tự dùng phương án dự phòng (kiểm tra TCP), kém chính xác hơn.
- Chu kỳ 3 phút tốn pin hơn; khi máy ngủ sâu (Doze) hệ thống có thể giãn alarm ra, nhất là nếu app chưa được bỏ tối ưu pin.
- App không thể tự bỏ giới hạn của OriginOS (cần root hoặc quyền hệ thống); phần quyết định là các cài đặt ở bước 2.
- Heartbeat gửi tới GMS là nỗ lực tốt nhất, bản GMS mới có thể bỏ qua.
- Tên màn hình cài đặt của vivo thay đổi theo phiên bản; nếu không mở thẳng được, app sẽ mở trang thông tin ứng dụng.


## OriginOS 5: connection history and Doze notes

This build adds:
- A live display of the currently confirmed CONNECTED session duration.
- The last time a confirmed transition to DISCONNECTED was recorded.
- GMS and GSF app-info shortcuts for manual battery/background settings.
- State handling that does not count an unknown GMS response as a confirmed disconnect; the continuous-session timer is cleared while status is unknown.

### Important platform limits
- An ordinary app cannot silently add GMS/GSF to Android's system Doze whitelist or override OriginOS autostart/background restrictions. The app can request battery-optimization exemption for itself and open GMS/GSF app-info screens for manual configuration.
- Optional PC helper: `scripts/whitelist-doze-adb.sh` uses ADB shell to request the device-idle whitelist for GMS, GSF, and FCM Keeper. Enable USB debugging and authorize the computer first. The command may be blocked by the ROM and does not replace OriginOS's own background/autostart settings.
- FCM message priority HIGH is selected by the sender/server for the individual message. A client APK cannot force other apps' FCM messages to HIGH.
- `GCM_RECONNECT` and heartbeat broadcasts are undocumented/best-effort actions; a successful broadcast does not prove that GMS reconnected. Check the reported status after the next probe.
- The connection timer measures the time since this app last observed a transition into CONNECTED; it cannot prove uninterrupted server-side connectivity between probes.


## Giao diện phiên bản IQOO fix
- Icon launcher mới có chữ “IQOO fix”.
- Hai dòng đầu màn hình là thời gian kết nối liên tục dạng `HH:mm:ss` (giờ được cộng dồn, không đổi sang ngày) và thời điểm disconnect gần nhất dạng `dd/MM/yyyy HH:mm`.
- Nút dịch vụ giữ kết nối và liên kết GMS hiển thị rõ `ON`/`OFF`.
