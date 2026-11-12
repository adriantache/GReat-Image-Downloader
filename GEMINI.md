# GReat Image Downloader - Agent Instructions & Guidelines

## Versioning & Release Rule (CRITICAL)
Whenever committing and pushing changes intended for release:
1. **Always Check Git Tags**: Fetch remote tags (`git fetch --tags`) and check the highest released tag (`git tag --sort=-v:refname | Select-Object -First 5`).
2. **Always Bump Versions in Both Modules**:
   - `app/build.gradle`: `versionCode` and `versionName`
   - `wear/build.gradle`: `versionCode` and `versionName`
   - Both modules **must always stay in sync** with identical `versionCode` and `versionName`.
   - Pattern: Version `1.2.9` -> `versionCode = 10209`, `versionName = "1.2.9"`.
3. **Run Unit Tests**: Ensure `./gradlew testDebugUnitTest` passes before committing and pushing.
4. **Include Version Bumps in Commits**: Never push release-bound code changes without bumping `versionName` and `versionCode`.

## Code & Architecture Guidelines
- **HTTP / Ricoh GR III**: The camera runs an embedded, single-threaded HTTP server. Never enable connection reuse (use `ConnectionPool(0, 1, TimeUnit.SECONDS)` and `Connection: close`). Avoid frequent status polling during downloads.
- **Wi-Fi Connectivity**: Use local-only `NetworkRequest` without `NET_CAPABILITY_INTERNET`. Preserve BSSID across retries to prevent Android from erasing credentials on screen-off.
- **Wear OS Sync**: Wear OS module communicates via `play-services-wearable`. Ensure data schemas and serialization in `wear/` and `app/` stay aligned.
