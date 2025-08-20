package com.alphabugx;

import com.alphabugx.config.AppConfig;
import com.alphabugx.dialog.SettingsDialog;
import com.alphabugx.editor.JavaCodeEditor;
import com.alphabugx.jar.JarProcessor;
import com.alphabugx.search.GlobalSearchDialog;
import com.alphabugx.util.LogCapture;
import com.alphabugx.util.ProgressReporter;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class FileEditorApp {

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem packageMenuItem;

    @FXML
    private MenuItem openJarMenuItem;

    @FXML
    private MenuItem jdkConfigMenuItem;

    @FXML
    private MenuItem cfrConfigMenuItem;

    @FXML
    private MenuItem findMenuItem;

    @FXML
    private MenuItem globalSearchMenuItem;

    @FXML
    private CheckMenuItem detailProgressMenuItem;

    @FXML
    private Button toggleLogButton;

    @FXML
    private Button clearLogButton;

    @FXML
    private VBox logContainer;

    @FXML
    private TextArea logTextArea;

    @FXML
    private Label logStatusLabel;

    @FXML
    private SplitPane splitPane;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label lineColumnLabel;

    @FXML
    private Label saveStatusLabel;

    @FXML
    private Label filePathLabel;
    private JarProcessor jarProcessor;
    private File currentJarFile;
    private ProgressReporter progressReporter;
    private LogCapture logCapture;
    private Map<TreeItem<String>, File> itemToFileMap = new HashMap();
    private Map<Tab, File> tabToFileMap = new HashMap();
    private Map<Tab, JavaCodeEditor> tabToEditorMap = new HashMap();
    private Map<Tab, Boolean> tabModifiedMap = new HashMap();
    private Set<File> modifiedJavaFiles = new HashSet();

    public void initialize() {
        this.progressReporter = ProgressReporter.getInstance();
        this.logCapture = LogCapture.getInstance();
        this.logCapture.setLogTextArea(this.logTextArea);
        this.logCapture.startCapture();
        TreeItem<String> rootItem = new TreeItem<>("请选择Jar文件开始编辑");
        rootItem.setExpanded(true);
        this.treeView.setRoot(rootItem);
        this.treeView.setOnMouseClicked(this::onTreeItemClick);
        this.tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateStatusBar();
        });
        updateStatusBar();
        setupShutdownHook();
        Platform.runLater(() -> {
            String separator = repeatString("=", 50);
            this.logCapture.info(separator);
            this.logCapture.info("�� JAWD (Jar文件冷补丁编辑器) 启动成功");
            this.logCapture.info("版本: v1.0 | 作者: Alphabug");
            this.logCapture.info("支持功能: Jar解压、反编译、编辑、重打包");
            this.logCapture.info(separator);
            this.logCapture.info("�� 提示: 点击状态栏的「显示日志」按钮来查看详细操作日志");
        });
        Platform.runLater(this::showInitialJarSelectionDialog);
    }

    private void showInitialJarSelectionDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("欢迎使用Jar冷补丁编辑器");
        alert.setHeaderText("开始您的Jar编辑之旅");
        alert.setContentText("请选择一个Jar文件开始编辑。\n\n点击菜单栏：工具 → 打开Jar文件");
        ButtonType openJarButton = new ButtonType("立即选择Jar文件");
        ButtonType laterButton = new ButtonType("稍后选择", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(new ButtonType[]{openJarButton, laterButton});
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openJarButton) {
            onOpenJar();
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this.jarProcessor != null) {
                System.out.println("应用程序关闭，清理临时文件...");
                this.jarProcessor.cleanup();
            }
        }));
    }

    private void loadDirectory(File directory, TreeItem<String> parentItem) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().startsWith(".")) {
                    TreeItem<String> item = new TreeItem<>(file.getName());
                    parentItem.getChildren().add(item);
                    this.itemToFileMap.put(item, file);
                    if (file.isDirectory()) {
                        TreeItem<String> placeholder = new TreeItem<>("Loading...");
                        item.getChildren().add(placeholder);
                        item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                            if (isExpanded.booleanValue() && item.getChildren().size() == 1 && "Loading...".equals(((TreeItem) item.getChildren().get(0)).getValue())) {
                                item.getChildren().clear();
                                loadDirectory(file, item);
                            }
                        });
                    }
                }
            }
        }
    }

    @FXML
    private void onTreeItemClick(MouseEvent event) {
        TreeItem<String> selectedItem;
        File file;
        if (event.getClickCount() == 2 && (selectedItem = (TreeItem) this.treeView.getSelectionModel().getSelectedItem()) != null && (file = this.itemToFileMap.get(selectedItem)) != null && file.isFile()) {
            openFile(file);
        }
    }

    private void openFile(File file) {
        if (file.getName().toLowerCase().endsWith(".jar")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText("检测到Jar文件");
            alert.setContentText("这是一个Jar文件，请使用菜单栏的\"打开Jar文件 → 选择Jar文件...\"功能来处理Jar文件。");
            alert.showAndWait();
            return;
        }
        for (Tab tab : this.tabPane.getTabs()) {
            if (this.tabToFileMap.get(tab) != null && this.tabToFileMap.get(tab).equals(file)) {
                this.tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        Tab newTab = new Tab(file.getName());
        AnchorPane anchorPane = new AnchorPane();
        JavaCodeEditor codeEditor = new JavaCodeEditor();
        codeEditor.setCurrentFile(file);
        codeEditor.replaceText(readFile(file));
        codeEditor.setGlobalSearchCallback(this::onGlobalSearch);
        AnchorPane.setTopAnchor(codeEditor, Double.valueOf(0.0d));
        AnchorPane.setBottomAnchor(codeEditor, Double.valueOf(0.0d));
        AnchorPane.setLeftAnchor(codeEditor, Double.valueOf(0.0d));
        AnchorPane.setRightAnchor(codeEditor, Double.valueOf(0.0d));
        codeEditor.textProperty().addListener((obs, oldText, newText) -> {
            this.tabModifiedMap.put(newTab, true);
            updateTabTitle(newTab);
            updateStatusBar();
            if (file.getName().endsWith(".java") && this.jarProcessor != null) {
                this.modifiedJavaFiles.add(file);
            }
        });
        codeEditor.caretPositionProperty().addListener((obs2, oldPos, newPos) -> {
            updateStatusBar();
        });
        anchorPane.getChildren().add(codeEditor);
        newTab.setContent(anchorPane);
        this.tabToFileMap.put(newTab, file);
        this.tabToEditorMap.put(newTab, codeEditor);
        this.tabModifiedMap.put(newTab, false);
        newTab.setOnCloseRequest(event -> {
            if (this.tabModifiedMap.get(newTab).booleanValue()) {
                Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
                alert2.setTitle("确认");
                alert2.setHeaderText("文件已修改");
                alert2.setContentText("是否保存文件 " + file.getName() + "？");
                ButtonType saveButton = new ButtonType("保存");
                ButtonType dontSaveButton = new ButtonType("不保存");
                ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert2.getButtonTypes().setAll(new ButtonType[]{saveButton, dontSaveButton, cancelButton});
                Optional<ButtonType> result = alert2.showAndWait();
                if (result.get() == saveButton) {
                    saveTabFile(newTab);
                } else if (result.get() == cancelButton) {
                    event.consume();
                    return;
                }
            }
            this.tabToFileMap.remove(newTab);
            JavaCodeEditor editor = this.tabToEditorMap.remove(newTab);
            if (editor != null) {
                editor.cleanup();
            }
            this.tabModifiedMap.remove(newTab);
            this.tabPane.getTabs().remove(newTab);
            updateStatusBar();
        });
        this.tabPane.getTabs().add(newTab);
        this.tabPane.getSelectionModel().select(newTab);
        updateStatusBar();
    }

    private void updateTabTitle(Tab tab) {
        File file = this.tabToFileMap.get(tab);
        if (file != null) {
            String title = file.getName();
            if (this.tabModifiedMap.get(tab).booleanValue()) {
                title = title + " *";
            }
            tab.setText(title);
        }
    }

    private void updateStatusBar() {
        Tab currentTab = (Tab) this.tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && this.tabToFileMap.containsKey(currentTab)) {
            File file = this.tabToFileMap.get(currentTab);
            this.filePathLabel.setText(file.getAbsolutePath());
            boolean isModified = this.tabModifiedMap.getOrDefault(currentTab, false).booleanValue();
            this.saveStatusLabel.setText(isModified ? "未保存" : "已保存");
            JavaCodeEditor codeEditor = this.tabToEditorMap.get(currentTab);
            if (codeEditor != null) {
                String text = codeEditor.getText();
                int caretPosition = codeEditor.getCaretPosition();
                int line = 1;
                int column = 1;
                for (int i = 0; i < caretPosition && i < text.length(); i++) {
                    if (text.charAt(i) == '\n') {
                        line++;
                        column = 1;
                    } else {
                        column++;
                    }
                }
                this.lineColumnLabel.setText("行: " + line + ", 列: " + column);
                return;
            }
            return;
        }
        this.filePathLabel.setText("无文件");
        this.saveStatusLabel.setText("已保存");
        this.lineColumnLabel.setText("行: 1, 列: 1");
    }

    private String readFile(File file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file.toURI())));
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading file: " + e.getMessage();
        }
    }

    private void saveTabFile(Tab tab) {
        File file = (File)this.tabToFileMap.get(tab);
        JavaCodeEditor codeEditor = (JavaCodeEditor)this.tabToEditorMap.get(tab);
        if (file != null && codeEditor != null) {
            String content = codeEditor.getText();

            try {
                Files.write(Paths.get(file.toURI()), content.getBytes(), new OpenOption[0]);
                this.tabModifiedMap.put(tab, false);
                this.updateTabTitle(tab);
                this.updateStatusBar();
                System.out.println("文件已保存: " + file.getName());
            } catch (IOException var7) {
                IOException e = var7;
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("保存失败");
                alert.setHeaderText("无法保存文件");
                alert.setContentText("错误信息: " + e.getMessage());
                alert.showAndWait();
            }
        }

    }

    @FXML
    private void onNewFile() {
        Tab newTab = new Tab("未命名");
        AnchorPane anchorPane = new AnchorPane();
        TextArea fileTextArea = new TextArea();
        fileTextArea.setWrapText(true);
        AnchorPane.setTopAnchor(fileTextArea, Double.valueOf(0.0d));
        AnchorPane.setBottomAnchor(fileTextArea, Double.valueOf(0.0d));
        AnchorPane.setLeftAnchor(fileTextArea, Double.valueOf(0.0d));
        AnchorPane.setRightAnchor(fileTextArea, Double.valueOf(0.0d));
        fileTextArea.textProperty().addListener((obs, oldText, newText) -> {
            this.tabModifiedMap.put(newTab, true);
            updateTabTitle(newTab);
            updateStatusBar();
        });
        fileTextArea.caretPositionProperty().addListener((obs2, oldPos, newPos) -> {
            updateStatusBar();
        });
        anchorPane.getChildren().add(fileTextArea);
        newTab.setContent(anchorPane);
        this.tabModifiedMap.put(newTab, false);
        this.tabPane.getTabs().add(newTab);
        this.tabPane.getSelectionModel().select(newTab);
        updateStatusBar();
    }

    @FXML
    private void onOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter[]{new FileChooser.ExtensionFilter("所有文件", new String[]{"*.*"}), new FileChooser.ExtensionFilter("Java文件", new String[]{"*.java"}), new FileChooser.ExtensionFilter("文本文件", new String[]{"*.txt"}), new FileChooser.ExtensionFilter("XML文件", new String[]{"*.xml"}), new FileChooser.ExtensionFilter("配置文件", new String[]{"*.properties", "*.yml", "*.yaml"})});
        Stage stage = (Stage)this.menuBar.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            if (selectedFile.getName().toLowerCase().endsWith(".jar")) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("提示");
                alert.setHeaderText("检测到Jar文件");
                alert.setContentText("您选择的是Jar文件。如果要进行Jar冷补丁编辑，请使用：\n工具 → 打开Jar文件");
                alert.showAndWait();
                return;
            }
            openFile(selectedFile);
        }
    }

    @FXML
    private void onSaveButtonClick() throws IOException {
        Tab selectedTab = (Tab) this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            if (this.tabToFileMap.containsKey(selectedTab)) {
                saveTabFile(selectedTab);
            } else {
                onSaveAsFile();
            }
        }
    }

    @FXML
    private void onSaveAsFile() throws IOException {
        Tab selectedTab = (Tab) this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("另存为");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            Stage stage = (Stage)this.menuBar.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);
            if (selectedFile != null) {
                AnchorPane anchorPane = (AnchorPane)selectedTab.getContent();
                TextArea textArea = (TextArea) anchorPane.getChildren().get(0);
                String content = textArea.getText();
                try {
                    Files.write(Paths.get(selectedFile.toURI()), content.getBytes(), new OpenOption[0]);
                    this.tabToFileMap.put(selectedTab, selectedFile);
                    this.tabModifiedMap.put(selectedTab, false);
                    selectedTab.setText(selectedFile.getName());
                    updateStatusBar();
                    System.out.println("文件已保存: " + selectedFile.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("保存失败");
                    alert.setHeaderText("无法保存文件");
                    alert.setContentText("错误信息: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    @FXML
    private void onExit() {
        Stage stage = (Stage) this.menuBar.getScene().getWindow();
        handleWindowClose(stage);
    }

    private void handleWindowClose(Stage stage) {
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
    }

    private void performForceExit() {
        try {
            if (this.jarProcessor != null) {
                System.out.println("清理临时文件...");
                this.jarProcessor.cleanup();
            }
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

    @FXML
    private void onToggleLogArea() {
        boolean isVisible = this.logContainer.isVisible();
        if (isVisible) {
            this.logContainer.setVisible(false);
            this.logContainer.setManaged(false);
            this.toggleLogButton.setText("显示日志");
            this.logStatusLabel.setText("日志已隐藏");
            return;
        }
        this.logContainer.setVisible(true);
        this.logContainer.setManaged(true);
        this.toggleLogButton.setText("隐藏日志");
        this.logStatusLabel.setText("日志监控中");
        this.logCapture.info("日志区域已打开，所有系统输出将显示在这里");
    }

    @FXML
    private void onClearLog() {
        if (this.logCapture != null) {
            this.logCapture.clearLog();
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

    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String formatFileSize(long bytes) {
        return bytes < 1024 ? bytes + " B" : bytes < 1048576 ? String.format("%.1f KB", Double.valueOf(bytes / 1024.0d)) : String.format("%.1f MB", Double.valueOf(bytes / 1048576.0d));
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于JAWD - by Alphabug");
        alert.setHeaderText("Jar冷补丁编辑器 (JAWD) - v1.0 - 作者: Alphabug");
        alert.setContentText("�� Jar冷补丁编辑器 - 让修补变得so easy！\n\n✨ 核心功能:\n• 一键反编译Jar文件\n• 可视化代码编辑器\n• 智能编译+多线程打包\n• 完美支持Spring Boot\n\n�� 实战场景:\n• �� 紧急漏洞修复 - 线上救火必备\n• �� AWD攻防比赛 - 快速打补丁神器\n• �� 代码逆向分析 - 看透第三方库\n• �� Java学习研究 - 探索底层奥秘\n\n⚡ 性能亮点:\n• 多线程处理，速度提升3-4倍\n• 实时进度显示，告别卡顿\n• 智能依赖解析，编译无忧\n\n��\u200d�� 开发者信息:\n• 作者: Alphabug\n• 版本: v1.0\n• 发布: 2024年1月\n• 专注: 安全工具开发\n\n�� 小贴士: 首次使用记得配置JDK和CFR路径哦~\n\n�� 让每一次Jar编辑都成为愉快的体验！");
        alert.getDialogPane().setPrefWidth(480.0d);
        alert.getDialogPane().setPrefHeight(400.0d);
        alert.setResizable(true);
        alert.showAndWait();
    }

    @FXML
    private void onFind() {
        JavaCodeEditor codeEditor;
        Tab currentTab = (Tab) this.tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && (codeEditor = this.tabToEditorMap.get(currentTab)) != null) {
            System.out.println("打开当前文件搜索");
            showSimpleSearchDialog(codeEditor);
        }
    }

    @FXML
    private void onGlobalSearch() {
        if (this.jarProcessor != null && this.jarProcessor.getWorkDir() != null) {
            Stage stage = (Stage)this.menuBar.getScene().getWindow();
            GlobalSearchDialog searchDialog = new GlobalSearchDialog(stage, this.jarProcessor.getWorkDir());
            searchDialog.setResultHandler(result -> {
                openFileAndGoToLine(new File(result.getFilePath()), result.getLineNumber());
            });
            searchDialog.showDialog();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("全局搜索");
        alert.setHeaderText("无法执行全局搜索");
        alert.setContentText("请先打开一个Jar文件，然后才能在反编译的Java文件中进行全局搜索。");
        alert.showAndWait();
    }

    private void showSimpleSearchDialog(JavaCodeEditor codeEditor) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("查找");
        dialog.setHeaderText("在当前文件中查找");
        dialog.setContentText("请输入搜索内容:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String searchText = result.get().trim();
            codeEditor.searchText(searchText, false, false);
        }
    }

    private void openFileAndGoToLine(File file, int lineNumber) {
        openFile(file);
        Platform.runLater(() -> {
            JavaCodeEditor codeEditor;
            Tab currentTab = (Tab) this.tabPane.getSelectionModel().getSelectedItem();
            if (currentTab != null && (codeEditor = this.tabToEditorMap.get(currentTab)) != null) {
                try {
                    codeEditor.moveTo(lineNumber - 1, 0);
                    codeEditor.requestFollowCaret();
                } catch (Exception e) {
                    System.err.println("跳转到行失败: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onPackageButtonClick() throws IOException {
        if (this.jarProcessor == null || this.currentJarFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("打包失败");
            alert.setHeaderText("没有可打包的Jar文件");
            alert.setContentText("请先打开一个Jar文件进行编辑");
            alert.showAndWait();
            return;
        }
        saveAllModifiedFiles();
        repackageJar();
    }

    @FXML
    private void onOpenJar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要编辑的Jar文件");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar文件", new String[]{"*.jar"}));
        Stage stage = (Stage)this.menuBar.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            openJarFile(selectedFile);
        }
    }

    @FXML
    private void onJdkConfig() {
        Stage stage = (Stage)this.menuBar.getScene().getWindow();
        SettingsDialog.showJdkConfigDialog(stage);
    }

    @FXML
    private void onCfrConfig() {
        Stage stage = (Stage)this.menuBar.getScene().getWindow();
        SettingsDialog.showCfrConfigDialog(stage);
    }

    @FXML
    private void onToggleProgressMode() {
        if (this.progressReporter != null) {
            this.progressReporter.toggleMode();
            boolean isDetailed = this.progressReporter.getCurrentMode() == ProgressReporter.DisplayMode.DETAILED;
            this.detailProgressMenuItem.setSelected(isDetailed);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("进度显示模式");
            alert.setHeaderText("进度显示模式已切换");
            alert.setContentText("当前模式: " + (isDetailed ? "详细模式" : "简洁模式") + "\n\n" + (isDetailed ? "详细模式：显示每个操作的详细信息、时间戳和错误详情" : "简洁模式：只显示主要进度条和当前操作"));
            alert.showAndWait();
        }
    }

    private void openJarFile(final File jarFile) {
        this.logCapture.info("\ud83d\udcc2 开始处理Jar文件: " + jarFile.getName());
        this.logCapture.info("文件路径: " + jarFile.getAbsolutePath());
        this.logCapture.info("文件大小: " + this.formatFileSize(jarFile.length()));
        Task<Void> configCheckTask = new Task<Void>() {
            protected Void call() throws Exception {
                AppConfig config = AppConfig.getInstance();
                if (config.getJdkPath().isEmpty()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("配置不完整");
                        alert.setHeaderText("JDK路径未配置");
                        alert.setContentText("请先配置JDK路径才能处理Jar文件");
                        alert.showAndWait();
                    });
                    return null;
                } else if (!(new File(config.getCfrPath())).exists()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("配置不完整");
                        alert.setHeaderText("CFR工具未找到");
                        alert.setContentText("CFR工具路径: " + config.getCfrPath() + "\n请确保CFR工具存在或重新配置路径");
                        alert.showAndWait();
                    });
                    return null;
                } else {
                    Platform.runLater(() -> {
                        FileEditorApp.this.processJarFileInBackground(jarFile);
                    });
                    return null;
                }
            }
        };
        Thread configThread = new Thread(configCheckTask);
        configThread.setDaemon(true);
        configThread.start();
    }
    private void processJarFileInBackground(final File jarFile) {
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.show();
        Task<Void> task = new Task<Void>() {
            protected Void call() throws Exception {
                try {
                    this.updateMessage("初始化工作环境...");
                    this.updateProgress(0L, 100L);
                    if (FileEditorApp.this.jarProcessor != null) {
                        FileEditorApp.this.jarProcessor.cleanup();
                    }

                    FileEditorApp.this.jarProcessor = new JarProcessor();
                    FileEditorApp.this.currentJarFile = jarFile;
                    FileEditorApp.this.modifiedJavaFiles.clear();
                    this.updateMessage("解压Jar文件...");
                    this.updateProgress(10L, 100L);
                    FileEditorApp.this.jarProcessor.initWorkspace(jarFile);
                    this.updateMessage("正在解压Jar内容...");
                    this.updateProgress(20L, 100L);
                    FileEditorApp.this.jarProcessor.extractJar();
                    this.updateMessage("复制Class文件...");
                    this.updateProgress(35L, 100L);
                    FileEditorApp.this.jarProcessor.copyClassFiles();
                    this.updateMessage("开始多线程反编译Class文件...");
                    this.updateProgress(30L, 100L);
                    FileEditorApp.this.jarProcessor.decompileClasses();
                    this.updateMessage("反编译完成，正在整理文件...");
                    this.updateProgress(75L, 100L);
                    this.updateMessage("加载到编辑器...");
                    this.updateProgress(90L, 100L);
                    Platform.runLater(() -> {
                        try {
                            FileEditorApp.this.loadJarToEditor();
                            this.updateProgress(100L, 100L);
                            this.updateMessage("Jar文件加载完成");
                        } catch (Exception var2) {
                            Exception e = var2;
                            System.err.println("加载编辑器失败: " + e.getMessage());
                            e.printStackTrace();
                        }

                    });
                    Thread.sleep(500L);
                    return null;
                } catch (Exception var2) {
                    Exception e = var2;
                    e.printStackTrace();
                    throw new RuntimeException("处理Jar文件失败: " + e.getMessage(), e);
                }
            }
        };
        progressDialog.bindToTask(task);
        task.setOnSucceeded((e) -> {
            Platform.runLater(() -> {
                progressDialog.close();
                this.showInfo("成功", "Jar文件已加载到编辑器中\n\n工作目录: " + this.jarProcessor.getWorkDir().getAbsolutePath() + "\n\n现在可以开始编辑Java源码了！");
            });
        });
        task.setOnFailed((e) -> {
            Platform.runLater(() -> {
                progressDialog.close();
                Throwable exception = task.getException();
                exception.printStackTrace();
                this.showError("处理失败", "处理Jar文件时发生错误", exception.getMessage() + "\n\n请检查：\n1. Jar文件是否完整\n2. CFR工具是否可用\n3. 是否有足够的磁盘空间");
                if (this.jarProcessor != null) {
                    this.jarProcessor.cleanup();
                    this.jarProcessor = null;
                }

                this.currentJarFile = null;
            });
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


    public void loadJarToEditor() {
        this.tabPane.getTabs().clear();
        TreeItem<String> rootItem = new TreeItem<>(this.currentJarFile.getName() + " (反编译)");
        rootItem.setExpanded(true);
        this.treeView.setRoot(rootItem);
        this.itemToFileMap.clear();
        this.tabToFileMap.clear();
        this.tabModifiedMap.clear();
        File sourceDir = this.jarProcessor.getSourceDir();
        this.itemToFileMap.put(rootItem, sourceDir);
        loadDirectory(sourceDir, rootItem);
        updateStatusBar();
    }

    private void saveAllModifiedFiles() throws IOException {
        for (Tab tab : this.tabPane.getTabs()) {
            if (this.tabModifiedMap.getOrDefault(tab, false).booleanValue()) {
                saveTabFile(tab);
                File file = this.tabToFileMap.get(tab);
                if (file != null && file.getName().endsWith(".java")) {
                    this.modifiedJavaFiles.add(file);
                }
            }
        }
    }

    private void repackageJar() {
        if (this.modifiedJavaFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("打包");
            alert.setHeaderText("没有修改的文件");
            alert.setContentText("没有检测到修改的Java文件，无需重新编译");
            alert.showAndWait();
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存打包后的Jar文件");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            fileChooser.setInitialFileName(this.currentJarFile.getName().replace(".jar", "_patched.jar"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar文件", new String[]{"*.jar"}));
            Stage stage = (Stage)this.menuBar.getScene().getWindow();
            final File outputFile = fileChooser.showSaveDialog(stage);
            if (outputFile != null) {
                ProgressDialog progressDialog = new ProgressDialog();
                progressDialog.show();
                Task<File> task = new Task<File>() {
                    protected File call() throws Exception {
                        try {
                            this.updateMessage("编译修改的Java文件...");
                            this.updateProgress(5L, 100L);
                            List<File> javaFilesList = new ArrayList(FileEditorApp.this.modifiedJavaFiles);
                            this.updateMessage("编译 " + javaFilesList.size() + " 个修改的Java文件...");
                            FileEditorApp.this.jarProcessor.compileJavaFiles(javaFilesList);
                            this.updateMessage("编译完成，开始打包...");
                            this.updateProgress(20L, 100L);
                            this.updateMessage("替换编译后的class文件...");
                            this.updateProgress(25L, 100L);
                            this.updateMessage("开始重新打包Jar文件...");
                            this.updateProgress(30L, 100L);
                            FileEditorApp.this.jarProcessor.setProgressCallback((step, progress, message) -> {
                                Platform.runLater(() -> {
                                    double totalProgress = 30.0 + progress * 70.0 / 100.0;
                                    this.updateProgress(totalProgress, 100.0);
                                    this.updateMessage(message);
                                });
                            });
                            File result = FileEditorApp.this.jarProcessor.repackageJar(outputFile.getAbsolutePath());
                            this.updateProgress(100L, 100L);
                            this.updateMessage("打包完成");
                            return result;
                        } catch (Exception var3) {
                            Exception e = var3;
                            e.printStackTrace();
                            throw new RuntimeException("打包失败: " + e.getMessage(), e);
                        }
                    }
                };
                progressDialog.bindToTask(task);
                task.setOnSucceeded((e) -> {
                    progressDialog.close();
                    File result = (File)task.getValue();
                    this.showInfo("打包成功", "Jar文件已保存到: " + result.getAbsolutePath());
                    this.modifiedJavaFiles.clear();
                });
                task.setOnFailed((e) -> {
                    progressDialog.close();
                    Throwable exception = task.getException();
                    this.showError("打包失败", "重新打包Jar文件时发生错误", exception.getMessage());
                });
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }


    private static class ProgressDialog {
        private Dialog<Void> dialog = new Dialog<>();
        private ProgressBar progressBar;
        private Label messageLabel;
        private Label detailLabel;
        private Task<?> currentTask;

        public ProgressDialog() {
            this.dialog.setTitle("Jar冷补丁处理中");
            this.dialog.setHeaderText("正在处理Jar文件，请耐心等待...");
            this.progressBar = new ProgressBar(0.0d);
            this.progressBar.setPrefWidth(280.0d);
            this.messageLabel = new Label("初始化工作环境...");
            this.messageLabel.setStyle("-fx-font-weight: bold;");
            this.detailLabel = new Label("准备开始处理");
            this.detailLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
            AnchorPane content = new AnchorPane();
            content.setPrefSize(320.0d, 120.0d);
            content.getChildren().addAll(new Node[]{this.progressBar, this.messageLabel, this.detailLabel});
            AnchorPane.setTopAnchor(this.progressBar, Double.valueOf(20.0d));
            AnchorPane.setLeftAnchor(this.progressBar, Double.valueOf(20.0d));
            AnchorPane.setRightAnchor(this.progressBar, Double.valueOf(20.0d));
            AnchorPane.setTopAnchor(this.messageLabel, Double.valueOf(50.0d));
            AnchorPane.setLeftAnchor(this.messageLabel, Double.valueOf(20.0d));
            AnchorPane.setRightAnchor(this.messageLabel, Double.valueOf(20.0d));
            AnchorPane.setTopAnchor(this.detailLabel, Double.valueOf(75.0d));
            AnchorPane.setLeftAnchor(this.detailLabel, Double.valueOf(20.0d));
            AnchorPane.setRightAnchor(this.detailLabel, Double.valueOf(20.0d));
            this.dialog.getDialogPane().setContent(content);
            ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            this.dialog.getDialogPane().getButtonTypes().add(cancelButtonType);
            this.dialog.setResultConverter(dialogButton -> {
                if (dialogButton == cancelButtonType && this.currentTask != null) {
                    this.currentTask.cancel();
                    return null;
                }
                return null;
            });
        }

        public void show() {
            this.dialog.show();
        }

        public void close() {
            this.dialog.close();
        }

        public void bindToTask(Task<?> task) {
            this.currentTask = task;
            this.progressBar.progressProperty().bind(task.progressProperty());
            this.messageLabel.textProperty().bind(task.messageProperty());
            task.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress != null) {
                    int percentage = (int) (newProgress.doubleValue() * 100.0d);
                    Platform.runLater(() -> {
                        this.detailLabel.setText("已完成 " + percentage + "%");
                    });
                }
            });
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText((String) null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
