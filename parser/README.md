# 文法1 不是LR(1)

P -> D P | S P | Epsilon.
D -> prco X id ( M ) @ P # | T id A ; | record id @ P #.
A -> : F A | _ id A | Epsilon.
M -> M _ X id | X id.
T -> X C.
X -> int | float | char.
C -> @@ digit ## C | Epsilon.
S -> L : E; | if ( B ) S else S | do S while ( B ) ; | call id ( Elist ) ; | return E ;.
L -> L @@ digit ## | id.
E -> E + G | G.
G -> G * F | F.
F -> ( E ) | digit | id.
B -> H B1.
B1 -> or H B1 | Epslion.
H -> I H1.
H1 -> and I H1 | Epslion.
I -> not B | ( B ) | E Relop E | true | false.
Relop -> ? | ?: | ~ | ~: | :: | !:.
Elist -> Elist _ E | E.
Epslion -> .

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
S -> L : E; | if ( B ) S else S | do S while ( B ) ; | call id ( Elist ) ; | return E ;.
L -> L @@ digit ## | id.
E -> E + G | G.
G -> G * F | F.
F -> ( E ) | digit | id.
SE -> SE or AndE | AndE.
AndE -> AndE and UnaryE | UnaryE.
UnaryE -> not UnaryE | E Relop E | true | false.
Relop -> ? | ?: | ~ | ~: | :: | !:.
Elist -> Elist _ E | E.
Epslion -> .
