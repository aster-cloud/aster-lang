import {KW, TokenKind} from './tokens.js';
import {Node} from './ast.js';
import type {
    Block,
    Case,
    Declaration,
    Expression,
    Field,
    Module,
    Parameter,
    Pattern,
    Statement,
    Token,
    Type,
} from './types.js';
import {Diagnostics} from './diagnostics.js';

export function parse(tokens: readonly Token[]): Module {
  let i = 0;
  const peek = (): Token => tokens[i] || tokens[tokens.length - 1]!;
  const next = (): Token => tokens[i++]!;
  const at = (kind: TokenKind, value?: string | number | boolean | null): boolean => {
    const t = peek();
    if (!t) return false;
    if (t.kind !== kind) return false;
    if (value === undefined) return true;
    return t.value === value;
  };
  // const expect = (kind: TokenKind, msg: string): Token => {
  //   if (!at(kind)) error(msg + `, got ${peek().kind}`);
  //   return next();
  // };
  function error(msg: string, tok: Token = peek()): never {
    Diagnostics.unexpectedToken(msg, tok.start).withMessage(msg).throw();
    throw new Error('unreachable'); // For TypeScript
  }

  function consumeNewlines(): void {
    while (at(TokenKind.NEWLINE)) next();
  }

  const decls: Declaration[] = [];
  let moduleName: string | null = null;
  consumeNewlines();
  while (!at(TokenKind.EOF)) {
    consumeNewlines();
    while (at(TokenKind.DEDENT)) next();
    while (at(TokenKind.INDENT)) next();
    consumeNewlines();
    if (at(TokenKind.EOF)) break;
    if (isKeywordSeq(KW.MODULE_IS)) {
      nextWords(kwParts(KW.MODULE_IS));
      const name = parseDottedIdent();
      moduleName = name;
      expectDot();
    } else if (isKeyword(KW.USE)) {
      nextWord();
      const name = parseDottedIdent();
      let asName: string | null = null;
      if (isKeyword(KW.AS)) {
        nextWord();
        asName = parseIdent();
      }
      expectDot();
      decls.push(Node.Import(name, asName));
    } else if (isKeyword(KW.DEFINE)) {
      nextWord();
      const typeName = parseTypeIdent();
      if (isKeywordSeq(KW.WITH)) {
        nextWord();
        const fields = parseFieldList();
        expectDot();
        decls.push(Node.Data(typeName, fields));
      } else if (isKeywordSeq(KW.ONE_OF)) {
        nextWords(kwParts(KW.ONE_OF));
        const variants = parseVariantList();
        expectDot();
        decls.push(Node.Enum(typeName, variants));
      } else {
        error("Expected 'with' or 'as one of' after type name");
      }
    } else if (isKeyword(KW.TO)) {
      // Function
      nextWord();
      const name = parseIdent();
      // Optional type parameters: 'of' TypeId ('and' TypeId)*
      const typeParams: string[] = [];
      if (isKeyword('of')) {
        nextWord();
        let more = true;
        while (more) {
          // Stop if we ran into parameter list or produce clause
          if (isKeyword(KW.WITH) || isKeyword(KW.PRODUCE) || at(TokenKind.COLON)) break;
          // Parse a type variable name (prefer TYPE_IDENT; fall back to IDENT)
          const tv = at(TokenKind.TYPE_IDENT) ? (next().value as string) : parseIdent();
          typeParams.push(tv);
          if (isKeyword(KW.AND)) {
            nextWord();
            continue;
          }
          if (at(TokenKind.COMMA)) {
            next();
            // If comma is followed by 'with' or produce, stop
            if (isKeyword(KW.WITH) || isKeyword(KW.PRODUCE)) {
              more = false;
              break;
            }
            continue;
          }
          more = false;
        }
      }
      const params = parseParamList();
      expectCommaOr();
      expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType();
      let effects: string[] = [];
      let body: Block | null = null;

      // After return type, we can see '.' ending the sentence, then an optional effect sentence
      // Or we can see an inline effect ending with ':'
      if (at(TokenKind.DOT)) {
        next();
        consumeNewlines();
        if (
          isKeywordSeq(KW.PERFORMS) ||
          (tokLowerAt(i) === 'it' && tokLowerAt(i + 1) === 'performs')
        ) {
          if (!isKeywordSeq(KW.PERFORMS)) nextWord();
          nextWords(kwParts(KW.PERFORMS));
          effects = parseEffectList();
          if (at(TokenKind.DOT)) {
            next();
          } else if (at(TokenKind.COLON)) {
            next();
            expectNewline();
            body = parseBlock();
          } else {
            error("Expected '.' or ':' after effect clause");
          }
        }
      } else if (
        isKeywordSeq(KW.PERFORMS) ||
        (tokLowerAt(i) === 'it' && tokLowerAt(i + 1) === 'performs')
      ) {
        if (!isKeywordSeq(KW.PERFORMS)) nextWord();
        nextWords(kwParts(KW.PERFORMS));
        effects = parseEffectList();
        if (at(TokenKind.DOT)) {
          next();
        } else if (at(TokenKind.COLON)) {
          next();
          expectNewline();
          body = parseBlock();
        } else {
          error("Expected '.' or ':' after effect clause");
        }
      } else if (at(TokenKind.COLON)) {
        next();
        expectNewline();
        body = parseBlock();
      } else {
        error("Expected '.' or ':' after return type");
      }

      decls.push(Node.Func(name, typeParams, params, retType, effects, body));
    } else if (at(TokenKind.NEWLINE) || at(TokenKind.DEDENT) || at(TokenKind.INDENT)) {
      // Tolerate stray whitespace/dedent/indent at top-level
      next();
    } else {
      error('Unexpected token at top level');
    }
    consumeNewlines();
  }

  return Node.Module(moduleName, decls);

  // Helpers
  function kwParts(phrase: string): string[] {
    return phrase.split(' ');
  }
  function tokLowerAt(idx: number): string | null {
    const t = tokens[idx];
    if (!t) return null;
    if (t.kind !== TokenKind.IDENT && t.kind !== TokenKind.TYPE_IDENT) return null;
    return ((t.value as string) || '').toLowerCase();
  }
  function isKeyword(kw: string): boolean {
    const v = tokLowerAt(i);
    return v === kw;
  }
  function isKeywordSeq(words: string | string[]): boolean {
    const ws = Array.isArray(words) ? words : kwParts(words);
    for (let k = 0; k < ws.length; k++) {
      const v = tokLowerAt(i + k);
      if (v !== ws[k]) return false;
    }
    return true;
  }
  function nextWord(): Token {
    if (!(at(TokenKind.IDENT) || at(TokenKind.TYPE_IDENT))) error('Expected keyword/identifier');
    return next();
  }
  function nextWords(ws: string[]): void {
    ws.forEach(() => nextWord());
  }
  // function isWordSeq(ws: string[]): boolean { for (let k=0;k<ws.length;k++){ if (tokLowerAt(i+k)!==ws[k]) return false; } return true; }
  // function consumeWord(w: string): void { if (tokLowerAt(i)!==w) error(`Expected '${w}'`); next(); }
  // function consumeWords(ws: string[]): void { for (const w of ws) consumeWord(w); }
  function expectKeyword(kw: string, msg: string): void {
    if (!isKeyword(kw)) Diagnostics.expectedKeyword(kw, peek().start).withMessage(msg).throw();
    nextWord();
  }
  function expectDot(): void {
    if (!at(TokenKind.DOT)) Diagnostics.expectedPunctuation('.', peek().start).throw();
    next();
  }
  function expectCommaOr(): void {
    if (!at(TokenKind.COMMA)) Diagnostics.expectedPunctuation(',', peek().start).throw();
    next();
  }
  function expectNewline(): void {
    if (!at(TokenKind.NEWLINE))
      Diagnostics.expectedToken('newline', peek().kind, peek().start).throw();
    next();
  }

  function parseDottedIdent(): string {
    const parts = [parseIdent()];
    while (
      at(TokenKind.DOT) &&
      tokens[i + 1] &&
      (tokens[i + 1]!.kind === TokenKind.IDENT || tokens[i + 1]!.kind === TokenKind.TYPE_IDENT)
    ) {
      next();
      if (at(TokenKind.IDENT)) {
        parts.push(parseIdent());
      } else if (at(TokenKind.TYPE_IDENT)) {
        parts.push(next().value as string);
      }
    }
    return parts.join('.');
  }
  function parseIdent(): string {
    if (!at(TokenKind.IDENT)) Diagnostics.expectedIdentifier(peek().start).throw();
    return next().value as string;
  }
  function parseTypeIdent(): string {
    if (!at(TokenKind.TYPE_IDENT))
      Diagnostics.expectedToken('Type identifier', peek().kind, peek().start).throw();
    return next().value as string;
  }

  function parseFieldList(): Field[] {
    const fields: Field[] = [];
    let hasMore = true;
    while (hasMore) {
      const name = parseIdent();
      if (!at(TokenKind.COLON)) error("Expected ':' after field name");
      next();
      const t = parseType();
      fields.push({ name, type: t });
      if (at(TokenKind.COMMA)) {
        next();
        continue;
      }
      if (isKeyword(KW.AND)) {
        nextWord();
        continue;
      }
      hasMore = false;
    }
    return fields;
  }
  function parseVariantList(): string[] {
    const vars: string[] = [];
    let hasMore = true;
    while (hasMore) {
      const v = parseTypeIdent();
      vars.push(v);
      if (at(TokenKind.IDENT) && ((peek().value as string) || '').toLowerCase() === KW.OR) {
        nextWord();
        continue;
      }
      if (at(TokenKind.COMMA)) {
        next();
        continue;
      }
      hasMore = false;
    }
    return vars;
  }

  function parseParamList(): Parameter[] {
    const params: Parameter[] = [];
    // 'with' params
    if (isKeyword(KW.WITH)) {
      nextWord();
      let hasMore = true;
      while (hasMore) {
        const name = parseIdent();
        if (!at(TokenKind.COLON)) error("Expected ':' after parameter name");
        next();
        const type = parseType();
        params.push({ name, type });
        if (at(TokenKind.IDENT) && ((peek().value as string) || '').toLowerCase() === KW.AND) {
          nextWord();
          continue;
        }
        hasMore = false;
      }
      return params;
    }
    // Bare params: name: Type [and name: Type]*
    if (at(TokenKind.IDENT) && tokens[i + 1] && tokens[i + 1]!.kind === TokenKind.COLON) {
      let hasMore = true;
      while (hasMore) {
        const name = parseIdent();
        if (!at(TokenKind.COLON)) error("Expected ':' after parameter name");
        next();
        const type = parseType();
        params.push({ name, type });
        if (at(TokenKind.IDENT) && ((peek().value as string) || '').toLowerCase() === KW.AND) {
          nextWord();
          continue;
        }
        hasMore = false;
      }
    }
    return params;
  }

  function parseEffectList(): string[] {
    const effs: string[] = [];
    if (isKeyword(KW.IO)) {
      nextWord();
      effs.push('io');
    }
    if (isKeyword(KW.CPU)) {
      nextWord();
      effs.push('cpu');
    }
    return effs;
  }

  function parseType(): Type {
    // maybe T | Option of T | Result of T or E | list of T | map Text to Int | Text/Int/Float/Bool | TypeIdent
    if (isKeyword(KW.MAYBE)) {
      nextWord();
      return Node.Maybe(parseType());
    }
    if (isKeywordSeq(KW.OPTION_OF)) {
      nextWords(kwParts(KW.OPTION_OF));
      return Node.Option(parseType());
    }
    if (isKeywordSeq(KW.RESULT_OF)) {
      nextWords(kwParts(KW.RESULT_OF));
      const ok = parseType();
      expectKeyword(KW.OR, "Expected 'or' in Result of");
      const err = parseType();
      return Node.Result(ok, err);
    }
    if (isKeywordSeq(KW.FOR_EACH)) {
      /* not a type; handled elsewhere */
    }
    if (isKeywordSeq(KW.WITHIN)) {
      /* not a type */
    }

    if (isKeywordSeq(['list', 'of'])) {
      nextWord();
      nextWord();
      return Node.List(parseType());
    }
    if (isKeyword('map')) {
      nextWord();
      const k = parseType();
      expectKeyword(KW.TO_WORD, "Expected 'to' in map type");
      const v = parseType();
      return Node.Map(k, v);
    }

    if (isKeyword(KW.TEXT)) {
      nextWord();
      return Node.TypeName('Text');
    }
    if (isKeyword(KW.INT)) {
      nextWord();
      return Node.TypeName('Int');
    }
    if (isKeyword(KW.FLOAT)) {
      nextWord();
      return Node.TypeName('Float');
    }
    if (isKeyword(KW.BOOL_TYPE)) {
      nextWord();
      return Node.TypeName('Bool');
    }

    // Handle capitalized type keywords (Int, Bool, etc.) that are tokenized as IDENT
    if (at(TokenKind.IDENT)) {
      const value = peek().value as string;
      if (value === 'Int') {
        nextWord();
        return Node.TypeName('Int');
      }
      if (value === 'Bool') {
        nextWord();
        return Node.TypeName('Bool');
      }
      if (value === 'Text') {
        nextWord();
        return Node.TypeName('Text');
      }
      if (value === 'Float') {
        nextWord();
        return Node.TypeName('Float');
      }
    }

    if (at(TokenKind.TYPE_IDENT)) {
      const name = next().value as string;
      // Generic application: TypeName of T [and U]*
      if (isKeyword('of')) {
        nextWord();
        const args: Type[] = [];
        let more = true;
        while (more) {
          args.push(parseType());
          if (isKeyword(KW.AND)) {
            nextWord();
            continue;
          }
          if (at(TokenKind.COMMA)) {
            next();
            continue;
          }
          more = false;
        }
        return Node.TypeApp(name, args);
      }
      return Node.TypeName(name);
    }

    error('Expected type');
  }

  function parseBlock(): Block {
    const statements: Statement[] = [];
    consumeNewlines();
    if (!at(TokenKind.INDENT)) error('Expected indent');
    next();
    while (!at(TokenKind.DEDENT) && !at(TokenKind.EOF)) {
      consumeNewlines();
      if (at(TokenKind.DEDENT) || at(TokenKind.EOF)) break;
      statements.push(parseStatement());
      consumeNewlines();
    }
    if (!at(TokenKind.DEDENT)) error('Expected dedent');
    next();
    return Node.Block(statements);
  }

  function expectPeriodEnd(): void {
    if (!at(TokenKind.DOT)) error("Expected '.' at end of statement");
    next();
  }
  function expectPeriodEndOrLine(): void {
    if (at(TokenKind.DOT)) {
      next();
      return;
    }
    // Tolerate newline/dedent/EOF terminators inside blocks for certain statements (e.g., Return)
    if (at(TokenKind.NEWLINE) || at(TokenKind.DEDENT) || at(TokenKind.EOF)) return;
    error("Expected '.' at end of statement");
  }

  function parseStatement(): Statement {
    if (isKeyword(KW.LET)) {
      nextWord();
      const name = parseIdent();
      expectKeyword(KW.BE, "Use 'be' in bindings: 'Let x be ...'.");
      // Special-case lambda block form to avoid trailing '.'
      if ((isKeyword('a') && tokLowerAt(i + 1) === 'function') || isKeyword('function')) {
        if (isKeyword('a')) nextWord(); // optional 'a'
        nextWord(); // 'function'
        const params = parseParamList();
        expectCommaOr();
        expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
        const retType = parseType();
        if (!at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
        next();
        expectNewline();
        const body = parseBlock();
        return Node.Let(name, Node.Lambda(params, retType, body));
      }
      const expr = parseExpr();
      expectPeriodEnd();
      return Node.Let(name, expr);
    }
    if (isKeyword(KW.SET)) {
      nextWord();
      const name = parseIdent();
      expectKeyword(KW.TO_WORD, "Use 'to' in assignments: 'Set x to ...'.");
      const expr = parseExpr();
      expectPeriodEnd();
      return Node.Set(name, expr);
    }
    if (isKeyword(KW.RETURN)) {
      nextWord();
      const expr = parseExpr();
      expectPeriodEndOrLine();
      return Node.Return(expr);
    }
    if (isKeyword(KW.AWAIT)) {
      nextWord();
      if (!at(TokenKind.LPAREN)) error("Expected '(' after await");
      const args = parseArgList();
      if (args.length !== 1) error('await(expr) takes exactly one argument');
      const aw = Node.Call(Node.Name('await'), args);
      expectPeriodEnd();
      return aw as unknown as Statement;
    }
    if (isKeyword(KW.IF)) {
      nextWord();
      let negate = false;
      if (isKeyword(KW.NOT)) {
        nextWord();
        negate = true;
      }
      const cond = parseExpr();
      const condExpr = negate ? Node.Call(Node.Name('not'), [cond]) : cond;
      if (!at(TokenKind.COMMA)) error("Expected ',' after condition");
      next();
      if (!at(TokenKind.COLON)) error("Expected ':' after ',' in If");
      next();
      expectNewline();
      const thenBlock = parseBlock();
      let elseBlock: Block | null = null;
      if (isKeyword(KW.OTHERWISE)) {
        nextWord();
        if (!at(TokenKind.COMMA)) error("Expected ',' after 'Otherwise'");
        next();
        if (!at(TokenKind.COLON)) error("Expected ':' after ',' in Otherwise");
        next();
        expectNewline();
        elseBlock = parseBlock();
      }
      return Node.If(condExpr, thenBlock, elseBlock);
    }
    if (isKeyword(KW.MATCH)) {
      nextWord();
      const expr = parseExpr();
      if (!at(TokenKind.COLON)) error("Expected ':' after match expression");
      next();
      expectNewline();
      const cases = parseCases();
      return Node.Match(expr, cases);
    }
    // Plain bare expression as statement (allow method calls, constructions) ending with '.'
    if (
      at(TokenKind.IDENT) ||
      at(TokenKind.TYPE_IDENT) ||
      at(TokenKind.STRING) ||
      at(TokenKind.INT) ||
      at(TokenKind.BOOL) ||
      at(TokenKind.NULL) ||
      at(TokenKind.LPAREN)
    ) {
      const exprStart = i;
      try {
        const e = parseExpr();
        expectPeriodEnd();
        return e as Statement; // Not lowering; in v0, only Return statements are valid side-effects.
      } catch (e) {
        // rewind
        i = exprStart;
      }
    }
    if (isKeyword(KW.WITHIN)) {
      nextWord();
      expectKeyword(KW.SCOPE, "Expected 'scope' after 'Within'");
      if (!at(TokenKind.COLON)) error("Expected ':' after 'scope'");
      next();
      expectNewline();
      return parseBlock(); // Lowering later
    }
    if (isKeyword(KW.START)) {
      nextWord();
      const name = parseIdent();
      expectKeyword(KW.AS, "Expected 'as' after name");
      expectKeyword(KW.ASYNC, "Expected 'async'");
      const expr = parseExpr();
      expectPeriodEnd();
      return Node.Start(name, expr) as Statement;
    }
    if (isKeywordSeq(KW.WAIT_FOR)) {
      nextWords(kwParts(KW.WAIT_FOR));
      const names: string[] = [];
      names.push(parseIdent());
      while (isKeyword(KW.AND) || at(TokenKind.COMMA)) {
        if (isKeyword(KW.AND)) {
          nextWord();
          names.push(parseIdent());
        } else {
          next();
          names.push(parseIdent());
        }
      }
      expectPeriodEnd();
      return Node.Wait(names) as Statement;
    }

    error('Unknown statement');
  }

  function parseCases(): Case[] {
    const cases: Case[] = [];
    if (!at(TokenKind.INDENT)) error('Expected indent for cases');
    next();
    while (!at(TokenKind.DEDENT)) {
      if (!isKeyword(KW.WHEN)) error("Expected 'When'");
      nextWord();
      const pat = parsePattern();
      if (!at(TokenKind.COMMA)) error("Expected ',' after pattern");
      next();
      const body = parseCaseBody();
      cases.push(Node.Case(pat, body));
      while (at(TokenKind.NEWLINE)) next();
    }
    next();
    return cases;
  }
  function parseCaseBody(): Block | import('./types.js').Return {
    if (isKeyword(KW.RETURN)) {
      nextWord();
      const e = parseExpr();
      expectPeriodEnd();
      return Node.Return(e);
    }
    return parseBlock();
  }

  function parseExpr(): Expression {
    return parseComparison();
  }

  function parseComparison(): Expression {
    let left = parseAddition();

    let more = true;
    while (more) {
      if (at(TokenKind.LT)) {
        next();
        const right = parseAddition();
        left = Node.Call(Node.Name('<'), [left, right]);
      } else if (at(TokenKind.GT)) {
        next();
        const right = parseAddition();
        left = Node.Call(Node.Name('>'), [left, right]);
      } else if (isKeyword(KW.LESS_THAN)) {
        nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('<'), [left, right]);
      } else if (isKeyword(KW.GREATER_THAN)) {
        nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('>'), [left, right]);
      } else if (isKeyword(KW.EQUALS_TO)) {
        nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('=='), [left, right]);
      } else {
        more = false;
      }
    }
    return left;
  }

  function parseAddition(): Expression {
    let left = parsePrimary();

    let more = true;
    while (more) {
      if (at(TokenKind.PLUS)) {
        next();
        const right = parsePrimary();
        left = Node.Call(Node.Name('+'), [left, right]);
      } else if (at(TokenKind.MINUS)) {
        next();
        const right = parsePrimary();
        left = Node.Call(Node.Name('-'), [left, right]);
      } else if (isKeyword(KW.PLUS)) {
        nextWord();
        const right = parsePrimary();
        left = Node.Call(Node.Name('+'), [left, right]);
      } else if (isKeyword(KW.MINUS)) {
        nextWord();
        const right = parsePrimary();
        left = Node.Call(Node.Name('-'), [left, right]);
      } else {
        more = false;
      }
    }
    return left;
  }

  function parsePrimary(): Expression {
    // Minimal: construction, literals, names, Ok/Err/Some/None, call with dotted names and parens args
    // Lambda (block form): 'a function' (or 'function') ... 'produce' Type ':' \n Block
    if ((isKeyword('a') && tokLowerAt(i + 1) === 'function') || isKeyword('function')) {
      if (isKeyword('a')) nextWord(); // optional 'a'
      nextWord(); // 'function'
      const params = parseParamList();
      expectCommaOr();
      expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType();
      if (!at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
      next();
      expectNewline();
      const body = parseBlock();
      return Node.Lambda(params, retType, body);
    }
    // Lambda (short form): (x: Text, y: Int) => expr
    if (at(TokenKind.LPAREN)) {
      const save = i;
      try {
        next();
        const params: Parameter[] = [];
        let first = true;
        while (!at(TokenKind.RPAREN)) {
          if (!first) {
            if (at(TokenKind.COMMA)) {
              next();
            } else {
              throw new Error('comma');
            }
          }
          const pname = parseIdent();
          if (!at(TokenKind.COLON)) throw new Error('colon');
          next();
          const ptype = parseType();
          params.push({ name: pname, type: ptype });
          first = false;
        }
        next(); // consume ')'
        if (!(at(TokenKind.EQUALS) && tokens[i + 1] && tokens[i + 1]!.kind === TokenKind.GT)) {
          throw new Error('arrow');
        }
        next(); // '='
        next(); // '>'
        // Expression body; infer return type when possible
        const bodyExpr = parseExpr();
        const body = Node.Block([Node.Return(bodyExpr)]);
        const retType = inferLambdaReturnType(bodyExpr);
        return Node.Lambda(params, retType, body);
      } catch {
        // rewind and treat as parenthesized expression
        i = save;
      }
    }
    if (isKeywordSeq(KW.OK_OF)) {
      nextWords(kwParts(KW.OK_OF));
      return Node.Ok(parseExpr());
    }
    if (isKeywordSeq(KW.ERR_OF)) {
      nextWords(kwParts(KW.ERR_OF));
      return Node.Err(parseExpr());
    }
    if (isKeywordSeq(KW.SOME_OF)) {
      nextWords(kwParts(KW.SOME_OF));
      return Node.Some(parseExpr());
    }
    if (isKeyword(KW.NONE)) {
      nextWord();
      return Node.None();
    }
    if (at(TokenKind.STRING)) return Node.String(next().value as string);
    if (at(TokenKind.BOOL)) return Node.Bool(next().value as boolean);
    if (at(TokenKind.NULL)) return Node.Null();
    if (at(TokenKind.INT)) return Node.Int(next().value as number);
    if (isKeyword(KW.AWAIT)) {
      nextWord();
      const args = parseArgList();
      if (args.length !== 1) error('await(expr) takes exactly one argument');
      return Node.Call(Node.Name('await'), args);
    }

    // Parenthesized expressions
    if (at(TokenKind.LPAREN)) {
      next();
      const expr = parseExpr();
      if (!at(TokenKind.RPAREN)) error("Expected ')' after expression");
      next();
      return expr;
    }

    // Construction: Type with a = expr and b = expr
    if (at(TokenKind.TYPE_IDENT)) {
      const typeName = next().value as string;
      if (isKeyword(KW.WITH)) {
        nextWord();
        const fields: import('./types.js').ConstructField[] = [];
        let hasMore = true;
        while (hasMore) {
          const name = parseIdent();
          if (!at(TokenKind.EQUALS)) error("Expected '=' in construction");
          next();
          const e = parseExpr();
          fields.push({ name, expr: e });
          if (isKeyword(KW.AND)) {
            nextWord();
            continue;
          }
          hasMore = false;
        }
        return Node.Construct(typeName, fields);
      }
      // Dotted chain after TypeIdent (e.g., AuthRepo.verify)
      let full = typeName;
      while (
        at(TokenKind.DOT) &&
        tokens[i + 1] &&
        (tokens[i + 1]!.kind === TokenKind.IDENT || tokens[i + 1]!.kind === TokenKind.TYPE_IDENT)
      ) {
        next();
        full += '.' + parseIdent();
      }
      if (at(TokenKind.LPAREN)) {
        const target = Node.Name(full);
        const args = parseArgList();
        return Node.Call(target, args);
      }
      return Node.Name(full);
    }

    if (at(TokenKind.IDENT)) {
      const name = parseIdent();
      // dotted chain
      let full = name;
      while (
        at(TokenKind.DOT) &&
        tokens[i + 1] &&
        (tokens[i + 1]!.kind === TokenKind.IDENT || tokens[i + 1]!.kind === TokenKind.TYPE_IDENT)
      ) {
        next();
        if (at(TokenKind.IDENT)) {
          full += '.' + parseIdent();
        } else if (at(TokenKind.TYPE_IDENT)) {
          full += '.' + next().value;
        }
      }
      const target = Node.Name(full);
      if (at(TokenKind.LPAREN)) {
        const args = parseArgList();
        return Node.Call(target, args);
      }
      return target;
    }

    error('Unexpected expression');
  }

  function parseArgList(): Expression[] {
    if (!at(TokenKind.LPAREN)) error("Expected '('");
    next();
    const args: Expression[] = [];
    while (!at(TokenKind.RPAREN)) {
      args.push(parseExpr());
      if (at(TokenKind.COMMA)) {
        next();
        continue;
      } else break;
    }
    if (!at(TokenKind.RPAREN)) error("Expected ')'");
    next();
    return args;
  }

  function parsePattern(): Pattern {
    if (isKeyword(KW.NULL) || at(TokenKind.NULL)) {
      if (at(TokenKind.NULL)) next();
      else nextWord();
      return Node.PatternNull();
    }
    if (at(TokenKind.TYPE_IDENT)) {
      const typeName = next().value as string;
      if (at(TokenKind.LPAREN)) {
        next();
        const names: string[] = [];
        while (!at(TokenKind.RPAREN)) {
          names.push(parseIdent());
          if (at(TokenKind.COMMA)) {
            next();
            continue;
          } else break;
        }
        if (!at(TokenKind.RPAREN)) error("Expected ')' in pattern");
        next();
        return Node.PatternCtor(typeName, names);
      }
      // No LPAREN: treat bare TypeIdent as a variant name pattern (enum member)
      return Node.PatternName(typeName);
    }
    const name = parseIdent();
    return Node.PatternName(name);
  }
}
function inferLambdaReturnType(e: Expression): Type {
  switch (e.kind) {
    case 'String':
      return Node.TypeName('Text');
    case 'Int':
      return Node.TypeName('Int');
    case 'Bool':
      return Node.TypeName('Bool');
    case 'Call': {
      if (e.target.kind === 'Name') {
        const n = e.target.name;
        if (n === 'Text.concat') return Node.TypeName('Text');
        if (n === 'Text.length') return Node.TypeName('Int');
        if (n === '+') return Node.TypeName('Int');
        if (n === 'not') return Node.TypeName('Bool');
        if (n === '<' || n === '>' || n === '==') return Node.TypeName('Bool');
      }
      return Node.TypeName('Unknown');
    }
    default:
      return Node.TypeName('Unknown');
  }
}
