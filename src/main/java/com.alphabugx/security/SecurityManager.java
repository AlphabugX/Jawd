package com.alphabugx.security;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;


public class SecurityManager {
    private static final String ENCRYPTED_APP_NAME = "Mdze";
    private static final String ENCRYPTED_VERSION = "4#3#4";

    public static void performSecurityCheck() {
        try {
            if (isDebugMode()) {
                exitGracefully("Development environment detected");
            } else if (hasDebugArguments()) {
                exitGracefully("Debug arguments detected");
            } else if (!checkIntegrity()) {
                exitGracefully("File integrity check failed");
            }
        } catch (Exception e) {
        }
    }

    public static boolean isDebugMode() {
        try {
            List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : arguments) {
                if (arg.contains("-agentlib:jdwp") || arg.contains("-Xdebug") || arg.contains("-Xrunjdwp")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasDebugArguments() {
        try {
            List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : arguments) {
                if (arg.toLowerCase().contains("javaagent") || arg.toLowerCase().contains("jdwp") || arg.toLowerCase().contains("debug")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkIntegrity() {
        try {
            URL location = SecurityManager.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return true;
            }
            File jarFile = new File(location.toURI());
            calculateMD5(jarFile);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static String calculateMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        Throwable th = null;
        try {
            try {
                byte[] buffer = new byte[8192];
                while (true) {
                    int bytesRead = fis.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    md.update(buffer, 0, bytesRead);
                }
                if (fis != null) {
                    if (0 != 0) {
                        try {
                            fis.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    } else {
                        fis.close();
                    }
                }
                byte[] hashBytes = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", Byte.valueOf(b)));
                }
                return sb.toString();
            } finally {
            }
        } catch (Throwable th3) {
            if (fis != null) {
                if (th != null) {
                    try {
                        fis.close();
                    } catch (Throwable th4) {
                        th.addSuppressed(th4);
                    }
                } else {
                    fis.close();
                }
            }
            throw th3;
        }
    }

    public static String decrypt(String encrypted, int offset) {
        StringBuilder result = new StringBuilder();
        for (char c : encrypted.toCharArray()) {
            result.append((char) (c - offset));
        }
        return result.toString();
    }

    public static String getAppName() {
        return decrypt(ENCRYPTED_APP_NAME, 3);
    }

    public static String getVersion() {
        return decrypt(ENCRYPTED_VERSION, 3);
    }

    private static void exitGracefully(String reason) throws InterruptedException {
        System.err.println("Application initialization failed: Configuration error");
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.exit(1);
    }

    public static boolean isSecureEnvironment() {
        return true;
        // return (isDebugMode() || hasDebugArguments() || isModified()) ? false : true;
    }

    private static boolean isModified() {
        try {
            URL location = SecurityManager.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return false;
            }
            File jarFile = new File(location.toURI());
            if (!jarFile.exists()) {
                return false;
            }
            if (!jarFile.getName().endsWith(".jar")) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void performRuntimeCheck() throws InterruptedException {
        if (!isSecureEnvironment()) {
            exitGracefully("Runtime security validation failed");
        }
    }

    public static void performCleanup() {
        try {
            clearSensitiveData();
            System.out.println("Application shutdown completed");
        } catch (Exception e) {
        }
    }

    private static void clearSensitiveData() {
        System.gc();
    }

    public static String getEncryptedErrorMessage(String key) {
        switch (key) {
            case "config_error":
                return decrypt("Frqiljxudwlrq#iloh#qrw#irxqg", 3);
            case "runtime_error":
                return decrypt("Uxqwlph#hqylurqphqw#huuru", 3);
            case "permission_error":
                return decrypt("Lqvxiilflhqw#shuplvvlrqv", 3);
            default:
                return decrypt("Xqnqrzq#huuru", 3);
        }
    }

    public static String generateRuntimeToken() {
        try {
            long timestamp = System.currentTimeMillis();
            String baseString = ENCRYPTED_APP_NAME + timestamp;
            int hash = baseString.hashCode();
            return Integer.toHexString(Math.abs(hash));
        } catch (Exception e) {
            return "default_token";
        }
    }

    public static boolean validateRuntimeToken(String token) {
        try {
            String expectedToken = generateRuntimeToken();
            if (token != null) {
                if (token.equals(expectedToken)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
