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

## 5 — Wave-Alphabet
- **Skalierung ueber `graphicsLayer`-Lambda, aktiver Index ueber `derivedStateOf`.** Das
  Ziehen loest nur Neuzeichnen aus; rekomponiert wird genau einmal pro Buchstabenwechsel,
  und zwar nur die Leiste.
- **Gleich hohe Slots ueber `weight(1f)` statt gemessener Positionen.** Damit ist die
  Slot-Hoehe im Layer-Lambda direkt `size.height` und die Wave braucht keine Messwerte.
- **`indexAt` klemmt statt zu verwerfen.** Wer ueber das obere oder untere Ende hinausfaehrt,
  bleibt auf dem ersten bzw. letzten Buchstaben haengen, statt die Leiste zu verlieren.
- **`SEGMENT_TICK` erst ab API 34, darunter `CLOCK_TICK`.** minSdk ist 31; das Zielgeraet
  bekommt den richtigen Tick.
- **Ausschluss-Rechteck nur bei Aenderung setzen.** `onGloballyPositioned` feuert oft; ein
  Vergleich spart einen Systemaufruf pro Layout-Durchlauf.
- **`FallbackEdgeInset` = 8 dp existiert als Konstante, ist aber noch nicht verdrahtet.**
  Das System begrenzt Ausschluss-Rechtecke auf 200 dp Hoehe pro Kante, die Leiste ist
  hoeher. Ob das in der Praxis stoert, entscheidet der Geraetetest; die Umschaltung kommt
  als Einstellung in Etappe 12.

## 6 — Suche und App-Aktionen
- **Eigener `TextNormalizer` mit Index-Rueckabbildung.** Fuer die Hervorhebung muss jeder
  gefaltete Buchstabe auf seine Stelle im Originallabel zurueckzeigen; "ss" aus "ß" zeigt
  auf dasselbe Original-Zeichen.
- **Sechs Rangstufen statt eines Punktesystems.** Exakt > Praefix > Wortanfang > Initialen >
  Teilfolge ab Wortanfang > Teilfolge. Das ist die Reihenfolge, die Nutzer erwarten, und
  laesst sich testen.
- **Suche laeuft ueber die sichtbaren Apps, nicht ueber alle.** Ausgeblendete Apps sollen
  ausgeblendet bleiben; ein Schalter dafuer kommt mit den Einstellungen in Etappe 16.
- **Kein Material3-`SearchBar`.** Die Komponente bringt eigene Vollbild-Expansion und
  eigenes Zurueck-Verhalten mit, das mit "Zurueck verlaesst den Launcher nie" kollidiert.
- **Web-Suche mit DuckDuckGo-URL als Rueckfall**, falls keine App `ACTION_WEB_SEARCH`
  beantwortet. Die letzte Zeile darf nie ins Leere fuehren.
- **Langdruck auf einen Favoriten startet das Ziehen, nicht das Menue.** Wer ohne Bewegung
  loslaesst, bekommt trotzdem das Menue — `AppRow.onLongClick` ist dafuer nullbar geworden.
- **`applyDrag` als reine Funktion.** Das Umsortieren ist damit ohne Geraet testbar; die
  Composable haelt nur noch den Zustand.
- **Deinstallieren geht ueber `ACTION_DELETE` an das System.** Der Launcher entfernt selbst
  nichts, die Bestaetigung bleibt beim Nutzer.

## 7 — Benachrichtigungen
- **`NotificationTextExtractor` arbeitet auf einem eigenen `NotificationContent`,
  nicht auf `Bundle`.** Das Auspacken der Plattform-Extras steckt in einer duennen Adapter-
  Funktion, die Regeln dahinter sind ohne Geraet testbar.
- **Tippen auf den Vorschautext oeffnet die Benachrichtigung, Tippen auf den App-Namen die
  App.** Ein einziger Klickbereich haette entweder die App oder die Nachricht unerreichbar
  gemacht.
- **`FLAG_GROUP_SUMMARY` wird zusaetzlich zu `FLAG_ONGOING_EVENT` verworfen.** Sonst steht
  bei Messengern die Sammelmeldung statt der eigentlichen Nachricht in der Zeile.
- **Pro-App-Schalter in Room, nicht in DataStore.** Es ist eine Angabe pro App, also
  Nutzerdaten; DataStore bleibt fuer globale Einstellungen. Dafuer Schema-Version 2 mit
  echter Migration.
