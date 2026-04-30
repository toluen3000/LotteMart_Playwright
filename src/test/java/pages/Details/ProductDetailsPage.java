package pages.Details;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsPage {
    private final Page page;

    // locator using
    private final Locator productName;
    private final Locator productPrice;
    private final Locator ratingScore;
    private final Locator reviewCount;
    private final Locator noReviewText;
    private final Locator shortDescription;
    private final Locator detailedDescription;
    private final Locator ratingContainer;

    //dạng bảng
    private final Locator tabContent;
    private final Locator detailTable;
    private final Locator tableRows;
    private final Locator detailTabButton;
    private final Locator descriptionTab;
    private final Locator specTab;

    // define for quantity test
    private final Locator btnMinus;
    private final Locator btnAddToCart;
    private final Locator btnPlus;
    private final Locator inputQuantity;
    private final Locator stockWarning;
    private final Locator outStock;
    private final Locator ratingValue;

    // define locators
    public ProductDetailsPage(Page page) {
        this.page = page;

        this.productName = page.locator("h2[itemprop='name']");
        this.ratingContainer = page.locator(".field-score").first();
        this.productPrice = page.locator("[itemprop='price']").first();
        this.ratingScore = ratingContainer.locator(".lbl-rating");
        this.reviewCount = ratingContainer.locator(".btn-link");
        this.noReviewText = page.locator("text=Chưa có đánh giá");
        this.shortDescription = page.locator(".short-desc[itemprop='description']");
        this.detailedDescription = page.locator("#desc");
        this.tabContent = page.locator("div.pro-tab-content");
        this.detailTable = tabContent.locator("table.table");
        this.tableRows = detailTable.locator("tr");
        this.detailTabButton = page.locator("button:has-text('Đặc điểm sản phẩm')");

        this.btnPlus = page.locator("button[data-type='plus']").first();
        this.btnMinus = page.locator("button[data-type='minus']").first();
        this.inputQuantity = page.locator("input.input-number").first();
        this.btnAddToCart = page.locator("button:has-text('Thêm vào giỏ hàng')");
        this.stockWarning = page.locator("text=Chỉ còn");
        this.outStock = page.locator("div:has-text('hết hàng')").first();
        this.ratingValue = page.locator(".lbl-rating").first();
        this.descriptionTab = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Mô tả sản phẩm"));
        this.specTab = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đặc điểm sản phẩm"));

    }


    public boolean isDescriptionVisible() { return descriptionTab.isVisible(); }

    public void clickSpecTab() { specTab.click(); }

    //
    public boolean isDescriptionTabVisible() {
        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Mô tả sản phẩm")
        ).isVisible();
    }

    //
    public void openDetailTab1() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Đặc điểm sản phẩm")
        ).click();
    }

    //
    public boolean isDetailTableVisible() {
        // Thêm .first() hoặc chèn thêm class để loại bỏ bảng delivery
        return page.locator("table.table:not(.table-delivery)").first().isVisible();
    }

    // commit 2
    public boolean isProductNameVisible() {
        return page.locator("h2[itemprop='name']").isVisible();
    }

    public String getProductSKU() {
        return page.locator("th:has-text('SKU') + td").innerText();
    }

    public void scrollToRelatedProducts() {
        page.locator("text=Sản phẩm liên quan").scrollIntoViewIfNeeded();
    }

    public void clickFirstRelatedProduct() {
        page.locator(".product-item").first().click();
    }

    public String getReviewText() {
        if (noReviewText.isVisible()) {
            return noReviewText.innerText();
        }

        if (reviewCount.isVisible()) {
            return reviewCount.innerText();
        }

        return "";
    }

    public boolean isReviewVisible() {
        return ratingValue.isVisible() || reviewCount.isVisible();
    }


    // func to help testing

    public void clickIncreaseQuantity() {
        btnPlus.click();
    }

    public String getRawQuantity() {
        return inputQuantity.inputValue();
    }

    public void clickDecreaseQuantity() {
        btnMinus.click();
    }

    public int getCurrentQuantity() {
        String value = inputQuantity.inputValue().trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public boolean isStockWarningVisible() {
        return stockWarning.isVisible();
    }

    public String getStockWarningText() {
        return stockWarning.innerText();
    }

    public boolean isAddToCartDisabled() {
        return btnAddToCart.isVisible();
    }

    public boolean isStockTextVisible() {
        return outStock.isVisible();
    }

    public String getStockText() {
        return outStock.first().innerText();
    }


    public int getMaxQuantity() {
        return Integer.parseInt(inputQuantity.getAttribute("max"));
    }

    public boolean isStockWarningDisplayed() {
        return page.locator("text=Chỉ còn").isVisible(); // tùy site
    }

    public void clickAddToCart() {
        btnAddToCart.click();
    }

    public String getAddToCartText() {
        return btnAddToCart.innerText();
    }

    public boolean isMinusDisable(){
        return btnMinus.isDisabled();
    }

    public void enterQuantity(String quantity) {
        inputQuantity.fill(quantity);
    }

    public void typeQuantity(String quantity) {
        inputQuantity.click();
        page.keyboard().press("Home");
        inputQuantity.pressSequentially(quantity);
    }

    public void navigateToProduct(String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        page.locator("h2[itemprop='name']").waitFor();
    }

    public String getProductName() {
        return productName.innerText().trim();
    }

    public String getProductPrice() {
        return productPrice.innerText().trim().replace('\u00A0', ' ');
    }

    public boolean hasReviews() {
        return reviewCount.isVisible() && !reviewCount.innerText().contains("0");
    }

    public String getRatingText() {
        if (hasReviews()) {
            return ratingScore.innerText().trim() +
                    " (" + reviewCount.innerText().trim() + ")";
        }
        return noReviewText.isVisible() ? noReviewText.innerText().trim() : "Chưa có đánh giá";
    }


    public boolean isShortDescriptionDisplayed() {
        return shortDescription.isVisible();
    }

    public boolean isDetailedDescriptionDisplayed() {
        return detailedDescription.isVisible();
    }

    public void scrollToDetailSection() {
        tabContent.scrollIntoViewIfNeeded();
    }

    public boolean isDetailTableDisplayed() {
        Locator section = page.locator("text=SKU");

        try {
            section.waitFor(new Locator.WaitForOptions()
                    .setTimeout(5000));
            return section.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public int getDetailRowCount() {
        return tableRows.count();
    }

    public void waitForPageLoaded() {
        productName.waitFor();
    }

    public void openDetailTab() {
        detailTabButton.scrollIntoViewIfNeeded();
        detailTabButton.click();
    }

    public boolean hasImportantFields() {
        return tabContent.innerText().contains("SKU")
                && tabContent.innerText().contains("Hạn sử dụng");
    }

    public String[] clickFirstSuggestedProductAndGetDetails() {
        // 1. Khởi động cuộn trang
        page.locator("body").click(new Locator.ClickOptions().setForce(true).setDelay(100));

        System.out.println("⬇️ Đang cuộn mồi để kích hoạt API tải Sản phẩm gợi ý...");

        // Cuộn mồi 5 lần chắc chắn sẽ chạm tới đáy/khu vực gợi ý
        for (int i = 0; i < 5; i++) {
            page.keyboard().press("PageDown");
            page.evaluate("window.scrollBy(0, 600)");
            page.waitForTimeout(1000); // Đợi 1s cho mỗi lần cuộn để ảnh load
            System.out.println("🔄 Cuộn lần " + (i + 1));
        }

        // 2. Tìm thẻ Sản phẩm (Ở trang chi tiết, class .item chính là các SP gợi ý)
        Locator allCards = page.locator(".item:has(a[href*='/product/'])");
        Locator suggestedCard = null;

        // Quét danh sách, tóm ngay cái thẻ đầu tiên ĐANG HIỂN THỊ trên màn hình
        for (int i = 0; i < allCards.count(); i++) {
            if (allCards.nth(i).isVisible()) {
                suggestedCard = allCards.nth(i);
                break;
            }
        }

        if (suggestedCard == null) {
            throw new RuntimeException("❌ Đã cuộn 5 lần nhưng không có Sản phẩm Gợi ý nào xuất hiện!");
        }

        // 3. Móc Tên
        String name = "";
        Locator linkLoc = suggestedCard.locator("a[href*='/product/']").first();
        name = linkLoc.getAttribute("title");

        if (name == null || name.trim().isEmpty()) {
            Locator nameLoc = suggestedCard.locator(".name, h3, h4, .product-name").first();
            if (nameLoc.isVisible()) {
                name = nameLoc.innerText().trim();
            } else {
                name = linkLoc.innerText().trim();
            }
        }
        if (name == null) name = ""; else name = name.trim();

        // 4. Móc Giá (Bổ sung thêm class .price chung chung nhất để chống vỡ locator)
        Locator priceLoc = suggestedCard.locator(".price, .current-price, .price-discount, [itemprop='price']").first();

        // Ép đợi Giá hiển thị để tránh Timeout như vừa nãy
        try {
            priceLoc.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        } catch (TimeoutError e) {
            System.out.println("⚠️ Cảnh báo: Không tìm thấy giá cho SP này, lấy mặc định là 0");
        }

        String price = priceLoc.isVisible() ? priceLoc.innerText().trim() : "0";
        if (price.contains("/")) {
            price = price.split("/")[0].trim();
        }

        System.out.println("✅ Đã bắt được SP Tương Tự: " + name);

        // ==========================================
        // 5. ÉP CHỜ CHUYỂN TRANG
        // ==========================================
        suggestedCard.scrollIntoViewIfNeeded();
        String currentUrl = page.url();
        linkLoc.click(new Locator.ClickOptions().setForce(true));

        // Đứng chờ cho tới khi URL thay đổi (Xác nhận đã nhảy sang SP mới)
        page.waitForCondition(() -> !page.url().equals(currentUrl),
                new Page.WaitForConditionOptions().setTimeout(10000));

        return new String[]{name, price};
    }

    public List<String> getSuggestedProductNames(int limit) {
        //  scroll mồi để kích hoạt Lazy Loading
        page.locator("body").click(new Locator.ClickOptions().setForce(true).setDelay(100));
        System.out.println("⬇Đang cuộn xuống khu vực 'Sản phẩm tương tự'...");
        for (int i = 0; i < 5; i++) {
            page.keyboard().press("PageDown");
            page.evaluate("window.scrollBy(0, 600)");
            page.waitForTimeout(1000);
        }

        // locator trỏ đúng vào thẻ span có chữ cần tìm
        Locator headingSpan = page.locator("span").filter(new Locator.FilterOptions().setHasText("Sản phẩm tương tự"));

        Locator similarProductsBlock = page.locator(".list-products-grid")
                .filter(new Locator.FilterOptions().setHas(headingSpan))
                .first();

        //tìm các thẻ item
        Locator allCards = similarProductsBlock.locator(".swiper-slide .item");

        List<String> productNames = new ArrayList<>();
        int count = 0;

        // 3. Quét các thẻ sản phẩm trong khối này
        for (int i = 0; i < allCards.count(); i++) {
            Locator currentCard = allCards.nth(i);

            String name = "";
            Locator linkLoc = currentCard.locator("a[href*='/product/']").first();

            name = linkLoc.getAttribute("title");

            if (name == null || name.trim().isEmpty()) {
                Locator nameLoc = currentCard.locator(".name, .field-name a").first();
                if (nameLoc.isVisible()) {
                    name = nameLoc.innerText().trim();
                } else {
                    name = linkLoc.innerText().trim();
                }
            }

            if (name != null && !name.trim().isEmpty()) {
                productNames.add(name.trim());
                count++;
            }

            if (count >= limit) {
                break;
            }
        }

        if (productNames.isEmpty()) {
            throw new RuntimeException("Đã tìm thấy khối 'Sản phẩm tương tự' nhưng bên trong không có sản phẩm nào!");
        }

        return productNames;
    }

    public String getSkuFromUrl() {
        String currentUrl = page.url();
        System.out.println("🔗 Đang kiểm tra URL: " + currentUrl);

        // Ví dụ: ...vay-ca-hoi-sg-food-500g-8936013232416
        String[] parts = currentUrl.split("-");

        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            // Tìm chuỗi có 13 chữ số (chuẩn EAN-13 của Barcode) trong URL
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{13}").matcher(currentUrl);
            if(m.find()) {
                return m.group();
            }
        }
        return "";
    }

    public String getCurrentUrl() {
        return page.url();
    }
}