# Known Issues & Performance Bugs — sloosh Android TV

Этот файл содержит полный список выявленных проблем производительности, багов навигации фокуса и архитектурных недочётов, обнаруженных в ходе глубокого аудита проекта.

---

## 🚨 КРИТИЧЕСКИЕ ПРОБЛЕМЫ ПРОИЗВОДИТЕЛЬНОСТИ — ВСЕ УСТРАНЕНЫ ✅

### 0. [ИСПРАВЛЕНО] Лаг, двойная анимация, серый фон, навигация вправо, центрирование кнопок и нативная обводка карточек
- **Файлы**: `MainActivity.kt`, `ui/components/SlooshSideDrawer.kt`, `ui/components/SlooshFocusableCard.kt`, `ui/home/HomeScreen.kt`, `ui/search/SearchScreen.kt`, `ui/continue_watching/ContinueScreen.kt`, `ui/profile/ProfileScreen.kt`, `ui/settings/SettingsScreen.kt`
- **Суть проблемы**:
  1. Боковая панель переключалась через `ModalNavigationDrawer`, вызывая двойную конфликтующую анимацию смещения и расширения ширины.
  2. Панель имела собственный серый фон `Color(0xFF0C0C0E)` и обводку, выделяясь на черном фоне экрана.
  3. Невозможно было выйти из панели нажатием кнопки Вправо (D-pad Right) — закрытие работало только по кнопке Назад. Это происходило потому, что `focusBridge.contentFocusRequester` не был подключен к экранам, а в обработчике `KEYCODE_DPAD_RIGHT` не вызывалось закрытие панели и переключение фокуса.
  4. Кнопки навигации («Главная», «Поиск», «Продолжить», «Избранное») находились вверху, в то время как по эргономике TV они должны быть в центре экрана.
  5. Карточки контента использовали тяжелый непрерывный шейдер светового луча (`drawWithContent`, `PathMeasure`, тригонометрические расчеты), который переливался и нагружал процессор приставки.
- **Решение**:
  1. `ModalNavigationDrawer` полностью устранен. Панель встроена в единый 레이аут с плавной физикой `animateDpAsState(72.dp -> 210.dp)`.
  2. В свернутом виде фон прозрачный, в открытом — `BackgroundDark` (`#050505`) с затемнением контента на 60%. Убран текст "sloosh" из шапки.
  3. В `SideDrawerFocusBridge` реализован механизм `contentFocusCallback` и `requestContentFocus()`. Каждый из 5 экранов (`HomeScreen`, `SearchScreen`, `ContinueScreen`, `ProfileScreen`, `SettingsScreen`) регистрирует свой целевой фокус через `DisposableEffect`. При нажатии D-pad Right, Enter или клика панель немедленно закрывается, а фокус гарантированно передается на карточки или поисковую строку контента. На `ContinueScreen` в пустом состоянии добавлена интерактивная кнопка "В каталог".
  4. Основной блок навигации («Главная», «Поиск», «Продолжить», «Избранное») выровнен строго по вертикальному центру боковой панели (через `Spacer(weight = 1f)` сверху и снизу), а кнопка «Настройки» зафиксирована в самом низу. Настроены плавные сквозные переходы D-pad вверх/вниз между «Избранным» и «Настройками».
  5. В `SlooshSideDrawer` добавлен внешний обработчик потери фокуса `.onFocusChanged { if (!it.hasFocus && isOpen) onOpenChanged(false) }`, гарантирующий автоматическое сворачивание панели при любом уходе фокуса в контент.
  6. В `SlooshFocusableCard` полностью вырезан кастомный шейдер светового луча и заменен на стандартную чистую нативную обводку Compose TV `CardDefaults.border(focusedBorder = Border(BorderStroke(2.5.dp, Color.White), shape))`. Рендеринг мгновенный, нулевая нагрузка на процессор.
  7. В `SettingsScreen` переключатели `ExpressiveSwitch` и сегменты `SegmentedToggle` снабжены явным модификатором `.focusable()` для идеального управления с пульта.

### 1. [ИСПРАВЛЕНО] `SlooshFocusableCard` — Бесконечная анимация на всех 30–50 карточках сетки
- **Файл**: `ui/components/SlooshFocusableCard.kt`
- **Решение**: `rememberInfiniteTransition` заменен на `Animatable(0f)` с запуском в `LaunchedEffect(isFocused)`. Анимация светового луча работает строго для сфокусированной карточки, нулевая нагрузка на CPU для всех остальных карточек.

### 2. [ИСПРАВЛЕНО] `Brush` и `arrayOf(colorStops)` создаются на каждый кадр в Draw Scope
- **Файлы**: `ui/details/DetailsScreen.kt`, `ui/continue_watching/ContinueScreen.kt`, `ui/home/HomeScreen.kt`
- **Решение**: Все градиенты обернуты в `remember { Brush... }`.

### 3. [ИСПРАВЛЕНО] Disk I/O (SharedPreferences) прямо в теле Composable-функций
- **Файлы**: `ui/search/SearchScreen.kt`, `ui/profile/ProfileScreen.kt`
- **Решение**: `appSettings` и `gridColumns` вынесены на верхний уровень за пределы `AnimatedContent`.

### 4. [ИСПРАВЛЕНО] Раздельные кэши между экранами в `MoviesRepository`
- **Файл**: `data/repository/MoviesRepository.kt`
- **Решение**: `MoviesRepository.instance` сделан единым потокобезопасным синглтоном приложения. Все экраны разделяют кэши страниц и деталей.

