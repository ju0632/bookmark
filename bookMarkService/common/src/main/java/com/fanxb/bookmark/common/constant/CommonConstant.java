package com.fanxb.bookmark.common.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 类功能简述：系统及常量类
 * 类功能详述：
 *
 * @author fanxb
 */
@Component
public class CommonConstant {


    /**
     * jwt key
     */
    public static final String JWT_KEY = "jwt-token";


    /**
     * 验证码过期时间
     */
    public static int AUTH_CODE_EXPIRE = 15 * 60 * 1000;

    /**
     * Description: 生成email存在redis中的key
     *
     * @param email 邮箱地址
     * @return java.lang.String
     * @author fanxb
     * @date 2019/7/6 10:56
     */
    public static String authCodeKey(String email) {
        return email + "_authCode";
    }

    /**
     * 是否为开发环境
     */
    public static boolean isDev = false;

    @Value("${spring.profiles.active}")
    public void setIsDev(String active) {
        CommonConstant.isDev = active.contains("dev");
    }

    public static String jwtSecret = "";

    @Value("${jwtSecret}")
    public void setJwtSecret(String jwtSecret) {
        CommonConstant.jwtSecret = jwtSecret;
    }

    /**
     * 文件存储基路径
     */
    public static String fileSavePath = "./";

    @Value("${fileSavePath}")
    public void setFileSavePath(String path) {
        fileSavePath = path;
    }


    /**
     * 服务部署地址
     */
    public static String serviceAddress = "http://localhost";

    @Value("${serviceAddress}")
    public void setServiceAddress(String address) {
        serviceAddress = address;
    }
}
