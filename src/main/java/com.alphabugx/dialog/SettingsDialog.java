package com.alphabugx.dialog;

import com.alphabugx.config.AppConfig;
import java.io.File;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class SettingsDialog {
    public static void showJdkConfigDialog(Stage owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("JDK路径配置");
        dialog.setHeaderText("请选择JDK安装路径");
        dialog.initOwner(owner);
        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType[]{okButtonType, ButtonType.CANCEL});
        GridPane grid = new GridPane();
        grid.setHgap(10.0d);
        grid.setVgap(10.0d);
        grid.setPadding(new Insets(20.0d, 150.0d, 10.0d, 10.0d));
        TextField jdkPathField = new TextField();
        jdkPathField.setPromptText("JDK安装路径");
        jdkPathField.setPrefWidth(300.0d);
        AppConfig config = AppConfig.getInstance();
        jdkPathField.setText(config.getJdkPath());
        Button browseButton = new Button("浏览...");
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择JDK安装目录");
            String currentPath = jdkPathField.getText();
            if (!currentPath.isEmpty() && new File(currentPath).exists()) {
                directoryChooser.setInitialDirectory(new File(currentPath));
            }
            File selectedDirectory = directoryChooser.showDialog(dialog.getOwner());
            if (selectedDirectory != null) {
                jdkPathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        Button autoDetectButton = new Button("自动检测");
        autoDetectButton.setOnAction(e2 -> {
            String detectedPath = detectJdkPath();
            if (detectedPath != null) {
                jdkPathField.setText(detectedPath);
                showInfo("自动检测成功", "检测到JDK路径: " + detectedPath);
            } else {
                showWarning("自动检测失败", "无法自动检测JDK路径，请手动选择");
            }
        });
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        updateJdkStatus(jdkPathField.getText(), statusLabel);
        jdkPathField.textProperty().addListener((obs, oldText, newText) -> {
            updateJdkStatus(newText, statusLabel);
        });
        grid.add(new Label("JDK路径:"), 0, 0);
        grid.add(jdkPathField, 1, 0);
        grid.add(browseButton, 2, 0);
        grid.add(autoDetectButton, 3, 0);
        grid.add(statusLabel, 1, 1, 3, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return jdkPathField.getText();
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(jdkPath -> {
            config.setJdkPath(jdkPath);
            config.saveConfig();
            showInfo("配置保存成功", "JDK路径已更新: " + jdkPath);
        });
    }

    public static void showCfrConfigDialog(Stage owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("CFR工具配置");
        dialog.setHeaderText("请选择CFR工具路径");
        dialog.initOwner(owner);
        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType[]{okButtonType, ButtonType.CANCEL});
        GridPane grid = new GridPane();
        grid.setHgap(10.0d);
        grid.setVgap(10.0d);
        grid.setPadding(new Insets(20.0d, 150.0d, 10.0d, 10.0d));
        TextField cfrPathField = new TextField();
        cfrPathField.setPromptText("CFR工具路径");
        cfrPathField.setPrefWidth(300.0d);
        AppConfig config = AppConfig.getInstance();
        cfrPathField.setText(config.getCfrPath());
        Button browseButton = new Button("浏览...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择CFR工具");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar文件", new String[]{"*.jar"}));
            String currentPath = cfrPathField.getText();
            if (!currentPath.isEmpty()) {
                File currentFile = new File(currentPath);
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    fileChooser.setInitialDirectory(currentFile.getParentFile());
                }
            }
            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                cfrPathField.setText(selectedFile.getAbsolutePath());
            }
        });
        Button defaultButton = new Button("使用默认");
        defaultButton.setOnAction(e2 -> {
            cfrPathField.setText("cfr-0.152.jar");
        });
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        updateCfrStatus(cfrPathField.getText(), statusLabel);
        cfrPathField.textProperty().addListener((obs, oldText, newText) -> {
            updateCfrStatus(newText, statusLabel);
        });
        grid.add(new Label("CFR路径:"), 0, 0);
        grid.add(cfrPathField, 1, 0);
        grid.add(browseButton, 2, 0);
        grid.add(defaultButton, 3, 0);
        grid.add(statusLabel, 1, 1, 3, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return cfrPathField.getText();
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cfrPath -> {
            config.setCfrPath(cfrPath);
            config.saveConfig();
            showInfo("配置保存成功", "CFR工具路径已更新: " + cfrPath);
        });
    }


    public static void updateJdkStatus(String jdkPath, Label statusLabel) {
        if (jdkPath == null || jdkPath.trim().isEmpty()) {
            statusLabel.setText("状态: 未配置JDK路径");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
            return;
        }
        File jdkDir = new File(jdkPath);
        if (!jdkDir.exists() || !jdkDir.isDirectory()) {
            statusLabel.setText("状态: 路径不存在");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
            return;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String javacExe = osName.contains("win") ? "javac.exe" : "javac";
        File javacFile = new File(jdkDir, "bin" + File.separator + javacExe);
        if (javacFile.exists()) {
            statusLabel.setText("状态: JDK路径有效");
            statusLabel.setStyle("-fx-text-fill: #00aa00; -fx-font-size: 11px;");
        } else {
            statusLabel.setText("状态: 路径无效，未找到javac");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
        }
    }


    public static void updateCfrStatus(String cfrPath, Label statusLabel) {
        if (cfrPath == null || cfrPath.trim().isEmpty()) {
            statusLabel.setText("状态: 未配置CFR工具路径");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
            return;
        }
        File cfrFile = new File(cfrPath);
        if (!cfrFile.exists() || !cfrFile.isFile()) {
            statusLabel.setText("状态: CFR工具不存在");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
        } else if (cfrFile.getName().toLowerCase().endsWith(".jar")) {
            statusLabel.setText("状态: CFR工具有效");
            statusLabel.setStyle("-fx-text-fill: #00aa00; -fx-font-size: 11px;");
        } else {
            statusLabel.setText("状态: 文件类型错误，需要jar文件");
            statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 11px;");
        }
    }

    private static String detectJdkPath() {
        File[] jdkDirs;
        File jdkPath;
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && isValidJdkPath(javaHome)) {
            return javaHome;
        }
        String javaHome2 = System.getProperty("java.home");
        if (javaHome2 != null) {
            if (javaHome2.endsWith("jre") && (jdkPath = new File(javaHome2).getParentFile()) != null && isValidJdkPath(jdkPath.getAbsolutePath())) {
                return jdkPath.getAbsolutePath();
            }
            if (isValidJdkPath(javaHome2)) {
                return javaHome2;
            }
        }
        String[] commonPaths = {"C:\\Program Files\\Java", "C:\\Program Files (x86)\\Java", "/usr/lib/jvm", "/usr/java", "/opt/java", "/Library/Java/JavaVirtualMachines"};
        for (String basePath : commonPaths) {
            File baseDir = new File(basePath);
            if (baseDir.exists() && baseDir.isDirectory() && (jdkDirs = baseDir.listFiles((dir, name) -> {
                return name.toLowerCase().contains("jdk");
            })) != null) {
                for (File jdkDir : jdkDirs) {
                    if (isValidJdkPath(jdkDir.getAbsolutePath())) {
                        return jdkDir.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isValidJdkPath(String jdkPath) {
        if (jdkPath == null || jdkPath.trim().isEmpty()) {
            return false;
        }
        File jdkDir = new File(jdkPath);
        if (!jdkDir.exists() || !jdkDir.isDirectory()) {
            return false;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        String javacExe = osName.contains("win") ? "javac.exe" : "javac";
        File javacFile = new File(jdkDir, "bin" + File.separator + javacExe);
        return javacFile.exists();
    }

    private static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText((String) null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText((String) null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
