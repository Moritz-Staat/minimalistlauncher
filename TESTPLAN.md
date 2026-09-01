# TESTPLAN

Alles, was nur am echten Gerät (Nothing Phone (2), Nothing OS 4.x, Android 16) prüfbar ist.
Vollständig: Etappen 2 bis 17 sind erfasst.

Während der Entwicklung gab es weder Gerät noch Emulator. Grüner Build und grüne Unit-Tests
sagen nichts über Gesten, Rendering, Berechtigungsdialoge und Akkuverhalten — dafür ist diese
Liste da. Punkte mit **Heikel** sind die, bei denen ein Fehler wahrscheinlich oder besonders
ärgerlich ist; wer wenig Zeit hat, nimmt zuerst die.

Reihenfolge für den ersten Durchlauf auf einem frischen Gerät: Etappe 2, dann 16
(Einrichtung), dann 4 bis 6, danach der Rest in beliebiger Folge.

Installation:
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Der Nothing-Launcher bleibt installiert. Rückweg jederzeit über
Einstellungen > Apps > Standard-Apps > Start-App.

## Etappe 2 — Launcher-Grundgerüst
1. App installieren, über die Einrichtung "Als Standard-Launcher setzen" antippen.
   Erwartung: Systemdialog erscheint, nach Bestätigung ist Minimalist die Start-App.
2. Home-Taste/Geste drücken. Erwartung: kein weißes oder schwarzes Aufblitzen, das
   Wallpaper bleibt sichtbar.
3. Zurück-Geste auf dem Homescreen. Erwartung: nichts passiert, der Launcher bleibt.
4. App öffnen, App-Liste aufziehen, Home drücken. Erwartung: App-Liste ist geschlossen.
5. Gerät drehen. Erwartung: kein Neustart der Activity, kein Flackern.

## Etappe 4 — Homescreen
1. Uhr steht linksbündig auf etwa einem Viertel der Bildschirmhöhe, Datum direkt darunter.
2. Uhr antippen. Erwartung: die Uhr-App öffnet sich (Wecker-Ansicht).
3. Minutenwechsel abwarten. Erwartung: die Uhr springt ohne Zutun weiter.
4. Systemeinstellung auf 12-Stunden-Format umstellen. Erwartung: Uhr übernimmt das Format.
5. Von unten nach oben wischen. Erwartung: die App-Liste fährt mit dem Finger hoch, der
   Homescreen dahinter wird unscharf und dunkler.
6. Wischen und auf halbem Weg loslassen. Erwartung: die Liste fällt zurück oder rastet
   oben ein, je nach Richtung und Geschwindigkeit.
7. App-Liste ganz oben, nach unten wischen. Erwartung: die Liste schließt sich in derselben
   Geste, ohne abzusetzen.
8. Zeilenmaße prüfen: Icon links 40 dp, Zeilenhöhe 56 dp, kein Raster, keine Seiten.
9. Langdruck auf die freie Fläche unter den Favoriten. Erwartung: Einrichtung öffnet sich.
10. Zurück-Geste bei offener App-Liste. Erwartung: Liste schließt, Launcher bleibt.

## Etappe 5 — Wave-Alphabet
1. Leiste am rechten Rand prüfen: nur belegte Anfangsbuchstaben, `#` am Ende, ca. 32 dp breit.
2. Finger auf einen Buchstaben legen (nicht wischen). Erwartung: die App-Liste fährt sofort
   hoch und springt auf diesen Buchstaben.
3. Finger die Leiste entlangziehen. Erwartung: der Buchstabe unter dem Finger ist groß, die
   Nachbarn abgestuft kleiner, alles ruckelfrei.
4. Bei jedem Buchstabenwechsel ein kurzer haptischer Tick.
5. **Heikel:** Ziehen ganz am rechten Rand starten. Erwartung: die Leiste reagiert, die
   Zurück-Geste greift nicht. Symptom bei Fehlschlag: statt der Leiste passiert Zurück oder
   es blitzt der Zurück-Pfeil auf.
   Gegenmaßnahme: `AlphabetBarDefaults.FallbackEdgeInset` (8 dp) als `edgeInset` an
   `AlphabetBar` übergeben, damit die Leiste vom Rand abrückt.
6. Über das obere und untere Ende der Leiste hinausziehen. Erwartung: es bleibt beim ersten
   bzw. letzten Buchstaben, die Geste bricht nicht ab.
7. Finger heben. Erwartung: die Buchstaben gehen auf Normalgröße zurück, die Liste bleibt
   an der angesprungenen Stelle stehen.