### 5. [ИСПРАВЛЕНО] Миграция на новый backend API `api-sloosh.vercel.app`
- **Файлы**: `data/api/MoviesApi.kt`, `data/api/Models.kt`, `data/repository/MoviesRepository.kt`
- **Решение**: Добавлены выделенные эндпоинты `api/v1/cartoons` и `api/v1/anime` (вместо хака с поиском по слову "мультфильм"), проксирование изображений TMDB для обхода блокировок в РФ (`/api/v1/images/tmdb/`).

---

## 🎮 НАВИГАЦИЯ И УПРАВЛЕНИЕ С ПУЛЬТА — ВСЕ УСТРАНЕНО ✅

### 6. [ИСПРАВЛЕНО] `ContinueScreen`: Диалог действий (удалить/отметить) недосягаем с пульта
- **Файл**: `ui/continue_watching/ContinueScreen.kt`
- **Решение**: Переработан на карточки 16:9 по образцу iOS `ContinueWatchingCard.swift`. Добавлена поддержка вызова меню по кнопке `KEYCODE_MENU`, долгому клику и кнопке опций. В диалоге действий установлен гарантированный захват фокуса `firstActionFocusRequester` с поддержкой `BackHandler`.

### 7. [ИСПРАВЛЕНО] `DetailsScreen`: Застревание фокуса и навигация
- **Файл**: `ui/details/DetailsScreen.kt`
- **Решение**: Направленные переходы фокуса между кнопкой "Назад", описанием ("Ещё") и кнопками "Смотреть" / "Избранное". Добавлена секция актёров с круглыми аватарками.

### 8. [ИСПРАВЛЕНО] Обработка кнопки Back на пульте для диалогов
- **Файлы**: `ui/details/DetailsScreen.kt`, `ui/continue_watching/ContinueScreen.kt`
- **Решение**: Добавлены `BackHandler(enabled = ...)` для закрытия диалогов и оверлеев без закрытия экрана.

---

## 🎨 UI / UX НЕДОЧЁТЫ (Несоответствие идеалу iOS / Android TV)

### 9. Отсутствие скелетонов загрузки (Shimmer)
- **Файлы**: `ui/home/HomeScreen.kt`, `ui/search/SearchScreen.kt`, `ui/profile/ProfileScreen.kt`
- **Суть проблемы**: При загрузке отображается дешевый `CircularProgressIndicator` вместо красивого мерцающего скелетона карточек (2:3 для постеров, 16:9 для продолжения просмотра), хотя `ShimmerPlaceholder.kt` уже написан.
- **Решение**: Встроить `ShimmerPosterGrid` на время загрузки.

### 10. [ИСПРАВЛЕНО] `SourceSelectionDialog` в виде `Dialog` вместо полноэкранного оверлея
- **Файлы**: `ui/details/SourceSelectionDialog.kt`, `ui/details/DetailsScreen.kt`, `ui/details/DetailsViewModel.kt`, `data/repository/AllohaRepository.kt`
- **Суть проблемы**: Компонент `Dialog` на лаунчерах Android TV вызывал сброс фокуса D-pad, потерю кадров (`HWUI Davey! duration=1087ms`) и отмену через `WindowOnBackDispatcher`. Кликабельная подложка `Box.clickable { onDismiss() }` перехватывала нажатия кнопки пульта и закрывала диалог за доли секунды.
- **Решение**: Полностью переведено на нативный Compose-оверлей `SourceSelectionOverlay` внутри экрана деталей.

---

## 🎬 ИСПРАВЛЕННЫЕ КРИТИЧЕСКИЕ БАГИ

### ✅ [ИСПРАВЛЕНО] Зависание воспроизведения через 7-8 минут (TTL-рестарт и сброс сокетов)
- **Файлы**:
  - `data/alloha/HlsProxyServer.kt`
  - `data/alloha/AllohaSessionManager.kt`
  - `ui/player/PlayerViewModel.kt`
  - `data/repository/AllohaRuntimeParser.kt`
  - `data/repository/AllohaRuntimeResolver.kt`
- **Причина**: В `AllohaSessionManager.kt` срабатывал таймер `scheduleTtlRestart` через `(ttl * 1000 - 20_000)` мс (при TTL = 480c это ровно 7 минут 40 секунд). Рестарт вызывал `oldPool.evictAll()`, закрывал активные HTTP-соединения во время скачивания сегментов, сбрасывал кэш и отправлял ExoPlayer ошибку HTTP 503 (`send503`), после чего плеер навсегда зависал.
- **Решение**:
  1. Воспроизведение переведено на эталонную архитектуру iOS: использование `AllohaRuntimeResolver` / `AllohaRepository.resolveStream` напрямую. Поток резолвится один раз, WebView уничтожается, таймеры TTL-рестартов полностью удалены.
  2. `HlsProxyServer.kt` полностью переписан по образцу `HlsProxyServer.swift`: постоянный синглтон-сокет (`HlsProxyServer.shared`), изолированный `supervisorScope` для предотвращения падения цикла accept, доверительный SSL OkHttp пул (TrustAll + 5-минутный keepAlive без `evictAll`), прямое потоковое проксирование сегментов через буфер 32 КБ без перегрузки кучи JVM, полная пересылка всех заголовков авторизации/сессии к CDN, поддержка заголовков `Range`, исключение отправки кода 503, безопасное переписывание путей m3u8 (включая протокол-относительные ссылки `//`).
  3. Переключение озвучки и качества реализовано бесшовно с мгновенным сохранением и восстановлением позиции воспроизведения (`lastPreservedPositionMs`) и без размонтирования поверхности видео (`AndroidView` остается в дереве композиции со стильным оверлеем переключения потока).
  4. В `PlayerScreen.kt` добавлены: автоматическое однократное восстановление воспроизведения при ошибке ExoPlayer с сохранением позиции (`hasAutoRetried`), сторожевой таймер застревания буферизации (Stall Watchdog > 12c), полнофункциональный диалог ошибок с захватом фокуса на кнопке "Повторить", а также `LifecycleEventObserver` для паузы при сворачивании и гарантированного запуска прокси при возобновлении.
  5. В `AllohaRuntimeResolver.kt` коллекция заголовков переведена на `ConcurrentHashMap` для потокобезопасности при одновременных сетевых перехватах в WebView.

