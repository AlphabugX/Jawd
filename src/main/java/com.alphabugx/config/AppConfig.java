package com.alphabugx.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


public class AppConfig {
    private static final String CONFIG_FILE = "jawd_config.properties";
    private Properties properties = new Properties();
    private static AppConfig instance;


    private AppConfig() {
        this.loadConfig();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }

        return instance;
    }

    private void loadConfig() {
        File configFile = new File("jawd_config.properties");
        if (configFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(configFile);
                Throwable var3 = null;

                try {
                    this.properties.load(fis);
                } catch (Throwable var13) {
                    var3 = var13;
                    throw var13;
                } finally {
                    if (fis != null) {
                        if (var3 != null) {
                            try {
                                fis.close();
                            } catch (Throwable var12) {
                                var3.addSuppressed(var12);
                            }
                        } else {
                            fis.close();
                        }
                    }

                }
            } catch (IOException var15) {
                IOException e = var15;
                System.err.println("加载配置文件失败: " + e.getMessage());
                this.setDefaultConfig();
            }
        } else {
            this.setDefaultConfig();
        }

    }

    private void setDefaultConfig() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            if (javaHome.endsWith("jre")) {
                File jdkPath = (new File(javaHome)).getParentFile();
                if (jdkPath.exists()) {
                    javaHome = jdkPath.getAbsolutePath();
                }
            }

            this.properties.setProperty("jdk.path", javaHome);
        } else {
            this.properties.setProperty("jdk.path", "");
        }

        this.properties.setProperty("cfr.path", "cfr-0.152.jar");
        this.saveConfig();
    }


    public void saveConfig() {
        try {
            FileOutputStream fos = new FileOutputStream("jawd_config.properties");
            Throwable var2 = null;

            try {
                this.properties.store(fos, "JAWD Configuration File");
            } catch (Throwable var12) {
                var2 = var12;
                throw var12;
            } finally {
                if (fos != null) {
                    if (var2 != null) {
                        try {
                            fos.close();
                        } catch (Throwable var11) {
                            var2.addSuppressed(var11);
                        }
                    } else {
                        fos.close();
                    }
                }

            }
        } catch (IOException var14) {
            IOException e = var14;
            System.err.println("保存配置文件失败: " + e.getMessage());
        }

    }

    public String getJdkPath() {
        return this.properties.getProperty("jdk.path", "");
    }

    public void setJdkPath(String jdkPath) {
        this.properties.setProperty("jdk.path", jdkPath);
    }

    public String getCfrPath() {
        return this.properties.getProperty("cfr.path", "cfr-0.152.jar");
    }

    public void setCfrPath(String cfrPath) {
        this.properties.setProperty("cfr.path", cfrPath);
    }

    public String getJavaExecutable() {
        String jdkPath = getJdkPath();
        if (jdkPath.isEmpty()) {
            return "java";
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String javaExe = osName.contains("win") ? "java.exe" : "java";
        return new File(jdkPath, "bin" + File.separator + javaExe).getAbsolutePath();
    }

    public String getJavacExecutable() {
        String jdkPath = getJdkPath();
        if (jdkPath.isEmpty()) {
            return "javac";
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String javacExe = osName.contains("win") ? "javac.exe" : "javac";
        return new File(jdkPath, "bin" + File.separator + javacExe).getAbsolutePath();
    }

    public String getJarExecutable() {
        String jdkPath = getJdkPath();
        if (jdkPath.isEmpty()) {
            return "jar";
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String jarExe = osName.contains("win") ? "jar.exe" : "jar";
        return new File(jdkPath, "bin" + File.separator + jarExe).getAbsolutePath();
    }
}
