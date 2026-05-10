package tests.Details;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.ProductDetailsPage;

public class ProductQuantityTest extends BaseTest {

    // Chỉ giữ lại duy nhất Page Object
    private ProductDetailsPage detailsPage;

    // Khởi tạo Page Object từ biến 'page' của BaseTest
    @BeforeMethod
    public void initPage() {
        detailsPage = new ProductDetailsPage(page);
    }

    // Hàm tiện ích để load sản phẩm mặc định cho các test cần thiết
    private void loadDefaultProduct() {
        detailsPage.navigateToProduct("https://www.lottemart.vn/vi-bdh/product/sua-almond-breeze-original-946ml-8850096818506-p59578");
    }

    @Test(description = "DT_06 - Test btnPlus n btnMinus")
    public void DT_06_testIncreaseDecreaseQuantity() {
        loadDefaultProduct(); // Gọi link web ở đây

        int initial = detailsPage.getCurrentQuantity();

        detailsPage.clickIncreaseQuantity();
        Assert.assertEquals(detailsPage.getCurrentQuantity(), initial + 1);

        detailsPage.clickDecreaseQuantity();
        Assert.assertEquals(detailsPage.getCurrentQuantity(), initial);
    }

    @Test(description = "DT_06 - Test invalid input")
    public void DT_06_testInvalidQuantityInput() {
        loadDefaultProduct();

        detailsPage.enterQuantity("abc");
        int value = detailsPage.getCurrentQuantity();
        Assert.assertTrue(value >= 1, "Không cho nhập chữ");
    }

    @Test(description = "DT_06 - Negative Case")
    public void DT_06_testNegativeInput() {
        loadDefaultProduct();

        detailsPage.typeQuantity("-");
        String raw = detailsPage.getRawQuantity();
        System.out.println("Raw value: " + raw);

        page.waitForTimeout(2000); // Đã giảm từ 10s xuống 2s để test chạy nhanh hơn

        int value = detailsPage.getCurrentQuantity();
        Assert.assertTrue(value >= 1, "Giá trị phải >= 1 sau khi xử lý");
    }

    @Test(description = "DT_06 - Test Disable Minus Button")
    public void DT_06_testMinusDisabledAtOne() {
        loadDefaultProduct();

        detailsPage.enterQuantity("1");
        Assert.assertTrue(detailsPage.isMinusDisable(), "Nút - phải bị disable khi = 1");
    }

    @Test(description = "DT_13 - input value > stock")
    public void DT_13_testQuantityExceedStock() {
        loadDefaultProduct();

        int stock = detailsPage.getMaxQuantity();
        int exceedValue = stock + 5;
        detailsPage.typeQuantity(String.valueOf(exceedValue));

        page.locator("body").click(); // Đổi page.click() thành chuẩn Playwright

        int actual = detailsPage.getCurrentQuantity();
        System.out.println("Stock: " + stock + " | Input: " + exceedValue + " | Actual: " + actual);

        if (actual <= stock) {
            Assert.assertTrue(true);
        } else if (detailsPage.isStockWarningDisplayed()) {
            Assert.assertTrue(true);
        } else {
            Assert.fail("Không chặn vượt tồn kho");
        }
    }

    @Test(description = "DT_14 - input very large num")
    public void DT_14_testVeryLargeQuantity() {
        loadDefaultProduct();

        System.out.println("BƯỚC 1: Lấy số lượng tồn kho tối đa (Max Stock)");
        int max = detailsPage.getMaxQuantity();
        System.out.println("   -> Tồn kho tối đa: " + max);

        System.out.println("BƯỚC 2: Nhập số lượng cực lớn (888)");
        detailsPage.typeQuantity("888");

        System.out.println("BƯỚC 3: Trigger sự kiện Validation (Click ra ngoài ô nhập liệu)");
        page.locator("body").click();
        page.waitForTimeout(500);

        System.out.println("BƯỚC 4: Kiểm tra kết quả hiển thị");
        String raw = detailsPage.getRawQuantity();
        System.out.println("   -> Raw value trên UI: " + raw);

        int value = detailsPage.getCurrentQuantity();
        System.out.println("   -> Giá trị đã parse (int): " + value);

        Assert.assertNotNull(raw, "Input bị null");
        Assert.assertFalse(raw.equalsIgnoreCase("NaN"), "Không được NaN");
        Assert.assertTrue(value <= max, "LỖI: Số lượng hiện tại (" + value + ") vẫn lớn hơn tồn kho tối đa (" + max + ")!");
    }

    @Test(description = "DT_08 - Out of stock")
    public void DT_08_testOutOfStock() {
        // Test này có link riêng, nên KHÔNG GỌI loadDefaultProduct()
        detailsPage.navigateToProduct("https://www.lottemart.vn/vi-bdh/product/hop-tp-hokkaido-290ml-8935275210620-p87783");

        Assert.assertTrue(
                detailsPage.isStockTextVisible(),
                "Phải hiển thị text tạm thời hết hàng"
        );

        String text = detailsPage.getStockText();
        Assert.assertTrue(
                text.contains("Tạm thời hết hàng"), "Không hiển thị còn x sản phẩm"
        );
    }

    @Test(description = "DT_09 - Low stock warning")
    public void DT_09_testLowStockWarning() {
        // Test này có link riêng, nên KHÔNG GỌI loadDefaultProduct()
        detailsPage.navigateToProduct("https://www.lottemart.vn/vi-bdh/product/kr-bo-tui-gn-va-hop-tt-400-710ml-2-8936148090189-p71645");

        Assert.assertTrue(
                detailsPage.isStockWarningVisible(),
                "Phải hiển thị cảnh báo sắp hết hàng"
        );

        String text = detailsPage.getStockWarningText();
        Assert.assertTrue(
                text.contains("Chỉ còn"),
                "Text phải chứa 'Chỉ còn'"
        );
    }
}