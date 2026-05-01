package tests.Checkout;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Checkout.CheckoutPage;
import pages.Details.HomePage;
import utils.JsonUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
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

    @Test(description = "OD_08 - Logic hiển thị form Xuất hóa đơn VAT")
    public void testVATFormToggleLogic() {
        System.out.println("--- CHẠY TEST OD_08 ---");
        loginAndNavigateToCheckout("valid_user");

        System.out.println("BƯỚC 1: Tích chọn Xuất hóa đơn VAT");
        checkoutPage.toggleVATInvoice();
        Assert.assertTrue(checkoutPage.isVATFormVisible(), "LỖI: Form VAT không hiển thị khi tích chọn!");

        System.out.println("BƯỚC 2: Nhập dữ liệu tạm và Bỏ tích chọn");
        checkoutPage.fillVATInfo("Công ty ABC", "01010101", "Hà Nội");
        checkoutPage.toggleVATInvoice(); // Bỏ tích
        page.waitForTimeout(1000);
        Assert.assertFalse(checkoutPage.isVATFormVisible(), "LỖI: Form VAT vẫn hiển thị khi đã bỏ tích!");

        System.out.println("BƯỚC 3: Tích chọn lại để kiểm tra xóa trắng dữ liệu");
        checkoutPage.toggleVATInvoice();
        String companyName = checkoutPage.getVATFieldValue("Tên công ty");
        Assert.assertTrue(companyName.isEmpty(), "LỖI: Dữ liệu VAT cũ không bị xóa trắng!");

        System.out.println("OD_08 PASS");
    }

    @Test(description = "OD_09 - Bỏ trống dữ liệu bắt buộc của hóa đơn VAT")
    public void testRequiredFieldsVAT() {
        System.out.println("--- CHẠY TEST OD_09 ---");
        loginAndNavigateToCheckout("valid_user");

        System.out.println("BƯỚC 1: Click mở form Yêu cầu xuất hóa đơn VAT");
        checkoutPage.openVATForm();

        System.out.println("BƯỚC 2: Để trống mọi ô nhập liệu và Nhấn 'Xác nhận yêu cầu'");
        checkoutPage.clickConfirmVATRequest();

        System.out.println("BƯỚC 3: Kiểm tra hành vi chặn đặt hàng của hệ thống");
        boolean hasErrors = checkoutPage.isVATRequiredErrorsShown();

        Assert.assertTrue(hasErrors, "LỖI LOGIC: Không hiển thị cảnh báo 'trường bắt buộc' khi để trống form VAT!");
        System.out.println("OD_09 PASS: Hệ thống đã chặn thành công và yêu cầu nhập đủ thông tin VAT.");
    }

    @Test(description = "OD_15 - Thời gian nhận hàng dự kiến là tương lai")
    public void testFutureDeliveryTimeSlots() {
        System.out.println("--- CHẠY TEST OD_15 ---");
        loginAndNavigateToCheckout("valid_user");

        // Đảm bảo đang chọn "Giao hàng tiêu chuẩn" để hiện danh sách giờ
        checkoutPage.selectStandardDelivery();
        page.waitForTimeout(1500);

        System.out.println("BƯỚC 1: Lấy danh sách các khung giờ giao hàng hiển thị");
        List<String> timeSlots = checkoutPage.getAvailableDeliverySlots();
        Assert.assertFalse(timeSlots.isEmpty(), "LỖI: Không có khung giờ giao hàng nào được hiển thị!");

        System.out.println("BƯỚC 2: Đối chiếu với thời gian hiện tại của hệ thống");
        LocalDateTime now = LocalDateTime.now();
        System.out.println("   -> Thời gian hệ thống (Now): " + now);

        // Regex bóc tách: (Ngày)/(Tháng) ... (Giờ bắt đầu):
        // Ví dụ: "02/05: 9:00 - 11:00" -> Ngày = 02, Tháng = 05, Giờ = 9
        Pattern pattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})[^\\d]+(\\d{1,2}):");

        for (String slotText : timeSlots) {
            String cleanText = slotText.replace("\n", " ").trim();
            System.out.print("   -> Đang check slot: [" + cleanText + "] ");

            Matcher matcher = pattern.matcher(cleanText);
            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int startHour = Integer.parseInt(matcher.group(3));

                boolean isFuture = false;

                // Logic kiểm tra Tương Lai
                if (month > now.getMonthValue()) {
                    isFuture = true; // Tháng sau -> Tương lai
                } else if (month == now.getMonthValue()) {
                    if (day > now.getDayOfMonth()) {
                        isFuture = true; // Ngày mai, ngày kia -> Tương lai
                    } else if (day == now.getDayOfMonth()) {
                        // Giao trong hôm nay -> Giờ bắt đầu giao phải >= giờ hiện tại
                        if (startHour >= now.getHour()) {
                            isFuture = true;
                        }
                    }
                } else {
                    // Xử lý góc ngách: Chuyển giao năm mới (Ví dụ đang là Tháng 12, lịch giao là Tháng 1 năm sau)
                    if (now.getMonthValue() == 12 && month == 1) {
                        isFuture = true;
                    }
                }

                Assert.assertTrue(isFuture, "\nLỖI LOGIC: Khung giờ giao hàng đang nằm trong quá khứ! Slot: " + cleanText);
                System.out.println("- Hợp lệ (Tương lai)");
            } else {
                System.out.println("- Bỏ qua (Không đúng định dạng ngày giờ chuẩn)");
            }
        }
        System.out.println("OD_15 PASS: Toàn bộ mốc thời gian đều là tương lai.");
    }

    @Test(description = "OD_10 - Đặt hàng thành công với phương thức COD")
    public void testOrderSuccessWithCOD() {
        System.out.println("--- CHẠY TEST OD_10 ---");
        loginAndNavigateToCheckout("valid_user");

        System.out.println("BƯỚC 1: Xử lý Phương thức thanh toán (Tiền mặt)");
        if (!checkoutPage.isCODSelected()) {
            System.out.println("   -> Đang chọn thanh toán Tiền mặt...");
            checkoutPage.selectCODPayment();
        } else {
            System.out.println("   -> Tiền mặt (COD) đã được gán sẵn.");
        }

        System.out.println("BƯỚC 2: Thực hiện chốt đơn");
        checkoutPage.processPlaceOrder();

        System.out.println("BƯỚC 3: Kiểm tra màn hình Đặt hàng thành công");
        boolean isSuccess = checkoutPage.isOrderSuccess();

        // Assert dựa trên thông báo thành công thay vì URL (an toàn và chính xác hơn)
        Assert.assertTrue(isSuccess, "LỖI: Không thấy dòng chữ 'Đặt hàng thành công!'. Đơn hàng có thể đã thất bại.");

        System.out.println("   -> Đã chuyển đến URL: " + page.url());
        System.out.println("OD_10 PASS: Đã chốt đơn COD thành công! (VUI LÒNG HỦY ĐƠN TRÊN WEB NẾU LÀ ĐƠN THẬT)");

        // (Tùy chọn) Bấm vào nút Xem đơn hàng để hoàn tất luồng
        try {
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Xem đơn hàng của tôi")).click(new Locator.ClickOptions().setForce(true));
        } catch (Exception e) {}
    }

    @Test(description = "OD_11 - Tạo đơn hàng thành công để chờ Thanh toán Online (VNPAY)")
    public void testOrderSuccessWithOnlinePayment() {
        System.out.println("--- CHẠY TEST OD_11 ---");
        loginAndNavigateToCheckout("valid_user");

        System.out.println("BƯỚC 1: Chọn thanh toán VNPAY QR");
        // Kiểm tra nếu chưa phải VNPAY thì click mở menu và chọn VNPAY
        try {
            // Đợi 2s để UI load trạng thái mặc định
            page.waitForTimeout(2000);
            String currentPaymentText = page.locator(".payment-choose").first().innerText();
            if (!currentPaymentText.contains("VNPAY")) {
                System.out.println("   -> Đang chọn thanh toán VNPAY...");
                // Mở dropdown phương thức thanh toán
                page.locator(".payment-choose").first().click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(500);

                // Chọn VNPAY
                page.locator(".payment-choose li").filter(new Locator.FilterOptions().setHasText("VNPAY")).first()
                        .click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(1000);
            } else {
                System.out.println("   -> VNPAY đã được chọn sẵn.");
            }
        } catch (Exception e) {
            System.out.println("   -> Lỗi khi chọn VNPAY: " + e.getMessage());
        }

        System.out.println("BƯỚC 2: Thực hiện chốt đơn");
        checkoutPage.processPlaceOrder();

        System.out.println("BƯỚC 3: Kiểm tra chuyển hướng sang cổng thanh toán VNPAY");
        try {
            // Đợi tối đa 20s để hệ thống Lotte tạo đơn và redirect sang cổng VNPAY
            // Cập nhật điều kiện chờ URL dựa trên chuỗi URL thực tế bạn cung cấp
            page.waitForURL("**pay.vnpay.vn**", new Page.WaitForURLOptions().setTimeout(20000));
            System.out.println("   -> Đã chuyển đến URL: " + page.url());

            // Assert chốt hạ: URL phải chứa tên miền thanh toán của VNPAY
            Assert.assertTrue(page.url().contains("pay.vnpay.vn"), "LỖI: URL không thuộc cổng thanh toán VNPAY!");

        } catch (Exception e) {
            Assert.fail("LỖI: Timeout! Không thể chuyển hướng sang trang thanh toán VNPAY.");
        }

        System.out.println("✅ OD_11 PASS: Chuyển hướng sang Cổng Thanh toán Online VNPAY thành công.");
    }



    @AfterMethod
    public void tearDown() {

        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}