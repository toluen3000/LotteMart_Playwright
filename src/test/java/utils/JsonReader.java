package utils;

import com.google.gson.Gson;
import dto.ProductData;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class JsonReader {

    public static <T> T[] readJson(String fileName, Class<T[]> clazz) {
        Gson gson = new Gson();
        InputStream inputStream = JsonReader.class
                .getClassLoader()
                .getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("KHÔNG TÌM THẤY FILE: " + fileName);
        }

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            T[] data = gson.fromJson(reader, clazz);

            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("DATA JSON RỖNG: " + fileName);
            }

            System.out.println("Loaded " + data.length + " test cases from " + fileName);

            return data;

        } catch (Exception e) {
            throw new RuntimeException("LỖI ĐỌC JSON: " + fileName, e);
        }
    }

    public static <T> Object[][] toTestNGFormat(T[] dataArray) {
        Object[][] testNgData = new Object[dataArray.length][1];

        for (int i = 0; i < dataArray.length; i++) {
            testNgData[i][0] = dataArray[i];
        }

        return testNgData;
    }
}