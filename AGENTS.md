# Agent Onboarding — sloosh Android TV

## Project Identity

- **Product name**: `sloosh`
- **Platform**: Android TV (Leanback / Compose for TV)
- **Package**: `com.sloosh.tv`
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 34
- **Architecture**: MVVM + Jetpack Compose for TV (androidx.tv.material3)
- **Primary language**: Kotlin
- **Streaming source**: Alloha (ONLY — do NOT implement or mention Collaps)
- **Backend API**: `https://api-sloosh.vercel.app/` (with `X-API-Key` auth)
- **User communication language**: Russian

> **NEVER** mention `NeoMovies`, `neomovies`, `Alloha`, `Collaps`, or other internal source/provider names in the UI.
> **NEVER** add screens like Credits, About popup clutter, or watermarks.
> **NEVER** use any shade of green in UI elements — **the only exception is rating badges** (`RatingIosGreen = #1CB54B`).
> All other UI uses **only black and white** (monochrome palette).

---

## Workspace Structure

```
w:\sloosh-android-tv\
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/sloosh/tv/
│   │   ├── MainActivity.kt              ← Root: NavHost + NavigationDrawer + UpdateDialog
│   │   ├── SlooshApplication.kt         ← Coil image loader setup, DB init
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── Models.kt            ← All data models: MediaDto, MediaDetailsDto, AllohaResolvedStream, etc.
│   │   │   │   └── MoviesApi.kt         ← Retrofit API interface + singleton
│   │   │   ├── alloha/
│   │   │   │   ├── AllohaParser.kt      ← Low-level Alloha payload parser (JS response)
│   │   │   │   ├── AllohaSessionHolder.kt ← Singleton session reference
│   │   │   │   ├── AllohaSessionManager.kt ← Manages WebView session lifecycle
│   │   │   │   └── HlsProxyServer.kt    ← Local NanoHTTPD HLS proxy with quality switching
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt       ← Room database singleton
│   │   │   │   ├── Daos.kt              ← DAOs for progress, favorites, search history
│   │   │   │   └── Entities.kt          ← ProgressEntity, FavoriteEntity, SearchHistoryEntity
│   │   │   ├── repository/
│   │   │   │   ├── AllohaRepository.kt  ← Fetches Alloha catalog, parses translations
│   │   │   │   ├── AllohaRuntimeParser.kt ← Parses raw Alloha stream payload → AllohaResolvedStream
│   │   │   │   ├── AllohaRuntimeResolver.kt ← Resolves iframe URL → HLS via HTTP hops + WebView fallback
│   │   │   │   ├── AppSettings.kt       ← SharedPreferences: quality, autoplay, gridColumns
│   │   │   │   ├── MoviesRepository.kt  ← Wraps API calls with in-memory page caching
│   │   │   │   └── PlaybackProgressStore.kt ← Room-based progress save/load
│   │   │   └── update/
│   │   │       └── UpdateManager.kt     ← GitHub Releases self-update check + download/install
│   │   └── ui/
│   │       ├── components/
│   │       │   ├── SlooshFocusableCard.kt   ← Universal TV focusable card with light-beam border effect
│   │       │   ├── SlooshSideDrawer.kt      ← Side navigation drawer with NavSection enum
│   │       │   ├── SlooshButton.kt          ← Primary/secondary capsule button
│   │       │   ├── ShimmerPlaceholder.kt    ← Shimmer skeleton for loading states
│   │       │   ├── ProgressiveGradientOverlay.kt ← Reusable gradient depth overlay
│   │       │   └── UpdateDialog.kt          ← In-app update download dialog
│   │       ├── home/
│   │       │   ├── HomeScreen.kt        ← Category tabs + HorizontalPager + TvLazyVerticalGrid
│   │       │   └── HomeViewModel.kt     ← Categories: ALL/MOVIES/SERIES/CARTOONS/ANIME; filters: POPULAR/TOP_RATED
│   │       ├── search/
│   │       │   ├── SearchScreen.kt      ← Search input + history chips + results grid
│   │       │   └── SearchViewModel.kt
│   │       ├── details/
│   │       │   ├── DetailsScreen.kt     ← Full details: backdrop + ambient color + metadata + play button
│   │       │   ├── DetailsViewModel.kt
│   │       │   └── SourceSelectionDialog.kt ← Alloha translation/season/episode picker
│   │       ├── player/
│   │       │   ├── PlayerScreen.kt      ← ExoPlayer + custom TV controls HUD
│   │       │   └── PlayerViewModel.kt   ← AllohaSessionManager integration, progress, episode nav
│   │       ├── continue_watching/
│   │       │   ├── ContinueScreen.kt    ← 16:9 cards grid with progress bar
│   │       │   └── ContinueViewModel.kt
│   │       ├── profile/
│   │       │   ├── ProfileScreen.kt     ← Favorites with segmented category filter
│   │       │   └── ProfileViewModel.kt
│   │       ├── settings/
│   │       │   └── SettingsScreen.kt    ← 4 category panels: Playback/Appearance/Data/About
│   │       ├── theme/
│   │       │   ├── Color.kt             ← ALL color constants (see Design System below)
│   │       │   ├── Theme.kt             ← SlooshTVTheme wrapping MaterialTheme
│   │       │   └── Type.kt              ← SlooshTypography: tight negative letter-spacing
│   │       └── util/
│   │           ├── MediaHelpers.kt      ← cleanTranslationName(), displayTranslationName()
│   │           └── PaletteHelper.kt     ← rememberAdaptiveAmbientColor() for details backdrop
│   └── res/
│       ├── drawable/                    ← ic_banner (TV banner), ic_launcher variants
│       └── xml/file_paths.xml          ← FileProvider paths for APK update install
├── iOS-sloosh/                          ← iOS reference (read-only, do NOT modify)
│   ├── AGENTS.md                        ← iOS onboarding (read for context)
│   └── neomovies-mobile/               ← React Native reference (Alloha logic, API models)
├── errors.md                            ← Current known errors log (keep updated)
├── AGENTS.md                            ← THIS FILE
└── gradle/libs.versions.toml           ← Dependency versions catalog
```

