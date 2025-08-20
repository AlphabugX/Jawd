package com.alphabugx;

import com.alphabugx.security.AntiDecompileShield;
import com.alphabugx.security.SecurityManager;
import com.alphabugx.util.EncodingHelper;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class Jawd extends Application {
    static {
        try {
            boolean isDevelopment = isDebugEnvironment();
            if (!isDevelopment) {
                AntiDecompileShield.performDecoySecurityCheck();
                SecurityManager.performSecurityCheck();
                if (!AntiDecompileShield.isProtectionActive()) {
                    throw new SecurityException("Protection validation failed");
                }
            } else {
                System.out.println("开发环境检测到，跳过安全检查");
                EncodingHelper.printEncodingInfo();
            }
        } catch (Exception e) {
            System.err.println("Configuration initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isDebugEnvironment() {
        String classpath = System.getProperty("java.class.path", "");
        if (classpath.contains("idea") || classpath.contains("eclipse") || classpath.contains("netbeans")) {
            return true;
        }
        String userDir = System.getProperty("user.dir", "");
        if (userDir.contains("target") || userDir.contains("IdeaProjects")) {
            return true;
        }
        String vmOptions = System.getProperty("java.vm.name", "");
        if (vmOptions.contains("HotSpot") && System.getProperty("sun.java.command", "").contains("idea")) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        if (!SecurityManager.isSecureEnvironment()) {
            System.err.println("Unsupported runtime environment");
            System.exit(1);
        } else {
            launch(args);
        }
    }

    public void start(Stage primaryStage) throws IOException {
        if (!SecurityManager.isSecureEnvironment()) {
            System.err.println("Runtime security check failed");
            Platform.exit();
            return;
        }
        FXMLLoader loader = new FXMLLoader(Jawd.class.getResource("/com/alphabugx/Editor.fxml"));
        BorderPane root = (BorderPane) loader.load();
        String appName = SecurityManager.getAppName();
        SecurityManager.getVersion();
        String title = appName + " v1.0 - by Alphabug";
        Scene scene = new Scene(root, 1200.0d, 800.0d);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800.0d);
        primaryStage.setMinHeight(600.0d);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("确认退出");
            alert.setHeaderText("强制退出应用程序");
            alert.setContentText("检测到后台可能存在运行的进程。\n点击「强制退出」将终止所有相关的Java进程。\n点击「取消」返回应用程序。\n\n是否确认强制退出？");
            ButtonType forceExitButton = new ButtonType("强制退出", ButtonBar.ButtonData.YES);
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(new ButtonType[]{forceExitButton, cancelButton});
            alert.setAlertType(Alert.AlertType.WARNING);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == forceExitButton) {
                performForceExit();
            }
        });
        primaryStage.show();
    }

    private void performForceExit() {
        try {
            SecurityManager.performCleanup();
            System.out.println("正在强制退出应用程序...");
            killJavaProcesses();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("强制退出时发生异常: " + e.getMessage());
            System.exit(1);
        }
    }

    private void killJavaProcesses() throws InterruptedException, IOException {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                System.out.println("终止Windows Java进程...");
                ProcessBuilder pb1 = new ProcessBuilder("taskkill", "/F", "/T", "/PID", getCurrentProcessId());
                pb1.start();
                Thread.sleep(1000L);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                System.out.println("终止Unix/Linux Java进程...");
                String currentPid = getCurrentProcessId();
                ProcessBuilder pb = new ProcessBuilder("kill", "-TERM", currentPid);
                pb.start();
                Thread.sleep(500L);
                ProcessBuilder pb2 = new ProcessBuilder("kill", "-KILL", currentPid);
                pb2.start();
            }
        } catch (Exception e) {
            System.err.println("终止Java进程时出错: " + e.getMessage());
        }
    }

    private String getCurrentProcessId() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            if (jvmName.contains("@")) {
                return jvmName.split("@")[0];
            }
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            return processName.substring(0, processName.indexOf(64));
        } catch (Exception e) {
            System.err.println("无法获取进程ID: " + e.getMessage());
            return "0";
        }
    }
}
