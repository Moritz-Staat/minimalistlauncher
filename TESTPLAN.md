# TESTPLAN

Alles, was nur am echten Geraet (Nothing Phone (2), Nothing OS 4.x, Android 16) pruefbar ist.
Vollstaendig: Etappen 2 bis 17 sind erfasst.

Waehrend der Entwicklung gab es weder Geraet noch Emulator. Gruener Build und gruene Unit-Tests
sagen nichts ueber Gesten, Rendering, Berechtigungsdialoge und Akkuverhalten — dafuer ist diese
Liste da. Punkte mit **Heikel** sind die, bei denen ein Fehler wahrscheinlich oder besonders
aergerlich ist; wer wenig Zeit hat, nimmt zuerst die.

Reihenfolge fuer den ersten Durchlauf auf einem frischen Geraet: Etappe 2, dann 16
(Einrichtung), dann 4 bis 6, danach der Rest in beliebiger Folge.

Installation:
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Der Nothing-Launcher bleibt installiert. Rueckweg jederzeit ueber
Einstellungen > Apps > Standard-Apps > Start-App.

## Etappe 2 — Launcher-Grundgeruest
1. App installieren, ueber die Einrichtung "Als Standard-Launcher setzen" antippen.
   Erwartung: Systemdialog erscheint, nach Bestaetigung ist Minimalist die Start-App.
2. Home-Taste/Geste druecken. Erwartung: kein weisses oder schwarzes Aufblitzen, das
   Wallpaper bleibt sichtbar.
3. Zurueck-Geste auf dem Homescreen. Erwartung: nichts passiert, der Launcher bleibt.
4. App oeffnen, App-Liste aufziehen, Home druecken. Erwartung: App-Liste ist geschlossen.
5. Geraet drehen. Erwartung: kein Neustart der Activity, kein Flackern.

## Etappe 4 — Homescreen
1. Uhr steht linksbuendig auf etwa einem Viertel der Bildschirmhoehe, Datum direkt darunter.
2. Uhr antippen. Erwartung: die Uhr-App oeffnet sich (Wecker-Ansicht).
3. Minutenwechsel abwarten. Erwartung: die Uhr springt ohne Zutun weiter.
4. Systemeinstellung auf 12-Stunden-Format umstellen. Erwartung: Uhr uebernimmt das Format.
5. Von unten nach oben wischen. Erwartung: die App-Liste faehrt mit dem Finger hoch, der
   Homescreen dahinter wird unscharf und dunkler.
6. Wischen und auf halbem Weg loslassen. Erwartung: die Liste faellt zurueck oder rastet
   oben ein, je nach Richtung und Geschwindigkeit.
7. App-Liste ganz oben, nach unten wischen. Erwartung: die Liste schliesst sich in derselben
   Geste, ohne abzusetzen.
8. Zeilenmasse pruefen: Icon links 40 dp, Zeilenhoehe 56 dp, kein Raster, keine Seiten.
9. Langdruck auf die freie Flaeche unter den Favoriten. Erwartung: Einrichtung oeffnet sich.
10. Zurueck-Geste bei offener App-Liste. Erwartung: Liste schliesst, Launcher bleibt.

## Etappe 5 — Wave-Alphabet
1. Leiste am rechten Rand pruefen: nur belegte Anfangsbuchstaben, `#` am Ende, ca. 32 dp breit.
2. Finger auf einen Buchstaben legen (nicht wischen). Erwartung: die App-Liste faehrt sofort
   hoch und springt auf diesen Buchstaben.
3. Finger die Leiste entlangziehen. Erwartung: der Buchstabe unter dem Finger ist gross, die
   Nachbarn abgestuft kleiner, alles ruckelfrei.
