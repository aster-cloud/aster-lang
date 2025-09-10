## Summary
Briefly describe what this PR changes and why.

## Changes
- What: e.g., CI workflow tweaks, JVM check, golden updates
- Where: list key files/paths changed

## CI
- Ensures Node 18/20/22 matrix runs typecheck, lint, format, build, tests
- Adds JVM emitter verification with Java 17 and `javap`

## Checklist
- [ ] CI green (tests + golden + property)
- [ ] Formatting and lint pass (`npm run format:check`, `npm run lint`)
- [ ] No unrelated reformatting/renames
- [ ] Docs unaffected or updated as needed
- [ ] Changesets added if user‑facing (`npm run changeset`)

## Screenshots / Logs (optional)
Paste relevant logs or screenshots to aid review.

## Related Issues
Closes #<issue>, relates to #<issue>

## Notes for Reviewers
Call out any risky areas or follow‑ups.

