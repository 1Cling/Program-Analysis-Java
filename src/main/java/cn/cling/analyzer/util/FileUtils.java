package cn.cling.analyzer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件操作工具类
 * 包括：文件读写、路径处理功能
 */
public class FileUtils {
    public static boolean isValidDirectory(String dirPath) {
        File dir = new File(dirPath);
        return dir.exists() && dir.isDirectory();
    }

    public static List<Path> getJavaFiles(Path directory) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        Files.walk(directory)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(javaFiles::add);
        return javaFiles;
    }

    public static String readFile(Path filePath) throws IOException {
        return new String(Files.readAllBytes(filePath));
    }

    public static void writeFile(Path filePath, String content) throws IOException {
        Files.write(filePath, content.getBytes());
    }
} 