4. Bei jedem Buchstabenwechsel ein kurzer haptischer Tick.
5. **Heikel:** Ziehen ganz am rechten Rand starten. Erwartung: die Leiste reagiert, die
   Zurueck-Geste greift nicht. Symptom bei Fehlschlag: statt der Leiste passiert Zurueck oder
   es blitzt der Zurueck-Pfeil auf.
   Gegenmassnahme: `AlphabetBarDefaults.FallbackEdgeInset` (8 dp) als `edgeInset` an
   `AlphabetBar` uebergeben, damit die Leiste vom Rand abrueckt.
6. Ueber das obere und untere Ende der Leiste hinausziehen. Erwartung: es bleibt beim ersten
   bzw. letzten Buchstaben, die Geste bricht nicht ab.
7. Finger heben. Erwartung: die Buchstaben gehen auf Normalgroesse zurueck, die Liste bleibt
   an der angesprungenen Stelle stehen.

## Etappe 6 — Suche und App-Aktionen
1. App-Liste aufziehen. Erwartung: Suchfeld sitzt unten, ueber der Navigationsleiste.
2. "gm" tippen. Erwartung: Gmail steht weit oben, die getroffenen Buchstaben sind fett.
3. "off" tippen. Erwartung: "Öffi" wird gefunden (Diakritika egal).
4. "strassen" tippen, wenn eine App mit "Straße" im Namen installiert ist. Erwartung: Treffer.
5. Letzte Zeile ist immer "Im Web suchen". Antippen oeffnet die Websuche.
6. Ohne Kontakt-Berechtigung suchen. Erwartung: keine Kontakte, keine Fehlermeldung.
7. Langdruck auf eine Zeile in der App-Liste. Erwartung: Menue mit den App-Shortcuts oben,
   darunter Favorit, Umbenennen, Icon aendern, Ausblenden, App-Info, Deinstallieren.
8. **Heikel:** App-Shortcuts erscheinen erst, wenn Minimalist der Standard-Launcher ist.
   Symptom sonst: das Menue zeigt nur die Launcher-Aktionen.
9. Umbenennen, App-Liste schliessen und wieder oeffnen. Erwartung: der neue Name steht da und
   die App ist danach an der neuen alphabetischen Stelle einsortiert.
10. Ausblenden. Erwartung: die App verschwindet aus Liste und Favoriten, bleibt installiert.
11. Langdruck auf einen Favoriten und ziehen. Erwartung: die Zeile klebt am Finger, die
    anderen ruecken nach, nach dem Loslassen bleibt die Reihenfolge erhalten.
12. Langdruck auf einen Favoriten ohne Bewegung. Erwartung: das App-Menue oeffnet sich.
13. Zurueck-Geste bei offener Suche. Erwartung: erst wird die Suche geleert, erst danach
    schliesst die Liste.

## Etappe 7 — Benachrichtigungen
1. Ohne erteilten Zugriff starten. Erwartung: Launcher laeuft vollstaendig, nur ohne
   Vorschautexte. Keine Fehlermeldung, keine leere Zeile.
2. Zugriff in der Einrichtung erteilen. Erwartung: der Systemdialog fuer
   Benachrichtigungszugriff oeffnet sich; danach erscheinen Vorschautexte.
3. Nachricht in einem Messenger empfangen. Erwartung: Text steht einzeilig unter dem
   App-Namen, mit Ellipsis statt Umbruch.
4. Zweite und dritte Nachricht derselben App. Erwartung: Zaehler rechts in der Zeile, Vorschau
   zeigt die neueste.
5. Gruppenchat. Erwartung: "Absender: Text". Einzelchat: nur der Text, ohne Namensdopplung.
6. Auf den Vorschautext tippen. Erwartung: die App oeffnet sich an der Nachricht.
7. Auf den App-Namen tippen. Erwartung: die App startet normal.
8. Zeile nach links wischen. Erwartung: die Benachrichtigung ist verworfen, auch in der
   Systemleiste.
9. Musik abspielen. Erwartung: die Medien-Benachrichtigung taucht NICHT als Vorschautext auf
   (FLAG_ONGOING_EVENT).
