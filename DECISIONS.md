# DECISIONS

Jede selbst getroffene Entscheidung mit einer Zeile Begründung.

## 0 — Aufsetzen
- **Repo liegt in `C:\Users\morit\minimalistlauncher`, nicht direkt im Arbeitsverzeichnis.**
  `git clone ... .` hätte das komplette Home-Verzeichnis zum Repo gemacht.
- **Android SDK selbst installiert nach `C:\Users\morit\Android\sdk`** (cmdline-tools,
  platform-tools, platforms;android-36, build-tools;36.0.0) — auf dem Rechner war keines
  vorhanden, ohne SDK kein grüner Build.

## 1 — Projektgerüst
- **AGP 8.13.2 + Gradle 8.14.3 statt AGP 9.x.** AGP 9 verlangt compileSdk 37; compileSdk 36
  ist vorgegeben und nicht verhandelbar.
- **Compose BOM 2026.06.01 (Compose 1.11.4) statt 2026.08.00.** Compose 1.12 fordert
  compileSdk 37 und AGP 9.1+.
- **Kotlin 2.2.21 + KSP 2.2.21-2.0.5.** Kotlin-gekoppelte KSP-Version, damit Compiler und
  Prozessor garantiert zueinander passen.
- **`org.gradle.configuration-cache=true`.** Kaltere Builds sind hier egal, aber der
  Konfigurations-Cache fängt Fehler in Build-Skripten früh ab.
- **Room-Schema-Export nach `app/schemas`.** Migrationen sind sonst nicht nachvollziehbar.
- **Eigenes Launcher-Icon als Adaptive Icon mit Monochrome-Layer**, damit die App im
  Nothing-Theme nicht aus dem Rahmen fällt.

## 2 — Launcher-Grundgerüst
- **Kein `CATEGORY_LAUNCHER` im Intent-Filter**, nur `HOME` + `DEFAULT` + `LAUNCHER_APP`.
  Sonst taucht der Launcher in seiner eigenen App-Liste auf.
- **`BackHandler(enabled = true)` global.** Die Zurück-Geste wird immer verschluckt; offene
  Overlays schließen, sonst passiert nichts. Ein Home-Screen hat kein Zurück.
- **`windowDisablePreview` + `windowAnimationStyle=@null`.** Ein Starting-Window über dem
  Wallpaper blitzt beim Home-Druck sichtbar auf.
- **`HomeRole.createSettingsIntent()` als Fallback** auf `ACTION_HOME_SETTINGS`, falls ein
  OEM-Build den Rollendialog verweigert. Erzwungen wird nichts.
- **`tools/build.sh`** pinnt JDK 21, weil `java` nicht in der PATH-Umgebung liegt.

## 3 — App-Index
- **`Collator.SECONDARY` statt `TERTIARY`.** Bei TERTIARY sortiert die deutsche Collation
  Kleinbuchstaben vor Großbuchstaben; "gmail" landete dann vor "Gmail". Groß/Klein
  entscheidet jetzt nicht, der Tiebreak ist ein einfacher String-Vergleich.
- **Eigene Konstanten für die Trim-Level (40/80).** `TRIM_MEMORY_BACKGROUND` und
  `TRIM_MEMORY_COMPLETE` sind auf API 36 deprecated, der Callback liefert die Werte aber
  weiterhin. `TRIM_MEMORY_UI_HIDDEN` wird bewusst ignoriert — das kommt bei jedem App-Start
  und würde den Icon-Cache ständig leeren.
- **`AppKey` wird als flacher String `paket/klasse#serial` persistiert.** Eine Spalte statt
  drei in jeder Tabelle und im JSON-Backup; die Trennzeichen kommen in Paket- oder
  Klassennamen nicht vor.
- **`AppIndex` als eigene Schicht über `AppRepository`.** Das Repository liefert nur, was das
  System kennt; Custom-Labels und versteckte Apps kommen erst darüber dazu.
- **Manuelle DI über `ServiceLocator`.** Hilt wäre eine weitere Bibliothek und Startzeit
  für eine Handvoll Singletons.
