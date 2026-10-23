# Vinyl – Plattenspieler für das Galaxy Z Fold

Eine Android-App (Kotlin + Jetpack Compose), die das Fold wie eine kleine Stereoanlage nutzt:

| Gerät | Display | Inhalt |
|---|---|---|
| aufgeklappt | Innendisplay | **Plattenschrank**: scrollbare Fächer mit Plattenrücken |
| zugeklappt | Außendisplay | **Plattenspieler**: Plattenteller, Tonarm, Wiedergabe |

## Bedienung

1. **Aufklappen**: Im Schrank scrollen und eine Platte antippen. Sie wird ein Stück aus dem Fach gezogen, dann öffnet sich die Hülle.
2. **Platte aus der Hülle ziehen**: auf der Hülle nach rechts wischen.
3. **Nach vorne schicken**: die herausgezogene Platte nach oben wischen. Sie fliegt aus dem Bild, und unten erscheint der Hinweis „liegt vorne bereit“.
4. **Zuklappen**: Auf dem Außendisplay liegt die Platte neben dem Plattenspieler. Zieh sie auf den Plattenteller oder tippe sie an.
5. **Nadel auflegen**: den Tonarm über die Platte schwenken und loslassen. Wo die Nadel aufsetzt, bestimmt Titel und Position, wie bei echtem Vinyl. Die glänzenden Ringe auf der Platte sind die Lücken zwischen den Titeln.
6. **Stoppen**: den Tonarm greifen (die Nadel hebt sich) und zurück auf die Stütze legen. Am Ende der Platte hebt sich die Nadel automatisch.
7. **Platte wechseln**: Bei abgelegtem Tonarm die Platte vom Teller ziehen. Alternativ eine neue Platte nach vorne schicken und auf den Teller ziehen, dann wird getauscht.

Auch aufgeklappt läuft die Musik weiter. Unten im Schrank zeigt eine Statuskarte den laufenden Titel und einen Knopf „Nadel heben“.

## Projekt öffnen und starten

- **Android Studio** (aktuelle Version) → *Open* → diesen Ordner wählen. Das Gradle-Wrapper-Skript lädt Gradle 8.14.3.
- JDK 17 oder neuer, Android SDK 35.
- Auf dem Fold starten. Zum Testen ohne Fold eignen sich ein Emulator mit Geräteprofil *Foldable* oder die Compose-Vorschauen in `ui/Previews.kt`.

### Samsung-Einstellung fürs Zuklappen

Damit die App beim Zuklappen auf dem Außendisplay weiterläuft, muss das in One UI pro App erlaubt sein:
*Einstellungen → Anzeige → „Apps auf Frontdisplay fortsetzen“* (Name je nach One-UI-Version leicht unterschiedlich), dort **Vinyl** aktivieren.

### Wie wird erkannt, welches Display aktiv ist?

`fold/ScreenRole.kt`: Ist das Gerät aufgeklappt, meldet Jetpack WindowManager das Scharnier als `FoldingFeature`. Ohne Scharnier entscheidet die kleinste Displaybreite (ab 600 dp gilt das Display als Innendisplay). So funktioniert die App auch auf Tablets und im Emulator.
Die Activity wird beim Falten nicht neu erstellt (`configChanges` im Manifest), deshalb laufen Animationen und Musik ohne Unterbrechung weiter.

## Architektur

```
app/src/main/java/com/marco/vinyl/
├── MainActivity.kt            Wechselt je nach Display zwischen Schrank und Plattenspieler
├── VinylViewModel.kt          Gemeinsamer Zustand: wartende Platte, Platte auf dem Teller, Nadel
├── VinylApplication.kt        AppContainer (Repository + Player), hier wird die Datenquelle getauscht
├── fold/ScreenRole.kt         Innen- oder Außendisplay?
├── model/                     Album, Track, DeckState, MusicRepository (Schnittstelle zur Datenquelle)
├── data/demo/                 Demo-Platten + Synthesizer für die Demo-Musik
├── playback/PlayerController  Media3 ExoPlayer, app-weit, veröffentlicht die Position für den Tonarm
├── turntable/                 Reine Logik ohne Android: Tonarm-Geometrie, Rille ↔ Titel/Position
└── ui/
    ├── library/               Schrank, Hülle mit Zieh- und Wischgesten
    ├── player/                Plattenspieler (Canvas), Tonarm- und Plattengesten
    └── components/            Gezeichnete Platte, Hülle, Plattenrücken
```

Alle Grafiken sind mit Compose-`Canvas` gezeichnet, es gibt keine Bilddateien. Das ist ein bewusst schlichtes erstes Design, das sich später durch eigene Grafiken ersetzen lässt.

### Demo-Daten

`DemoMusicRepository` liefert 12 erfundene Platten. Die Musik (kurze Lo-Fi-Loops mit Knistern) wird beim Auflegen auf dem Gerät erzeugt und im Cache gespeichert. So gibt es keine urheberrechtlich geschützten Dateien im Repo, und die App braucht kein Netz.

### Web-Prototyp

`prototype/vinyl.html` ist ein klickbarer Nachbau der App für den Browser, mit denselben Maßen, Gesten und derselben Demo-Musik. Damit lassen sich Optik und Gesten schnell ausprobieren, ohne die App zu bauen. Maßgeblich bleibt die Android-App: Änderungen am Design werden in beiden nachgezogen.

### Tests

```
./gradlew test
```

Die Unit-Tests decken Tonarm-Geometrie, die Zuordnung von Rille zu Titel und Position sowie das WAV-Format der Demo-Musik ab.

## Nächste Schritte

- **SoundCloud** als zweite Implementierung von `MusicRepository`:
  1. App bei SoundCloud registrieren (Client-ID und Secret, Redirect-URI).
  2. Login per OAuth 2.1 mit PKCE, z. B. über Custom Tabs.
  3. `GET /me/playlists` (inklusive Alben) → `Album`, `GET /me/likes/tracks` → eine Sammlung „Likes“.
  4. `resolveStreams` holt pro Titel die Stream-URL (`GET /tracks/{id}/streams`); ExoPlayer spielt HLS direkt ab.
  5. Cover über `artworkUrl` laden (z. B. mit Coil).
- **Hintergrundwiedergabe** mit `MediaSessionService`, inklusive Benachrichtigung und Sperrbildschirm.
- **Sound-Details**: Aufsetzgeräusch der Nadel, Anlaufen der Drehzahl, Haptik beim Einrasten.
- **Grafik**: eigene Illustrationen für Schrank, Holz und Plattenspieler.
