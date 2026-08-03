# Tennis Calendar (Android)

Native Android app (Kotlin, Jetpack Compose, Material 3) for the 2026 tennis season —
month calendar, ATP/WTA tours, surface filters, level-weighted YTD player stats, rankings
with movement, month results, champions wall, and tournament/player search.

- **Adaptive theme** — follows the system light/dark setting, with tier accents tuned per theme.
- **Responsive layout** — single column in portrait; a two-pane controls-plus-calendar split in
  landscape.
- **Offline-first with live recovery** — auto-refreshes when connectivity returns, pull-to-refresh
  on the calendar, and a tap-to-retry offline banner.
- **Player search** shows country flags and is sorted alphabetically.

Data comes from the same public Gist the web app uses (no backend of our own):

- `.../c75d3f961da94fdeed16cdbd8e2ec08e/raw/tournaments.json`
- `.../c75d3f961da94fdeed16cdbd8e2ec08e/raw/rankings.json`
- `.../c75d3f961da94fdeed16cdbd8e2ec08e/raw/players.json` (name → country, for flags)

(Base: `https://gist.githubusercontent.com/abhinavp403/`.)

On launch the app fetches fresh, caches the last good payload to disk, and falls back to that
cache when offline. Permissions: `INTERNET` and `ACCESS_NETWORK_STATE` (to detect reconnection).

## Architecture (MVVM)

```
model/            Serializable data models (Tournament, RankingPlayer, Tour, Surface, …)
data/             TennisApi (Retrofit), TennisCache (disk), TennisRepository (error boundary → DataError), ConnectivityObserver
domain/           PlayerStats (level-weighted points + dense rank), RankingKeys (mixed key normalization), NameMatch (accent-insensitive + surname fallback)
ui/calendar/      CalendarViewModel (StateFlow<UiState>, derives leaderboard + active snapshot), CalendarScreen, CalendarGrid
ui/common/        TournamentBadge (Coil + fallback), TournamentDetailSheet, FullScreenDialog
ui/stats/         PlayerStatsDialog (leaderboard), PlayerProfileDialog
ui/rankings/      RankingsDialog (▲▼ movement)
ui/summary/       MonthSummaryDialog, ChampionsWallDialog
ui/search/        TournamentSearchDialog, PlayerSearchDialog
di/               Koin module + TennisApp (Application)
```

- **Networking:** Retrofit 3 + OkHttp + `kotlinx.serialization`.
- **DI:** Koin.
- **Async/state:** Coroutines + `StateFlow`, collected lifecycle-safely.
- **Images:** Coil 3 — logos load from `assets/logos/<file>`, falling back to a tier-colored
  abbreviation badge (see below).
- **Errors:** mapped to a domain `DataError` at the repository; platform exceptions never leak to the UI.

The level-weighted stats and the active-ranking-snapshot selection are computed in the
domain/ViewModel layer, not the UI — see unit tests in `app/src/test`.

## Build & run

```bash
# JDK 21 (Android Studio's JBR works). If java isn't on PATH:
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests (stats / ranking keys / name match)
./gradlew :app:bundleRelease        # signed release AAB (needs signing config, see PUBLISHING.md)
```

Install the debug APK on an emulator/device from Android Studio, or:
`adb install app/build/outputs/apk/debug/app-debug.apk`.

## Design tokens

Material 3, adaptive light/dark. Tier accents (dark theme) — Grand Slam (2000) / Finals (1500)
violet `#A78BFA`; 1000 amber `#F59E0B`; 500 blue `#60A5FA`; 250 grey `#9CA3AF`; each has a darker
light-theme variant so chips stay legible on white. Tour accents — ATP `#0066CC`, WTA `#BE398D`.
The 1500 tier label is tour-aware ("ATP Finals" / "WTA Finals").

## Notes / known gaps

- **Logo assets are not in the repo or the Gist.** Every tournament therefore renders the
  fallback abbreviation badge, exactly as the web app does for missing logos. Drop the PNGs
  into `app/src/main/assets/logos/` (Gist filenames) and they appear automatically via Coil.
- The launcher uses a generated tennis-ball adaptive icon (API 26+). The legacy raster
  fallback (API 24–25) still shows the scaffold icon; regenerate via **Image Asset Studio** if
  you need it. The referenced `app-logo.png` did not exist in the repo.
- `applicationId` and Kotlin namespace are both `dev.abhinav.tenniscalendar` (matches the
  Play Console listing).
