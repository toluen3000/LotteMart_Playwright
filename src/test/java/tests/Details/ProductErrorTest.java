package tests.Details;

import base.BaseTest;
import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.HomePage;
import pages.Details.ProductDetailsPage;
import pages.Details.SearchPage;

import static utils.StringUtils.normalizeText;

public class ProductErrorTest extends BaseTest {

//    private Playwright playwright;
//    private Browser browser;
//    private BrowserContext context;
//    private Page page;

    private HomePage homePage;
    private SearchPage searchPage;
    private ProductDetailsPage detailsPage;

    @BeforeMethod
    public void initPageObjects() {
        homePage = new HomePage(page);
        searchPage = new SearchPage(page);
        detailsPage = new ProductDetailsPage(page);

        page.navigate("https://www.lottemart.vn/");

        try {
            homePage.handleStartupPopups();
        } catch (Exception e) {
            System.out.println("⏩ Không thấy Popup xuất hiện, bỏ qua bước tắt popup!");
        }
    }

    @Test(description = "DT_10 - Xử lý lỗi Không tìm thấy sản phẩm (SKU sai)")
    public void DT_10_testProductNotFoundHandling() {
        // 1. Chuẩn bị URL chứa SKU rác (thêm số 9 vào cuối để làm sai SKU thật)
        String invalidUrl = "https://www.lottemart.vn/vi-bdh/product/89360132324169";

        System.out.println("BƯỚC 1: Truy cập URL sản phẩm không tồn tại");
        detailsPage.navigateToUrl(invalidUrl);

        // 2. Kiểm chứng trang lỗi 404 / Not Found
        System.out.println("BƯỚC 2: Kiểm tra giao diện báo lỗi");

        boolean isErrorPage = detailsPage.isNotFoundErrorDisplayed();
        boolean isRedirectedToHome = detailsPage.getCurrentUrl().equals("https://www.lottemart.vn/vi-bdh/");

        Assert.assertTrue(
                isErrorPage || isRedirectedToHome,
                "Lỗi Nghiêm Trọng: Hệ thống không xử lý được URL sai! Giao diện bị trắng hoặc crash."
        );

        // Nếu nó ở lại trang báo lỗi, phải có nút Về trang chủ
        if (isErrorPage && !isRedirectedToHome) {
            Assert.assertTrue(
                    detailsPage.isBackToHomeButtonVisible(),
                    "Lỗi UX: Trang báo lỗi không có nút 'Về trang chủ' để điều hướng người dùng!"
            );
            System.out.println("Hệ thống xử lý lỗi chuẩn UX: Báo lỗi và có nút Về trang chủ.");
        } else {
            System.out.println("Hệ thống xử lý lỗi bằng cách Redirect về trang chủ an toàn.");
        }
    }

    @Test(description = "DT_15 - Kiểm tra tính toàn vẹn dữ liệu khi Refresh (F5) trang")
    public void DT_15_testDataIntegrityOnRefresh() {
        String targetProductName = "Vây Cá Hồi Tẩm Gia Vị SG Food 500G";

        System.out.println("BƯỚC 1: Tìm và truy cập một sản phẩm hợp lệ");
        homePage.search(targetProductName);

        String[] productInfo = searchPage.clickFirstProductAndGetDetails();
        String expectedName = productInfo[0];

        detailsPage.waitForPageLoaded();

        // Lưu lại dữ liệu trước khi F5
        String nameBeforeRefresh = detailsPage.getProductName();
        System.out.println("Tên SP trước khi F5: " + nameBeforeRefresh);

        // Đảm bảo load đúng trước đã
        Assert.assertTrue(
                normalizeText(nameBeforeRefresh).contains(normalizeText(targetProductName)),
                "Lỗi: Không vào được trang sản phẩm từ đầu!"
        );

        System.out.println("BƯỚC 2: Thực hiện F5 tải lại trang");
        detailsPage.refreshPage();

        System.out.println("BƯỚC 3: Kiểm tra trạng thái dữ liệu sau khi F5");

        // Ép chờ load lại các DOM quan trọng
        detailsPage.waitForPageLoaded();
        String nameAfterRefresh = detailsPage.getProductName();
        System.out.println("Tên SP sau khi F5: " + nameAfterRefresh);

        // Kiểm chứng tính toàn vẹn
        Assert.assertEquals(
                nameAfterRefresh,
                nameBeforeRefresh,
                "Lỗi Dữ Liệu: Tên sản phẩm bị thay đổi hoặc mất tích sau khi F5!"
        );

        Assert.assertTrue(
                detailsPage.isDescriptionTabVisible(),
                "Lỗi UI: Khung mô tả sản phẩm bị lỗi/biến mất sau khi F5!"
        );

        System.out.println("Test Case DT_15: PASS! Dữ liệu web rất bền bỉ.");
    }

//    @AfterMethod
//    public void tearDown() {
//        page.close();
//        context.close();
//        browser.close();
//        playwright.close();
//    }
}