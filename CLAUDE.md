# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
./gradlew assembleDebug      # build debug APK
./gradlew compileDebugKotlin # compile only, faster
./gradlew test                # run unit tests
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

**归物本 (GuiWu Ben)** — personal item/purchase tracker. Record items (name, price, purchase date) and see stats (total assets, daily cost, monthly trends, price share pie chart).

- **Single Activity** (`MainActivity.kt`) — creates Room DB inline, passes DAO into Compose root
- **No ViewModel, no DI framework** — state managed at composable level with `remember`/`mutableStateOf`, DAO passed as parameter through composable tree
- **Pure Compose UI** — no XML layouts
- **Navigation**: Jetpack Navigation Compose. Two bottom-nav tabs (首页 / 统计), plus add/edit routes. `GuiWuNavGraph.kt` is the root composable with `Scaffold` + `NavigationBar` + `NavHost`

### Data Layer

- `GuiWuItem` — Room `@Entity`: `id: String` (UUID PK), `name`, `price: Double`, `date: LocalDate`, `createdAt: Long` (epoch millis)
- `GuiWuDao` — `@Dao`: `getAllItems(): Flow<List<GuiWuItem>>` ordered by `createdAt DESC`, `insertItem`, `deleteItem`
- `AppDatabase` — version 2, includes `MIGRATION_1_2` (adds `createdAt` column)
- `Converters` — Room `@TypeConverter` for `LocalDate` ↔ `String`

### UI Layer

| File | Purpose |
|---|---|
| `navigation/GuiWuNavGraph.kt` | Root: Scaffold + BottomNavBar + NavHost (routes: `home`, `stats`, `add_item`, `add_item/{itemId}`) |
| `ui/HomeScreen.kt` | Item list, summary card, sort (by price/time/default=createdAt), long-press to edit/delete, FAB to add |
| `ui/AddEditScreen.kt` | Add/edit form: name, price, date (year/month/day dropdowns) |
| `ui/DateDropdowns.kt` | Reusable `ExposedDropdownMenuBox` for year/month/day selection (2000–current year) |
| `ui/StatScreen.kt` | Stats shell: TabRow + HorizontalPager with 2 tabs |
| `ui/StatTrendTab.kt` | Vico column + line chart: monthly spend + item count, x-axis shows yyyy-MM labels via ExtraStore |
| `ui/StatPieTab.kt` | Vico pie chart: price share, 9 custom colors, white stroke borders, legend with % |
| `ui/StatCalculations.kt` | Pure data functions: `getMonthlyTrends`, `getPieData`, `getDailyCostList` |
| `ui/SortType.kt` | Enum: DEFAULT, PRICE_ASC, PRICE_DESC, TIME_ASC, TIME_DESC |

### Theme

Material 3 with light/dark color schemes, dynamic color support (Android 12+), purple palette.

## Key Dependencies

- Compose BOM `2026.05.00` — **must stay aligned with Kotlin 2.2.10 compiler**. Older BOMs cause `NoSuchMethodError` at runtime
- Vico `3.1.0` — chart library, requires `material-icons-core` (explicit dep since BOM 2026 split it out)
- Room `2.8.4` with KAPT
- `ExposedDropdownMenuBox` uses `menuAnchor(MenuAnchorType.PrimaryNotEditable)` — the old no-arg `menuAnchor()` crashes on newer BOMs

## Gotchas

- BOM version must match Kotlin compiler version. If upgrading Kotlin, upgrade BOM too
- `material-icons-core` is a separate dependency since BOM 2026
- Vico `CartesianValueFormatter` takes `(context, x, chartValues)` — use `ExtraStore.Key` + `extras {}` in `runTransaction` to pass category labels
- Pie chart `Slice` params are `strokeFill` and `strokeThickness`, not `stroke`
- `PieChart.SliceProvider.series` cycles through provided slice definitions — provide at least as many slices as data items or colors will repeat
