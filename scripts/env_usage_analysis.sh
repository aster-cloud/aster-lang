#!/bin/bash
set -euo pipefail

echo "=== Env Usage Analysis ==="
echo ""
echo "Env.get() calls:"
grep -rn "env\.get(" aster-truffle/src/main/java/ | wc -l
echo ""
echo "Env.set() calls:"
grep -rn "env\.set(" aster-truffle/src/main/java/ | wc -l
echo ""
echo "New Env() creations:"
grep -rn "new Env(" aster-truffle/src/main/java/ | wc -l
