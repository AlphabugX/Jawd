package com.alphabugx.jar;

import com.alphabugx.config.AppConfig;
import com.alphabugx.security.SecurityManager;
import com.alphabugx.util.EncodingHelper;
import com.alphabugx.util.ProgressReporter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;


public class JarProcessor {
    private File workDir;
    private File jarFile;
    private File extractDir;
    private File sourceDir;
    private ProgressCallback progressCallback;
    private AppConfig config = AppConfig.getInstance();
    private ProgressReporter progress = ProgressReporter.getInstance();


    public interface ProgressCallback {
        void updateProgress(String str, double d, String str2);
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    private void notifyProgress(String step, double progress, String message) {
        if (this.progressCallback != null) {
            this.progressCallback.updateProgress(step, progress, message);
        }
        System.out.println("[" + step + " " + String.format("%.1f", Double.valueOf(progress)) + "%] " + message);
    }

    public void initWorkspace(File jarFile) throws IOException {
        String tempBasePath;
        this.jarFile = jarFile;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            String tempBasePath2 = System.getenv("TEMP");
            if (tempBasePath2 == null) {
                tempBasePath2 = "C:\\Windows\\Temp";
            }
            tempBasePath = tempBasePath2 + File.separator + "jawd";
        } else {
            tempBasePath = "/tmp/jawd";
        }
        File jawdTempDir = new File(tempBasePath);
        if (!jawdTempDir.exists() && !jawdTempDir.mkdirs()) {
            throw new IOException("无法创建JAWD临时目录: " + jawdTempDir.getAbsolutePath());
        }
        String jarBaseName = jarFile.getName().replaceAll("\\.[^.]+$", "");
        String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
        String randomSuffix = String.format("%06d", Integer.valueOf((int) (Math.random() * 1000000.0d)));
        this.workDir = new File(jawdTempDir, jarBaseName + "_" + timestamp + "_" + randomSuffix);
        this.extractDir = new File(this.workDir, "extracted");
        this.sourceDir = new File(this.workDir, "src/main/java");
        if (!this.sourceDir.mkdirs()) {
            throw new IOException("无法创建工作目录: " + this.sourceDir.getAbsolutePath());
        }
        System.out.println("工作目录初始化完成: " + this.workDir.getAbsolutePath());
    }

    public void extractJar() throws IOException {
        this.progress.startMainTask("解压Jar文件", 100);
        this.progress.updateMainProgress(0, "初始化解压 " + this.jarFile.getName());
        JarFile jar = new JarFile(this.jarFile);
        Throwable th = null;
        try {
            long totalEntries = jar.stream().count();
            this.progress.reportInfo("发现 " + totalEntries + " 个文件/目录");
            AtomicInteger processedEntries = new AtomicInteger(0);
            jar.stream().forEach(entry -> {
                try {
                    int current = processedEntries.incrementAndGet();
                    int progressPercent = (int) ((current * 100.0d) / totalEntries);
                    String fileName = entry.getName();
                    if (fileName.length() > 50) {
                        fileName = "..." + fileName.substring(fileName.length() - 47);
                    }
                    this.progress.updateMainProgress(progressPercent, "解压: " + fileName);
                    File targetFile = new File(this.extractDir, entry.getName());
                    if (entry.isDirectory()) {
                        targetFile.mkdirs();
                        this.progress.reportInfo("创建目录: " + entry.getName());
                    } else {
                        try {
                            targetFile.getParentFile().mkdirs();
                            InputStream inputStream = jar.getInputStream(entry);
                            Throwable th2 = null;
                            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
                            Throwable th3 = null;
                            try {
                                byte[] bArr = new byte[8192];
                                long j = 0;
                                while (true) {
                                    int i = inputStream.read(bArr);
                                    if (i <= 0) {
                                        break;
                                    }
                                    fileOutputStream.write(bArr, 0, i);
                                    j += i;
                                }
                                if (j > 1048576) {
                                    this.progress.reportInfo("解压大文件: " + entry.getName() + " (" + formatFileSize(j) + ")");
                                }
                                if (fileOutputStream != null) {
                                    if (0 != 0) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (Throwable th4) {
                                            th3.addSuppressed(th4);
                                        }
                                    } else {
                                        fileOutputStream.close();
                                    }
                                }
                                if (inputStream != null) {
                                    if (0 != 0) {
                                        try {
                                            inputStream.close();
                                        } catch (Throwable th5) {
                                            th2.addSuppressed(th5);
                                        }
                                    } else {
                                        inputStream.close();
                                    }
                                }
                            } catch (Throwable th6) {
                                if (fileOutputStream != null) {
                                    if (0 != 0) {
                                        try {
                                            fileOutputStream.close();
                                        } catch (Throwable th7) {
                                            th3.addSuppressed(th7);
                                        }
                                    } else {
                                        fileOutputStream.close();
                                    }
                                }
                                throw th6;
                            }
                        } finally {
                        }
                    }
                } catch (IOException e) {
                    this.progress.reportError("解压文件失败: " + entry.getName(), e);
                    throw new RuntimeException("解压文件失败: " + entry.getName(), e);
                }
            });
            if (jar != null) {
                if (0 != 0) {
                    try {
                        jar.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                } else {
                    jar.close();
                }
            }
            this.progress.updateMainProgress(100, "解压完成");
            this.progress.completeMainTask();
        } catch (Throwable th3) {
            if (jar != null) {
                if (0 != 0) {
                    try {
                        jar.close();
                    } catch (Throwable th4) {
                        th.addSuppressed(th4);
                    }
                } else {
                    jar.close();
                }
            }
            throw th3;
        }
    }

    public void copyClassFiles() throws IOException {
        System.out.println("复制Class文件到源码目录");
        File classesDir = new File(this.extractDir, "BOOT-INF/classes");
        if (!classesDir.exists()) {
            classesDir = this.extractDir;
        }
        if (classesDir.exists()) {
            copyDirectory(classesDir, this.sourceDir);
            System.out.println("Class文件复制完成");
            return;
        }
        throw new IOException("未找到Class文件目录");
    }

