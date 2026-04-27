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
    }


    // func to help testing
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