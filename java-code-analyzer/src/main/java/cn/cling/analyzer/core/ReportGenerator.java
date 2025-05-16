package cn.cling.analyzer.core;

import cn.cling.analyzer.model.ClassInfo;
import cn.cling.analyzer.model.MethodInfo;
import cn.cling.analyzer.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java代码分析报告
 */
public class ReportGenerator {
    
    /**
     * @param classList 类信息列表
     * @param outputPath 输出文件路径
     */
    public void generateReport(List<ClassInfo> classList, String outputPath) throws IOException {
        StringBuilder report = new StringBuilder();

        report.append("===================\n");
        report.append("\t  Java项目分析报告\n");
        report.append("===================\n\n");

        generateClassSummary(report, classList);
        generateMethodStatistics(report, classList);
        generateDependencyGraph(report, classList);
        generateMetricsSummary(report, classList);
        FileUtils.writeFile(Paths.get(outputPath), report.toString());
    }

    private void generateClassSummary(StringBuilder report, List<ClassInfo> classList) {
        report.append("1. 类 / 接口汇总\n");
        report.append("------------------\n");
        
        // 遍历所有类信息
        for (ClassInfo cls : classList) {
            String type = cls.isInterface() ? "接口" : "类";
            report.append(String.format("%s %s.%s   [%d字段, %d方法, 注释率%d%%]\n",
                    type, cls.getPackageName(), cls.getClassName(),
                    cls.getFields().size(), cls.getMethods().size(), cls.getCommentRatio()));
        }
        report.append("\n");
    }

    private void generateMethodStatistics(StringBuilder report, List<ClassInfo> classList) {
        report.append("2. 方法统计\n");
        report.append("------------------\n");

        for (ClassInfo cls : classList) {
            report.append(String.format("%s.%s (共%d个方法):\n", 
                    cls.getPackageName(), cls.getClassName(), 
                    cls.getMethods().size()));
            
            // 类的圈复杂度
            int classComplexity = calculateClassComplexity(cls);
            report.append(String.format("该类的圈复杂度: %d", classComplexity));
            
            // 类的注释率
            report.append(String.format("       注释率: %d%%\n", cls.getCommentRatio()));
            
            // 遍历类中的所有方法
            for (MethodInfo method : cls.getMethods()) {
                report.append(String.format("  方法: %s\n", method.getName()));
                report.append(String.format("    参数个数: %d\n", method.getParameterCount()));
                report.append(String.format("    代码行数: %d\n", method.getLineCount()));
                report.append(String.format("    圈复杂度: %d\n", method.getCyclomaticComplexity()));
                report.append("\n");
            }
        }
    }

    /**
     * 类复杂度
     * @param cls 具体类
     * @return 类复杂度
     */
    private int calculateClassComplexity(ClassInfo cls) {
        int totalComplexity = 0;
        for (MethodInfo method : cls.getMethods()) {
            totalComplexity += method.getCyclomaticComplexity();
        }
        return totalComplexity;
    }

    /**
     * 生成依赖关系图部分
     * @param report
     * @param classList
     */
    private void generateDependencyGraph(StringBuilder report, List<ClassInfo> classList) {
        report.append("3. 类依赖关系\n");
        report.append("------------------\n");
        
        boolean hasDependencies = false;

        for (ClassInfo cls : classList) {
            // 获取该类的所有依赖
            Set<String> dependencies = cls.getDependencies();

            if (dependencies != null && !dependencies.isEmpty()) {
                hasDependencies = true;

                report.append(String.format("%s.%s 依赖于:\n", 
                        cls.getPackageName(), cls.getClassName()));

                for (String dependency : dependencies) {
                    report.append(String.format("  -> %s\n", dependency));
                }
                report.append("\n");
            }
        }
        
        // 没有检测到依赖关系
        if (!hasDependencies) {
            report.append("未检测到类依赖关系\n\n");
        }
    }

    private void generateMetricsSummary(StringBuilder report, List<ClassInfo> classList) {
        report.append("4. 代码度量汇总\n");
        report.append("------------------\n");
        
        // 总类数
        int totalClasses = classList.size();

        // 计算总方法数
        int totalMethods = 0;
        for (ClassInfo cls : classList) {
            totalMethods += cls.getMethods().size();
        }
        
        // 计算平均圈复杂度
        int methodCount = 0;
        double totalComplexity = 0;
        for (ClassInfo cls : classList) {
            for (MethodInfo method : cls.getMethods()) {
                totalComplexity += method.getCyclomaticComplexity();
                methodCount++;
            }
        }
        double avgComplexity = methodCount > 0 ? totalComplexity / methodCount : 0;
        
        // 计算平均注释率
        double totalCommentRatio = 0;
        for (ClassInfo cls : classList) {
            totalCommentRatio += cls.getCommentRatio();
        }
        double avgCommentRatio = classList.size() > 0 ? totalCommentRatio / classList.size() : 0;

        report.append(String.format("总类数: %d\n", totalClasses));
        report.append(String.format("总方法数: %d\n", totalMethods));
        report.append(String.format("平均圈复杂度: %.1f\n", avgComplexity));
        report.append(String.format("平均注释率: %.1f%%\n", avgCommentRatio));
    }
} 