- **Icon-Cache in Bytes begrenzt (1/8 Heap, hart bei 96 MB).** Wenige riesige Icons dürfen
  den Cache nicht leerdrücken; Budget aus Etappe 17.

## 4 — Homescreen
- **Uhr bei 22 % der Bildschirmhöhe statt exakt 25 %.** Mit Statusleisten-Inset landet der
  optische Schwerpunkt sonst zu tief.
- **App-Liste als eigene Ebene über dem Homescreen, nicht als BottomSheet-Komponente.**
  Material3s ModalBottomSheet bringt eigenes Scrim- und Back-Verhalten mit, das mit der
  Regel "Zurück verlässt den Launcher nie" kollidiert.
- **Blur und Verschiebung über `graphicsLayer`-Lambdas.** Der Fortschritt wird erst in der
  Draw-Phase gelesen; Ziehen löst damit keine Rekomposition der Liste aus.
- **Sheet-Fortschritt in einem `Animatable`, Bedienung über NestedScroll.** Eine
  durchgehende Geste kann so scrollen und danach die Liste wieder wegschieben.
- **Langdruck auf die freie Fläche öffnet die Einstellungen** (Etappe 14 zieht das
  ohnehin so vor) — sonst gibt es bei leerer Favoritenliste keinen Weg in die Einrichtung.
- **Icons über einen eigenen `IconLoader`, nicht über Coil.** Coil bringt für
  `LauncherActivityInfo`-Drawables nichts; es bleibt für Cover-Bilder in Etappe 8.

## 5 — Wave-Alphabet
- **Skalierung über `graphicsLayer`-Lambda, aktiver Index über `derivedStateOf`.** Das
  Ziehen löst nur Neuzeichnen aus; rekomponiert wird genau einmal pro Buchstabenwechsel,
  und zwar nur die Leiste.
- **Gleich hohe Slots über `weight(1f)` statt gemessener Positionen.** Damit ist die
  Slot-Höhe im Layer-Lambda direkt `size.height` und die Wave braucht keine Messwerte.
- **`indexAt` klemmt statt zu verwerfen.** Wer über das obere oder untere Ende hinausfährt,
  bleibt auf dem ersten bzw. letzten Buchstaben hängen, statt die Leiste zu verlieren.
- **`SEGMENT_TICK` erst ab API 34, darunter `CLOCK_TICK`.** minSdk ist 31; das Zielgerät
  bekommt den richtigen Tick.
- **Ausschluss-Rechteck nur bei Änderung setzen.** `onGloballyPositioned` feuert oft; ein
  Vergleich spart einen Systemaufruf pro Layout-Durchlauf.
- **`FallbackEdgeInset` = 8 dp existiert als Konstante, ist aber noch nicht verdrahtet.**
  Das System begrenzt Ausschluss-Rechtecke auf 200 dp Höhe pro Kante, die Leiste ist
  höher. Ob das in der Praxis stört, entscheidet der Gerätetest; die Umschaltung kommt
  als Einstellung in Etappe 12.

## 6 — Suche und App-Aktionen
- **Eigener `TextNormalizer` mit Index-Rückabbildung.** Für die Hervorhebung muss jeder
  gefaltete Buchstabe auf seine Stelle im Originallabel zurückzeigen; "ss" aus "ß" zeigt
  auf dasselbe Original-Zeichen.
- **Sechs Rangstufen statt eines Punktesystems.** Exakt > Präfix > Wortanfang > Initialen >
  Teilfolge ab Wortanfang > Teilfolge. Das ist die Reihenfolge, die Nutzer erwarten, und
  lässt sich testen.
- **Suche läuft über die sichtbaren Apps, nicht über alle.** Ausgeblendete Apps sollen
  ausgeblendet bleiben; ein Schalter dafür kommt mit den Einstellungen in Etappe 16.
- **Kein Material3-`SearchBar`.** Die Komponente bringt eigene Vollbild-Expansion und
  eigenes Zurück-Verhalten mit, das mit "Zurück verlässt den Launcher nie" kollidiert.
- **Web-Suche mit DuckDuckGo-URL als Rückfall**, falls keine App `ACTION_WEB_SEARCH`
  beantwortet. Die letzte Zeile darf nie ins Leere führen.