10. Im Langdruck-Menue "Nur 'Neue Nachricht' zeigen" waehlen. Erwartung: ab der naechsten
    Nachricht steht dort nur noch "Neue Nachricht".
11. **Heikel:** Nach jedem `adb install -r` ist der Benachrichtigungszugriff weg. Symptom:
    alle Vorschautexte verschwinden nach einem Update. Gegenmassnahme: in den Systemein-
    stellungen erneut erteilen; die Einrichtung zeigt den Status an.

## Etappe 8 — Media-Widget
1. Ohne Benachrichtigungszugriff Musik abspielen. Erwartung: kein Widget, kein Fehler.
2. Mit Zugriff Musik abspielen. Erwartung: Cover, Titel, Interpret, Play/Pause und
   Skip-Knoepfe erscheinen zwischen Uhr und Favoriten.
3. Pausieren. Erwartung: das Widget bleibt rund 30 Sekunden stehen und verschwindet dann.
4. Innerhalb dieser 30 Sekunden wieder starten. Erwartung: das Widget bleibt, ohne zu blinken.
5. Titel mit farbigem Cover. Erwartung: Interpret und Knoepfe nehmen eine Farbe aus dem
   Cover an, die auf dem Hintergrund lesbar bleibt.
6. Titel ohne Cover. Erwartung: Platzhalterflaeche, keine leere Luecke.
7. Auf das Widget tippen. Erwartung: die abspielende App oeffnet sich.
8. Skip-Knoepfe bei einer App, die kein Skip anbietet. Erwartung: die Knoepfe sind sichtbar
   abgeblendet und reagieren nicht.
9. Kopfhoerer einstecken (oder Bluetooth-Box verbinden), nachdem in den Einstellungen
   Musik-Apps hinterlegt wurden. Erwartung: die Apps erscheinen kurz oben und verschwinden
   nach rund 20 Sekunden von selbst.

## Etappe 9 — Widget-Host
1. Einrichtung oeffnen, "Widget unter der Uhr" waehlen. Erwartung: Liste aller installierten
   Widget-Anbieter, alphabetisch.
2. Ein Widget ohne Konfiguration waehlen. Erwartung: es erscheint sofort unter der Uhr.
3. Ein Widget mit Konfiguration waehlen (z. B. eine Uhr- oder Wetter-App). Erwartung: die
   Konfigurationsseite der App oeffnet sich; nach Abbrechen bleibt kein leeres Kaestchen zurueck.
4. Ein Widget waehlen, das eine Bestaetigung verlangt. Erwartung: der Systemdialog
   "Zugriff auf Widget erlauben" erscheint.
5. Zweites Widget in denselben Slot legen. Erwartung: horizontal wischbarer Stapel mit
   Punktanzeige darunter.
6. Waehrend des Wischens im Stapel: die App-Liste darf sich nicht mitbewegen.
7. Langdruck auf ein Widget. Erwartung: Rueckfrage "Widget entfernen?", danach ist es weg.
8. "Widget statt der Uhr" setzen. Erwartung: die Uhr verschwindet, das Widget steht an ihrer
   Stelle. Nach dem Entfernen ist die Uhr wieder da.
9. **Heikel:** App verlassen und zurueckkehren. Erwartung: die Widgets sind noch da und
   aktualisieren sich. Symptom bei Fehlschlag: leere graue Flaechen.
   Ursache waere ein fehlendes `startListening()`; es haengt an `MainActivity.onStart`.
10. **Heikel:** Widget entfernen, Geraet neu starten. Erwartung: kein Geister-Widget. Symptom
    sonst: eine leere Flaeche, deren ID nie freigegeben wurde.

## Etappe 10 — Pop-ups
1. Zeile in der App-Liste nach rechts wischen. Erwartung: die Karte erscheint auf Hoehe der
   Zeile, nicht in der Bildmitte; der Hintergrund wird dunkler und unscharf.
2. Neben die Karte tippen, Zurueck-Geste, Karte nach links wischen. Erwartung: jedes davon
   schliesst das Pop-up.
