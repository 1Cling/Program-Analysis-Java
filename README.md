# Java项目静态分析工具

​	这是一个`基于 JavaParser 开发的 Java 源代码静态分析工具`，能够**深入分析** Java 项目的结构、依赖关系以及代码质量指标。

​	通过`自动化程序分析技术`，帮助开发者快速了解项目的整体状况，在**理解代码、提取度量、发现缺陷**等方面发挥着重要作用。

## 功能特点

1. `类/接口分析：`统计项目中的**类和接口信息**，包括完整的类名、接口名。
2. `方法分析：`分析**每个类中**的方法数量、每个方法的方法名称、参数个数、代码行数、圈复杂度以及**该类**的圈复杂度和注释率。
3. `依赖分析：`生成类之间的**调用关系图**（文本格式）。
4. `代码量度汇总：`计算**所有类的**数量、总方法数、平均圈复杂度、平均注释率。

## 技术栈

- **Java 8**：项目的基础开发语言，确保工具的兼容性和性能。
- **JavaParser**：Java 源代码解析库，用于生成抽象语法树（AST），便于进行代码分析。
- **Maven**：项目构建和依赖管理工具，简化项目的编译和部署过程。

## 快速开始

### 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本

### 使用方法

1. 克隆项目到本地

```bash
git clone https://github.com/1Cling/java-code-analyzer.git
cd code-analyzer
```
2. 使用Maven编译项目

```bash
mvn package
```

此命令会自动下载项目依赖，并将项目编译成可执行的 JAR 文件。

## 使用说明

- Application.java 为**程序入口**，可以在 IDE 中直接运行该类。
- 运行程序后，可用**以下命令**：
  1. `report [源代码路径] [报告输出路径]` ：指定要分析的 Java 源代码目录和报告输出的文件路径，生成详细的代码分析报告。
  2. `exit`：退出程序。

**eg:**

```bash
report G:\analyse\commons-cli G:\workspace\code-analyzer\Report-cli.txt
```

![1747312261834](https://github.com/1Cling/java-code-analyzer/blob/main/images/1747312261834.png)

## 输出报告内容

**生成的报告包含以下部分：**

1. `类/接口汇总：`

   **格式为：**

   - 类： 完整类名
   - 接口： 完整接口名

   **结构：** Java 包路径 + 类名

2. `方法统计`

   - 每个类中的方法数
   - 方法名
   - 参数数量
   - 代码行数
   - 圈复杂度

3. 类调用关系
   - 类之间的调用关系图
   - 依赖关系以文本格式展示

4. 代码度量汇总
   - 类的总数
   - 方法的总数
   - 平均圈复杂度
   - 整体注释率

## 项目结构

```
code-analyzer/
└── src/
    └── main/
        └── java/
            └── cn/
                └── cling/
                    └── analyzer/
                        ├── Application.java    			      # 应用程序入口
                        ├── core/              				      # 核心功能模块
                        │   ├── JavaSourceAnalyzer.java     # 源代码分析器
                        │   └── ReportGenerator.java        # 报告生成器
                        ├── model/             				      # 数据模型
                        │   ├── ClassInfo.java              # 类信息模型
                        │   └── MethodInfo.java             # 方法信息模型
                        └── util/              				      # 工具类
                            └── FileUtils.java              # 文件操作工具类
```

## 注意事项

- 确保源代码目录包含有效的Java源文件
- 建议使用UTF-8编码的Java源文件
- 分析大型项目时可能需要较长时间