- **Langdruck auf einen Favoriten startet das Ziehen, nicht das Menü.** Wer ohne Bewegung
  loslässt, bekommt trotzdem das Menü — `AppRow.onLongClick` ist dafür nullbar geworden.
- **`applyDrag` als reine Funktion.** Das Umsortieren ist damit ohne Gerät testbar; die
  Composable hält nur noch den Zustand.
- **Deinstallieren geht über `ACTION_DELETE` an das System.** Der Launcher entfernt selbst
  nichts, die Bestätigung bleibt beim Nutzer.

## 7 — Benachrichtigungen
- **`NotificationTextExtractor` arbeitet auf einem eigenen `NotificationContent`,
  nicht auf `Bundle`.** Das Auspacken der Plattform-Extras steckt in einer dünnen Adapter-
  Funktion, die Regeln dahinter sind ohne Gerät testbar.
- **Tippen auf den Vorschautext öffnet die Benachrichtigung, Tippen auf den App-Namen die
  App.** Ein einziger Klickbereich hätte entweder die App oder die Nachricht unerreichbar
  gemacht.
- **`FLAG_GROUP_SUMMARY` wird zusätzlich zu `FLAG_ONGOING_EVENT` verworfen.** Sonst steht
  bei Messengern die Sammelmeldung statt der eigentlichen Nachricht in der Zeile.
- **Pro-App-Schalter in Room, nicht in DataStore.** Es ist eine Angabe pro App, also
  Nutzerdaten; DataStore bleibt für globale Einstellungen. Dafür Schema-Version 2 mit
  echter Migration.
- **`SwipeableRow` als eigene Komponente.** Wischen links verwirft die Benachrichtigung,
  wischen rechts ist schon für das Pop-up in Etappe 10 verdrahtet. Nur Richtungen mit
  Handler lassen sich ziehen.
- **Der Listener-Dienst hält eine statische Referenz auf sich selbst.** `cancelNotification`
  gibt es nur auf der gebundenen Instanz; alles andere läuft über das Repository.

## 8 — Media-Widget
- **`AccentPicker` prüft den WCAG-Kontrast und hellt notfalls auf.** Palette liefert bei
  dunklen Covern regelmäßig Farben, die auf schwarzem Grund unsichtbar sind; das reine
  Rechnen auf ARGB-Ints ist ohne Bitmap testbar.
- **Die 30-Sekunden-Regel steckt im ViewModel, nicht im Repository.** Sie ist eine
  Darstellungsentscheidung; das Repository meldet nur, was das System sagt.
- **`AudioOutputRepository` registriert zur Laufzeit.** `ACTION_HEADSET_PLUG` und die
  A2DP-Zustandsänderung werden nicht an im Manifest deklarierte Empfänger zugestellt.
- **Musik-Apps für die Einblendung liegen in DataStore.** Das ist eine globale Einstellung
  des Launchers, keine Angabe pro App.
- **Transport-Knöpfe als Unicode-Zeichen statt Material-Icons.** `material-icons-extended`
  wäre eine weitere Bibliothek und mehrere MB für drei Symbole.
- **`LauncherSettings` schon jetzt angelegt**, obwohl DataStore erst ab Etappe 12 richtig
  gebraucht wird — die Musik-App-Liste ist die erste globale Einstellung.

## 9 — Widget-Host
- **Feste Host-ID `0x4C41`.** Sie identifiziert den Launcher gegenüber dem System über
  Neustarts hinweg; eine geänderte ID würde alle gebundenen Widgets verwaisen lassen.
- **Eigener Widget-Wähler über `getInstalledProviders()` statt `ACTION_APPWIDGET_PICK`.**
  Der Systempicker bindet selbst und umgeht damit `bindAppWidgetIdIfAllowed`.
- **`deleteAppWidgetId` immer zusammen mit der Datenbankzeile.** `WidgetViewModel.remove`
  macht beides, jeder abgebrochene Pfad im Wähler ruft `discard`. Zusätzlich räumt
  `pruneOrphans` beim Laden auf, falls doch einmal eine ID übrig bleibt.
