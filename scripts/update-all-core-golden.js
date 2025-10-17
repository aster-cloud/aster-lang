#!/usr/bin/env node
/**
 * Update all Core IR golden test files to match current compiler output.
 * This is needed when the Core IR structure changes (e.g., adding annotations support).
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalize, lex, parse } from '../dist/src/index.js';
import { lowerModule } from '../dist/src/lower_to_core.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.dirname(__dirname);

// Find all .aster files in cnl/examples
const examplesDir = path.join(rootDir, 'cnl', 'examples');
const files = fs.readdirSync(examplesDir).filter(f => f.endsWith('.aster'));

console.log(`Found ${files.length} .aster files in cnl/examples`);

let updated = 0;
let errors = 0;

for (const file of files) {
  const asterPath = path.join(examplesDir, file);
  const baseName = file.replace(/\.aster$/, '');
  const corePath = path.join(examplesDir, `expected_${baseName}_core.json`);

  // Skip if no corresponding expected_*_core.json file exists
  if (!fs.existsSync(corePath)) {
    continue;
  }

  try {
    // Generate new Core IR
    const src = fs.readFileSync(asterPath, 'utf8');
    const can = canonicalize(src);
    const toks = lex(can);
    const ast = parse(toks);
    const core = lowerModule(ast);

    // Remove metadata fields (origin, span, etc.) to match golden file format
    const pruned = JSON.parse(JSON.stringify(core, (key, value) => {
      if (key === 'origin' || key === 'span' || key === 'nameSpan' || key === 'variantSpans') {
        return undefined;
      }
      return value;
    }));

    // Write updated golden file
    fs.writeFileSync(corePath, JSON.stringify(pruned, null, 2) + '\n', 'utf8');
    updated++;
    console.log(`✓ Updated expected_${baseName}_core.json`);
  } catch (e) {
    errors++;
    console.error(`✗ Failed to update expected_${baseName}_core.json:`, e.message);
  }
}

console.log(`\nSummary: ${updated} updated, ${errors} errors`);
process.exitCode = errors > 0 ? 1 : 0;