## Etappe 6 — Suche und App-Aktionen
1. App-Liste aufziehen. Erwartung: Suchfeld sitzt unten, über der Navigationsleiste.
2. "gm" tippen. Erwartung: Gmail steht weit oben, die getroffenen Buchstaben sind fett.
3. "off" tippen. Erwartung: "Öffi" wird gefunden (Diakritika egal).
4. "strassen" tippen, wenn eine App mit "Straße" im Namen installiert ist. Erwartung: Treffer.
5. Letzte Zeile ist immer "Im Web suchen". Antippen öffnet die Websuche.
6. Ohne Kontakt-Berechtigung suchen. Erwartung: keine Kontakte, keine Fehlermeldung.
7. Langdruck auf eine Zeile in der App-Liste. Erwartung: Menü mit den App-Shortcuts oben,
   darunter Favorit, Umbenennen, Icon ändern, Ausblenden, App-Info, Deinstallieren.
8. **Heikel:** App-Shortcuts erscheinen erst, wenn Minimalist der Standard-Launcher ist.
   Symptom sonst: das Menü zeigt nur die Launcher-Aktionen.
9. Umbenennen, App-Liste schließen und wieder öffnen. Erwartung: der neue Name steht da und
   die App ist danach an der neuen alphabetischen Stelle einsortiert.
10. Ausblenden. Erwartung: die App verschwindet aus Liste und Favoriten, bleibt installiert.
11. Langdruck auf einen Favoriten und ziehen. Erwartung: die Zeile klebt am Finger, die
    anderen rücken nach, nach dem Loslassen bleibt die Reihenfolge erhalten.
12. Langdruck auf einen Favoriten ohne Bewegung. Erwartung: das App-Menü öffnet sich.
13. Zurück-Geste bei offener Suche. Erwartung: erst wird die Suche geleert, erst danach
    schließt die Liste.

## Etappe 7 — Benachrichtigungen
1. Ohne erteilten Zugriff starten. Erwartung: Launcher läuft vollständig, nur ohne
   Vorschautexte. Keine Fehlermeldung, keine leere Zeile.
2. Zugriff in der Einrichtung erteilen. Erwartung: der Systemdialog für
   Benachrichtigungszugriff öffnet sich; danach erscheinen Vorschautexte.
3. Nachricht in einem Messenger empfangen. Erwartung: Text steht einzeilig unter dem
   App-Namen, mit Ellipsis statt Umbruch.
4. Zweite und dritte Nachricht derselben App. Erwartung: Zähler rechts in der Zeile, Vorschau
   zeigt die neueste.
5. Gruppenchat. Erwartung: "Absender: Text". Einzelchat: nur der Text, ohne Namensdopplung.
6. Auf den Vorschautext tippen. Erwartung: die App öffnet sich an der Nachricht.
7. Auf den App-Namen tippen. Erwartung: die App startet normal.
8. Zeile nach links wischen. Erwartung: die Benachrichtigung ist verworfen, auch in der
   Systemleiste.
9. Musik abspielen. Erwartung: die Medien-Benachrichtigung taucht NICHT als Vorschautext auf
   (FLAG_ONGOING_EVENT).
10. Im Langdruck-Menü "Nur 'Neue Nachricht' zeigen" wählen. Erwartung: ab der nächsten
    Nachricht steht dort nur noch "Neue Nachricht".
11. **Heikel:** Nach jedem `adb install -r` ist der Benachrichtigungszugriff weg. Symptom:
    alle Vorschautexte verschwinden nach einem Update. Gegenmaßnahme: in den Systemein-
    stellungen erneut erteilen; die Einrichtung zeigt den Status an.

## Etappe 8 — Media-Widget
1. Ohne Benachrichtigungszugriff Musik abspielen. Erwartung: kein Widget, kein Fehler.
2. Mit Zugriff Musik abspielen. Erwartung: Cover, Titel, Interpret, Play/Pause und
   Skip-Knöpfe erscheinen zwischen Uhr und Favoriten.
3. Pausieren. Erwartung: das Widget bleibt rund 30 Sekunden stehen und verschwindet dann.
4. Innerhalb dieser 30 Sekunden wieder starten. Erwartung: das Widget bleibt, ohne zu blinken.
5. Titel mit farbigem Cover. Erwartung: Interpret und Knöpfe nehmen eine Farbe aus dem
   Cover an, die auf dem Hintergrund lesbar bleibt.
6. Titel ohne Cover. Erwartung: Platzhalterfläche, keine leere Lücke.
7. Auf das Widget tippen. Erwartung: die abspielende App öffnet sich.
8. Skip-Knöpfe bei einer App, die kein Skip anbietet. Erwartung: die Knöpfe sind sichtbar
   abgeblendet und reagieren nicht.
9. Kopfhörer einstecken (oder Bluetooth-Box verbinden), nachdem in den Einstellungen
   Musik-Apps hinterlegt wurden. Erwartung: die Apps erscheinen kurz oben und verschwinden
   nach rund 20 Sekunden von selbst.

## Etappe 9 — Widget-Host
1. Einrichtung öffnen, "Widget unter der Uhr" wählen. Erwartung: Liste aller installierten
   Widget-Anbieter, alphabetisch.
2. Ein Widget ohne Konfiguration wählen. Erwartung: es erscheint sofort unter der Uhr.
3. Ein Widget mit Konfiguration wählen (z. B. eine Uhr- oder Wetter-App). Erwartung: die
   Konfigurationsseite der App öffnet sich; nach Abbrechen bleibt kein leeres Kästchen zurück.