### ✅ [ИСПРАВЛЕНО] Вылет (FATAL EXCEPTION) на главной из-за дублирования ключей в TvLazyVerticalGrid
- **Файлы**:
  - `ui/home/HomeViewModel.kt`
  - `ui/home/HomeScreen.kt`
  - `ui/search/SearchViewModel.kt`
  - `ui/search/SearchScreen.kt`
  - `ui/continue_watching/ContinueScreen.kt`
  - `ui/profile/ProfileScreen.kt`
- **Причина**: `java.lang.IllegalArgumentException: Key "MOVIES_kp_7421341" was already used`. При пагинации (подгрузке следующей страницы каталога) или при наличии одинаковых фильмов в ответе API список `existing + newItems` не дедуплицировался. Compose требовал 100% уникальности ключей для элементов `items()`, и при совпадении идентификатора приложение моментально крашилось.
- **Решение**:
  1. В `HomeViewModel` и `SearchViewModel` списки элементов теперь строго дедуплицируются через `.filter { it.identifier.isNotBlank() }.distinctBy { it.identifier }`.
  2. Во всех сетках `TvLazyVerticalGrid` и строках `TvLazyRow` (`HomeScreen`, `SearchScreen`, `ContinueScreen`, `ProfileScreen`) ключи дополнены индексом (`"${id}_$index"`), что математически гарантирует абсолютную уникальность ключа для каждого слота и делает подобные вылеты невозможными.

### ✅ [ИСПРАВЛЕНО] Мигание окна загрузки и исчезновение окна выбора озвучки при нажатии «Смотреть»
- **Файлы**:
  - `ui/details/SourceSelectionDialog.kt`
  - `ui/details/DetailsScreen.kt`
  - `ui/details/DetailsViewModel.kt`
  - `data/repository/AllohaRepository.kt`
- **Причина**:
  1. `SourceSelectionLoadingDialog` и `SourceSelectionDialog` использовали системный `androidx.compose.ui.window.Dialog`. На Android TV открытие отдельного системного окна вызывало конфликт с диспетчером фокуса (`WindowOnBackDispatcher: sendCancelIfRunning`), а фоновая подложка `Box.clickable { onDismiss() }` ошибочно перехватывала отпускание кнопки пульта Enter/DPAD_CENTER, моментально вызывая `onDismiss()`.
  2. При возникновении сетевой ошибки или возврате `null` из `fetchAllohaData`, флаг `isFetchingSources` становился `false`, а `allohaData` оставался `null`. В `DetailsScreen` отсутствовала ветка для отображения ошибки, из-за чего окно загрузки просто исчезало, оставляя экран пустым.
  3. `AllohaRepository.kt` использовал стандартный `OkHttpClient` без доверенного SSL (`TrustAllCerts`). На приставках с устаревшими корневыми сертификатами или при строгой проверке TLS вызов к `api.alloha.tv` падал с исключением и тихо возвращал `null`.
  4. Синхронное чтение `SharedPreferences` на главном потоке внутри Composable-функции вызывало пропуск кадров (`Skipped 58 frames!`).
- **Решение**:
  1. Полный отказ от системных окон `Dialog` в пользу нативного полноэкранного оверлея `SourceSelectionOverlay`, анимированного через `AnimatedVisibility(fadeIn/fadeOut)`.
  2. Устранена кликабельная подложка, перехватывавшая нажатия пульта. Закрытие оверлея осуществляется исключительно кнопкой «Назад» на пульте (`BackHandler`) или явной кнопкой закрытия.
  3. Добавлены явные состояния и компоненты `SourceSelectionLoadingView`, `SourceSelectionErrorView` (с кнопками «Повторить» и «Закрыть») и `SourceSelectionContentView`.
  4. В `AllohaRepository.kt` внедрён доверенный `getUnsafeOkHttpClient()` с обходом проверок самоподписанных сертификатов (аналогично iOS `TrustAllSessionDelegate`) и поддержка запросов по альтернативным идентификаторам (`kpId`, `imdbId`, `tmdbId`).
  5. Чтение предпочтений перенесено из Compose UI в фоновый поток `Dispatchers.IO` во `ViewModel`.