---

## iOS Reference Projects

Both are located inside this workspace at `iOS-sloosh/`:

| Project | Path | Purpose |
|---------|------|---------|
| `sloosh-iOS` | `iOS-sloosh/sloosh-iOS/` | Reference for UX flows, screen designs, feature parity |
| `neomovies-mobile` | `iOS-sloosh/neomovies-mobile/` | Reference for Alloha API logic, data models, stream resolution |

**Key rules when referencing iOS:**
- Adapt logic/patterns to Android TV idioms — never copy iOS UI literally
- TV navigation is fundamentally different (D-pad, no touch)
- iOS uses `AVPlayerViewController` → Android uses `ExoPlayer` with custom controls
- iOS uses `WKWebView` → Android uses `android.webkit.WebView`
- iOS uses `SwiftData` → Android uses `Room`

---

## Design System & Visual Rules

### Color Palette (from `Color.kt`)

| Token | Hex | Usage |
|-------|-----|-------|
| `BackgroundDark` | `#050505` | Screen backgrounds — near-black, richer than pure black |
| `SurfaceDark` | `#111111` | Card surfaces |
| `GlassSurfaceDark` | `#1A1A1A` | Dialogs, modals |
| `GlassSurfaceFocusedDark` | `#252525` | Input focus state |
| `TextPrimaryDark` | `#FFFFFF` | Primary text |
| `TextSecondaryDark` | `#B3FFFFFF` (70% white) | Secondary/meta text |
| `TextMutedDark` | `#66FFFFFF` (40% white) | Placeholder, inactive |
| `RatingIosGreen` | `#1CB54B` | Rating ≥ 7.0 badge **ONLY** |
| `RatingIosGray` | `#6B7280` | Rating 5.0–7.0 badge **ONLY** |
| `RatingIosRed` | `#E11D48` | Rating < 5.0 badge **ONLY** |

> **CRITICAL**: Green color (`RatingIosGreen`) is ONLY allowed in rating badges. Nowhere else.
> All interactive states use white with varying opacity.

### Gradient Depth (Progressive Blur alternative)