4. Ein Widget wählen, das eine Bestätigung verlangt. Erwartung: der Systemdialog
   "Zugriff auf Widget erlauben" erscheint.
5. Zweites Widget in denselben Slot legen. Erwartung: horizontal wischbarer Stapel mit
   Punktanzeige darunter.
6. Während des Wischens im Stapel: die App-Liste darf sich nicht mitbewegen.
7. Langdruck auf ein Widget. Erwartung: Rückfrage "Widget entfernen?", danach ist es weg.
8. "Widget statt der Uhr" setzen. Erwartung: die Uhr verschwindet, das Widget steht an ihrer
   Stelle. Nach dem Entfernen ist die Uhr wieder da.
9. **Heikel:** App verlassen und zurückkehren. Erwartung: die Widgets sind noch da und
   aktualisieren sich. Symptom bei Fehlschlag: leere graue Flächen.
   Ursache wäre ein fehlendes `startListening()`; es hängt an `MainActivity.onStart`.
10. **Heikel:** Widget entfernen, Gerät neu starten. Erwartung: kein Geister-Widget. Symptom
    sonst: eine leere Fläche, deren ID nie freigegeben wurde.

## Etappe 10 — Pop-ups
1. Zeile in der App-Liste nach rechts wischen. Erwartung: die Karte erscheint auf Höhe der
   Zeile, nicht in der Bildmitte; der Hintergrund wird dunkler und unscharf.
2. Neben die Karte tippen, Zurück-Geste, Karte nach links wischen. Erwartung: jedes davon
   schließt das Pop-up.
3. Während die App-Liste offen ist: nach links und rechts wischen darf die Liste nie
   horizontal verschieben.
4. Im App-Pop-up: Shortcuts der App, die aktuelle Benachrichtigung und "Weitere Aktionen".
5. Langdruck auf eine Zeile, "In Ordner legen", neuen Ordner anlegen. Erwartung: die App
   verschwindet aus der obersten Ebene, der Ordner steht alphabetisch an ihrer Stelle.
6. Ordnerzeile antippen oder nach rechts wischen. Erwartung: Pop-up mit den Apps des Ordners.
7. Letzte App aus einem Ordner nehmen. Erwartung: der Ordner verschwindet von selbst.
8. Ordner-Icon prüfen: bis zu vier Mini-Icons der enthaltenen Apps.

## Etappe 11 — Icons
1. Ohne installiertes Icon-Pack in die Einstellungen. Erwartung: "Kein Icon-Pack installiert",
   Eintrag "Icon-Pack" ist nicht wählbar.
2. Ein ADW- oder Nova-Pack installieren, Einstellungen erneut öffnen. Erwartung: das Pack
   steht in der Liste.
3. Pack wählen. Erwartung: Icons wechseln sofort, in Liste und Favoriten gleichzeitig.
4. Eine App prüfen, die das Pack nicht kennt. Erwartung: entweder ein passendes Icon per
   Namensabgleich oder das Original auf dem Pack-Hintergrund — nie ein offensichtlich
   falsches Icon.
5. Langdruck auf eine App, "Icon ändern", ein Drawable wählen. Erwartung: nur diese App
   ändert sich.
6. Auf ein anderes Pack wechseln. Erwartung: das von Hand gesetzte Icon bleibt erhalten.
7. "Zurücksetzen" im Icon-Dialog. Erwartung: die App bekommt wieder das Pack- bzw.
   Original-Icon.
8. Punkte-Modus. Erwartung: jede App bekommt einen farbigen Punkt, auch frisch installierte.
9. Monochrom-Modus. Erwartung: einfarbige Silhouetten; Apps ohne Monochrom-Ebene erscheinen
   in Graustufen statt zu fehlen.
10. Nach einem Pack-Wechsel Gerät drehen. Erwartung: Icons bleiben korrekt (Cache wird bei
    Konfigurationswechsel geleert).

## Etappe 12 — Theming
1. Vorlagen der Reihe nach antippen. Erwartung: Uhr, Farben und Abdunklung wechseln sofort,
   eine gewählte eigene Schrift bleibt dabei erhalten.
2. Uhr-Stile durchgehen. Erwartung: Groß, Schmal, Zweizeilig (Stunde über Minute) und
   "Als Text" ("viertel nach drei"); die Uhr bleibt immer linksbündig an derselben Höhe.
3. Zeitformat auf 12 Stunden stellen, obwohl das System auf 24 steht. Erwartung: die Uhr
   folgt der App-Einstellung, "Wie im System" folgt wieder dem Gerät.
4. Wortuhr eine Minute lang beobachten. Erwartung: der Text wechselt zur vollen Fünf-Minuten-
   Stufe, ohne dass die Zeile umbricht oder springt.
