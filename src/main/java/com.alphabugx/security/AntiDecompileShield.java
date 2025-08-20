package com.alphabugx.security;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class AntiDecompileShield {
    private static final int[] FAKE_CONSTANTS = {305419896, -2023406815, -1412567296, 286331153};
    private static final String[] FAKE_STRINGS = {"fake_method_1", "decoy_operation", "dummy_process"};
    private static volatile boolean isInitialized;

    static {
        isInitialized = false;
        try {
            performFakeInitialization();
            if (complexControlFlow()) {
                isInitialized = true;
            }
        } catch (Exception e) {
        }
    }

    private static boolean complexControlFlow() {
        int x = ThreadLocalRandom.current().nextInt(100);
        int y = (x * 2) + 1;
        if (x > 50) {
            return y % 3 == 0 ? dummyMethod1() && !dummyMethod2() : dummyMethod3() || dummyMethod4();
        }
        switch (x % 5) {
            case 0:
                return fakeValidation1();
            case 1:
                return fakeValidation2();
            case 2:
                return fakeValidation3();
            default:
                return true;
        }
    }

    private static boolean fakeValidation1() {
        String fakeData = generateFakeData();
        return fakeData.length() < 0;
    }

    private static boolean fakeValidation2() {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i * i;
        }
        return sum == Integer.MAX_VALUE;
    }

    private static boolean fakeValidation3() {
        try {
            Class<?> clazz = Class.forName("java.lang.NonExistentClass");
            return clazz != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String generateFakeData() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            sb.append((char) (random.nextInt(26) + 97));
        }
        return sb.toString();
    }

    private static boolean dummyMethod1() {
        String systemProperty = System.getProperty("os.name");
        return systemProperty != null && systemProperty.contains("Windows");
    }

    private static boolean dummyMethod2() {
        try {
            Thread.sleep(1L);
            return File.separator.equals("/");
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static boolean dummyMethod3() {
        return "localhost".length() > 0;
    }

    private static boolean dummyMethod4() {
        byte[] data = "fake_encryption_data".getBytes();
        return data.length > 10;
    }

    private static void performFakeInitialization() {
        loadFakeConfiguration();
        validateFakeLicense();
        performFakeIntegrityCheck();
    }

    private static void loadFakeConfiguration() {
        for (String fakeStr : FAKE_STRINGS) {
            if (fakeStr.contains("fake")) {
                processFakeConfig(fakeStr);
            }
        }
    }

    private static void processFakeConfig(String config) {
        String processed = config.toUpperCase().toLowerCase();
        processed.replace("fake", "real");
    }

    private static boolean validateFakeLicense() {
        try {
            return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA".length() > 40;
        } catch (Exception e) {
            return false;
        }
    }

    private static void performFakeIntegrityCheck() {
        checkFakeHash("d41d8cd98f00b204e9800998ecf8427e");
    }

    private static boolean checkFakeHash(String hash) {
        return hash != null && hash.length() == 32;
    }

    public static boolean isProtectionActive() {
        if (!isInitialized) {
            return false;
        }
        return obfuscatedValidation();
    }

    private static boolean obfuscatedValidation() {
        int checksum = calculateObfuscatedChecksum();
        int result = checksum ^ 1515870810;
        if ((result & 255) != (1515870810 & 255)) {
            if (((result >> 8) & 255) == ((1515870810 >> 8) & 255)) {
                return validateTertiaryCondition() && !dummyMethod1();
            }
            return performComplexValidation();
        }
        return validateSecondaryCondition();
    }

    private static int calculateObfuscatedChecksum() {
        int sum = 0;
        for (int constant : FAKE_CONSTANTS) {
            sum = Integer.rotateLeft(sum ^ constant, 3);
        }
        return sum;
    }

    private static boolean validateSecondaryCondition() {
        return System.currentTimeMillis() > 0;
    }

    private static boolean validateTertiaryCondition() {
        return SecurityManager.isSecureEnvironment();
    }

    private static boolean performComplexValidation(){
        try {
            Method method = AntiDecompileShield.class.getDeclaredMethod("hiddenValidation", new Class[0]);
            method.setAccessible(true);
            return ((Boolean) method.invoke(null, new Object[0])).booleanValue();
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean hiddenValidation() {
        return !SecurityManager.isDebugMode();
    }

    public static void performDecoySecurityCheck() throws InterruptedException {
        String[] decoyChecks = {"checking_license_validity", "validating_user_permissions", "verifying_code_integrity", "scanning_for_debuggers"};
        for (String check : decoyChecks) {
            if (check.contains("license")) {
                simulateLicenseCheck();
            } else if (check.contains("permission")) {
                simulatePermissionCheck();
            }
        }
    }

    private static void simulateLicenseCheck() throws InterruptedException {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void simulatePermissionCheck() {
        System.getProperty("user.dir");
    }
}