### ✅ [ИСПРАВЛЕНО] Кнопка «Пропустить заставку» не встроена в интерфейс и висела на экране
- **Файл**: `ui/player/PlayerScreen.kt`
- **Причина**: Кнопка «Пропустить заставку / титры» отображалась отдельным парящим элементом поверх экрана без согласованной интеграции с нижним баром управления плеера и конфликтовала с D-pad навигацией контролов.
- **Решение**: 
  1. Реализована двухрежимная адаптивная интеграция: когда панель управления плеера скрыта, отображается аккуратная полупрозрачная плашка TV (`[OK] Пропустить заставку`), автоматически исчезающая через 7 секунд или активируемая нажатием центральной кнопки пульта DPAD_CENTER / OK без необходимости раскрытия HUD.
  2. Когда панель управления раскрыта пользователем, кнопка бесшовно встроена в правый нижний блок контролов над полосой перемотки (seekbar) с полноценным переходом фокуса по D-pad (Вверх с полосы перемотки → фокус на кнопке пропуска, Вниз → возврат на полосу, Влево → контролы).

### ✅ [ИСПРАВЛЕНО] «Таймаут загрузки видеопотока», WebViewMethodCalledOnWrongThreadViolation и ошибка Pipe closed
- **Файлы**:
  - `data/repository/AllohaRuntimeResolver.kt`
- **Причина**:
  1. В `shouldInterceptRequest` вызывался метод `view?.url`. Так как метод перехвата сетевых запросов вызывается на фоновом пуле потоков Chromium (`ThreadPoolForeg`), вызов любого метода `WebView` приводил к `android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation: at android.webkit.WebView.getUrl()` и прерывал обработку перехвата.
  2. Headless WebView не переопределял `getDefaultVideoPoster()` в `WebChromeClient`. При попытке воспроизведения HTML5-видео внутри невидимого/однопиксельного WebView движок Skia пытался отрисовать постер и падал с ошибкой `libpng encode error: sk_write_fn cannot write to stream / Pipe closed`.
  3. В `loadDataWithBaseURL` в качестве базового URL передавался полный `iframeUrl` (с query-параметрами) вместо чистого origin (`${protocol}://${host}/`), что приводило к нарушению Same-Origin Policy между родительской страницей обёртки и `iframe`, блокируя доступ скриптов к `iframe.contentWindow`.
  4. Попытка клика по кнопке воспроизведения (`.allplay__play-btn`) производилась на внешнем `document`, а не внутри `iframe.contentWindow.document`. Из-за этого плеер Alloha не стартовал, не отправлял запрос к `/bnsi/` и не слал WebSocket-сообщение `config_update` с `edge_hash`.
  5. Метод `cleanup()` вызывал `v.destroy()` мгновенно, в то время как в очереди Looper Chromium оставались невыполненные события, что приводило к ошибке `cr_AwContents: Application attempted to call on a destroyed WebView`.
- **Решение**:
  1. Из `shouldInterceptRequest` полностью убраны любые обращения к экземпляру `WebView` (`view?.url`); заголовок `Referer` извлекается безопасно из заголовков запроса или URI ресурса.
  2. В `WebChromeClient` добавлен `getDefaultVideoPoster()`, возвращающий прозрачный 1x1 Bitmap, исключая сбои пайпа Skia.
  3. Для `loadDataWithBaseURL` базовый URL теперь строго формируется как origin домена Alloha (`"$origin/"`), гарантируя одинаковое происхождение (same-origin) обёртки и фрейма.
  4. Внедрена надёжная логика инициализации фрейма: хуки на `XMLHttpRequest`, `fetch` и `WebSocket` прикрепляются к `iframe.contentWindow` по событию `iframe.onload` и с периодической проверкой; каждые 300 мс опрашивается и нажимается кнопка `.allplay__play-btn` непосредственно внутри документа фрейма до момента готовности потока.
  5. В `cleanup()` уничтожение WebView отложено на 2000 мс (`mainHandler.postDelayed`), гарантируя корректное завершение всех внутренних задач Chromium без вылетов.

### ✅ [ИСПРАВЛЕНО] Зависание воспроизведения через ~11-12 минут (HTTP 403 vkvideo.cloud / 404) и неработающая кнопка «Повторить»
- **Файлы**:
  - `data/alloha/HlsProxyServer.kt`
  - `ui/player/PlayerViewModel.kt`
  - `ui/player/PlayerScreen.kt`
- **Причина**:
  1. Токены CDN Alloha / VK Video имеют ограниченный срок жизни (TTL, от 360 до 600 секунд). По истечении TTL CDN возвращает HTTP 403 Forbidden. Ранее упреждающее фоновое обновление токенов отсутствовало, из-за чего воспроизведение обрывалось ровно на 11.7 минутах (701849 мс).
  2. В `HlsProxyServer.kt` для подписанных CDN-ссылок без расширения (`ext.isEmpty`) по умолчанию назначался суффикс `stream.ts` вместо `stream.m3u8`, а при ошибке 403 от источника прокси сразу отдавал ExoPlayer код 404, не запуская регенерацию сессии.
  3. В `PlayerScreen.kt` кнопка «Повторить» в диалоге ошибки проверяла `if (state.errorMessage != null)`, и если ошибка пришла напрямую из ExoPlayer (`playerError`), в ветке `else` она просто вызывала `exoPlayer.seekTo(pos); exoPlayer.prepare()` на том же протухшем URL с кодом 403/404.