5. **Heikel:** Material You wählen und das Hintergrundbild wechseln. Erwartung: die Akzente
   folgen dem neuen Bild, spätestens nach dem nächsten Öffnen des Launchers.
6. Eigene Akzentfarbe wählen. Erwartung: der Modus springt auf "Eigene Akzentfarbe", Texte
   auf farbigen Flächen bleiben lesbar (heller Text auf dunklem Akzent und umgekehrt).
7. "Extra dunkel" auf dem OLED-Panel. Erwartung: Flächen sind wirklich schwarz, nicht
   dunkelgrau; im Dunkeln gegen den ausgeschalteten Bildschirm prüfen.
8. Regler "Abdunkeln" von 0 auf 100 %. Erwartung: das Hintergrundbild wird stufenlos dunkler,
   die Schrift bleibt gleich hell.
9. **Heikel:** Regler "Weichzeichnen" hochziehen. Erwartung: das Hintergrundbild wird
   unscharf. Bei aktivem Energiesparmodus oder abgeschalteten Systemblurs passiert nichts —
   dann nur die Abdunklung prüfen (`isCrossWindowBlurEnabled`).
10. "Statusleiste ausblenden" ein- und ausschalten. Erwartung: die Leiste verschwindet, die
    Navigationsleiste bleibt, und von oben wischen holt die Statusleiste kurz zurück.
11. Helles Theme einschalten. Erwartung: die Symbole in Status- und Navigationsleiste werden
    dunkel, bleiben also sichtbar.
12. Schrift wählen (.ttf oder .otf). Erwartung: Uhr, Liste und Einstellungen nutzen die neue
    Schrift. Danach dieselbe Datei durch eine andere ersetzen und erneut wählen — es muss die
    neue Schrift erscheinen, nicht die alte aus dem Cache.
13. **Heikel:** Gewählte Schriftdatei im Dateimanager löschen, Launcher neu starten.
    Erwartung: Systemschrift, kein Absturz.
14. Theme exportieren, Theme ändern, Datei wieder importieren. Erwartung: alles außer der
    Schrift ist zurück; die Schrift bleibt die des Geräts.
15. Beschädigte JSON-Datei importieren. Erwartung: die Einstellung fällt auf Standardwerte
    zurück, der Launcher läuft weiter.
16. Gerät neu starten. Erwartung: Theme, Uhr-Stil und Schrift sind unverändert, und der
    erste Frame zeigt schon die eigenen Farben statt kurz die Standardfarben.

## Etappe 13 — Kalender und Wetter
1. Ohne erteilte Berechtigung "Termine anzeigen" einschalten. Erwartung: der Systemdialog
   erscheint; bei Ablehnung bleibt der Schalter aus und der Hinweistext steht da.
2. Zugriff erteilen. Erwartung: die nächsten bis zu drei Termine stehen unter der Uhr.
3. Einen Termin in der nächsten Stunde anlegen. Erwartung: "in NN Min.", und die Zahl zählt
   im Minutentakt herunter, ohne dass der Launcher neu geöffnet werden muss.
4. **Heikel:** Warten, bis dieser Termin zu Ende ist. Erwartung: er verschwindet von selbst;
   während er läuft steht "Jetzt bis HH:MM".
5. **Heikel:** Einen ganztägigen Termin für heute anlegen. Erwartung: "Ganztägig", nicht
   "Morgen" und nicht "Gestern" — der UTC-Fall.
6. Termin für übermorgen anlegen. Erwartung: Wochentag und Uhrzeit ("Do. 09:00").
7. Einen Termin antippen. Erwartung: die Kalender-App öffnet genau diesen Termin.
8. Einen Kalender in den Einstellungen abwählen. Erwartung: nur dessen Termine verschwinden,
   die anderen bleiben. Alle wieder anwählen ergibt "Alle Kalender".
9. Eine Einladung ablehnen. Erwartung: der Termin verschwindet vom Homescreen.
10. Termin in der Kalender-App verschieben, ohne den Launcher zu öffnen. Erwartung: beim
    nächsten Blick auf den Homescreen steht die neue Zeit da.
11. "Wetter anzeigen" einschalten. Erwartung: Standortdialog, danach binnen weniger Sekunden
    eine Zeile mit Symbol, Temperatur, Beschreibung und Tageshoch/-tief.
12. **Heikel:** Flugmodus einschalten und die Wetterzeile antippen. Erwartung: der alte Wert
    bleibt stehen, nichts stürzt ab, keine leere Zeile.
13. Flugmodus aus, Zeile antippen. Erwartung: "Stand" in den Einstellungen springt auf jetzt.
14. Einheit auf Fahrenheit stellen. Erwartung: neuer Abruf, Anzeige in °F.
15. **Heikel:** Standortberechtigung in den Systemeinstellungen wieder entziehen. Erwartung:
    der Schalter zeigt sich beim nächsten Öffnen der Einstellungen als aus.
16. **Heikel:** Gerät über Nacht liegen lassen. Erwartung: am Morgen ist die Wetterzeile
    aktuell (stündlicher WorkManager-Job), und der Akkuverbrauch des Launchers ist unauffällig.
