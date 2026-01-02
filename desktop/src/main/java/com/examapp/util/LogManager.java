package com.examapp.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class LogManager {

    private static final Logger logger = Logger.getLogger(LogManager.class.getName());
    private static FileHandler fileHandler;

    public static void initialize() {
        // 防止 JUL 默认的控制台处理器打印重复日志
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }
        
        logger.setLevel(Level.ALL); // 设置记录器级别

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            String logFilePath = "logs/app_" + timestamp + ".log";
            
            // 配置 FileHandler
            fileHandler = new FileHandler(logFilePath, true); // true for append mode
            fileHandler.setLevel(Level.ALL); // 设置处理器级别
            fileHandler.setFormatter(new LogFormatter());
            logger.addHandler(fileHandler);

            // 添加一个控制台处理器，用于在控制台也显示日志
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO); // 控制台可以只显示 INFO 及以上级别
            consoleHandler.setFormatter(new LogFormatter());
            logger.addHandler(consoleHandler);

            info("LogManager initialized. Log file: " + logFilePath);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize LogManager", e);
        }
    }

    public static String getLogFilePath() {
        // This is a bit tricky as we don't store the path directly now.
        // We can get it from the handler, but it's complex.
        // For simplicity, let's just return the directory.
        return new File("logs").getAbsolutePath();
    }

    public static void shutdown() {
        if (fileHandler != null) {
            info("LogManager shutting down.");
            fileHandler.close();
        }
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warning(String message) {
        log(Level.WARNING, message);
    }
    
    public static void severe(String message) {
        log(Level.SEVERE, message);
    }

    public static void severe(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        log(Level.FINE, message);
    }

    private static void log(Level level, String message) {
        // 获取调用者的信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // stackTrace[0] 是 getStackTrace
        // stackTrace[1] 是 log
        // stackTrace[2] 是 info/warning/severe
        // stackTrace[3] 是真正的调用者
        String callerClassName = "UnknownClass";
        String callerMethodName = "UnknownMethod";
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[3];
            callerClassName = caller.getClassName();
            callerMethodName = caller.getMethodName();
        }
        
        LogRecord record = new LogRecord(level, message);
        record.setSourceClassName(callerClassName);
        record.setSourceMethodName(callerMethodName);
        logger.log(record);
    }

    /**
     * 自定义日志格式化器
     */
    private static class LogFormatter extends Formatter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // 时间戳
            sb.append(sdf.format(new Date(record.getMillis()))).append(" ");
            
            // 日志级别
            sb.append(String.format("%-7s", record.getLevel().getName())).append(" ");
            
            // 线程ID
            sb.append("[").append(Thread.currentThread().getName()).append("] ");
            
            // 类名和方法名
            String className = record.getSourceClassName() != null ? record.getSourceClassName() : logger.getName();
            String methodName = record.getSourceMethodName() != null ? record.getSourceMethodName() : "";
            sb.append("[").append(className.substring(className.lastIndexOf('.') + 1));
            sb.append("#").append(methodName).append("] ");
            
            // 日志消息
            sb.append(": ").append(formatMessage(record));
            
            // 换行
            sb.append(System.lineSeparator());
            
            // 异常信息
            if (record.getThrown() != null) {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }
            
            return sb.toString();
        }
    }
}