- **Решение**:
  1. В `PlayerViewModel.kt` внедрено упреждающее фоновое обновление сессии `scheduleProactiveRefresh(iframeUrl, ttlSeconds)` (по образцу iOS `AllohaSessionManager.swift`), которое за 25 секунд до истечения TTL запрашивает свежие токены в фоне, обновляет заголовки и Master URL в `HlsProxyServer.shared` без малейшего прерывания воспроизведения у пользователя.
  2. В `HlsProxyServer.kt` добавлен реактивный триггер `onSessionExpired`: при получении HTTP 403 или 410 прокси сигнализирует `PlayerViewModel.refreshSessionSilently()` для сброса и обновления токенов, а при запросе плейлиста ожидает до 1.5 секунд обновления URL перед отправкой 404.
  3. В `HlsProxyServer.kt` суффикс для URL без расширения заменён на `"stream.m3u8"` (аналогично iOS `HlsProxyServer.swift`), улучшено сопоставление путей плейлистов.
  4. В `PlayerScreen.kt` и авто-восстановление при ошибке (`onPlayerError`), и кнопка «Повторить» в диалоге ошибки переведены на `viewModel.retryPlayback(pos) { newUrl -> ... }` с получением свежего URL потока и переустановкой `MediaItem` в ExoPlayer с сохранением точной миллисекунды просмотра.

### ✅ [ИСПРАВЛЕНО] Полный сбой запуска воспроизведения (ошибка парсера / зависание на оверлее / Same-Origin Policy)
- **Файлы**:
  - `data/repository/AllohaRuntimeResolver.kt`
  - `data/repository/AllohaRepository.kt`
  - `data/alloha/HlsProxyServer.kt`
  - `MainActivity.kt`
- **Причина**:
  1. В `AllohaRuntimeResolver.kt` вызов `loadDataWithBaseURL` передавал `"$origin/"` в качестве baseUrl, в то время как iframe загружался с полного `iframeUrl`. Движок Chromium блокировал межфреймовое взаимодействие из-за нарушения Same-Origin Policy (`Blocked a frame with origin from accessing a cross-origin frame`), что делало невозможным доступ к `iframe.contentWindow.document`.
  2. Скрипт обёртки ожидал обязательного наступления двух событий: ответа `/bnsi/` и WebSocket авторизации (`hasAuth || hasAccept`), а также кликал только по селектору `.allplay__play-btn`. При сетевых задержках или изменениях формата сокетов срабатывал 20-секундный таймаут с ошибкой «Не удалось получить видеопоток».
  3. В `AllohaRepository.kt` методы добавления параметров трансляции просто конкатенировали строку без очистки уже существующих query-параметров (`&translation=66&translation=66`), а для серий отсутствовала явная инъекция `season` и `episode` в `iframeUrl`.
  4. В `HlsProxyServer.kt` при отсутствии заголовков `Referer` / `Origin` в `activeHeaders` прокси посылал запрос к CDN без них, приводя к HTTP 403 Forbidden.
  5. В `MainActivity.kt` пустые или null названия медиа приводили к невалидным маршрутам в `NavHost` (`player/{iframeUrl}/...`), краша навигационный граф.
- **Решение**:
  1. В `AllohaRuntimeResolver.kt` baseUrl и historyUrl в `loadDataWithBaseURL` строго установлены в точный `iframeUrl`.
  2. Внедрён расширенный JS-рантайм: глобальный перехват `XMLHttpRequest`, `fetch` и `WebSocket` (захват `accepts-controls`, `ttl`), циклический опрос `performance.getEntriesByType('resource')`, принудительный запуск воспроизведения тегов `<video>` и клик по кнопкам воспроизведения.
  3. Добавлен упреждающий резолв `scheduleFallbackResolve`: при наличии `bestMasterPayload` или `bestDirectPayload` поток возвращается немедленно (через 400-2000 мс), не дожидаясь 20-секундного таймаута сокетов.
  4. В `AllohaRepository.kt` реализовано безопасное удаление дублирующихся query-параметров перед добавлением новых и гарантированная инъекция `season` и `episode` для каждого эпизода сериала.
  5. В `HlsProxyServer.kt` добавлен автоматический fallback для заголовков `Referer` и `Origin` из домена целевого медиа-URL.
  6. В `MainActivity.kt` добавлен fallback для пустого заголовка (`"none"`) и безопасное Base64 URL-safe кодирование параметров маршрута.

### ✅ [ИСПРАВЛЕНО] Навигация D-Pad на экранах «Поиск» и «Избранное»
- **Файлы**:
  - `ui/profile/ProfileScreen.kt`
  - `ui/search/SearchScreen.kt`
- **Причина**:
  - При переходе со строки поиска или с табов категорий кнопка D-pad Вниз теряла фокус или не переходила к первой карточке сетки; при нажатии D-pad Вверх с первой строки карточек фокус не возвращался к строке ввода или табам.
- **Решение**:
  - В `ProfileScreen.kt` и `SearchScreen.kt` добавлены связанные `FocusRequester` и перехватчики `onPreviewKeyEvent`: Вниз с табов/поиска переходит к первой карточке/чипу, Вверх с первой строки возвращает фокус обратно на элементы управления.

### ✅ [ИСПРАВЛЕНО] Задержка загрузки категорий на главном экране (HomeViewModel)
- **Файл**: `ui/home/HomeViewModel.kt`
- **Причина**: При холодном старте загружалась только первая категория «Все». Переключение на другие вкладки вызывало заметную задержку загрузки.
- **Решение**: Добавлен параллельный асинхронный предзагрузчик `prefetchOtherCategories()` на `Dispatchers.IO` в `init` и при смене фильтра.