17. Wetter ausschalten. Erwartung: die Zeile verschwindet, und in den Entwickleroptionen bzw.
    per `adb shell dumpsys jobscheduler` ist kein Wetter-Job mehr eingeplant.

## Etappe 14 — Gesten
1. Auf den leeren Bereich lange drücken. Erwartung: die Einstellungen öffnen sich (Standard).
   Das muss auch ohne jede erteilte Berechtigung funktionieren.
2. Auf die Uhr, ein Favoriten-Icon und ein Widget lange drücken. Erwartung: deren eigene
   Reaktion, nicht die Hintergrundgeste.
3. Doppeltippen auf den Hintergrund ohne Bedienungshilfen-Dienst. Erwartung: der Toast
   "Dafür fehlt der Bedienungshilfen-Dienst.", kein Absturz.
4. **Heikel:** Dienst aktivieren. Bei einer per `adb install` installierten App blendet
   Android den Schalter aus; er muss erst über App-Info → Menü →
   "Eingeschränkte Einstellung zulassen" freigegeben werden.
5. Doppeltippen mit laufendem Dienst. Erwartung: der Bildschirm geht aus.
6. Nach unten wischen. Erwartung: die Benachrichtigungsleiste fährt aus.
7. Nach unten wischen, während die App-Liste offen ist. Erwartung: die Liste geht zu, die
   Leiste bleibt zu.
8. Nach links und rechts wischen mit "Nichts" belegt. Erwartung: nichts passiert, besonders
   kein Zucken der App-Liste.
9. Nach links "App starten" mit einer App belegen und wischen. Erwartung: die App startet.
10. Eine Geste auf "Suche öffnen" legen. Erwartung: die App-Liste fährt hoch, die Tastatur
    kommt und der Cursor steht im Suchfeld.
11. **Heikel:** Kurze, schräge Wischer probieren. Erwartung: entweder App-Liste oder
    Seitengeste, nie beides gleichzeitig.
12. Die zur Geste gewählte App deinstallieren. Erwartung: die Geste tut nichts, der Launcher
    läuft weiter; in den Einstellungen steht wieder "App starten".
13. Bedienungshilfen-Dienst wieder abschalten und den Launcher neu öffnen. Erwartung: die
    Einstellungen zeigen den Hinweistext, die Gesten selbst bleiben eingestellt.

## Etappe 15 — Nutzungsbremse
1. Bremse ausgeschaltet lassen und Apps öffnen. Erwartung: nichts ändert sich, kein
   Zwischenschritt, kein spürbarer Verzögerung beim Start.
2. Bremse einschalten, eine App wählen, Schwelle auf 1 stellen. Erwartung: beim nächsten
   Start kommt die Pausenseite mit dem Namen der App und der Zahl des Tages.
3. Wartezeit auf 5 Sekunden stellen. Erwartung: "Trotzdem öffnen" ist erst nach dem
   Herunterzählen anwählbar.
4. "Lieber nicht" antippen. Erwartung: zurück auf den Homescreen, die App startet nicht, und
   der Zähler steigt nicht.
5. "Trotzdem öffnen" antippen. Erwartung: die App startet, und beim nächsten Start ist die
   Zahl um eins höher.
6. Zurück-Geste auf der Pausenseite. Erwartung: sie schließt sich wie "Lieber nicht".
7. Dieselbe App aus der Suche und aus dem Pop-up starten. Erwartung: die Pause kommt auch da.
8. Wartezeit auf 0 stellen. Erwartung: die Seite erscheint, "Trotzdem öffnen" ist sofort da.
9. **Heikel:** Ohne Nutzungszugriff die App aus den letzten Apps öffnen und danach über den
   Launcher. Erwartung: nur der Start über den Launcher wurde gezählt.
10. Nutzungszugriff erteilen und die Einstellungen erneut öffnen. Erwartung: der Hinweis
    verschwindet, und die Zahlen in der App-Auswahl sind höher als vorher.
11. **Heikel:** Über Mitternacht hinweg prüfen. Erwartung: die Zahl fängt wieder bei null
    an, sobald der Launcher wieder in den Vordergrund kommt.
12. **Heikel:** Eine gebremste App zwanzig Mal hintereinander öffnen. Erwartung: jedes Mal
    die Pause, nie ein Durchrutschen, und die Datenbank wächst nicht sichtbar.
13. Nach einer Woche prüfen, dass alte Tageszeilen verschwunden sind
    (`adb shell run-as` bzw. Backup der launcher.db).

## Etappe 16 — Einstellungen, Sicherung, Einrichtung
1. Frisch installieren. Erwartung: die Einrichtung erscheint vor allem anderen, vier Schritte,
   jeder überspringbar.
2. "Überspringen" auf Schritt 1. Erwartung: der Homescreen ist bedienbar, ohne dass eine
   einzige Berechtigung erteilt wurde.
