lexer grammar AsterLexer;

// Whitespace & comments
WS                  : [ \t\r\n]+ -> skip ;
LINE_COMMENT        : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT       : '/*' .*? '*/' -> skip ;

// Literals
BOOL                : 'true' | 'false' ;
DURATION            : [0-9]+ ( 'ms' | 's' | 'm' | 'h' | 'd' ) ;
DECIMAL             : [0-9]+ '.' [0-9]+ ;
INT                 : [0-9]+ ;
STRING              : '"' ( '\\\\' . | ~['"\\r\\n] )* '"' ;

// Keywords
MODULE              : 'module' ;
IMPORT              : 'import' ;
TYPE                : 'type' ;
RECORD              : 'record' ;
ENUM                : 'enum' ;
CAPABILITIES        : 'capabilities' ;
USES                : 'uses' ;
FN                  : 'fn' ;
WORKFLOW            : 'workflow' ;
ON                  : 'on' ;
START               : 'start' ;
STEP                : 'step' ;
RETRY               : 'retry' ;
BACKOFF             : 'backoff' ;
EXP                 : 'exp' ;
LET                 : 'let' ;
SET                 : 'set' ;
IF                  : 'if' ;
ELSE                : 'else' ;
MATCH               : 'match' ;
OK                  : 'Ok' ;
ERR                 : 'Err' ;
CONTINUE            : 'continue' ;
STOP                : 'stop' ;
AWAIT               : 'await' ;
ENSURE              : 'ensure' ;
COMPENSATE          : 'compensate' ;

// Punctuation & operators
LBRACE              : '{' ;
RBRACE              : '}' ;
LBRACK              : '[' ;
RBRACK              : ']' ;
LPAREN              : '(' ;
RPAREN              : ')' ;
LANGLE              : '<' ;
RANGLE              : '>' ;
COLON               : ':' ;
SEMI                : ';' ;
COMMA               : ',' ;
DOT                 : '.' ;
EQ                  : '=' ;
ARROW               : '->' ;
PIPE                : '|' ;

// Identifiers
ID                  : [_A-Za-z] [_A-Za-z0-9]* ;