3. Waehrend die App-Liste offen ist: nach links und rechts wischen darf die Liste nie
   horizontal verschieben.
4. Im App-Pop-up: Shortcuts der App, die aktuelle Benachrichtigung und "Weitere Aktionen".
5. Langdruck auf eine Zeile, "In Ordner legen", neuen Ordner anlegen. Erwartung: die App
   verschwindet aus der obersten Ebene, der Ordner steht alphabetisch an ihrer Stelle.
6. Ordnerzeile antippen oder nach rechts wischen. Erwartung: Pop-up mit den Apps des Ordners.
7. Letzte App aus einem Ordner nehmen. Erwartung: der Ordner verschwindet von selbst.
8. Ordner-Icon pruefen: bis zu vier Mini-Icons der enthaltenen Apps.

## Etappe 11 — Icons
1. Ohne installiertes Icon-Pack in die Einstellungen. Erwartung: "Kein Icon-Pack installiert",
   Eintrag "Icon-Pack" ist nicht waehlbar.
2. Ein ADW- oder Nova-Pack installieren, Einstellungen erneut oeffnen. Erwartung: das Pack
   steht in der Liste.
3. Pack waehlen. Erwartung: Icons wechseln sofort, in Liste und Favoriten gleichzeitig.
4. Eine App pruefen, die das Pack nicht kennt. Erwartung: entweder ein passendes Icon per
   Namensabgleich oder das Original auf dem Pack-Hintergrund — nie ein offensichtlich
   falsches Icon.
5. Langdruck auf eine App, "Icon aendern", ein Drawable waehlen. Erwartung: nur diese App
   aendert sich.
6. Auf ein anderes Pack wechseln. Erwartung: das von Hand gesetzte Icon bleibt erhalten.
7. "Zuruecksetzen" im Icon-Dialog. Erwartung: die App bekommt wieder das Pack- bzw.
   Original-Icon.
8. Punkte-Modus. Erwartung: jede App bekommt einen farbigen Punkt, auch frisch installierte.
9. Monochrom-Modus. Erwartung: einfarbige Silhouetten; Apps ohne Monochrom-Ebene erscheinen
   in Graustufen statt zu fehlen.
10. Nach einem Pack-Wechsel Geraet drehen. Erwartung: Icons bleiben korrekt (Cache wird bei
    Konfigurationswechsel geleert).

## Etappe 12 — Theming
1. Vorlagen der Reihe nach antippen. Erwartung: Uhr, Farben und Abdunklung wechseln sofort,
   eine gewaehlte eigene Schrift bleibt dabei erhalten.
2. Uhr-Stile durchgehen. Erwartung: Gross, Schmal, Zweizeilig (Stunde ueber Minute) und
   "Als Text" ("viertel nach drei"); die Uhr bleibt immer linksbuendig an derselben Hoehe.
3. Zeitformat auf 12 Stunden stellen, obwohl das System auf 24 steht. Erwartung: die Uhr
   folgt der App-Einstellung, "Wie im System" folgt wieder dem Geraet.
4. Wortuhr eine Minute lang beobachten. Erwartung: der Text wechselt zur vollen Fuenf-Minuten-
   Stufe, ohne dass die Zeile umbricht oder springt.
5. **Heikel:** Material You waehlen und das Hintergrundbild wechseln. Erwartung: die Akzente
   folgen dem neuen Bild, spaetestens nach dem naechsten Oeffnen des Launchers.
6. Eigene Akzentfarbe waehlen. Erwartung: der Modus springt auf "Eigene Akzentfarbe", Texte
   auf farbigen Flaechen bleiben lesbar (heller Text auf dunklem Akzent und umgekehrt).
7. "Extra dunkel" auf dem OLED-Panel. Erwartung: Flaechen sind wirklich schwarz, nicht
   dunkelgrau; im Dunkeln gegen den ausgeschalteten Bildschirm pruefen.
