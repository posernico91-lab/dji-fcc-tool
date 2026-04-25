# DJI FCC Tool – Datenschutzerklärung

**Stand:** April 2026
**Verantwortlich:** Betreiber der App „DJI Mini FCC Switch" (Kontakt-E-Mail in der App-Store-Listung).

## 1. Was die App tut
Die App sendet zwei verifizierte DUML-Datenpakete (37 Bytes) per USB-OTG an die DJI-N1/N2-Fernbedienung, um den Funkmodus von CE auf FCC umzustellen. Es werden keine Daten an die Drohne selbst geschickt.

## 2. Welche Daten werden verarbeitet?
- **USB-Geräteinformationen** (Vendor-ID, Produkt-ID): nur lokal zur Identifikation der Remote.
- **Werbe-Daten** über das Google Mobile Ads SDK (siehe Abschnitt 3).
- **Keine** Standortdaten, keine Account-Daten, kein Telemetrie-Upload an unseren Server.

## 3. Werbung (Google AdMob)
Die App finanziert sich über Werbung von Google AdMob (Publisher-ID `pub-3778451046949775`). AdMob kann folgende Daten verarbeiten:
- Werbe-ID (Android Advertising ID)
- IP-Adresse, Gerätetyp, Sprache, grobe Standortregion
- App-Nutzungssignale für Frequency Capping

Vor dem ersten Werbe-Request fragt die App über die **Google User Messaging Platform (UMP / IAB TCF v2)** Ihre Einwilligung ab. Sie können:
- Personalisierte Werbung **erlauben**, oder
- **Ablehnen** → die App liefert nur **non-personalized ads** (NPA).

Die Einwilligung kann jederzeit über den Button „Datenschutzeinstellungen / Werbung" in der App widerrufen oder angepasst werden.

Mehr Infos zur Datenverarbeitung durch Google: <https://policies.google.com/technologies/partner-sites>

## 4. ads.txt
Diese Domain hostet eine `ads.txt` zur Authorisierung des AdMob-Publishers gemäß IAB-Spezifikation: [`/ads.txt`](./ads.txt)

## 5. Ihre Rechte (DSGVO)
Sie haben das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung sowie das Recht, der Verarbeitung zu widersprechen. Anfragen bitte an die in der Store-Listung hinterlegte E-Mail-Adresse.
