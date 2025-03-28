package lab.dragon.invoice.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author mickey.wang
 */
public class FileChecksum {

    public static String sha256(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        // 支持的算法: "MD5", "SHA-1", "SHA-256" 等
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // 读取文件
        try (inputStream) {
            byte[] buffer = new byte[8192]; // 8KB 缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        // 计算校验和并转换为十六进制字符串
        byte[] hashBytes = digest.digest();
        BigInteger bigInt = new BigInteger(1, hashBytes); // 1 表示正数
        String checksum = bigInt.toString(16);

        // 补齐前导零（可选，确保长度一致）
        while (checksum.length() < 32) {
            checksum = "0" + checksum;
        }

        return checksum;
    }
}