### ✅ [ИСПРАВЛЕНО] Полный отказ воспроизведения видео: обход парсера, заморозка WebView, ошибка сопоставления плейлиста в HlsProxy и сбой Referer
- **Файлы**:
  - `data/repository/AllohaRuntimeResolver.kt`
  - `SlooshApplication.kt`
  - `data/alloha/HlsProxyServer.kt`
  - `ui/player/PlayerScreen.kt`
  - `ui/home/HomeScreen.kt`
  - `ui/profile/ProfileScreen.kt`
- **Причина**:
  1. **Ложный обход через статический regex (`resolveViaHttpHops`)**: в `AllohaRuntimeResolver` первоочередной вызов парсил статический HTML через regex на поиск `.m3u8`/`.mp4`. Статический HTML Alloha возвращал фиктивные/неавторизованные прямые ссылки без токенов сессии (`accepts-controls`, `authorization`). Приложение полностью обходило запуск WebView, а ExoPlayer и `HlsProxyServer` получали HTTP 403 Forbidden от CDN.
  2. **Заморозка отсоединённого WebView (Headless Throttling)**: экземпляр `WebView` создавался без прикрепления к оконной иерархии (`Activity.decorView`). На Android TV движок Chromium расценивал невидимый отсоединённый WebView как фоновый процесс и приостанавливал выполнение таймеров JS (`setInterval`), WebSockets и HTML5-видеоплеера, блокируя захват токенов.
  3. **Междоменная изоляция (Same-Origin Policy) во фреймах**: использование внешней обёртки с `<iframe src="...">` приводило к блокировке `frames[i].contentWindow` при перенаправлениях на сторонние CDN (`DOMException: Blocked a frame with origin`), из-за чего хуки на `XMLHttpRequest` и `WebSocket` никогда не устанавливались в плеере.
  4. **Фатальная ошибка парсинга путей в HlsProxyServer**: проверка `rawPath.lowercase().endsWith(".m3u8")` всегда возвращала `false`, так как `rawPath` имел вид `/proxy/stream.m3u8?url=...` (заканчивался query-параметром). Плейлисты без `.m3u8` в URL обрабатывались как TS-сегменты без перезаписи путей, что ломало воспроизведение дочерних чанков.
  5. **Искажение заголовка Referer**: при локальном проксировании `videoUri.host` (`127.0.0.1`) ошибочно подставлялся как `Referer: http://127.0.0.1/` в upstream-запросы к CDN.
  6. **Сбой Chunkless Preparation в ExoPlayer**: флаг `.setAllowChunklessPreparation(true)` в `HlsMediaSource.Factory` вызывал зависания на проксированных HLS-потоках без явных метаданных кодеков.
  7. **Блокировка авто-восстановления на нулевой секунде**: в `onPlayerError` условие `if (!hasAutoRetried && pos > 0)` при ошибке на старте (`pos == 0`) вычислялось как `false`, сразу показывая плашку ошибки без единой попытки восстановления.
  8. **Фокусируемость табов с пульта**: в `HomeScreen` и `ProfileScreen` на плашках категорий отсутствовал явный модификатор `.focusable()`, что затрудняло навигацию D-pad.
### ✅ [ИСПРАВЛЕНО] «Таймаут загрузки видеопотока» (20с) из-за OkHttp-перехвата HTML, заморозки WebView (View.INVISIBLE) и отсутствия хуков Plyr
- **Файлы**:
  - `data/repository/AllohaRuntimeResolver.kt`
  - `data/repository/AllohaRuntimeParser.kt`
- **Причина**:
  1. **Сетевой перехват HTML в `shouldInterceptRequest`**: `shouldInterceptRequest` перехватывал основной запрос HTML и выполнял его синхронно через отдельный `OkHttpClient`. OkHttp не передавал куки сессии в `CookieManager`, не имел доверительного SSL/TLS пула для всех доменов CDN, не воспроизводил браузерные заголовки и TLS fingerprint, из-за чего Cloudflare/защита от ботов блокировали запрос или отдавали ответ без `Set-Cookie`. В результате страница плеера загружалась неполноценно без авторизационных кук.
  2. **Дублирование запросов к `/bnsi/`**: в `shouldInterceptRequest` запускался параллельный фоновый запрос через OkHttp к `/bnsi/`, в то время как скрипт плеера внутри страницы также слал запрос к `/bnsi/`. Это приводило к сбросу сессионного токена или блокировке по rate-limit на стороне сервера Alloha.
  3. **Заморозка отсоединённого WebView (View.INVISIBLE Throttling)**: контейнер WebView добавлялся в `decorView` с `visibility = View.INVISIBLE`. На Android TV движок Chromium расценивал невидимый WebView как фоновый процесс и приостанавливал выполнение таймеров JS (`setInterval`), WebSockets и полностью блокировал автовоспроизведение HTML5-видео (`document.visibilityState == 'hidden'`), из-за чего поток так и не запрашивался.
  4. **Пропуск клика по кнопке воспроизведения плеера Plyr**: скрипт искал только устаревшие селекторы (`.allplay__play-btn`, `#player`), в то время как плеер Alloha использует Plyr (`.plyr__control--overlaid`, `button[data-plyr="play"]`). Без клика по оверлею Plyr не инициировал создание `Hls` и запрос плейлиста `master.m3u8`.
  5. **Неполноценный перехват WebSocket**: слушатель сообщений сокета добавлялся только при вызове `.send()`. Если сокет получал `config_update` до отправки исходящих сообщений, хук не срабатывал.
  6. **Отсутствие упреждающего резолва до 20-секундного таймаута**: при отсутствии `bestHlsSourcePayload` или `bestMasterPayload` таймер 20 секунд в `AllohaRuntimeResolver` завершался фатальным исключением `java.lang.RuntimeException: Таймаут загрузки видеопотока`.
