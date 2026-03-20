package dev.langchain4j.openapi;

import dev.langchain4j.internal.Json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a copy of langchain ToolExecutionRequestUtil. Consider to PR to make it the existing one public.
 */
public class ToolExecutionRequestUtil {

    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");

    private ToolExecutionRequestUtil() {
    }

    private static final Type MAP_TYPE = new ParameterizedType() {

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, Object.class};
        }

        @Override
        public Type getRawType() {
            return Map.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    };

    /**
     * 将 arguments 转换为 map。
     *
     * @param arguments json 字符串
     * @return map
     */
    static Map<String, Object> argumentsAsMap(String arguments) {
        return Json.fromJson(removeTrailingComma(arguments), MAP_TYPE);
    }

    /**
     * 在 JSON 字符串中于右括号/右中括号之前移除尾部逗号。
     *
     * @param json JSON 字符串
     * @return 已修正的 JSON 字符串
     */
    static String removeTrailingComma(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
        return matcher.replaceAll("$1");
    }
}
