package pages.Login;

import com.microsoft.playwright.Page;

public class LoginPage {

    private Page page;

    // Constructor
    public LoginPage(Page page) {
        this.page = page;
    }

    // using id of element to locate fields
    private String usernameInput = "#floatingInput";
    private String passwordInput = "#floatingPassword";
    private String loginButton = "button[type='submit']";

    // Actions
    public void login(String username, String password) {
        page.fill(usernameInput, username);
        page.fill(passwordInput, password);
        page.click(loginButton);
    }
}
