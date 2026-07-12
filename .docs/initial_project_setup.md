# DoodleFrens — Initial Project Setup

Modernized the tutorial's old Groovy build scripts (Doodlekong by Philipp Lackner) to modern Kotlin DSL with `libs.versions.toml` version catalog, following the same patterns as the [Runique project](file:///F:/ProjectSalmonBoy-WORK/ProjectMobileDev/PhilipLacknerCourse/EssentialAndroidCourse/runiquesalmonboy/runique-salmonboy).

**Architecture**: Full Compose (no XML/ViewBinding). Single-module for now.

---

## Toolchain

| Tool | Version |
|---|---|
| AGP | 9.2.1 |
| Kotlin | 2.4.0 |
| Gradle | 9.4.1 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2026.06.01 |

---

## What Changed vs Tutorial

### Removed (not needed in full Compose / AGP 9.x)
- ~~`kotlin-android` plugin~~ — AGP 9.x has built-in Kotlin support
- ~~`kotlinOptions` block~~ — JVM target derived from `compileOptions` in AGP 9.x
- ~~`kapt`~~ — replaced by **KSP** (faster)
- ~~`appcompat`~~ — Compose handles theming
- ~~`material` (XML)~~ — replaced by Material3 Compose
- ~~`constraintlayout`~~ — Compose has its own layout system
- ~~`recyclerview`~~ — replaced by `LazyColumn` / `LazyRow`
- ~~`navigation-fragment-ktx` / `navigation-ui-ktx`~~ — replaced by Navigation Compose
- ~~`navigation.safeargs.kotlin` plugin~~ — not used with Navigation Compose
- ~~`hilt-lifecycle-viewmodel`~~ — removed in modern Hilt
- ~~`lifecycle-extensions`~~ — deprecated, replaced by individual lifecycle artifacts
- ~~`easypermissions`~~ — unmaintained, will add if needed
- ~~`viewBinding`~~ — full Compose, not needed

### Modernized

| Library | Tutorial Version | Current Version |
|---|---|---|
| Kotlin | 1.4.31 | 2.4.0 |
| Core KTX | 1.3.2 | 1.19.0 |
| Compose BOM | — | 2026.06.01 |
| Navigation | 2.3.5 (Fragment) | 2.9.8 (Compose) |
| Hilt | 2.33-beta (kapt) | 2.60.1 (KSP) |
| Lifecycle | 2.3.1 | 2.11.0 |
| Activity KTX | 1.2.2 | 1.13.0 |
| Retrofit | 2.9.0 | 3.0.0 |
| OkHttp | 4.9.0 | 5.4.0 |
| Gson | 2.8.6 | 2.14.0 |
| DataStore | 1.0.0-alpha08 | 1.2.1 |
| Coroutines | 1.4.3 | 1.11.0 |
| Timber | 4.7.1 | 5.0.1 |
| Lottie | 3.4.4 (XML) | 6.7.1 (Compose) |
| Ktor | — | 3.5.1 |
| Material Icons | — | 1.7.8 |

### Added (not in tutorial)
- **Compose BOM** — manages all Compose library versions
- **Material3 Compose** — modern Material Design
- **Material Icons Extended** — full icon set
- **Navigation Compose** — type-safe Compose navigation
- **Lifecycle Runtime Compose** — `collectAsStateWithLifecycle()`
- **Lifecycle ViewModel Compose** — `viewModel()` in Compose
- **Lottie Compose** — Compose-compatible Lottie animations
- **KSP plugin** — Kotlin Symbol Processing for Hilt

---

## Files Modified

### [libs.versions.toml](file:///f:/ProjectSalmonBoy-WORK/ProjectMobileDev/Passion%20Project/DoodleFren/gradle/libs.versions.toml)
Full rewrite. Following Runique's structure:
- `module` shorthand for most library declarations
- Project version constants in `[versions]`
- `[bundles]` section for grouped dependencies: `compose`, `compose-debug`, `ktor`, `retrofit`, `lifecycle`, `coroutines`

### [build.gradle.kts](file:///f:/ProjectSalmonBoy-WORK/ProjectMobileDev/Passion%20Project/DoodleFren/build.gradle.kts) (root)
Declares 4 plugins with `apply false`:
- `android.application`, `kotlin.compose`, `ksp`, `hilt.android`

### [app/build.gradle.kts](file:///f:/ProjectSalmonBoy-WORK/ProjectMobileDev/Passion%20Project/DoodleFren/app/build.gradle.kts)
Full rewrite with:
- Plugins: `android.application`, `kotlin.compose`, `ksp`, `hilt.android`
- `compose = true` (no ViewBinding)
- `META-INF` packaging excludes (from tutorial)
- Java 11 compatibility
- Clean grouped dependencies using bundles

### [gradle.properties](file:///f:/ProjectSalmonBoy-WORK/ProjectMobileDev/Passion%20Project/DoodleFren/gradle.properties)
Added:
- `android.useAndroidX=true`
- `android.nonTransitiveRClass=true`

### [settings.gradle.kts](file:///f:/ProjectSalmonBoy-WORK/ProjectMobileDev/Passion%20Project/DoodleFren/settings.gradle.kts)
Added:
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` — for future multi-module support

---

## AGP 9.x Gotchas Encountered

| Issue | Cause | Fix |
|---|---|---|
| Duplicate `kotlin` extension | AGP 9.x bundles Kotlin internally | Remove `kotlin-android` plugin |
| `kotlinOptions` unresolved | AGP 9.x removed this DSL block | Remove block; JVM target follows `compileOptions` |
| `BaseExtension not found` | Hilt < 2.59.2 uses removed AGP APIs | Upgrade Hilt to 2.59.2+ |
| KSP version not found | Old `1.0.x` versioning scheme | Use KSP2 format: `2.2.10-2.0.2` |

---

## Resolved Issues

- **Scarlet Deprecation**: Scarlet is archived. Switched to **Ktor WebSockets** (3.5.1) for future-proofing and better coroutine support.
