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

# `Grammar_Good.txt`文法的翻译方案
Start -> P `{{ env = null; offsetStack = new Stack();}}`

P -> PStart D P 丨 PStart S P 丨 ε

PStart -> ε `{{ env = new Env(env); offsetStack.push(offset); offset=0;}}`

D -> proc X id ( M ) { P } 丨 record id { P } 丨 T id A ; `{{enter
(id.lexeme, T.type, offset);offset = offset + T.width;}}`

A -> = F A 丨 , id A 丨 ε

M -> M , X id 丨 X id

T -> X `{{t = X.type; w = X.width;}}` C `{{T.type = C.type; T.width = C.width;}}`

X -> int `{{X.type = interger; X.width = 4;}}`丨 float `{{X.type = float; X.width = 8;}}`丨 bool 丨 char

C -> [ num ] C 丨 ε `{{C.type = t; C.width = w;}}`

S -> L = E ; 丨 if ( B ) S else S 丨 do S while ( B ) 丨 call id ( Elist ) ; 丨 return E ; 丨 if ( B ) S

L -> L [ num ] 丨 id

E -> E + G 丨 G

G -> G * F 丨 F

F -> ( E ) 丨 num 丨 id 丨 real 丨 string

B -> B || H 丨 H

H -> H && I 丨 I

I -> ! I 丨 ( B ) 丨 E Relop E 丨 true 丨 false

Relop -> < 丨 <= 丨 > 丨 >= 丨 == 丨 !=

Elist -> Elist , E 丨 E