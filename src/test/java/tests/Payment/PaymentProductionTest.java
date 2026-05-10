package tests.Payment;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Checkout.CheckoutPage;
import pages.Details.HomePage;
import pages.Payment.PaymentPage;
import utils.JsonUtils;

public class PaymentProductionTest extends BaseTest {

//    private Playwright playwright;
//    private Browser browser;
//    private BrowserContext context;
//    private Page page;
    private HomePage homePage;
    private PaymentPage paymentPage;


    @BeforeMethod
    public void initPageObjects() {
        homePage = new HomePage(page);
        paymentPage = new PaymentPage(page);
//        checkoutPage = new CheckoutPage(page);
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

        page.getByRole(com.microsoft.playwright.options.AriaRole.LINK, new Page.GetByRoleOptions().setName("Đăng nhập"))
                .first().click(new Locator.ClickOptions().setForce(true));
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


    @Test(description = "E2E_04 (PM_05) - Khách hàng chủ động hủy giao dịch trên cổng thanh toán VNPAY")
    public void test_E2E_04_PM05_CancelOnlinePayment() {
        System.out.println("--- CHẠY TEST E2E_04 (PM_05) ---");
        loginAndNavigateToCheckout("valid_user");

        paymentPage.selectVNPayPayment();
        paymentPage.processPlaceOrder();

        try {
            page.waitForURL("**pay.vnpay.vn**", new Page.WaitForURLOptions().setTimeout(20000));
        } catch (Exception e) {
            Assert.fail("LỖI: Không thể chuyển sang VNPAY để thực hiện test hủy!");
        }

        paymentPage.cancelVNPayTransaction();

        try {
            page.waitForURL("**lottemart.vn**", new Page.WaitForURLOptions().setTimeout(20000));

            boolean isSuccessPage = page.url().contains("/success") || page.locator("text='Đặt hàng thành công'").isVisible();
            Assert.assertFalse(isSuccessPage, "LỖI FATAL: Hủy đơn trên VNPAY nhưng Lotte Mart lại báo đặt hàng thành công!");

            boolean isCancelMessageVisible = page.locator("text='Thanh toán thất bại', text='Đã hủy', text='Lỗi thanh toán'").first().isVisible();

        } catch (Exception e) {
            Assert.fail("LỖI: Timeout! VNPAY không chuyển hướng lại về Lotte Mart sau khi nhấn Hủy.");
        }

        System.out.println("E2E_04 PASS: Luồng Hủy giao dịch tích hợp VNPAY hoạt động chính xác.");
    }

}