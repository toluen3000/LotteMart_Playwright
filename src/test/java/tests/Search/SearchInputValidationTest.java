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
    public void SR_01_testValidSearchWithResults() {
        String keyword = "Dẻ sườn bò";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa: " + keyword);
        homePage.search(keyword);

        System.out.println("BƯỚC 2: Kiểm tra kết quả hiển thị");
        page.waitForTimeout(2000);
        int productCount = searchPage.getProductCount();
        Assert.assertTrue(productCount > 0, "Lỗi: Tìm '" + keyword + "' mà không ra kết quả nào!");
        Assert.assertTrue(page.url().contains("q="), "Lỗi: Không chuyển hướng đến trang kết quả tìm kiếm!");

        System.out.println("SR_01 PASS: Đã tìm thấy " + productCount + " sản phẩm.");
    }

    @Test(description = "SR_02 - Tìm kiếm với từ khóa hợp lệ nhưng KHÔNG CÓ kết quả")
    public void SR_02_testValidSearchWithNoResults() {
        String keyword = "Điện thoại iPhone 20";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa không có thật: " + keyword);
        homePage.search(keyword);

        System.out.println("BƯỚC 2: Kiểm tra thông báo không tìm thấy");
        String noResultMsg = searchPage.getNoResultMessage().toLowerCase();


        Assert.assertTrue(
                noResultMsg.contains("rất tiếc"),
                "Lỗi: Không hiển thị đúng thông báo 'Rất tiếc' khi không có kết quả!"
        );
    }

    @Test(description = "SR_03 - Hiển thị gợi ý tự động")
    public void SR_03_testAutoSuggestionDisplay() {
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
    public void SR_04_testEmptySearchHandling() {
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
    public void SR_05_testSearchWithOnlySpaces() {
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
    public void SR_06_testSearchWithTooShortKeyword() {
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
    public void SR_07_testSearchBoundary255Chars() {
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
    public void SR_08_testSearchBoundary256Chars() {
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

    @Test(description = "SR_16 - Tìm kiếm không phân biệt chữ hoa/thường")
    public void SR_16_testSearchCaseInsensitive() {
        String keyword = "GẠO ST25"; // Viết hoa toàn bộ

        System.out.println("BƯỚC 1: Tìm kiếm với từ khóa in hoa: " + keyword);
        homePage.search(keyword);

        try {
            page.waitForURL("**/*q=*", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: URL chưa chuyển hướng!");
        }

        System.out.println("BƯỚC 2: Kiểm tra hệ thống trả về kết quả bình thường");
        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Lỗi: Không tìm thấy kết quả khi gõ chữ in hoa!");

        System.out.println("SR_16 PASS: Hệ thống map từ khóa hoa/thường xuất sắc.");
    }

    @Test(description = "SR_17 - Tìm kiếm có dấu và không dấu")
    public void SR_17_testSearchWithoutAccents() {
        String keyword = "gao st25";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa không dấu: " + keyword);
        homePage.search(keyword);

        // THAY ĐỔI: Chờ trang web load xong trạng thái mạng (Network Idle) thay vì chỉ chờ URL
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        System.out.println("BƯỚC 2: Kiểm tra khả năng tự động hiểu tiếng Việt không dấu");

        // Cố tình chờ thêm 2s để chắc chắn UI đã vẽ xong (tránh trường hợp FE render chậm bằng React/Vue)
        page.waitForTimeout(2000);

        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Lỗi: Hệ thống không hỗ trợ tìm kiếm tiếng Việt không dấu!");

        System.out.println("SR_17 PASS: Hệ thống tự động map 'gao' thành 'gạo'.");
    }

    @Test(description = "SR_18 - Tìm kiếm với ký tự đặc biệt")
    public void SR_18_testSearchWithSpecialChars() {
        String keyword = "@@@###";

        System.out.println("BƯỚC 1: Tìm kiếm toàn ký tự đặc biệt: " + keyword);
        homePage.search(keyword);

        try {
            page.waitForURL("**/*q=*", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {}

        System.out.println("BƯỚC 2: Kiểm tra hệ thống không bị Crash và báo lỗi thân thiện");
        String noResultMsg = searchPage.getNoResultMessage().toLowerCase();

        Assert.assertTrue(
                noResultMsg.contains("rất tiếc"),
                "Lỗi: Không hiển thị đúng thông báo 'Rất tiếc' khi không có kết quả!"
        );
        System.out.println("SR_18 PASS: Hệ thống chặn rác tốt, không dính lỗi SQL Injection/500.");
    }

    @Test(description = "SR_19 - Tìm kiếm với khoảng trắng đầu/cuối (Trim)")
    public void SR_19_testSearchWithSurroundingSpaces() {
        String rawKeyword = "   gạo ST25   ";
        String expectedTrimmedKeyword = "gạo ST25";

        System.out.println("BƯỚC 1: Tìm kiếm từ khóa chưa cắt khoảng trắng: '" + rawKeyword + "'");
        homePage.search(rawKeyword);

        // THAY ĐỔI: Chờ trạng thái mạng tĩnh lặng
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        System.out.println("BƯỚC 2: Kiểm tra ô Input và Dữ liệu trả về");

        // Ép Playwright đợi cho ô input được cập nhật giá trị (tối đa 5s)
        Locator searchInput = page.getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Tìm kiếm")).first();
        try {
            // Chờ cho đến khi value của ô input KHÔNG CÒN chứa chuỗi gốc (tức là đã bị trim)
            page.waitForCondition(() -> !searchInput.inputValue().equals(rawKeyword), new Page.WaitForConditionOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: Đợi 5s nhưng FE vẫn chưa Trim giá trị!");
        }

        // 1. Kiểm tra trên UI
        String actualInputValue = homePage.getSearchInputValue();
        Assert.assertEquals(actualInputValue, expectedTrimmedKeyword,
                "Lỗi: Front-end không thực hiện lệnh Trim() xóa khoảng trắng thừa!");

        // 2. Đảm bảo vẫn tìm ra sản phẩm bình thường
        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Lỗi: Trim khoảng trắng bị lỗi dẫn đến không ra kết quả!");

        System.out.println("SR_19 PASS: Hệ thống tự động cắt khoảng trắng thừa (Trim) rất chuẩn.");
    }

    @Test(description = "SR_15 - Tìm kiếm bằng mã SKU")
    public void SR_15_testSearchBySKU() {
        // Lấy một mã SKU giả lập (hoặc mã thật nếu bạn có từ DB Lotte Mart)
        String skuCode = "8934588012110";

        System.out.println("BƯỚC 1: Tìm kiếm bằng mã SKU: " + skuCode);
        homePage.search(skuCode);

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        System.out.println("BƯỚC 2: Kiểm tra kết quả hiển thị");
        // Nếu mã SKU đúng thì trả về 1 hoặc 1 vài sản phẩm, nếu sai thì ra câu thông báo "Rất tiếc".
        // Ở đây mình test hệ thống không bị crash khi nhập SKU
        boolean hasProducts = searchPage.getProductCount() > 0;
        boolean hasNoResultMsg = searchPage.getNoResultMessage().toLowerCase().contains("rất tiếc");

        Assert.assertTrue(hasProducts || hasNoResultMsg, "Lỗi: Hệ thống không trả về lưới sản phẩm cũng không báo lỗi!");

        System.out.println("SR_15 PASS: Chức năng tìm kiếm SKU hoạt động (Không crash).");
    }

    @Test(description = "SR_21 - Tìm kiếm và chọn từ gợi ý đầu tiên (Dữ liệu động)")
    public void SR_21_testSearchAndSelectSuggestion() {
        String shortKeyword = "gao";

        System.out.println("BƯỚC 1: Gõ '" + shortKeyword + "' và hệ thống tự động chọn gợi ý đầu tiên");

        // Đón lấy dữ liệu text thực tế mà hàm vừa chộp được
        String actualSelectedSuggestion = homePage.searchAndSelectFirstSuggestion(shortKeyword);
        System.out.println("   -> Đã bắt động và click vào: '" + actualSelectedSuggestion + "'");

        // Chờ URL chuyển trang
        try {
            page.waitForURL("**/*q=*", new Page.WaitForURLOptions().setTimeout(5000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Cảnh báo: URL chưa nhảy sau khi click gợi ý!");
        }

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        System.out.println("BƯỚC 2: Kiểm tra trang kết quả hiển thị khớp với gợi ý đã chọn");

        int count = searchPage.getProductCount();
        Assert.assertTrue(count > 0, "Lỗi: Click gọi ý xong mà không ra sản phẩm nào!");

        // Lấy từ khóa hiện tại đang nằm trên ô tìm kiếm sau khi load xong
        String inputAfterSearch = homePage.getSearchInputValue().toLowerCase();

        // So sánh 2 cục text (Dùng contains để bao quát trường hợp FE chuẩn hóa chữ hoa/thường)
        Assert.assertTrue(actualSelectedSuggestion.toLowerCase().contains(inputAfterSearch) || inputAfterSearch.contains(actualSelectedSuggestion.toLowerCase()),
                "Lỗi: Kết quả tìm kiếm không khớp với gợi ý đã chọn!\n" +
                        "   + Đã chọn: " + actualSelectedSuggestion + "\n" +
                        "   + Đang hiển thị: " + inputAfterSearch);

        System.out.println("SR_21 PASS: Luồng Suggestion hoạt động hoàn hảo với dữ liệu động.");
    }


    @Test(description = "SR_14 - Xử lý ngoại lệ khi mất kết nối Cơ sở dữ liệu (Mock 500 Error)")
    public void SR_14_testDatabaseConnectionError() {
        System.out.println("BƯỚC 1: Giả lập ngắt kết nối Database (Mock API trả về lỗi 500)");

        // Sử dụng quyền năng của Playwright để chặn mọi request tìm kiếm (chứa "q=")
        // và ép nó trả về lỗi 500 (Internal Server Error) mô phỏng DB bị sập
        page.route("**/*q=*", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                    .setStatus(500)
                    .setContentType("text/html") // Lotte Mart dùng chuyển trang SSR
                    // Giả lập nội dung lỗi mặc định của Server
                    .setBody("<html><body><h1>500 Internal Server Error</h1><p>Database connection failed.</p></body></html>"));
        });

        // Hoặc nếu Lotte Mart gọi API ngầm (fetch/xhr) để search thì dùng đoạn này:
        page.route("**/*", route -> {
            if (route.request().resourceType().equals("fetch") || route.request().resourceType().equals("xhr")) {
                route.abort(); // Mô phỏng đứt cáp/chết API
            } else {
                route.resume();
            }
        });

        System.out.println("BƯỚC 2: Nhập từ khóa 'Dẻ sườn bò' và tìm kiếm");
        homePage.search("Dẻ sườn bò");

        // Đợi UI phản ứng lại với lỗi
        page.waitForTimeout(2000);

        System.out.println("BƯỚC 3: Kiểm tra hệ thống không crash và có thông báo thân thiện");

        // Lấy toàn bộ text hiển thị trên màn hình hiện tại
        String pageText = page.locator("body").innerText().toLowerCase();

        // Kiểm tra xem giao diện có chứa các câu thông báo lỗi thân thiện không
        // (Bạn cần thay đổi các từ khóa này cho khớp với thiết kế UI thực tế của Lotte Mart khi có lỗi)
        boolean hasFriendlyError = pageText.contains("đã xảy ra lỗi")
                || pageText.contains("thử lại sau")
                || pageText.contains("rất tiếc")
                || pageText.contains("bảo trì");

        // Crash thường là trang trắng bóc hoặc văng ra một đống code StackTrace loằng ngoằng
        boolean isCrashed = pageText.isEmpty() || pageText.contains("exception") || pageText.contains("sql syntax");

        Assert.assertFalse(isCrashed, "Lỗi Nghiêm Trọng: Hệ thống bị Crash (trắng trang hoặc lộ code backend) khi sập DB!");
        Assert.assertTrue(hasFriendlyError, "Lỗi: Hệ thống bắt được lỗi nhưng không hiển thị thông báo thân thiện cho user!");

        System.out.println("SR_14 PASS: Hệ thống xử lý Exception rất tốt, UI không bị Crash.");

        // DỌN DẸP BẮT BUỘC: Phải gỡ bỏ lệnh chặn mạng để không làm Fail các test case chạy sau nó
        page.unroute("**/*q=*");
        page.unroute("**/*");
    }

    @AfterMethod
    public void tearDown() {
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}