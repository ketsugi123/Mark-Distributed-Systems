package grpc.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonParserUtils {
    public static JsonObject parseJsonString(String jsonString) {
        JsonElement jsonElement = JsonParser.parseString(jsonString);

        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        } else {
            throw new IllegalArgumentException("The provided string is not a valid JSON object.");
        }
    }

    public static String getStringProperty(JsonObject jsonObject, String propertyName) {
        if (jsonObject.has(propertyName)) {
            return jsonObject.get(propertyName).getAsString();
        } else {
            throw new IllegalArgumentException("Property '" + propertyName + "' does not exist in the JSON object.");
        }
    }

    public static int getIntProperty(JsonObject jsonObject, String propertyName) {
        if (jsonObject.has(propertyName)) {
            return jsonObject.get(propertyName).getAsInt();
        } else {
            throw new IllegalArgumentException("Property '" + propertyName + "' does not exist in the JSON object.");
        }
    }

    public static boolean getBooleanProperty(JsonObject jsonObject, String propertyName) {
        if (jsonObject.has(propertyName)) {
            return jsonObject.get(propertyName).getAsBoolean();
        } else {
            throw new IllegalArgumentException("Property '" + propertyName + "' does not exist in the JSON object.");
        }
    }
}