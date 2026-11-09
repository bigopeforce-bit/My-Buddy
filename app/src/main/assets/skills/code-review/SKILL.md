---
name: code-review
description: Review a diff, file or pull request for bugs, edge cases, security and missing tests, most serious first.
---

# Code review

Use when the user asks to review code, a commit, a diff or a pull request.

1. Read the change and the surrounding code. Run the tests or build if the project has them.
2. Report findings in this order:
   - **Blocking**: bugs, data loss, security holes, broken builds.
   - **Should fix**: edge cases, missing error handling, race conditions, wrong assumptions.
   - **Nits**: naming, style, comments. Keep these short.
3. For each finding give: severity, `file:line`, what breaks, how to reproduce it, and a suggested fix.
4. Check specifically: empty and null input, error paths, permissions, input validation, secrets in code, and tests for the new behaviour.
5. Say what the change does well, briefly. Do not pad the review.
6. Do not rewrite the whole file, and do not commit or push without approval. If you make fixes, show the diff first.