3. Launcher neu starten. Erwartung: die Einrichtung kommt nicht wieder.
4. In den Einstellungen "Einrichtung erneut zeigen", Launcher neu öffnen. Erwartung: sie
   erscheint wieder.
5. Einstellungen öffnen. Erwartung: alle Gruppen zugeklappt bis auf "System", solange der
   Launcher nicht Standard ist.
6. Jede Gruppe auf- und zuklappen. Erwartung: der Inhalt ist derselbe wie vorher, nichts
   springt, der Scrollzustand bleibt brauchbar.
7. "Sichern" und eine Datei anlegen. Erwartung: Meldung "Sicherung geschrieben"; die Datei ist
   im Texteditor lesbar und enthält Favoriten, Ordner und Einstellungen.
8. **Heikel:** Alles umstellen (Theme, Favoriten, Ordner, eigene Namen, Icons, versteckte
   Apps), dann die Sicherung einspielen. Erwartung: der alte Stand ist zurück, und die
   Meldung nennt die Zahl der Einträge.
9. **Heikel:** Nach dem Einspielen prüfen, dass die Favoritenreihenfolge stimmt und kein
   Ordner doppelt existiert.
10. Eine fremde JSON-Datei einspielen. Erwartung: "Datei ist keine Sicherung.", nichts ändert
    sich.
11. Eine Sicherung eines anderen Geräts einspielen, auf dem Apps fehlen. Erwartung: die
    fehlenden Einträge werden einfach nicht angezeigt, kein Absturz.
12. **Heikel:** Nach dem Einspielen prüfen, dass platzierte Widgets unverändert
    weiterlaufen — sie sind nicht Teil der Sicherung und dürfen auch nicht verschwinden.
13. App deinstallieren, neu installieren, Sicherung einspielen. Erwartung: alles außer
    Widgets, Schrift und Benachrichtigungszugriff ist wieder da.

## Etappe 17 — Alltagstauglichkeit
1. Release-APK bauen und installieren (`assembleRelease`, danach signieren oder
   `adb install -t`). Erwartung: der Launcher startet und verhält sich wie das Debug-APK.
2. **Heikel:** Im Release-Build jede Einstellung einmal umstellen, App beenden, neu öffnen.
   Erwartung: alles ist noch so eingestellt — der Test für die Enum-Keep-Regel.
3. **Heikel:** Im Release-Build das Wetter einschalten und eine Stunde warten. Erwartung: der
   WorkManager-Job läuft (Keep-Regel für die Worker).
4. Kaltstart messen: Gerät neu starten, dann Home drücken. Erwartung: der Homescreen steht
   ohne sichtbaren Aufbau da; die Uhr ist sofort sichtbar, nicht nach einem Frame Leere.
5. `adb shell dumpsys package de.moritzstaat.launcher | grep -i profile` bzw.
   `adb shell cmd package compile -m speed-profile -f de.moritzstaat.launcher`. Erwartung:
   das Baseline-Profil ist installiert.
6. Medien: Musik-App in den Einstellungen wählen, Kopfhörer verbinden. Erwartung: die App
   wird kurz auf dem Homescreen eingeblendet. Schalter aus: nichts wird eingeblendet.
7. Speicher: eine Stunde normal benutzen, dann `adb shell dumpsys meminfo`. Erwartung: kein
   stetig wachsender Java-Heap.

## Gesamtdurchlauf — der Tag danach

Nach den Etappenpunkten einmal einen normalen Tag lang benutzen und dabei auf das achten, was
sich nur so zeigt:

1. Akku über 24 Stunden. Erwartung: der Launcher taucht in der Akkunutzung nicht auffällig
   auf. Verdächtig wären der stündliche Wetter-Job und der Benachrichtigungs-Listener.
2. Speicher nach einem Tag (`adb shell dumpsys meminfo de.moritzstaat.launcher`). Erwartung:
   kein stetig wachsender Java-Heap; der Icon-Cache wird bei Speicherdruck geleert.
3. Zwanzig Mal Home drücken, jedes Mal aus einer anderen App. Erwartung: immer derselbe
   Zustand — Liste zu, Overlays zu, kein Aufblitzen.
4. Alle Berechtigungen entziehen und den Launcher benutzen. Erwartung: nichts stürzt ab,
   jede betroffene Zeile verschwindet einfach.
5. Gerät neu starten. Erwartung: Home führt sofort hierher, Widgets leben, Theme und Icons
   stimmen, das Wetter zeigt den letzten bekannten Wert.
6. Eine App installieren und eine deinstallieren, ohne den Launcher zu öffnen. Erwartung:
   beim nächsten Blick stimmt die Liste.
7. Zweites Nutzerprofil oder Arbeitsprofil, falls vorhanden. Erwartung: dessen Apps tauchen
   auf und starten.
8. Sicherung schreiben, App deinstallieren, neu installieren, Sicherung einspielen.
   Erwartung: der Stand ist wieder da (außer Widgets, Schrift, Benachrichtigungszugriff).

