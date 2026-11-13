package com.neucity.common.core.utils.sign;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/10/29 9:59
 */

public class AKSKGenerator {

    // 生成 AK（18位，字母+数字）
    public static String generateAK() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder ak = new StringBuilder(18);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 18; i++) {
            ak.append(chars.charAt(random.nextInt(chars.length())));
        }
        return ak.toString();
    }

    // 生成 SK（32位，基于 AES 密钥生成，Base64 编码）
    public static String generateSK() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom()); // 256位密钥，高安全性
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded()).substring(0, 32);
    }

    // 测试生成
    public static void main(String[] args) throws NoSuchAlgorithmException {
        String ak = generateAK();
        String sk = generateSK();
        String encryptedSk = SignUtil.encryptData(ak,  sk);
        String decryptedSk = SignUtil.decryptData("kcg5aMzfF92wkYgVuc",  "Df0z7uIVJt+Dti823OCxeO0GoZcakJ998ht+OLdEVsm8B3M6kaofrcHKOkF4Kuj/Qawse");
//        System.out.println("生成的 AK：" + ak);
//        System.out.println("生成的 SK：" + sk);
//        System.out.println("加密的SK：" +  encryptedSk);
        System.out.println("解密的SK：" + decryptedSk);
    }

}