8. Regler "Abdunkeln" von 0 auf 100 %. Erwartung: das Hintergrundbild wird stufenlos dunkler,
   die Schrift bleibt gleich hell.
9. **Heikel:** Regler "Weichzeichnen" hochziehen. Erwartung: das Hintergrundbild wird
   unscharf. Bei aktivem Energiesparmodus oder abgeschalteten Systemblurs passiert nichts —
   dann nur die Abdunklung pruefen (`isCrossWindowBlurEnabled`).
10. "Statusleiste ausblenden" ein- und ausschalten. Erwartung: die Leiste verschwindet, die
    Navigationsleiste bleibt, und von oben wischen holt die Statusleiste kurz zurueck.
11. Helles Theme einschalten. Erwartung: die Symbole in Status- und Navigationsleiste werden
    dunkel, bleiben also sichtbar.
12. Schrift waehlen (.ttf oder .otf). Erwartung: Uhr, Liste und Einstellungen nutzen die neue
    Schrift. Danach dieselbe Datei durch eine andere ersetzen und erneut waehlen — es muss die
    neue Schrift erscheinen, nicht die alte aus dem Cache.
13. **Heikel:** Gewaehlte Schriftdatei im Dateimanager loeschen, Launcher neu starten.
    Erwartung: Systemschrift, kein Absturz.
14. Theme exportieren, Theme aendern, Datei wieder importieren. Erwartung: alles ausser der
    Schrift ist zurueck; die Schrift bleibt die des Geraets.
15. Beschaedigte JSON-Datei importieren. Erwartung: die Einstellung faellt auf Standardwerte
    zurueck, der Launcher laeuft weiter.
16. Geraet neu starten. Erwartung: Theme, Uhr-Stil und Schrift sind unveraendert, und der
    erste Frame zeigt schon die eigenen Farben statt kurz die Standardfarben.

## Etappe 13 — Kalender und Wetter
1. Ohne erteilte Berechtigung "Termine anzeigen" einschalten. Erwartung: der Systemdialog
   erscheint; bei Ablehnung bleibt der Schalter aus und der Hinweistext steht da.
2. Zugriff erteilen. Erwartung: die naechsten bis zu drei Termine stehen unter der Uhr.
3. Einen Termin in der naechsten Stunde anlegen. Erwartung: "in NN Min.", und die Zahl zaehlt
   im Minutentakt herunter, ohne dass der Launcher neu geoeffnet werden muss.
4. **Heikel:** Warten, bis dieser Termin zu Ende ist. Erwartung: er verschwindet von selbst;
   waehrend er laeuft steht "Jetzt bis HH:MM".
5. **Heikel:** Einen ganztaegigen Termin fuer heute anlegen. Erwartung: "Ganztaegig", nicht
   "Morgen" und nicht "Gestern" — der UTC-Fall.
6. Termin fuer uebermorgen anlegen. Erwartung: Wochentag und Uhrzeit ("Do. 09:00").
7. Einen Termin antippen. Erwartung: die Kalender-App oeffnet genau diesen Termin.
8. Einen Kalender in den Einstellungen abwaehlen. Erwartung: nur dessen Termine verschwinden,
   die anderen bleiben. Alle wieder anwaehlen ergibt "Alle Kalender".
9. Eine Einladung ablehnen. Erwartung: der Termin verschwindet vom Homescreen.
10. Termin in der Kalender-App verschieben, ohne den Launcher zu oeffnen. Erwartung: beim
    naechsten Blick auf den Homescreen steht die neue Zeit da.
11. "Wetter anzeigen" einschalten. Erwartung: Standortdialog, danach binnen weniger Sekunden
    eine Zeile mit Symbol, Temperatur, Beschreibung und Tageshoch/-tief.
12. **Heikel:** Flugmodus einschalten und die Wetterzeile antippen. Erwartung: der alte Wert
    bleibt stehen, nichts stuerzt ab, keine leere Zeile.
