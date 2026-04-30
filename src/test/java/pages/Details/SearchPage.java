package pages.Details;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

public class SearchPage {
    private final Page page;

    public SearchPage(Page page) {
        this.page = page;
    }


    public String[] clickFirstProductAndGetDetails() {
        page.waitForLoadState();

        Locator firstCard = page.locator(".item:has(a[href*='/product/'])").first();

        int maxScrolls = 5;
        int scrolls = 0;

        page.locator("body").click(new Locator.ClickOptions().setForce(true).setDelay(100));

        while (!firstCard.isVisible() && scrolls < maxScrolls) {
            page.keyboard().press("PageDown");
            page.evaluate("window.scrollBy(0, 500)");
            page.waitForTimeout(1500);
            scrolls++;
        }

        try {
            firstCard.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        } catch (TimeoutError e) {
            throw new RuntimeException("Không tìm thấy kết quả nào cho từ khóa này!");
        }

        String name = "";
        Locator linkLoc = firstCard.locator("a[href*='/product/']").first();
        name = linkLoc.getAttribute("title");

        if (name == null || name.trim().isEmpty()) {
            Locator nameLoc = firstCard.locator(".name, h3, h4, .product-name").first();
            if (nameLoc.isVisible()) {
                name = nameLoc.innerText().trim();
            } else {
                name = linkLoc.innerText().trim();
            }
        }
        if (name == null) name = ""; else name = name.trim();

        Locator priceLoc = firstCard.locator(".current-price, .price-discount, [itemprop='price']").first();
        String price = priceLoc.innerText().trim();
        if (price.contains("/")) {
            price = price.split("/")[0].trim();
        }
        System.out.println("🔎 Đã bắt được SP từ ô Tìm kiếm: " + name);
        linkLoc.evaluate("node => node.scrollIntoView({block: 'center'})");
        page.waitForTimeout(500);

        linkLoc.evaluate("node => node.click()");

        return new String[]{name, price};
    }
}