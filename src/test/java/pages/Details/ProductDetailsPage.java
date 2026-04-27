package pages.Details;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

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

    // define for quantity test
    private final Locator btnMinus;
    private final Locator btnAddToCart;
    private final Locator btnPlus;
    private final Locator inputQuantity;


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

    public int getMaxQuantity() {
        return Integer.parseInt(inputQuantity.getAttribute("max"));
    }

    public boolean isStockWarningDisplayed() {
        return page.locator("text=Chỉ còn").isVisible(); // tùy site
    }

    public void clickAddToCart() {
        btnAddToCart.click();
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
        page.navigate(url);
        productName.waitFor();
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

    public void openDetailTab() {
        detailTabButton.scrollIntoViewIfNeeded();
        detailTabButton.click();
    }

    public boolean hasImportantFields() {
        return tabContent.innerText().contains("SKU")
                && tabContent.innerText().contains("Hạn sử dụng");
    }
}