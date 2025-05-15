package com.chat.allchatonthis.common.util.http;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.TableMap;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.chat.allchatonthis.common.exception.ServiceException;
import com.chat.allchatonthis.common.util.json.JsonUtils;
import com.chat.allchatonthis.entity.dataobject.ConversationMessageDO;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.CONFIG_NOT_EXISTS;

/**
 * HTTP 工具类
 */
@Slf4j
public class HttpUtils {

    @SuppressWarnings("unchecked")
    public static String replaceUrlQuery(String url, String key, String value) {
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());
        // 先移除
        TableMap<CharSequence, CharSequence> query = (TableMap<CharSequence, CharSequence>)
                ReflectUtil.getFieldValue(builder.getQuery(), "query");
        query.remove(key);
        // 后添加
        builder.addQuery(key, value);
        return builder.build();
    }

    /**
     * 拼接 URL
     * <p>
     * copy from Spring Security OAuth2 的 AuthorizationEndpoint 类的 append 方法
     *
     * @param base     基础 URL
     * @param query    查询参数
     * @param keys     query 的 key，对应的原本的 key 的映射。例如说 query 里有个 key 是 xx，实际它的 key 是 extra_xx，则通过 keys 里添加这个映射
     * @param fragment URL 的 fragment，即拼接到 # 中
     * @return 拼接后的 URL
     */
    public static String append(String base, Map<String, ?> query, Map<String, String> keys, boolean fragment) {
        UriComponentsBuilder template = UriComponentsBuilder.newInstance();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        URI redirectUri;
        try {
            // assume it's encoded to start with (if it came in over the wire)
            redirectUri = builder.build(true).toUri();
        } catch (Exception e) {
            // ... but allow client registrations to contain hard-coded non-encoded values
            redirectUri = builder.build().toUri();
            builder = UriComponentsBuilder.fromUri(redirectUri);
        }
        template.scheme(redirectUri.getScheme()).port(redirectUri.getPort()).host(redirectUri.getHost())
                .userInfo(redirectUri.getUserInfo()).path(redirectUri.getPath());

        if (fragment) {
            StringBuilder values = new StringBuilder();
            if (redirectUri.getFragment() != null) {
                String append = redirectUri.getFragment();
                values.append(append);
            }
            for (String key : query.keySet()) {
                if (values.length() > 0) {
                    values.append("&");
                }
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }
                values.append(name).append("={").append(key).append("}");
            }
            if (values.length() > 0) {
                template.fragment(values.toString());
            }
            UriComponents encoded = template.build().expand(query).encode();
            builder.fragment(encoded.getFragment());
        } else {
            for (String key : query.keySet()) {
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }
                template.queryParam(name, "{" + key + "}");
            }
            template.fragment(redirectUri.getFragment());
            UriComponents encoded = template.build().expand(query).encode();
            builder.query(encoded.getQuery());
        }
        return builder.build().toUriString();
    }

    public static String[] obtainBasicAuthorization(HttpServletRequest request) {
        String clientId;
        String clientSecret;
        // 先从 Header 中获取
        String authorization = request.getHeader("Authorization");
        authorization = StrUtil.subAfter(authorization, "Basic ", true);
        if (StringUtils.hasText(authorization)) {
            authorization = Base64.decodeStr(authorization);
            clientId = StrUtil.subBefore(authorization, ":", false);
            clientSecret = StrUtil.subAfter(authorization, ":", false);
            // 再从 Param 中获取
        } else {
            clientId = request.getParameter("client_id");
            clientSecret = request.getParameter("client_secret");
        }

        // 如果两者非空，则返回
        if (StrUtil.isNotEmpty(clientId) && StrUtil.isNotEmpty(clientSecret)) {
            return new String[]{clientId, clientSecret};
        }
        return null;
    }

    /**
     * HTTP post 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     * <p>
     * 为什么要封装该方法，因为 HttpUtil 默认封装的方法，没有允许传递 headers 参数
     *
     * @param url         URL
     * @param headers     请求头
     * @param requestBody 请求体
     * @return 请求结果
     */
    public static String post(String url, Map<String, String> headers, String requestBody) {
        try (HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .body(requestBody)
                .execute()) {
            return response.body();
        }
    }

    /**
     * HTTP get 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     * <p>
     * 为什么要封装该方法，因为 HttpUtil 默认封装的方法，没有允许传递 headers 参数
     *
     * @param url     URL
     * @param headers 请求头
     * @return 请求结果
     */
    public static String get(String url, Map<String, String> headers) {
        try (HttpResponse response = HttpRequest.get(url)
                .addHeaders(headers)
                .execute()) {
            return response.body();
        }
    }

    /**
     * Prepares the request data (headers and body) for API calls based on configuration
     *
     * @param config      The user configuration containing API settings
     * @param messageText The message text to include in the request (can be null for some requests)
     * @return Map containing headers and requestBody
     */
    public static Map<String, Object> prepareRequestData(UserConfigDO config, String messageText) {
        // Prepare headers
        Map<String, String> headers = new HashMap<>(config.getHeaders() != null ? config.getHeaders() : new HashMap<>());

        // Check if API key is encrypted and decrypt if needed
        String apiKey = config.getApiKey();
        if (apiKey != null && apiKey.startsWith("enc:") && StringUtils.hasText(config.getSecretKey())) {
            try {
                apiKey = decryptApiKey(apiKey.substring(4), config.getSecretKey());
            } catch (Exception e) {
                log.error("Error decrypting API key", e);
                throw new ServiceException(CONFIG_NOT_EXISTS, "Failed to decrypt API key: " + e.getMessage());
            }
        }

        // Add API key to headers based on placement
        if ("header".equals(config.getApiKeyPlacement()) || config.getApiKeyPlacement() == null) {
            headers.put("Authorization", "Bearer " + apiKey);
        } else if ("custom_header".equals(config.getApiKeyPlacement()) && config.getApiKeyHeader() != null) {
            headers.put(config.getApiKeyHeader(), apiKey);
        }

        // Ensure Content-Type header exists
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        // Prepare request body by cloning the template
        Map<String, Object> requestBody = new HashMap<>(config.getRequestTemplate());

        // Add API key to body if needed
        if ("body".equals(config.getApiKeyPlacement()) && config.getApiKeyBodyPath() != null) {
            JsonUtils.setValueByPath(requestBody, config.getApiKeyBodyPath(), apiKey);
        }

        // Add the message text to the request body at the specified path if provided
        if (StringUtils.hasText(messageText) && StringUtils.hasText(config.getRequestMessageGroupPath())) {
            // Create a message object with the role and content
            Map<String, Object> messageObj = new HashMap<>();
            String rolePath = StringUtils.hasText(config.getRequestRolePathFromGroup()) ?
                    config.getRequestRolePathFromGroup() : "role";

            String textPath = StringUtils.hasText(config.getRequestTextPathFromGroup()) ?
                    config.getRequestTextPathFromGroup() : "content";

            String roleValue = StringUtils.hasText(config.getRequestUserRoleField()) ?
                    config.getRequestUserRoleField() : "user";

            messageObj.put(rolePath, roleValue);
            messageObj.put(textPath, messageText);

            // Set the message in the message group
            JsonUtils.setValueByPath(requestBody, config.getRequestMessageGroupPath(), new ArrayList<Map<String, Object>>());

            // Add the message to the message group
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(messageObj);
            JsonUtils.setValueByPath(requestBody, config.getRequestMessageGroupPath(), messages);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("headers", headers);
        result.put("requestBody", requestBody);

        return result;
    }

    /**
     * Prepares the request data (headers and body) for API calls based on configuration
     * This version supports message group functionality with conversation history
     *
     * @param config               The user configuration containing API settings
     * @param messageText          The message text to include in the request
     * @param conversationMessages Previous messages in the conversation
     * @return Map containing headers and requestBody
     */
    public static Map<String, Object> prepareRequestData(UserConfigDO config, String messageText, List<ConversationMessageDO> conversationMessages) {
        // Prepare headers
        Map<String, String> headers = new HashMap<>(config.getHeaders() != null ? config.getHeaders() : new HashMap<>());

        // Check if API key is encrypted and decrypt if needed
        String apiKey = config.getApiKey();
        if (apiKey != null && apiKey.startsWith("enc:") && StringUtils.hasText(config.getSecretKey())) {
            try {
                apiKey = decryptApiKey(apiKey.substring(4), config.getSecretKey());
            } catch (Exception e) {
                log.error("Error decrypting API key", e);
                throw new ServiceException(CONFIG_NOT_EXISTS, "Failed to decrypt API key: " + e.getMessage());
            }
        }

        // Add API key to headers based on placement
        if ("header".equals(config.getApiKeyPlacement()) || config.getApiKeyPlacement() == null) {
            headers.put("Authorization", "Bearer " + apiKey);
        } else if ("custom_header".equals(config.getApiKeyPlacement()) && config.getApiKeyHeader() != null) {
            headers.put(config.getApiKeyHeader(), apiKey);
        }

        // Ensure Content-Type header exists
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        // Prepare request body by cloning the template
        Map<String, Object> requestBody = new HashMap<>(config.getRequestTemplate());

        // Add API key to body if needed
        if ("body".equals(config.getApiKeyPlacement()) && config.getApiKeyBodyPath() != null) {
            JsonUtils.setValueByPath(requestBody, config.getApiKeyBodyPath(), apiKey);
        }

        // Extract field names and default values
        String rolePath = StringUtils.hasText(config.getRequestRolePathFromGroup()) ?
                config.getRequestRolePathFromGroup() : "role";

        String textPath = StringUtils.hasText(config.getRequestTextPathFromGroup()) ?
                config.getRequestTextPathFromGroup() : "content";

        String userRoleValue = StringUtils.hasText(config.getRequestUserRoleField()) ?
                config.getRequestUserRoleField() : "user";

        String assistantRoleValue = StringUtils.hasText(config.getRequestAssistantField()) ?
                config.getRequestAssistantField() : "assistant";

        // Create the message group and add conversation history
        if (StringUtils.hasText(config.getRequestMessageGroupPath()) && (conversationMessages != null || messageText != null)) {
            List<Map<String, Object>> messages = new ArrayList<>();

            // Add previous messages
            if (conversationMessages != null) {
                for (ConversationMessageDO message : conversationMessages) {
                    // Skip system messages
                    if ("system".equals(message.getRole())) {
                        continue;
                    }

                    Map<String, Object> messageObj = new HashMap<>();
                    // Map the role values appropriately
                    if ("user".equals(message.getRole())) {
                        messageObj.put(rolePath, userRoleValue);
                    } else if ("assistant".equals(message.getRole())) {
                        messageObj.put(rolePath, assistantRoleValue);
                    } else {
                        // Use the role as is for any other roles
                        messageObj.put(rolePath, message.getRole());
                    }

                    messageObj.put(textPath, message.getContent());
                    messages.add(messageObj);
                }
            }

            // Add the new user message
            if (messageText != null) {
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put(rolePath, userRoleValue);
                userMessage.put(textPath, messageText);
                messages.add(userMessage);
            }

            // Set the messages in the request body
            JsonUtils.setValueByPath(requestBody, config.getRequestMessageGroupPath(), messages);
        }
        // For backward compatibility
        else if (messageText != null && StringUtils.hasText(config.getRequestTextPathFromGroup())) {
            JsonUtils.setValueByPath(requestBody, config.getRequestTextPathFromGroup(), messageText);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("headers", headers);
        result.put("requestBody", requestBody);

        return result;
    }

    /**
     * Method to decrypt an API key using AES decryption with the provided secret key
     * This implementation is compatible with CryptoJS.AES encryption used in the frontend
     *
     * @param encryptedApiKey The encrypted API key
     * @param secretKey       The secret key to use for decryption
     * @return The decrypted API key
     */
    public static String decryptApiKey(String encryptedApiKey, String secretKey) {
        try {
            // CryptoJS uses OpenSSL format which includes salt
            // First, decode Base64
            byte[] cipherData = java.util.Base64.getDecoder().decode(encryptedApiKey);

            // CryptoJS format: "Salted__" + 8 byte salt + actual ciphertext
            byte[] saltBytes = new byte[8];
            byte[] cipherBytes = new byte[cipherData.length - 16];

            // Extract salt and ciphertext
            System.arraycopy(cipherData, 8, saltBytes, 0, 8);
            System.arraycopy(cipherData, 16, cipherBytes, 0, cipherData.length - 16);

            // Generate key and IV using OpenSSL EVP_BytesToKey derivation
            byte[][] keyAndIV = EVP_BytesToKey(32, 16, secretKey.getBytes(StandardCharsets.UTF_8), saltBytes, 1);
            byte[] key = keyAndIV[0];
            byte[] iv = keyAndIV[1];

            // Use AES/CBC/PKCS5Padding (which is what CryptoJS uses by default)
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting API key", e);
            throw new ServiceException(CONFIG_NOT_EXISTS, "Failed to decrypt API key: " + e.getMessage());
        }
    }

    /**
     * Implementation of OpenSSL's EVP_BytesToKey key derivation function
     * This is used by CryptoJS to derive the key and IV from the password and salt
     */
    private static byte[][] EVP_BytesToKey(int keyLen, int ivLen, byte[] password, byte[] salt, int iterations) {
        byte[] key = new byte[keyLen];
        byte[] iv = new byte[ivLen];

        byte[] concatenatedHashBytes = new byte[0];

        java.security.MessageDigest md5;
        try {
            md5 = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }

        int hashLen = 16; // MD5 hash length

        int keyAndIvLen = keyLen + ivLen;
        int numHashes = (keyAndIvLen + hashLen - 1) / hashLen;

        byte[] result = new byte[numHashes * hashLen];
        int resultLen = 0;

        for (int i = 1; i <= numHashes; i++) {
            // For the first hash, the data is just the password and salt
            // For subsequent hashes, add the previous hash result
            md5.reset();
            if (i > 1) {
                md5.update(concatenatedHashBytes);
            }
            md5.update(password);
            if (salt != null) {
                md5.update(salt);
            }
            concatenatedHashBytes = md5.digest();

            // Perform additional iterations if requested
            for (int j = 1; j < iterations; j++) {
                md5.reset();
                md5.update(concatenatedHashBytes);
                concatenatedHashBytes = md5.digest();
            }

            // Copy the hash into the result buffer
            System.arraycopy(
                    concatenatedHashBytes, 0,
                    result, resultLen,
                    Math.min(concatenatedHashBytes.length, result.length - resultLen)
            );
            resultLen += concatenatedHashBytes.length;
        }

        // Split the result into key and IV
        System.arraycopy(result, 0, key, 0, keyLen);
        System.arraycopy(result, keyLen, iv, 0, ivLen);

        return new byte[][]{key, iv};
    }


    private String append(String base, Map<String, ?> query, boolean fragment) {
        return append(base, query, null, fragment);
    }

}
