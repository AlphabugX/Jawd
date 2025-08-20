package com.alphabugx.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class EncodingHelper {
    public static final Charset UNIFIED_CHARSET = StandardCharsets.UTF_8;
    public static final Charset CONSOLE_CHARSET = getConsoleCharset();

    private static Charset getConsoleCharset() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String encoding = System.getProperty("file.encoding", "UTF-8");
        System.getProperty("console.encoding", encoding);
        try {
            if (osName.contains("windows")) {
                String windowsEncoding = System.getProperty("sun.jnu.encoding", "GBK");
                return Charset.forName(windowsEncoding);
            }
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    public static void safePrintln(String message) {
        try {
            byte[] bytes = message.getBytes(CONSOLE_CHARSET);
            String safeMassage = new String(bytes, CONSOLE_CHARSET);
            System.out.println(safeMassage);
        } catch (Exception e) {
            System.out.println(message);
        }
    }

    public static void safeErrorPrintln(String message) {
        try {
            byte[] bytes = message.getBytes(CONSOLE_CHARSET);
            String safeMassage = new String(bytes, CONSOLE_CHARSET);
            System.err.println(safeMassage);
        } catch (Exception e) {
            System.err.println(message);
        }
    }

    public static void setupProcessBuilderEncoding(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        env.put("LANG", "zh_CN.UTF-8");
        env.put("LC_ALL", "zh_CN.UTF-8");
        env.put("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8");
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows")) {
            env.put("CHCP", "65001");
        }
    }

    public static String readProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CONSOLE_CHARSET));
        Throwable th = null;
        while (true) {
            try {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (output.length() > 0) {
                        output.append(System.lineSeparator());
                    }
                    output.append(line);
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
        return output.toString();
    }

    public static PrintWriter createEncodedPrintWriter(OutputStream out) {
        try {
            return new PrintWriter((Writer) new OutputStreamWriter(out, UNIFIED_CHARSET), true);
        } catch (Exception e) {
            return new PrintWriter(out, true);
        }
    }

    public static void printEncodingInfo() {
        safePrintln("=== 编码信息 ===");
        safePrintln("系统默认编码: " + Charset.defaultCharset());
        safePrintln("file.encoding: " + System.getProperty("file.encoding"));
        safePrintln("console.encoding: " + System.getProperty("console.encoding", "未设置"));
        safePrintln("sun.jnu.encoding: " + System.getProperty("sun.jnu.encoding", "未设置"));
        safePrintln("操作系统: " + System.getProperty("os.name"));
        safePrintln("统一字符编码: " + UNIFIED_CHARSET);
        safePrintln("控制台编码: " + CONSOLE_CHARSET);
        safePrintln("===============");
    }
}
