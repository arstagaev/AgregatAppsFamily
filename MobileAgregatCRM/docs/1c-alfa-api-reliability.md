# 1C Alfa API Reliability Guide

This document captures known reliability rules for the Alfa HTTP API and the client behavior required to avoid false "not found" or empty-page results.

**Source of truth for server-side logic:** repository root [`1c_alfa_common_modules.txt`](../../1c_alfa_common_modules.txt) (1C common-module export). When behavior is unclear, search that file for `ОбработкаДляПриложенияGet`, `ПолучитьСписокСущностей`, and `ПолучитьСписокДокументов`.

## HTTP entry points

- `ОбработкаДляПриложенияGet(ДанныеЗапроса)` — primary router (JSON query params).
- `ОбработкаДляПриложенияPost` — URL-decodes the body, parses JSON, then calls the same `ОбработкаДляПриложенияGet` pipeline.

## Authenticated `task` values

All tasks below except `gettoken` run **after** `ПолучитьСотрудникаПоТокену(token)`; missing or invalid token returns structured errors (`Token authentification error`, `No token`, etc.).

| `task` | Handler (1C) | Notes |
|--------|----------------|------|
| `gettoken` | `ПолучитьДанныеЛогинуПаролю` | Runs **before** token validation; uses `user` / `pass`. |
| `getmetadata` | `ПолучитьМетаДатуБазы` | Requires token. |
| `getitemslist` | `ПолучитьСписокСущностей` | Requires token; see next section. |
| `getitem` | `ПолучитьСущность` | Routed in dump; implementation may be incomplete in export—verify in live configuration. |
| `setmessage` | `ДобавитьСообщениеВПереписку` | Requires `type`, `name`, `number`, `date`, `message`; resolves document by **exact** `Номер` and **calendar day** of `date`. |
| `getqrcomlectinfo` | `ПолучитьДанныеКомплектацииПоQR` | Requires token. |
| `getpermission` | *(см. конфигурацию 1С)* | Requires token. Ответ — JSON-массив объектов `{ "permission": "...", "value": "..." }` (права сессии). Клиент: `EventsApi.getPermission` / `SessionPermissions`. |
| `getroles` | `ПолучитьРолиПользователя` | Legacy; мобильный клиент переведён на `getpermission`. |

Unknown `task` → `{"warning":"Unknown task"}`. Exceptions inside the task branch → `warning` / `warningText` (e.g. `Task settings fault`).

## `getitemslist` — `ПолучитьСписокСущностей`

Dispatcher reads `type` and `name`, then calls:

- `type=Справочник` → `ПолучитьСписокСправочников`
- `type=Документ` → `ПолучитьСписокДокументов`
- `type=Перечисление` → `ПолучитьСписокПеречислений` (stub-like in dump: parses `ncount`/`count` then returns empty array)
- `type=РегистрСведений` → `ПолучитьСписокРС` (same pattern in dump)
- `type=Константа` → `ПолучитьСписокКонстант` (returns all constants; not paginated in the usual sense)

Unrecognized `type` → `Not recognized item type` / `No item type` warnings.

### Page size (`count`)

For **Справочник** and **Документ**, if `count` is missing, zero, or greater than **100**, the server forces **`count = 100`**. Clients should stay within that cap.

### Filtering (`filterby` / `filterval` / `filtertype`)

Default `filtertype` is **`value`**. When `filterval` is set:

| `filtertype` | Document list (`ПолучитьСписокДокументов`) | Typical meaning |
|--------------|--------------------------------------------|-----------------|
| `value` | `ПОДОБНО` with parameter **`%filterval%`** (reference attributes use `.Наименование`) | Substring / "contains" match; numeric fragments can match padded numbers (e.g. `162795` inside `0000162795`). |
| `code` | `... .Код = &ПарамФильтра` | Exact catalog code. |
| `bool` | `= Ложь` / `= Истина` for recognized Russian / `0` / `1` literals | Boolean requisites. |
| `int` | `=` comparison | Exact (non-substring) match path. |
| `list` | Special case: `name=Событие` + `filterby=Состояние` maps enum member to `ЗНАЧЕНИЕ(Перечисление.СостоянияСобытий....)` | Not a free-text list filter. |

Справочник lists use the same `filtertype` idea for `ПОДОБНО` / `code` / `bool` / `int` where applicable (see module text around `ПолучитьСписокСправочников`).

`filterby` must be a valid standard or user attribute of the metadata object; otherwise the API returns **`No item filterby requisite`**.

