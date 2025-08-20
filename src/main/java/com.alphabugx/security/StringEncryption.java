package com.alphabugx.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class StringEncryption {
    private static final int[] KEYS = {7, 13, 23, 31, 37};
    private static final String SALT = "JawdSecure2024";


    public static class EncryptedStrings {
        public static final String CFR_JAR = StringEncryption.encrypt("cfr-0.152.jar");
        public static final String JAVAC_EXE = StringEncryption.encrypt("javac.exe");
        public static final String JAR_EXE = StringEncryption.encrypt("jar.exe");
        public static final String COMPILATION_FAILED = StringEncryption.encrypt("Java编译失败");
        public static final String DECOMPILATION_FAILED = StringEncryption.encrypt("反编译失败");
        public static final String FILE_NOT_FOUND = StringEncryption.encrypt("文件未找到");
        public static final String CONFIG_FILE = StringEncryption.encrypt("jawd_config.properties");
        public static final String TEMP_DIR = StringEncryption.encrypt("jawd_temp");
        public static final String BACKUP_DIR = StringEncryption.encrypt("jawd_backup");
        public static final String JAVA_HOME = StringEncryption.encrypt("JAVA_HOME");
        public static final String USER_DIR = StringEncryption.encrypt("user.dir");
        public static final String TEMP_PATH = StringEncryption.encrypt("java.io.tmpdir");
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            String shifted = shiftEncrypt(plaintext, KEYS[0]);
            String xored = xorEncrypt(shifted, SALT);
            return Base64.getEncoder().encodeToString(xored.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return plaintext;
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(encrypted), StandardCharsets.UTF_8);
            String xored = xorDecrypt(decoded, SALT);
            return shiftDecrypt(xored, KEYS[0]);
        } catch (Exception e) {
            return encrypted;
        }
    }

    private static String shiftEncrypt(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append((char) (c + shift));
        }
        return result.toString();
    }

    private static String shiftDecrypt(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append((char) (c - shift));
        }
        return result.toString();
    }

    private static String xorEncrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char textChar = text.charAt(i);
            char keyChar = key.charAt(i % key.length());
            result.append((char) (textChar ^ keyChar));
        }
        return result.toString();
    }

    private static String xorDecrypt(String text, String key) {
        return xorEncrypt(text, key);
    }

    public static String getCfrJar() {
        return decrypt(EncryptedStrings.CFR_JAR);
    }

    public static String getJavacExe() {
        return decrypt(EncryptedStrings.JAVAC_EXE);
    }

    public static String getJarExe() {
        return decrypt(EncryptedStrings.JAR_EXE);
    }

    public static String getConfigFile() {
        return decrypt(EncryptedStrings.CONFIG_FILE);
    }

    public static String getTempDir() {
        return decrypt(EncryptedStrings.TEMP_DIR);
    }

    public static void main(String[] args) {
        System.out.println("CFR_JAR: " + encrypt("cfr-0.152.jar"));
        System.out.println("JAVAC_EXE: " + encrypt("javac.exe"));
        System.out.println("JAR_EXE: " + encrypt("jar.exe"));
        System.out.println("CONFIG_FILE: " + encrypt("jawd_config.properties"));
    }
}
