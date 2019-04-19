# 各个文件的内容
- Grammar_Good.txt 可以用来验收的稳定版本文法
- testParserFirst.txt 用来测试的文法文件
- c-Grammar.txt 将 c-Grammar.pdf 中部分内容进行删减的文法
- c-Grammar-test.txt 用于c-Grammar.txt中的文法网上测试的文件
- LRTable.txt 将LR文法转为对应的LR分析表
- Production.java 产生式类
- Item.java 项目类
- ItemSet.java 项目集类
# `Grammar_Good.txt`文法中的问题
- 不支持赋值同时运算，如`int a = x + 1;`。
- 不支持除过程之外的代码块，对于分支结构和循环结构的结构体均为单条语句，不支持大括号的代码块。
- 不支持布尔量赋值，如`bool x = false;`。
- 数组的声明方式奇怪，为`int [10] x;`。