Instead of real blur, depth is created with multi-stop linear gradients. This is a key visual effect:
- Use `Brush.verticalGradient` or `Brush.horizontalGradient` with 4-6 color stops
- Standard top scrim: `0% → 88% opacity → 58% → 20% → 0%`
- Standard bottom scrim: `transparent → 95% opacity black`
- Backdrop fade on details screen: uses `BlendMode.DstIn` drawn over `graphicsLayer(CompositingStrategy.Offscreen)`
- **Never use `.blur()` modifier** — it causes jank on TV hardware

### Corner Rounding (Capsule Library)

**ALWAYS** use `ContinuousCapsule` or `ContinuousRoundedRectangle(radius)` from the `capsule` library (`com.kyant.capsule`). Standard `RoundedCornerShape` is allowed for legacy areas but should be migrated.

| Shape | Use Case |
|-------|---------|
| `ContinuousCapsule` | Buttons, chips, pills, rating badges, small icons |
| `ContinuousRoundedRectangle(16.dp)` | Media cards (poster format) |
| `ContinuousRoundedRectangle(18-24.dp)` | Dialogs, bottom sheets |
| `CircleShape` | Icon-only round buttons (back, favorite) |

> **NEVER use standard `RoundedCornerShape`** for new UI components. Always use the capsule library.

### Typography (`SlooshTypography` from `Type.kt`)

Custom typography with **tight negative letter-spacing** (Apple SF Pro inspired):
- All sizes use negative `letterSpacing` from `-0.1.sp` to `-0.6.sp`
- `FontFamily.Default` (system font) — no custom font files needed
- Bold variants for titles, SemiBold for section headers, Medium for labels

### Focus Beam Effect (`SlooshFocusableCard`)

The universal focusable card shows a **traveling light-beam** along the border perimeter on focus:
- Light beam uses `PathMeasure` for exact constant-velocity travel
- Intensity: sine bell curve — thick in center, thin at tips
- Duration: 2400ms per cycle, `LinearEasing`
- Fade in/out: 200ms `tween` on focus gain/loss
- Uses `BlendMode.Plus` for additive white glow

> **DO NOT** replace `SlooshFocusableCard` with native TV `Card` — it has the custom beam animation.

### Animations

All animations must be **light and fast** — TV users need immediate feedback:

| Animation type | Spec |
|---------------|------|
| Focus scale (cards) | `spring(dampingRatio=0.82, stiffness=320)` or `tween(160-200ms)` |
| Category pill slide | `spring(dampingRatio=0.74, stiffness=380)` |
| Page transitions | `fadeIn+scaleIn` / `fadeOut+scaleOut`, 180-240ms |
| Favorite bounce | `spring(dampingRatio=0.4, stiffness=400)` |
| Drawer fade | `tween(140ms)` |

**Avoid** heavy animations: no bouncing physics with high stiffness, no `rememberInfiniteTransition` in grid items (only in focus-visible cards).

---

## Navigation Architecture

### Side Drawer (from `SlooshSideDrawer.kt`)

The app uses `androidx.tv.material3.NavigationDrawer` — a native TV drawer that reveals on D-pad left from the main content:
- Drawer is only shown for main tab routes: `home`, `search`, `continue`, `favorites`, `settings`
- Hidden for `details/{mediaId}` and `player/{...}` routes (edge-to-edge content)
- Nav items: Home / Search / Continue / Favorites / Settings

```
NavSection enum → route mapping:
HOME      → "home"
SEARCH    → "search"
CONTINUE  → "continue"
FAVORITES → "favorites"
SETTINGS  → "settings"
```

### Route Schema (`MainActivity.kt`)

```
home
search
continue
favorites
settings
details/{mediaId}
player/{iframeUrl}/{mediaId}/{season}/{episode}/{title}
  ↑ iframeUrl and title are Base64 URL-safe encoded to avoid route conflicts
```

### Focus Navigation Rules

> These are the most critical TV-specific rules. Violating them makes the app unusable with a remote.

