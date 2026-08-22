# ImageMediator: обмен фотографиями документов

## Граница ответственности

Мобильные клиенты Android, iOS и Desktop **не подключаются к FTP напрямую**: не используют FTP/FTPS/SFTP URL, логин или пароль. Они вызывают HTTP(S) API сервиса ImageMediator по базовому адресу из `IMAGE_MEDIATOR_BASE_URL`. ImageMediator сам выполняет операции с файловым хранилищем и возвращает клиенту метаданные: путь FTP и/или HTTP URL содержимого.

Во всех запросах используются заголовки:

```http
Authorization: Bearer <1C-токен>
X-ImageMediator-Contract: 1.7
```

`X-ImageMediator-Contract: 1.7` рекомендуется всегда. Отсутствующий или blank header сервер принимает как legacy; явно неизвестная версия возвращает HTTP 400 `unsupported_api_contract`.

`document_name` принимает только одно из значений: `Complects`, `WorkOrder`, `InnerOrder`, `Event`, `Delivery`.

Номер документа сохраняется полностью (без `filter(isDigit)`). После trim, Unicode NFC и uppercase допускаются:

- `^[0-9]{6,12}$`
- `^[А-ЯЁ]{2,3}[0-9]{6,12}$`

Год и месяц берутся только из даты документа, не из часов устройства. Для создания отсутствующей FTP-папки клиент передаёт полный номер, canonical `document_name` и `year/month`.

## Поток загрузки в файловое хранилище

```text
дата документа → year/month → can-upload → POST photos → ImageMediator → FTP
```

Год и месяц берутся только из даты документа (для внутренней заявки — из `ДатаСоздания`), не из часов устройства. При отсутствии даты клиент не вызывает ни один upload-endpoint.

### 1. Проверка возможности загрузки

`POST /api/v1/uploads/can-upload`

Назначение: проверить доступность папки документа и серверные лимиты **до** открытия камеры и перед отправкой пакета.

Заголовки:

```http
Authorization: Bearer <token>
X-ImageMediator-Contract: 1.7
Content-Type: application/json
```

Тело:

```json
{
  "document_number": "0000549041",
  "document_name": "Complects",
  "year": 2025,
  "month": 3
}
```

Ответ `200`:

```json
{
  "allowed": true,
  "document_number": "0000549041",
  "api_version": "1.5",
  "document_type": "Complects",
  "resolved_year": 2025,
  "resolved_month": 3,
  "folder_found": true,
  "folder_path": "/2025/3/0000549041",
  "limits": {
    "max_photos_per_document": 15,
    "photos_in_folder": 4,
    "remaining": 11,
    "max_files_per_request": 10
  }
}
```

Клиент допускает `200`, но не начинает загрузку, если `allowed=false`, `folder_found=false` или `remaining <= 0`. Дополнительно действует локальный лимит: максимум 15 успешно отправленных фото на один документ за один запуск приложения.

### 2. Отправка фотографий

`POST /api/v1/uploads/photos`

Назначение: передать пакет JPEG-файлов в ImageMediator для записи в FTP-папку документа.

Заголовки:

```http
Authorization: Bearer <token>
X-ImageMediator-Contract: 1.7
Idempotency-Key: <UUID-подобный уникальный ключ пакета>
Content-Type: multipart/form-data; boundary=...
```

Multipart-поля:

| Поле | Формат | Обязательно | Описание |
|---|---|---:|---|
| `document_number` | text | да | Нормализованный номер документа |
| `document_name` | text | да | Тип документа (`Complects` и т. п.) |
| `year` | text-число | да | Год даты документа |
| `month` | text-число | да | Месяц даты документа, `1..12` |
| `files[]` | `image/jpeg` | да, ≥1 | Файлы, имена: `photo_0.jpg`, `photo_1.jpg`, … |

`year/month` должны в точности совпадать со значениями из предшествующего `can-upload` для этого пакета. Для одной логической загрузки клиент создаёт стабильный `Idempotency-Key` и повторно использует его при сетевом retry; новый ключ — только для новой операции.

Ограничения клиента: файл не более 5&nbsp;242&nbsp;880 байт (5 MiB); размер пакета — не больше серверного `max_files_per_request` (если лимит отсутствует, не больше 10).

Ответ `200`:

