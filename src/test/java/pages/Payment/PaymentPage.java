package pages.Payment;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

public class PaymentPage {
    private Page page;


    public PaymentPage(Page page) {
        this.page = page;
    }


    public boolean isCODSelected() {
        try {
            String currentPaymentText = page.locator(".payment-choose").first().innerText().trim();
            return currentPaymentText.contains("Tiền mặt");
        } catch (Exception e) {
            return false;
        }
    }


    public void selectCODPayment() {
        System.out.println("   [PAGE] Đang chọn phương thức thanh toán: Tiền mặt...");
        page.locator(".payment-choose").first().click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(500); // Đợi menu xổ xuống

        page.locator(".payment-choose li")
                .filter(new Locator.FilterOptions().setHasText("Tiền mặt"))
                .first()
                .click(new Locator.ClickOptions().setForce(true));

        page.waitForTimeout(1000); // Đợi UI cập nhật trạng thái
    }


    public void processPlaceOrder() {
        System.out.println("   [PAGE] Click nút 'Đặt hàng' lần 1 (Mở popup)...");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đặt hàng"))
                .click(new Locator.ClickOptions().setForce(true));

        try {
            System.out.println("   [PAGE] Đang chờ popup xác nhận hiển thị...");
            Locator confirmBtn = page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Đặt Hàng").setExact(true));

            // Đợi tối đa 5s, nếu có popup thì click
            confirmBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

            System.out.println("   [PAGE] Click nút 'Đặt Hàng' lần 2 trên popup...");
            confirmBtn.click(new Locator.ClickOptions().setForce(true));
        } catch (Exception e) {
            System.out.println("   [PAGE] Bỏ qua popup xác nhận (Lotte không yêu cầu xác nhận lần này).");
        }
    }


