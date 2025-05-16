package cn.cling.analyzer.core;

import cn.cling.analyzer.model.ClassInfo;
import cn.cling.analyzer.model.MethodInfo;
import cn.cling.analyzer.util.FileUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 解析源代码文件
 */
public class JavaSourceAnalyzer {
    private final List<ClassInfo> classList = new ArrayList<>();
    // 存储类名和包路径的映射，用于解析完整类名
    private final Map<String, String> classToPackageMap = new HashMap<>();

    public List<ClassInfo> analyzeDirectory(String directoryPath) throws IOException {
        List<Path> javaFiles = FileUtils.getJavaFiles(Paths.get(directoryPath));

        for (Path file : javaFiles) {
            buildClassPackageMapping(file);
        }

        for (Path file : javaFiles) {
            analyzeFile(file);
        }

        return classList;
    }

    private void buildClassPackageMapping(Path filePath) {
        try {
            ParserConfiguration config = new ParserConfiguration();
            JavaParser javaParser = new JavaParser(config);
            
            ParseResult<CompilationUnit> result = javaParser.parse(filePath);
            if (!result.isSuccessful()) {
                return;
            }
            
            CompilationUnit cu = result.getResult().get();
            String packageName = cu.getPackageDeclaration()
                    .map(pkg -> pkg.getNameAsString())
                    .orElse("");
                    
            for (TypeDeclaration<?> type : cu.getTypes()) {
                String className = type.getNameAsString();
                classToPackageMap.put(className, packageName);
            }
        } catch (IOException e) {
            System.err.println("构建类名映射时出错: " + e.getMessage());
        }
    }

    private void analyzeFile(Path filePath) throws IOException {
        // 配置JavaParser 并保留注释
        ParserConfiguration config = new ParserConfiguration();
        config.setAttributeComments(true);

        JavaParser javaParser = new JavaParser(config);

        ParseResult<CompilationUnit> result = javaParser.parse(filePath);
        if (!result.isSuccessful()) {
            System.err.println("解析失败: " + filePath);
            return;
        }

        // 获取AST的根节点
        CompilationUnit cu = result.getResult().get();

        String packageName = cu.getPackageDeclaration()
                .map(pkg -> pkg.getNameAsString())
                .orElse("");

        // 保存导入的类
        Map<String, String> importedClasses = new HashMap<>();
        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
            importedClasses.put(simpleName, importName);
        }

        for (TypeDeclaration<?> type : cu.getTypes()) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setPackageName(packageName);
            classInfo.setClassName(type.getNameAsString());
            classInfo.setInterface(type instanceof ClassOrInterfaceDeclaration &&
                    ((ClassOrInterfaceDeclaration) type).isInterface());

            // 注释率 = 注释行数 / 总行数
            calculateCommentRatio(type, classInfo);
            
            // 分析继承关系和实现的接口
            analyzeInheritance(type, classInfo, importedClasses);
            
            // 收集类的字段信息
            collectClassFields(type, classInfo, importedClasses);
            
            // 收集类的方法信息
            collectClassMethods(type, classInfo, packageName, importedClasses);

