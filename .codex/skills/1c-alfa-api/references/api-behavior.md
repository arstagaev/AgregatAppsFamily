# 1C Alfa API Behavior Reference

This reference summarizes stable behavior used by the app integration.

## Core Endpoint Routing

- `task=getitemslist` -> list entities/documents.
- `task=getitem` -> get one entity.
- `task=setmessage` -> append message to document thread.
- `task=getroles` -> fetch user roles by token.
- `task=getqrcomlectinfo` -> QR lookup.

## `getitemslist` Notes

- `type=Документ` + `name=<ДокументИмя>` is required for document lists.
- `count` controls page size.
- `ncount` is offset (not page index).

### Filtering

- For `filtertype=value`, backend query uses `ПОДОБНО` with `%filterval%`.
- Number searches can match padded numbers (`162795` -> `0000162795`).

### Pagination

- Backend fetches up to `count + ncount`.
- Then skips first `ncount` rows in iteration.
- Result: valid match can disappear if `ncount` is non-zero on new query.

## Client Reliability Contract

1. Reset `ncount=0` when query signature changes.
2. Increment `ncount` only for "Load more" with unchanged query signature.
3. Send one effective `filterby/filterval` pair:
   - search pair when search text is present
   - otherwise fallback filter (for example, department).

## Fast Incident Triage

1. Run reported URL as-is.
2. Re-run with `ncount=0`.
3. Compare request builders in client to ensure:
   - no stale offset at refresh
   - no duplicate `filterby/filterval` assignment.

If step 2 fixes the issue, treat it as pagination-state bug first.
