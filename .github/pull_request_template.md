## Summary
- Describe the user-visible change.
- Link the ticket or context if there is one.

## Scope Check
- [ ] I changed only the files required for this task.
- [ ] I reviewed `git diff` and did not include unrelated local changes.
- [ ] I ran `npm run agent:lock-check`, and any locked files touched by this PR are owned by this branch/task.
- [ ] If I needed a lock, I used `npm run agent:lock-acquire` and pushed the branch so other Agents can see it.
- [ ] I pushed the branch before asking for review.

## Architecture Self-Check
- [ ] New business rules introduced by this PR live in a `core/` or `domain/` package; if they remain in a `Service`, I stated the reason.
- [ ] Touched `Service` files moved toward the 300-line budget (or at least did not grow further).
- [ ] No new business exceptions; expected business failures are returned as `Result` / `Optional` values.

## Verification
- [ ] `npm run build`
- [ ] `VITE_API_MODE=api npm run build`
- [ ] If backend changed, the relevant Maven compile/test commands passed.
- [ ] If backend quality-gate config or protected modules changed, I ran the documented quality audit/strict commands and included the result in the PR description.

## UI Evidence
- [ ] Not applicable
- [ ] Screenshots attached
- [ ] Screen recording attached

## Risk Check
- [ ] No schema or contract changes
- [ ] Contains API or schema changes
- [ ] Contains mock-data-only changes
- [ ] Contains quality-gate scope or governance changes

## Rollback
- Describe the fastest rollback path if this PR needs to be reverted.