- **Konfigurations-Activity zuerst per explizitem `ACTION_APPWIDGET_CONFIGURE`.** Nur wenn
  die Activity nicht auflösbar ist, übernimmt
  `AppWidgetHost.startAppWidgetConfigureActivityForResult`. Dessen Ergebnis landet in der
  Activity, nicht im Compose-Launcher — dieser Pfad behält das Widget optimistisch.
- **`updateAppWidgetSize` mit `SizeF`-Liste** (API 31), nicht die veraltete Min/Max-Variante.
- **Die Uhr wird ersetzt, nicht überlagert**, wenn ein Widget im Slot "statt der Uhr" liegt.
- **`WidgetSlot` als Composable in `ui/home` gelöscht.** Namenskollision mit dem gleichnamigen
  Enum; der Wrapper war ohnehin nur ein `Box`.

## 10 — Pop-ups
- **Eine Liste aus `AppListItem`, nicht zwei getrennte Listen.** Ordner stehen alphabetisch
  zwischen den Apps, also müssen sie durch dieselbe Sortierung laufen.
- **Eine App liegt in höchstens einem Ordner** und verschwindet dann aus der obersten Ebene.
  Sonst steht dieselbe App zweimal in der Liste.
- **Leere Ordner werden automatisch gelöscht.** Ein Ordner ohne Apps ist nur eine Zeile, die
  nichts tut.
- **Der Blur liegt auf einem Container um den ganzen Launcher**, gesteuert von einem
  `animateFloatAsState`, das nur im `graphicsLayer`-Lambda gelesen wird. So kostet das
  Öffnen eines Pop-ups keine Rekomposition der Liste.
- **Die Karte wird an der Zeilenposition verankert und auf 55 % Bildschirmhöhe begrenzt**,
  damit sie bei einer Zeile ganz unten nicht aus dem Bild läuft.
- **Das Widget-Pop-up ist kein eigener Fall**, sondern der Slot `WidgetSlot.Popup` mit dem
  AppKey als `ownerKey` innerhalb des App-Pop-ups.

## 11 — Icons
- **`packageManager.getResourcesForApplication` statt des in der Vorgabe genannten
  `Resources.forPackage`.** Eine solche API gibt es im Android-SDK nicht; das ist der
  vorhandene Weg an fremde Ressourcen.
- **`<queries>` mit genau den zwei Icon-Pack-Actions.** Damit ist `queryIntentActivities`
  für die Pack-Erkennung erlaubt, ohne `QUERY_ALL_PACKAGES`. Die App-Liste kommt weiterhin
  ausschließlich aus `LauncherApps`.
- **`IconPackFilterBuilder` ist die gemeinsame Regelbasis** für den kompilierten
  res/xml-Pfad (XmlPullParser) und den Assets-Pfad (SAX). Nur so ist das Parsen ohne Gerät
  testbar.
- **Auto-Replace nur bei sicherem Treffer (`MIN_SCORE`).** Ein falsches Icon ist schlimmer
  als das Original-Icon.
- **Manuell gesetzte Icons speichern das Pack mit.** Beim Pack-Wechsel wird das alte Pack bei
  Bedarf nachgeladen, statt die Auswahl stillschweigend zu verwerfen.
- **Punkte-Modus nutzt `getVibrantColor`, sonst die dominante Farbe.** Der Vibrant-Wert
  trifft die Markenfarbe besser; die dominante Farbe ist der Rückfall, damit jede App
  abgedeckt ist.
- **Monochrom bevorzugt `AdaptiveIconDrawable.getMonochrome()` (API 33+)** und fällt sonst
  auf eine Sättigung von 0 zurück — auf minSdk 31 gibt es die Ebene noch nicht.

## 12 — Theming
- **`ThemeConfig` ist ein einziges Datenobjekt in DataStore**, nicht zehn lose Schlüssel im
  UI. Export, Import und die Vorlagen schreiben damit denselben Weg wie jede Einstellung.
- **Zahlen und Wahrheitswerte werden im Theme-JSON als Strings abgelegt**, passend zum
  bestehenden `JsonWriter`. Ein Zahlentyp im Parser wäre mehr Code für keinen Gewinn.
