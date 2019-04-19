**这个README内容过于混乱，参考Compiler/src/parser/README**

# 文法1

P -> D P | S P | Epsilon.
D -> prco X id ( M ) @ P # | record id @ P #.
Block -> @ Decls Stmts #.
Decls -> Decls Decl | Epsilon.
Decl -> T id A ;.
Stmts -> Stmts S | Epsilon.
A -> : F A | _ id A | Epsilon.
M -> M _ X id | X id.
T -> X C.
X -> int | float.
C -> @@ digit ## C | Epsilon.
S -> MatchedS | OpenS.
MatchedS -> if ( B ) MatchedS else MatchedS | L : E ; | do S while ( B ) ; | call id ( Elist ) ; | return E ; | Block | break ;.
OpenS -> if ( B ) S | if ( B ) MatchedS else OpenS.
L -> L @@ digit ## | id.
E -> E + G | G.
G -> G * F | F.
F -> ( E ) | digit | id.
B -> B or H | H.
H -> H and I | I.
I -> not I | ( B ) | E Relop E | true | false.
Relop -> ? | ?: | ~ | ~: | :: | !:.
Elist -> Elist _ E | E.
Epsilon ->.

S -> L : E ; | if ( B ) S else S | do S while ( B ) ; | call id ( Elist ) ; | return E ; | if ( B ) S | Block | break ;.
<!-- replace '{' with '@' 
    replace '}' with '#'
    replace ',' with '_'
    replace '[' with '@@'
    replace ']' with '##'
    replace '>' with '~'
    replace '<' with '?'
    replace '=' with ':'
    replace 'or' with 'o'
-->

# 文法2

P -> D P | S P | Epsilon.
D -> prco X id ( M ) @ P # | T id A ; | record id @ P #.
A -> : F A | _ id A | Epsilon.
M -> M _ X id | X id.
T -> X C.
X -> int | float | char.
C -> @@ digit ## C | Epsilon.
S -> L : E ; | if ( B ) S else S | do S while ( B ) ; | call id ( Elist ) ; | return E ;.
L -> L @@ digit ## | id.
E -> E + G | G.
G -> G * F | F.
F -> ( E ) | digit | id.
SE -> SE or AndE | AndE.
AndE -> AndE and UnaryE | UnaryE.
UnaryE -> not UnaryE | ( SE ) | E Relop E | true | false.
Relop -> ? | ?: | ~ | ~: | :: | !:.
Elist -> Elist _ E | E.
Epslion -> .

**文法1和文法2均是LR(1), 但不是SLR(1)**

# 确定的文法

Start -> P

P -> D P | S P | ε

D -> prco X id ( M ) { P } | record id { P }

Block -> { Delcs Stmts }

Delcs -> Delcs Delc | ε

Delc -> T id A ; 

Stmts -> Stmts S | ε

A -> = F A | , id A | ε

M -> M , X id | X id

T -> X C

X -> int | float | char 

C -> [ num ] C | ε

S -> MachedS | OpenS

MachedS -> if ( B ) MachedS else MachedS | L = E ; | do S while ( B ) ; | call id ( Elist ) ; | return E ; | break ; | Block

OpenS -> if ( B ) S | if ( B ) MachedS else OpenS

L -> L [ num ] | id

E -> E + G | G

G -> G * F | F

F -> ( E ) | num | id

B -> B or H | H

H -> H and I | I

I -> not I | ( B ) | E Relop E | true | false

Relop -> < | <= | > | >= | == | !=

Elist -> Elist , E | E



Start -> P
P -> D P | S P | ε
D -> prco X id ( M ) { P } | record id { P }
Block -> { Decls Stmts }
Decls -> Decls Decl | ε
Decl -> T id A ;
Stmts -> Stmts S | ε
A -> = F A | , id A | ε
M -> M , X id | X id
T -> X C
X -> int | float
C -> [ digit ] C | ε
S -> MatchedS | OpenS
MatchedS -> if ( B ) MatchedS else MatchedS | L = E ; | do S while ( B ) ; | call id ( Elist ) ; | return E ; | Block | break ;
OpenS -> if ( B ) S | if ( B ) MatchedS else OpenS
L -> L [ digit ] | id
E -> E + G | G
G -> G * F | F
F -> ( E ) | digit | id
B -> B or H | H
H -> H and I | I
I -> not I | ( B ) | E Relop E | true | false
Relop -> < | <= | > | >= | == | !=
Elist -> Elist , E | E

**最终使用的文法为`Compiler/src/parser/Grammar.txt`**


# 老师给的文法

Start -> P
P -> D P 丨 S P 丨 ε
D -> prco X id ( M ) { P } 丨 record id { P } 丨 T id A ;
A -> = F A 丨 , id A 丨 ε
M -> M , X id 丨 X id
T -> X C
X -> int 丨 float
C -> [ num ] C 丨 ε
S -> L = E ; 丨 if ( B ) S else S 丨 do S while ( B ) ; 丨 call id ( Elist ) ; 丨 return E ;
L -> L [ num ] 丨 id
E -> E + G 丨 G
G -> G * F 丨 F
F -> ( E ) 丨 num 丨 id
B -> B || H 丨 H
H -> H && I 丨 I
I -> ! I 丨 ( B ) 丨 E Relop E 丨 true 丨 false
Relop -> < 丨 <= 丨 > 丨 >= 丨 == 丨 !=
Elist -> Elist , E 丨 E