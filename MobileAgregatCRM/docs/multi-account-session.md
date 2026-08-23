# Мультиаккаунт, PIN и сессия CRM

Документ описывает функции, добавленные вокруг нескольких аккаунтов на одном устройстве, PIN, блокировки по неактивности и изоляции данных между пользователями.

Основные файлы:

- `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/accounts/`
- `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/accounts/`
- `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/login/CrmAuthUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/ui/root/RootComponent.kt`

## 1. Несколько аккаунтов на устройстве

На устройстве можно сохранить до трёх слотов (`MAX_SAVED_ACCOUNTS = 3`).

- Экран «Аккаунты»: список слотов, текущий, PIN, удаление, «Добавить аккаунт».
- Переключение: PIN (если аккаунтов два и больше) → смена токена и core-сессии → полный рестарт UI-сессии (`restartActiveSession()` через `Login` с сохранённым токеном).
- Кнопка «Назад» на экране аккаунтов делает **тот же рестарт сессии**, что и выбор аккаунта. Нельзя возвращаться `nav.pop()` в старый стек (Настройки / списки документов): там остаются in-memory списки прошлого пользователя.

Активный слот пишется в `ACCOUNTS_JSON_V1` / `ACCOUNTS_ACTIVE_ID_V1`. Токен CRM — в `TOKEN_KEY`, ФИО — в `PERSONAL_DATA`.

## 2. PIN

- Длина: 4 цифры. Хранится как salt + hash, не в открытом виде.
- **Один аккаунт:** PIN необязателен. Можно «Установить ПИН» и «Убрать ПИН» (с подтверждением).
- **Два и больше аккаунтов:** PIN обязателен у каждого слота. Без PIN нельзя продолжить работу (`needsPinGate()`).
- Ввод PIN на переключении — полноэкранная клавиатура (`PinUnlockScreen`) с зазорами между клавишами. Установка нового PIN — компактный диалог.
- «Забыл ПИН»: повторный логин этого слота (`LoginMode.Reauth` + `lockedLogin`), затем PIN слота сбрасывается. При двух и более аккаунтах снова требуется задать PIN.

Лимиты ошибок PIN: предупреждение после 5 неверных, после 10 — блокировка 10 минут, затем 1 час.

## 3. Блокировка по неактивности

- Срабатывает **только если аккаунтов ≥ 2**.
- Таймаут: 1 час (`PIN_INACTIVITY_LOCK_MS`).
- Если после обновления приложения нет метки `ACCOUNTS_LAST_APP_SCREEN_OPENED_MS`, блокировка **не** ставится; метка выставляется при resume.
- При блокировке корневой стек заменяется на экран аккаунтов. Переключение слота с PIN и рестарт сессии — как обычный switch.

Переключение аккаунта и `restartActiveSession()` выполняются на `Dispatchers.Main.immediate`, чтобы Decompose не падал с `NotOnMainThreadException`.

## 4. Запрет дублей ФИО

Одинаковое ФИО (нормализация: trim, схлопывание пробелов, без учёта регистра) **нельзя** сохранить во втором слоте.

- Совпадает **логин** → слот переиспользуется (`Reused`), это не дубль.
- Совпадает только ФИО при другом логине → ошибка на экране логина, слот не создаётся.
- Подразделение в сравнение **не** входит.
- Пустое ФИО не считается совпадением со всеми.
- Уже сохранённые дубли схлопываются при `ensureMigrated()`: остаётся текущий слот (или слот с PIN).

Проверка идёт **сразу после `getToken`**, до записи токена, bootstrap, heartbeat и прочих core-session вызовов (`preflightLoginBind` / `wouldRejectDuplicateFullName`). Иначе core получил бы сессию «лишнего» пользователя.

Сообщение: ключ `accounts_duplicate_identity`.

На экране добавления аккаунта поле логина **пустое** (не подставляется логин текущего пользователя). Режим `LoginMode.Reauth` по-прежнему фиксирует логин слота.

## 5. Изоляция данных между аккаунтами

При switch / выходе с экрана аккаунтов / logout активного слота:

1. Сбрасываются SQL-кэш событий, избранное, refine/фильтры списков, черновик сообщения, отпечаток push.
2. Права сессии (`SessionPermissions`) очищаются.
3. Счётчик непрочитанных уведомлений обнуляется.
4. Сбрасывается process-квота загрузок фото и **кэш просмотренных файлов ImageMediator** (ключ кэша — тип+номер документа, без user id).
5. Инкрементируется `AccountSessionCaches.listGeneration()`: ответ API, начатый ещё предыдущим аккаунтом, в новый список не пишется.
6. Списки документов (события, комплектации, заказ-наряды, грузы и т.д.) при refresh показывают `Loading`, а не старый in-memory массив.
7. Запросы CRM-списков берут токен из `TOKEN_KEY` (`MainRepository.authorizedConfig()`), а не устаревший `ApiConfig.token` в памяти.

Не трогать при switch:

- FCM token устройства
- identity установки
- pending-файлы камеры / `open-docs` (временные файлы открытия)

## 6. Порядок логина (CRM + Core)

Успешный логин по паролю:

1. `getToken` (CRM) — получаем ФИО.
2. Preflight: дубль ФИО / лимит слотов. При отказе токен не пишем, core не трогаем.
3. Запись token / ФИО / подразделение / логин.
4. `getPermission` (CRM).
5. Bind слота.
6. Только потом: push-permission, feature flags, **bootstrap** core, **heartbeat**.

Если bootstrap нового слота упал, созданный слот откатывается, восстанавливается предыдущий.

## 7. Тесты

- `composeApp/src/commonTest/kotlin/com/tagaev/trrcrm/data/accounts/AccountSessionStoreTest.kt`
- `composeApp/src/commonTest/kotlin/com/tagaev/trrcrm/data/accounts/AccountSessionCachesTest.kt`

```bash
./gradlew :composeApp:desktopTest --tests com.tagaev.trrcrm.data.accounts.AccountSessionStoreTest --tests com.tagaev.trrcrm.data.accounts.AccountSessionCachesTest
```
