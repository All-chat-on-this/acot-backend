package com.chat.allchatonthis.common.util.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 */
@Slf4j
public class JsonUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // 忽略 null 值
        objectMapper.registerModules(new JavaTimeModule()); // 解决 LocalDateTime 的序列化
    }

    /**
     * 初始化 objectMapper 属性
     * <p>
     * 通过这样的方式，使用 Spring 创建的 ObjectMapper Bean
     *
     * @param objectMapper ObjectMapper 对象
     */
    public static void init(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    @SneakyThrows
    public static String toJsonString(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }

    @SneakyThrows
    public static String toJsonPrettyString(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, Type type) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字符串解析成指定类型的对象
     * 使用 {@link #parseObject(String, Class)} 时，在@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) 的场景下，
     * 如果 text 没有 class 属性，则会报错。此时，使用这个方法，可以解决。
     *
     * @param text 字符串
     * @param clazz 类型
     * @return 对象
     */
    public static <T> T parseObject2(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        return JSONUtil.toBean(text, clazz);
    }

    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", bytes, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 JSON 字符串成指定类型的对象，如果解析失败，则返回 null
     *
     * @param text 字符串
     * @param typeReference 类型引用
     * @return 指定类型的对象
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(byte[] text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 判断字符串是否为 JSON 类型的字符串
     * @param str 字符串
     */
    public static boolean isJsonObject(String str) {
        return JSONUtil.isTypeJSONObject(str);
    }

    /**
     * Extract a value from a nested JSON structure using dot notation path
     *
     * @param data The data structure to extract from
     * @param path Path in dot notation (e.g., "choices[0].message.content")
     * @return The extracted value as a string or null if not found
     */
    @SuppressWarnings("unchecked")
    public static String extractValueFromPath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // Handle array notation like choices[0]
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));

                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    if (map.containsKey(arrayName) && map.get(arrayName) instanceof List) {
                        List<Object> array = (List<Object>) map.get(arrayName);
                        if (index < array.size()) {
                            current = array.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Regular property access
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    return null;
                }
            }
        }

        return current != null ? current.toString() : null;
    }

    /**
     * Set a value in a nested JSON structure using dot notation path
     *
     * @param data The data structure to modify
     * @param path Path in dot notation (e.g., "messages[0].content")
     * @param value The value to set
     */
    @SuppressWarnings("unchecked")
    public static void setValueByPath(Map<String, Object> data, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            // Handle array notation
            if (part.contains("[") && part.contains("]")) {
                String arrayName = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));

                // Create array if it doesn't exist
                if (!current.containsKey(arrayName) || !(current.get(arrayName) instanceof List)) {
                    current.put(arrayName, new ArrayList<>());
                }

                List<Object> array = (List<Object>) current.get(arrayName);

                // Ensure array is large enough
                while (array.size() <= index) {
                    array.add(new HashMap<String, Object>());
                }

                // Move to the next level
                if (!(array.get(index) instanceof Map)) {
                    array.set(index, new HashMap<String, Object>());
                }

                current = (Map<String, Object>) array.get(index);
            } else {
                // Regular property access
                if (!current.containsKey(part) || !(current.get(part) instanceof Map)) {
                    current.put(part, new HashMap<String, Object>());
                }
                current = (Map<String, Object>) current.get(part);
            }
        }

        // Set the final value
        String lastPart = parts[parts.length - 1];

        // Handle array notation for the last part
        if (lastPart.contains("[") && lastPart.contains("]")) {
            String arrayName = lastPart.substring(0, lastPart.indexOf('['));
            int index = Integer.parseInt(lastPart.substring(lastPart.indexOf('[') + 1, lastPart.indexOf(']')));

            // Create array if it doesn't exist
            if (!current.containsKey(arrayName) || !(current.get(arrayName) instanceof List)) {
                current.put(arrayName, new ArrayList<>());
            }

            List<Object> array = (List<Object>) current.get(arrayName);

            // Ensure array is large enough
            while (array.size() <= index) {
                array.add(null);
            }

            array.set(index, value);
        } else {
            // Regular property set
            current.put(lastPart, value);
        }
    }
}