- **Решение**:
  1. **Полный отказ от OkHttp-перехвата в `shouldInterceptRequest`**: убраны любые блокирующие вызовы OkHttp. `shouldInterceptRequest` выполняет только пассивный сбор заголовков (`authorizations`, `authorization`, `accepts-controls`, `referer`) и обнаружение URL `master.m3u8`, всегда возвращая `null`, благодаря чему Chromium обрабатывает 100% сетевых запросов нативно с сохранением всех кук, сессий и TLS.
  2. **Активный фоновый контейнер WebView на нулевом слое окна**: размер 1x1 заменён на полноценный `MATCH_PARENT` (размер 1x1 расценивался политикой Chromium как tracking pixel и блокировал автоплей видео, а также схлопывал адаптивный CSS плеера Plyr). Контейнер размещается на индексе 0 (`decorView.addView(container, 0)`), находясь строго под Compose-иерархией, с `alpha = 0.01f`, `isFocusable = false` и `importantForAccessibility = NO`, что гарантирует полноценный рендеринг видео без помех для пользователя и D-pad навигации.
  3. **Комплексные хуки Plyr, безусловное снятие паузы и автоплей**: в `HOOK_JS` добавлен вызов `player.play()`, безусловный сброс модалки «Продолжить просмотр» (`.time_save__btn`), запуск воспроизведения `video.play()` даже при пустом/blob `src`, эмуляция клика по всем кнопкам плеера без преждевременного `break`, а также полифилл `document.visibilityState = 'visible'`.
  4. **Глобальный перехват WebSocket (включая setter `onmessage`)**: перехвачен не только конструктор `WebSocket` и его методы (`send`, `addEventListener`), но и дескриптор сеттера свойства `onmessage` на `WebSocket.prototype`, что гарантирует захват `config_update` (`edge_hash`, `ttl`) даже при назначении обработчика через `ws.onmessage = ...`.
  5. **Мгновенный адаптивный резолв**: при получении `hlsSource` или перехвате плейлиста `master.m3u8` в `shouldInterceptRequest` при наличии заголовков воспроизведения поток декодируется и передаётся в плеер немедленно (за 1-2 секунды) без 300-миллисекундной задержки; добавлен сторожевой таймер на 3.0 секунды для упреждающего резолва накопленных данных без ожидания таймаута.
  6. **Идемпотентная очистка ресурсов**: метод `cleanup()` защищён атомарным флагом `isCleanedUp` от гонок при параллельной отмене корутины и успешном резолве.
  7. **Расширенная поддержка `skipTime` в `AllohaRuntimeParser`**: добавлена поддержка формата `JSONArray` с объектами `[{"start": ..., "end": ...}]` в дополнение к строкам.

### ✅ [ИСПРАВЛЕНО] Сброс фокуса на первый фильм при возврате из боковой панели (Exact Card Focus Restoration)
- **Файлы**:
  - `ui/home/HomeScreen.kt`
  - `ui/search/SearchScreen.kt`
  - `ui/continue_watching/ContinueScreen.kt`
  - `ui/profile/ProfileScreen.kt`
- **Причина**:
  - На экранах с сетками карточек (`HomeScreen`, `SearchScreen`, `ContinueScreen`, `ProfileScreen`) экземпляр `FocusRequester` (`firstCardFocusRequester`, `firstResultFocusRequester`, `firstItemFocusRequester`) был статически прикреплён исключительно к карточке с `index == 0`.
  - При выходе пользователя в боковую панель (D-pad Left) и последующем возврате в контент (D-pad Right, Enter, клик или Back), `contentFocusCallback` всегда вызывал `firstCardFocusRequester.requestFocus()`, принудительно сбрасывая фокус на самый первый фильм в списке и прокручивая сетку в начало.
  - Аналогично при перемещении D-pad Вверх на табы категорий или строку поиска и последующем нажатии D-pad Вниз фокус также сбрасывался на элемент 0.
  - В `ProfileScreen.kt` в обработчике D-pad Вверх была опечатка `selectedIndex` вместо `selectedCategory.ordinal`.
- **Решение**:
  1. Внедрена динамическая архитектура запоминания фокуса с `rememberSaveable`:
     - `var lastFocusedCardIndex by rememberSaveable { mutableIntStateOf(0) }`
     - `var lastFocusedArea by rememberSaveable { mutableStateOf(...) }`
     - Единый `activeCardFocusRequester`, который динамически прикрепляется к карточке с `index == targetIndex` (`targetIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)`).
  2. При получении фокуса любой карточкой `lastFocusedCardIndex` мгновенно обновляется (`onFocusChanged` / `onFocus`).
  3. При возврате из боковой панели `contentFocusCallback` восстанавливает фокус строго на `activeCardFocusRequester` на той же самой карточке. Если карточка была смещена за пределы видимой области `TvLazyVerticalGrid`, корутина выполняет `gridState.scrollToItem(safeTarget)` и гарантированно возвращает фокус.
  4. При переходе D-pad Вниз с табов категорий или строки поиска фокус опускается ровно на ту карточку, с которой пользователь поднимался наверх.
  5. При смене категории каталога или поискового запроса `lastFocusedCardIndex` предсказуемо сбрасывается в 0 через `LaunchedEffect`.
  6. Исправлена опечатка `selectedIndex` в `ProfileScreen.kt` на `selectedCategory.ordinal`.

