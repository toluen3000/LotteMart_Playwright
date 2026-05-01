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
        System.out.println("Đã bắt được SP từ ô Tìm kiếm: " + name);
        linkLoc.evaluate("node => node.scrollIntoView({block: 'center'})");
        page.waitForTimeout(500);

        linkLoc.evaluate("node => node.click()");

        return new String[]{name, price};
    }

    public int getProductCount() {
        page.waitForLoadState();

        // Dùng lại Locator đã được chứng minh là chạy tốt trên Lotte Mart
        Locator firstProduct = page.locator(".item:has(a[href*='/product/'])").first();

        try {
            firstProduct.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: Quá 10s vẫn không có sản phẩm nào load lên!");
            return 0;
        }

        // Đếm tổng số thẻ thỏa mãn điều kiện
        return page.locator(".item:has(a[href*='/product/'])").count();
    }

    public String getNoResultMessage() {
        page.waitForLoadState();

        //  getByText để bắt chính xác đoạn text hiển thị trên UI
        Locator errorMessage = page.getByText("Rất tiếc", new Page.GetByTextOptions().setExact(false)).first();

        try {
            // Chờ tối đa 5 giây
            errorMessage.waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            return errorMessage.innerText().trim();

        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: Đã chờ 5s nhưng không thấy chữ 'Rất tiếc'!");
            return "";
        }

    }

    /**
     * Nhập khoảng giá và nhấn nút Áp dụng (Fix lỗi Front-end không nhận diện Fill)
     */
    public void applyPriceFilter(String minPrice, String maxPrice) {
        Locator filterSidebar = page.locator("#offcanvas_aside").first();
        Locator priceRangeBox = filterSidebar.locator(".form-range-rice").first();

        // Bắt các thẻ input bên trong khu vực chọn giá
        Locator textboxes = priceRangeBox.locator("input");

        // --- XỬ LÝ Ô MIN ---
        // 1. Click vào ô để focus
        textboxes.nth(0).click(new Locator.ClickOptions().setForce(true));
        textboxes.nth(0).clear();
        // 2. Gõ từng phím (type) với độ trễ 50ms giữa mỗi phím để kích hoạt sự kiện FE
        if (!minPrice.isEmpty()) {
            textboxes.nth(0).pressSequentially(minPrice, new Locator.PressSequentiallyOptions().setDelay(50));
        }

        // --- XỬ LÝ Ô MAX ---
        textboxes.nth(1).click(new Locator.ClickOptions().setForce(true));
        textboxes.nth(1).clear();
        if (!maxPrice.isEmpty()) {
            textboxes.nth(1).pressSequentially(maxPrice, new Locator.PressSequentiallyOptions().setDelay(50));
        }

        // --- BẤM ÁP DỤNG ---
        // Đợi 1 chút xíu (200ms) cho FE kịp lưu State trước khi bấm nút
        page.waitForTimeout(200);
        Locator applyButton = priceRangeBox.locator(".btn-search").first();
        applyButton.click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Kiểm tra giá trị thực tế trong ô Min Price
     */
    public String getMinPriceInputValue() {
        return page.locator("#offcanvas_aside").getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX).nth(0).inputValue();
    }

    /**
     * Mở dropdown Sắp xếp và chọn theo tên (SR_12)
     */
    public void selectSortOption(String optionName) {
        // Bắt nút Sắp xếp theo log bạn cung cấp: getByRole('button', { name: 'Sắp xếp theo...' })
        Locator sortBtn = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText("Sắp xếp theo")).first();
        sortBtn.click(new Locator.ClickOptions().setForce(true));

        // Chờ menu xổ ra và chọn
        page.getByText(optionName, new Page.GetByTextOptions().setExact(false)).last().click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Lấy danh sách GIÁ TIỀN của các sản phẩm đang hiển thị
     */
    public java.util.List<Integer> getDisplayedPrices() {
        // Dùng locator bất bại để chỉ nhắm vào thẻ sản phẩm thật sự
        Locator productItem = page.locator(".item:has(a[href*='/product/'])");

        try {
            // Ép chờ phần tử đầu tiên xuất hiện sau khi load
            productItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (Exception e) {
            System.out.println("⚠️ Cảnh báo: Không có sản phẩm nào load lên!");
        }

        // Chỉ tìm thẻ giá (price/strong) NẰM BÊN TRONG thẻ sản phẩm hợp lệ
        Locator priceElements = productItem.locator("strong:has-text('₫'), span:has-text('₫'), .price");

        java.util.List<String> rawPrices = priceElements.allInnerTexts();
        java.util.List<Integer> parsedPrices = new java.util.ArrayList<>();

        for (String text : rawPrices) {
            String cleanText = text.replace(".", "").replace(",", "").replace("₫", "").replace("đ", "").trim();
            try {
                if (!cleanText.isEmpty()) {
                    parsedPrices.add(Integer.parseInt(cleanText));
                }
            } catch (NumberFormatException e) {
                // Bỏ qua rác
            }
        }
        return parsedPrices;
    }




}