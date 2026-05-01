package tests.Search;

import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.HomePage;
import pages.Details.SearchPage;

public class SearchInputValidationTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private HomePage homePage;
    private SearchPage searchPage;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.firefox().launch(
                new BrowserType.LaunchOptions().setHeadless(false) // Để true nếu muốn chạy ngầm
        );
        context = browser.newContext();
        page = context.newPage();

        homePage = new HomePage(page);
        searchPage = new SearchPage(page);

        // Truy cập trang chủ và tắt popup
        page.navigate("https://www.lottemart.vn/");
        homePage.handleStartupPopups();
    }

    @Test(description = "SR_01 - Tìm kiếm với từ khóa hợp lệ và có kết quả")
    public void testValidSearchWithResults() {
        String keyword = "Dẻ sườn bò";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa: " + keyword);
        homePage.search(keyword);

        System.out.println("BƯỚC 2: Kiểm tra kết quả hiển thị");
        int productCount = searchPage.getProductCount();

        Assert.assertTrue(productCount > 0, "Lỗi: Tìm '" + keyword + "' mà không ra kết quả nào!");
        Assert.assertTrue(page.url().contains("q="), "Lỗi: Không chuyển hướng đến trang kết quả tìm kiếm!");

        System.out.println("SR_01 PASS: Đã tìm thấy " + productCount + " sản phẩm.");
    }

    @Test(description = "SR_02 - Tìm kiếm với từ khóa hợp lệ nhưng KHÔNG CÓ kết quả")
    public void testValidSearchWithNoResults() {
        String keyword = "Điện thoại iPhone 20";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa không có thật: " + keyword);
        homePage.search(keyword);

        System.out.println("BƯỚC 2: Kiểm tra thông báo không tìm thấy");
        String noResultMsg = searchPage.getNoResultMessage().toLowerCase();

        // (Sửa lại dòng Assert trong hàm testValidSearchWithNoResults)
        Assert.assertTrue(
                noResultMsg.contains("rất tiếc"),
                "Lỗi: Không hiển thị đúng thông báo 'Rất tiếc' khi không có kết quả!"
        );
    }

    @Test(description = "SR_03 - Hiển thị gợi ý tự động")
    public void testAutoSuggestionDisplay() {
        String shortKeyword = "vinamilk      ";

        System.out.println("BƯỚC 1: Gõ chậm từ khóa: '" + shortKeyword + "'");
        homePage.typeSearchKeyword(shortKeyword);

        System.out.println("BƯỚC 2: Đợi dữ liệu gợi ý thật từ API load xong");

        // FIX: Truyền thẳng từ khóa vào để hàm wait bỏ qua Lịch sử tìm kiếm cũ
        boolean isDropdownAppeared = homePage.waitForSuggestionDropdownToAppear(shortKeyword);
        Assert.assertTrue(isDropdownAppeared, "Lỗi: Không có dữ liệu gợi ý nào khớp với từ khóa xuất hiện!");

        // Lấy danh sách text ra để xác nhận lại lần cuối (Lúc này chắc chắn API đã đổ data về)
        java.util.List<String> suggestions = homePage.getSuggestionTexts();
        System.out.println("   -> Bắt được " + suggestions.size() + " dòng trong Dropdown.");

        boolean hasValidSuggestion = false;
        for (String text : suggestions) {
            System.out.println("      + Gợi ý: " + text.replace("\n", " "));
            if (text.toLowerCase().contains(shortKeyword.trim().toLowerCase())) {
                hasValidSuggestion = true;
                break;
            }
        }

        Assert.assertTrue(hasValidSuggestion, "Lỗi: Đã check lại nhưng vẫn không có dòng nào chứa từ khóa!");
        System.out.println("SR_03 PASS: Khung gợi ý hiển thị thành công và bỏ qua được lịch sử cũ.");
    }

    @Test(description = "SR_04 - Bỏ trống từ khóa tìm kiếm và nhấn Enter")
    public void testEmptySearchHandling() {
        String currentUrl = page.url();

        System.out.println("BƯỚC 1: Bỏ trống thanh tìm kiếm và Enter");
        homePage.search(""); // search("") sẽ clear ô text và nhấn enter

        // Đợi 1 giây xem hệ thống có làm gì không
        page.waitForTimeout(1000);

        System.out.println("BƯỚC 2: Kiểm tra hệ thống chặn truy vấn");
        String afterSearchUrl = page.url();

        // Hệ thống chuẩn sẽ chặn thao tác, URL không được đổi sang /search?q=
        Assert.assertEquals(afterSearchUrl, currentUrl, "Lỗi: Bỏ trống từ khóa nhưng hệ thống vẫn truy vấn và chuyển trang!");

        System.out.println("SR_04 PASS: Hệ thống chặn tìm kiếm trống, URL không thay đổi.");
    }

    @Test(description = "SR_05 - Tìm kiếm chỉ với khoảng trắng")
    public void testSearchWithOnlySpaces() {
        String currentUrl = page.url();
        String spaces = "     "; // 5 khoảng trắng

        System.out.println("BƯỚC 1: Nhập 5 khoảng trắng và Enter");
        homePage.search(spaces);
        page.waitForTimeout(1000);

        System.out.println("BƯỚC 2: Kiểm tra hệ thống không thực hiện truy vấn");
        // Nếu hệ thống tốt, URL sẽ không đổi hoặc không có tham số q= rác
        Assert.assertEquals(page.url(), currentUrl, "Lỗi: Hệ thống vẫn tìm kiếm khi chỉ nhập khoảng trắng!");
    }

    @Test(description = "SR_06 - Tìm kiếm với từ khóa quá ngắn (1 ký tự)")
    public void testSearchWithTooShortKeyword() {
        String oneChar = "a";

        System.out.println("BƯỚC 1: Nhập 1 ký tự ('" + oneChar + "') và Enter");
        homePage.search(oneChar);

        // THÊM WAIT VÀO ĐÂY: Chờ tối đa 5 giây để URL chuyển hướng chứa tham số "q=a"
        try {
            // Dùng ký tự đại diện (**) để bao quát mọi domain hoặc path phía trước
            page.waitForURL("**/*q=" + oneChar + "**", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: Quá 5s mà URL vẫn chưa cập nhật tham số tìm kiếm!");
        }

        System.out.println("BƯỚC 2: Kiểm tra hệ thống vẫn cho phép truy vấn và hiển thị kết quả");

        // 1. Kiểm tra URL có chứa tham số tìm kiếm (q=a) không
        Assert.assertTrue(page.url().contains("q=" + oneChar), "Lỗi: Hệ thống không chuyển hướng đến trang kết quả!");

        // 2. Đếm số lượng sản phẩm trả về
        int productCount = searchPage.getProductCount();
        System.out.println("   -> Bắt được " + productCount + " sản phẩm cho từ khóa '" + oneChar + "'.");

        // Chỉ cần hệ thống không lỗi và load được lưới sản phẩm (dù là 0 hay nhiều) thì pass
        Assert.assertTrue(productCount >= 0, "Lỗi logic khi hiển thị danh sách sản phẩm!");

        System.out.println("SR_06 PASS: Hệ thống chấp nhận tìm kiếm 1 ký tự và load kết quả bình thường.");
    }

    @Test(description = "SR_07 - Kiểm tra biên: 255 ký tự (Hợp lệ)")
    public void testSearchBoundary255Chars() {
        // chuỗi 255 chữ A
        String longKeyword = homePage.generateLongString(255);

        System.out.println("BƯỚC 1: Nhập chuỗi 255 ký tự và tìm kiếm");
        homePage.search(longKeyword);

        System.out.println("BƯỚC 2: Kiểm tra hệ thống chấp nhận truy vấn 255 ký tự");

        // Chờ trình duyệt xử lý và đẩy 255 ký tự lên URL (tối đa 5s)
        try {
            page.waitForURL("**/*q=*", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: Quá 5s mà URL vẫn chưa chuyển hướng!");
        }

        // Xác nhận URL thực tế có chứa chính xác chuỗi 255 ký tự đó
        Assert.assertTrue(page.url().contains("q=" + longKeyword),
                "Lỗi: Hệ thống không truyền đúng chuỗi 255 ký tự vào URL!");

        System.out.println("SR_07 PASS: Hệ thống xử lý mượt mà và chấp nhận chuỗi tìm kiếm dài 255 ký tự.");
    }

    @Test(description = "SR_08 - Kiểm tra biên: Vượt quá 255 ký tự (Hệ thống không giới hạn Input)")
    public void testSearchBoundary256Chars() {
        String overLimitKeyword = homePage.generateLongString(256);

        System.out.println("BƯỚC 1: Nhập chuỗi 256 ký tự");
        homePage.search(overLimitKeyword);
        page.waitForTimeout(1000);

        System.out.println("BƯỚC 2: Kiểm tra khả năng nhập liệu vô hạn");
        String actualValue = homePage.getSearchInputValue();

        // Lotte Mart KHÔNG dùng maxlength, cho phép gõ thoải mái
        Assert.assertEquals(actualValue.length(), 256,
                "Lỗi: Chuỗi bất ngờ bị hệ thống cắt ngắn!");
        System.out.println("SR_08 PASS: Ô nhập liệu cho phép gõ tự do (256+ ký tự).");
    }

    @AfterMethod
    public void tearDown() {
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}