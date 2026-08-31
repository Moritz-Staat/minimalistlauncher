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
