package tests.Checkout;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Checkout.CheckoutPage;
import pages.Details.HomePage;
import utils.JsonUtils;
import java.util.regex.Pattern;

public class CheckoutBasicTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private HomePage homePage;
    private CheckoutPage checkoutPage;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        page = context.newPage();

        homePage = new HomePage(page);
        checkoutPage = new CheckoutPage(page);
    }

    private void loginFromHomePage(String userKey) {
        JsonObject user = JsonUtils.getUserData(userKey);
        if (user == null) throw new RuntimeException("Lỗi: Không tìm thấy user " + userKey);
        String phone = user.get("phone").getAsString();
        String pass = user.get("password").getAsString();

        page.navigate("https://www.lottemart.vn/");
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        try { homePage.handleStartupPopups(); } catch (Exception e) {}

        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Đăng nhập")).first().click();

        Locator phoneInput = page.getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("Email/Số điện thoại *")).first();
        phoneInput.click();
        phoneInput.fill(phone);

        Locator passInput = page.getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("Mật khẩu *")).first();
        passInput.click();
        passInput.fill(pass);

        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Đăng nhập")).first().click();

        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(10000));
        } catch (Exception e) {}
        page.waitForTimeout(2000);
    }

    // =========================================================
    // CÁC TEST CASES OD (ORDER DATA)
    // =========================================================

    @Test(description = "OD_01 - Tự động điền địa chỉ mặc định (Có sẵn địa chỉ)")
    public void testAutoFillAddress() {
        System.out.println("--- CHẠY TEST OD_01 ---");
        loginFromHomePage("valid_user");

        // Thêm vào giỏ hàng và hàm này sẽ ĐƯA BẠN VÀO LUÔN TRANG GIỎ HÀNG
        homePage.quickAddToCart("bánh");

        System.out.println("BƯỚC 1: Từ giỏ hàng, bấm nút Thanh toán (Flow 3)");
        // Dùng đúng Locator Flow 3 của bạn
        Locator payButton = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Thanh toán$"))).nth(1);
        payButton.click(new Locator.ClickOptions().setForce(true));

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000); // Chờ load trang Checkout

        System.out.println("BƯỚC 2: Kiểm tra khối Thông tin người nhận");
        // Kiểm tra xem nút báo "Không có địa chỉ" có xuất hiện không
        boolean isMissingAddress = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Hiện tại bạn không có địa ch")).first().isVisible();

        Assert.assertFalse(isMissingAddress, "Lỗi: User CÓ địa chỉ nhưng hệ thống lại báo 'Không có địa chỉ'!");
        System.out.println("✅ OD_01 PASS: Địa chỉ đã được tự động điền.");
    }

    @Test(description = "OD_02 - Xử lý khi User KHÔNG CÓ địa chỉ giao hàng")
    public void testNoAddressHandling() {
        System.out.println("--- CHẠY TEST OD_02 ---");
        loginFromHomePage("user_no_address");

        homePage.quickAddToCart("kẹo");

        System.out.println("BƯỚC 1: Click Thanh toán từ Giỏ hàng");
        Locator payButton = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Thanh toán$"))).nth(1);
        payButton.click(new Locator.ClickOptions().setForce(true));

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000);

        System.out.println("BƯỚC 2: Kiểm tra giao diện báo thiếu địa chỉ (Flow 2)");
        boolean isMissingAddressBtnVisible = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Hiện tại bạn không có địa ch")).first().isVisible();

        Assert.assertTrue(isMissingAddressBtnVisible, "Lỗi: Account mới tinh nhưng không thấy hiển thị cảnh báo thiếu địa chỉ!");
        System.out.println("✅ OD_02 PASS: Hệ thống cảnh báo chưa có địa chỉ chuẩn xác.");
    }
