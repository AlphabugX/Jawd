package com.alphabugx.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class GlobalSearchDialog {
    private Stage dialog;
    private TextField searchField;
    private CheckBox regexCheckBox;
    private CheckBox caseSensitiveCheckBox;
    private CheckBox wholeWordCheckBox;
    private TableView<SearchResult> resultsTable;
    private Label statusLabel;
    private Button searchButton;
    private File searchDirectory;
    private SearchResultHandler resultHandler;


    public interface SearchResultHandler {
        void onResultSelected(SearchResult searchResult);
    }

    public GlobalSearchDialog(Stage owner, File searchDirectory) {
        this.searchDirectory = searchDirectory;
        initializeDialog(owner);
    }

    private void initializeDialog(Stage owner) {
        this.dialog = new Stage();
        this.dialog.initModality(Modality.NONE);
        this.dialog.initOwner(owner);
        this.dialog.setTitle("全局搜索 - 在所有Java文件中查找");
        this.dialog.setResizable(true);
        createSearchControls();
        createResultsTable();
        createLayout();
        Scene scene = new Scene(createMainLayout(), 800.0d, 600.0d);
        this.dialog.setScene(scene);
    }

    private void createSearchControls() {
        this.searchField = new TextField();
        this.searchField.setPromptText("输入搜索关键字...");
        this.searchField.setPrefWidth(300.0d);
        this.regexCheckBox = new CheckBox("正则表达式");
        this.caseSensitiveCheckBox = new CheckBox("区分大小写");
        this.wholeWordCheckBox = new CheckBox("全词匹配");
        this.searchButton = new Button("搜索");
        this.searchButton.setDefaultButton(true);
        this.searchButton.setOnAction(e -> {
            performSearch();
        });
        Button clearButton = new Button("清空");
        clearButton.setOnAction(e2 -> {
            clearResults();
        });
        this.searchField.setOnAction(e3 -> {
            performSearch();
        });
        this.statusLabel = new Label("请输入搜索关键字");
    }

    private void createResultsTable() {
        this.resultsTable = new TableView<>();
        TableColumn<SearchResult, String> fileColumn = new TableColumn<>("文件");
        fileColumn.setCellValueFactory(new PropertyValueFactory("fileName"));
        fileColumn.setPrefWidth(150.0d);
        TableColumn<SearchResult, Integer> lineColumn = new TableColumn<>("行号");
        lineColumn.setCellValueFactory(new PropertyValueFactory("lineNumber"));
        lineColumn.setPrefWidth(60.0d);
        TableColumn<SearchResult, String> contentColumn = new TableColumn<>("匹配内容");
        contentColumn.setCellValueFactory(new PropertyValueFactory("matchedLine"));
        contentColumn.setPrefWidth(500.0d);
        TableColumn<SearchResult, String> pathColumn = new TableColumn<>("路径");
        pathColumn.setCellValueFactory(new PropertyValueFactory("filePath"));
        pathColumn.setPrefWidth(200.0d);
        this.resultsTable.getColumns().addAll(new TableColumn[]{fileColumn, lineColumn, contentColumn, pathColumn});
        this.resultsTable.setOnMouseClicked(event -> {
            SearchResult selected;
            if (event.getClickCount() == 2 && this.resultHandler != null && (selected = (SearchResult) this.resultsTable.getSelectionModel().getSelectedItem()) != null) {
                this.resultHandler.onResultSelected(selected);
            }
        });
    }

    private BorderPane createMainLayout() {
        BorderPane mainLayout = new BorderPane();
        VBox topArea = new VBox(10.0);
        topArea.setPadding(new Insets(10.0));
        HBox searchBox = new HBox(10.0);
        searchBox.getChildren().addAll(new Node[]{new Label("搜索:"), this.searchField, this.searchButton, new Button("清空")});
        HBox optionsBox = new HBox(15.0);
        optionsBox.getChildren().addAll(new Node[]{this.regexCheckBox, this.caseSensitiveCheckBox, this.wholeWordCheckBox});
        topArea.getChildren().addAll(new Node[]{searchBox, optionsBox, this.statusLabel});
        mainLayout.setTop(topArea);
        mainLayout.setCenter(this.resultsTable);
        HBox bottomArea = new HBox(10.0);
        bottomArea.setPadding(new Insets(10.0));
        Button closeButton = new Button("关闭");
        closeButton.setOnAction((e) -> {
            this.dialog.close();
        });
        bottomArea.getChildren().add(closeButton);
        mainLayout.setBottom(bottomArea);
        return mainLayout;
    }

    private void createLayout() {
    }

    private void performSearch() {
        final String searchText = this.searchField.getText().trim();
        if (searchText.isEmpty()) {
            this.showStatus("请输入搜索关键字");
        } else {
            this.searchButton.setDisable(true);
            this.showStatus("搜索中...");
            this.clearResults();
            Task<List<SearchResult>> searchTask = new Task<List<SearchResult>>() {
                protected List<SearchResult> call() throws Exception {
                    return GlobalSearchDialog.this.searchInFiles(searchText);
                }
            };
            searchTask.setOnSucceeded((e) -> {
                List<SearchResult> results = (List)searchTask.getValue();
                Platform.runLater(() -> {
                    this.resultsTable.getItems().addAll(results);
                    this.showStatus("找到 " + results.size() + " 个匹配项");
                    this.searchButton.setDisable(false);
                });
            });
            searchTask.setOnFailed((e) -> {
                Platform.runLater(() -> {
                    this.showStatus("搜索失败: " + searchTask.getException().getMessage());
                    this.searchButton.setDisable(false);
                });
            });
            Thread searchThread = new Thread(searchTask);
            searchThread.setDaemon(true);
            searchThread.start();
        }
    }


    public List<SearchResult> searchInFiles(String searchText) {
        List<SearchResult> results = new ArrayList<>();
        List<File> javaFiles = findJavaFiles(this.searchDirectory);
        try {
            Pattern pattern = createSearchPattern(searchText);
            for (File file : javaFiles) {
                searchInFile(file, pattern, results);
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("搜索过程中发生错误", e);
        }
    }

    private Pattern createSearchPattern(String searchText) {
        String patternString;
        int flags = 0;
        if (!this.caseSensitiveCheckBox.isSelected()) {
            flags = 0 | 2;
        }
        if (this.regexCheckBox.isSelected()) {
            patternString = searchText;
        } else {
            patternString = Pattern.quote(searchText);
            if (this.wholeWordCheckBox.isSelected()) {
                patternString = "\\b" + patternString + "\\b";
            }
        }
        return Pattern.compile(patternString, flags);
    }

    private void searchInFile(File file, Pattern pattern, List<SearchResult> results) throws IOException {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    SearchResult result = new SearchResult(file.getName(), file.getAbsolutePath(), i + 1, line.trim(), matcher.start(), matcher.end());
                    results.add(result);
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件失败: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursive(directory, javaFiles);
        return javaFiles;
    }

    private void findJavaFilesRecursive(File directory, List<File> javaFiles) {
        File[] files;
        if (directory.exists() && directory.isDirectory() && (files = directory.listFiles()) != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findJavaFilesRecursive(file, javaFiles);
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
    }

    private void clearResults() {
        this.resultsTable.getItems().clear();
    }

    private void showStatus(String message) {
        this.statusLabel.setText(message);
    }

    public void setResultHandler(SearchResultHandler handler) {
        this.resultHandler = handler;
    }

    public void showDialog() {
        this.dialog.show();
    }

    public void setSearchText(String text) {
        this.searchField.setText(text);
    }


    public static class SearchResult {
        private final String fileName;
        private final String filePath;
        private final int lineNumber;
        private final String matchedLine;
        private final int startPos;
        private final int endPos;

        public SearchResult(String fileName, String filePath, int lineNumber, String matchedLine, int startPos, int endPos) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.matchedLine = matchedLine;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getFilePath() {
            return this.filePath;
        }

        public int getLineNumber() {
            return this.lineNumber;
        }

        public String getMatchedLine() {
            return this.matchedLine;
        }

        public int getStartPos() {
            return this.startPos;
        }

        public int getEndPos() {
            return this.endPos;
        }
    }
}
