package cn.cling.analyzer.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 存储 类分析结果的数据结构
 */
public class ClassInfo {
    private String packageName;
    private String className;
    private boolean isInterface;
    private List<String> fields = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private int commentRatio;
    private Set<String> dependencies = new HashSet<>(); // 依赖关系

    // 设置访问器和更改器
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }

    public int getCommentRatio() {
        return commentRatio;
    }

    public void setCommentRatio(int commentRatio) {
        this.commentRatio = commentRatio;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * 添加依赖关系
     * @param dependency 依赖的类名
     */
    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    /**
     * 添加方法信息
     * @param method 方法信息
     */
    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }
} 