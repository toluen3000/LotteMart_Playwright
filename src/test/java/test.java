import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class test {
        public static void main(String[] args) {

        // first commit - sample
        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setSlowMo(50)
            );

            Page page = browser.newPage();
            System.out.println("Đang điều hướng tới Google...");
            page.navigate("https://www.google.com");

            page.waitForTimeout(3000);

            System.out.println("Chạy script hoàn tất!");
        }

        }

}