## Nach v0.1.0 — Nachprüfung der Gerätefunde
1. **Heikel:** App-Liste öffnen und mit der App-Liste des Nothing-Launchers vergleichen.
   Erwartung: dieselben Apps, insbesondere alle selbst installierten. In v0.1.0 fehlte alles
   außer System-Apps und Icon-Packs.
2. Alphabetbalken prüfen. Erwartung: viele Buchstaben, nicht nur vier.
3. Eine App neu installieren, ohne den Launcher zu öffnen. Erwartung: sie steht anschließend
   in der Liste.
4. Arbeitsprofil, falls vorhanden. Erwartung: dessen Apps sind ebenfalls da.
5. Einrichtung durchklicken. Erwartung: die Überschriften sind lesbar, nicht schwarz auf
   schwarz.
6. Einstellungen öffnen und durchscrollen. Erwartung: kein Text unsichtbar.
7. App-Liste aufziehen. Erwartung: die erste Zeile beginnt unter der Statusleiste, die Uhrzeit
   oben bleibt frei.
8. Suchen, bis die Trefferliste länger als der Bildschirm ist. Erwartung: der oberste Treffer
   liegt unter der Statusleiste, der unterste über dem Suchfeld.

## Nach v0.1.1 — Umlaute und Widget-Wähler
1. Einrichtung, Einstellungen und Gestenmenü durchsehen. Erwartung: überall echte Umlaute,
   kein „oe", „ae", „ue" und kein „ss" statt „ß".
2. Wortuhr auf „Als Text" stellen. Erwartung: „fünf", „zwölf" korrekt — die waren schon immer
   richtig, dürfen aber nicht mit umgestellt worden sein.
3. Suche nach einer App mit Umlaut im Namen. Erwartung: sie wird sowohl mit „ä" als auch mit
   „ae" gefunden. Das ist der Test dafür, dass `TextNormalizer` unangetastet blieb.
4. Widget hinzufügen. Erwartung: eine kurze Liste von App-Namen mit Widget-Anzahl, nicht
   hunderte Einzelzeilen.
5. Eine App aufklappen. Erwartung: pro Widget eine Vorschau, der Name, die Größe als „4 × 1"
   und, falls die App eine mitliefert, eine Beschreibung.
6. **Heikel:** Eine App mit vielen Widgets aufklappen und scrollen. Erwartung: die Vorschauen
   erscheinen zügig, das Scrollen ruckelt nicht.
7. Im Suchfeld „uhr" tippen. Erwartung: nur passende Widgets, und bleibt eine App übrig, ist
   sie schon aufgeklappt.
8. Nach dem Namen einer App suchen. Erwartung: alle Widgets dieser App bleiben sichtbar, nicht
   nur die namensgleichen.
9. Ein Widget mit Konfigurations-Dialog wählen (z. B. eine Uhr oder ein Kalender-Widget).
   Erwartung: der Dialog kommt, und bei Abbruch bleibt kein leerer Platz zurück.
10. **Heikel:** Ein Widget wählen, dessen Anbieter keine Vorschau mitbringt. Erwartung: das
    App-Icon steht an der Stelle, keine leere Fläche und kein Absturz.

## Nach v0.1.2 — Punktraster-Uhr und Media-Karte
1. Uhr-Stil „Punktraster" wählen. Erwartung: große Blockziffern, zentriert, Datum klein
   darüber, Lücke zwischen Stunde und Minute statt Doppelpunkt.
2. Alle Ziffern sehen: über eine Stunde hinweg oder Systemzeit umstellen. Erwartung: 0 bis 9
   sind sauber lesbar und unterscheidbar, besonders 6/8, 3/9 und 1.
3. Auf 12-Stunden-Format stellen. Erwartung: um 9:05 steht „9 05" ohne führende Null, um
   Mitternacht „12 00".
4. **Heikel:** Gerät drehen bzw. Schriftgröße im System ändern. Erwartung: das Raster skaliert
   mit der Breite und bleibt quadratisch, die Ziffern werden nicht verzerrt.
5. Datum abschalten. Erwartung: nur das Raster, weiter zentriert.
6. Eigene Schrift wählen. Erwartung: das Punktraster ändert sich nicht — es ist gezeichnet,
   keine Schrift. Die anderen vier Stile folgen der Schrift weiter.
7. Musik starten. Erwartung: die Karte zeigt Cover, Titel, Interpret, große Play-Taste,
   Fortschrittsbalken mit Zeit links und Gesamtlänge rechts.
8. **Heikel:** Eine Minute laufen lassen, ohne die App zu berühren. Erwartung: der Balken
   wandert gleichmäßig, die Zeit links zählt mit.
9. Pausieren. Erwartung: der Balken bleibt stehen und läuft nicht weiter.
10. **Heikel:** Innerhalb der Musik-App spulen, dann zum Launcher zurück. Erwartung: der
    Balken sitzt an der neuen Stelle, nicht an der alten.
