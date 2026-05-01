package tests.Checkout;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Checkout.CheckoutPage;
import pages.Details.HomePage;
import utils.JsonUtils;
import java.util.regex.Pattern;

public class DeliveryPaymentTest {
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

    private void loginAndNavigateToCheckout(String userKey) {
        // (Sử dụng lại logic Login từ bài trước - mình rút gọn ở đây để tập trung vào test case)
        JsonObject user = JsonUtils.getUserData(userKey);
        if (user == null) throw new RuntimeException("Lỗi: Không tìm thấy user " + userKey);
        String phone = user.get("phone").getAsString();
        String pass = user.get("password").getAsString();

        page.navigate("https://www.lottemart.vn/");
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        try { homePage.handleStartupPopups(); } catch (Exception e) {}

        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK, new Page.GetByRoleOptions().setName("Đăng nhập")).first().click();
        page.getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Email/Số điện thoại *")).first().fill(phone);
        page.getByRole(com.microsoft.playwright.options.AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Mật khẩu *")).first().fill(pass);
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Đăng nhập")).first().click();

        try { page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(10000)); } catch (Exception e) {}
        page.waitForTimeout(2000);

        // Đảm bảo có sản phẩm trong giỏ và vào thẳng Checkout
        homePage.quickAddToCart("bánh");
        Locator payButton = page.locator("div.btn-primary:has-text('Thanh toán')").first();
//        Locator payButton = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Thanh toán$"))).nth(1);
        payButton.click(new Locator.ClickOptions().setForce(true));

        payButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));

        int retry = 0;
        while (!payButton.isEnabled() && retry < 10) {
            page.waitForTimeout(500);
            retry++;
        }

        payButton.scrollIntoViewIfNeeded();
        payButton.click(new Locator.ClickOptions().setForce(true));

        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        page.waitForTimeout(2000);
    }

    @Test(description = "OD_04 - Tính toán chính xác Tổng tiền mặc định")
    public void testDefaultInvoiceCalculation() {
        System.out.println("--- CHẠY TEST OD_04 ---");
        loginAndNavigateToCheckout("valid_user");

        int shipFee = checkoutPage.getShippingFee();
        int finalTotal = checkoutPage.getFinalTotal();

        System.out.println("   -> Phí ship: " + shipFee);
        System.out.println("   -> Tổng tiền: " + finalTotal);

        Assert.assertTrue(finalTotal > 0, "Lỗi: Tổng tiền không hiển thị!");
    }

    @Test(description = "OD_05 - Cập nhật Tổng tiền khi đổi phương thức giao hàng sang Nhận tại cửa hàng")
    public void testUpdateTotalOnStorePickup() {
        System.out.println("--- CHẠY TEST OD_05 ---");
        loginAndNavigateToCheckout("valid_user");

        page.waitForTimeout(2000); // Chờ render số tiền

        int initialShippingFee = checkoutPage.getShippingFee(); // Thực tế sẽ lấy được 5000
        int initialTotal = checkoutPage.getFinalTotal(); // Thực tế sẽ lấy được 143300

        System.out.println("   -> Phí ship ban đầu: " + initialShippingFee);
        System.out.println("   -> Tổng tiền ban đầu: " + initialTotal);

        System.out.println("BƯỚC 2: Đổi sang Nhận hàng tại cửa hàng");
        checkoutPage.selectStorePickup();

        int newShippingFee = checkoutPage.getShippingFee();
        int newTotal = checkoutPage.getFinalTotal();

        Assert.assertEquals(newShippingFee, 0, "LỖI: Phí ship không về 0!");
        Assert.assertEquals(newTotal, initialTotal - initialShippingFee, "LỖI: Tổng tiền không trừ đi phí ship!");
        System.out.println("OD_05 PASS");
    }

    @Test(description = "OD_07 - Ràng buộc phương thức thanh toán khi Nhận tại cửa hàng")
    public void testPaymentConstraintOnStorePickup() {
        System.out.println("--- CHẠY TEST OD_07 ---");
        loginAndNavigateToCheckout("valid_user");

        checkoutPage.selectStorePickup();

        // Kiểm tra xem thông báo "không áp dụng Tiền mặt" có hiện lên không
        boolean isCODBlocked = checkoutPage.isCODPaymentDisabled();

        Assert.assertTrue(isCODBlocked, "LỖI: Không hiển thị cảnh báo chặn Tiền mặt khi nhận tại kho!");
        System.out.println("OD_07 PASS");
    }

    @Test(description = "OD_06 - Kiểm tra có phí ship cho đơn hàng dưới 200k khi đổi lại phương thức giao hàng")
    public void testUpdateTotalOnStandardDelivery() {
        System.out.println("--- CHẠY TEST OD_06 ---");
        loginAndNavigateToCheckout("valid_user");

        // 1. Kiểm tra đơn hàng có thực sự dưới 200k không
        int subtotal = checkoutPage.getSubtotal();
        System.out.println("   -> Tổng tiền hàng: " + subtotal);

        if (subtotal >= 200000) {
            System.out.println("⚠CẢNH BÁO: Đơn hàng >= 200k, phí ship sẽ bằng 0 theo chính sách web.");
        }

        // 2. Chuyển sang Nhận tại cửa hàng (Baseline phí ship = 0)
        System.out.println("BƯỚC 1: Chọn Nhận hàng tại cửa hàng");
        checkoutPage.selectStorePickup();
        int pickupTotal = checkoutPage.getFinalTotal();
        Assert.assertEquals(checkoutPage.getShippingFee(), 0, "Lỗi: Phí ship chưa về 0 khi nhận tại kho!");

        // 3. Đổi lại Giao hàng tiêu chuẩn
        System.out.println("BƯỚC 2: Đổi lại Giao hàng tiêu chuẩn");
        checkoutPage.selectStandardDelivery();
        page.waitForTimeout(3000); // Đợi API tính ship

        int standardShip = checkoutPage.getShippingFee();
        int finalTotal = checkoutPage.getFinalTotal();
        System.out.println("   -> Phí ship thực tế lấy được: " + standardShip);

        // 4. Assert: Nếu đơn < 200k thì phí ship BẮT BUỘC phải lớn hơn 0
        if (subtotal < 200000) {
            Assert.assertTrue(standardShip > 0, "LỖI: Đơn dưới 200k nhưng phí ship vẫn bằng 0!");
            Assert.assertEquals(finalTotal, pickupTotal + standardShip, "LỖI: Tổng tiền cuối cùng không cộng thêm phí ship!");
            System.out.println("OD_06 PASS: Phí ship " + standardShip + "đ đã được áp dụng.");
        } else {
            Assert.assertEquals(standardShip, 0, "LỖI: Đơn trên 200k không được freeship!");
            System.out.println("OD_06 PASS: Đơn trên 200k nên phí ship bằng 0 là đúng.");
        }
    }



    @AfterMethod
    public void tearDown() {
        browser.close();
        playwright.close();
    }
}