1. **Every interactive element must be reachable** with D-pad Up/Down/Left/Right from every other element on screen
2. **Use `FocusRequester` + `onPreviewKeyEvent`** to implement custom D-pad routing between non-adjacent elements
3. **Never use `clickable` without `focusable`** for TV — remote control needs focus, not touch
4. **Default focus on screen entry**: use `LaunchedEffect` to request focus on the primary action button (e.g., "Watch" button on details screen)
5. **Category tabs**: D-pad Left/Right navigates between tabs; D-pad Down goes to first card; D-pad Up from first card row goes back to tab bar
6. **Modals/dialogs**: When a modal opens, trap focus inside it immediately (use `LaunchedEffect` + `FocusRequester`)
7. **Back button**: `BackHandler` must dismiss modals before navigating back
8. **`NavigationDrawerItem`**: Do NOT add custom scale animations — use `NavigationDrawerItemDefaults.scale(focusedScale=1.0f)` to prevent conflicts with custom icon animations
9. **`onFocusChanged`** on nav items: use to detect focus for alpha/scale animations, not for navigation logic (use `onPreviewKeyEvent` for D-pad navigation)

### Edge-to-Edge Layout

All screens MUST be edge-to-edge:
- `fillMaxSize()` on root Box
- Floating elements (tab bar, back button) use absolute positioning within a `Box`
- Content padding accounts for floating elements (e.g., `top = 75.dp` in grid for tab bar overlay)
- **No visible headers with backgrounds** — all header areas use gradient scrims only

---

## Screen Reference

### HomeScreen

**Purpose**: Main catalog — the first screen users see. Must load fast and feel premium.

**UX flow**:
1. Floating category tab bar at top (Все / Фильмы / Сериалы / Мультфильмы)
2. `HorizontalPager` for category pages — swiped programmatically, not by user
3. `TvLazyVerticalGrid` with 5 or 6 columns (user-configurable in Settings)
4. Infinite scroll — loads next page when 4 items from bottom
5. D-pad Up from first card row → tab bar; D-pad Down from tab → first card

**Performance notes**:
- Grid uses `key` parameter to avoid full recomposition on data changes
- `TvLazyVerticalGrid` from `androidx.tv.foundation` (not standard Compose)
- Category switching: cached in `HomeViewModel.categoryItems` map — no reload if already cached
- `AsyncImage` from Coil 2.x with hardware acceleration and 200MB disk cache

**Filters** (POPULAR / TOP_RATED): not currently visible in UI — controlled via `HomeViewModel.selectFilter()`

### SearchScreen

**Purpose**: Text search with history and debounced results.

**UX flow**:
1. Screen title "Поиск" (floating, no header bar)
2. `OutlinedTextField` — user types search query (Android TV has on-screen keyboard)
3. D-pad Down from field → first result card
4. Recent searches shown as horizontally scrollable chips when query is empty
5. Results grid matches home grid (5-6 columns)

**Important**: On Android TV, text input is done via voice or on-screen keyboard. The `OutlinedTextField` must be focusable and support D-pad navigation.

### DetailsScreen

**Purpose**: Full-screen movie/series details with backdrop, metadata, and play action.

**UX flow**:
1. Full-screen backdrop image shifted right, fading left with gradient
2. Ambient color extracted from backdrop using `rememberAdaptiveAmbientColor` (Palette API)
3. Left panel (56% width): back button → logo/title → rating chip → meta → genres → description → progress → action buttons
4. D-pad flow: Back → Watch → Favorite (horizontal) — all connected with `FocusRequester`
5. "Watch" button opens `SourceSelectionDialog`

**Ambient color**: The `SidePosterDetailsLayout` background uses `ambientColor` (dominant color from backdrop) + horizontal gradient overlay for depth. The backdrop image is rendered with `graphicsLayer(CompositingStrategy.Offscreen)` + `BlendMode.DstIn` gradient for smooth left fade.

**Source selection**: `SourceSelectionDialog` is a `Dialog` composable (not bottom sheet — TV has no bottom sheets) with translation tabs, season selector, and episode grid.

### PlayerScreen

**Purpose**: Video playback with TV-optimized controls.

**UX flow**:
1. Full-screen `PlayerView` (ExoPlayer) — no OS player UI (`useController = false`)
2. Custom HUD: Top bar (back + title + season/episode + audio/quality badges) + bottom bar (seek bar + time + controls)
3. D-pad: Left/Right = ±10s seek; Center/Enter = play/pause; Up/Down = show controls
4. Controls auto-hide after 5 seconds during playback
5. Center HUD icon (play/pause/rewind/forward) appears briefly on action
6. Skip intro / Skip outro / Next episode buttons (bottom-right, animated in/out)

