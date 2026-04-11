# 1C Alfa API Reliability Guide

This document captures known reliability rules for `task=getitemslist` and the client behavior required to avoid false "not found" results.

## Incident Summary

- Observed issue: `Комплектация` was not found by number `162795` through API.
- Reproduction request (failed):
  - `...&task=getitemslist&type=Документ&name=Комплектация&ncount=30&count=30&filterby=Номер&filterval=162795`
  - Response: `{"warning":"Empty answers"}`
- Same request with `ncount=0` returned the expected document (`Номер = 0000162795`).

## Confirmed Backend Behavior (`1c_alfa_common_modules.txt`)

- `getitemslist` routes to `ПолучитьСписокСущностей` -> `ПолучитьСписокДокументов`.
- For `filtertype=value`, filtering uses `ПОДОБНО` with `%<filterval>%`.
  - Practical effect: `filterval=162795` matches `0000162795`.
- Pagination offset is applied after query execution:
  - API requests `ПЕРВЫЕ count+ncount` rows.
  - Then skips first `ncount` rows in loop.
  - If only one row matches and `ncount > 0`, result becomes empty.

## Reliability Rules for Clients

1. Reset `ncount` to `0` on every new search/filter/sort apply.
2. Treat search/filter apply as a "new query signature", not "next page".
3. Use only one effective `filterby/filterval` pair in a single request.
4. If both search and department filter are configured, prefer search (explicit user intent).
5. Keep document names exact in Russian (`Событие`, `ЗаказНаряд`, `Комплектация`, etc.).

## Practical Request Patterns

- Safe number search:
  - `task=getitemslist&type=Документ&name=Комплектация&ncount=0&count=30&filterby=Номер&filterval=162795`
- Next page only:
  - Same query signature, increment `ncount` (`30`, `60`, ...).
- After changing any of these:
  - `searchQuery`, `searchQueryType`, `filter`, `filterValue`, `orderBy`, `orderDir`, `state`
  - Always send `ncount=0`.

## Backend Hardening Proposal (Not Implemented Here)

No 1C backend deployment is included in this repository cycle. For future backend hardening:

1. Add query-signature awareness:
  - Track signature from `(type,name,filterby,filterval,filtertype,orderby,orderdir,state,viewtype)`.
  - If signature changed, force `ncount=0` server-side.
2. Add optional strict number mode:
  - For `filterby=Номер`, optionally support exact normalized comparison.
3. Add diagnostics:
  - Include effective query signature and effective `ncount` in debug mode response.

These changes are optional defenses. Primary reliability remains on client-side pagination reset.
