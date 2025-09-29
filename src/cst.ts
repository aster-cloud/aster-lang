import type { Position, Span } from './types.js';
import { TokenKind } from './types.js';

export interface CstToken {
  readonly kind: TokenKind;
  readonly lexeme: string;
  readonly start: Position;
  readonly end: Position;
  readonly startOffset: number;
  readonly endOffset: number;
}

export interface Trivia {
  readonly text: string;
}

export interface CstNodeBase {
  readonly kind: string;
  readonly span: Span;
}

export interface CstModule extends CstNodeBase {
  readonly kind: 'Module';
  readonly tokens: readonly CstToken[];
  readonly leading?: Trivia;
  readonly trailing?: Trivia;
  // When built in lossless mode, retain the original full text so printers
  // can reconstruct inter-token trivia exactly.
  readonly fullText?: string;
  readonly children: readonly CstNodeBase[];
}