    public boolean isOrderSuccess() {
        try {
            System.out.println("   [PAGE] Đang kiểm tra trạng thái Đặt hàng thành công...");
            Locator successHeading = page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Đặt hàng thành công!"));

            // Cho phép server tối đa 20s để tạo đơn xong
            successHeading.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000));
            return successHeading.isVisible();
        } catch (Exception e) {
            return false;
        }
    }


    public void cancelVNPayTransaction() {
        System.out.println("   [VNPAY] Đang thao tác hủy trên cổng thanh toán VNPAY...");
        try {
            // Đợi trang VNPAY load DOM xong
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(2000);

            // Ưu tiên tìm text "Hủy thanh toán"
            Locator cancelLink = page.locator("a").filter(new Locator.FilterOptions().setHasText("Hủy thanh toán"));

            if (cancelLink.isVisible()) {
                System.out.println("   [VNPAY] Đã tìm thấy và nhấn nút 'Hủy thanh toán'...");
                cancelLink.click();
            } else {
                System.out.println("   [VNPAY] Không có chữ Hủy, thử nhấn icon Quay lại (Back)...");
                page.locator(".header-back, .btn-back, a:has(i.fa-arrow-left)").first().click();
            }

            // Xử lý popup confirm "Bạn có chắc chắn muốn hủy?" của VNPAY (Nếu có)
            try {
                Locator confirmCancelBtn = page.getByRole(AriaRole.LINK,
                        new Page.GetByRoleOptions().setName("Xác nhận hủy"));
                confirmCancelBtn.waitFor(new Locator.WaitForOptions().setTimeout(3000));
                System.out.println("   [VNPAY] Xác nhận Hủy giao dịch trên popup...");
                confirmCancelBtn.click();
            } catch (Exception e) {
                // Không có popup thì lơ đi
            }

        } catch (Exception e) {
            System.out.println("   [VNPAY] Lỗi khi thao tác hủy trên VNPAY: " + e.getMessage());
        }
    }


    public void clearCart() {
        System.out.println("🧹 [TEARDOWN] Bắt đầu dọn dẹp giỏ hàng...");
        try {
            page.navigate("https://www.lottemart.vn/cart");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Chờ 1 trong 2: Chữ 'Chưa có sản phẩm' HOẶC 'icon thùng rác'
            page.locator("text='Chưa có sản phẩm!', i.icon-trash").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(10000));

            boolean isEmpty = page.getByRole(AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Chưa có sản phẩm!")).first().isVisible();

            if (isEmpty) {
                System.out.println("   -> Giỏ hàng đang rỗng (Bỏ qua dọn dẹp).");
                return;
            }

            Locator trashIcons = page.locator("i.icon-trash");

            while (trashIcons.count() > 0) {
                System.out.println("   -> Đang xóa sản phẩm... (Còn lại: " + trashIcons.count() + " item)");

                trashIcons.first().click(); // Bấm nút xóa item đầu tiên

                // Xác nhận xóa
                Locator confirmBtn = page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("Xóa").setExact(true));

                confirmBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                confirmBtn.click();

                // Đợi popup ẩn hẳn (Server đã xóa xong) mới lặp vòng tiếp theo
                confirmBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
            }
            System.out.println("✅ Đã dọn sạch giỏ hàng thành công.");

        } catch (Exception e) {
            System.out.println("⚠️ Lỗi Teardown (Không thể dọn giỏ hàng): " + e.getMessage());
        }
    }


    public void selectVNPayPayment() {
        System.out.println("   [PAGE] Đang thao tác chọn thanh toán VNPAY...");
        try {
            page.waitForTimeout(2000);
            String currentPaymentText = page.locator(".payment-choose").first().innerText();
            if (!currentPaymentText.contains("VNPAY") && !currentPaymentText.contains("Quét mã")) {
                page.locator(".payment-choose").first().click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(1000);
                page.locator("li:has-text('VNPAY'), li:has-text('Quét mã')")
                        .first().click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(1500);
            } else {
                System.out.println("   [PAGE] VNPAY đã được chọn sẵn.");
            }
        } catch (Exception e) {
            System.out.println("   [PAGE] Lỗi khi chọn VNPAY: " + e.getMessage());
        }
    }


    public void cancelLatestOrder() {
        System.out.println("🧹 [TEARDOWN] Đang tiến hành hủy đơn hàng vừa đặt trên Production...");
        try {
            page.navigate("https://www.lottemart.vn/vi-bdh/my-page/order-information");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(2000); // Chờ list đơn hàng fetch API xong

            Locator detailLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Xem chi")).first();

            if (detailLink.isVisible()) {
                System.out.println("   -> Đã tìm thấy đơn hàng mới nhất, đang vào xem chi tiết...");
                detailLink.click();
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                page.waitForTimeout(1500);
            } else {
                System.out.println("   -> Lỗi: Không tìm thấy đơn hàng nào trong lịch sử!");
                return;
            }

            // Bước 3: Tìm nút "Hủy đơn"
            Locator cancelBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Hủy đơn"));

            if (cancelBtn.isVisible()) {
                System.out.println("   -> Bấm nút 'Hủy đơn'...");
                cancelBtn.click();
                page.waitForTimeout(1000);

                // Bước 4: Chọn lý do (Bấm thẳng vào text giống như script record của bạn)
                System.out.println("   -> Chọn lý do: 'Chọn nhầm cửa hàng'...");
                page.getByText("Chọn nhầm cửa hàng").first().click();

                // Bước 5: Xác nhận hủy
                System.out.println("   -> Click 'Xác nhận hủy đơn'...");
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xác nhận hủy đơn")).click();

                // Chờ popup biến mất hoặc trạng thái thay đổi
                page.waitForTimeout(2000);
                System.out.println("Đã hủy đơn hàng thành công! Hoàn tất dọn dẹp Production.");
            } else {
                System.out.println("⚠Không tìm thấy nút 'Hủy đơn'. Đơn này có thể đã bị hủy trước đó hoặc đang được xử lý kho.");
            }

        } catch (Exception e) {
            System.out.println("Lỗi trong quá trình tự động hủy đơn: " + e.getMessage());
        }
    }
}