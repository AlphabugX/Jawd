package com.alphabugx.editor;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.StyledDocument;


public class JavaCodeEditor extends CodeArea {
    private static final String[] KEYWORDS;
    private static final String KEYWORD_PATTERN;
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    private static final String ANNOTATION_PATTERN = "@\\w+";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b";
    private static final Pattern PATTERN;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private File currentFile;
    private Runnable globalSearchCallback;
    static final /* synthetic */ boolean $assertionsDisabled;

    static {
        $assertionsDisabled = !JavaCodeEditor.class.desiredAssertionStatus();
        KEYWORDS = new String[]{"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};
        KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")|(?<PAREN>" + PAREN_PATTERN + ")|(?<BRACE>" + BRACE_PATTERN + ")|(?<BRACKET>" + BRACKET_PATTERN + ")|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")|(?<STRING>" + STRING_PATTERN + ")|(?<COMMENT>" + COMMENT_PATTERN + ")|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")|(?<NUMBER>" + NUMBER_PATTERN + ")");
    }

    public JavaCodeEditor() {
        initializeEditor();
        setupSyntaxHighlighting();
        setupCodeNavigation();
        setupKeyboardShortcuts();
    }

    private void initializeEditor() {
        setParagraphGraphicFactory(LineNumberFactory.get(this));
        setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 14px;");
        applyLightTheme();
    }

    private void applyLightTheme() {
        try {
            String cssUrl = getClass().getResource("/com/alphabugx/editor/themes/intellij-light.css").toExternalForm();
            getStylesheets().clear();
            getStylesheets().add(cssUrl);
        } catch (Exception e) {
            System.err.println("无法加载IntelliJ Light主题CSS文件，使用默认样式");
            setStyle(getStyle() + "; -fx-background-color: #ffffff; -fx-text-fill: #000000;");
        }
        if (!getText().isEmpty()) {
            setStyleSpans(0, computeHighlighting(getText()));
        }
    }

    private void setupSyntaxHighlighting() {
        this.richChanges().filter((ch) -> {
            return !((StyledDocument)ch.getInserted()).equals(ch.getRemoved());
        }).successionEnds(Duration.ofMillis(500L)).subscribe((ignore) -> {
            Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
                protected StyleSpans<Collection<String>> call() throws Exception {
                    return JavaCodeEditor.computeHighlighting(JavaCodeEditor.this.getText());
                }
            };
            task.setOnSucceeded((e) -> {
                this.setStyleSpans(0, (StyleSpans)task.getValue());
            });
            this.executor.execute(task);
        });
    }


    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        String str;
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int iEnd = 0;
        while (true) {
            int lastKwEnd = iEnd;
            if (matcher.find()) {
                if (matcher.group("KEYWORD") != null) {
                    str = "keyword";
                } else if (matcher.group("PAREN") != null) {
                    str = "paren";
                } else if (matcher.group("BRACE") != null) {
                    str = "brace";
                } else if (matcher.group("BRACKET") != null) {
                    str = "bracket";
                } else if (matcher.group("SEMICOLON") != null) {
                    str = "semicolon";
                } else if (matcher.group("STRING") != null) {
                    str = "string";
                } else if (matcher.group("COMMENT") != null) {
                    str = "comment";
                } else if (matcher.group("ANNOTATION") != null) {
                    str = "annotation";
                } else {
                    str = matcher.group("NUMBER") != null ? "number" : null;
                }
                String styleClass = str;
                if (!$assertionsDisabled && styleClass == null) {
                    throw new AssertionError();
                }
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                iEnd = matcher.end();
            } else {
                spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
                return spansBuilder.create();
            }
        }
    }

    private void setupCodeNavigation() {
    }

    private void setupKeyboardShortcuts() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (AnonymousClass2.$SwitchMap$javafx$scene$input$KeyCode[event.getCode().ordinal()]) {
                    case 1:
                        if (this.globalSearchCallback != null) {
                            this.globalSearchCallback.run();
                        }
                        event.consume();
                        break;
                    case 2:
                        openGoToLineDialog();
                        event.consume();
                        break;
                    case 3:
                        showCodeCompletion();
                        event.consume();
                        break;
                }
            }
        });
    }

    /* renamed from: com.alphabugx.editor.JavaCodeEditor$2, reason: invalid class name */

    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$javafx$scene$input$KeyCode = new int[KeyCode.values().length];

        static {
            try {
                $SwitchMap$javafx$scene$input$KeyCode[KeyCode.F.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$javafx$scene$input$KeyCode[KeyCode.G.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$javafx$scene$input$KeyCode[KeyCode.SPACE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public void searchText(String searchText, boolean useRegex, boolean caseSensitive) {
        Pattern pattern;
        String text = getText();
        try {
            if (useRegex) {
                int flags = caseSensitive ? 0 : 2;
                pattern = Pattern.compile(searchText, flags);
            } else {
                String escaped = Pattern.quote(searchText);
                int flags2 = caseSensitive ? 0 : 2;
                pattern = Pattern.compile(escaped, flags2);
            }
            Matcher matcher = pattern.matcher(text);
            clearSearchHighlight();
            while (matcher.find()) {
                setStyle(matcher.start(), matcher.end(), Collections.singleton("search-highlight"));
            }
            matcher.reset();
            if (matcher.find()) {
                selectRange(matcher.start(), matcher.end());
                requestFollowCaret();
            }
        } catch (Exception e) {
            System.err.println("搜索表达式错误: " + e.getMessage());
        }
    }

    private void clearSearchHighlight() {
        clearStyle(0, getLength());
        setStyleSpans(0, computeHighlighting(getText()));
    }

    private void openSearchDialog() {
        System.out.println("打开搜索对话框");
    }

    private void openGoToLineDialog() {
        System.out.println("打开跳转到行对话框");
    }

    private void showCodeCompletion() {
        System.out.println("显示代码补全");
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
    }

    public void setGlobalSearchCallback(Runnable callback) {
        this.globalSearchCallback = callback;
    }

    public File getCurrentFile() {
        return this.currentFile;
    }

    public void cleanup() {
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }
}
