# PROGRESS

Status je Etappe: `offen` / `in Arbeit` / `fertig`.
Nach jedem Kompaktieren oder Neustart: diese Datei lesen, letzte unfertige Etappe finden,
dort weiterarbeiten.

| # | Etappe | Status | Zuletzt |
|---|--------|--------|---------|
| 0 | Repo, Doku, .gitignore | fertig | Repo geklont, CLAUDE.md/PROGRESS.md/DECISIONS.md/.gitignore angelegt |
| 1 | Projektgerüst (Gradle, Compose, Room/KSP, DataStore, Coil) | fertig | Version Catalog, App-Modul, leere MainActivity mit Compose; assembleDebug grün |
| 2 | Launcher-Grundgerüst (Manifest, Theme, HOME-Rolle) | fertig | HOME-Intent-Filter, transparentes Wallpaper-Theme, edge-to-edge, BackHandler, onNewIntent schließt Overlays, ROLE_HOME-Dialog |
| 3 | App-Index (LauncherApps, Collator-Sortierung, Room) | fertig | AppRepository über LauncherApps mit registerCallback, AppIndex mit Custom-Labels/Hidden, Collator de_DE, Room-Tabellen, IconCache/IconLoader; 13 Unit-Tests grün |
| 4 | Homescreen (Uhr, Favoriten, App-Liste) | fertig | Uhr+Datum, Widget-Slot, Favoriten (max. 8), App-Liste als LazyColumn mit stabilen Keys, Wisch-Sheet mit Blur; TESTPLAN.md angelegt |
| 5 | Wave-Alphabet | fertig | Leiste rechts, Gauß-Skalierung im graphicsLayer, derivedStateOf für den aktiven Index, SEGMENT_TICK, Gesten-Ausschluss; 8 Unit-Tests grün |
| 6 | Suche und App-Aktionen | fertig | Fuzzy-Matcher mit 6 Rangstufen, Suchfeld unten, Shortcuts/Kontakte/Websuche, Langdruck-Menü, Favoriten-Drag&Drop; 41 Unit-Tests grün |
| 7 | Benachrichtigungen | fertig | NotificationListenerService, Vorschau pro Paket mit Zähler, MessagingStyle, Tippen öffnet contentIntent, Wischen verwirft, Pro-App-Redaktion (DB v2); 52 Unit-Tests grün |
| 8 | Media-Widget | fertig | MediaSessionManager über den Notification-Listener, eigenes Composable, Palette-Akzent mit Kontrastprüfung, 30-s-Nachlauf, Kopfhörer-Einblendung; 59 Unit-Tests grün |
| 9 | Widget-Host | fertig | AppWidgetHost mit fester ID, eigener Wähler mit bindAppWidgetIdIfAllowed/ACTION_APPWIDGET_BIND, Konfigurations-Activity, HorizontalPager-Stapel, deleteAppWidgetId inkl. Orphan-Aufräumen (DB v3) |
| 10 | Pop-ups | fertig | Eine Pop-up-Komponente für App, Ordner und Widget; Wischen rechts öffnet, Blur über RenderEffect, Ordner alphabetisch in der Liste (DB v4) |
| 11 | Icons (Icon-Packs, Dots, Monochrom) | fertig | ADW/Nova-Erkennung über queries, appfilter.xml-Parser (res/xml + assets), Fuzzy-Auto-Replace, Pro-App-Override mit Pack-Bindung, Punkte- und Monochrom-Modus; 73 Unit-Tests grün |
| 12 | Theming | fertig | ThemeConfig in DataStore, vier Uhr-Stile inkl. Wortuhr, Material You/eigener Akzent/extra dunkel, Wallpaper-Dimmen und System-Blur, Statusleiste, eigene Schrift, Vorlagen und Theme-Export/Import; 102 Unit-Tests grün |
| 13 | Kalender und Wetter | fertig | CalendarContract.Instances mit ContentObserver, UTC-Regel für ganztägige Termine, Kalenderauswahl; Open-Meteo über HttpURLConnection, LocationManager ohne Play Services, JSON-Cache in DataStore, stündlicher WorkManager-Job; 123 Unit-Tests grün |
| 14 | Gesten | fertig | Fünf belegbare Gesten (Doppeltipp, unten/links/rechts, Langdruck) mit Aktionen inkl. App-Start; optionaler Bedienungshilfen-Dienst für Benachrichtigungsleiste und Bildschirmsperre; 130 Unit-Tests grün |
| 15 | Usage Breaker | fertig | Tageszähler pro Paket in Room (DB v5), optionaler Nutzungszugriff für die Systemzahlen, Pausenseite mit Countdown vor dem Start, Schwelle und Wartezeit einstellbar; 137 Unit-Tests grün |
| 16 | Einstellungen, Backup, Onboarding | fertig | Einstellungen als ein Bildschirm mit aufklappbaren Gruppen (SetupOverlay ersetzt), Sicherung aller Einstellungen und Nutzerdaten als JSON über SAF, vierstufige Einrichtung beim ersten Start; 144 Unit-Tests grün |
| 17 | Alltagstauglichkeit (Baseline Profile, R8) | fertig | R8-Regeln (Enums, Worker) und grüner Release-Build 3,2 MB statt 35 MB, handgeschriebenes Baseline-Profil plus profileinstaller, toter Code entfernt, Medien-Einstellungen nachgezogen; 144 Unit-Tests grün |
| 18 | Abschluss (TESTPLAN.md, README.md) | fertig | README.md mit Funktionen, Bau- und Installationsweg, Berechtigungstabelle und Grenzen; TESTPLAN.md mit Einleitung, Reihenfolge und Gesamtdurchlauf abgeschlossen |