            classList.add(classInfo);
        }
    }
    
    /**
     * 分析类的继承和接口实现关系
     */
    private void analyzeInheritance(TypeDeclaration<?> type, ClassInfo classInfo, Map<String, String> importedClasses) {
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) type;
            
            // 添加父类依赖
            for (ClassOrInterfaceType extendedType : classOrInterface.getExtendedTypes()) {
                String typeName = extendedType.getNameAsString();
                String fullName = resolveFullClassName(typeName, importedClasses);
                if (fullName != null) {
                    classInfo.addDependency(fullName);
                }
            }
            
            // 添加接口依赖
            for (ClassOrInterfaceType implementedType : classOrInterface.getImplementedTypes()) {
                String typeName = implementedType.getNameAsString();
                String fullName = resolveFullClassName(typeName, importedClasses);
                if (fullName != null) {
                    classInfo.addDependency(fullName);
                }
            }
        }
    }

    /**
     * 计算类的注释率
     */
    private void calculateCommentRatio(TypeDeclaration<?> type, ClassInfo classInfo) {
        List<Comment> comments = type.getAllContainedComments();
        int commentLines = comments.stream()
                .mapToInt(comment -> comment.getEnd().get().line - comment.getBegin().get().line + 1)
                .sum();

        int totalLines = type.getEnd().get().line - type.getBegin().get().line + 1;

        if (totalLines > 0) {
            classInfo.setCommentRatio((int) Math.round((commentLines * 100.0) / totalLines));
        }
    }

    /**
     * 收集类的字段信息
     */
    private void collectClassFields(TypeDeclaration<?> type, ClassInfo classInfo, Map<String, String> importedClasses) {
        type.getFields().forEach(field -> {
            for (VariableDeclarator variable : field.getVariables()) {
                classInfo.getFields().add(variable.getNameAsString());
                
                // 添加字段类型依赖
                String typeName = variable.getType().asString();
                String fullName = resolveFullClassName(typeName, importedClasses);
                if (fullName != null) {
                    classInfo.addDependency(fullName);
                }
            }
        });
    }

    /**
     * 收集类的方法信息
     */
    private void collectClassMethods(TypeDeclaration<?> type, ClassInfo classInfo, String packageName, 
                                      Map<String, String> importedClasses) {
        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) member;
                MethodInfo methodInfo = new MethodInfo();
                methodInfo.setName(method.getNameAsString());
                methodInfo.setParameterCount(method.getParameters().size());

                // 计算方法行数
                method.getBody().ifPresent(body -> {
                    methodInfo.setLineCount(body.getEnd().get().line - body.getBegin().get().line + 1);
                });

                // 计算方法的圈复杂度
                methodInfo.setCyclomaticComplexity(calculateCyclomaticComplexity(method));

                classInfo.addMethod(methodInfo);

                // 添加返回类型依赖
                String returnTypeName = method.getType().asString();
                if (!returnTypeName.equals("void")) {
                    String fullReturnType = resolveFullClassName(returnTypeName, importedClasses);
                    if (fullReturnType != null) {
                        classInfo.addDependency(fullReturnType);
                    }
                }
                
                // 添加参数类型依赖
                for (Parameter param : method.getParameters()) {
                    String paramTypeName = param.getType().asString();
                    String fullParamType = resolveFullClassName(paramTypeName, importedClasses);
                    if (fullParamType != null) {
                        classInfo.addDependency(fullParamType);
                    }
                }

                // 分析方法中的方法调用
                analyzeMethodCalls(method, packageName + "." + classInfo.getClassName(), classInfo, importedClasses);
            }
        }
    }

    /**
     * 计算方法的圈复杂度
     */
    private int calculateCyclomaticComplexity(MethodDeclaration method) {
        AtomicInteger complexity = new AtomicInteger(1); // 基础复杂度为1

        // 统计条件分支
        complexity.addAndGet(method.findAll(IfStmt.class).size());
        complexity.addAndGet(method.findAll(ForStmt.class).size());
        complexity.addAndGet(method.findAll(WhileStmt.class).size());
        complexity.addAndGet(method.findAll(DoStmt.class).size());
        complexity.addAndGet(method.findAll(SwitchEntry.class).size());
        complexity.addAndGet(method.findAll(ConditionalExpr.class).size());

        // 统计catch块
        method.getBody().ifPresent(body -> {
            complexity.addAndGet(body.findAll(TryStmt.class).stream()
                    .mapToInt(tryStmt -> tryStmt.getCatchClauses().size())
                    .sum());
        });

        return complexity.get();
    }

    /**
     * 分析方法中的方法调用关系
     */
    private void analyzeMethodCalls(MethodDeclaration method, String callerFullName, ClassInfo classInfo, 
                                     Map<String, String> importedClasses) {
        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr call, Void arg) {
                super.visit(call, arg);

                // 解析被调用的方法所属的类
                call.getScope().ifPresent(scope -> {
                    String calleeClass = scope.toString();
                    // 尝试解析完整类名
                    String fullName = resolveFullClassName(calleeClass, importedClasses);
                    if (fullName != null) {
                        classInfo.addDependency(fullName);
                    }
                });
            }
            
            @Override
            public void visit(ClassOrInterfaceType type, Void arg) {
                super.visit(type, arg);
                
                // 处理方法体内部的类型引用
                String typeName = type.getNameAsString();
                String fullName = resolveFullClassName(typeName, importedClasses);
                if (fullName != null) {
                    classInfo.addDependency(fullName);
                }
            }
        }, null);
    }
    
    /**
     * 解析完整类名
     * @param simpleName 简单类名
     * @param importedClasses 导入的类映射表
     * @return 完整类名
     */
    private String resolveFullClassName(String simpleName, Map<String, String> importedClasses) {
        if (simpleName.contains("<")) {
            simpleName = simpleName.substring(0, simpleName.indexOf('<'));
        }

        if (isBasicType(simpleName)) {
            return null;
        }

        if (importedClasses.containsKey(simpleName)) {
            return importedClasses.get(simpleName);
        }

        if (classToPackageMap.containsKey(simpleName)) {
            String packageName = classToPackageMap.get(simpleName);
            return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        }

        if (Character.isUpperCase(simpleName.charAt(0))) {
            return simpleName;
        }
        
        return null;
    }

    private boolean isBasicType(String typeName) {
        Set<String> basicTypes = new HashSet<>(Arrays.asList(
            "void", "boolean", "byte", "char", "short", "int", "long", "float", "double",
            "Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "String", "Object"
        ));
        return basicTypes.contains(typeName);
    }
}