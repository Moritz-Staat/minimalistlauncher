# DECISIONS

Jede selbst getroffene Entscheidung mit einer Zeile Begruendung.

## 0 — Aufsetzen
- **Repo liegt in `C:\Users\morit\minimalistlauncher`, nicht direkt im Arbeitsverzeichnis.**
  `git clone ... .` haette das komplette Home-Verzeichnis zum Repo gemacht.
- **Android SDK selbst installiert nach `C:\Users\morit\Android\sdk`** (cmdline-tools,
  platform-tools, platforms;android-36, build-tools;36.0.0) — auf dem Rechner war keines
  vorhanden, ohne SDK kein gruener Build.

## 1 — Projektgeruest
- **AGP 8.13.2 + Gradle 8.14.3 statt AGP 9.x.** AGP 9 verlangt compileSdk 37; compileSdk 36
  ist vorgegeben und nicht verhandelbar.
- **Compose BOM 2026.06.01 (Compose 1.11.4) statt 2026.08.00.** Compose 1.12 fordert
  compileSdk 37 und AGP 9.1+.
- **Kotlin 2.2.21 + KSP 2.2.21-2.0.5.** Kotlin-gekoppelte KSP-Version, damit Compiler und
  Prozessor garantiert zueinander passen.
- **`org.gradle.configuration-cache=true`.** Kaltere Builds sind hier egal, aber der
  Konfigurations-Cache faengt Fehler in Build-Skripten frueh ab.
- **Room-Schema-Export nach `app/schemas`.** Migrationen sind sonst nicht nachvollziehbar.
- **Eigenes Launcher-Icon als Adaptive Icon mit Monochrome-Layer**, damit die App im
  Nothing-Theme nicht aus dem Rahmen faellt.
