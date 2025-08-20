package com.alphabugx.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Platform;
import javafx.scene.control.TextArea;


public class LogCapture {
    private static LogCapture instance;
    private TextArea logTextArea;
    private PrintStream customOut;
    private PrintStream customErr;
    private boolean isCapturing = false;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private PrintStream originalOut = System.out;
    private PrintStream originalErr = System.err;


    private enum LogLevel {
        INFO,
        ERROR,
        WARN,
        DEBUG
    }

    private LogCapture() {
    }

    public static LogCapture getInstance() {
        if (instance == null) {
            instance = new LogCapture();
        }
        return instance;
    }

    public void setLogTextArea(TextArea logTextArea) {
        this.logTextArea = logTextArea;
    }

    public void startCapture() {
        if (this.isCapturing || this.logTextArea == null) {
            return;
        }
        this.isCapturing = true;
        this.customOut = new PrintStream(new LogOutputStream(LogLevel.INFO));
        this.customErr = new PrintStream(new LogOutputStream(LogLevel.ERROR));
        System.setOut(this.customOut);
        System.setErr(this.customErr);
        appendToLog("INFO", "日志捕获已启动");
    }

    public void stopCapture() {
        if (!this.isCapturing) {
            return;
        }
        this.isCapturing = false;
        System.setOut(this.originalOut);
        System.setErr(this.originalErr);
        if (this.customOut != null) {
            this.customOut.close();
        }
        if (this.customErr != null) {
            this.customErr.close();
        }
        appendToLog("INFO", "日志捕获已停止");
    }

    public void clearLog() {
        if (this.logTextArea != null) {
            Platform.runLater(() -> {
                this.logTextArea.clear();
                appendToLog("INFO", "日志已清空");
            });
        }
    }

    public void log(String level, String message) {
        appendToLog(level, message);
        this.originalOut.println("[" + this.timeFormat.format(new Date()) + "] [" + level + "] " + message);
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    public void debug(String message) {
        log("DEBUG", message);
    }


    public void appendToLog(String level, String message) {
        if (this.logTextArea == null) {
            return;
        }
        String timestamp = this.timeFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] %s\n", timestamp, level, message);
        Platform.runLater(() -> {
            this.logTextArea.appendText(logEntry);
            this.logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }


    private class LogOutputStream extends OutputStream {
        private LogLevel level;
        private StringBuilder buffer = new StringBuilder();

        public LogOutputStream(LogLevel level) {
            this.level = level;
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) b;
            if (c == '\n') {
                if (this.buffer.length() > 0) {
                    String message = this.buffer.toString().trim();
                    if (!message.isEmpty()) {
                        LogCapture.this.appendToLog(this.level.name(), message);
                        if (this.level == LogLevel.ERROR) {
                            LogCapture.this.originalErr.println(message);
                        } else {
                            LogCapture.this.originalOut.println(message);
                        }
                    }
                    this.buffer.setLength(0);
                    return;
                }
                return;
            }
            if (c != '\r') {
                this.buffer.append(c);
            }
        }

        @Override
        public void flush() throws IOException {
            if (this.buffer.length() > 0) {
                String message = this.buffer.toString().trim();
                if (!message.isEmpty()) {
                    LogCapture.this.appendToLog(this.level.name(), message);
                    if (this.level == LogLevel.ERROR) {
                        LogCapture.this.originalErr.println(message);
                    } else {
                        LogCapture.this.originalOut.println(message);
                    }
                }
                this.buffer.setLength(0);
            }
        }
    }

    public boolean isCapturing() {
        return this.isCapturing;
    }
}