### Sorting (`orderby` / `orderdir`)

- **Документ:** default sort field **`Дата`**, direction **`УБЫВ`**. `orderdir` **`ASC`/`asc`** switches to ascending; otherwise descending behavior remains.
- **Справочник:** default **`Наименование`** / **`ВОЗР`**; `DESC`/`desc` requests descending.

Invalid `orderby` → **`No item orderby requisite`**.

### Pagination — **two different implementations**

1. **`ПолучитьСписокДокументов` (documents)**  
   - Query uses **`ПЕРВЫЕ (count + ncount)`** rows (variable `КолвоВыборкиСтарт`).  
   - After execution, the code **skips the first `ncount` rows** in a `Пока Выборка.Следующий()` loop, then serializes the rest (up to `count`).  
   - **Effect:** if exactly one row matches and `ncount > 0`, the client sees **`Empty answers`** even though the row exists.

2. **`ПолучитьСписокСправочников` (catalogs)**  
   - Uses a **`НЕ ... В (ВЫБРАТЬ ПЕРВЫЕ ncount ... УПОРЯДОЧИТЬ ПО ...)`** subquery to exclude the first `ncount` ordered references, then **`ПЕРВЫЕ count`** on the outer select.  
   - **Effect:** changing filters/search while reusing a non-zero `ncount` can still empty the page or shift windows unexpectedly. **Always reset `ncount` when the query signature changes** (same operational rule as for documents).

### Document-specific server filters (not overridden by client `filterby`)

These are applied inside `ПолучитьСписокДокументов` in addition to optional `filterby` / `state`:

- **`Событие`:** users without roles **«Все события»** or **«Полные права»** are restricted to their own events (author / participants). If `viewtype=onlymy`, the same style restriction applies even for privileged roles.
- **`ЗаказНаряд`:** users without **«Полные права»** get an extra **`ПодразделениеКомпании В (...)`** restriction from the user’s allowed departments.
- **`state`:** for `Событие` and `ЗаказНаряд`, `state=активно` / `неактивно` appends fixed condition blocks on states (see module).

Treat these as part of the **effective query signature** when debugging "missing" rows.

## Incident Summary (documents)

- Observed issue: `Комплектация` was not found by number `162795` through API.
- Reproduction request (failed):
  - `...&task=getitemslist&type=Документ&name=Комплектация&ncount=30&count=30&filterby=Номер&filterval=162795`
  - Response: `{"warning":"Empty answers"}`
- Same request with `ncount=0` returned the expected document (`Номер = 0000162795`).

This matches **`filtertype=value`** → `ПОДОБНО %162795%` **plus** pagination skip when `ncount` is non-zero.

## Reliability Rules for Clients

1. Reset **`ncount` to `0`** on every new search, filter, sort, `state`, or `viewtype` apply (and when switching list `type` / `name`).
2. Treat search/filter apply as a **new query signature**, not "next page".
3. Use only **one** effective `filterby` / `filterval` pair per request (avoid stacking client-side filters that the API cannot express as a single pair).
4. If both search and department-style filtering matter, prefer the **explicit user search** pair when the API only supports one active filter pair.
5. Keep metadata names exact (`Событие`, `ЗаказНаряд`, `Комплектация`, etc.) and use correct Russian `type` literals (`Документ`, `Справочник`, ...).

## Practical Request Patterns

- Safe number search (documents):
  - `task=getitemslist&type=Документ&name=Комплектация&ncount=0&count=30&filterby=Номер&filterval=162795`
- Next page only:
  - Same query signature, increment `ncount` (`30`, `60`, ...).
- After changing any of these, always send **`ncount=0`**:
  - `searchQuery`, `searchQueryType`, `filter`, `filterValue`, `filtertype`, `orderBy`, `orderDir`, `state`, `viewtype`, `type`, `name`

## Backend Hardening Proposal (Not Implemented Here)

No 1C backend deployment is included in this repository cycle. For future backend hardening:

1. Add query-signature awareness:
   - Track signature from `(type, name, filterby, filterval, filtertype, orderby, orderdir, state, viewtype, …)`.
   - If signature changed, force `ncount=0` server-side.
2. Add optional strict number mode:
   - For `filterby=Номер`, optionally support exact normalized comparison.
3. Add diagnostics:
   - Include effective query signature and effective `ncount` in debug mode response.

These changes are optional defenses. Primary reliability remains on client-side pagination reset and awareness of document-only pagination mechanics.
