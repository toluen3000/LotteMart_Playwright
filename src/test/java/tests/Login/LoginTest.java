package tests.Login;

import base.BaseTest;
import org.testng.annotations.Test;
import pages.Login.LoginPage;

public class LoginTest extends BaseTest {

    @Test
    public void testLogin() {
        page.navigate("http://lottemart.vn/login");

        LoginPage loginPage = new LoginPage(page);

        loginPage.login("lukakuvn04@gmail.com", "Quankhonlam06!00000000000");

        page.waitForTimeout(3000);
        System.out.println("Login test executed");
    }

}
