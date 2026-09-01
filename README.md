# Minimalist Launcher

Ein Android-Launcher als funktionale Kopie des Niagara Launchers. Gebaut für genau ein Gerät
— Nothing Phone (2), Nothing OS 4.x, Android 16 — und nicht für den Vertrieb.

Der Nothing-Launcher bleibt dabei installiert. Der Rückweg geht jederzeit über
*Einstellungen > Apps > Standard-Apps > Start-App*.

## Was er kann

**Homescreen.** Uhr auf etwa einem Viertel der Höhe, Datum darunter, danach Wetter, die
nächsten Termine, ein Widget-Slot und die Favoriten. Alles linksbündig, eine Spalte, darunter
absichtlich Leere — dort fährt die App-Liste hoch.

**App-Liste.** Nach oben wischen. Oben stehen die Apps, die in der letzten Woche am häufigsten
geöffnet wurden — der Homescreen bleibt trotzdem leer. Rechts liegt der Wellen-Alphabetbalken: der Finger fährt
daran entlang, die Buchstaben unter ihm werden größer, die Liste springt mit. Suchfeld unten,
wo der Daumen ist, mit Fuzzy-Treffern über Apps, Shortcuts und Kontakte und der Websuche als
letzter Zeile.

**Benachrichtigungen.** Vorschautext pro App direkt an der Zeile, Tippen öffnet sie, Wischen
verwirft sie, und pro App lässt sich der Inhalt auf "da ist etwas" reduzieren.

**Medien.** Ein eigenes Widget aus der laufenden Session, dessen Akzentfarbe aus dem Cover
kommt und auf Lesbarkeit geprüft wird. Beim Verbinden von Kopfhörern werden kurz die
gewählten Musik-Apps eingeblendet.

**Widgets.** Zwei Slots auf dem Homescreen (unter der Uhr, statt der Uhr) und Stapel, durch die
gewischt wird. Der eigene Wähler gruppiert nach App statt die flache Liste des Systems zu
zeigen, und nennt pro Widget Vorschaubild, Größe in Zellen und Beschreibung.

**Pop-ups.** Zeile nach rechts wischen: eine Karte auf Höhe der Zeile mit Shortcuts, aktueller
Benachrichtigung und Ordnerinhalten, der Rest unscharf dahinter.

**Icons.** ADW- und Nova-Icon-Packs, automatische Zuordnung über `appfilter.xml`, Icon pro App
von Hand setzbar, dazu ein Punkte-, ein Monochrom- und ein reiner Textmodus ohne Icons.

**Darstellung.** Fünf Uhr-Stile — von groß über die Wortuhr ("viertel nach drei") bis zum
gezeichneten Punktraster —, Material You oder
eigene Akzentfarbe oder extra dunkel fürs OLED, Hintergrund abdunkeln und weichzeichnen,
eigene Schriftdatei, vier Vorlagen, Theme als JSON exportierbar.

**Kalender und Wetter.** Die nächsten Termine aus den gewählten Kalendern, Wetter von
Open-Meteo ohne Konto und ohne Google Play Services.

**Gesten.** Doppeltippen, Wischen nach unten, links und rechts sowie langer Druck auf den
Hintergrund, jeweils frei belegbar — bis hin zu "Benachrichtigungen öffnen" und "Bildschirm
sperren" über einen optionalen Bedienungshilfen-Dienst.

**Nutzungsbremse.** Ab einer eingestellten Zahl von Öffnungen am Tag fragt der Launcher nach,
statt die App sofort zu starten. Gesperrt wird nie.

**Sicherung.** Einstellungen, Favoriten, Ordner, eigene Namen und Icons in einer lesbaren
JSON-Datei.

## Technik

Kotlin, Jetpack Compose, Material 3, ein einziges App-Modul, Single-Activity, MVVM mit
StateFlow. Room für Nutzerdaten, DataStore für Einstellungen. Die App-Liste kommt
ausschließlich aus `LauncherApps` — kein `QUERY_ALL_PACKAGES`, keine Play Services. Die einzige
Netzverbindung geht über `HttpURLConnection` zu Open-Meteo.

