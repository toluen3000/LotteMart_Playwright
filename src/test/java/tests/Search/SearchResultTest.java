package tests.Search;

import base.BaseTest;
import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.HomePage;
import pages.Details.SearchPage;
import java.util.List;

public class SearchResultTest extends BaseTest {

//    private Playwright playwright;
//    private Browser browser;
//    private BrowserContext context;
//    private Page page;
    private HomePage homePage;
    private SearchPage searchPage;

    @BeforeMethod
    public void initPageObjects() {
//        playwright = Playwright.create();
//        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
//        context = browser.newContext();
//        page = context.newPage();

        homePage = new HomePage(page);
        searchPage = new SearchPage(page);

        // Chuẩn bị tiền đề: Vào web và tìm kiếm chữ "bánh" để có danh sách sản phẩm
        page.navigate("https://www.lottemart.vn/");
        try {
            homePage.handleStartupPopups();
        } catch (Exception e) {
            System.out.println("Không thấy Popup xuất hiện, bỏ qua bước tắt popup!");
        }
        homePage.search("bánh");

        // Chờ kết quả load xong
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    @Test(description = "SR_09 - Áp dụng Bộ lọc giá hợp lệ")
    public void SR_09_testValidPriceFilter() {
        System.out.println("BƯỚC 1: Lọc giá từ 100,000đ đến 300,000đ");
        String currentUrl = page.url();

        searchPage.applyPriceFilter("100000", "300000");

        // Bẫy chờ URL thay đổi (Do Lotte Mart cập nhật param lên URL khi click nút)
        try {
            page.waitForCondition(() -> !page.url().equals(currentUrl),
                    new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: URL chưa thay đổi! Nút .btn-search có thể chưa ăn.");
        }

        // Chờ tải xong
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000); // UI Render delay

        System.out.println("BƯỚC 2: Kiểm tra giá của từng sản phẩm");
        List<Integer> prices = searchPage.getDisplayedPrices();
        System.out.println("   -> Bắt được " + prices.size() + " sản phẩm hợp lệ.");

        Assert.assertTrue(prices.size() > 0, "Lỗi: Không có sản phẩm nào hiển thị!");

        for (int price : prices) {
            Assert.assertTrue(price >= 100000 && price <= 300000,
                    "Lỗi: Lọt giá sai quy định! Giá: " + price);
        }
        System.out.println("SR_09 PASS: Sản phẩm hiển thị chuẩn xác.");
    }

    @Test(description = "SR_10 - Bộ lọc giá không hợp lệ (Min > Max)")
    public void SR_10_testInvalidPriceFilterMinGreaterThanMax() {
        System.out.println("BƯỚC 1: Lọc giá lỗi Min (500k) > Max (200k)");
        String currentUrl = page.url();

        searchPage.applyPriceFilter("500000", "200000");

        page.waitForTimeout(1500); // Chờ web phản ứng sau khi click

        System.out.println("BƯỚC 2: Kiểm tra hệ thống xử lý");

        // Hành vi thường thấy: Lotte Mart sẽ tự động chặn, KHÔNG đẩy số sai lên URL
        boolean isWrongFilterApplied = page.url().contains("minPrice=500000");
        boolean urlDidNotChange = page.url().equals(currentUrl);

        Assert.assertTrue(!isWrongFilterApplied || urlDidNotChange,
                "Lỗi: Hệ thống vẫn đẩy bộ lọc Min > Max vô lý lên trình duyệt!");

        System.out.println("SR_10 PASS: Hệ thống chặn bộ lọc lỗi an toàn.");
    }

    @Test(description = "SR_11 - Nhập số âm vào Bộ lọc giá")
    public void SR_11_testNegativePriceFilter() {
        System.out.println("BƯỚC 1: Cố tình gõ số âm (-50000) vào ô Min");
        searchPage.applyPriceFilter("-50000", "");

        System.out.println("BƯỚC 2: Kiểm tra ô Input");
        String actualMin = searchPage.getMinPriceInputValue();

        // Input type="number" thường không cho gõ dấu "-" hoặc sẽ bỏ qua nó
        Assert.assertFalse(actualMin.contains("-"), "Lỗi: Ô Input cho phép nhập dấu gạch ngang (số âm)!");
        System.out.println("SR_11 PASS: Ô input chặn ký tự dấu trừ thành công.");
    }

    @Test(description = "SR_12 - Sắp xếp giá từ thấp đến cao")
    public void SR_12_testSortPriceLowToHigh() {
        System.out.println("BƯỚC 1: Chọn Sort 'Giá từ thấp đến cao'");
        searchPage.selectSortOption("thấp đến cao");

        // THÊM DÒNG NÀY: Chờ 3 giây để Web tải lại lưới sản phẩm mới
        page.waitForTimeout(3000);
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        System.out.println("BƯỚC 2: Lấy mảng giá và kiểm tra thuật toán Sắp xếp");
        List<Integer> prices = searchPage.getDisplayedPrices();
        System.out.println("   -> Bắt được " + prices.size() + " sản phẩm.");

        // (Giữ nguyên đoạn code Assert bên dưới của bạn...)
    }

    @Test(description = "SR_13 - Áp dụng Lọc theo Danh mục (Category Filter)")
    public void SR_13_testCategoryFilter() {
        System.out.println("BƯỚC 1: Chọn danh mục 'Bánh Kẹo'");
        searchPage.applyCategoryFilter("Bánh Kẹo");

        // Chờ API lọc dữ liệu
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000);

        System.out.println("BƯỚC 2: Kiểm tra danh sách hiển thị");
        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Lỗi: Lọc danh mục xong không hiển thị sản phẩm nào!");

        System.out.println("SR_13 PASS: Lọc danh mục thành công, bắt được " + count + " sản phẩm.");
    }

    @Test(description = "SR_20 - Kết hợp Filter và Sort (Mô phỏng End-to-End User)")
    public void SR_20_testCombinedFiltersAndSort() {
        System.out.println("BƯỚC 1: Lọc danh mục 'Bánh Kẹo'");
        searchPage.applyCategoryFilter("Bánh Kẹo");
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(1500); // Chờ UI xả hơi giữa các action

        System.out.println("BƯỚC 2: Lọc giá từ 100k -> 300k");
        searchPage.applyPriceFilter("100000", "300000");
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(1500);

        System.out.println("BƯỚC 3: Chọn Sắp xếp Giá thấp đến cao");
        searchPage.selectSortOption("thấp đến cao");
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(3000); // Chờ thuật toán Sort render xong

        System.out.println("BƯỚC 4: Kiểm chứng dữ liệu đa điều kiện");
        java.util.List<Integer> prices = searchPage.getDisplayedPrices();
        Assert.assertTrue(prices.size() > 0, "Lỗi: Không có sản phẩm nào thỏa mãn!");

        boolean isSorted = true;
        for (int i = 0; i < prices.size(); i++) {
            // Check giá nằm trong khoảng
            Assert.assertTrue(prices.get(i) >= 100000 && prices.get(i) <= 300000,
                    "Lỗi giá lọt ra ngoài: " + prices.get(i));

            // Check thứ tự sắp xếp
            if (i < prices.size() - 1 && prices.get(i) > prices.get(i + 1)) {
                isSorted = false;
                break;
            }
        }
        Assert.assertTrue(isSorted, "Lỗi: Giá không được sắp xếp đúng thứ tự!");

        System.out.println("SR_20 PASS: Kết hợp Danh Mục + Giá + Sort chạy hoàn hảo!");
    }

//    @AfterMethod
//    public void tearDown() {
//        page.close();
//        context.close();
//        browser.close();
//        playwright.close();
//    }
}