- **Unbekannte Werte beim Import fallen auf den Standard zurück**, nie auf eine Ausnahme.
  Eine kaputte Datei darf den Launcher nicht am Starten hindern.
- **Material You kommt aus `dynamicDarkColorScheme`/`dynamicLightColorScheme`.** Ab API 31
  liefert das System die Wallpaper-Palette; eine eigene wäre schlechter und größer.
- **Für die eigene Akzentfarbe rechnet `AccentPalette` die Stufen selbst** (HSL auf gepacktem
  ARGB). Material stellt außerhalb der dynamischen Schemata keine Tonpalette bereit, und so
  ist die Regel ohne Gerät testbar.
- **Der Kontrast wird mit `AccentPicker.contrastRatio` geprüft** statt mit einer zweiten
  Implementierung derselben WCAG-Formel.
- **Extra dunkel ist der manuelle Modus mit schwarzen Flächen**, keine dritte Farblogik.
- **Das Abdunkeln ist eine schwarze Ebene im Compose-Baum**, das Weichzeichnen dagegen
  `FLAG_BLUR_BEHIND` am Fenster: an das Hintergrundbild selbst kommt eine App ohne
  Speicherzugriff nicht heran. Ist der System-Blur aus, bleibt die Abdunklung.
- **Nur die Statusleiste lässt sich ausblenden.** Die Navigationsleiste mitzunehmen würde
  den Gestenbereich verschlucken.
- **Die vier Uhr-Stile leiten sich alle von `displayLarge` ab**, damit eine gewählte Schrift
  auch in der Uhr ankommt.
- **Die Wortuhr rundet auf fünf Minuten und wechselt ab "fünf vor halb" auf die kommende
  Stunde** — so wird die Zeit im Deutschen gesprochen.
- **Die Schriftdatei wird in den App-Speicher kopiert und trägt einen Zeitstempel im Namen.**
  Die SAF-Berechtigung überlebt den Neustart nicht, und Compose merkt sich geladene Schriften
  pro Datei — gleicher Name wäre gleich die alte Schrift.
- **Vorlagen und Import lassen den Schriftpfad in Ruhe.** Ein Pfad aus einer fremden
  Installation zeigt hier auf nichts.
- **`SelectableRow` liegt jetzt in `ui/common`**, weil die Icon- und die Theme-Einstellungen
  dieselbe Zeile brauchen.

## 13 — Kalender und Wetter
- **Der Kalender wird über `CalendarContract.Instances` gelesen, nicht über `Events`.** Ein
  wöchentlicher Termin ist ein Event, aber der Homescreen braucht die nächste Wiederholung.
- **Ganztägige Termine tragen Mitternacht UTC**, nicht lokale Mitternacht. Ihr Datum wird
  deshalb in UTC ausgewertet, sonst wird aus "heute" westlich von Greenwich "gestern".
- **Abgelehnte Einladungen werden weggefiltert** (`SELF_ATTENDEE_STATUS`); sie gehören nicht
  auf den Homescreen.
- **Die Terminliste wird zusätzlich im Composable pro Minutentakt gefiltert.** Der
  ContentObserver merkt nichts davon, dass ein Termin gerade zu Ende gegangen ist.
- **Leere Kalenderauswahl heißt "alle Kalender".** Beim ersten Abwählen wird deshalb von der
  vollständigen Liste ausgegangen, sonst bliebe genau der abgewählte Kalender übrig.
- **Wetter kommt von Open-Meteo über `HttpURLConnection`**, ohne Konto, ohne Schlüssel, ohne
  Play Services — so steht es in der Vorgabe. Eine HTTP-Bibliothek wäre für eine URL zu viel.
- **Koordinaten werden mit `Locale.US` formatiert.** Mit deutscher Locale entstünde
  "latitude=52,5200" und die Anfrage käme als Fehler zurück.
- **Der Standort kommt aus dem `LocationManager`, zuerst als letzte bekannte Position.** Für
  eine Temperatur reicht grobe Genauigkeit; das GPS dafür zu wecken wäre Verschwendung.
