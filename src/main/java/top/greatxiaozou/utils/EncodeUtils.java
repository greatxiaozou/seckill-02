package top.greatxiaozou.utils;

import java.util.Base64;
import java.util.Base64.Encoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncodeUtils {
    public static String EncodeByMd5(String str) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Encoder base64Encoder = Base64.getEncoder();
        byte[] encode = base64Encoder.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        String res = new String(encode);
        //加密字符串
        return res;
    }
}
