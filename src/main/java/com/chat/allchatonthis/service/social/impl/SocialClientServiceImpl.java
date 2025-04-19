package com.chat.allchatonthis.service.social.impl;

import cn.hutool.core.lang.Assert;
import com.chat.allchatonthis.common.util.http.HttpUtils;
import com.chat.allchatonthis.enums.SocialTypeEnum;
import com.chat.allchatonthis.service.social.SocialClientService;
import com.google.common.annotations.VisibleForTesting;
import com.xingyuv.jushauth.model.AuthCallback;
import com.xingyuv.jushauth.model.AuthResponse;
import com.xingyuv.jushauth.model.AuthUser;
import com.xingyuv.jushauth.request.AuthRequest;
import com.xingyuv.jushauth.utils.AuthStateUtils;
import com.xingyuv.justauth.AuthRequestFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.chat.allchatonthis.common.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;
import static com.chat.allchatonthis.common.exception.util.ServiceExceptionUtil.exception;
import static com.chat.allchatonthis.common.util.json.JsonUtils.toJsonString;


/**
 * 社交应用 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class SocialClientServiceImpl implements SocialClientService {


    @Resource
    private AuthRequestFactory authRequestFactory;

    @Override
    public String getAuthorizeUrl(Integer socialType, Integer userType, String redirectUri) {
        // 获得对应的 AuthRequest 实现
        AuthRequest authRequest = buildAuthRequest(socialType, userType);
        // 生成跳转地址
        String authorizeUri = authRequest.authorize(AuthStateUtils.createState());
        return HttpUtils.replaceUrlQuery(authorizeUri, "redirect_uri", redirectUri);
    }

    @Override
    public AuthUser getAuthUser(Integer socialType, Integer userType, String code, String state) {
        // 构建请求
        AuthRequest authRequest = buildAuthRequest(socialType, userType);
        AuthCallback authCallback = AuthCallback.builder().code(code).state(state).build();
        // 执行请求
        AuthResponse<?> authResponse = authRequest.login(authCallback);
        log.info("[getAuthUser][请求社交平台 type({}) request({}) response({})]", socialType,
                toJsonString(authCallback), toJsonString(authResponse));
        if (!authResponse.ok()) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, authResponse.getMsg());
        }
        return (AuthUser) authResponse.getData();
    }

    /**
     * 构建 AuthRequest 对象，支持多租户配置
     *
     * @param socialType 社交类型
     * @param userType   用户类型
     * @return AuthRequest 对象
     */
    @VisibleForTesting
    AuthRequest buildAuthRequest(Integer socialType, Integer userType) {
        // 1. 先查找默认的配置项，从 application-*.yaml 中读取
        AuthRequest request = authRequestFactory.get(SocialTypeEnum.valueOfType(socialType).getSource());
        Assert.notNull(request, String.format("社交平台(%d) 不存在", socialType));

        return request;
    }


}
