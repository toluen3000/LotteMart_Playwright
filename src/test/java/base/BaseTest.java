//package base;
//
//import com.microsoft.playwright.Browser;
//import com.microsoft.playwright.BrowserType;
//import com.microsoft.playwright.Page;
//import com.microsoft.playwright.Playwright;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//
//public class BaseTest {
//
//    // install base Components
//    Playwright playwright;
//    Browser browser;
//    protected Page page;
//
//    // set up the test before executes
//    @BeforeMethod
//    public void setUp() {
//        playwright = Playwright.create();
//        // using chromium
//        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
//        page = browser.newPage();
//    }
//
//    // set up close the test
//    @AfterMethod
//    public void tearDown() {
//        if (browser != null) {
//            browser.close();
//        }
//        if (playwright != null) {
//            playwright.close();
//        }
//    }
//}


package base;

import com.microsoft.playwright.*;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BaseTest {

    // Chuyển thành protected để các lớp Test con có thể sử dụng
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();

        browser = playwright.firefox().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(List.of("--start-maximized")));

        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(null));

        page = context.newPage();
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            String testName = result.getMethod().getMethodName();
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            try {
                // Tự động tạo thư mục "screenshots" nếu chưa tồn tại
                Path screenshotDir = Paths.get("screenshots");
                if (!Files.exists(screenshotDir)) {
                    Files.createDirectories(screenshotDir);
                }

                // Lưu ảnh với tên: TênHàmTest_ThờiGian.png
                Path filePath = screenshotDir.resolve(testName + "_" + timeStamp + ".png");
                System.out.println("Kịch bản thất bại! Đang lưu ảnh tại: " + filePath.toAbsolutePath());

                if (page != null) {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(filePath)
                            .setFullPage(true)); // Chụp toàn bộ chiều dài trang web
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi chụp ảnh màn hình: " + e.getMessage());
            }
        }
        //clear
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}