### ✅ [ИСПРАВЛЕНО] На вкладке «Все» отображались только фильмы без сериалов, мультфильмов и аниме
- **Файлы**:
  - `data/api/MoviesApi.kt`
  - `data/repository/MoviesRepository.kt`
  - `ui/home/HomeViewModel.kt`
- **Причина**:
  - В исходной логике (унаследованной напрямую из iOS `HomeView.swift:886 case .all, .movies:`) категория `HomeCategory.ALL` была сгруппирована с фильмами и вызывала только `getPopularMovies(page)` / `getTopMovies(page)`. В результате вкладка «Все» физически содержала 100% исключительно фильмы, сериалы и аниме полностью отсутствовали в общем каталоге.
- **Решение**:
  1. В `MoviesApiService` и `MoviesRepository` добавлен эндпоинт `getTrending(page, window = "week")`, возвращающий всемирный трендовый контент TMDB (смесь популярных фильмов, сериалов и анимации).
  2. В `HomeViewModel.kt` для вкладки `HomeCategory.ALL` реализовано параллельное асинхронное получение контента:
     - Для `HomeFilter.POPULAR`: параллельно запрашиваются `getTrending(page)` (фильмы + сериалы), `getCartoons(page)` (мультфильмы) и `getAnime(page)` (аниме), после чего результаты гармонично чередуются функцией `interleaveMedia` (на каждые 4 трендовых фильма/сериала добавляется 1 мультфильм и 1 аниме).
     - Для `HomeFilter.TOP_RATED`: параллельно запрашиваются `getTopMovies`, `getTopTv`, `getCartoons` и `getAnime(order = "RATING")`, чередуясь функцией `interleaveFour`.
  3. Вкладка «Все» теперь содержит полноценную богатую витрину всех типов медиа, а отдельные вкладки («Фильмы», «Сериалы», «Мультфильмы», «Аниме») продолжают отдавать строго отфильтрованные категории.

### ✅ [ИСПРАВЛЕНО] Ошибка сборки релизной версии (assembleRelease) в GitHub Actions при push в репозиторий
- **Файлы**:
  - `.github/workflows/build-apk.yml`
  - `app/build.gradle.kts`
  - `.gitignore`
  - `gradlew`
- **Причина**:
  1. В `.github/workflows/build-apk.yml` выполнялась команда `gradle assembleRelease --stacktrace` вместо использования Gradle Wrapper (`./gradlew`). На раннере `ubuntu-latest` системный `gradle` отсутствует (`command not found`) либо конфликтует с версией Gradle Wrapper (8.6) и AGP 8.3.
  2. Файл `gradlew` в git-индексе имел права `100644` (не исполняемый). На Linux-раннере при попытке запуска `./gradlew` возникала ошибка `Permission denied`.
  3. В `app/build.gradle.kts` в `signingConfigs.release` при отсутствии файла хранилища `keys/slooshkey` или пароля вызывался `initWith(getByName("debug"))`. При таком наследовании имя конфигурации оставалось `"release"`, из-за чего AGP-задача `validateSigningRelease` искала `~/.android/debug.keystore` на диске и не создавала его автоматически, падая с `Keystore file not found for signing config 'release'`.
  4. Переменные окружения для подписи (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) не передавались в шаг сборки в workflow.
  5. Шаг подготовки артефакта жестко требовал файл `app-release.apk` без поиска сгенерированного APK, а публикация релиза с тегом `latest` не учитывала версионирование для встроенного `UpdateManager`.
- **Решение**:
  1. В `.github/workflows/build-apk.yml` сборка переведена на `./gradlew assembleRelease --no-daemon --stacktrace` с предварительным шагом `chmod +x gradlew`.
  2. В git-индексе для `gradlew` установлен исполняемый режим `100755` (`git update-index --chmod=+x gradlew`).
  3. В `app/build.gradle.kts` реализован безопасный fallback: если файл `keys/slooshkey` или `KEYSTORE_PASSWORD` не заданы, `signingConfig` напрямую указывает на `signingConfigs.getByName("debug")`. В этом случае AGP автоматически генерирует временный ключ отладки на чистом раннере, и сборка релиза проходит без ошибок.
  4. В workflow добавлен шаг декодирования секретного хранилища `KEYSTORE_BASE64` (если он задан в GitHub Secrets) и проброс переменных окружения подписи.
  5. Добавлен динамический поиск APK в каталоге сборки (`find app/build/outputs/apk/release/ -type f -name "*.apk"`).
  6. Внедрено авто-извлечение `versionName` из `build.gradle.kts` для создания тега релиза (например, `v1.0.5`), что гарантирует корректную работу автообновления через `UpdateManager` на телевизоре.
  7. Папка `/keys/` и файлы `*.keystore`, `*.jks` добавлены в `.gitignore` для безопасности публичного репозитория.