```json
{
  "document_number": "0000549041",
  "resolved_year": 2025,
  "resolved_month": 3,
  "ftp_folder_path": "/2025/3/0000549041",
  "uploaded_files": [
    {
      "original_filename": "photo_0.jpg",
      "stored_filename": "photo.jpg",
      "ftp_file_path": "/2025/3/0000549041/photo.jpg",
      "mime_type": "image/jpeg",
      "size_bytes": 2488120,
      "sha256": "..."
    }
  ]
}
```

`ftp_folder_path` и `ftp_file_path` — метаданные результата; клиент не использует их для прямого FTP-доступа.

## Прочие endpoint’ы ImageMediator

### 3. Количество фотографий документа

`GET /api/v1/documents/{year}/{month}/{document_number}/images/count?document_name={type}`

Назначение: показать количество доступных фотографий. `{document_number}` кодируется как отдельный URL path segment (кириллица percent-encoding), URL целиком не кодируется. Тело запроса отсутствует.

Ответ `200`:

```json
{
  "document_number": "0000549041",
  "resolved_document_number": "0000549041",
  "resolved_year": 2025,
  "resolved_month": 3,
  "count": 27
}
```

HTTP `404` до `can-upload` может означать, что папка ещё не создана; клиент показывает `count=0`. После успешного `can-upload` для нового документа ожидается `count=0`. HTTP `401` не интерпретируется как отсутствие фото.

### 4. Список фотографий документа

`GET /api/v1/documents/{year}/{month}/{document_number}/images?page={page}&document_name={type}`

Назначение: получить постраничный список метаданных для просмотра. `page` начинается с 1. Тело запроса отсутствует. Клиент учитывает `resolved_document_number`, `resolved_year` и `resolved_month`.

Ответ `200`:

```json
{
  "document_number": "0000549041",
  "resolved_year": 2025,
  "resolved_month": 3,
  "page": 1,
  "page_size": 10,
  "total_count": 27,
  "total_pages": 3,
  "has_next": true,
  "has_previous": false,
  "images": [
    {
      "image_id": "img_a83fd912e2c4f719",
      "content_url": "/api/v1/images/img_a83fd912e2c4f719/content",
      "size_bytes": 2488120
    }
  ]
}
```

### 5. Загрузка содержимого фотографии

`GET {content_url}`

Назначение: получить байты фото для просмотра. `content_url` берётся из endpoint’а списка; обычно это `/api/v1/images/{image_id}/content`. Поддерживаются относительный URL ImageMediator и абсолютный URL, который сервис вернул в ответе.

Тело ответа — исходные байты изображения, например с `Content-Type: image/jpeg`. Успешно полученные байты сохраняются в локальном кэше по ключу типа документа, номера и `image_id`.

При `503` клиент выполняет одну повторную попытку после `Retry-After` (минимум 1 секунда; если заголовка нет — через 2 секунды) для чтения контента и для POST can-upload/upload. При retry загрузки используется тот же `Idempotency-Key`. HTTP `429` не повторяется немедленно. Принудительный logout по 503 не выполняется.

## Ошибки и безопасное поведение

| HTTP | Интерпретация клиента |
|---:|---|
| `400` `unsupported_api_contract` | Явно переданная версия контракта не поддерживается |
| `400` | Некорректные поля или формат изображения |
| `401` | AGR-токен отсутствует или отклонён; это не означает отсутствие фото |
| `404` | Папка документа недоступна/не найдена; для count до can-upload → 0 |
| `409` `ambiguous_document_folder` | Нужны полный номер и явный year/month |
| `409` `legacy_document_identity_incomplete` | Нужно дополнить идентичность документа |
| `409` | Конфликт повторной отправки по `Idempotency-Key` |
| `413` | Файл или запрос слишком большой |
| `429` | Лимит; не повторять немедленно |
| `503` | Временная ошибка AGR/FTP/capacity; Retry-After и ограниченный retry |
| `5xx` | Временная ошибка сервиса |

Сетевые ошибки и неаутентификационные HTTP-ошибки не изменяют токен. Прямые FTP-операции из клиента отсутствуют.

## Реализация в клиенте

- HTTP-контракт: `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/remote/ImageMediatorApi.kt`.
- Оркестрация проверок, лимитов и кэша: `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/MainRepository.kt`.
- Контрактные тесты: `composeApp/src/commonTest/kotlin/com/tagaev/trrcrm/data/remote/ImageMediatorApiContractTest.kt`.
