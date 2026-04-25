# DJI FCC Tool — Android App

> ✅ **Diese App enthält verifizierte FCC-Patch-Bytes** aus dem Open-Source-
> Projekt [M4TH1EU/DJI-FCC-HACK](https://github.com/M4TH1EU/DJI-FCC-HACK)
> (ursprüngliches Reverse-Engineering: **@galbb** auf MavicPilots).
> Funktioniert mit DJI Mini 4K, Mavic Air 2/2S, Mini 2/2 SE, Neo 2 und Flip,
> sofern eine **N1/N2-Remote** (ohne Display) verwendet wird.
>
> Du kannst sie sofort installieren und benutzen. Wer absolut sicher gehen
> will: in `DjiFccProtocol.kt` die Konstante `SAFE_MODE = true` setzen —
> dann sendet die App nichts und zeigt nur das UI.

## Was die App tut

- Erkennt eine angeschlossene DJI-Remote (per USB-VID `0x2CA3`).
- Fragt USB-Berechtigung an, öffnet eine Bulk-Verbindung.
- Liest den aktuellen Funkmodus (CE / FCC / unbekannt).
- Würde im echten Modus den FCC-Patch senden (aktuell SAFE-MODE).
- Zeigt ein klares 4-Schritt-Wizard-UI, Status, Logs und rechtlichen Hinweis.

## Voraussetzungen

| Komponente            | Version     | Bezugsquelle                                            |
|----------------------|------------|--------------------------------------------------------|
| JDK                  | 17 (LTS)   | https://learn.microsoft.com/java/openjdk/download      |
| Android SDK          | 34         | Android Studio → SDK Manager                            |
| Gradle               | 8.5        | wird vom Wrapper automatisch geladen                    |
| Android Studio       | Hedgehog+  | https://developer.android.com/studio                    |
| Android-Telefon      | API 26+    | aktiviertes USB-OTG, USB-Debugging                      |
| USB-OTG-Kabel        | —          | USB-C → USB-A bzw. passender Stecker für deine Remote   |

## Erstes Build

### Variante A: Android Studio (für Anfänger empfohlen)
1. Android Studio öffnen → *Open* → Ordner `dji-fcc-tool` wählen.
2. Wenn Studio fragt: SDK 34 installieren → bestätigen.
3. Oben rechts „Run app“ (▶) → APK landet automatisch auf dem verbundenen Handy.

### Variante B: Kommandozeile (Windows PowerShell)
```powershell
# JDK + SDK Pfade setzen (einmal pro Terminal)
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot'
$env:ANDROID_HOME = 'C:\android-sdk'
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path

cd C:\Users\nicop\dji-fcc-tool

# Debug-APK
./gradlew.bat assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk

# Release-APK + AAB (signiert nur mit Debug-Key, vor Veröffentlichung Keystore eintragen)
./gradlew.bat assembleRelease bundleRelease
```

> Falls das Skript meckert „SDK location not found“, lege im Projektroot
> eine Datei `local.properties` an (siehe `local.properties.example`).

## Sideload aufs Handy

1. Auf dem Handy: *Einstellungen → Über das Telefon → Build-Nummer 7×* tippen.
2. *Entwickleroptionen → USB-Debugging* aktivieren.
3. Per USB ans PC anschließen, Hinweis bestätigen.
4. `./gradlew.bat installDebug` — fertig.

## Bedienung der App (Schritt-für-Schritt)

1. **Schritt 1 – Scan:** Remote einschalten, OTG-Kabel an Telefon, in der
   App auf *Remote suchen* tippen.
2. **Schritt 2 – Verbinden:** Android-Dialog *USB-Zugriff erlauben* mit *OK*
   bestätigen.
3. **Schritt 3 – Erkennen:** App liest den aktuellen Modus (CE/FCC). In
   SAFE-MODE wird hier ein Demo-Wert „CE“ angezeigt.
4. **Schritt 4 – Patch:** *FCC aktivieren* – im SAFE-MODE wird nur der Plan
   geloggt, im echten Modus die Pakete an die Remote gesendet. Anschließend
   USB trennen, Remote aus- und wieder einschalten.

Reset auf CE erfolgt durch *Auf CE zurücksetzen* oder einen Power-Cycle der
Remote.

## SAFE-MODE (optional)

Wenn Du die App nur testen willst, ohne wirklich Bytes an die Remote zu
senden, setze in `app/src/main/java/com/example/djifcctool/DjiFccProtocol.kt`:

```kotlin
const val SAFE_MODE: Boolean = true
```

Dann werden alle Schreibzugriffe ausgelassen und nur ein Demo-Trace ins
Log geschrieben.

## Projektstruktur

```
dji-fcc-tool/
├── app/
│   ├── build.gradle.kts          # App-Build-Config (AGP 8.2.2, Kotlin 1.9.22)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/djifcctool/
│       │   │   ├── DjiFccApp.kt              # Hilt-Application
│       │   │   ├── AppModule.kt              # DI-Modul (UsbManager)
│       │   │   ├── DjiFccProtocol.kt         # Hex-Befehle + SAFE_MODE
│       │   │   ├── DjiUsbSerialManager.kt    # USB-Bulk-IO
│       │   │   ├── MainActivity.kt           # Activity + USB-Receiver
│       │   │   ├── MainViewModel.kt          # Wizard-Logik
│       │   │   ├── MainScreen.kt             # Compose-UI
│       │   │   └── Theme.kt                  # Material-3-Theme
│       │   └── res/
│       │       ├── values/                   # strings, colors, themes
│       │       └── xml/usb_device_filter.xml # Auto-Launch bei DJI-USB
│       └── test/                             # Unit-Tests
├── build.gradle.kts                          # Root-Build (Plugin-Versionen)
├── settings.gradle.kts
├── gradle.properties
├── gradlew, gradlew.bat                      # Wrapper-Skripte
└── gradle/wrapper/                           # gradle-wrapper.jar
```

## Tests ausführen

```powershell
./gradlew.bat test
```

Die Unit-Tests prüfen Checksumme, Mode-Parser und Konfigurations-Defaults.

## Rechtlicher Hinweis

Das Erzwingen von FCC-Funkleistung ist nur in FCC-zugelassenen Regionen
(USA, Teile Lateinamerikas, Asien) erlaubt. In der EU/UK ist es illegal
und kann Bußgelder, Versicherungs- und Garantieverluste nach sich ziehen.
**Verwendung auf eigene Verantwortung.** Der Autor übernimmt keinerlei
Haftung.

## Bekannte Einschränkungen

- Funktioniert NUR mit N1/N2-Remotes (ohne Display). Smart-Controller wird
  nicht unterstützt.
- Mini 3 wird laut Original-Repo nicht unterstützt.
- Patch muss nach jedem Power-Cycle der Remote/Drohne neu angewendet werden.
- Reset auf CE = Drohne und Remote ausschalten und wieder einschalten
  (es existiert kein Software-Reset-Befehl).
- Release-APK wird mit dem Default-Debug-Key signiert. Für Veröffentlichung
  einen eigenen Keystore in `app/build.gradle.kts` konfigurieren.

## Credits

- Reverse Engineering: **@galbb** auf [MavicPilots](https://mavicpilots.com/threads/mavic-air-2-switch-to-fcc-mode-using-an-android-app.115027/)
- Referenzimplementierung: [M4TH1EU/DJI-FCC-HACK](https://github.com/M4TH1EU/DJI-FCC-HACK) (GPL-3.0)
- USB-Serial-Treiber: [mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