**Focus hierarchy**: Back → Seekbar → Play/Pause; modals trap focus inside.

**Session flow**: `PlayerViewModel` → `AllohaSessionManager` → `HlsProxyServer` (local NanoHTTPD on 127.0.0.1) → ExoPlayer reads `http://127.0.0.1:{port}/master.m3u8`

**Progress**: Saved to Room every ~10 seconds or on dispose. Resumes from saved position on next play.

### ContinueScreen

**Purpose**: Resume watching — shows all in-progress content.

**Layout**: 3-column grid of 16:9 cards with poster/backdrop, title, episode info, and progress bar.

**Card interaction**: Click → navigate to DetailsScreen (which picks up saved position).

**Empty state**: Centered icon + message. Well-designed empty states are important — don't skip them.

**Known issue**: Action dialog (delete/mark watched) is unreachable via D-pad. Must be redesigned for TV.

### ProfileScreen (Favorites)

**Purpose**: User's saved favorites, filterable by type.

**Layout**: Segmented tab bar (same physics as HomeScreen) + poster grid.

**Filter categories**: Все / Фильмы / Сериалы / Мульты — filtered client-side from Room DB.

### SettingsScreen

**Purpose**: App configuration — 4 category panels in a left-side list.

**Categories**: Воспроизведение / Интерфейс / Данные и хранилище / О приложении

**Layout**: Left side = category list with icons; Right side = settings items for selected category.
All interactive rows are `SlooshFocusableCard`-based with D-pad navigation.

---

## Data Layer

### API (`MoviesApi.kt`)

Base URL: `https://api-sloosh.vercel.app/`
Header: `X-API-Key: sloosh_app_sec_v1_8f93e14b2d07`

| Endpoint | Method |
|----------|--------|
| `api/v1/movies/popular` | GET ?page |
| `api/v1/movies/top-rated` | GET ?page |
| `api/v1/tv/top-rated` | GET ?page |
| `api/v1/cartoons` | GET ?page |
| `api/v1/anime` | GET ?page&order |
| `api/v2/movie/{id}` | GET ?v=2 |
| `api/v2/tv/{id}` | GET ?v=2 |
| `api/v1/tv/{id}/season/{season}` | GET |
| `api/v1/search` | GET ?query&page |
| `api/v1/images/tmdb/{size}/{path}` | GET (proxies TMDB images bypassing RU ISP blocks) |
| `api/v1/images/kp/{id}` | GET (Kinopoisk poster) |
| `api/v1/images/logos/{id}/original` | GET (Kinopoisk logo) |

Response envelope: `ApiEnvelope<T>` with `success: Boolean` and `data: T` (or `MediaResponse` with `items` / `results`).

### Caching Strategy

| Data type | Cache location | TTL |
|-----------|---------------|-----|
| API pages (popular/top/tv/cartoons/anime) | `MoviesRepository.instance` in-memory `ConcurrentHashMap` | App lifetime |
| Media details | `MoviesRepository.instance.detailsCache` | App lifetime |
| Alloha catalog | `AllohaRepository.cache` (in-memory) | 5 minutes |
| Images | Coil disk cache | Disk 200MB, memory 25% heap |
| Playback progress | Room DB | Permanent |
| Favorites | Room DB | Permanent |
| Search history | Room DB | Permanent |

> `MoviesRepository.instance` is an Application-level shared singleton, caching all requests globally across all screens and ViewModels.

### Room Database

Tables: `playback_progress`, `favorites`, `search_history`

Accessed via `PlaybackProgressStore` (progress) and `DetailsViewModel`/`ProfileViewModel` (favorites) and `SearchViewModel` (history).

### Alloha Stream Resolution Pipeline

```
DetailsScreen (user taps "Watch")
    ↓ SourceSelectionDialog (pick translation/season/episode)
        ↓ AllohaRepository.fetchAllohaData(mediaId) → AllohaApiResult
            ↓ User selects → iframeUrl passed to PlayerScreen
                ↓ PlayerViewModel.initPlayer(iframeUrl)
                    ↓ AllohaSessionManager.startSession(iframeUrl)
                        ↓ [AllohaSessionHolder singleton]
                            ├── Path 1 (fast): AllohaRuntimeResolver → HTTP hops → AllohaRuntimeParser
                            └── Path 2 (fallback): Headless WebView + JS injection → intercept m3u8 URL
                                ↓ HlsProxyServer (NanoHTTPD on localhost)
                                    ↓ ExoPlayer → http://127.0.0.1:{port}/master.m3u8
```