    public void decompileClasses() throws ExecutionException, InterruptedException, IOException {
        this.progress.startMainTask("反编译Class文件", 100);
        this.progress.updateMainProgress(0, "初始化反编译环境");
        String cfrPath = this.config.getCfrPath();
        String javaExe = this.config.getJavaExecutable();
        if (!new File(cfrPath).exists()) {
            this.progress.reportError("CFR工具不存在: " + cfrPath, null);
            throw new IOException("CFR工具不存在: " + cfrPath);
        }
        this.progress.updateMainProgress(10, "查找Class文件");
        List<File> classFiles = findClassFiles(this.sourceDir);
        this.progress.reportInfo("找到 " + classFiles.size() + " 个Class文件需要反编译");
        if (classFiles.isEmpty()) {
            this.progress.updateMainProgress(100, "没有找到Class文件");
            this.progress.completeMainTask();
            return;
        }
        int threadCount = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors()), 8);
        this.progress.reportInfo("使用 " + threadCount + " 个线程进行并行反编译");
        this.progress.updateMainProgress(15, "启动多线程反编译");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();
        for (File classFile : classFiles) {
            Future<Void> future = executor.submit(() -> {
                try {
                    int current = processedCount.incrementAndGet();
                    int progressPercent = 15 + ((int) ((current * 80.0d) / classFiles.size()));
                    String fileName = classFile.getName();
                    if (fileName.length() > 40) {
                        fileName = "..." + fileName.substring(fileName.length() - 37);
                    }
                    this.progress.reportFileProgress(fileName, current, classFiles.size(), "反编译");
                    this.progress.updateMainProgress(progressPercent, "反编译: " + fileName);
                    double callbackProgress = 30.0d + ((current * 40.0d) / classFiles.size());
                    notifyProgress("反编译", callbackProgress, "反编译: " + classFile.getName() + " (" + current + "/" + classFiles.size() + ")");
                    decompileClassFile(classFile, javaExe, cfrPath);
                    successCount.incrementAndGet();
                    if (current % 50 == 0 || current == classFiles.size()) {
                        this.progress.reportInfo("反编译统计: 成功 " + successCount.get() + ", 失败 " + failureCount.get() + " (总进度: " + current + "/" + classFiles.size() + ")");
                    }
                    return null;
                } catch (Exception e) {
                    this.progress.reportError("反编译失败: " + classFile.getName(), e);
                    failureCount.incrementAndGet();
                    return null;
                }
            });
            futures.add(future);
        }
        for (Future<Void> future2 : futures) {
            try {
                future2.get();
            } catch (ExecutionException e) {
                System.err.println("反编译任务执行异常: " + e.getCause().getMessage());
            }
        }
        executor.shutdown();
        if (!executor.awaitTermination(30L, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        this.progress.updateMainProgress(100, "反编译完成");
        this.progress.reportInfo("反编译最终统计: 成功 " + successCount.get() + ", 失败 " + failureCount.get());
        this.progress.completeMainTask();
        if (successCount.get() == 0) {
            this.progress.reportError("所有Class文件反编译都失败了，请检查CFR工具配置", null);
            throw new IOException("所有Class文件反编译都失败了，请检查CFR工具配置");
        }
    }

    private void decompileClassFile(File classFile, String javaExe, String cfrPath) throws InterruptedException, IOException {
        String classDir = classFile.getParent();
        String className = classFile.getName().replace(".class", "");
        File javaFile = new File(classDir, className + ".java");
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-jar", cfrPath, "--silent", "true", "--renamedupmembers", "false", "--renameillegalidents", "false", "--renamesmallmembers", "0", "--lenient", "true", "--recover", "true", "--allowcorrecting", "true", classFile.getAbsolutePath());
        pb.directory(new File("."));
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Throwable th = null;
        try {
            FileWriter writer = new FileWriter(javaFile);
            Throwable th2 = null;
            boolean skipCfrComment = true;
            while (true) {
                try {
                    try {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        String trimmedLine = line.trim();
                        if (!skipCfrComment) {
                            writer.write(line + System.lineSeparator());
                        } else if (trimmedLine.startsWith("package ") || trimmedLine.startsWith("import ") || trimmedLine.startsWith("public class ") || trimmedLine.startsWith("class ") || trimmedLine.startsWith("public interface ") || trimmedLine.startsWith("interface ") || trimmedLine.startsWith("@")) {
                            skipCfrComment = false;
                            writer.write(line + System.lineSeparator());
                        } else if (!trimmedLine.startsWith("/*") && !trimmedLine.startsWith("*") && !trimmedLine.startsWith("*/") && !trimmedLine.isEmpty() && !trimmedLine.startsWith("//")) {
                            skipCfrComment = false;
                            writer.write(line + System.lineSeparator());
                        }
                    } catch (Throwable th3) {
                        if (writer != null) {
                            if (th2 != null) {
                                try {
                                    writer.close();
                                } catch (Throwable th4) {
                                    th2.addSuppressed(th4);
                                }
                            } else {
                                writer.close();
                            }
                        }
                        throw th3;
                    }
                } finally {
                }
            }
            if (writer != null) {
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (Throwable th5) {
                        th2.addSuppressed(th5);
                    }
                } else {
                    writer.close();
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return;
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            Throwable th6 = null;
            try {
                try {
                    StringBuilder errorMsg = new StringBuilder();
                    while (true) {
                        String errorLine = errorReader.readLine();
                        if (errorLine == null) {
                            throw new RuntimeException("CFR反编译失败: " + errorMsg.toString());
                        }
                        errorMsg.append(errorLine).append("\n");
                    }
                } catch (Throwable th7) {
                    if (errorReader != null) {
                        if (th6 != null) {
                            try {
                                errorReader.close();
                            } catch (Throwable th8) {
                                th6.addSuppressed(th8);
                            }
                        } else {
                            errorReader.close();
                        }
                    }
                    throw th7;
                }
            } finally {
            }
        } finally {
            if (reader != null) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (Throwable th9) {
                        th.addSuppressed(th9);
                    }
                } else {
                    reader.close();
                }
            }
        }
    }

    public void compileJavaFiles(List<File> javaFiles) throws InterruptedException, IOException {
        if (!SecurityManager.isSecureEnvironment()) {
            throw new SecurityException(SecurityManager.getEncryptedErrorMessage("permission_error"));
        }
        if (javaFiles.isEmpty()) {
            System.out.println("没有需要编译的Java文件");
            return;
        }
        EncodingHelper.safePrintln("开始编译Java文件，初始文件数: " + javaFiles.size() + " 个");
        Set<File> allFilesToCompile = new HashSet<>(javaFiles);
        boolean compilationSuccessful = false;
        for (int attempt = 1; attempt <= 3; attempt++) {
            EncodingHelper.safePrintln("编译尝试 " + attempt + "/3 (共 " + allFilesToCompile.size() + " 个文件)");
            try {
                compileFilesInternal(new ArrayList(allFilesToCompile));
                System.out.println("Java文件编译完成，共编译 " + allFilesToCompile.size() + " 个文件");
                compilationSuccessful = true;
                break;
            } catch (CompilationException e) {
                if (attempt < 3) {
                    System.out.println("编译失败，分析依赖关系...");
                    boolean fixedAny = autoFixCommonErrors(e.getErrorOutput(), new ArrayList(allFilesToCompile));
                    Set<File> additionalFiles = findDependentFiles(e.getErrorOutput());
                    if (!additionalFiles.isEmpty()) {
                        allFilesToCompile.addAll(additionalFiles);
                        System.out.println("发现依赖文件，添加 " + additionalFiles.size() + " 个文件到编译列表 (总共 " + allFilesToCompile.size() + " 个文件)");
                    } else if (!fixedAny) {
                        throw new RuntimeException("Java编译失败: " + e.getErrorOutput());
                    }
                } else {
                    throw new RuntimeException("Java编译失败，已达到最大重试次数: " + e.getErrorOutput());
                }
            }
        }
        if (compilationSuccessful) {
            System.out.println("Java文件编译完成，共编译 " + allFilesToCompile.size() + " 个文件");
        }
    }

    private void compileFilesInternal(List<File> javaFiles) throws InterruptedException, CompilationException, IOException {
        String javacExe = this.config.getJavacExecutable();
        File compiledClassesDir = new File(this.workDir, "compiled_classes");
        if (compiledClassesDir.exists()) {
            deleteDirectory(compiledClassesDir);
        }
        compiledClassesDir.mkdirs();
        File classpathFile = new File(this.workDir, "cp.txt");
        String classpath = buildClasspath();
        PrintWriter writer = new PrintWriter(classpathFile, "UTF-8");
        Throwable th = null;
        try {
            writer.println(classpath);
            if (writer != null) {
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                } else {
                    writer.close();
                }
            }
            File sourceListFile = new File(this.workDir, "sources.txt");
            PrintWriter writer2 = new PrintWriter(sourceListFile, "UTF-8");
            Throwable th3 = null;
            try {
                try {
                    for (File javaFile : javaFiles) {
                        try {
                            Path relativePath = this.workDir.toPath().relativize(javaFile.toPath());
                            writer2.println(relativePath.toString());
                        } catch (Exception e) {
                            writer2.println(javaFile.getAbsolutePath());
                        }
                    }
                    if (writer2 != null) {
                        if (0 != 0) {
                            try {
                                writer2.close();
                            } catch (Throwable th4) {
                                th3.addSuppressed(th4);
                            }
                        } else {
                            writer2.close();
                        }
                    }
                    List<String> command = new ArrayList<>();
                    command.add(javacExe);
                    command.add("-cp");
                    command.add("@" + classpathFile.getAbsolutePath());
                    command.add("-d");
                    command.add(compiledClassesDir.getAbsolutePath());
                    command.add("-encoding");
                    command.add("UTF-8");
                    command.add("-g");
                    command.add("-source");
                    command.add("1.8");
                    command.add("-target");
                    command.add("1.8");
                    command.add("@" + sourceListFile.getAbsolutePath());
                    EncodingHelper.safePrintln("执行编译命令: " + String.join(" ", command));
                    EncodingHelper.safePrintln("编译输出目录: " + compiledClassesDir.getAbsolutePath());
                    EncodingHelper.safePrintln("使用classpath文件: " + classpathFile.getAbsolutePath());
                    EncodingHelper.safePrintln("使用源文件列表: " + sourceListFile.getAbsolutePath());
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.directory(this.workDir);
                    EncodingHelper.setupProcessBuilderEncoding(pb);
                    Process process = pb.start();
                    StringBuilder errorOutput = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), EncodingHelper.CONSOLE_CHARSET));
                    Throwable th5 = null;
                    try {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), EncodingHelper.CONSOLE_CHARSET));
                        Throwable th6 = null;
                        while (true) {
                            try {
                                try {
                                    String line = reader.readLine();
                                    if (line == null) {
                                        break;
                                    } else {
                                        EncodingHelper.safePrintln(line);
                                    }
                                } catch (Throwable th7) {
                                    if (errorReader != null) {
                                        if (th6 != null) {
                                            try {
                                                errorReader.close();
                                            } catch (Throwable th8) {
                                                th6.addSuppressed(th8);
                                            }
                                        } else {
                                            errorReader.close();
                                        }
                                    }
                                    throw th7;
                                }
                            } finally {
                            }
                        }
                        while (true) {
                            String line2 = errorReader.readLine();
                            if (line2 == null) {
                                break;
                            }
                            EncodingHelper.safeErrorPrintln(line2);
                            errorOutput.append(line2).append("\n");
                        }
                        if (errorReader != null) {
                            if (0 != 0) {
                                try {
                                    errorReader.close();
                                } catch (Throwable th9) {
                                    th6.addSuppressed(th9);
                                }
                            } else {
                                errorReader.close();
                            }
                        }
                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            throw new CompilationException("编译失败，退出码: " + exitCode, errorOutput.toString());
                        }
                    } finally {
                        if (reader != null) {
                            if (0 != 0) {
                                try {
                                    reader.close();
                                } catch (Throwable th10) {
                                    th5.addSuppressed(th10);
                                }
                            } else {
                                reader.close();
                            }
                        }
                    }
                } catch (Throwable th11) {
                    if (writer2 != null) {
                        if (th3 != null) {
                            try {
                                writer2.close();
                            } catch (Throwable th12) {
                                th3.addSuppressed(th12);
                            }
                        } else {
                            writer2.close();
                        }
                    }
                    throw th11;
                }
            } finally {
            }
        } catch (Throwable th13) {
            if (writer != null) {
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (Throwable th14) {
                        th.addSuppressed(th14);
                    }
                } else {
                    writer.close();
                }
            }
            throw th13;
        }
    }

    private boolean autoFixCommonErrors(String errorOutput, List<File> filesToCompile) {
        boolean fixedAny = false;
        if (errorOutput.contains("SpelExpressionParser") && errorOutput.contains("FileHandler")) {
            System.out.println("检测到SpelExpressionParser类型转换错误，尝试自动修复...");
            fixedAny = false | fixSpelExpressionParserError(filesToCompile);
        }
        if (errorOutput.contains("未终止的注释") || errorOutput.contains("到达文件结尾") || errorOutput.contains("unterminated comment") || errorOutput.contains("reached end of file") || ((errorOutput.contains("/*") && errorOutput.contains("错误")) || (errorOutput.contains("*/") && errorOutput.contains("错误")))) {
            System.out.println("检测到注释格式错误，尝试自动修复...");
            fixedAny |= fixCommentFormatError(filesToCompile);
        }
        if (!fixedAny && (errorOutput.contains("/*") || errorOutput.contains("文件结尾"))) {
            System.out.println("检测到可能的文件格式错误，尝试通用修复...");
            fixedAny |= fixGeneralFormatErrors(filesToCompile);
        }
        return fixedAny;
    }

    private boolean fixSpelExpressionParserError(List<File> filesToCompile) {
        boolean fixed = false;
        for (File javaFile : filesToCompile) {
            if (javaFile.getName().equals("UserRealm.java")) {
                try {
                    String content = readFileContent(javaFile);
                    String[] lines = content.split("\n");
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        String trimmedLine = line.trim();
                        if (trimmedLine.contains("fileHandler = new SpelExpressionParser()") || trimmedLine.contains("Expression expression = fileHandler.parseExpression") || trimmedLine.contains("expression.getValue(") || (trimmedLine.startsWith("private") && trimmedLine.contains("FileHandler") && trimmedLine.contains("fileHandler"))) {
                            result.append(line.replaceFirst("^(\\s*)", "$1// ")).append("\n");
                        } else if (trimmedLine.equals("return bl;")) {
                            String indentation = line.substring(0, line.indexOf("return"));
                            result.append(indentation).append("return false; // 修复：bl变量未定义").append("\n");
                            fixed = true;
                        } else if (trimmedLine.matches("return [a-z]{1,3};")) {
                            String varName = trimmedLine.substring(7, trimmedLine.length() - 1);
                            String indentation2 = line.substring(0, line.indexOf("return"));
                            String replacement = inferReturnValue(lines, i);
                            result.append(indentation2).append("return ").append(replacement).append("; // 修复：").append(varName).append("变量未定义").append("\n");
                            fixed = true;
                        } else if (trimmedLine.matches(".*\\s[a-z]{1,3}\\s*=.*") && !trimmedLine.contains("=")) {
                            result.append(line.replaceFirst("^(\\s*)", "$1// ")).append(" // 注释：可能的变量问题").append("\n");
                            fixed = true;
                        } else {
                            result.append(line).append("\n");
                        }
                    }
                    String content2 = result.toString();
                    if (!content2.equals(content)) {
                        writeFileContent(javaFile, content2);
                        System.out.println("已自动修复 " + javaFile.getName() + " 中的类型转换错误 (注释了错误的SpelExpressionParser代码)");
                        fixed = true;
                    }
                } catch (Exception e) {
                    System.err.println("修复 " + javaFile.getName() + " 时出错: " + e.getMessage());
                }
            }
        }
        return fixed;
    }

    private String inferReturnValue(String[] lines, int currentLineIndex) {
        for (int i = currentLineIndex - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.contains("(") && line.contains(")") && (line.contains("public") || line.contains("private") || line.contains("protected"))) {
                if (line.contains("boolean")) {
                    return "false";
                }
                if (line.contains("int") || line.contains("Integer")) {
                    return "0";
                }
                if (line.contains("String")) {
                    return "null";
                }
                if (line.contains("void")) {
                    return "";
                }
                return "false";
            }
            if (line.equals("}") && i < currentLineIndex - 10) {
                return "false";
            }
        }
        return "false";
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Throwable th = null;
        while (true) {
            try {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    content.append(line).append(System.lineSeparator());
                } finally {
                }
            } catch (Throwable th2) {
                if (reader != null) {
                    if (th != null) {
                        try {
                            reader.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        reader.close();
                    }
                }
                throw th2;
            }
        }
        if (reader != null) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                }
            } else {
                reader.close();
            }
        }
        return content.toString();
    }

    private void writeFileContent(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        Throwable th = null;
        try {
            try {
                writer.write(content);
                if (writer != null) {
                    if (0 != 0) {
                        try {
                            writer.close();
                            return;
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                            return;
                        }
                    }
                    writer.close();
                }
            } catch (Throwable th3) {
                th = th3;
                throw th3;
            }
        } catch (Throwable th4) {
            if (writer != null) {
                if (th != null) {
                    try {
                        writer.close();
                    } catch (Throwable th5) {
                        th.addSuppressed(th5);
                    }
                } else {
                    writer.close();
                }
            }
            throw th4;
        }
    }

    private boolean fixCommentFormatError(List<File> filesToCompile) {
        boolean fixed = false;
        for (File javaFile : filesToCompile) {
            if (javaFile.getName().endsWith(".java")) {
                try {
                    String content = readFileContent(javaFile);
                    String content2 = removeBrokenComments(fixUnterminatedComments(content));
                    if (!content2.equals(content)) {
                        writeFileContent(javaFile, content2);
                        System.out.println("已自动修复 " + javaFile.getName() + " 中的注释格式错误");
                        fixed = true;
                    }
                } catch (Exception e) {
                    System.err.println("修复 " + javaFile.getName() + " 注释错误时出错: " + e.getMessage());
                }
            }
        }
        return fixed;
    }

    private String fixUnterminatedComments(String content) {
        int openCount = 0;
        int closeCount = 0;
        int pos = 0;
        while (true) {
            int pos2 = content.indexOf("/*", pos);
            if (pos2 == -1) {
                break;
            }
            openCount++;
            pos = pos2 + 2;
        }
        int pos3 = 0;
        while (true) {
            int pos4 = content.indexOf("*/", pos3);
            if (pos4 == -1) {
                break;
            }
            closeCount++;
            pos3 = pos4 + 2;
        }
        if (openCount > closeCount) {
            content = content.trim();
            if (!content.endsWith("*/")) {
                content = content + "\n */\n";
            }
        }
        return content;
    }

    private String removeBrokenComments(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inComment = false;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("/*") && !trimmedLine.contains("*/")) {
                inComment = true;
                if (!trimmedLine.equals("/*") && !trimmedLine.equals("/**")) {
                    String contentAfterComment = line.substring(line.indexOf("/*") + 2).trim();
                    if (!contentAfterComment.isEmpty()) {
                        result.append("// " + contentAfterComment).append("\n");
                    }
                }
            } else if (inComment) {
                if (trimmedLine.contains("*/")) {
                    inComment = false;
                    int endIndex = line.indexOf("*/");
                    if (endIndex + 2 < line.length()) {
                        String remainingContent = line.substring(endIndex + 2).trim();
                        if (!remainingContent.isEmpty()) {
                            result.append(remainingContent).append("\n");
                        }
                    }
                }
            } else if (!trimmedLine.equals("*/") && !trimmedLine.equals("**/")) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private boolean fixGeneralFormatErrors(List<File> filesToCompile) {
        boolean fixed = false;
        for (File javaFile : filesToCompile) {
            if (javaFile.getName().endsWith(".java")) {
                try {
                    String content = readFileContent(javaFile);
                    String content2 = removeBrokenComments(fixUnterminatedComments(content));
                    if (!content2.endsWith("\n")) {
                        content2 = content2 + "\n";
                    }
                    String content3 = cleanupFileEnd(cleanupFileStart(content2));
                    if (!content3.equals(content)) {
                        writeFileContent(javaFile, content3);
                        System.out.println("已应用通用修复到 " + javaFile.getName());
                        fixed = true;
                    }
                } catch (Exception e) {
                    System.err.println("通用修复 " + javaFile.getName() + " 时出错: " + e.getMessage());
                }
            }
        }
        return fixed;
    }

    private String cleanupFileStart(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundValidContent = false;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!foundValidContent) {
                if (!trimmedLine.equals("/*") && !trimmedLine.equals("/**") && !trimmedLine.equals("*/") && !trimmedLine.equals("**/") && !trimmedLine.isEmpty()) {
                    foundValidContent = true;
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private String cleanupFileEnd(String content) {
        String[] lines = content.split("\n");
        int lastValidLine = lines.length - 1;
        for (int i = lines.length - 1; i >= 0; i--) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.equals("/*") || trimmedLine.equals("/**") || trimmedLine.equals("*/") || trimmedLine.equals("**/")) {
                lastValidLine = i - 1;
            } else if (!trimmedLine.isEmpty()) {
                break;
            }
        }
        StringBuilder result = new StringBuilder();
        for (int i2 = 0; i2 <= lastValidLine && i2 < lines.length; i2++) {
            result.append(lines[i2]).append("\n");
        }
        String finalContent = result.toString().trim();
        if (!finalContent.isEmpty() && !finalContent.endsWith("}")) {
            result.append("}\n");
        }
        return result.toString();
    }

    private Set<File> findDependentFiles(String errorOutput) {
        String className;
        String className2;
        String className3;
        String className4;
        Set<File> dependentFiles = new HashSet<>();
        Set<String> missingClasses = new HashSet<>();
        String[] lines = errorOutput.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("import ") && (className4 = extractClassNameFromImport(line)) != null) {
                missingClasses.add(className4);
            }
            if (line.contains("符号:   类 ") && (className3 = extractClassNameFromErrorMessage(line)) != null) {
                missingClasses.add(className3);
            }
            if (line.contains("symbol:   class ") && (className2 = extractClassNameFromErrorMessage(line)) != null) {
                missingClasses.add(className2);
            }
            if (line.contains("找不到符号") || line.contains("cannot find symbol")) {
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    String nextLine = lines[j];
                    if ((nextLine.contains("符号:") || nextLine.contains("symbol:")) && (className = extractSymbolFromLine(nextLine)) != null) {
                        missingClasses.add(className);
                    }
                }
            }
        }
        for (String className5 : missingClasses) {
            File javaFile = findJavaFileByClassName(className5);
            if (javaFile != null) {
                dependentFiles.add(javaFile);
                System.out.println("找到依赖文件: " + javaFile.getName() + " (类名: " + className5 + ")");
            } else {
                System.out.println("未找到依赖文件: " + className5 + ".java");
            }
        }
        if (dependentFiles.isEmpty()) {
            dependentFiles.addAll(findCommonDependencies());
        }
        return dependentFiles;
    }

    private String extractSymbolFromLine(String line) {
        try {
            if (line.contains("符号:") && line.contains("类 ")) {
                int classIndex = line.indexOf("类 ") + 2;
                String remaining = line.substring(classIndex).trim();
                int spaceIndex = remaining.indexOf(32);
                if (spaceIndex > 0) {
                    return remaining.substring(0, spaceIndex);
                }
                return remaining;
            }
            if (line.contains("symbol:") && line.contains("class ")) {
                int classIndex2 = line.indexOf("class ") + 6;
                String remaining2 = line.substring(classIndex2).trim();
                int spaceIndex2 = remaining2.indexOf(32);
                if (spaceIndex2 > 0) {
                    return remaining2.substring(0, spaceIndex2);
                }
                return remaining2;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Set<File> findCommonDependencies() {
        Set<File> commonDeps = new HashSet<>();
        List<File> allJavaFiles = findAllJavaFiles(this.sourceDir);
        int maxCommonDeps = Math.min(5, allJavaFiles.size());
        for (int i = 0; i < maxCommonDeps; i++) {
            commonDeps.add(allJavaFiles.get(i));
        }
        return commonDeps;
    }

    private List<File> findAllJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursive(directory, javaFiles);
        return javaFiles;
    }

    private void findJavaFilesRecursive(File directory, List<File> javaFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findJavaFilesRecursive(file, javaFiles);
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
    }

    private String extractClassNameFromImport(String importLine) {
        try {
            String trimmed = importLine.trim();
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {
                String fullClassName = trimmed.substring(7, trimmed.length() - 1);
                return fullClassName.substring(fullClassName.lastIndexOf(46) + 1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractClassNameFromErrorMessage(String errorLine) {
        try {
            if (errorLine.contains("符号:   类 ")) {
                int start = errorLine.indexOf("符号:   类 ") + 6;
                int end = errorLine.indexOf(" ", start);
                if (end == -1) {
                    end = errorLine.length();
                }
                return errorLine.substring(start, end).trim();
            }
            if (errorLine.contains("symbol:   class ")) {
                int start2 = errorLine.indexOf("symbol:   class ") + 16;
                int end2 = errorLine.indexOf(" ", start2);
                if (end2 == -1) {
                    end2 = errorLine.length();
                }
                return errorLine.substring(start2, end2).trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private File findJavaFileByClassName(String className) {
        return findJavaFileRecursive(this.sourceDir, className + ".java");
    }

    private File findJavaFileRecursive(File directory, String fileName) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findJavaFileRecursive(file, fileName);
                    if (found != null) {
                        return found;
                    }
                } else if (file.getName().equals(fileName)) {
                    return file;
                }
            }
            return null;
        }
        return null;
    }


    private static class CompilationException extends Exception {
        private final String errorOutput;

        public CompilationException(String message, String errorOutput) {
            super(message);
            this.errorOutput = errorOutput;
        }

        public String getErrorOutput() {
            return this.errorOutput;
        }
    }

    private String buildClasspath() throws IOException {
        File[] jarFiles;
        StringBuilder classpath = new StringBuilder();
        classpath.append(".");
        File libDir = new File(this.extractDir, "BOOT-INF/lib");
        if (!libDir.exists()) {
            libDir = new File(this.extractDir, "lib");
        }
        if (libDir.exists() && (jarFiles = libDir.listFiles((dir, name) -> {
            return name.endsWith(".jar");
        })) != null) {
            int maxJars = Math.min(jarFiles.length, 1000);
            int count = 0;
            Arrays.sort(jarFiles, (f1, f2) -> {
                String name1 = f1.getName().toLowerCase();
                String name2 = f2.getName().toLowerCase();
                int priority1 = getJarPriority(name1);
                int priority2 = getJarPriority(name2);
                if (priority1 != priority2) {
                    return Integer.compare(priority2, priority1);
                }
                return Long.compare(f2.length(), f1.length());
            });
            int length = jarFiles.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                File jarFile = jarFiles[i];
                if (count >= maxJars) {
                    String warningMsg = "警告: 为避免classpath过长，跳过了 " + (jarFiles.length - maxJars) + " 个低优先级jar文件";
                    EncodingHelper.safePrintln(warningMsg);
                    EncodingHelper.safePrintln("提示: 如果编译失败，可能需要手动添加缺失的依赖");
                    break;
                }
                try {
                    Path relativePath = this.workDir.toPath().relativize(jarFile.toPath());
                    String pathToAdd = relativePath.toString();
                    if (pathToAdd.length() > 100) {
                        File copiedJar = new File(this.workDir, jarFile.getName());
                        if (!copiedJar.exists()) {
                            Files.copy(jarFile.toPath(), copiedJar.toPath(), new CopyOption[0]);
                        }
                        pathToAdd = copiedJar.getName();
                    }
                    classpath.append(File.pathSeparator).append(pathToAdd);
                    count++;
                } catch (Exception e) {
                    classpath.append(File.pathSeparator).append(jarFile.getAbsolutePath());
                    count++;
                }
                i++;
            }
        }
        return classpath.toString();
    }

    private int getJarPriority(String jarName) {
        if (jarName.contains("spring") || jarName.contains("hibernate") || jarName.contains("mybatis") || jarName.contains("servlet")) {
            return 100;
        }
        if (jarName.contains("apache") || jarName.contains("commons") || jarName.contains("tomcat") || jarName.contains("maven")) {
            return 80;
        }
        if (jarName.contains("javax") || jarName.contains("jakarta") || jarName.contains("javaee") || jarName.contains("j2ee")) {
            return 75;
        }
        if (jarName.contains("log") || jarName.contains("slf4j") || jarName.contains("jackson") || jarName.contains("gson") || jarName.contains("fastjson") || jarName.contains("guava")) {
            return 60;
        }
        if (jarName.contains("mysql") || jarName.contains("oracle") || jarName.contains("postgresql") || jarName.contains("h2") || jarName.contains("derby") || jarName.contains("sqlite")) {
            return 55;
        }
        if (jarName.contains("test") || jarName.contains("junit") || jarName.contains("mockito") || jarName.contains("hamcrest")) {
            return 30;
        }
        if (jarName.contains("doc") || jarName.contains("sample") || jarName.contains("example") || jarName.contains("demo")) {
            return 10;
        }
        return 50;
    }

    public File repackageJar(String outputFileName) throws InterruptedException, IOException {
        SecurityManager.performRuntimeCheck();
        System.out.println("开始重新打包Jar文件，按照标准流程...");
        notifyProgress("打包", 0.0d, "开始重新打包Jar文件");
        long originalSize = this.jarFile.length();
        System.out.println("原始Jar文件大小: " + formatFileSize(originalSize));
        notifyProgress("打包", 10.0d, "替换编译后的class文件");
        replaceCompiledClasses();
        notifyProgress("打包", 20.0d, "class文件替换完成");
        File libDir = new File(this.extractDir, "BOOT-INF/lib");
        if (libDir.exists()) {
            notifyProgress("打包", 25.0d, "检测到Spring Boot应用，分析嵌套Jar文件");
            boolean needProcessNestedJars = checkIfNestedJarsNeedProcessing(libDir);
            if (needProcessNestedJars) {
                System.out.println("检测到需要处理嵌套Jar文件，开始处理...");
                processNestedJarsOptimized(libDir);
                notifyProgress("打包", 80.0d, "嵌套Jar文件处理完成");
            } else {
                System.out.println("嵌套Jar文件无需修改，跳过处理以提升性能");
                notifyProgress("打包", 80.0d, "嵌套Jar文件无需处理，已跳过");
            }
        } else {
            notifyProgress("打包", 80.0d, "无嵌套Jar文件，跳过处理");
        }
        notifyProgress("打包", 85.0d, "开始重新打包主Jar文件");
        File outputJar = new File(outputFileName);
        repackageWithPrecision(outputJar);
        notifyProgress("打包", 95.0d, "验证Spring Boot兼容性");
        if (isSpringBootJar(outputJar)) {
            validateSpringBootCompatibility(outputJar);
            System.out.println("Spring Boot兼容性验证通过");
        }
        long newSize = outputJar.length();
        System.out.println("新Jar文件大小: " + formatFileSize(newSize));
        System.out.println("大小变化: " + formatFileSize(newSize - originalSize));
        notifyProgress("打包", 100.0d, "Jar文件重新打包完成");
        System.out.println("Jar文件重新打包完成: " + outputJar.getAbsolutePath());
        return outputJar;
    }

    private void repackageWithPrecision(File outputJar) throws InterruptedException, IOException {
        if (isSpringBootJar(this.jarFile)) {
            System.out.println("检测到Spring Boot应用，使用专门的Spring Boot兼容打包方法");
            repackageSpringBootJar(outputJar);
        } else {
            System.out.println("使用标准jar打包方法");
            repackageStandardJar(outputJar);
        }
    }

    private void repackageSpringBootJar(File outputJar) throws InterruptedException, IOException {
        String jarExe = this.config.getJarExecutable();
        File manifestFile = new File(this.extractDir, "META-INF/MANIFEST.MF");
        List<String> command = new ArrayList<>();
        command.add(jarExe);
        if (manifestFile.exists()) {
            command.add("-c0fm");
            command.add(outputJar.getAbsolutePath());
            command.add(manifestFile.getAbsolutePath());
        } else {
            command.add("-c0f");
            command.add(outputJar.getAbsolutePath());
        }
        command.add("-C");
        command.add(this.extractDir.getAbsolutePath());
        command.add(".");
        notifyProgress("打包", 90.0d, "执行Spring Boot兼容打包命令: " + outputJar.getName());
        System.out.println("执行Spring Boot兼容打包命令: " + String.join(" ", command));
        System.out.println("关键：使用-0参数确保嵌套jar文件不压缩，符合Spring Boot要求");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(this.workDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Throwable th = null;
        int lineCount = 0;
        while (true) {
            try {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    lineCount++;
                    if (lineCount <= 5 || lineCount % 1000 == 0) {
                        System.out.println(line);
                    }
                } finally {
                }
            } catch (Throwable th2) {
                if (reader != null) {
                    if (th != null) {
                        try {
                            reader.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        reader.close();
                    }
                }
                throw th2;
            }
        }
        if (lineCount > 5) {
            System.out.println("... (共处理 " + lineCount + " 个文件)");
        }
        if (reader != null) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                }
            } else {
                reader.close();
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Spring Boot jar重新打包失败，退出码: " + exitCode);
        }
        System.out.println("Spring Boot兼容打包完成，嵌套jar文件已正确存储（未压缩）");
    }

    private void repackageStandardJar(File outputJar) throws InterruptedException, IOException {
        String jarExe = this.config.getJarExecutable();
        File manifestFile = new File(this.extractDir, "META-INF/MANIFEST.MF");
        List<String> command = new ArrayList<>();
        command.add(jarExe);
        if (manifestFile.exists()) {
            command.add("-cfm");
            command.add(outputJar.getAbsolutePath());
            command.add(manifestFile.getAbsolutePath());
        } else {
            command.add("-cf");
            command.add(outputJar.getAbsolutePath());
        }
        command.add("-C");
        command.add(this.extractDir.getAbsolutePath());
        command.add(".");
        notifyProgress("打包", 90.0d, "执行标准jar打包命令: " + outputJar.getName());
        System.out.println("执行标准jar打包命令: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(this.workDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Throwable th = null;
        int lineCount = 0;
        while (true) {
            try {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    lineCount++;
                    if (lineCount <= 5 || lineCount % 1000 == 0) {
                        System.out.println(line);
                    }
                } finally {
                }
            } catch (Throwable th2) {
                if (reader != null) {
                    if (th != null) {
                        try {
                            reader.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        reader.close();
                    }
                }
                throw th2;
            }
        }
        if (lineCount > 5) {
            System.out.println("... (共处理 " + lineCount + " 个文件)");
        }
        if (reader != null) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                }
            } else {
                reader.close();
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("标准jar重新打包失败，退出码: " + exitCode);
        }
    }

    private String formatFileSize(long bytes) {
        return bytes < 1024 ? bytes + " B" : bytes < 1048576 ? String.format("%.1f KB", Double.valueOf(bytes / 1024.0d)) : String.format("%.1f MB", Double.valueOf(bytes / 1048576.0d));
    }

    private boolean checkIfNestedJarsNeedProcessing(File libDir) {
        File compiledClassesDir = new File(this.workDir, "compiled_classes");
        if (!compiledClassesDir.exists()) {
            return false;
        }
        int classFileCount = countClassFiles(compiledClassesDir);
        EncodingHelper.safePrintln("检测到 " + classFileCount + " 个编译后的class文件");
        if (classFileCount == 0) {
            return false;
        }
        if (classFileCount <= 5) {
            EncodingHelper.safePrintln("修改的class文件较少，假设不影响嵌套jar文件");
            return false;
        }
        return true;
    }

    private int countClassFiles(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countClassFiles(file);
                } else if (file.getName().endsWith(".class")) {
                    count++;
                }
            }
        }
        return count;
    }

    private void processNestedJarsOptimized(File libDir) throws InterruptedException, IOException {
        System.out.println("开始优化处理嵌套Jar文件...");
        File[] jarFiles = libDir.listFiles((dir, name) -> {
            return name.endsWith(".jar");
        });
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("没有找到嵌套的Jar文件");
            return;
        }
        int maxJarsToProcess = Math.min(jarFiles.length, 100);
        System.out.println("发现 " + jarFiles.length + " 个嵌套Jar文件，优化处理前 " + maxJarsToProcess + " 个");
        Arrays.sort(jarFiles, (f1, f2) -> {
            return Long.compare(f1.length(), f2.length());
        });
        File libUnpackedDir = new File(this.extractDir, "BOOT-INF/lib_unpacked");
        if (!libUnpackedDir.exists() && !libUnpackedDir.mkdirs()) {
            throw new IOException("无法创建lib_unpacked目录");
        }
        int threadCount = Math.min(2, Runtime.getRuntime().availableProcessors() / 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < maxJarsToProcess; i++) {
            try {
                File jarFile = jarFiles[i];
                System.out.println("处理jar文件 " + (i + 1) + "/" + maxJarsToProcess + ": " + jarFile.getName() + " (" + formatFileSize(jarFile.length()) + ")");
                processSimpleNestedJar(jarFile, libDir, libUnpackedDir);
            } finally {
                executor.shutdown();
                if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {
                    System.err.println("强制关闭线程池");
                    executor.shutdownNow();
                }
            }
        }
        try {
            deleteDirectory(libUnpackedDir);
            System.out.println("清理临时目录完成");
        } catch (IOException e) {
            System.err.println("警告：清理临时目录失败: " + e.getMessage());
        }
        System.out.println("优化的嵌套Jar文件处理完成");
    }

    private void processSimpleNestedJar(File jarFile, File libDir, File libUnpackedDir) throws InterruptedException, IOException {
        if (jarFile.length() > 1048576) {
            System.out.println("跳过大文件: " + jarFile.getName() + " (" + formatFileSize(jarFile.length()) + ")");
        } else {
            System.out.println("快速处理: " + jarFile.getName());
        }
    }

    private void processNestedJars(File libDir) throws InterruptedException, IOException {
        System.out.println("开始处理嵌套Jar文件...");
        File[] jarFiles = libDir.listFiles((dir, name) -> {
            return name.endsWith(".jar");
        });
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("没有找到嵌套的Jar文件");
            return;
        }
        System.out.println("发现 " + jarFiles.length + " 个嵌套Jar文件，启用多线程处理...");
        File libUnpackedDir = new File(this.extractDir, "BOOT-INF/lib_unpacked");
        if (!libUnpackedDir.exists() && !libUnpackedDir.mkdirs()) {
            throw new IOException("无法创建lib_unpacked目录");
        }
        int threadCount = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), 4);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            System.out.println("第1步：解压嵌套Jar文件（使用 " + threadCount + " 个线程）...");
            List<Future<Boolean>> extractFutures = new ArrayList<>();
            AtomicInteger extractedCount = new AtomicInteger(0);
            for (File jarFile : jarFiles) {
                extractFutures.add(executor.submit(() -> {
                    try {
                        return Boolean.valueOf(extractNestedJar(jarFile, libUnpackedDir, extractedCount, jarFiles.length));
                    } catch (Exception e) {
                        System.err.println("解压嵌套Jar失败: " + jarFile.getName() + " - " + e.getMessage());
                        return false;
                    }
                }));
            }
            int extractSuccessCount = 0;
            for (Future<Boolean> future : extractFutures) {
                try {
                    if (future.get(60L, TimeUnit.SECONDS).booleanValue()) {
                        extractSuccessCount++;
                    }
                } catch (TimeoutException e) {
                    System.err.println("解压任务超时");
                    future.cancel(true);
                } catch (Exception e2) {
                    System.err.println("等待解压任务完成时出错: " + e2.getMessage());
                }
            }
            System.out.println("解压完成: " + extractSuccessCount + "/" + jarFiles.length + " 成功");
            System.out.println("第2步：重新打包嵌套Jar文件...");
            File[] unpackedDirs = libUnpackedDir.listFiles();
            if (unpackedDirs != null && unpackedDirs.length > 0) {
                List<Future<Boolean>> packFutures = new ArrayList<>();
                AtomicInteger packedCount = new AtomicInteger(0);
                for (File unpackedDir : unpackedDirs) {
                    if (unpackedDir.isDirectory()) {
                        packFutures.add(executor.submit(() -> {
                            try {
                                return Boolean.valueOf(repackNestedJar(unpackedDir, libDir, packedCount, unpackedDirs.length));
                            } catch (Exception e3) {
                                System.err.println("重新打包嵌套Jar失败: " + unpackedDir.getName() + " - " + e3.getMessage());
                                return false;
                            }
                        }));
                    }
                }
                int packSuccessCount = 0;
                for (Future<Boolean> future2 : packFutures) {
                    try {
                        try {
                            if (future2.get(60L, TimeUnit.SECONDS).booleanValue()) {
                                packSuccessCount++;
                            }
                        } catch (TimeoutException e3) {
                            System.err.println("打包任务超时");
                            future2.cancel(true);
                        }
                    } catch (Exception e4) {
                        System.err.println("等待打包任务完成时出错: " + e4.getMessage());
                    }
                }
                System.out.println("重新打包完成: " + packSuccessCount + "/" + packFutures.size() + " 成功");
            }
            try {
                deleteDirectory(libUnpackedDir);
                System.out.println("清理临时目录完成");
            } catch (IOException e5) {
                System.err.println("警告：清理临时目录失败: " + e5.getMessage());
            }
            System.out.println("嵌套Jar文件处理完成");
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(30L, TimeUnit.SECONDS)) {
                System.err.println("强制关闭线程池");
                executor.shutdownNow();
            }
        }
    }

    private boolean extractNestedJar(File jarFile, File libUnpackedDir, AtomicInteger processedCount, int totalCount) throws InterruptedException, IOException {
        int current = processedCount.incrementAndGet();
        String jarName = jarFile.getName();
        System.out.println("[解压 " + current + "/" + totalCount + "] " + jarName);
        File unpackDir = new File(libUnpackedDir, jarName);
        if (!unpackDir.mkdirs()) {
            System.err.println("警告：无法创建解压目录 " + unpackDir.getAbsolutePath());
            return false;
        }
        String jarExe = this.config.getJarExecutable();
        List<String> extractCommand = new ArrayList<>();
        extractCommand.add(jarExe);
        extractCommand.add("-xf");
        extractCommand.add(jarFile.getAbsolutePath());
        ProcessBuilder extractPb = new ProcessBuilder(extractCommand);
        extractPb.directory(unpackDir);
        extractPb.redirectErrorStream(true);
        Process extractProcess = extractPb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(extractProcess.getInputStream()));
        Throwable th = null;
        do {
            try {
                try {
                } finally {
                }
            } catch (Throwable th2) {
                if (reader != null) {
                    if (th != null) {
                        try {
                            reader.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        reader.close();
                    }
                }
                throw th2;
            }
        } while (reader.readLine() != null);
        if (reader != null) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                }
            } else {
                reader.close();
            }
        }
        int extractExitCode = extractProcess.waitFor();
        if (extractExitCode != 0) {
            System.err.println("警告：解压Jar文件失败: " + jarName + " (退出码: " + extractExitCode + ")");
            return false;
        }
        return true;
    }

    private boolean repackNestedJar(File unpackedDir, File libDir, AtomicInteger processedCount, int totalCount) throws InterruptedException, IOException {
        int current = processedCount.incrementAndGet();
        String jarFileName = unpackedDir.getName();
        System.out.println("[打包 " + current + "/" + totalCount + "] " + jarFileName);
        File newJarFile = new File(libDir, jarFileName);
        String jarExe = this.config.getJarExecutable();
        List<String> packCommand = new ArrayList<>();
        packCommand.add(jarExe);
        packCommand.add("-cf");
        packCommand.add("-M0");
        packCommand.add(newJarFile.getAbsolutePath());
        packCommand.add("-C");
        packCommand.add(unpackedDir.getAbsolutePath());
        packCommand.add(".");
        ProcessBuilder packPb = new ProcessBuilder(packCommand);
        packPb.directory(this.workDir);
        packPb.redirectErrorStream(true);
        Process packProcess = packPb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(packProcess.getInputStream()));
        Throwable th = null;
        do {
            try {
                try {
                } finally {
                }
            } catch (Throwable th2) {
                if (reader != null) {
                    if (th != null) {
                        try {
                            reader.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        reader.close();
                    }
                }
                throw th2;
            }
        } while (reader.readLine() != null);
        if (reader != null) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                }
            } else {
                reader.close();
            }
        }
        int packExitCode = packProcess.waitFor();
        if (packExitCode != 0) {
            System.err.println("警告：重新打包Jar文件失败: " + jarFileName + " (退出码: " + packExitCode + ")");
            return false;
        }
        return true;
    }

    private void replaceCompiledClasses() throws IOException {
        System.out.println("替换编译后的class文件");
        File compiledClassesDir = new File(this.workDir, "compiled_classes");
        if (!compiledClassesDir.exists()) {
            System.out.println("没有找到编译输出目录，跳过class文件替换");
            return;
        }
        File targetDir = new File(this.extractDir, "BOOT-INF/classes");
        if (!targetDir.exists()) {
            targetDir = this.extractDir;
        }
        int replacedCount = copyCompiledClasses(compiledClassesDir, compiledClassesDir, targetDir);
        System.out.println("class文件替换完成，共替换 " + replacedCount + " 个文件");
    }

    private int copyCompiledClasses(File sourceDir, File baseDir, File targetDir) throws IOException {
        int count = 0;
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += copyCompiledClasses(file, baseDir, targetDir);
                } else if (file.getName().endsWith(".class")) {
                    String relativePath = baseDir.toPath().relativize(file.toPath()).toString();
                    File targetFile = new File(targetDir, relativePath);
                    targetFile.getParentFile().mkdirs();
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("已替换: " + relativePath);
                    count++;
                }
            }
        }
        return count;
    }

    private List<File> getModifiedJavaFiles() {
        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(this.sourceDir, javaFiles);
        return javaFiles;
    }

    private void findJavaFiles(File dir, List<File> javaFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findJavaFiles(file, javaFiles);
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.exists()) {
            Files.walk(dir.toPath(), new FileVisitOption[0]).sorted((a, b) -> {
                return -a.compareTo(b);
            }).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("删除文件失败: " + path + " - " + e.getMessage());
                }
            });
        }
    }

    private void addDirectoryToJar(File directory, String basePath, JarOutputStream jos) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String entryName = basePath + file.getName();
                if (file.isDirectory()) {
                    if (!entryName.endsWith("/")) {
                        entryName = entryName + "/";
                    }
                    JarEntry entry = new JarEntry(entryName);
                    jos.putNextEntry(entry);
                    jos.closeEntry();
                    addDirectoryToJar(file, entryName, jos);
                } else {
                    JarEntry entry2 = new JarEntry(entryName);
                    jos.putNextEntry(entry2);
                    FileInputStream fis = new FileInputStream(file);
                    Throwable th = null;
                    try {
                        try {
                            byte[] buffer = new byte[8192];
                            while (true) {
                                int length = fis.read(buffer);
                                if (length <= 0) {
                                    break;
                                } else {
                                    jos.write(buffer, 0, length);
                                }
                            }
                            if (fis != null) {
                                if (0 != 0) {
                                    try {
                                        fis.close();
                                    } catch (Throwable th2) {
                                        th.addSuppressed(th2);
                                    }
                                } else {
                                    fis.close();
                                }
                            }
                            jos.closeEntry();
                        } catch (Throwable th3) {
                            th = th3;
                            throw th3;
                        }
                    } catch (Throwable th4) {
                        if (fis != null) {
                            if (th != null) {
                                try {
                                    fis.close();
                                } catch (Throwable th5) {
                                    th.addSuppressed(th5);
                                }
                            } else {
                                fis.close();
                            }
                        }
                        throw th4;
                    }
                }
            }
        }
    }

    private List<File> findClassFiles(File directory) {
        List<File> classFiles = new ArrayList<>();
        findClassFilesRecursive(directory, classFiles);
        return classFiles;
    }

    private void findClassFilesRecursive(File directory, List<File> classFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findClassFilesRecursive(file, classFiles);
                } else if (file.getName().endsWith(".class")) {
                    classFiles.add(file);
                }
            }
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public void cleanup() {
        if (this.workDir != null && this.workDir.exists()) {
            try {
                deleteDirectory(this.workDir);
                System.out.println("工作目录清理完成");
            } catch (IOException e) {
                System.err.println("清理工作目录失败: " + e.getMessage());
            }
        }
    }

    public File getWorkDir() {
        return this.workDir;
    }

    public File getSourceDir() {
        return this.sourceDir;
    }
    private boolean isSpringBootJar(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            Throwable var3 = null;

            boolean var4;
            try {
                var4 = jar.getEntry("BOOT-INF/") != null || jar.getEntry("META-INF/spring-boot-version") != null || jar.getEntry("org/springframework/boot/loader/") != null;
            } catch (Throwable var14) {
                var3 = var14;
                throw var14;
            } finally {
                if (jar != null) {
                    if (var3 != null) {
                        try {
                            jar.close();
                        } catch (Throwable var13) {
                            var3.addSuppressed(var13);
                        }
                    } else {
                        jar.close();
                    }
                }

            }

            return var4;
        } catch (IOException var16) {
            return false;
        }
    }

    private void validateSpringBootCompatibility(File jarFile) throws IOException {
        System.out.println("验证Spring Boot兼容性...");
        JarFile jar = new JarFile(jarFile);
        Throwable var3 = null;

        try {
            Enumeration<JarEntry> entries = jar.entries();

            JarEntry manifestEntry;
            while(entries.hasMoreElements()) {
                manifestEntry = (JarEntry)entries.nextElement();
                if (manifestEntry.getName().startsWith("BOOT-INF/lib/") && manifestEntry.getName().endsWith(".jar")) {
                    try {
                        if (manifestEntry.getMethod() != 0) {
                            System.err.println("警告：嵌套jar文件 " + manifestEntry.getName() + " 被压缩，可能导致Spring Boot启动失败");
                            System.err.println("建议：使用-M0参数确保嵌套jar文件不压缩");
                        } else {
                            System.out.println("✓ 嵌套jar文件 " + manifestEntry.getName() + " 存储方式正确（未压缩）");
                        }
                    } catch (Exception var57) {
                        Exception e = var57;
                        System.err.println("检查嵌套jar文件 " + manifestEntry.getName() + " 时出错: " + e.getMessage());
                    }
                }
            }

            if (jar.getEntry("org/springframework/boot/loader/JarLauncher.class") == null) {
                System.err.println("警告：未找到Spring Boot加载器类，可能不是有效的Spring Boot jar");
            } else {
                System.out.println("✓ Spring Boot加载器类存在");
            }

            manifestEntry = (JarEntry)jar.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry != null) {
                InputStream is = jar.getInputStream(manifestEntry);
                Throwable var7 = null;

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    Throwable var9 = null;

                    try {
                        String line;
                        try {
                            while((line = reader.readLine()) != null) {
                                if (line.startsWith("Main-Class:")) {
                                    String mainClass = line.substring("Main-Class:".length()).trim();
                                    if (mainClass.contains("springframework.boot.loader")) {
                                        System.out.println("✓ Spring Boot Main-Class正确: " + mainClass);
                                    } else {
                                        System.err.println("警告：Main-Class可能不正确: " + mainClass);
                                    }
                                    break;
                                }
                            }
                        } catch (Throwable var58) {
                            var9 = var58;
                            throw var58;
                        }
                    } finally {
                        if (reader != null) {
                            if (var9 != null) {
                                try {
                                    reader.close();
                                } catch (Throwable var56) {
                                    var9.addSuppressed(var56);
                                }
                            } else {
                                reader.close();
                            }
                        }

                    }
                } catch (Throwable var60) {
                    var7 = var60;
                    throw var60;
                } finally {
                    if (is != null) {
                        if (var7 != null) {
                            try {
                                is.close();
                            } catch (Throwable var55) {
                                var7.addSuppressed(var55);
                            }
                        } else {
                            is.close();
                        }
                    }

                }
            }
        } catch (Throwable var62) {
            var3 = var62;
            throw var62;
        } finally {
            if (jar != null) {
                if (var3 != null) {
                    try {
                        jar.close();
                    } catch (Throwable var54) {
                        var3.addSuppressed(var54);
                    }
                } else {
                    jar.close();
                }
            }

        }

        System.out.println("Spring Boot兼容性验证完成");
    }

}
