This is a Kotlin Multiplatform project targeting Android, iOS.

AI agent work guide: [docs/ai-agents-work-guide.md](./docs/ai-agents-work-guide.md)

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

---

## Feature toggles (CoreService)

Клиентские ключи в `config.flags` / `GET /feature-toggles/mobile` — **без** префикса `feature_toggle.mobile.`.  
Если ключ отсутствует → **выключено** (`false`). Сервер должен явно прислать `true`, чтобы включить.

Store: `MobileFeatureFlagsStore`. Лог ON/OFF каждого ключа только при `IS_PUBLISH=true`.

### Когда приложение запрашивает / применяет toggles

| Когда | Что происходит |
|-------|----------------|
| **После успешного логина** | Явный `GET /feature-toggles/mobile` (`MobileFeatureFlagsSync`, reason=`login`) |
| **После успешного логина** | `GET /feature-toggles/push-notifications` (push toggle, force) |
| **Bootstrap** `POST /core/session/bootstrap` | Если в ответе есть непустой `config.flags` — apply в store (без отдельного GET) |
| **Heartbeat** `POST /core/session/heartbeat` | То же: непустой `config.flags` → apply |
| **Регистрация push** | `refreshPushFeatureToggleIfNeeded` (с TTL-кэшем), не каждый раз ходит на сервер |

**Не** вызывается на каждый foreground/resume приложения.

Пустой / отсутствующий `config.flags` **не** стирает уже сохранённый кэш.

### Mobile photo toggles

| Клиентский ключ | Документы | Эффект |
|-----------------|-----------|--------|
| `photos_upload_work_orders_etc` | Заказ-наряд | Только **загрузка** фото |
| `photos_download_work_orders_etc` | Заказ-наряд | Только **просмотр** / count / list |
| `photos_inner_order` | Внутренняя заявка | Upload **и** viewer |
| `photos_events` | Событие | Upload **и** viewer |
| `photos_cargo` | Доставка (Cargo → wire `Delivery`) | Upload **и** viewer |
| — | Комплектация | Всегда включено, серверными photo-toggles не гейтится |

При выключенном toggle:

- UI (кнопка камеры / «Открыть фотографии») скрывается;
- **не** вызываются ImageMediator-эндпоинты count / can-upload / list / download для этого типа (`DocumentPhotoSession` early-return).

### Push toggle

| Клиентский путь | Эффект |
|-----------------|--------|
| `GET/POST /feature-toggles/push-notifications` | Вкл/выкл push; кэш в prefs с TTL |

### Серверные имена (справочно)

На сервере ключи могут жить как `feature_toggle.mobile.<key>`; в JSON для мобильного клиента отдавать **короткие** ключи из таблицы выше.