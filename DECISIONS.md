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

## 11 — Icons
- **`packageManager.getResourcesForApplication` statt des in der Vorgabe genannten
  `Resources.forPackage`.** Eine solche API gibt es im Android-SDK nicht; das ist der
  vorhandene Weg an fremde Ressourcen.
- **`<queries>` mit genau den zwei Icon-Pack-Actions.** Damit ist `queryIntentActivities`
  fuer die Pack-Erkennung erlaubt, ohne `QUERY_ALL_PACKAGES`. Die App-Liste kommt weiterhin
  ausschliesslich aus `LauncherApps`.
- **`IconPackFilterBuilder` ist die gemeinsame Regelbasis** fuer den kompilierten
  res/xml-Pfad (XmlPullParser) und den Assets-Pfad (SAX). Nur so ist das Parsen ohne Geraet
  testbar.
- **Auto-Replace nur bei sicherem Treffer (`MIN_SCORE`).** Ein falsches Icon ist schlimmer
  als das Original-Icon.
- **Manuell gesetzte Icons speichern das Pack mit.** Beim Pack-Wechsel wird das alte Pack bei
  Bedarf nachgeladen, statt die Auswahl stillschweigend zu verwerfen.
- **Punkte-Modus nutzt `getVibrantColor`, sonst die dominante Farbe.** Der Vibrant-Wert
  trifft die Markenfarbe besser; die dominante Farbe ist der Rueckfall, damit jede App
  abgedeckt ist.
- **Monochrom bevorzugt `AdaptiveIconDrawable.getMonochrome()` (API 33+)** und faellt sonst
  auf eine Saettigung von 0 zurueck — auf minSdk 31 gibt es die Ebene noch nicht.

## 12 — Theming
- **`ThemeConfig` ist ein einziges Datenobjekt in DataStore**, nicht zehn lose Schluessel im
  UI. Export, Import und die Vorlagen schreiben damit denselben Weg wie jede Einstellung.
- **Zahlen und Wahrheitswerte werden im Theme-JSON als Strings abgelegt**, passend zum
  bestehenden `JsonWriter`. Ein Zahlentyp im Parser waere mehr Code fuer keinen Gewinn.
- **Unbekannte Werte beim Import fallen auf den Standard zurueck**, nie auf eine Ausnahme.
  Eine kaputte Datei darf den Launcher nicht am Starten hindern.
- **Material You kommt aus `dynamicDarkColorScheme`/`dynamicLightColorScheme`.** Ab API 31
  liefert das System die Wallpaper-Palette; eine eigene waere schlechter und groesser.
- **Fuer die eigene Akzentfarbe rechnet `AccentPalette` die Stufen selbst** (HSL auf gepacktem
  ARGB). Material stellt ausserhalb der dynamischen Schemata keine Tonpalette bereit, und so
  ist die Regel ohne Geraet testbar.
- **Der Kontrast wird mit `AccentPicker.contrastRatio` geprueft** statt mit einer zweiten
  Implementierung derselben WCAG-Formel.
- **Extra dunkel ist der manuelle Modus mit schwarzen Flaechen**, keine dritte Farblogik.
- **Das Abdunkeln ist eine schwarze Ebene im Compose-Baum**, das Weichzeichnen dagegen
  `FLAG_BLUR_BEHIND` am Fenster: an das Hintergrundbild selbst kommt eine App ohne
  Speicherzugriff nicht heran. Ist der System-Blur aus, bleibt die Abdunklung.
- **Nur die Statusleiste laesst sich ausblenden.** Die Navigationsleiste mitzunehmen wuerde
  den Gestenbereich verschlucken.
- **Die vier Uhr-Stile leiten sich alle von `displayLarge` ab**, damit eine gewaehlte Schrift
  auch in der Uhr ankommt.
- **Die Wortuhr rundet auf fuenf Minuten und wechselt ab "fuenf vor halb" auf die kommende
  Stunde** — so wird die Zeit im Deutschen gesprochen.
- **Die Schriftdatei wird in den App-Speicher kopiert und traegt einen Zeitstempel im Namen.**
  Die SAF-Berechtigung ueberlebt den Neustart nicht, und Compose merkt sich geladene Schriften
  pro Datei — gleicher Name waere gleich die alte Schrift.
