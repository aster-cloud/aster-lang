## Summary
Briefly describe what this PR changes and why.

## Changes
- What: e.g., CI workflow tweaks, JVM check, golden updates
- Where: list key files/paths changed

## CI
- Runs typecheck, lint, format, build, and tests for TypeScript changes
- Runs Gradle build for Java changes (aster-core, aster-asm-emitter)
- Security audit for dependency vulnerabilities

## Checklist
- [ ] CI green (all applicable checks pass)
- [ ] Formatting and lint pass (`npm run format:check`, `npm run lint`)
- [ ] No unrelated reformatting/renames
- [ ] Tests pass (`npm test`)
- [ ] Docs unaffected or updated as needed
- [ ] Changesets added if user‑facing (`npm run changeset`)

## Screenshots / Logs (optional)
Paste relevant logs or screenshots to aid review.

## Related Issues
Closes #<issue>, relates to #<issue>

## Notes for Reviewers
Call out any risky areas or follow‑ups.

