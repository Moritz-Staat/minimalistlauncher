# CLAUDE.md — minimalistlauncher

**Nach jedem Kompaktieren zuerst PROGRESS.md lesen.**

## Projektziel
Android-Launcher, funktionale Kopie des Niagara Launchers. Zielgeraet: Nothing Phone (2),
Nothing OS 4.x, Android 16. Kein Vertrieb, laeuft nur auf diesem einen Geraet.

## Wichtigste Regel
Der Nothing-Launcher bleibt installiert. Nichts schreiben, was ihn entfernt, deaktiviert
oder diese App zwangsweise als einzigen Launcher setzt.

## Verbindliche Technikentscheidungen (nicht neu verhandeln)
- Kotlin, Jetpack Compose, Material 3
- Ein einziges App-Modul (`:app`), kein Multi-Module
- minSdk 31, targetSdk 36, compileSdk 36, JDK 21
- Package `de.moritzstaat.launcher`
- Single-Activity, MVVM, StateFlow, viewModelScope
- Room (KSP) fuer Nutzerdaten, DataStore fuer Einstellungen
- App-Quelle: `LauncherApps` + `UserManager`. Kein `queryIntentActivities`, kein `QUERY_ALL_PACKAGES`
- Netzwerk nur Open-Meteo ueber `HttpURLConnection`. Keine Google Play Services
- Bibliotheken: Compose BOM, Room, DataStore, Coil, Palette, WorkManager. Sonst nichts ohne
  Eintrag in DECISIONS.md
- Verteilung: Debug-APK per `adb install`, kein Play Store

## Build
```
./gradlew assembleDebug     # muss gruen sein, bevor eine Etappe fertig ist
./gradlew testDebugUnitTest # JUnit-Tests fuer framework-freie Logik
```
Lokale Toolchain (nicht im Repo): JDK 21 unter
`C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot`,
Android SDK unter `C:\Users\morit\Android\sdk` (via `local.properties`).

## Regeln
- Kommentare und Commit-Messages englisch, Doku-Dateien deutsch.
- Kleine, benannte Composables und Klassen. Keine 500-Zeilen-Dateien.
- Warnungen aufraeumen, nicht unterdruecken.
- Ein Commit pro Etappe, danach pushen. PROGRESS.md vorher aktualisieren.
- Jede selbst getroffene Entscheidung landet mit einer Zeile Begruendung in DECISIONS.md.
- Alles, was nur am echten Geraet pruefbar ist, kommt als konkreter Pruefschritt in TESTPLAN.md.
- Kein Geraet, kein Emulator verfuegbar. Verifikation = Build gruen, Unit-Tests gruen,
  Code gegengelesen.
