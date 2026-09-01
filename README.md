# Minimalist Launcher

Ein Android-Launcher als funktionale Kopie des Niagara Launchers. Gebaut fuer genau ein Geraet
— Nothing Phone (2), Nothing OS 4.x, Android 16 — und nicht fuer den Vertrieb.

Der Nothing-Launcher bleibt dabei installiert. Der Rueckweg geht jederzeit ueber
*Einstellungen > Apps > Standard-Apps > Start-App*.

## Was er kann

**Homescreen.** Uhr auf etwa einem Viertel der Hoehe, Datum darunter, danach Wetter, die
naechsten Termine, ein Widget-Slot und die Favoriten. Alles linksbuendig, eine Spalte, darunter
absichtlich Leere — dort faehrt die App-Liste hoch.

**App-Liste.** Nach oben wischen. Rechts liegt der Wellen-Alphabetbalken: der Finger fahrt
daran entlang, die Buchstaben unter ihm werden groesser, die Liste springt mit. Suchfeld unten,
wo der Daumen ist, mit Fuzzy-Treffern ueber Apps, Shortcuts und Kontakte und der Websuche als
letzter Zeile.

**Benachrichtigungen.** Vorschautext pro App direkt an der Zeile, Tippen oeffnet sie, Wischen
verwirft sie, und pro App laesst sich der Inhalt auf "da ist etwas" reduzieren.

**Medien.** Ein eigenes Widget aus der laufenden Session, dessen Akzentfarbe aus dem Cover
kommt und auf Lesbarkeit geprueft wird. Beim Verbinden von Kopfhoerern werden kurz die
gewaehlten Musik-Apps eingeblendet.

**Widgets.** Eigener Waehler, zwei Slots auf dem Homescreen (unter der Uhr, statt der Uhr) und
Stapel, durch die gewischt wird.

**Pop-ups.** Zeile nach rechts wischen: eine Karte auf Hoehe der Zeile mit Shortcuts, aktueller
Benachrichtigung und Ordnerinhalten, der Rest unscharf dahinter.

**Icons.** ADW- und Nova-Icon-Packs, automatische Zuordnung ueber `appfilter.xml`, Icon pro App
von Hand setzbar, dazu ein Punkte- und ein Monochrom-Modus.

**Darstellung.** Vier Uhr-Stile bis hin zur Wortuhr ("viertel nach drei"), Material You oder
eigene Akzentfarbe oder extra dunkel fuers OLED, Hintergrund abdunkeln und weichzeichnen,
eigene Schriftdatei, vier Vorlagen, Theme als JSON exportierbar.

**Kalender und Wetter.** Die naechsten Termine aus den gewaehlten Kalendern, Wetter von
Open-Meteo ohne Konto und ohne Google Play Services.

**Gesten.** Doppeltippen, Wischen nach unten, links und rechts sowie langer Druck auf den
Hintergrund, jeweils frei belegbar — bis hin zu "Benachrichtigungen oeffnen" und "Bildschirm
sperren" ueber einen optionalen Bedienungshilfen-Dienst.

**Nutzungsbremse.** Ab einer eingestellten Zahl von Oeffnungen am Tag fragt der Launcher nach,
statt die App sofort zu starten. Gesperrt wird nie.

**Sicherung.** Einstellungen, Favoriten, Ordner, eigene Namen und Icons in einer lesbaren
JSON-Datei.

## Technik

Kotlin, Jetpack Compose, Material 3, ein einziges App-Modul, Single-Activity, MVVM mit
StateFlow. Room fuer Nutzerdaten, DataStore fuer Einstellungen. Die App-Liste kommt
ausschliesslich aus `LauncherApps` — kein `QUERY_ALL_PACKAGES`, keine Play Services. Die einzige
Netzverbindung geht ueber `HttpURLConnection` zu Open-Meteo.

minSdk 31, targetSdk 36, compileSdk 36, JDK 21.

## Bauen

Vorausgesetzt werden JDK 21 und ein Android SDK; der Pfad zum SDK steht in `local.properties`
(nicht im Repo).

```
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"

./gradlew assembleDebug       # das APK, das auf dem Geraet landet
./gradlew testDebugUnitTest   # 144 Unit-Tests, framework-frei
./gradlew assembleRelease     # R8-Durchlauf, faengt fehlende Keep-Regeln
```

`JAVA_HOME` muss gesetzt sein — ohne sie bricht `gradlew` ab, **und zwar mit Exit-Code 0**.

## Installieren

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Danach in der Einrichtung "Als Standard-Launcher setzen". Der Benachrichtigungszugriff muss
nach **jeder** Neuinstallation erneut erteilt werden — Android nimmt ihn dabei zurueck.

## Berechtigungen, und wofuer

Alle optional; ohne jede einzelne laeuft der Launcher.

| Berechtigung | Wofuer | Ohne sie |
|---|---|---|
| Benachrichtigungszugriff | Vorschautexte, Media-Widget | Zeilen ohne Vorschau, kein Media-Widget |
| Kontakte | Kontakte in der Suche | Suche findet nur Apps und Shortcuts |
| Kalender | Termine unter der Uhr | Zeile bleibt leer |
| Grober Standort | Wetter | Keine Wetterzeile |
| Nutzungszugriff | Echte Oeffnungszahlen | Gezaehlt wird nur, was ueber den Launcher laeuft |
| Bedienungshilfen-Dienst | Benachrichtigungsleiste, Bildschirm sperren | Diese zwei Gesten melden sich als nicht verfuegbar |

Bei einer per `adb` installierten App versteckt Android den Schalter fuer den
Bedienungshilfen-Dienst; er muss ueber App-Info → Menue → "Eingeschraenkte Einstellung
zulassen" freigegeben werden.

## Dateien im Repo

- `PROGRESS.md` — Stand je Etappe. Beim Wiedereinstieg zuerst lesen.
- `DECISIONS.md` — jede selbst getroffene Entscheidung mit Begruendung.
- `TESTPLAN.md` — alles, was nur am echten Geraet pruefbar ist, nach Etappen.
- `CLAUDE.md` — Projektregeln.

## Grenzen

Verifiziert ist, was ohne Geraet verifizierbar ist: gruener Build, gruene Unit-Tests,
gegengelesener Code. Es gab waehrend der Entwicklung weder Geraet noch Emulator — alles
Uebrige steht als konkreter Pruefschritt im `TESTPLAN.md` und ist dort noch abzuarbeiten.

Nicht enthalten: eine Wiederherstellung platzierter Widgets (die IDs gehoeren dem
`AppWidgetHost` genau dieser Installation), ein Play-Store-Build und jede Form von Telemetrie.
