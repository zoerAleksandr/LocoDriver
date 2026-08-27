# Задание: голосовой и текстовый помощник для заполнения данных

## Контекст проекта

Приложение — учёт рабочих рейсов машиниста (Kotlin Multiplatform, Android + iOS).
- Shared-логика: **Compose Multiplatform / KMP**, весь общий код в `commonMain`.
- iOS UI: **нативный SwiftUI**, Android UI: **Compose**.
- Backend: Python/FastAPI (для этой задачи не трогаем, кроме опционального прокси к LLM — см. ниже).

### Жёсткие архитектурные ограничения (не нарушать)
- В `commonMain` **запрещены** импорты `java.*`, `android.*`, `UIKit.*`, `platform.*`.
- Всё платформенно-зависимое — только через `expect/actual`.
- Цвета — только через `MaterialTheme.colorScheme`, без хардкода.
- Не создавать параллельную бизнес-логику: помощник должен вызывать **существующие UseCase/Repository**, а не писать свои пути записи в БД.

---

## Цель

Пользователь голосом или текстом отдаёт команду вида:
- «создай новый маршрут и добавь туда локомотив ВЛ10 №123»
- «установи время приёмки локомотива согласно нормам»
- «добавь станцию Тверь, прибытие 14:30»

Приложение распознаёт команду, заполняет соответствующие модели данных и **озвучивает результат** (TTS).

---

## ШАГ 0 — обязательный: разведка репозитория

**Не приступай к коду, пока не изучишь проект.** Сначала выполни и проанализируй:

1. Найди структуру shared-модуля и source set'ы:
   ```
   find . -type d -name commonMain
   find . -path "*commonMain*" -name "*.kt" | head -80
   ```
2. Найди модели данных маршрута: `Route`, `BasicData`, `Locomotive`, `Train`, `Station`, `Passenger`, `UserSettings`, `SalarySetting`.
   - Обрати внимание: модели уже существуют. Изучи их реальные поля (например, `BasicData.timeStartWork/timeEndWork: Long?`, `Locomotive.series/number/type`, `Locomotive.timeStartOfAcceptance/timeEndOfAcceptance`, `Train.stations`, `Station.timeArrival/timeDeparture`).
3. Найди слой домена и данных:
   ```
   find . -path "*commonMain*" \( -iname "*usecase*" -o -iname "*repository*" -o -iname "*interactor*" \)
   ```
   Составь список UseCase-ов, отвечающих за **создание/редактирование маршрута, добавление локомотива, добавление поезда/станции, чтение UserSettings**.
4. Найди 1–2 ViewModel редактирования маршрута — понять, как сейчас данные доходят от UI до репозитория (какой DI, корутины/Flow, как сохраняется `Route`).
5. Определи DI-фреймворк (Koin / Kodein / ручной) — новые классы регистрировать так же.

**Выведи краткий отчёт** (модели, UseCase-ы, DI, точки входа сохранения) перед тем как писать код. Если чего-то из списка нет — адаптируйся к реальной структуре, а не выдумывай.

---

## ШАГ 1 — Архитектура помощника

Слои (сверху вниз):

```
Платформа (STT)  ──expect/actual──►  SpeechRecognizer
                                          │ текст
                                          ▼
                                     CommandParser  (гибрид: правила + LLM fallback)
                                          │ VoiceCommand (sealed)
                                          ▼
                                     CommandExecutor  → существующие UseCase
                                          │ результат (текст)
                                          ▼
Платформа (TTS)  ◄──expect/actual──  SpeechSynthesizer
```

### 1.1 STT — `expect/actual`
`commonMain`:
```kotlin
interface SpeechRecognizer {
    val state: StateFlow<RecognizerState>
    suspend fun startListening(languageTag: String = "ru-RU")
    fun stopListening()
}

sealed interface RecognizerState {
    data object Idle : RecognizerState
    data object Listening : RecognizerState
    data class PartialResult(val text: String) : RecognizerState
    data class FinalResult(val text: String) : RecognizerState
    data class Error(val code: Int, val message: String) : RecognizerState
}
```
- **androidMain**: `android.speech.SpeechRecognizer` + `RecognizerIntent`. Обработать разрешение `RECORD_AUDIO` (проверку разрешения оставить UI-слою, интерфейс лишь сообщает ошибку).
- **iosMain**: `SFSpeechRecognizer` + `AVAudioEngine`. Разрешения `NSSpeechRecognitionUsageDescription` и `NSMicrophoneUsageDescription` — добавить в Info.plist (указать это в отчёте).
- Ошибки нормализовать в человекочитаемый русский текст + числовой код (согласовать с уже принятым в проекте подходом к кодам ошибок — если для сетевых используется `-1`/`0`, придерживаться той же логики).

### 1.2 Модель команды — `commonMain`
```kotlin
sealed interface VoiceCommand {
    data class CreateRoute(val number: String?) : VoiceCommand
    data class AddLocomotive(
        val series: String,
        val number: String?,
        val type: LocoType? = null
    ) : VoiceCommand
    data class SetAcceptanceTimeByNorm(val locoIndex: Int? = null) : VoiceCommand
    data class AddStation(
        val name: String,
        val arrival: LocalTime? = null,
        val departure: LocalTime? = null
    ) : VoiceCommand
    data class SetWorkTime(val start: LocalTime?, val end: LocalTime?) : VoiceCommand
    data class Composite(val commands: List<VoiceCommand>) : VoiceCommand   // «создай маршрут И добавь локомотив»
    data class Unknown(val rawText: String) : VoiceCommand
}
```
> Для времени использовать `kotlinx-datetime` (`LocalTime`), **не** `java.time` и **не** `java.util.Calendar` — они недоступны в iOS-таргете commonMain.

