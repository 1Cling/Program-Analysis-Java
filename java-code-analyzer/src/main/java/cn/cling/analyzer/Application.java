package cn.cling.analyzer;

import cn.cling.analyzer.core.JavaSourceAnalyzer;
import cn.cling.analyzer.core.ReportGenerator;
import cn.cling.analyzer.model.ClassInfo;
import cn.cling.analyzer.util.FileUtils;

import java.util.List;
import java.util.Scanner;

/**
 * @author Cling
 * @create 2025-05-14 15:17
 */
public class Application {
    public static void main(String[] args) {
        System.out.println("欢迎使用Java项目静态分析工具！");
        System.out.println("可用命令：");
        System.out.println("    report [源代码路径] [报告输出路径] —— 生成代码分析报告");
        System.out.println("    exit —— 退出程序");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n>>> ");
            String input = scanner.nextLine().trim();
            
            if (input.equals("exit")) {
                System.out.println("感谢使用，再见！");
                break;
            }
            
            if (input.startsWith("report ")) {
                String[] parts = input.split("\\s+");
                if (parts.length != 3) {
                    System.out.println("命令格式错误！正确格式：report [源代码路径] [报告输出路径]");
                    continue;
                }
                
                try {
                    // 检查源代码目录是否存在
                    if (!FileUtils.isValidDirectory(parts[1])) {
                        System.out.println("错误：源代码目录不存在或不是有效目录");
                        continue;
                    }
                    
                    // 分析源代码
                    System.out.println("正在分析源代码...");
                    JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
                    List<ClassInfo> classInfoList = analyzer.analyzeDirectory(parts[1]);
                    
                    // 生成报告
                    System.out.println("正在生成分析报告...");
                    ReportGenerator reportGenerator = new ReportGenerator();
                    reportGenerator.generateReport(classInfoList, parts[2]);
                    
                    System.out.println("分析完成！报告已保存到：" + parts[2]);
                } catch (Exception e) {
                    System.out.println("发生错误：" + e.getMessage());
                }
            } else {
                System.out.println("未知命令！可用命令：report, exit");
            }
        }
        scanner.close();
    }
}