13. Flugmodus aus, Zeile antippen. Erwartung: "Stand" in den Einstellungen springt auf jetzt.
14. Einheit auf Fahrenheit stellen. Erwartung: neuer Abruf, Anzeige in °F.
15. **Heikel:** Standortberechtigung in den Systemeinstellungen wieder entziehen. Erwartung:
    der Schalter zeigt sich beim naechsten Oeffnen der Einstellungen als aus.
16. **Heikel:** Geraet ueber Nacht liegen lassen. Erwartung: am Morgen ist die Wetterzeile
    aktuell (stuendlicher WorkManager-Job), und der Akkuverbrauch des Launchers ist unauffaellig.
17. Wetter ausschalten. Erwartung: die Zeile verschwindet, und in den Entwickleroptionen bzw.
    per `adb shell dumpsys jobscheduler` ist kein Wetter-Job mehr eingeplant.

## Etappe 14 — Gesten
1. Auf den leeren Bereich lange druecken. Erwartung: die Einstellungen oeffnen sich (Standard).
   Das muss auch ohne jede erteilte Berechtigung funktionieren.
2. Auf die Uhr, ein Favoriten-Icon und ein Widget lange druecken. Erwartung: deren eigene
   Reaktion, nicht die Hintergrundgeste.
3. Doppeltippen auf den Hintergrund ohne Bedienungshilfen-Dienst. Erwartung: der Toast
   "Dafuer fehlt der Bedienungshilfen-Dienst.", kein Absturz.
4. **Heikel:** Dienst aktivieren. Bei einer per `adb install` installierten App blendet
   Android den Schalter aus; er muss erst ueber App-Info → Menue →
   "Eingeschraenkte Einstellung zulassen" freigegeben werden.
5. Doppeltippen mit laufendem Dienst. Erwartung: der Bildschirm geht aus.
6. Nach unten wischen. Erwartung: die Benachrichtigungsleiste faehrt aus.
7. Nach unten wischen, waehrend die App-Liste offen ist. Erwartung: die Liste geht zu, die
   Leiste bleibt zu.
8. Nach links und rechts wischen mit "Nichts" belegt. Erwartung: nichts passiert, besonders
   kein Zucken der App-Liste.
9. Nach links "App starten" mit einer App belegen und wischen. Erwartung: die App startet.
10. Eine Geste auf "Suche oeffnen" legen. Erwartung: die App-Liste faehrt hoch, die Tastatur
    kommt und der Cursor steht im Suchfeld.
11. **Heikel:** Kurze, schraege Wischer probieren. Erwartung: entweder App-Liste oder
    Seitengeste, nie beides gleichzeitig.
12. Die zur Geste gewaehlte App deinstallieren. Erwartung: die Geste tut nichts, der Launcher
    laeuft weiter; in den Einstellungen steht wieder "App starten".
13. Bedienungshilfen-Dienst wieder abschalten und den Launcher neu oeffnen. Erwartung: die
    Einstellungen zeigen den Hinweistext, die Gesten selbst bleiben eingestellt.

## Etappe 15 — Nutzungsbremse
1. Bremse ausgeschaltet lassen und Apps oeffnen. Erwartung: nichts aendert sich, kein
   Zwischenschritt, kein spuerbarer Verzoegerung beim Start.
2. Bremse einschalten, eine App waehlen, Schwelle auf 1 stellen. Erwartung: beim naechsten
   Start kommt die Pausenseite mit dem Namen der App und der Zahl des Tages.
3. Wartezeit auf 5 Sekunden stellen. Erwartung: "Trotzdem oeffnen" ist erst nach dem
   Herunterzaehlen anwaehlbar.
4. "Lieber nicht" antippen. Erwartung: zurueck auf den Homescreen, die App startet nicht, und
   der Zaehler steigt nicht.
5. "Trotzdem oeffnen" antippen. Erwartung: die App startet, und beim naechsten Start ist die
   Zahl um eins hoeher.
