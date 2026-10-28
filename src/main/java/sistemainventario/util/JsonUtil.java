package sistemainventario.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        List<T> result = GSON.fromJson(json, listType);
        return result != null ? result : new ArrayList<>();
    }

    public static Gson getGson() { return GSON; }
}
