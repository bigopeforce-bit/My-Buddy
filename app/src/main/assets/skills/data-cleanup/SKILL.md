---
name: data-cleanup
description: Clean a CSV or table: duplicates, inconsistent formats, missing values and outliers, with a change log.
---

# Data cleanup

Use when the user shares a CSV, spreadsheet export or table and asks to clean it.

1. Inspect before changing anything: row and column counts, types, sample values, and what looks inconsistent.
2. Propose the rules in a short list, then apply them. Do not silently change data.
3. Normalise:
   - dates to one format (ISO `YYYY-MM-DD` unless the user prefers another)
   - whitespace, casing and units
   - split or merge columns only when the pattern is clear
4. Duplicates: show a few examples and the count before removing anything.
5. Missing values: mark them clearly. Do not fill them with a guess unless the user approves the rule.
6. Outliers: list them with the row number and value; do not delete them without approval.
7. Save the cleaned file next to the original, never over it. Add a change log with counts: rows in, rows out, duplicates removed, values fixed.
