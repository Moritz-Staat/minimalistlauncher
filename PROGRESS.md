# PROGRESS

Status je Etappe: `offen` / `in Arbeit` / `fertig`.
Nach jedem Kompaktieren oder Neustart: diese Datei lesen, letzte unfertige Etappe finden,
dort weiterarbeiten.

| # | Etappe | Status | Zuletzt |
|---|--------|--------|---------|
| 0 | Repo, Doku, .gitignore | fertig | Repo geklont, CLAUDE.md/PROGRESS.md/DECISIONS.md/.gitignore angelegt |
| 1 | Projektgeruest (Gradle, Compose, Room/KSP, DataStore, Coil) | fertig | Version Catalog, App-Modul, leere MainActivity mit Compose; assembleDebug gruen |
| 2 | Launcher-Grundgeruest (Manifest, Theme, HOME-Rolle) | fertig | HOME-Intent-Filter, transparentes Wallpaper-Theme, edge-to-edge, BackHandler, onNewIntent schliesst Overlays, ROLE_HOME-Dialog |
| 3 | App-Index (LauncherApps, Collator-Sortierung, Room) | fertig | AppRepository ueber LauncherApps mit registerCallback, AppIndex mit Custom-Labels/Hidden, Collator de_DE, Room-Tabellen, IconCache/IconLoader; 13 Unit-Tests gruen |
| 4 | Homescreen (Uhr, Favoriten, App-Liste) | fertig | Uhr+Datum, Widget-Slot, Favoriten (max. 8), App-Liste als LazyColumn mit stabilen Keys, Wisch-Sheet mit Blur; TESTPLAN.md angelegt |
| 5 | Wave-Alphabet | fertig | Leiste rechts, Gauss-Skalierung im graphicsLayer, derivedStateOf fuer den aktiven Index, SEGMENT_TICK, Gesten-Ausschluss; 8 Unit-Tests gruen |
| 6 | Suche und App-Aktionen | fertig | Fuzzy-Matcher mit 6 Rangstufen, Suchfeld unten, Shortcuts/Kontakte/Websuche, Langdruck-Menue, Favoriten-Drag&Drop; 41 Unit-Tests gruen |
| 7 | Benachrichtigungen | offen | - |
| 8 | Media-Widget | offen | - |
| 9 | Widget-Host | offen | - |
| 10 | Pop-ups | offen | - |
| 11 | Icons (Icon-Packs, Dots, Monochrom) | offen | - |
| 12 | Theming | offen | - |
| 13 | Kalender und Wetter | offen | - |
| 14 | Gesten | offen | - |
| 15 | Usage Breaker | offen | - |
| 16 | Einstellungen, Backup, Onboarding | offen | - |
| 17 | Alltagstauglichkeit (Baseline Profile, R8) | offen | - |
| 18 | Abschluss (TESTPLAN.md, README.md) | offen | - |
