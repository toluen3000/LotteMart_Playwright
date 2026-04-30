package tests.Details;

import com.microsoft.playwright.*;
import dto.ProductData;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.ProductDetailsPage;
import utils.JsonReader;

public class ProductDetailsTest {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private ProductDetailsPage detailsPage;


    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
        detailsPage = new ProductDetailsPage(page);
    }

    /**
     * DataProvider cung cấp dữ liệu cho 4 Test Cases (DT_01 -> DT_04)
     * Cấu trúc mảng: { TestCaseID, ProductURL, ExpectedName, ExpectedPrice, HasReview(boolean), ExpectedReviewText }
     */
    @DataProvider(name = "productData")
    public Object[][] getProductData() {
        String path = "testdata/Details/product_data.json";

        ProductData[] data = JsonReader.readJson(path, ProductData[].class);

        return JsonReader.toTestNGFormat(data);
    }

    // DT01 -> DT04
    @Test(dataProvider = "productData")
    public void testProductDetailsContent(ProductData data) {
        System.out.println("Running: " + data.testCaseId());

        detailsPage.navigateToProduct(data.url());

        validateBasicInfo(data);
        validateReview(data);
    }

    private void validateBasicInfo(ProductData data) {
        Assert.assertEquals(detailsPage.getProductName(), data.expectedName());
        Assert.assertEquals(detailsPage.getProductPrice(), data.expectedPrice());
    }

    private void validateReview(ProductData data) {
        if (data.expectedHasReview()) {
            Assert.assertTrue(detailsPage.hasReviews());
        } else {
            Assert.assertFalse(detailsPage.hasReviews());
        }

        Assert.assertEquals(detailsPage.getRatingText(), data.expectedReviewText());
    }

    @Test(dataProvider = "productData")
    public void testDT04_ProductDescription(ProductData data) {
        if (!data.testCaseId().equals("DT_04")) return;

        System.out.println("Running: " + data.testCaseId());

        detailsPage.navigateToProduct(data.url());

        // scroll tới phần chi tiết
        detailsPage.scrollToDetailSection();

        detailsPage.openDetailTab();

        Assert.assertTrue(
                detailsPage.isDetailTableDisplayed(),
                "Không hiển thị bảng thông tin chi tiết"
        );

        Assert.assertTrue(
                detailsPage.getDetailRowCount() > 3,
                "Bảng thông tin quá ít dữ liệu (có thể render lỗi)"
        );

        Assert.assertTrue(
                detailsPage.hasImportantFields(),
                "Thiếu thông tin quan trọng (SKU / Hạn sử dụng)"
        );
    }

    @AfterMethod
    public void tearDown() {
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}