**AllohaRuntimeResolver** (`AllohaRuntimeResolver.kt`):
1. Fast HTTP hop: fetch iframe HTML → parse with `AllohaRuntimeParser`
2. WebView fallback: inject JS that hooks XHR/fetch/WebSocket to intercept stream URLs
3. Intercepts `/bnsi/` endpoint (Alloha's config API) and `master.m3u8` URLs
4. 20-second timeout — on timeout tries already-collected payloads
5. Rotates 4 User-Agent strings to avoid detection

---

## Known Issues & Performance Problems

> Read `errors.md` for current active errors. Update it when you fix or discover issues.

### Critical Performance Issues — All Resolved ✅

1. **[RESOLVED] Side Drawer Lag & Hierarchy Recreation Storm**
   - In `MainActivity.kt`, the side drawer previously used `if (showDrawer) { ModalNavigationDrawer { AppNavHost } } else { AppNavHost }`, disposing and recreating the entire screen hierarchy on every navigation to details or player!
   - **Resolution**: `AppNavHost` is now permanently mounted inside `ModalNavigationDrawer`, and `showDrawer` is checked inside `drawerContent`. Drawer items use `panelWidth` and `graphicsLayer(alpha)` animations rather than recomposing layout trees.

2. **[RESOLVED] `SlooshFocusableCard` — `rememberInfiniteTransition` running when unfocused**
   - **Resolution**: Light beam animation converted to `Animatable(0f)` with `LaunchedEffect(isFocused)` that runs strictly when `isFocused == true`.

3. **[RESOLVED] `AppSettings` instantiated inside `AnimatedContent`**
   - **Resolution**: Hoisted `appSettings` and `gridColumns` to top level in `SearchScreen` and `ProfileScreen`.

4. **[RESOLVED] `MoviesRepository` created per-ViewModel, not shared**
   - **Resolution**: Converted to `MoviesRepository.instance` singleton across all ViewModels with shared thread-safe caches.

5. **[RESOLVED] Cartoons and Anime using search API**
   - **Resolution**: New backend (`api-sloosh.vercel.app`) provides dedicated endpoints `getCartoons(page)` and `getAnime(page, order)`.

6. **[RESOLVED] `HomeViewModel` prefetching sequentially**
   - **Resolution**: Parallel prefetching across IO dispatchers for all 5 categories (`ALL`, `MOVIES`, `SERIES`, `CARTOONS`, `ANIME`).

7. **[RESOLVED] Missing shimmer placeholders in grids**
   - **Resolution**: Integrated `PosterGridShimmer` and `ContinueGridShimmer` in `HomeScreen`, `SearchScreen`, and `ContinueScreen`.

### UI / Focus Issues — Resolved ✅

8. **[RESOLVED] `ContinueScreen` action dialog unreachable via D-pad**
   - **Resolution**: Redesigned 16:9 cards matching iOS `ContinueWatchingCard.swift` with D-pad context action menu, explicit `firstActionFocusRequester`, and `LaunchedEffect` focus trap.

9. **[RESOLVED] `SourceSelectionDialog` focus issues**
   - **Resolution**: Converted to full-screen in-hierarchy overlay `SourceSelectionOverlay` with strict focus management.

10. **[RESOLVED] Missing `BackHandler` for dialogs and overlays**
    - **Resolution**: `BackHandler` integrated for all overlays and modals across `DetailsScreen` and `ContinueScreen`.

### Architecture Issues

13. **No Dependency Injection**
    - All dependencies manually instantiated in ViewModel constructors and composables
    - Not blocking, but makes unit testing impossible and creates duplicate instances
    - **Note**: Do not add Hilt/Koin without user request — just document the issue

14. **`AllohaSessionHolder` global mutable singleton is not thread-safe**
    - `AllohaSessionHolder.session = session` assignment without synchronization
    - Could cause race conditions if multiple player instances exist (unlikely but possible)

---

## Component Usage Guide

### SlooshFocusableCard

```kotlin
SlooshFocusableCard(
    onClick = { /* action */ },
    shape = ContinuousCapsule,               // or ContinuousRoundedRectangle(16.dp)
    focusedScale = 1.08f,                    // optional, default 1.05f
    modifier = Modifier
        .focusRequester(myFocusRequester)    // add for programmatic focus
        .onPreviewKeyEvent { /* D-pad routing */ }
) { isFocused ->
    // isFocused is true when card has focus — use for visual state
    Box(modifier = Modifier.background(
        if (isFocused) Color.White.copy(alpha = 0.25f)
        else Color.White.copy(alpha = 0.10f)
    )) { ... }
}
```

### SlooshButton

```kotlin
SlooshButton(
    text = "Смотреть",
    isWhite = true,        // white fill + black text (primary CTA)
    isPrimary = false,     // alternative primary style
    icon = { Icon(...) }, // optional leading icon
    onClick = { ... },
    modifier = Modifier.focusRequester(...)
)
```

### ratingColor()

```kotlin
// Returns the correct color for a rating value
val color = ratingColor(item.rating ?: 0.0)  // Green / Gray / Red
// Use ONLY in rating badge Box backgrounds — NOT for any other UI elements
```

### rememberAdaptiveAmbientColor()

```kotlin
// Extracts dominant color from image URL for background depth
val ambientColor by rememberAdaptiveAmbientColor(
    primaryUrl = backdropUrl,
    fallbackUrl = posterUrl
)
// Use as screen background with gradient overlays on top
```

---

## Development Workflow

### Building & Testing

- **Build**: Android Studio → Run on connected Android TV device or emulator
- **DO NOT** build from command line unless asked
- Preferred test device: physical Android TV with physical remote control
- Test all D-pad navigation paths after any UI change

### Code Style

- Kotlin idiomatic code: `val` over `var` where possible, extension functions, lambdas
- No unused imports
- Compose: extract large composables into smaller private `@Composable` functions
- ViewModel: keep UI logic in Composables, business/data logic in ViewModels
- Avoid `@SuppressLint` unless absolutely necessary

### Interrogation Before Coding

> **ALWAYS ask the user before making significant changes.** Describe:
> 1. What you plan to change and exactly why
> 2. What it will look like / how it will behave
> 3. What you WON'T touch (reassurance)
>
> Wait for explicit "go ahead" before modifying code.
> For simple bug fixes with obvious solutions — fix directly and report.

### What NOT to Do

- **DO NOT** add any green color to UI elements (only rating badges use green)
- **DO NOT** add loading spinners where shimmer skeletons would be better
- **DO NOT** add new screens, features, or UI elements without user request
- **DO NOT** remove the border light-beam effect from `SlooshFocusableCard`
- **DO NOT** replace `ContinuousCapsule` / `ContinuousRoundedRectangle` with `RoundedCornerShape`
- **DO NOT** use `.blur()` modifier (performance issue on TV hardware)
- **DO NOT** add visible header bars with solid backgrounds — use gradient scrims only
- **DO NOT** use `ultraThinMaterial` or any Material blur effects
- **DO NOT** add redundant empty state messages or decorative placeholder elements ("water" in UI)
- **DO NOT** break existing working functionality — always read affected code before editing
- **DO NOT** modify files in `iOS-sloosh/` — reference only

---

## Quick Reference: Key Files for Common Tasks

| Task | Files to modify |
|------|----------------|
| Add new screen | `MainActivity.kt` (route), new `Screen.kt` + `ViewModel.kt` |
| Fix focus navigation | The specific Screen.kt — add `FocusRequester` + `onPreviewKeyEvent` |
| Change colors | `Color.kt` |
| Change typography | `Type.kt` |
| Add new settings | `SettingsScreen.kt` + `AppSettings.kt` |
| Fix Alloha parsing | `AllohaRuntimeParser.kt`, `AllohaRuntimeResolver.kt` |
| Fix progress tracking | `PlaybackProgressStore.kt`, `PlayerViewModel.kt` |
| Fix image loading | `SlooshApplication.kt` (Coil config), `Models.kt` (URL normalization) |
| Improve animations | The specific Screen.kt or `SlooshFocusableCard.kt` |
| Add API endpoint | `MoviesApi.kt` + `MoviesRepository.kt` |
