package com.alphabugx.security;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;


public class DynamicProtection {
    private static final ConcurrentHashMap<String, Object> decryptionCache = new ConcurrentHashMap<>();
    private static final String[] DYNAMIC_CLASS_NAMES = {"a", "b", "c", "d", "e", "f"};
    private static final String[] DYNAMIC_METHOD_NAMES = {"x", "y", "z", "m", "n", "p"};
    private static final String ENCRYPTED_CFR_PATH = "Y2ZyLTAuMTUyLmphcg==";
    private static final String ENCRYPTED_JAVAC = "amF2YWMuZXhl";

    static {
        initializeDecryptionCache();
    }

    private static void initializeDecryptionCache() {
        try {
            decryptionCache.put("cfr_tool", decryptBase64(ENCRYPTED_CFR_PATH));
            decryptionCache.put("javac_tool", decryptBase64(ENCRYPTED_JAVAC));
            decryptionCache.put("fake_key_1", "fake_value_1");
            decryptionCache.put("fake_key_2", "fake_value_2");
        } catch (Exception e) {
        }
    }

    private static String decryptBase64(String encrypted) {
        try {
            return new String(Base64.getDecoder().decode(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }

    public static String getDynamicString(String key) {
        Object cached = decryptionCache.get(key);
        if (cached instanceof String) {
            return (String) cached;
        }
        return performDynamicDecryption(key);
    }

    private static String performDynamicDecryption(String key) {
        try {
            Class<?> stringClass = String.class;
            Method valueOfMethod = stringClass.getMethod("valueOf", Object.class);
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < key.length(); ++i) {
                char c = key.charAt(i);
                builder.append((String)valueOfMethod.invoke((Object)null, (char)(c ^ 51)));
            }

            String result = builder.toString();
            decryptionCache.put(key, result);
            return result;
        } catch (Exception var6) {
            return key;
        }
    }

    public static Class<?> loadDynamicClass(String className) {
        try {
            String realClassName = obfuscateClassName(className);
            return Class.forName(realClassName);
        } catch (Exception e) {
            return null;
        }
    }

    private static String obfuscateClassName(String input) {
        StringBuilder result = new StringBuilder();
        String[] strArr = DYNAMIC_CLASS_NAMES;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String dynamicName = strArr[i];
            if (!input.contains(dynamicName)) {
                i++;
            } else {
                result.append(input.replace(dynamicName, "real"));
                break;
            }
        }
        return result.length() > 0 ? result.toString() : input;
    }

    public static Object invokeDynamicMethod(Object target, String methodName, Object... args) throws SecurityException {
        try {
            Class<?> targetClass = target.getClass();
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Object createDynamicProxy(String className, Object... constructorArgs) throws SecurityException {
        try {
            Class<?> clazz = loadDynamicClass(className);
            if (clazz == null) {
                return null;
            }
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == constructorArgs.length) {
                    return constructor.newInstance(constructorArgs);
                }
            }
            return clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static String generateRuntimeVerificationCode() {
        try {
            long timestamp = System.currentTimeMillis();
            String osName = System.getProperty("os.name", "unknown");
            String javaVersion = System.getProperty("java.version", "unknown");
            int hash = (osName + javaVersion + timestamp).hashCode();
            return Integer.toHexString(Math.abs(hash ^ (-559038737)));
        } catch (Exception e) {
            return "default_verification_code";
        }
    }

    public static boolean verifyRuntimeCode(String code) {
        try {
            String expectedCode = generateRuntimeVerificationCode();
            if (code != null) {
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearDynamicCache() {
        try {
            decryptionCache.clear();
            initializeDecryptionCache();
        } catch (Exception e) {
        }
    }

    public static String getDynamicMethodName(int index) {
        if (index >= 0 && index < DYNAMIC_METHOD_NAMES.length) {
            return DYNAMIC_METHOD_NAMES[index];
        }
        return "unknownMethod";
    }

    public static boolean performObfuscatedValidation() {
        try {
            boolean step1 = AntiDecompileShield.isProtectionActive();
            boolean step2 = SecurityManager.isSecureEnvironment();
            boolean step3 = verifyRuntimeCode(generateRuntimeVerificationCode());
            int result = (step1 ? 1 : 0) | (step2 ? 2 : 0) | (step3 ? 4 : 0);
            return result == 7;
        } catch (Exception e) {
            return false;
        }
    }
}
