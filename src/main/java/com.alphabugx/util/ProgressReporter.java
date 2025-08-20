package com.alphabugx.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ProgressReporter {
    private static final ProgressReporter INSTANCE = new ProgressReporter();
    private volatile DisplayMode currentMode = DisplayMode.SIMPLE;
    private volatile String currentMainTask = "";
    private volatile String currentSubTask = "";
    private volatile int mainProgress = 0;
    private volatile int subProgress = 0;
    private volatile int totalFiles = 0;
    private volatile int processedFiles = 0;
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Long> stepTimings = new ConcurrentHashMap<>();
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");


    public enum DisplayMode {
        SIMPLE,
        DETAILED
    }

    private ProgressReporter() {
    }

    public static ProgressReporter getInstance() {
        return INSTANCE;
    }

    public void toggleMode() {
        this.currentMode = this.currentMode == DisplayMode.SIMPLE ? DisplayMode.DETAILED : DisplayMode.SIMPLE;
        this.out.println("\n" + getTimestamp() + " [模式] 切换到" + (this.currentMode == DisplayMode.DETAILED ? "详细模式" : "简洁模式"));
        refreshDisplay();
    }

    public void setDisplayMode(DisplayMode mode) {
        this.currentMode = mode;
        this.out.println("\n" + getTimestamp() + " [模式] 设置为" + (mode == DisplayMode.DETAILED ? "详细模式" : "简洁模式"));
    }

    public void startMainTask(String taskName, int totalSteps) {
        this.currentMainTask = taskName;
        this.mainProgress = 0;
        this.totalFiles = totalSteps;
        this.processedFiles = 0;
        this.startTime.set(System.currentTimeMillis());
        this.successCount.set(0);
        this.failureCount.set(0);
        this.stepTimings.clear();
        this.out.println("\n" + repeatString("=", 60));
        this.out.println(getTimestamp() + " [开始] " + taskName);
        this.out.println(repeatString("=", 60));
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println("总步骤: " + totalSteps);
            this.out.println("显示模式: 详细 (输入 'toggle' 切换到简洁模式)");
        }
        refreshDisplay();
    }

    public void updateMainProgress(int progress, String description) {
        this.mainProgress = progress;
        this.currentSubTask = description;
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [主进度] " + progress + "% - " + description);
        }
        refreshDisplay();
    }

    public void startSubTask(String subTaskName) {
        this.currentSubTask = subTaskName;
        this.subProgress = 0;
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [子任务] " + subTaskName);
        }
        this.stepTimings.put(subTaskName, Long.valueOf(System.currentTimeMillis()));
        refreshDisplay();
    }

    public void updateSubProgress(int progress, String detail) {
        this.subProgress = progress;
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [子进度] " + progress + "% - " + detail);
        }
        refreshDisplay();
    }

    public void completeSubTask(boolean success) {
        if (success) {
            this.successCount.incrementAndGet();
        } else {
            this.failureCount.incrementAndGet();
        }
        this.processedFiles++;
        Long startTime = this.stepTimings.get(this.currentSubTask);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime.longValue();
            if (this.currentMode == DisplayMode.DETAILED) {
                this.out.println(getTimestamp() + " [完成] " + this.currentSubTask + " (" + (success ? "成功" : "失败") + ", 耗时: " + formatDuration(duration) + ")");
            }
        }
        this.subProgress = 100;
        refreshDisplay();
    }

    public void reportFileProgress(String fileName, int fileIndex, int totalFiles, String operation) {
        this.processedFiles = fileIndex;
        this.totalFiles = totalFiles;
        this.currentSubTask = operation + ": " + fileName;
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [文件] [" + fileIndex + "/" + totalFiles + "] " + operation + ": " + fileName);
        }
        refreshDisplay();
    }

    public void reportError(String errorMessage, Exception exception) {
        String errorLog = getTimestamp() + " [错误] " + errorMessage;
        EncodingHelper.safeErrorPrintln(errorLog);
        if (this.currentMode == DisplayMode.DETAILED && exception != null) {
            String detailError = "详细错误: " + exception.getMessage();
            EncodingHelper.safeErrorPrintln(detailError);
            if (exception.getCause() != null) {
                String causeError = "根本原因: " + exception.getCause().getMessage();
                EncodingHelper.safeErrorPrintln(causeError);
            }
        }
        this.failureCount.incrementAndGet();
    }

    public void reportWarning(String warningMessage) {
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [警告] " + warningMessage);
        }
    }

    public void reportInfo(String infoMessage) {
        if (this.currentMode == DisplayMode.DETAILED) {
            this.out.println(getTimestamp() + " [信息] " + infoMessage);
        }
    }

    public void completeMainTask() {
        long totalDuration = System.currentTimeMillis() - this.startTime.get();
        this.out.println("\n" + repeatString("=", 60));
        this.out.println(getTimestamp() + " [完成] " + this.currentMainTask);
        this.out.println("总耗时: " + formatDuration(totalDuration));
        this.out.println("成功: " + this.successCount.get() + ", 失败: " + this.failureCount.get());
        this.out.println(repeatString("=", 60) + "\n");
    }

    private void refreshDisplay() {
        if (this.currentMode == DisplayMode.SIMPLE) {
            StringBuilder sb = new StringBuilder();
            sb.append("\r");
            sb.append("[");
            int filledLength = (this.mainProgress * 30) / 100;
            for (int i = 0; i < 30; i++) {
                if (i < filledLength) {
                    sb.append("█");
                } else {
                    sb.append("░");
                }
            }
            sb.append("] ");
            sb.append(String.format("%3d%%", Integer.valueOf(this.mainProgress)));
            if (this.totalFiles > 0) {
                sb.append(String.format(" [%d/%d]", Integer.valueOf(this.processedFiles), Integer.valueOf(this.totalFiles)));
            }
            if (!this.currentSubTask.isEmpty()) {
                String shortTask = this.currentSubTask.length() > 40 ? this.currentSubTask.substring(0, 37) + "..." : this.currentSubTask;
                sb.append(" " + shortTask);
            }
            sb.append("                    ");
            this.out.print(sb.toString());
            this.out.flush();
        }
    }

    private String getTimestamp() {
        return this.timeFormat.format(new Date());
    }

    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        }
        if (milliseconds < 60000) {
            return String.format("%.1fs", Double.valueOf(milliseconds / 1000.0d));
        }
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return String.format("%dm%ds", Long.valueOf(minutes), Long.valueOf(seconds));
    }

    public String getStatistics() {
        long elapsed = System.currentTimeMillis() - this.startTime.get();
        return String.format("统计 - 成功: %d, 失败: %d, 耗时: %s", Integer.valueOf(this.successCount.get()), Integer.valueOf(this.failureCount.get()), formatDuration(elapsed));
    }

    public DisplayMode getCurrentMode() {
        return this.currentMode;
    }

    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
