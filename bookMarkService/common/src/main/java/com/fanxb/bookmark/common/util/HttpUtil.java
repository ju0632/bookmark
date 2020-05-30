package com.fanxb.bookmark.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fanxb.bookmark.common.exception.CustomException;
import okhttp3.*;

import javax.servlet.http.HttpServletRequest;
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
public class HttpUtil {
    private static final int IP_LENGTH = 15;

    public static final OkHttpClient CLIENT = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS).build();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * 功能描述: get
     *
     * @param url url
     * @return com.alibaba.fastjson.JSONObject
     * @author fanxb
     * @date 2020/3/22 21:11
     */
    public static JSONObject get(String url) {
        return get(url, null, JSONObject.class);
    }

    /**
     * 功能描述: get请求
     *
     * @param url     url
     * @param headers header
     * @author fanxb
     * @date 2020/3/22 21:07
     */
    public static JSONObject get(String url, Map<String, String> headers) {
        return get(url, headers, JSONObject.class);
    }

    /**
     * 功能描述: get请求
     *
     * @param url       url
     * @param headers   header
     * @param typeClass type
     * @return T
     * @author fanxb
     * @date 2020/3/22 21:07
     */
    public static <T> T get(String url, Map<String, String> headers, Class<T> typeClass) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                builder = builder.addHeader(key, headers.get(key));
            }
        }
        return request(builder.build(), typeClass);
    }

    /**
     * 功能描述:不带header，返回jsonObject的post方法
     *
     * @param url     url
     * @param jsonObj body
     * @return com.alibaba.fastjson.JSONObject
     * @author fanxb
     * @date 2020/3/22 21:05
     */
    public static JSONObject post(String url, String jsonObj) {
        return post(url, jsonObj, null, JSONObject.class);
    }

    /**
     * 功能描述:返回jsonObject的post方法
     *
     * @param url     url
     * @param jsonObj body
     * @param headers headers
     * @return com.alibaba.fastjson.JSONObject
     * @author fanxb
     * @date 2020/3/22 21:05
     */
    public static JSONObject post(String url, String jsonObj, Map<String, String> headers) {
        return post(url, jsonObj, headers, JSONObject.class);
    }

    /**
     * 发送post请求，带header
     *
     * @param url     url
     * @param jsonObj 请求体
     * @param headers 请求头
     */
    public static <T> T post(String url, String jsonObj, Map<String, String> headers, Class<T> typeClass) {
        RequestBody body = RequestBody.create(JSON, jsonObj);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            Set<String> set = headers.keySet();
            for (String key : set) {
                builder = builder.addHeader(key, headers.get(key));
            }
        }
        return request(builder.build(), typeClass);
    }

    /**
     * 构造request，获取响应
     *
     * @param request request
     * @return
     */
    public static <T> T request(Request request, Class<T> typeClass) {
        try (Response res = CLIENT.newCall(request).execute()) {
            return parseResponse(res, typeClass);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * 解析响应
     *
     * @param res
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseResponse(Response res, Class<T> typeClass) {
        try {
            if (checkIsOk(res.code())) {
                String str = res.body().string();
                if (typeClass.getCanonicalName().equals(JSONObject.class.getCanonicalName())) {
                    return (T) JSONObject.parseObject(str);
                } else {
                    return (T) JSONArray.parseArray(str);
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
        String ipAddress = null;
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