11. Kopfhörer verbinden. Erwartung: rechts oben erscheint der Gerätename. Trennen: die
    Kennzeichnung verschwindet.
12. **Heikel:** Spotify-eigene Tasten (Shuffle, Herz/Speichern) prüfen. Erwartung: sie
    erscheinen mit Spotifys eigenen Symbolen und tun beim Antippen das Richtige. Bei einer App
    ohne solche Aktionen ist die Zeile einfach kürzer.
13. Podcast oder Livestream abspielen. Erwartung: ohne bekannte Länge kein Balken, aber Titel
    und Tasten funktionieren.
14. **Heikel:** Ein Hörbuch mit über einer Stunde Länge. Erwartung: die Zeitangabe hat ein
    Stundenfeld ("1:02:03").

## Nach v0.2.0 — Ruhezustand, Textliste, Enter in der Suche
1. App-Liste aufziehen, Bildschirm sperren, entsperren. Erwartung: der Homescreen steht da, die
   Liste ist zu.
2. **Heikel:** Pop-up öffnen (Zeile nach rechts wischen), sperren, entsperren. Erwartung: das
   Pop-up ist weg. Dasselbe mit dem Langdruck-Menü, dem Umbenennen-Dialog und der Ordnerauswahl.
3. Einstellungen öffnen, sperren, entsperren. Erwartung: Homescreen.
4. Suchen, sperren, entsperren. Erwartung: das Suchfeld ist leer.
5. Dieselben vier Fälle mit dem Home-Druck statt dem Sperren. Erwartung: identisch — das war
   vorher auch schon falsch, nicht nur nach dem Entsperren.
6. **Heikel:** Widget-Wähler öffnen, sperren, entsperren. Erwartung: Homescreen, und beim
   nächsten Start des Launchers kein Geister-Widget in den Slots.
7. **Heikel:** Nutzungsbremse auslösen, auf der Pausenseite sperren, entsperren. Erwartung:
   Homescreen, und die App wurde nicht gestartet.
8. Icon-Stil „Keine" wählen. Erwartung: reine Textliste, kein Einzug, wo vorher das Icon war —
   auch bei Favoriten, Suchtreffern und Ordnerzeilen.
9. Von „Keine" zurück auf „Original". Erwartung: die Icons sind sofort wieder da.
10. „wh" tippen, sodass nur eine App übrig ist, Enter drücken. Erwartung: die App startet.
11. **Heikel:** Etwas tippen, das zwei Apps trifft, Enter drücken. Erwartung: die Websuche
    öffnet, **nicht** eine der beiden Apps.
12. Etwas tippen, das nichts trifft, Enter drücken. Erwartung: Websuche.
13. Einen Kontakt so eingrenzen, dass er der einzige Treffer ist, Enter. Erwartung: der Kontakt
    öffnet.
14. **Heikel:** Eine App mit Nutzungsbremse als einzigen Treffer suchen, Enter. Erwartung: die
    Pausenseite kommt — die Suche darf die Bremse nicht umgehen.

## Etappe 19 — Häufig genutzte Apps
1. App-Liste aufziehen. Erwartung: oben die Überschrift „Häufig" mit bis zu vier Apps, darunter
   unverändert das Alphabet.
2. Eine App zweimal öffnen, die vorher nicht im Block stand. Erwartung: sie erscheint dort.
3. **Heikel:** Eine App genau einmal öffnen. Erwartung: sie erscheint **nicht** — die Schwelle
   liegt bei zwei.
4. **Heikel:** Alphabetbalken benutzen, während der Block sichtbar ist. Erwartung: der Sprung
   landet exakt auf dem Buchstaben, nicht vier Zeilen daneben. Das ist der wahrscheinlichste
   Fehler dieser Etappe.
5. Denselben Test mit ausgeschaltetem Block (Einstellungen → Nutzung). Erwartung: der Sprung
   stimmt auch dann.
6. Eine App im Block prüfen: sie muss zusätzlich an ihrer alphabetischen Stelle stehen.
7. Eine App aus dem Block starten. Erwartung: sie startet normal, und der Zähler steigt.
8. **Heikel:** Eine App aus dem Block mit aktiver Nutzungsbremse starten. Erwartung: die
   Pausenseite kommt auch hier.
9. Langdruck und Rechtswischen auf einer Zeile im Block. Erwartung: Menü bzw. Pop-up wie in der
   normalen Liste.
10. Eine App im Block ausblenden. Erwartung: sie verschwindet aus dem Block.
11. Eine App im Block deinstallieren. Erwartung: der Block wird kürzer, kein Absturz.
12. Block abschalten. Erwartung: Überschrift und Zeilen verschwinden, die Liste beginnt direkt
    mit dem Alphabet.
13. Icon-Stil „Keine" zusammen mit dem Block. Erwartung: auch im Block keine Icons und kein
    Einzug.
14. **Heikel:** Eine Woche später nachsehen. Erwartung: der Block spiegelt die letzten sieben
    Tage, alte Gewohnheiten fallen heraus.
