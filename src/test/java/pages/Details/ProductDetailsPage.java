package pages.Details;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

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
        return noReviewText.isVisible() ? noReviewText.innerText().trim() : "No data";
    }


    public boolean isShortDescriptionDisplayed() {
        return shortDescription.isVisible();
    }

    public boolean isDetailedDescriptionDisplayed() {
        return detailedDescription.isVisible();
    }
}