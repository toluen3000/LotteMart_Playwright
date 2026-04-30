package utils;

public class StringUtils {
    public static String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim();
    }

    public static String normalizePrice(String price) {
        if (price == null) return "";

        return price
                .replace("\u00A0", "")
                .replace("₫", "")
                .replace(".", "")
                .trim();
    }
}