- **Vorlagen und Import lassen den Schriftpfad in Ruhe.** Ein Pfad aus einer fremden
  Installation zeigt hier auf nichts.
- **`SelectableRow` liegt jetzt in `ui/common`**, weil die Icon- und die Theme-Einstellungen
  dieselbe Zeile brauchen.

## 13 — Kalender und Wetter
- **Der Kalender wird ueber `CalendarContract.Instances` gelesen, nicht ueber `Events`.** Ein
  woechentlicher Termin ist ein Event, aber der Homescreen braucht die naechste Wiederholung.
- **Ganztaegige Termine tragen Mitternacht UTC**, nicht lokale Mitternacht. Ihr Datum wird
  deshalb in UTC ausgewertet, sonst wird aus "heute" westlich von Greenwich "gestern".
- **Abgelehnte Einladungen werden weggefiltert** (`SELF_ATTENDEE_STATUS`); sie gehoeren nicht
  auf den Homescreen.
- **Die Terminliste wird zusaetzlich im Composable pro Minutentakt gefiltert.** Der
  ContentObserver merkt nichts davon, dass ein Termin gerade zu Ende gegangen ist.
- **Leere Kalenderauswahl heisst "alle Kalender".** Beim ersten Abwaehlen wird deshalb von der
  vollstaendigen Liste ausgegangen, sonst bliebe genau der abgewaehlte Kalender uebrig.
- **Wetter kommt von Open-Meteo ueber `HttpURLConnection`**, ohne Konto, ohne Schluessel, ohne
  Play Services — so steht es in der Vorgabe. Eine HTTP-Bibliothek waere fuer eine URL zu viel.
- **Koordinaten werden mit `Locale.US` formatiert.** Mit deutscher Locale entstuende
  "latitude=52,5200" und die Anfrage kaeme als Fehler zurueck.
- **Der Standort kommt aus dem `LocationManager`, zuerst als letzte bekannte Position.** Fuer
  eine Temperatur reicht grobe Genauigkeit; das GPS dafuer zu wecken waere Verschwendung.
- **Die letzte Messung liegt als JSON in DataStore**, nicht in Room. Es ist genau ein Wert,
  und der Homescreen soll ihn sofort zeigen statt auf das Netz zu warten.
- **Fehlgeschlagene Abrufe aendern nichts.** Ein alter Wert ist besser als eine leere Zeile,
  und der Worker meldet `success`, statt in einen Retry-Sturm ohne Netz zu laufen.
- **Der stuendliche WorkManager-Job existiert nur, solange das Wetter eingeschaltet ist.**
- **Ein Tippen auf die Wetterzeile aktualisiert sie.** Welche Wetter-App gemeint waere, weiss
  der Launcher nicht; falsch zu raten ist schlechter als das Naheliegende zu tun.
- **Bei einem Wechsel der Einheit wird neu abgerufen statt umgerechnet.** Open-Meteo liefert
  die Einheit mit; selbst umzurechnen waere geraten.

## 14 — Gesten
- **Fuenf Gesten sind frei belegbar, Wischen nach oben nicht.** Es oeffnet die App-Liste und
  ist die eine Geste, die der Launcher nicht verschenken kann.
- **Jede Geste liegt als kurzer String in DataStore** (`none`, `app_list`, `app:<AppKey>` ...).
  Eine neue Aktion braucht so keine Migration, und Unbekanntes wird zu `None` statt zu einem
  Absturz.
- **Benachrichtigungen oeffnen und Bildschirm sperren laufen ueber einen optionalen
  Bedienungshilfen-Dienst** (`GLOBAL_ACTION_NOTIFICATIONS`, `GLOBAL_ACTION_LOCK_SCREEN`). Die
  Leiste laesst sich anders gar nicht oeffnen, und fuers Sperren waere die Alternative ein
  Geraeteadmin — deutlich uebergriffiger. Der Dienst abonniert keine Ereignisse und darf
  keine Fensterinhalte lesen.
- **Fehlt der Dienst, sagt ein Toast das.** Eine Geste, die stillschweigend nichts tut, ist
  der schlechtere Fehler.
