package com.fanxb.bookmark.common.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanxb.bookmark.common.exception.CustomException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 类功能简述：
 * 类功能详述：
 *
 * @author fanxb
 * @date 2019/4/4 15:53
 */
@Component
@Slf4j
public class HttpUtil {

    /**
     * 是否存在代理环境
     */
    @Getter
    private static Boolean proxyExist = false;

    @Value("${proxy.ip}")
    private String proxyIp;
    @Value("${proxy.port}")
    private String proxyPort;

    private static final int IP_LENGTH = 15;

    /**
     * 使用代理环境(如不存在代理配置，那么此客户端也是非代理)
     */
    private static OkHttpClient PROXY_CLIENT;
    /**
     * 无代理环境
     */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 超时时间1s
     */
    @Getter
    private static final OkHttpClient SHORT_CLIENT = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build();

    /**
     * 获取客户端
     *
     * @param proxy 是否代理
     * @return {@link OkHttpClient}
     * @author fanxb
     */
    public static OkHttpClient getClient(boolean proxy) {
        return proxy ? PROXY_CLIENT : CLIENT;
    }

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @PostConstruct
    public void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
        log.info("代理配置，ip:{},port:{}", proxyIp, proxyPort);
        if (StrUtil.isNotBlank(proxyIp) && StrUtil.isNotBlank(proxyPort)) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyIp, Integer.parseInt(proxyPort))));
            proxyExist = true;
            PROXY_CLIENT = builder.build();
        } else {
            PROXY_CLIENT = CLIENT;
        }
    }

    /**
     * 功能描述: get请求
     *
     * @param url     url
     * @param headers header
     * @param proxy   是否使用代理
     * @return T
     * @author fanxb
     * @date 2020/3/22 21:07
     */
    public static JSONObject getObj(String url, Map<String, String> headers, boolean proxy) {
        return get(url, headers, JSONObject.class, proxy);
    }

    /**
     * 功能描述: get请求
     *
     * @param url     url
     * @param headers header
     * @param proxy   是否使用代理
     * @return T
     * @author fanxb
     * @date 2020/3/22 21:07
     */
    public static JSONArray getArray(String url, Map<String, String> headers, boolean proxy) {
        return get(url, headers, JSONArray.class, proxy);
    }

    /**
     * 功能描述: get请求
     *
     * @param url       url
     * @param headers   header
     * @param typeClass type
     * @param proxy     是否使用代理
     * @return T
     * @author fanxb
     * @date 2020/3/22 21:07
     */
    private static <T> T get(String url, Map<String, String> headers, Class<T> typeClass, boolean proxy) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                builder = builder.addHeader(key, headers.get(key));
            }
        }
        return request(builder.build(), typeClass, proxy);
    }

    /**
     * POST请求,返回jsonObj
     *
     * @param url     url
     * @param jsonObj 请求体
     * @param headers 请求头
     * @param proxy   是否使用代理
     * @author fanxb
     * @date 2021/3/15
     **/
    public static JSONObject postObj(String url, String jsonObj, Map<String, String> headers, boolean proxy) {
        return post(url, jsonObj, headers, JSONObject.class, proxy);
    }

    /**
     * POST请求,返回jsonArray
     *
     * @param url     url
     * @param jsonObj 请求体
     * @param headers 请求头
     * @param proxy   是否使用代理
     * @author fanxb
     * @date 2021/3/15
     **/
    public static JSONArray postArray(String url, String jsonObj, Map<String, String> headers, boolean proxy) {
        return post(url, jsonObj, headers, JSONArray.class, proxy);
    }

    /**
     * POST请求
     *
     * @param url       url
     * @param jsonObj   请求体
     * @param headers   请求头
     * @param typeClass 响应类
     * @param proxy     是否使用代理
     * @return T
     * @author fanxb
     * @date 2021/3/15
     **/
    private static <T> T post(String url, String jsonObj, Map<String, String> headers, Class<T> typeClass, boolean proxy) {
        RequestBody body = RequestBody.create(JSON, jsonObj);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            Set<String> set = headers.keySet();
            for (String key : set) {
                builder = builder.addHeader(key, headers.get(key));
            }
        }
        return request(builder.build(), typeClass, proxy);
    }

    /**
     * 构造request，获取响应
     *
     * @param request   请求request
     * @param typeClass 相应类型
     * @param proxy     是否使用代理
     * @return T
     * @author fanxb
     * @date 2021/3/15
     **/
    public static <T> T request(Request request, Class<T> typeClass, boolean proxy) {
        try (Response res = (proxy ? PROXY_CLIENT : CLIENT).newCall(request).execute()) {
            return parseResponse(res, typeClass);
        } catch (Exception e) {
            throw new RuntimeException(request.url() + e.getLocalizedMessage(), e);
        }
    }

    /**
     * 解析响应
     *
     * @param res res
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseResponse(Response res, Class<T> typeClass) {
        try {
            assert res.body() != null;
            if (checkIsOk(res.code())) {
                String str = res.body().string();
                if (typeClass.getCanonicalName().equals(JSONObject.class.getCanonicalName())) {
                    return (T) JSONObject.parseObject(str);
                } else if (typeClass.getCanonicalName().equals(JSONArray.class.getCanonicalName())) {
                    return (T) JSONArray.parseArray(str);
                } else {
                    throw new CustomException("仅支持JSONObject,JSONArray");
                }
            } else {
                throw new CustomException("http请求出错:" + res.body().string());
            }
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public static boolean checkIsOk(int code) {
        return code >= 200 && code < 300;
    }

    /**
     * Description: 从请求头中获取用户访问ip
     *
     * @param request 请求头
     * @return java.lang.String
     * @author fanxb
     * @date 2019/6/12 15:36
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0) {
                ipAddress = request.getRemoteAddr();
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > IP_LENGTH) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        } catch (Exception e) {
            ipAddress = "";
        }
        return ipAddress;
    }


}