- **`SwipeableRow` als eigene Komponente.** Wischen links verwirft die Benachrichtigung,
  wischen rechts ist schon fuer das Pop-up in Etappe 10 verdrahtet. Nur Richtungen mit
  Handler lassen sich ziehen.
- **Der Listener-Dienst haelt eine statische Referenz auf sich selbst.** `cancelNotification`
  gibt es nur auf der gebundenen Instanz; alles andere laeuft ueber das Repository.

## 8 — Media-Widget
- **`AccentPicker` prueft den WCAG-Kontrast und hellt notfalls auf.** Palette liefert bei
  dunklen Covern regelmaessig Farben, die auf schwarzem Grund unsichtbar sind; das reine
  Rechnen auf ARGB-Ints ist ohne Bitmap testbar.
- **Die 30-Sekunden-Regel steckt im ViewModel, nicht im Repository.** Sie ist eine
  Darstellungsentscheidung; das Repository meldet nur, was das System sagt.
- **`AudioOutputRepository` registriert zur Laufzeit.** `ACTION_HEADSET_PLUG` und die
  A2DP-Zustandsaenderung werden nicht an im Manifest deklarierte Empfaenger zugestellt.
- **Musik-Apps fuer die Einblendung liegen in DataStore.** Das ist eine globale Einstellung
  des Launchers, keine Angabe pro App.
- **Transport-Knoepfe als Unicode-Zeichen statt Material-Icons.** `material-icons-extended`
  waere eine weitere Bibliothek und mehrere MB fuer drei Symbole.
- **`LauncherSettings` schon jetzt angelegt**, obwohl DataStore erst ab Etappe 12 richtig
  gebraucht wird — die Musik-App-Liste ist die erste globale Einstellung.

## 9 — Widget-Host
- **Feste Host-ID `0x4C41`.** Sie identifiziert den Launcher gegenueber dem System ueber
  Neustarts hinweg; eine geaenderte ID wuerde alle gebundenen Widgets verwaisen lassen.
- **Eigener Widget-Waehler ueber `getInstalledProviders()` statt `ACTION_APPWIDGET_PICK`.**
  Der Systempicker bindet selbst und umgeht damit `bindAppWidgetIdIfAllowed`.
- **`deleteAppWidgetId` immer zusammen mit der Datenbankzeile.** `WidgetViewModel.remove`
  macht beides, jeder abgebrochene Pfad im Waehler ruft `discard`. Zusaetzlich raeumt
  `pruneOrphans` beim Laden auf, falls doch einmal eine ID uebrig bleibt.
- **Konfigurations-Activity zuerst per explizitem `ACTION_APPWIDGET_CONFIGURE`.** Nur wenn
  die Activity nicht aufloesbar ist, uebernimmt
  `AppWidgetHost.startAppWidgetConfigureActivityForResult`. Dessen Ergebnis landet in der
  Activity, nicht im Compose-Launcher — dieser Pfad behaelt das Widget optimistisch.
- **`updateAppWidgetSize` mit `SizeF`-Liste** (API 31), nicht die veraltete Min/Max-Variante.
- **Die Uhr wird ersetzt, nicht ueberlagert**, wenn ein Widget im Slot "statt der Uhr" liegt.
- **`WidgetSlot` als Composable in `ui/home` geloescht.** Namenskollision mit dem gleichnamigen
  Enum; der Wrapper war ohnehin nur ein `Box`.

## 10 — Pop-ups
- **Eine Liste aus `AppListItem`, nicht zwei getrennte Listen.** Ordner stehen alphabetisch
  zwischen den Apps, also muessen sie durch dieselbe Sortierung laufen.
- **Eine App liegt in hoechstens einem Ordner** und verschwindet dann aus der obersten Ebene.
  Sonst steht dieselbe App zweimal in der Liste.
- **Leere Ordner werden automatisch geloescht.** Ein Ordner ohne Apps ist nur eine Zeile, die
  nichts tut.
- **Der Blur liegt auf einem Container um den ganzen Launcher**, gesteuert von einem
  `animateFloatAsState`, das nur im `graphicsLayer`-Lambda gelesen wird. So kostet das
  Oeffnen eines Pop-ups keine Rekomposition der Liste.
- **Die Karte wird an der Zeilenposition verankert und auf 55 % Bildschirmhoehe begrenzt**,
  damit sie bei einer Zeile ganz unten nicht aus dem Bild laeuft.
- **Das Widget-Pop-up ist kein eigener Fall**, sondern der Slot `WidgetSlot.Popup` mit dem
  AppKey als `ownerKey` innerhalb des App-Pop-ups.
