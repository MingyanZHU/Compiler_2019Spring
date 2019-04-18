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
Epslion ->.

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