6. Zurueck-Geste auf der Pausenseite. Erwartung: sie schliesst sich wie "Lieber nicht".
7. Dieselbe App aus der Suche und aus dem Pop-up starten. Erwartung: die Pause kommt auch da.
8. Wartezeit auf 0 stellen. Erwartung: die Seite erscheint, "Trotzdem oeffnen" ist sofort da.
9. **Heikel:** Ohne Nutzungszugriff die App aus den letzten Apps oeffnen und danach ueber den
   Launcher. Erwartung: nur der Start ueber den Launcher wurde gezaehlt.
10. Nutzungszugriff erteilen und die Einstellungen erneut oeffnen. Erwartung: der Hinweis
    verschwindet, und die Zahlen in der App-Auswahl sind hoeher als vorher.
11. **Heikel:** Ueber Mitternacht hinweg pruefen. Erwartung: die Zahl faengt wieder bei null
    an, sobald der Launcher wieder in den Vordergrund kommt.
12. **Heikel:** Eine gebremste App zwanzig Mal hintereinander oeffnen. Erwartung: jedes Mal
    die Pause, nie ein Durchrutschen, und die Datenbank waechst nicht sichtbar.
13. Nach einer Woche pruefen, dass alte Tageszeilen verschwunden sind
    (`adb shell run-as` bzw. Backup der launcher.db).

## Etappe 16 — Einstellungen, Sicherung, Einrichtung
1. Frisch installieren. Erwartung: die Einrichtung erscheint vor allem anderen, vier Schritte,
   jeder ueberspringbar.
2. "Ueberspringen" auf Schritt 1. Erwartung: der Homescreen ist bedienbar, ohne dass eine
   einzige Berechtigung erteilt wurde.
3. Launcher neu starten. Erwartung: die Einrichtung kommt nicht wieder.
4. In den Einstellungen "Einrichtung erneut zeigen", Launcher neu oeffnen. Erwartung: sie
   erscheint wieder.
5. Einstellungen oeffnen. Erwartung: alle Gruppen zugeklappt bis auf "System", solange der
   Launcher nicht Standard ist.
6. Jede Gruppe auf- und zuklappen. Erwartung: der Inhalt ist derselbe wie vorher, nichts
   springt, der Scrollzustand bleibt brauchbar.
7. "Sichern" und eine Datei anlegen. Erwartung: Meldung "Sicherung geschrieben"; die Datei ist
   im Texteditor lesbar und enthaelt Favoriten, Ordner und Einstellungen.
8. **Heikel:** Alles umstellen (Theme, Favoriten, Ordner, eigene Namen, Icons, versteckte
   Apps), dann die Sicherung einspielen. Erwartung: der alte Stand ist zurueck, und die
   Meldung nennt die Zahl der Eintraege.
9. **Heikel:** Nach dem Einspielen pruefen, dass die Favoritenreihenfolge stimmt und kein
   Ordner doppelt existiert.
10. Eine fremde JSON-Datei einspielen. Erwartung: "Datei ist keine Sicherung.", nichts aendert
    sich.
11. Eine Sicherung eines anderen Geraets einspielen, auf dem Apps fehlen. Erwartung: die
    fehlenden Eintraege werden einfach nicht angezeigt, kein Absturz.
12. **Heikel:** Nach dem Einspielen pruefen, dass platzierte Widgets unveraendert
    weiterlaufen — sie sind nicht Teil der Sicherung und duerfen auch nicht verschwinden.
13. App deinstallieren, neu installieren, Sicherung einspielen. Erwartung: alles ausser
    Widgets, Schrift und Benachrichtigungszugriff ist wieder da.

## Etappe 17 — Alltagstauglichkeit
1. Release-APK bauen und installieren (`assembleRelease`, danach signieren oder
   `adb install -t`). Erwartung: der Launcher startet und verhaelt sich wie das Debug-APK.
2. **Heikel:** Im Release-Build jede Einstellung einmal umstellen, App beenden, neu oeffnen.
   Erwartung: alles ist noch so eingestellt — der Test fuer die Enum-Keep-Regel.
