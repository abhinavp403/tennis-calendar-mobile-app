# Publishing to Google Play

The signed release **AAB** is produced by the project and verified. The Play Console steps
below require your Google Play developer account and must be done by you — they involve
account authentication and irreversible public publishing.

## 1. Signing (already wired)

A release keystore was generated at `keystore/release.jks` and credentials live in
`local.properties` (git-ignored) under `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` /
`RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`. `app/build.gradle.kts` reads them (falling back to
environment variables of the same names for CI).

> Back up `keystore/release.jks` and its passwords securely. If you enroll in Play App Signing,
> Google manages the app-signing key and this keystore becomes your *upload* key.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab   (R8 minify + resource shrinking on)
```

Verify the signature:
```bash
"$JAVA_HOME/bin/jarsigner" -verify -verbose:summary app/build/outputs/bundle/release/app-release.aab
```

## 2. App identity

- **Package (applicationId):** `com.tennis.calendar`
- **versionCode:** 1 · **versionName:** 1.0.0 (bump both for each upload)
- **minSdk:** 24 · **targetSdk:** 36

## 3. Store listing copy (draft — reuse/edit)

**Title:** Tennis Calendar

**Short description (≤80 chars):**
> ATP & WTA 2026 season calendar — results, rankings, and year-to-date player stats.

**Full description:**
> Follow the entire 2026 tennis season in one place. Tennis Calendar shows every ATP and WTA
> tournament on a clean month grid, color-coded by tier — Grand Slams, the Tour Finals, 1000s,
> 500s and 250s. Tap any completed event for the champion, runner-up and final score.
>
> • Switch instantly between the ATP and WTA tours
> • Filter by surface: Hard, Indoor, Clay or Grass
> • Year-to-date Player Stats with level-weighted points, titles, finals and finals win %
> • Per-player profiles with best surface and full title / runner-up lists
> • Live rankings with ▲▼ movement between snapshots
> • Month results summary and a season-long Champions Wall
> • Search for any tournament or player
>
> Data refreshes on launch and is cached for offline viewing. No account, no ads, no tracking.

## 4. Data safety form

- Data collected/shared: **None.**
- The app makes read-only HTTPS requests to a public GitHub Gist and stores only a copy of that
  public tournament/ranking data on the device for offline use. No personal or device data is
  collected, and nothing is sent anywhere.

## 5. Content rating

Questionnaire answers: no objectionable content → **Everyone / PEGI 3**.

## 6. Privacy policy (required)

A privacy policy URL is mandatory even though the app collects nothing. Host a short page
stating: "Tennis Calendar does not collect, store, or share any personal data. It reads a public
tennis data file over HTTPS and caches it on your device for offline use." Put that URL in the
Play Console *App content → Privacy policy* field. (GitHub Pages or a Gist works.)

## 7. Upload flow (you perform these)

1. Play Console → **Create app** (`com.tennis.calendar`, default language, app, free).
2. **Testing → Internal testing → Create release** → upload `app-release.aab`.
3. Complete: store listing (copy above), graphics (icon 512×512, feature graphic 1024×500,
   ≥2 phone screenshots), content rating, data safety, privacy policy URL, target audience.
4. Add internal testers, roll out, and install via the opt-in link on a physical device to
   verify (`bundletool build-apks --mode=universal` also works for a local check).
5. When satisfied, **promote** the internal release to **Production**.

## 8. Local device check from the AAB (optional)

```bash
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks --mode=universal \
  --ks=keystore/release.jks --ks-key-alias=tennis \
  --ks-pass="pass:$RELEASE_STORE_PASSWORD" --key-pass="pass:$RELEASE_KEY_PASSWORD"
bundletool install-apks --apks=app-release.apks
```
