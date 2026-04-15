---
name: 1c-alfa-api
description: Diagnose and stabilize integrations with 1C Alfa API endpoints used by this repository. Use when troubleshooting getitemslist/getitem/setmessage/getroles/getqrcomlectinfo behavior, search misses, pagination offsets (ncount/count), filter precedence, or when designing reliable client request patterns against agrapp.agregatka.ru.
---

# 1C Alfa API Skill

Follow this workflow for incidents and implementation tasks related to the 1C Alfa API.

## Workflow

1. Reproduce the issue with the exact URL/params from the report.
2. Re-run the same request with `ncount=0` to check offset-related misses.
3. Verify if more than one `filterby/filterval` pair is being sent by client code.
4. Check client pagination reset behavior:
   - `ncount` must reset to `0` on new search/filter/sort.
5. Confirm backend behavior in the repo export `1c_alfa_common_modules.txt` (repository root):
   - `ОбработкаДляПриложенияGet` task table (`gettoken` … `getroles`)
   - `getitemslist` → `ПолучитьСписокСущностей` → branch by `type`
   - **Документ:** `ПЕРВЫЕ count+ncount` + skip first `ncount` rows in the selection loop
   - **Справочник:** `NOT IN (SELECT ПЕРВЫЕ ncount …)` + outer `ПЕРВЫЕ count` (different SQL; same reset rule)
   - `filtertype=value` → `ПОДОБНО` with `%filterval%`
6. Propose the smallest safe fix:
   - client-side reset first
   - backend hardening only as optional defense

## Reliability Rules

- Treat query changes as a new query signature and start from page 0.
- Prefer explicit search over implicit department filter when API supports one active filter pair.
- Keep document names exact and in Russian.
- Avoid contract changes unless explicitly requested.

## Output Checklist

- Root cause statement (client vs API behavior).
- Reproduction matrix (`ncount>0` vs `ncount=0`).
- Proposed fix with low regression risk.
- Validation steps and expected results.

## References

- Read [API behavior reference](references/api-behavior.md) for known endpoint semantics and troubleshooting shortcuts.
- Cross-check behavior against [`1c_alfa_common_modules.txt`](../../../1c_alfa_common_modules.txt) at the repository root when triaging server-side edge cases.