### 1.3 Парсер — гибрид
```kotlin
interface CommandParser {
    suspend fun parse(text: String): VoiceCommand
}
```
Реализация `HybridCommandParser`:
1. Сначала `RuleBasedParser` — regex/шаблоны по-русски для набора команд из v1 (см. Scope). Работает офлайн, мгновенно, детерминированно.
2. Если правила вернули `Unknown` **и** есть сеть — `LlmCommandParser` (fallback).

`RuleBasedParser` — учесть русскую морфологию и профтерминологию:
- серии локомотивов: `ВЛ10`, `ВЛ80`, `2ЭС6`, `3ЭС5К`, `ЧС7` и т.п. (regex по паттерну + список из `UserSettings.locomotiveSeriesList`, если он есть);
- номер: `№?\s*(\d+)`;
- «согласно нормам / по нормам / по нормативу» → взять норматив из `UserSettings`;
- союзы «и», «а также», «потом» → разбить на `Composite`.

`LlmCommandParser`:
- Отправляет текст на **backend-прокси** (ключ LLM держать на сервере, не в клиенте — как с ckassa/секретами).
- Промпт просит вернуть **строго JSON** по схеме intent+entities, без пояснений и markdown.
- Ответ парсить безопасно (`kotlinx.serialization`), при ошибке парсинга → `Unknown`.
- Таймаут + нормализация сетевых ошибок в русский текст (не показывать stacktrace, оригинал — в Sentry).

### 1.4 Исполнитель
```kotlin
class CommandExecutor(/* существующие UseCase через DI */) {
    suspend fun execute(command: VoiceCommand): ExecutionResult
}

data class ExecutionResult(
    val success: Boolean,
    val spokenFeedback: String,   // для TTS и для показа в UI
    val affectedRouteId: String? = null
)
```
- Для `SetAcceptanceTimeByNorm` — прочитать норматив из `UserSettings`, вычислить `timeStartOfAcceptance/timeEndOfAcceptance` относительно `BasicData.timeStartWork`.
- `Composite` — выполнять по порядку, накапливая контекст (созданный маршрут → в него добавляется локомотив).
- **Никаких прямых обращений к БД** — только через UseCase/Repository, найденные на Шаге 0.

### 1.5 TTS — `expect/actual`
`commonMain`:
```kotlin
interface SpeechSynthesizer {
    suspend fun speak(text: String, languageTag: String = "ru-RU")
    fun stop()
}
```
- androidMain: `android.speech.tts.TextToSpeech`.
- iosMain: `AVSpeechSynthesizer` + `AVSpeechUtterance`.

### 1.6 Оркестрация — `commonMain`
`VoiceAssistantViewModel` (или под стиль проекта), связывающий: STT → Parser → Executor → TTS, с состоянием для UI:
```kotlin
sealed interface AssistantUiState {
    data object Idle
    data object Listening
    data class Recognized(val text: String)
    data object Processing
    data class Done(val feedback: String)
    data class Failed(val message: String)
}
```
Ввод и голосом (STT), и текстом (пользователь набрал команду вручную) — оба пути идут в один `parse → execute`.

---

## Scope первой версии (минимум, но надёжно)

Реализовать и покрыть тестами **только** эти команды (правилами, без LLM):
1. `CreateRoute` — «создай/новый маршрут [номер N]»
2. `AddLocomotive` — «добавь локомотив <серия> [№ номер]»
3. `SetAcceptanceTimeByNorm` — «установи время приёмки по нормам»
4. `Composite` из (1)+(2) — «создай маршрут и добавь локомотив ВЛ10 №123»

`AddStation` и `SetWorkTime` — заложить в sealed-модель и парсер интерфейсно, но реализацию можно пометить `TODO` для следующей итерации. LLM-fallback подключить инфраструктурно, но допускается заглушка прокси-эндпоинта с TODO, если backend ещё не готов.

---

## Тесты (обязательно)

В `commonTest`:
- Юнит-тесты `RuleBasedParser` на все команды v1, включая:
  - варианты формулировок и падежей («создай маршрут», «новый маршрут», «заведи маршрут»);
  - разбор серии и номера локомотива (`ВЛ10 №123`, `вл 10 номер 123`);
  - `Composite` с союзом «и»;
  - мусорный ввод → `Unknown`.
- Тест `CommandExecutor` с мок-UseCase: проверить, что вызываются правильные UseCase с правильными аргументами и что `Composite` сохраняет контекст маршрута.
- Парсер и исполнитель не должны зависеть от платформы (запускаться на JVM-тесте commonTest).

---

## Что НЕ делать
- Не тянуть тяжёлые NLP-библиотеки на устройство.
- Не хранить ключи LLM в клиенте.
- Не дублировать бизнес-логику записи данных.
- Не использовать `java.time`, `java.util.Calendar`, `android.*`, `UIKit.*` в `commonMain`.
- Не менять существующие модели данных без явной необходимости; если поле реально отсутствует — сообщить в отчёте, а не молча добавлять.

---

## Порядок сдачи
1. Создать ветку `feature/voice-assistant`.
2. Отчёт по Шагу 0 (структура, UseCase-ы, DI).
3. Реализация shared-слоя (модель, парсер, исполнитель, интерфейсы STT/TTS, VM) + тесты.
4. `actual`-реализации Android и iOS + заметка по разрешениям (AndroidManifest / Info.plist).
5. Краткая инструкция по подключению UI (кнопка микрофона + поле текстовой команды).

Комментарии в коде — на русском.
