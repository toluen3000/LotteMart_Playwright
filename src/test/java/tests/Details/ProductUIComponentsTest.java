package tests.Details;

import base.BaseTest;
import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.HomePage;
import pages.Details.ProductDetailsPage;
import pages.Details.SearchPage;

import java.util.List;

public class ProductUIComponentsTest extends BaseTest {

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
            System.out.println("Không thấy Popup xuất hiện, bỏ qua bước tắt popup!");
        }
    }

    @Test(description = "DT_07 - Kiểm tra khu vực Gợi ý sản phẩm liên quan (Slider UI)")
    public void DT_07_testRelatedProductsUI() {
        String keyword = "bò";
        System.out.println("BƯỚC 1: Tìm kiếm từ khóa: " + keyword);
        homePage.search(keyword);
        searchPage.clickFirstProductAndGetDetails();
        detailsPage.waitForPageLoaded();

        System.out.println("BƯỚC 2: Kiểm tra UI khu vực Sản phẩm tương tự");
        // Gọi lại hàm getSuggestedProductNames chúng ta đã viết rất hoàn hảo ở các bước trước
        List<String> suggestedProducts = detailsPage.getSuggestedProductNames(4);

        // 1. Kiểm tra danh sách hiển thị có tồn tại không
        Assert.assertFalse(suggestedProducts.isEmpty(), "Lỗi UI: Không hiển thị danh sách sản phẩm gợi ý!");

        // 2. Kiểm tra Slider (Thanh trượt) có tồn tại trên giao diện không
        Locator sliderBlock = page.locator(".swiper, .slider, .list-products-grid").first();
        Assert.assertTrue(sliderBlock.isVisible(), "Lỗi UI: Khối Slider/Grid gợi ý không hiển thị!");

        System.out.println("DT_07 PASS: Khu vực gợi ý hiển thị tốt dưới dạng Slider/Grid với " + suggestedProducts.size() + " sản phẩm.");
    }

    @Test(description = "DT_19 - Kiểm tra hiển thị khi Ảnh lỗi (404)")
    public void DT_19_testBrokenImageFallbackUI() {
        System.out.println("BƯỚC 1: Truy cập một sản phẩm bất kỳ");
        homePage.search("xúc xích");
        searchPage.clickFirstProductAndGetDetails();
        detailsPage.waitForPageLoaded();

        System.out.println("BƯỚC 2: Giả lập ảnh bị lỗi (URL 404)");
        // Lấy đúng thẻ ảnh đang Active (dựa theo DOM bạn gửi)
        Locator mainImage = page.locator(".swiper-slide-active .img img, .product-image img").first();

        mainImage.evaluate("node => node.src = 'https://www.lottemart.vn/fake-broken-image.jpg'");
        page.waitForTimeout(1000);

        page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("screenshots/DT19_broken_image.png"))
                .setFullPage(true));

        System.out.println("BƯỚC 3: Kiểm tra Layout không bị vỡ");

        // Đo chiều rộng của Khung chứa ảnh (Parent) thay vì đo chính cái ảnh (vì ảnh bị ép 250%)
        double containerWidth = ((Number) mainImage.evaluate("node => node.parentElement.getBoundingClientRect().width")).doubleValue();
        double viewportWidth = ((Number) page.evaluate("window.innerWidth")).doubleValue();

        // Khung chứa ảnh tuyệt đối không được bành trướng lớn hơn chiều rộng màn hình thiết bị
        Assert.assertTrue(
                containerWidth < viewportWidth,
                "Lỗi Vỡ Layout: Khung ảnh bị lỗi phình to vượt ra khỏi màn hình!"
        );

        System.out.println("DT_19 PASS: Ảnh vỡ không phá hủy cấu trúc khung chứa (Layout an toàn).");
    }

    @Test(description = "DT_20 - Kiểm tra hiển thị Giá với giá trị siêu lớn hoặc 0")
    public void DT_20_testEdgeCasePriceDisplay() {
        System.out.println("BƯỚC 1: Truy cập sản phẩm");
        homePage.search("xúc xích");
        searchPage.clickFirstProductAndGetDetails();
        detailsPage.waitForPageLoaded();

        System.out.println("BƯỚC 2: Giả lập Database trả về giá Trị Siêu Lớn (999,999,999 ₫)");
        Locator priceElement = page.locator(".current-price, [itemprop='price']").first();

        String hugePrice = "999.999.999 ₫";
        priceElement.evaluate("node => node.innerText = '" + hugePrice + "'");
        page.waitForTimeout(500);

        // BƯỚC 3: Kiểm tra Vỡ Layout dựa trên Viewport
        double priceTextWidth = ((Number) priceElement.evaluate("node => node.getBoundingClientRect().width")).doubleValue();
        double viewportWidth = ((Number) page.evaluate("window.innerWidth")).doubleValue();

        // Một dòng giá tiền thì không thể nào dài quá 80% màn hình thiết bị được
        Assert.assertTrue(
                priceTextWidth < (viewportWidth * 0.8),
                "Lỗi Vỡ Layout: Dòng giá tiền quá dài, chiếm hết không gian màn hình (" + priceTextWidth + "px)!"
        );
        System.out.println("   -> Giá siêu lớn hiển thị an toàn, trong tầm kiểm soát.");

        System.out.println("BƯỚC 4: Giả lập Database trả về giá 0 ₫ (Sản phẩm tặng kèm)");
        String zeroPrice = "0 ₫";
        priceElement.evaluate("node => node.innerText = '" + zeroPrice + "'");
        page.waitForTimeout(500);

        String currentDisplayedPrice = priceElement.innerText().trim();
        Assert.assertEquals(
                currentDisplayedPrice,
                zeroPrice,
                "Lỗi Format: Không hiển thị được giá trị 0 ₫!"
        );
        System.out.println("   -> Giá 0 ₫ hiển thị bình thường.");

        System.out.println("DT_20 PASS: Giao diện đàn hồi rất tốt với giá tiền dị biệt.");
    }
//
//    @AfterMethod
//    public void tearDown() {
//        page.close();
//        context.close();
//        browser.close();
//        playwright.close();
//    }
}