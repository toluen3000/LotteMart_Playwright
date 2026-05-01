package pages.Checkout;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class CheckoutPage {
    private Page page;

    public CheckoutPage(Page page) {
        this.page = page;
    }

    /**
     * Kiểm tra xem khối địa chỉ giao hàng đã được tự động điền chưa (OD_01, OD_02)
     */
    public boolean isAddressAutoFilled() {
        // Bắt nút chứa thông tin địa chỉ mà bạn đã inspect
        Locator addressButton = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Thông tin người nhận"));

        // Cố gắng chờ nút hiển thị trong 3 giây
        try {
            addressButton.waitFor(new Locator.WaitForOptions().setTimeout(3000));
            // Kiểm tra xem bên trong có chứa thông tin thực tế không (ví dụ: tên bạn)
            String addressText = addressButton.innerText();
            return addressText.contains("Ngo Quoc Quan") && addressText.contains("Liễu Giai");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy tổng giá trị Thành tiền để đối chiếu với Cổng thanh toán (PM_02)
     */
    public String getTotalAmount() {
        // Tìm chữ "Thành tiền"
        Locator totalTextLocator = page.getByText("Thành tiền").first();
        // Lấy toàn bộ text của cha nó (thường chứa cả số tiền)
        String fullText = totalTextLocator.locator("..").innerText();

        // Ví dụ text là: "Thành tiền 76.400 ₫ Thanh toán", cần bóc tách số
        String amount = fullText.replace("Thành tiền", "")
                .replace("Thanh toán", "")
                .replace("₫", "")
                .replace(".", "")
                .trim();
        return amount;
    }

    /**
     * Chọn phương thức thanh toán
     */
    public void selectPaymentMethod(String methodName) {
        // Lotte Mart thường dùng Radio/Label cho phần chọn cổng thanh toán
        // Giả sử có một khu vực chọn (Cần điều chỉnh locator này cho chính xác nếu sai)
        Locator methodLabel = page.getByText(methodName, new Page.GetByTextOptions().setExact(false)).first();
        methodLabel.click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Bấm nút Thanh toán (Chốt đơn)
     */
    public void clickPayButton() {
        Locator payButton = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Thanh toán")).last(); // Nút to dưới cùng

        // Scroll xuống cho chắc chắn
        payButton.scrollIntoViewIfNeeded();
        payButton.click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Bóc tách các con số để kiểm tra thuật toán tính tổng (OD_04)
     */
    public int getSubtotal() {
        // Tìm thẻ <dt> chứa text, rồi lấy thẻ <dd> đầu tiên ngay sau nó
        Locator subtotalText = page.locator("dt").filter(new Locator.FilterOptions().setHasText("Tổng giá trị đơn hàng"))
                .locator("xpath=following-sibling::dd[1]")
                .locator("strong")
                .filter(new Locator.FilterOptions().setHasText("₫")).first();
        return parseCurrency(subtotalText.innerText());
    }

    public int getBagFee() {
        try {
            Locator bagFeeText = page.locator("dt").filter(new Locator.FilterOptions().setHasText("Túi Nylon"))
                    .locator("xpath=following-sibling::dd[1]")
                    .locator("strong")
                    .filter(new Locator.FilterOptions().setHasText("₫")).first();
            return parseCurrency(bagFeeText.innerText());
        } catch (Exception e) {
            return 0;
        }
    }

    public int getShippingFee() {
        Locator shipFeeText = page.locator("dt").filter(new Locator.FilterOptions().setHasText("Phí vận chuyển"))
                .locator("xpath=following-sibling::dd[1]")
                .locator("strong")
                .filter(new Locator.FilterOptions().setHasText("₫")).first();
        return parseCurrency(shipFeeText.innerText());
    }

    public int getFinalTotal() {
        // Thành tiền nằm trong cấu trúc <div> riêng, nên mình bắt theo div có class chứa "text-red" cho chắc chắn
        Locator finalTotalText = page.locator("div.row").filter(new Locator.FilterOptions().setHasText("Thành tiền"))
                .locator(".text-red")
                .filter(new Locator.FilterOptions().setHasText("₫")).first();
        return parseCurrency(finalTotalText.innerText());
    }

    /**
     * Hàm dùng chung để xóa chữ và ép kiểu tiền tệ về dạng số nguyên (Giữ nguyên như bài trước)
     */
    private int parseCurrency(String text) {
        String cleanText = text.replaceAll("[^0-9]", "");
        if (cleanText.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(cleanText);
    }


}