# Dreamora (compat branch)

Android novel reader (Kotlin, Jetpack Compose, Material 3) with a **compatibility
layer for extension ecosystems that are not Dreamora's own Lua plugins**: Legado
`bookSource` JSON, Mihon/Tachiyomi/keiyoushi indexes, LNReader, Mangayomi and
CloudStream.

This README describes what this branch changes. The upstream app (library, reader,
TTS, translation, EPUB/FB2, backups) is unchanged in behaviour.

Upstream project: `HnDK0/NoveLA` · License: [GPL-3.0](LICENSE)

---

## What's new here

### 1. Extension repositories (JSON indexes)

A repository manager lives next to the Lua plugin repository: **Finder → Extensions →
"Third-party extension repos"**. One pasted URL is enough — a repository root, an
`index.json`/`bookSource.json` blob, a `releases/latest` page, a GitLab project, or a
raw file on any domain. The format is detected from the content, not from a hard-coded
site.

| Ecosystem | Indexes understood | Browse | Download | Runs in the app |
|---|---|:---:|:---:|:---:|
| **Legado** `bookSource` | object / array / `{bookSources:[…]}` export / one-file-per-source repos / `.txt` + URL-array subscription lists | ✅ | ✅ | ✅ |
| **Mihon / Tachiyomi / keiyoushi** | `repo.json` pointer → `index.json` (v2), legacy flat `index.min.json` array | ✅ | ✅ APK | ❌ no extension-API runtime |
| **LNReader** | `.dist/plugins.min.json` | ✅ | ✅ JS | ❌ no `PluginBase` runtime |
| **Mangayomi** | `repo.json`, `index.json`, `novel_index.json` | ✅ | ✅ JS / Dart | ❌ no `MProvider` runtime |
| **CloudStream** | `repo.json` → `pluginLists[]` → nested `index.min.json` | ✅ | ✅ `.cs3` | ❌ no `MainAPI` runtime |

A source whose runtime is not part of the build is stored and labelled (with the
reason) instead of being silently dropped or falsely reported as installed.
Details, format notes and the follow-up list: [EXTENSION_COMPATIBILITY_MATRIX.md](EXTENSION_COMPATIBILITY_MATRIX.md).

### 2. UI rebrand

Settings and Extensions were rebuilt on the shared design system
(`coreui/theme/NovelaTokens.kt`, `NovelaColors.kt`, `NovelaScaffold.kt`,
`Typography.kt`): one spacing/radius/motion scale, semantic colors derived from the
active theme, section headers, filter chips, empty states. Motion is used where it
explains a change (section switching, expanding rows, install progress) and is
disabled under `Reduced motion`. Scope and the preserved-feature checklist:
[REDESIGN.md](REDESIGN.md).

### 3. Signing key in the repo

`keystore/dreamora-release.p12` — a self-generated key with empty passwords, used for
debug and release builds alike, so any machine or CI run produces APKs that install
over each other. No `key.properties`, no per-machine keystore. Trade-offs and how to
override it with a private key: [keystore/README.md](keystore/README.md).

### 4. CI

| Workflow | When | Output |
|---|---|---|
| [`CI`](.github/workflows/ci.yml) | every push and PR | module compiles, unit tests (fatal), **debug APK signed by the repository key** — the step compares the APK's certificate fingerprint with `keystore/dreamora-release.p12` and fails on a mismatch |
| [`Debug APK`](.github/workflows/debugApk.yml) | manual | debug APK for a chosen branch/SHA |
| [`Nightly Build`](.github/workflows/nightly.yml) | push to `default` | release APK + R8 mapping on the `nightly` GitHub release |
| [`Build & Publish`](.github/workflows/buildRelease.yml) | manual | draft release |

### 5. Bugs fixed and performance work in this pass

- Sources with an unknown language (a Legado `bookSource` has no language field) are no
  longer hidden by the Finder's language filter.
- `repo.json`-based Mihon repositories parse correctly even though `index.min.json` is
  now a stub for outdated clients; "Outdated App" rows are dropped instead of listed.
- Legado payloads keep their **raw** JSON, so Legado fields Dreamora does not model
  (`customOrder`, `lastUpdateTime`, rule defaults) survive into the rule engine.
- `CompatStorage` no longer touches the disk on the caller's thread during
  construction, and the repository catalog is served from a disk cache instead of
  waiting for the first network refresh.
- Extension list state is derived from flows (`combine`) rather than manual snapshots,
  removing the "empty list until you refresh" class of bug.

---

## Build

```bash
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew :tooling:extension_compat:testDebugUnitTest
./gradlew assembleRelease -PlocalPropertiesFilePath=custom.properties   # with a private key
```

Requirements: JDK 21, Android SDK 36 (`local.properties` with `sdk.dir`).
Or run the **CI** workflow and download the APK from the run.

## Adding sources

- **Lua plugins (native):** Extensions → ⚙️ → repository URL
  (`Stlucifer15t/external-sources` by default).
- **Legado bookSource JSON:** Extensions → *Third-party extension repos* → paste the
  repo or file URL → Install. The source appears in the Finder immediately.
- **Mihon/LNReader/Mangayomi/CloudStream:** same flow — the entry is fetched and kept,
  ready for the runtime that will execute it.
