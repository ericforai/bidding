## Summary
- Describe the user-visible change.
- Link the ticket or context if there is one.

## Scope Check
- [ ] I changed only the files required for this task.
- [ ] I reviewed `git diff` and did not include unrelated local changes.
- [ ] I pushed the branch before asking for review.

## Architecture Self-Check
- [ ] New business rules introduced by this PR live in a `core/` or `domain/` package; if they remain in a `Service`, I stated the reason.
- [ ] Touched `Service` files moved toward the 300-line budget (or at least did not grow further).
- [ ] No new business exceptions; expected business failures are returned as `Result` / `Optional` values.

## Verification
- [ ] `npm run build`
- [ ] `VITE_API_MODE=api npm run build`
- [ ] If backend changed, the relevant Maven compile/test commands passed.

## UI Evidence
- [ ] Not applicable
- [ ] Screenshots attached
- [ ] Screen recording attached

## Risk Check
- [ ] No schema or contract changes
- [ ] Contains API or schema changes
- [ ] Contains mock-data-only changes

## Rollback
- Describe the fastest rollback path if this PR needs to be reverted.