- **Vertikale und horizontale Gesten haengen an zwei getrennten `pointerInput`-Modifiern.**
  Wer den Touch-Slop zuerst ueberschreitet, bekommt die Geste — genau das gewuenschte
  Verhalten, und deutlich weniger Code als ein eigener Gestendetektor.
- **Nach unten wischen zaehlt nur, wenn die App-Liste geschlossen ist.** Sonst waere es der
  Zug, mit dem die Liste zugemacht wird.
- **Tippgesten liegen am Wurzel-Modifier des Homescreens.** Uhr, Favoriten und Widgets
  verbrauchen ihre Tipps selbst, also bleibt genau der leere Hintergrund uebrig.
- **"Suche oeffnen" oeffnet die Liste und setzt den Fokus ins Suchfeld**, statt ein zweites
  Such-Overlay zu bauen; das Feld gibt es schon.

## 15 — Nutzungsbremse
- **Die Bremse blockiert nie, sie fragt.** Sie zeigt die Zahl des Tages, wartet ein paar
  Sekunden und laesst dann oeffnen. Eine Sperre wuerde nur umgangen.
- **Gezaehlt wird pro Paket, nicht pro AppKey.** Zwei Profile derselben App sind dieselbe
  Gewohnheit.
- **Alle Starts laufen ueber `HomeViewModel.launch`.** Das ist der einzige Punkt, an dem die
  Bremse haengen muss — Homescreen, Liste, Suche und Pop-ups gehen alle dort durch.
- **Die Pruefung ist ein Lookup im Speicher** (Konfiguration und Tageszaehler als StateFlow).
  Zwischen Tippen und App darf keine Datenbankabfrage liegen.
- **Mit Nutzungszugriff zaehlt das System, ohne ihn zaehlt der Launcher selbst.** Ohne den
  Zugriff bleiben Starts aus Benachrichtigungen oder aus den letzten Apps unsichtbar; das ist
  ein schlechterer, aber ehrlicher Wert.
- **Ob der Nutzungszugriff erteilt ist, wird an der Abfrage selbst erkannt.** Saemtliche
  `AppOpsManager`-Pruefungen sind auf dem aktuellen SDK deprecated; ohne Zugriff kommt die
  Ereignisliste leer zurueck, mit Zugriff enthaelt sie mindestens den eigenen Start.
- **Aufeinanderfolgende `ACTIVITY_RESUMED`-Ereignisse desselben Pakets zaehlen als eines.**
  Hin- und Herwechseln innerhalb einer App ist ein Oeffnen, nicht fuenf.
- **Die Zaehler stehen in Room mit dem lokalen Tag als Schluessel** (DB v5) und werden nach
  sieben Tagen geloescht. Der Tageswechsel passiert beim naechsten Blick auf den Homescreen,
  nicht ueber einen Timer um Mitternacht.
- **Schwelle und Wartezeit werden beim Lesen und Schreiben geklemmt.** Ein kaputter Wert darf
  nicht dazu fuehren, dass eine App gar nicht mehr aufgeht.

## 16 — Einstellungen, Sicherung, Einrichtung
- **Die Einstellungen bleiben ein Bildschirm mit aufklappbaren Gruppen**, kein Navigationsbaum.
  Ein zweiter Back-Stack wuerde mit der Regel kollidieren, dass Zurueck nie aus dem Launcher
  fuehrt.
- **Die Sicherung ist eine JSON-Datei ueber SAF**, mit demselben Leser und Schreiber wie das
  Theme. Kein Auto-Backup von Android: `allowBackup` ist aus, und eine Datei kann der Nutzer
  selbst ablegen, weitergeben und lesen.
- **Einspielen ersetzt, es fuehrt nicht zusammen.** Zwei zusammengemischte Sicherungen ergaeben
  einen Zustand, den es auf keinem Geraet je gab.
- **Nicht gesichert werden platzierte Widgets, die Schriftdatei und die Zaehler der
  Nutzungsbremse.** Widget-IDs gehoeren dem `AppWidgetHost` genau dieser Installation, der
  Schriftpfad zeigt in deren Dateien, und die Zaehler beschreiben heute, nicht die Einrichtung.
- **Ordner werden beim Einspielen neu angelegt statt unter alten IDs wiederhergestellt.** Die
  IDs vergibt die Datenbank.
