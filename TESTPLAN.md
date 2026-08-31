# TESTPLAN

Alles, was nur am echten Geraet (Nothing Phone (2), Nothing OS 4.x, Android 16) pruefbar ist.
Wird bis Etappe 18 fortgeschrieben.

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
