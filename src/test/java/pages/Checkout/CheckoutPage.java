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


    public void toggleVATInvoice() {
        page.getByText("Yêu cầu xuất hóa đơn VAT").first().click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(1000); // Chờ hiệu ứng slide toggle của form
    }


    public boolean isVATFormVisible() {
        // Kiểm tra sự xuất hiện của ô nhập MST hoặc Tên công ty
        return page.getByPlaceholder("Mã số thuế").isVisible() ||
                page.getByPlaceholder("Tên công ty").isVisible();
    }


    public String getVATFieldValue(String placeholder) {
        return page.getByPlaceholder(placeholder).inputValue();
    }


    public void fillVATInfo(String company, String mst, String address) {
        page.getByPlaceholder("Tên công ty").fill(company);
        page.getByPlaceholder("Mã số thuế").fill(mst);
        page.getByPlaceholder("Địa chỉ công ty").fill(address);
    }


    public void openVATForm() {
        // Dùng đúng Locator từ Codegen của bạn
        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Yêu cầu xuất hóa đơn VAT")).click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(1000); // Đợi form trượt ra hoặc popup hiện lên
    }


    public void clickConfirmVATRequest() {
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Xác nhận yêu cầu")).click(new Locator.ClickOptions().setForce(true));
        page.waitForTimeout(1500); // Đợi text báo lỗi xuất hiện
    }


    public boolean isVATRequiredErrorsShown() {
        // Dùng getByText chứa 1 phần chuỗi lỗi từ Codegen (vì Lotte hay cắt chữ "bắt buộc" thành "bắt..." trên UI)
        boolean companyErr = page.getByText("Tên công ty là trường bắt", new Page.GetByTextOptions().setExact(false)).isVisible();
        boolean addressErr = page.getByText("Địa chỉ công ty là trường bắt", new Page.GetByTextOptions().setExact(false)).isVisible();
        boolean taxErr = page.getByText("Mã số thuế công ty là trường", new Page.GetByTextOptions().setExact(false)).isVisible();
        boolean emailErr = page.getByText("Email là trường bắt", new Page.GetByTextOptions().setExact(false)).isVisible();

        // Chỉ cần 1 trong các lỗi này xuất hiện là chứng tỏ hệ thống đã chặn thành công
        return companyErr || addressErr || taxErr || emailErr;
    }
}