package utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;

public class JsonUtils {

    private static final String FILE_PATH = "src/test/resources/testdata/testdata.json";

    public static JsonObject getUserData(String userType) {
        try (FileReader reader = new FileReader(FILE_PATH)) {
            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();

            return rootObj.getAsJsonObject(userType);

        } catch (IOException e) {
            System.err.println("Lỗi không tìm thấy hoặc không đọc được file JSON ở: " + FILE_PATH);
            e.printStackTrace();
            return null;
        }
    }
}