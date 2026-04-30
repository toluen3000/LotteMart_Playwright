package tests.Details;

import com.microsoft.playwright.*;
import dto.ProductData;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.HomePage;
import pages.Details.ProductDetailsPage;
import pages.Details.SearchPage;
import utils.JsonReader;

import java.util.List;

import static utils.StringUtils.normalizePrice;
import static utils.StringUtils.normalizeText;

public class ProductNavigationTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private HomePage homePage;
    private SearchPage searchPage;
    private ProductDetailsPage detailsPage;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.firefox().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
        );
        context = browser.newContext();
        page = context.newPage();

        homePage = new HomePage(page);
        searchPage = new SearchPage(page);
        detailsPage = new ProductDetailsPage(page);

        page.navigate("https://www.lottemart.vn/");
        // Xử lý ngay popup trước khi cuộn tìm sản phẩm
        homePage.handleStartupPopups();
    }

    @DataProvider(name = "navData")
    public Object[][] getNavData() {
        String jsonFileName = "testdata/Details/product_navigation.json";
        ProductData[] rawData = JsonReader.readJson(jsonFileName, ProductData[].class);
        return JsonReader.toTestNGFormat(rawData);
    }

    @Test(description = "DT_12 - Load đúng sản phẩm theo SKU qua URL")
    public void testProductLoadBySku() {
        // Data chuẩn bị theo Test Case
        String targetProductName = "Vây Cá Hồi Tẩm Gia Vị SG Food 500G";
        String expectedSku = "8936013232416";

        System.out.println("🔍 BƯỚC 1: Tìm đích danh sản phẩm cần test...");
        homePage.search(targetProductName);

        // BƯỚC 2: Click vào kết quả tìm kiếm đầu tiên
        searchPage.clickFirstProductAndGetDetails();
        detailsPage.waitForPageLoaded();

        System.out.println("✅ BƯỚC 3: Đang kiểm chứng dữ liệu trang chi tiết...");

        // 3.1 ASSERT URL chứa SKU
        String actualUrl = detailsPage.getCurrentUrl();
        Assert.assertTrue(
                actualUrl.contains(expectedSku),
                "Lỗi Nghiêm Trọng: URL KHÔNG chứa mã SKU mong đợi!\n" +
                        "Expected SKU: " + expectedSku + "\n" +
                        "Actual URL: " + actualUrl
        );
        System.out.println("   -> URL chứa mã SKU chuẩn xác.");

        // 3.2 Lấy SKU thông qua regex từ hàm vừa viết để xác nhận lại
        String extractedSku = detailsPage.getSkuFromUrl();
        Assert.assertEquals(
                extractedSku,
                expectedSku,
                "Lỗi: Mã SKU trích xuất từ hệ thống không khớp!"
        );

        // 3.3 (Tùy chọn) Kiểm tra giao diện hiển thị đúng tên không
        String actualName = detailsPage.getProductName();
        Assert.assertTrue(
                normalizeText(actualName).contains(normalizeText(targetProductName)),
                "Lỗi: Nhảy sang sai trang sản phẩm!\n" +
                        "Kỳ vọng hiển thị: " + targetProductName + "\n" +
                        "Thực tế hiển thị: " + actualName
        );
        System.out.println("   -> Tên sản phẩm hiển thị chuẩn xác.");

        System.out.println("🎉 Test Case DT_12: PASS! Sản phẩm map chuẩn 1-1 với SKU.");
    }

    @Test(description = "DT_16 - Home → Product Detail")
    public void testNavigateFromHome() {

        String[] productDetails = homePage.clickFirstProductAndGetDetails();
        // lay product dau tien trong list
        String expectedName = productDetails[0];
        String expectedPrice = normalizePrice(productDetails[1]);

        System.out.println("Home Name: " + expectedName);
        System.out.println("Home Price: " + expectedPrice);

        detailsPage.waitForPageLoaded();

        String actualName = detailsPage.getProductName();

        Assert.assertTrue(
                normalizeText(actualName).contains(normalizeText(expectedName)),
                "Tên sản phẩm không khớp\nExpected: " + expectedName + "\nActual: " + actualName
        );

        String actualPrice = normalizePrice(detailsPage.getProductPrice());

        Assert.assertEquals(
                actualPrice,
                expectedPrice,
                "Giá sản phẩm không khớp"
        );

        Assert.assertTrue(
                detailsPage.isDescriptionTabVisible(),
                "Không hiển thị tab mô tả"
        );

        detailsPage.openDetailTab();

        Assert.assertTrue(
                detailsPage.isDetailTableVisible(),
                "Không hiển thị bảng thông tin sản phẩm"
        );
    }


    @Test(description = "DT_17 - Search → Product Detail")
    public void testNavigateFromSearch() {
        // Lấy keyword từ JSON hoặc dùng chuỗi mặc định
        String keyword = "bò";

        System.out.println("Đang tìm kiếm từ khóa: " + keyword);
        homePage.search(keyword);

        // dung search bar
        String[] productDetails = searchPage.clickFirstProductAndGetDetails();

        String expectedName = productDetails[0];
        String expectedPrice = normalizePrice(productDetails[1]);

        System.out.println("Search Name: " + expectedName);
        System.out.println("Search Price: " + expectedPrice);

        detailsPage.waitForPageLoaded();

        String actualName = detailsPage.getProductName();
        Assert.assertTrue(
                normalizeText(actualName).contains(normalizeText(expectedName)),
                "Tên sản phẩm không khớp khi tìm kiếm\nExpected: " + expectedName + "\nActual: " + actualName
        );

        String actualPrice = normalizePrice(detailsPage.getProductPrice());
        Assert.assertEquals(
                actualPrice,
                expectedPrice,
                "Giá sản phẩm không khớp khi tìm kiếm"
        );

        Assert.assertTrue(
                detailsPage.isDescriptionTabVisible(),
                "Không hiển thị tab mô tả"
        );
    }

    @Test(description = "DT_18 - Related Product → Product Detail")
    public void testRelatedProductsRelevance() {
        //Search và vào sản phẩm đầu tiên
        String keyword = "xúc xích";
        System.out.println("BƯỚC 1: Tìm kiếm: " + keyword);
        homePage.search(keyword);

        searchPage.clickFirstProductAndGetDetails();
        detailsPage.waitForPageLoaded();
        System.out.println("Đang ở trang SP Gốc: " + detailsPage.getProductName());

        System.out.println("BƯỚC 2: Kiểm tra chéo 4 sản phẩm gợi ý...");
        List<String> suggestedNames = detailsPage.getSuggestedProductNames(4);

        System.out.println("BƯỚC 3: Đang Assert dữ liệu...");
        Assert.assertFalse(suggestedNames.isEmpty(), "Lỗi: Danh sách sản phẩm gợi ý bị rỗng!");

        String normalizedKeyword = normalizeText(keyword);

        for (int i = 0; i < suggestedNames.size(); i++) {
            String currentName = suggestedNames.get(i);
            System.out.println("   -> Đang check SP thứ " + (i+1) + ": " + currentName);

            String normalizedCurrentName = normalizeText(currentName);
            Assert.assertTrue(
                    normalizedCurrentName.contains(normalizedKeyword),
                    "Lỗi Thuật Toán Lotte: Sản phẩm gợi ý KHÔNG LIÊN QUAN!\n" +
                            "Từ khóa mong đợi: " + keyword + "\n" +
                            "Sản phẩm hiển thị: " + currentName
            );
        }

        System.out.println("Toàn bộ " + suggestedNames.size() + " SP gợi ý đều chuẩn xác!");
    }



    @AfterMethod
    public void tearDown() {
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}