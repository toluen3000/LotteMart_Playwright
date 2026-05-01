package pages.Checkout;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.regex.Pattern;

public class CheckoutPage {
    private final Page page;

    public CheckoutPage(Page page) {
        this.page = page;
    }

    private int parseCurrency(String text) {
        String cleanText = text.replaceAll("[^0-9]", "");
        return cleanText.isEmpty() ? 0 : Integer.parseInt(cleanText);
    }

    public int getSubtotal() {
        return parseCurrency(page.locator("dt:has-text('Tổng giá trị đơn hàng') + dd strong").innerText());
    }

    public int getBagFee() {
        try {
            return parseCurrency(page.locator("dt:has-text('Túi Nylon') + dd strong").innerText());
        } catch (Exception e) {
            return 0;
        }
    }

    public int getShippingFee() {
        return parseCurrency(page.locator("dt:has-text('Phí vận chuyển') + dd strong").innerText());
    }

    public int getFinalTotal() {
        Locator total = page.locator(".text-red.fontsize-34").filter(new Locator.FilterOptions().setHasText("₫"));
        return parseCurrency(total.innerText());
    }

    public void selectStorePickup() {
        page.locator("label:has-text('Nhận hàng tại cửa hàng')").click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(2000);
    }


    public void selectStandardDelivery() {
        // 1. Lấy phí ship hiện tại (đang là 0) để làm mốc so sánh
        String currentShipText = page.locator("dt:has-text('Phí vận chuyển') + dd strong").innerText();

        // 2. Click chọn Giao hàng tiêu chuẩn
        page.locator("label:has-text('Giao hàng tiêu chuẩn')").click(new Locator.ClickOptions().setForce(true));

        // 3. Đợi cho đến khi text phí ship thay đổi (khác với con số 0 cũ)
        try {
            page.locator("dt:has-text('Phí vận chuyển') + dd strong")
                    .waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));

            // Đợi thêm tối đa 5s để text bên trong thay đổi khác con số cũ
            int timeout = 0;
            while (page.locator("dt:has-text('Phí vận chuyển') + dd strong").innerText().equals(currentShipText) && timeout < 5000) {
                page.waitForTimeout(500);
                timeout += 500;
            }
        } catch (Exception e) {
            System.out.println("Cảnh báo: Phí ship cập nhật chậm hoặc không đổi.");
        }
    }

    public boolean isCODPaymentDisabled() {
        // Kiểm tra sự xuất hiện của dòng chữ cảnh báo màu đỏ ngay khi chọn Nhận hàng tại cửa hàng
        return page.locator(".text-red:has-text('không áp dụng hình thức thanh toán tiền mặt')").isVisible();
    }
}