//
//    @Test(description = "OD_03 - Truy cập trang Đặt hàng khi giỏ hàng rỗng")
//    public void testEmptyCartCheckoutBlock() {
//        System.out.println("--- CHẠY TEST OD_03 ---");
//        loginFromHomePage("valid_user");
//
//        System.out.println("BƯỚC 1: Vào trang /cart và kiểm tra giỏ hàng");
//        page.navigate("https://www.lottemart.vn/cart");
//        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
//        page.waitForTimeout(2000);
//
//        // KIỂM TRA TRẠNG THÁI RỖNG DỰA VÀO HTML THỰC TẾ
//        boolean isEmptyCart = page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING,
//                new Page.GetByRoleOptions().setName("Chưa có sản phẩm!")).first().isVisible();
//
//        if (!isEmptyCart) {
//            System.out.println("   -> Giỏ hàng đang có đồ. Tiến hành dọn dẹp...");
//
//            // Kết hợp chính xác class và text từ HTML của bạn để không bắt nhầm
//            Locator deleteAllBtn = page.locator("button.btn-delete:has-text('Xóa tất cả sản phẩm')").first();
//
//            if (deleteAllBtn.isVisible()) {
//                deleteAllBtn.click(new Locator.ClickOptions().setForce(true));
//
//                Locator confirmDeleteBtn = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
//                        new Page.GetByRoleOptions().setName("Xóa")).first();
//
//                confirmDeleteBtn.waitFor(new Locator.WaitForOptions()
//                        .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
//                        .setTimeout(5000));
//                confirmDeleteBtn.click(new Locator.ClickOptions().setForce(true));
//
//                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
//                page.waitForTimeout(2000);
//                System.out.println("   -> Đã dọn sạch giỏ hàng.");
//            }
//        } else {
//            System.out.println("   -> Giỏ hàng đã rỗng sẵn (0 sản phẩm). Bỏ qua bước xóa.");
//        }
//
//        System.out.println("BƯỚC 2: Cố tình truy cập thẳng URL /checkout");
//        page.navigate("https://www.lottemart.vn/checkout");
//        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
//        page.waitForTimeout(1500);
//
//        System.out.println("BƯỚC 3: Kiểm tra hành vi chặn (Bị đá ra 404 hoặc báo lỗi)");
//        String currentUrl = page.url();
//        boolean isBlockedBy404 = currentUrl.contains("404");
//        boolean isRedirected = !currentUrl.contains("checkout") || isBlockedBy404;
//
//        boolean isErrorShown = page.getByText("mặt hàng nào", new Page.GetByTextOptions().setExact(false)).isVisible()
//                || page.getByText("giỏ hàng trống", new Page.GetByTextOptions().setExact(false)).isVisible();
//
//        Assert.assertTrue(isRedirected || isErrorShown, "Lỗi: Trang Checkout vẫn mở ra khi không có sản phẩm!");
//        System.out.println("✅ OD_03 PASS: Xác nhận chặn vào trang Checkout khi giỏ rỗng thành công.");
//    }

    @Test(description = "OD_04 - Tính toán chính xác Tổng tiền mặc định")
    public void testInvoiceMathCalculation() {
        System.out.println("--- CHẠY TEST OD_04 ---");
        loginFromHomePage("valid_user");

        homePage.quickAddToCart("bánh");

        // Vào checkout
        Locator payButton = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Thanh toán$"))).nth(1);
        payButton.click(new Locator.ClickOptions().setForce(true));

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000);

        int subtotal = checkoutPage.getSubtotal();
        int bagFee = checkoutPage.getBagFee();
        int shippingFee = checkoutPage.getShippingFee();
        int finalTotalDisplayed = checkoutPage.getFinalTotal();

        int expectedCalculatedTotal = subtotal + bagFee + shippingFee;

        System.out.println("   => Code tự tính (Hàng + Túi + Ship): " + expectedCalculatedTotal);
        System.out.println("   => Hệ thống hiển thị: " + finalTotalDisplayed);

        Assert.assertEquals(finalTotalDisplayed, expectedCalculatedTotal, "LỖI KẾT TOÁN: Cộng sai tổng tiền!");
        System.out.println("OD_04 PASS: Thuật toán cộng tiền trên giao diện chính xác.");
    }

    @AfterMethod
    public void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}