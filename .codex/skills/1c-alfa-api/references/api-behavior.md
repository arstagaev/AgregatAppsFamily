# 1C Alfa API Behavior Reference

This reference summarizes stable behavior used by the app integration, aligned with the repository export [`1c_alfa_common_modules.txt`](../../../../1c_alfa_common_modules.txt) (search `ОбработкаДляПриложенияGet`, `ПолучитьСписокСущностей`, `ПолучитьСписокДокументов`).

## Core Endpoint Routing (`ОбработкаДляПриложенияGet`)

- `task=gettoken` — login; **no** prior token check.
- `task=getmetadata` — `ПолучитьМетаДатуБазы`.
- `task=getitemslist` — `ПолучитьСписокСущностей` (dispatches on `type`).
- `task=getitem` — `ПолучитьСущность`.
- `task=setmessage` — `ДобавитьСообщениеВПереписку` (needs `type`, `name`, `number`, `date`, `message`; finds row by exact `Номер` + day of `date`).
- `task=getqrcomlectinfo` — `ПолучитьДанныеКомплектацииПоQR`.
- `task=getroles` — `ПолучитьРолиПользователя`.

POST body is URL-decoded JSON and forwarded through the same GET handler.

## `getitemslist` Notes

- `type` + `name` must match metadata (`Документ.<name>`, `Справочник.<name>`, etc.).
- **`count`** for Справочник/Документ: coerced to **1..100** (missing/0/>100 → 100).
- **`ncount`** is an **offset into the ordered result**, not a page index.

### Filtering

- Default `filtertype` is `value`: backend uses **`ПОДОБНО`** with parameter **`%filterval%`** (catalog-style refs use `.Наименование`).
- Other modes in document lists include `code`, `bool`, `int`, and `list` (latter specialized for `Событие` / `Состояние`).
- Invalid `filterby` / `orderby` → warning structures (`No item filterby requisite`, `No item orderby requisite`).

### Pagination (important)

- **Документ (`ПолучитьСписокДокументов`):** query uses **`ПЕРВЫЕ count + ncount`**, then the server **skips the first `ncount` rows** when building the response. A single match disappears if `ncount > 0`.
- **Справочник (`ПолучитьСписокСправочников`):** different SQL shape — **`ПЕРВЫЕ count`** with **`НЕ В (ВЫБРАТЬ ПЕРВЫЕ ncount ...)`** subquery. Same **client rule:** reset `ncount` when anything in the query signature changes.

### Extra restrictions (documents)

- `Событие`: role-based and optional `viewtype=onlymy` visibility.
- `ЗаказНаряд`: department scoping unless user has **«Полные права»**.
- `state=активно` / `неактивно`: extra predicates for `Событие` / `ЗаказНаряд`.

## Client Reliability Contract

1. Reset `ncount=0` when query signature changes (`type`, `name`, filters, sort, `state`, `viewtype`, search).
2. Increment `ncount` only for "load more" with an **unchanged** signature.
3. Send one effective `filterby`/`filterval` pair (do not assume multiple simultaneous filters).

## Fast Incident Triage

1. Run reported URL as-is.
2. Re-run with `ncount=0`.
3. Compare request builders in client to ensure:
   - no stale offset at refresh
   - no duplicate or conflicting filter assignment
4. If the list is `Событие` / `ЗаказНаряд`, check server-side visibility (`state`, roles, departments).

If step 2 fixes the issue, treat it as pagination-state bug first.
