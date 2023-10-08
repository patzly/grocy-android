package xyz.zedler.patrick.grocy.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

public class Converters {
  private static Gson gson = new Gson();

  @TypeConverter
  public static Map<String, String> stringToMap(String value) {
    Type type = new TypeToken<Map<String, String>>() {}.getType();
    return gson.fromJson(value, type);
  }

  @TypeConverter
  public static String mapToString(Map<String, String> map) {
    return gson.toJson(map);
  }
}
