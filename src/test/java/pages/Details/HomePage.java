package pages.Details;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class HomePage {
    private final Page page;

    public HomePage(Page page) {
        this.page = page;
    }

    public void handleStartupPopups() {


        try {
            Locator regionBtn = page.locator("button:has-text('Hà Nội Center')");
            Locator confirmBtn = page.locator("button:has-text('Xác nhận')");
            regionBtn.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            regionBtn.click();
            confirmBtn.click();
            System.out.println("Đã xử lý popup khu vực");
        } catch (TimeoutError e) {
            System.out.println("không có popup khu vực");
        }
        try {

            Locator checkbox = page.locator("label:has-text('Không hiển thị lại hôm nay')");
            checkbox.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            checkbox.click();
            // đóng popup
            page.keyboard().press("Escape");
            System.out.println("Đã xử lý popup hôm nay");

        } catch (TimeoutError e) {
            System.out.println("Không có popup hôm nay");
        }

    }

    public String[] clickFirstProductAndGetDetails() {
        // tim row chứa sp
        Locator firstCard = page.locator(".item:has(a[href*='/product/'])").first();

        int maxScrolls = 5;
        int scrolls = 0;

        // Focus vào body để đảm bảo cuộn trang hoạt động
        page.locator("body").click(new Locator.ClickOptions().setForce(true).setDelay(100));

        while (!firstCard.isVisible() && scrolls < maxScrolls) {
            page.keyboard().press("PageDown");
            page.evaluate("window.scrollBy(0, 500)");
            page.waitForTimeout(1500);

            System.out.println("Đang thử cuộn lần " + (scrolls + 1));
            scrolls++;
        }

        try {
            firstCard.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        } catch (TimeoutError e) {
            throw new RuntimeException("Đã cuộn " + maxScrolls + " lần nhưng trang web không load được danh sách sản phẩm!");
        }

        // lay ten sp
        String name = "";
        Locator linkLoc = firstCard.locator("a[href*='/product/']").first();

        name = linkLoc.getAttribute("title");

        if (name == null || name.trim().isEmpty()) {
            Locator nameLoc = firstCard.locator(".name, h3, h4, .product-name").first();
            if (nameLoc.isVisible()) {
                name = nameLoc.innerText().trim();
            } else {
                // Fallback cuối cùng
                name = linkLoc.innerText().trim();
            }
        }

        // Tránh NullPointerException
        if (name == null) name = ""; else name = name.trim();
        // ==========================================

        Locator priceLoc = firstCard.locator(".current-price, .price-discount, [itemprop='price']").first();
        String price = priceLoc.innerText().trim();

        if (price.contains("/")) {
            price = price.split("/")[0].trim();
        }

        // click by javascript
        System.out.println("Đã bắt được SP đầu tiên: " + name);

        linkLoc.evaluate("node => node.scrollIntoView({block: 'center', inline: 'center'})");

        page.waitForTimeout(1000);

        linkLoc.evaluate("node => node.click()");

        return new String[]{name, price};
    }

    public void search(String keyword) {
        Locator searchBox = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Tìm kiếm"));

        searchBox.click(); // Click vào ô tìm kiếm
        searchBox.fill(keyword); // Nhập từ khóa
        searchBox.press("Enter"); // Nhấn Enter

    }

}