- **Die letzte Messung liegt als JSON in DataStore**, nicht in Room. Es ist genau ein Wert,
  und der Homescreen soll ihn sofort zeigen statt auf das Netz zu warten.
- **Fehlgeschlagene Abrufe ändern nichts.** Ein alter Wert ist besser als eine leere Zeile,
  und der Worker meldet `success`, statt in einen Retry-Sturm ohne Netz zu laufen.
- **Der stündliche WorkManager-Job existiert nur, solange das Wetter eingeschaltet ist.**
- **Ein Tippen auf die Wetterzeile aktualisiert sie.** Welche Wetter-App gemeint wäre, weiß
  der Launcher nicht; falsch zu raten ist schlechter als das Naheliegende zu tun.
- **Bei einem Wechsel der Einheit wird neu abgerufen statt umgerechnet.** Open-Meteo liefert
  die Einheit mit; selbst umzurechnen wäre geraten.

## 14 — Gesten
- **Fünf Gesten sind frei belegbar, Wischen nach oben nicht.** Es öffnet die App-Liste und
  ist die eine Geste, die der Launcher nicht verschenken kann.
- **Jede Geste liegt als kurzer String in DataStore** (`none`, `app_list`, `app:<AppKey>` ...).
  Eine neue Aktion braucht so keine Migration, und Unbekanntes wird zu `None` statt zu einem
  Absturz.
- **Benachrichtigungen öffnen und Bildschirm sperren laufen über einen optionalen
  Bedienungshilfen-Dienst** (`GLOBAL_ACTION_NOTIFICATIONS`, `GLOBAL_ACTION_LOCK_SCREEN`). Die
  Leiste lässt sich anders gar nicht öffnen, und fürs Sperren wäre die Alternative ein
  Geräteadmin — deutlich übergriffiger. Der Dienst abonniert keine Ereignisse und darf
  keine Fensterinhalte lesen.
- **Fehlt der Dienst, sagt ein Toast das.** Eine Geste, die stillschweigend nichts tut, ist
  der schlechtere Fehler.
- **Vertikale und horizontale Gesten hängen an zwei getrennten `pointerInput`-Modifiern.**
  Wer den Touch-Slop zuerst überschreitet, bekommt die Geste — genau das gewünschte
  Verhalten, und deutlich weniger Code als ein eigener Gestendetektor.
- **Nach unten wischen zählt nur, wenn die App-Liste geschlossen ist.** Sonst wäre es der
  Zug, mit dem die Liste zugemacht wird.
- **Tippgesten liegen am Wurzel-Modifier des Homescreens.** Uhr, Favoriten und Widgets
  verbrauchen ihre Tipps selbst, also bleibt genau der leere Hintergrund übrig.
- **"Suche öffnen" öffnet die Liste und setzt den Fokus ins Suchfeld**, statt ein zweites
  Such-Overlay zu bauen; das Feld gibt es schon.

## 15 — Nutzungsbremse
- **Die Bremse blockiert nie, sie fragt.** Sie zeigt die Zahl des Tages, wartet ein paar
  Sekunden und lässt dann öffnen. Eine Sperre würde nur umgangen.
- **Gezählt wird pro Paket, nicht pro AppKey.** Zwei Profile derselben App sind dieselbe
  Gewohnheit.
- **Alle Starts laufen über `HomeViewModel.launch`.** Das ist der einzige Punkt, an dem die
  Bremse hängen muss — Homescreen, Liste, Suche und Pop-ups gehen alle dort durch.
- **Die Prüfung ist ein Lookup im Speicher** (Konfiguration und Tageszähler als StateFlow).
  Zwischen Tippen und App darf keine Datenbankabfrage liegen.
- **Mit Nutzungszugriff zählt das System, ohne ihn zählt der Launcher selbst.** Ohne den
  Zugriff bleiben Starts aus Benachrichtigungen oder aus den letzten Apps unsichtbar; das ist
  ein schlechterer, aber ehrlicher Wert.
- **Ob der Nutzungszugriff erteilt ist, wird an der Abfrage selbst erkannt.** Sämtliche
  `AppOpsManager`-Prüfungen sind auf dem aktuellen SDK deprecated; ohne Zugriff kommt die
  Ereignisliste leer zurück, mit Zugriff enthält sie mindestens den eigenen Start.