minSdk 31, targetSdk 36, compileSdk 36, JDK 21.

## Bauen

Vorausgesetzt werden JDK 21 und ein Android SDK; der Pfad zum SDK steht in `local.properties`
(nicht im Repo).

```
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"

./gradlew assembleDebug       # das APK, das auf dem Gerät landet
./gradlew testDebugUnitTest   # 192 Unit-Tests, framework-frei
./gradlew assembleRelease     # R8-Durchlauf, fängt fehlende Keep-Regeln
```

`JAVA_HOME` muss gesetzt sein — ohne sie bricht `gradlew` ab, **und zwar mit Exit-Code 0**.

## Installieren

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Danach in der Einrichtung "Als Standard-Launcher setzen". Der Benachrichtigungszugriff muss
nach **jeder** Neuinstallation erneut erteilt werden — Android nimmt ihn dabei zurück.

Fertige APKs liegen unter [Releases](../../releases) und lassen sich auch direkt auf dem Telefon
herunterladen und antippen. Es sind Debug-Builds: mit dem Debug-Schlüssel signiert und damit
ohne eigenen Keystore installierbar. Das Release-APK aus dem R8-Durchlauf ist unsigniert und
deshalb nicht dabei.

Was als Nächstes ansteht, steht im [Issue-Tracker](../../issues).

## Berechtigungen, und wofür

Alle optional; ohne jede einzelne läuft der Launcher.

| Berechtigung | Wofür | Ohne sie |
|---|---|---|
| Benachrichtigungszugriff | Vorschautexte, Media-Widget | Zeilen ohne Vorschau, kein Media-Widget |
| Kontakte | Kontakte in der Suche | Suche findet nur Apps und Shortcuts |
| Kalender | Termine unter der Uhr | Zeile bleibt leer |
| Grober Standort | Wetter | Keine Wetterzeile |
| Nutzungszugriff | Echte Öffnungszahlen | Gezählt wird nur, was über den Launcher läuft |
| Bedienungshilfen-Dienst | Benachrichtigungsleiste, Bildschirm sperren | Diese zwei Gesten melden sich als nicht verfügbar |

Bei einer per `adb` installierten App versteckt Android den Schalter für den
Bedienungshilfen-Dienst; er muss über App-Info → Menü → "Eingeschränkte Einstellung
zulassen" freigegeben werden.

## Dateien im Repo

- `PROGRESS.md` — Stand je Etappe. Beim Wiedereinstieg zuerst lesen.
- `DECISIONS.md` — jede selbst getroffene Entscheidung mit Begründung.
- `TESTPLAN.md` — alles, was nur am echten Gerät prüfbar ist, nach Etappen.
- `CLAUDE.md` — Projektregeln.

## Grenzen

Entwickelt wurde ohne Gerät und ohne Emulator: verifiziert war zunächst nur, was sich so
verifizieren lässt — grüner Build, grüne Unit-Tests, gegengelesener Code. Seit v0.1.1 läuft der
Launcher auf dem Zielgerät, und der erste Lauf hat prompt drei Fehler gezeigt, die kein Test
finden konnte: eine App-Liste, die durch Package-Visibility-Filterung nur System-Apps enthielt,
schwarze Schrift auf schwarzem Grund und eine erste Listenzeile hinter der Statusleiste.

Der `TESTPLAN.md` ist deshalb kein Formalismus, sondern die eigentliche Prüfung, und er ist
noch nicht abgearbeitet. Getestet ist ausschließlich das Nothing Phone (2) mit aktuellem
Nothing OS.

Nicht enthalten: eine Wiederherstellung platzierter Widgets (die IDs gehören dem
`AppWidgetHost` genau dieser Installation), ein Play-Store-Build und jede Form von Telemetrie.

Für eine Veröffentlichung über das eigene Gerät hinaus fehlen vor allem zwei Dinge: die Texte
liegen als deutsche Literale im Code statt in `strings.xml`, und Barrierefreiheit ist unbelegt.
Beides ist im Issue-Tracker als `rollout` erfasst.
