package tests.Details;

import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.Details.ProductDetailsPage;

import java.util.List;

public class ProductQuantityTest {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private ProductDetailsPage detailsPage;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setArgs(List.of("--start-maximized")));
        context = browser.newContext();
        page = context.newPage();
        detailsPage = new ProductDetailsPage(page);
        // using fixed url
        detailsPage.navigateToProduct("https://www.lottemart.vn/vi-bdh/product/sua-almond-breeze-original-946ml-8850096818506-p59578");
    }


    @Test(description = "DT_06 - Test btnPlus n btnMinus")
    public void testIncreaseDecreaseQuantity() {
        int initial = detailsPage.getCurrentQuantity();

        detailsPage.clickIncreaseQuantity();
        Assert.assertEquals(detailsPage.getCurrentQuantity(), initial + 1);

        detailsPage.clickDecreaseQuantity();
        Assert.assertEquals(detailsPage.getCurrentQuantity(), initial);
    }

    @Test(description = "DT_06 - Test invalid input")
    public void testInvalidQuantityInput() {
        detailsPage.enterQuantity("abc");

        int value = detailsPage.getCurrentQuantity();
        Assert.assertTrue(value >= 1, "Không cho nhập chữ");

    }

    @Test(description = "DT_06 - Negative Case")
    public void testNegativeInput() {
        detailsPage.typeQuantity("-");


        String raw = detailsPage.getRawQuantity();
        System.out.println("Raw value: " + raw);

        page.waitForTimeout(10000);

        int value = detailsPage.getCurrentQuantity();
        Assert.assertTrue(value >= 1, "Giá trị phải >= 1 sau khi xử lý");
    }

    @Test(description = "DT_06 - Test Disable Minus Button")
    public void testMinusDisabledAtOne() {
        detailsPage.enterQuantity("1");

        Assert.assertTrue(detailsPage.isMinusDisable(), "Nút - phải bị disable khi = 1");
    }

    @Test(description = "DT_13 - input value > stock")
    public void testQuantityExceedStock() {
        int stock = detailsPage.getMaxQuantity();

        int exceedValue = stock + 5;
        detailsPage.typeQuantity(String.valueOf(exceedValue));

        page.click("body");

        int actual = detailsPage.getCurrentQuantity();
        System.out.println("Stock: " + stock + " | Input: " + exceedValue + " | Actual: " + actual);

        if (actual <= stock) {
            Assert.assertTrue(true);
        }
        else if (detailsPage.isStockWarningDisplayed()) {
            Assert.assertTrue(true);
        }
        else {
            Assert.fail("Không chặn vượt tồn kho");
        }
    }


    @Test(description = "DT_14 - input very large num")
    public void testVeryLargeQuantity() {
        detailsPage.typeQuantity("9999");

        page.click("body"); // trigger validate

        String raw = detailsPage.getRawQuantity();
        System.out.println("Raw value: " + raw);

        int value = detailsPage.getCurrentQuantity();


        Assert.assertNotNull(raw, "Input bị null");


        Assert.assertFalse(raw.equalsIgnoreCase("NaN"), "Không được NaN");


        int max = detailsPage.getMaxQuantity();
        Assert.assertTrue(value <= max, "Không được vượt quá max");
    }

    @Test(description = "DT_08 - Out of stock")
    public void testOutOfStock() {
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
    public void testLowStockWarning() {
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

    @AfterMethod
    public void tearDown() {
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}