- **Aufeinanderfolgende `ACTIVITY_RESUMED`-Ereignisse desselben Pakets zählen als eines.**
  Hin- und Herwechseln innerhalb einer App ist ein Öffnen, nicht fünf.
- **Die Zähler stehen in Room mit dem lokalen Tag als Schlüssel** (DB v5) und werden nach
  sieben Tagen gelöscht. Der Tageswechsel passiert beim nächsten Blick auf den Homescreen,
  nicht über einen Timer um Mitternacht.
- **Schwelle und Wartezeit werden beim Lesen und Schreiben geklemmt.** Ein kaputter Wert darf
  nicht dazu führen, dass eine App gar nicht mehr aufgeht.

## 16 — Einstellungen, Sicherung, Einrichtung
- **Die Einstellungen bleiben ein Bildschirm mit aufklappbaren Gruppen**, kein Navigationsbaum.
  Ein zweiter Back-Stack würde mit der Regel kollidieren, dass Zurück nie aus dem Launcher
  führt.
- **Die Sicherung ist eine JSON-Datei über SAF**, mit demselben Leser und Schreiber wie das
  Theme. Kein Auto-Backup von Android: `allowBackup` ist aus, und eine Datei kann der Nutzer
  selbst ablegen, weitergeben und lesen.
- **Einspielen ersetzt, es führt nicht zusammen.** Zwei zusammengemischte Sicherungen ergäben
  einen Zustand, den es auf keinem Gerät je gab.
- **Nicht gesichert werden platzierte Widgets, die Schriftdatei und die Zähler der
  Nutzungsbremse.** Widget-IDs gehören dem `AppWidgetHost` genau dieser Installation, der
  Schriftpfad zeigt in deren Dateien, und die Zähler beschreiben heute, nicht die Einrichtung.
- **Ordner werden beim Einspielen neu angelegt statt unter alten IDs wiederhergestellt.** Die
  IDs vergibt die Datenbank.
- **Die gesicherten Einstellungsschlüssel stehen als Namensliste im Code**, nach Typ getrennt.
  So kann eine fremde Datei nichts in den Store schreiben, was der Launcher nicht kennt.
- **Die Einrichtung lässt sich in jedem Schritt überspringen.** Der Launcher muss ohne jede
  Berechtigung laufen, und ein Setup, das auf Zustimmung besteht, erzieht zum Wegtippen.
- **Die Einrichtung liegt über allem anderen und wird über ein Flag in DataStore gesteuert**,
  das sich in den Einstellungen zurücksetzen lässt.

## 17 — Alltagstauglichkeit
- **R8 läuft nur im Release-Build**, der Debug-Build bleibt unverkleinert. Verteilt wird zwar
  das Debug-APK, aber ein regelmäßig gebauter Release-Build fängt Regeln ab, die später
  fehlen würden: das APK schrumpft von 35 MB auf 3,2 MB.
- **Es gibt genau zwei Keep-Regeln.** Enum-Konstanten des eigenen Pakets, weil Einstellungen
  ihren `name` speichern und ein umbenannter Wert die Einstellung stillschweigend
  zurücksetzen würde, und die WorkManager-Worker, die über ihren Klassennamen aus der
  Datenbank instanziiert werden. Alles Übrige bringen die Bibliotheken selbst mit.
- **Das Baseline-Profil ist von Hand geschrieben, nicht gemessen.** Ein Macrobenchmark braucht
  ein Gerät, das es hier nicht gibt. Aufgenommen ist deshalb der ehrliche Superset: alles aus
  `de.moritzstaat.launcher`. Für einen Launcher ist das vertretbar — er wird per Home-Taste
  gestartet, muss sofort da sein, und sein eigener Code ist neben Compose klein.
- **`androidx.profileinstaller` kommt dazu** (neue Bibliothek, deshalb dieser Eintrag). Ohne
  sie landet das Profil bei einer per `adb` installierten App nicht in ART.