3. **Heikel:** Im Release-Build das Wetter einschalten und eine Stunde warten. Erwartung: der
   WorkManager-Job laeuft (Keep-Regel fuer die Worker).
4. Kaltstart messen: Geraet neu starten, dann Home druecken. Erwartung: der Homescreen steht
   ohne sichtbaren Aufbau da; die Uhr ist sofort sichtbar, nicht nach einem Frame Leere.
5. `adb shell dumpsys package de.moritzstaat.launcher | grep -i profile` bzw.
   `adb shell cmd package compile -m speed-profile -f de.moritzstaat.launcher`. Erwartung:
   das Baseline-Profil ist installiert.
6. Medien: Musik-App in den Einstellungen waehlen, Kopfhoerer verbinden. Erwartung: die App
   wird kurz auf dem Homescreen eingeblendet. Schalter aus: nichts wird eingeblendet.
7. Speicher: eine Stunde normal benutzen, dann `adb shell dumpsys meminfo`. Erwartung: kein
   stetig wachsender Java-Heap.

## Gesamtdurchlauf — der Tag danach

Nach den Etappenpunkten einmal einen normalen Tag lang benutzen und dabei auf das achten, was
sich nur so zeigt:

1. Akku ueber 24 Stunden. Erwartung: der Launcher taucht in der Akkunutzung nicht auffaellig
   auf. Verdaechtig waeren der stuendliche Wetter-Job und der Benachrichtigungs-Listener.
2. Speicher nach einem Tag (`adb shell dumpsys meminfo de.moritzstaat.launcher`). Erwartung:
   kein stetig wachsender Java-Heap; der Icon-Cache wird bei Speicherdruck geleert.
3. Zwanzig Mal Home druecken, jedes Mal aus einer anderen App. Erwartung: immer derselbe
   Zustand — Liste zu, Overlays zu, kein Aufblitzen.
4. Alle Berechtigungen entziehen und den Launcher benutzen. Erwartung: nichts stuerzt ab,
   jede betroffene Zeile verschwindet einfach.
5. Geraet neu starten. Erwartung: Home fuehrt sofort hierher, Widgets leben, Theme und Icons
   stimmen, das Wetter zeigt den letzten bekannten Wert.
6. Eine App installieren und eine deinstallieren, ohne den Launcher zu oeffnen. Erwartung:
   beim naechsten Blick stimmt die Liste.
7. Zweites Nutzerprofil oder Arbeitsprofil, falls vorhanden. Erwartung: dessen Apps tauchen
   auf und starten.
8. Sicherung schreiben, App deinstallieren, neu installieren, Sicherung einspielen.
   Erwartung: der Stand ist wieder da (ausser Widgets, Schrift, Benachrichtigungszugriff).

## Nach v0.1.0 — Nachpruefung der Geraetefunde
1. **Heikel:** App-Liste oeffnen und mit der App-Liste des Nothing-Launchers vergleichen.
   Erwartung: dieselben Apps, insbesondere alle selbst installierten. In v0.1.0 fehlte alles
   ausser System-Apps und Icon-Packs.
2. Alphabetbalken pruefen. Erwartung: viele Buchstaben, nicht nur vier.
3. Eine App neu installieren, ohne den Launcher zu oeffnen. Erwartung: sie steht anschliessend
   in der Liste.
4. Arbeitsprofil, falls vorhanden. Erwartung: dessen Apps sind ebenfalls da.
5. Einrichtung durchklicken. Erwartung: die Ueberschriften sind lesbar, nicht schwarz auf
   schwarz.
6. Einstellungen oeffnen und durchscrollen. Erwartung: kein Text unsichtbar.
7. App-Liste aufziehen. Erwartung: die erste Zeile beginnt unter der Statusleiste, die Uhrzeit
   oben bleibt frei.
8. Suchen, bis die Trefferliste laenger als der Bildschirm ist. Erwartung: der oberste Treffer
   liegt unter der Statusleiste, der unterste ueber dem Suchfeld.