- **Die gesicherten Einstellungsschluessel stehen als Namensliste im Code**, nach Typ getrennt.
  So kann eine fremde Datei nichts in den Store schreiben, was der Launcher nicht kennt.
- **Die Einrichtung laesst sich in jedem Schritt ueberspringen.** Der Launcher muss ohne jede
  Berechtigung laufen, und ein Setup, das auf Zustimmung besteht, erzieht zum Wegtippen.
- **Die Einrichtung liegt ueber allem anderen und wird ueber ein Flag in DataStore gesteuert**,
  das sich in den Einstellungen zuruecksetzen laesst.

## 17 — Alltagstauglichkeit
- **R8 laeuft nur im Release-Build**, der Debug-Build bleibt unverkleinert. Verteilt wird zwar
  das Debug-APK, aber ein regelmaessig gebauter Release-Build faengt Regeln ab, die spaeter
  fehlen wuerden: das APK schrumpft von 35 MB auf 3,2 MB.
- **Es gibt genau zwei Keep-Regeln.** Enum-Konstanten des eigenen Pakets, weil Einstellungen
  ihren `name` speichern und ein umbenannter Wert die Einstellung stillschweigend
  zuruecksetzen wuerde, und die WorkManager-Worker, die ueber ihren Klassennamen aus der
  Datenbank instanziiert werden. Alles Uebrige bringen die Bibliotheken selbst mit.
- **Das Baseline-Profil ist von Hand geschrieben, nicht gemessen.** Ein Macrobenchmark braucht
  ein Geraet, das es hier nicht gibt. Aufgenommen ist deshalb der ehrliche Superset: alles aus
  `de.moritzstaat.launcher`. Fuer einen Launcher ist das vertretbar — er wird per Home-Taste
  gestartet, muss sofort da sein, und sein eigener Code ist neben Compose klein.
- **`androidx.profileinstaller` kommt dazu** (neue Bibliothek, deshalb dieser Eintrag). Ohne
  sie landet das Profil bei einer per `adb` installierten App nicht in ART.
- **Toter Code raus:** `HomeScaffold` aus Etappe 2 und `HomeViewModel.setFavorite` wurden von
  nichts mehr aufgerufen; R8s `usage.txt` hat beide gefunden.
- **Die Medien-Einstellungen waren nie erreichbar.** `setMediaApps` wurde nirgends aufgerufen,
  die Kopfhoerer-Einblendung aus Etappe 8 war damit tot. Jetzt gibt es die Gruppe "Medien".

## Nach v0.1.0 — Funde vom Geraet
- **`<queries>` bekommt MAIN + LAUNCHER.** In v0.1.0 zeigte die Liste nur sieben Apps: alle
  vorinstallierten plus die zwei Icon-Packs. Der Grund war nicht die Abfrage — die ueber
  `LauncherApps` war richtig — sondern die Package-Visibility-Filterung ab Android 11. Ohne
  passenden `<intent>`-Eintrag sieht eine App nur System-Apps und was sie ausdruecklich
  deklariert hat. `QUERY_ALL_PACKAGES` bleibt trotzdem draussen: der deklarierte Launcher-Intent
  liefert genau die startbaren Apps, und die Liste selbst kommt weiter aus `LauncherApps`.
- **`LauncherTheme` setzt jetzt `LocalContentColor`.** `MaterialTheme` tut das nicht, nur
  `Surface` — und der Launcher zeichnet aufs Hintergrundbild statt auf eine Surface. Jeder
  `Text` ohne eigene Farbe lief damit auf den Standardwert des Composition Locals, und der ist
  deckendes Schwarz. In der Einrichtung war die Ueberschrift schwarz auf schwarz.
- **`topInset` der App-Liste wird abgeleitet statt auf 0 zu stehen.** Der Parameter existierte,
  wurde aber von keinem Aufrufer gesetzt, also lag die erste Zeile unter der Statusleiste. Der
  Standardwert liest die Statusleisten-Inset jetzt selbst; ein vergessener Aufrufer kann den
  Fehler nicht mehr ausloesen.
- **Die Suchergebnisliste bekommt dasselbe Inset plus Platz unten.** Sie ersetzt die App-Liste
  im selben Sheet, hatte aber weder das eine noch das andere; die letzte Zeile lag hinter dem
  Suchfeld.