- **Toter Code raus:** `HomeScaffold` aus Etappe 2 und `HomeViewModel.setFavorite` wurden von
  nichts mehr aufgerufen; R8s `usage.txt` hat beide gefunden.
- **Die Medien-Einstellungen waren nie erreichbar.** `setMediaApps` wurde nirgends aufgerufen,
  die Kopfhörer-Einblendung aus Etappe 8 war damit tot. Jetzt gibt es die Gruppe "Medien".

## Nach v0.1.0 — Funde vom Gerät
- **`<queries>` bekommt MAIN + LAUNCHER.** In v0.1.0 zeigte die Liste nur sieben Apps: alle
  vorinstallierten plus die zwei Icon-Packs. Der Grund war nicht die Abfrage — die über
  `LauncherApps` war richtig — sondern die Package-Visibility-Filterung ab Android 11. Ohne
  passenden `<intent>`-Eintrag sieht eine App nur System-Apps und was sie ausdrücklich
  deklariert hat. `QUERY_ALL_PACKAGES` bleibt trotzdem draußen: der deklarierte Launcher-Intent
  liefert genau die startbaren Apps, und die Liste selbst kommt weiter aus `LauncherApps`.
- **`LauncherTheme` setzt jetzt `LocalContentColor`.** `MaterialTheme` tut das nicht, nur
  `Surface` — und der Launcher zeichnet aufs Hintergrundbild statt auf eine Surface. Jeder
  `Text` ohne eigene Farbe lief damit auf den Standardwert des Composition Locals, und der ist
  deckendes Schwarz. In der Einrichtung war die Überschrift schwarz auf schwarz.
- **`topInset` der App-Liste wird abgeleitet statt auf 0 zu stehen.** Der Parameter existierte,
  wurde aber von keinem Aufrufer gesetzt, also lag die erste Zeile unter der Statusleiste. Der
  Standardwert liest die Statusleisten-Inset jetzt selbst; ein vergessener Aufrufer kann den
  Fehler nicht mehr auslösen.
- **Die Suchergebnisliste bekommt dasselbe Inset plus Platz unten.** Sie ersetzt die App-Liste
  im selben Sheet, hatte aber weder das eine noch das andere; die letzte Zeile lag hinter dem
  Suchfeld.

## Nach v0.1.1 — Umlaute und Widget-Wähler
- **Umlaute stehen jetzt als Umlaute im Code.** Die Transkription („öffnen", „Überspringen")
  war reine Vorsicht meinerseits vor der Werkzeugkette und in der App schlicht falsch. Ersetzt
  wurde mit einem expliziten Wörterbuch, nicht mit einer ae/oe/ue-Regel: eine solche Regel
  frisst englische Bezeichner (`query`, `session`, `request`), korrektes Deutsch („aktuellen",
  „Neue", „Schauer", „muss") und vor allem `TextNormalizer`, der Umlaute absichtlich auf
  Digraphen abbildet, damit die Suche beides findet.
- **Der Widget-Wähler gruppiert nach App und ist zugeklappt.** Die Liste, die das System
  liefert, ist flach: ein paar hundert Zeilen mit Labels wie „1×1" oder „Shortcut", und nichts
  sagt, von welcher App eine Zeile kommt.
- **Jede Widget-Zeile zeigt die Vorschau des Anbieters, die Größe in Zellen und die
  Beschreibung.** Die Vorschau ist die eigentliche Antwort auf „was ist das".
- **Vorschauen werden pro Zeile geladen, nicht mit dem Katalog.** Es sind vollwertige Bitmaps,
  teils Screenshots; nur was auf dem Schirm ist, wird dekodiert und dabei auf 480 px begrenzt.
- **Die Größe wird in 70-dp-Zellen mit 30 dp Rand gerechnet.** Der Homescreen hat kein festes
  Raster, aber jedes Widget wurde gegen dieses Raster entworfen, und „4 × 1" ist die Angabe,
  die man aus anderen Launchern kennt. Ein 70-dp-Widget braucht deshalb zwei Zellen.
- **Bleibt bei der Suche genau eine App übrig, klappt sie von selbst auf.** Ein Tipp, der
  nichts entscheidet, ist ein Tipp zu viel.
