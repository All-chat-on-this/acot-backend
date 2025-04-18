package com.chat.allchatonthis.common.util.http;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.TableMap;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.chat.allchatonthis.common.util.json.JsonUtils;
import com.chat.allchatonthis.entity.dataobject.UserConfigDO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 工具类
 */
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

        // Add API key to headers based on placement
        if ("header".equals(config.getApiKeyPlacement()) || config.getApiKeyPlacement() == null) {
            headers.put("Authorization", "Bearer " + config.getApiKey());
        } else if ("custom_header".equals(config.getApiKeyPlacement()) && config.getApiKeyHeader() != null) {
            headers.put(config.getApiKeyHeader(), config.getApiKey());
        }

        // Ensure Content-Type header exists
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        // Prepare request body by cloning the template
        Map<String, Object> requestBody = new HashMap<>(config.getRequestTemplate());

        // Add API key to body if needed
        if ("body".equals(config.getApiKeyPlacement()) && config.getApiKeyBodyPath() != null) {
            JsonUtils.setValueByPath(requestBody, config.getApiKeyBodyPath(), config.getApiKey());
        }

        // Add the message text to the request body at the specified path if provided
        if (messageText != null && StringUtils.hasText(config.getRequestTextPath())) {
            JsonUtils.setValueByPath(requestBody, config.getRequestTextPath(), messageText);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("headers", headers);
        result.put("requestBody", requestBody);

        return result;
    }

    private String append(String base, Map<String, ?> query, boolean fragment) {
        return append(base, query, null, fragment);
    }

}
