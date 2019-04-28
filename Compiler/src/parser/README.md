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
Start -> P <!-- 如果要进行嵌套过程中声明语句的翻译，在开始规约的时候就新建一张符号表和重置offset，并将其各自压栈  -->

P -> PStart D P 丨 PStart S P 丨 ε

PStart -> ε `{{ env = new Env(env); offsetStack.push(offset); offset=0;}}`

D -> proc X id ( M ) DM { P } `{{pop(tableStack); pop(offset)}}` 丨 record id { P } 丨 T id A ; `{{enter(id.lexeme, T.type, offset);offset = offset + T.width;}}`

DM -> ε `{{table = mkTable(top(tableStack)); push(table); push(offset); offset = 0;}}`

A -> = F A 丨 , id A 丨 ε

M -> M , X id `{{enter(id.lexeme, X.type, offset); offset = offset + X.width; M.size = M1.size + 1;}}`丨 X id `{{enter(id.lexeme, X.type, offset); offset = offset + X.width; M.size = 1;}}`

T -> X `{{t = X.type; w = X.width;}}` C `{{T.type = C.type; T.width = C.width;}}`

X -> int `{{X.type = interger; X.width = 4;}}`丨 float `{{X.type = float; X.width = 8;}}`丨 bool 丨 char

C -> [ num ] C `{{C.type = C1.type + '[' + num.value + ']'; C.width = num.value * C1.width;}}` 丨 ε `{{C.type = t; C.width = w;}}`

S -> id = E ; `{{S.nextList = null; p = loopUp(id.lexeme); if p == null then error else gen(p, '=', E.addr);}}`丨 if ( B ) BM S N else BM S `{{backpatch(B.trueList, BM1.instr); backpatch(B.falseList, BM2.instr); temp = merge(S1.nextList, N.nextList); S.nextList = merge(temp, S2.nextList); }}` 丨 while BM ( B ) BM S `{{backpatch(S1.nextList, BM1.instr); backpatch(B.trueList, BM2.instr); S.nextList = B.falseList; gen('goto', BM1.instr); }}` 丨 call id ( Elist ) ; 丨 return E ; 丨 if ( B ) BM S `{{backpatch(B.trueList, BM.instr); S.nextList = merge(B.falseList, S1.nextList); }}` 丨 L = E ; `{{gen(L.array, L.addr, '=', E.addr)}}`

N -> ε `{{N.nextList = makeList(nextInstr); gen('goto'); }}`

L -> L [ E ] `{{L.array = L1.array; L.type = L1.type.elem; L.width = L.type.width; t = new Temp(); L.addr = new Temp(); gen(L.addr, '=', E.addr, '*', L.width); gen(L.addr, '=', L1.addr, '+', t); }}` 丨 id [ E ] `{{p = lookUp(id.lexeme); if p == null then error else L.array = p; L.type = id.type; L.addr = new Temp(); gen(L.addr, 'addr', E.addr, '*', L.width)}}`

E -> E + G `{{E.addr = newTemp(); gen(E.addr, '=', E1.addr, '+', G.addr);}}`丨 G `{{E.addr = G.addr;}}`

G -> G * F `{{G.addr = newTemp(); gen(G.addr, '=', G1.addr, '*', F.addr);}}`丨 F `{{G.addr = F.addr;}}`

F -> ( E ) `{{F.addr = E.addr;}}`丨 num `{{F.addr = num.value;}}`丨 id `{{F.addr = lookup(id.lexeme); if F.addr == null then error;}}`丨 real `{{F.addr = real.value;}}`丨 string 丨 L `{{F.addr = L.array + '[' + L.addr']'}}`

B -> B || BM H `{{backpatch(B1.falseList, BM.instr); B.trueList = merge(B1.trueList, H.trueList); B.falstList = H.falstList;}}`丨 H `{{B.trueList = H.trueList; B.falseList = H.falseList;}}`

H -> H && BM I `{{backpatch(H1.trueList, BM.instr); H.trueList = I.trueList; H.falseList = merge(H1.falseList, I.falseList);}}`丨 I `{{H.trueList = I.trueList; H.falseList = I.falseList;}}`

I -> ! I `{{I.trueList = I1.falseList; I.falseList = I1.falseList;}}`丨 ( B ) `{{I.trueList = B.trueList; I.falseList = B.falseList;}}`丨 E Relop E `{{I.trueList = makeList(nextInstr); I.falseList = makeList(nextInstr + 1); gen('if', E1.addr, Relop.op, E2.addr, 'goto'); gen('goto');}}` 丨 true `{{I.trueList = makeList(nextInstr); gen('goto');}}`丨 false `{{I.falseList = makeList(nextInstr); gen('goto');}}`

BM -> ε `{{BM.instr = nextInstr}}`

Relop -> < 丨 <= 丨 > 丨 >= 丨 == 丨 != `{{Relop.op = op}}`

Elist -> Elist , E `{{Elist.size = Elist1.size + 1;}}` 丨 E `{{Elist.size = 1;}}`
