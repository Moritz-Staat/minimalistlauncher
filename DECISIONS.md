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

## 2 — Launcher-Grundgeruest
- **Kein `CATEGORY_LAUNCHER` im Intent-Filter**, nur `HOME` + `DEFAULT` + `LAUNCHER_APP`.
  Sonst taucht der Launcher in seiner eigenen App-Liste auf.
- **`BackHandler(enabled = true)` global.** Die Zurueck-Geste wird immer verschluckt; offene
  Overlays schliessen, sonst passiert nichts. Ein Home-Screen hat kein Zurueck.
- **`windowDisablePreview` + `windowAnimationStyle=@null`.** Ein Starting-Window ueber dem
  Wallpaper blitzt beim Home-Druck sichtbar auf.
- **`HomeRole.createSettingsIntent()` als Fallback** auf `ACTION_HOME_SETTINGS`, falls ein
  OEM-Build den Rollendialog verweigert. Erzwungen wird nichts.
- **`tools/build.sh`** pinnt JDK 21, weil `java` nicht in der PATH-Umgebung liegt.

## 3 — App-Index
- **`Collator.SECONDARY` statt `TERTIARY`.** Bei TERTIARY sortiert die deutsche Collation
  Kleinbuchstaben vor Grossbuchstaben; "gmail" landete dann vor "Gmail". Gross/Klein
  entscheidet jetzt nicht, der Tiebreak ist ein einfacher String-Vergleich.
- **Eigene Konstanten fuer die Trim-Level (40/80).** `TRIM_MEMORY_BACKGROUND` und
  `TRIM_MEMORY_COMPLETE` sind auf API 36 deprecated, der Callback liefert die Werte aber
  weiterhin. `TRIM_MEMORY_UI_HIDDEN` wird bewusst ignoriert — das kommt bei jedem App-Start
  und wuerde den Icon-Cache staendig leeren.
- **`AppKey` wird als flacher String `paket/klasse#serial` persistiert.** Eine Spalte statt
  drei in jeder Tabelle und im JSON-Backup; die Trennzeichen kommen in Paket- oder
  Klassennamen nicht vor.
- **`AppIndex` als eigene Schicht ueber `AppRepository`.** Das Repository liefert nur, was das
  System kennt; Custom-Labels und versteckte Apps kommen erst darueber dazu.
- **Manuelle DI ueber `ServiceLocator`.** Hilt waere eine weitere Bibliothek und Startzeit
  fuer eine Handvoll Singletons.
- **Icon-Cache in Bytes begrenzt (1/8 Heap, hart bei 96 MB).** Wenige riesige Icons duerfen
  den Cache nicht leerdruecken; Budget aus Etappe 17.

## 4 — Homescreen
- **Uhr bei 22 % der Bildschirmhoehe statt exakt 25 %.** Mit Statusleisten-Inset landet der
  optische Schwerpunkt sonst zu tief.
- **App-Liste als eigene Ebene ueber dem Homescreen, nicht als BottomSheet-Komponente.**
  Material3s ModalBottomSheet bringt eigenes Scrim- und Back-Verhalten mit, das mit der
  Regel "Zurueck verlaesst den Launcher nie" kollidiert.
- **Blur und Verschiebung ueber `graphicsLayer`-Lambdas.** Der Fortschritt wird erst in der
  Draw-Phase gelesen; Ziehen loest damit keine Rekomposition der Liste aus.
- **Sheet-Fortschritt in einem `Animatable`, Bedienung ueber NestedScroll.** Eine
  durchgehende Geste kann so scrollen und danach die Liste wieder wegschieben.
- **Langdruck auf die freie Flaeche oeffnet die Einstellungen** (Etappe 14 zieht das
  ohnehin so vor) — sonst gibt es bei leerer Favoritenliste keinen Weg in die Einrichtung.
- **Icons ueber einen eigenen `IconLoader`, nicht ueber Coil.** Coil bringt fuer
  `LauncherActivityInfo`-Drawables nichts; es bleibt fuer Cover-Bilder in Etappe 8.
