# Notification Inbox Backend Agent Prompt

Implement Notification Inbox backend for MobileAgregatCRM with 7-day retention and per-user read/unread state.

## Existing context
- Current core endpoints include `/notifications/intents` and `/notifications/resolve-recipients`.
- Mobile app calls:
  1. `POST /notifications/feed`
  2. `POST /notifications/status/update`
  3. `POST /notifications/status/read-all`

## Required behavior
1. Persist one notification event per recipient whenever `/notifications/intents` is processed (and legacy `/push/thread-message` fallback if applicable).
2. Store fields:
   - `id`, `recipient_user`, `created_at`, `expires_at` (`created_at + 7 days`)
   - `title`, `message_text`, `screen`, `search_key`, `search_query_type`
   - `doc_number`, `doc_title`, `status` (`read|unread`), `read_at`
3. Implement feed endpoint with cursor pagination, search, status filter.
4. Implement status update endpoint.
5. Implement read-all endpoint.
6. Add hourly cleanup job deleting rows where `expires_at < now()`.
7. Keep compatibility:
   - if routing fields missing in incoming intent payload, fallback parse from `title/message_text`
   - map `screen -> search_query_type`:
     - `events`, `work_orders` => `CODE`
     - `complectation` => `KIT_CHARACTERISTIC`
8. Push payload enrichment (best effort):
   include `notification_id`, `screen`, `search_key`, `docTitle`, `message_text` in `data/custom` payload.

## Non-functional
- Add indexes for `(recipient_user, created_at desc)` and `(expires_at)`.
- Return deterministic ordering (newest first).
- Keep existing `/notifications/intents` contract backward compatible.

## API schemas

### 1) `POST /notifications/feed`
Request:
```json
{
  "session_id": "string",
  "limit": 30,
  "cursor": "string|null",
  "search_query": "string|null",
  "status_filter": "all|read|unread"
}
```

Response:
```json
{
  "status": "ok",
  "items": [
    {
      "id": "string",
      "created_at": "2026-05-08T10:12:33Z",
      "expires_at": "2026-05-15T10:12:33Z",
      "title": "Событие МСК0000809 (Москва)",
      "message_text": "Новый комментарий...",
      "screen": "events|work_orders|complectation",
      "search_key": "string|null",
      "search_query_type": "CODE|KIT_CHARACTERISTIC|null",
      "doc_number": "string|null",
      "doc_title": "string|null",
      "status": "read|unread",
      "read_at": "2026-05-08T10:13:00Z|null"
    }
  ],
  "next_cursor": "string|null",
  "unread_count": 12
}
```

### 2) `POST /notifications/status/update`
Request:
```json
{
  "session_id": "string",
  "notification_id": "string",
  "status": "read|unread",
  "source": "manual|open"
}
```

Response:
```json
{
  "status": "ok",
  "notification_id": "string",
  "new_status": "read|unread",
  "read_at": "2026-05-08T10:13:00Z|null"
}
```

### 3) `POST /notifications/status/read-all`
Request:
```json
{
  "session_id": "string"
}
```

Response:
```json
{
  "status": "ok",
  "updated": 17
}
```

## Deliverables
- DB migration(s)
- endpoint handlers + service logic
- validation rules
- tests for feed pagination, search, status updates, and TTL cleanup
- before/after sample JSON for all new endpoints
