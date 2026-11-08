# Glass Launcher — launcher Android ispirato a iOS 27

Launcher sostitutivo (Kotlin + Jetpack Compose, minSdk 26). Si apre con Android Studio (Koala o successivo): *Open* sulla cartella, attendi la sincronizzazione Gradle, esegui su dispositivo, poi premi Home e scegli "Glass Launcher" come predefinito.

## Cosa include (e come si lega ai punti richiesti)
- **Liquid Glass** (`Glass.kt`): sfondo sfocato dietro ogni pannello, effetto lente (ingrandimento 5%), saturazione, velo, riflesso superiore e bordo luminoso. Come in iOS 27 i riflessi sono statici. Lo **slider di trasparenza** (novità iOS 27) è in Personalizza (pressione prolungata sulla home).
- **Home** (`Home.kt`): griglia 4 colonne con icone arrotondate, dock in vetro, pallini pagina, pill "Cerca", pagine calcolate in base all'altezza dello schermo, tasto Home → prima pagina, menu a pressione prolungata (info/rimuovi).
- **App Library** (`Library.kt`): ultima pagina, gruppi per categoria con 3 icone + mini-cluster, tocco per aprire il gruppo, ricerca.
- **Widget a sinistra** (`Widgets.kt`): data, batteria (reale), meteo (dati di ESEMPIO), calendario extra-large (novità iOS 27), controlli musica.
- **Blocco schermo** (`Lock.kt`): 6 stili orologio incluso il **compatto** di iOS 27, colore, **altezza** e dimensione regolabili; pressione prolungata per personalizzare; scorri su per sbloccare; torcia e fotocamera.
- **Centro di controllo** (`ControlCenter.kt`): trascina verso il basso dall'angolo in alto a destra (o tocca). Connettività, media, luminosità, volume, torcia, timer, calcolatrice, fotocamera.

## Limiti reali di Android
- Il blocco schermo è **grafico**: non sostituisce la sicurezza di sistema (PIN/biometria restano quelle di Android). Compare all'avvio e dopo lo spegnimento dello schermo.
- Wi-Fi, Bluetooth, modalità aereo: da Android 10 le app non possono accenderli; i tasti aprono i pannelli di sistema.
- La luminosità cambia solo mentre il launcher è in primo piano.
- Icone: usate quelle delle app, ritagliate con angoli arrotondati (non il vero squircle Apple). Non vengono usati asset Apple.
- Il codice non è stato compilato in questo ambiente: alla prima build potrebbero servire piccoli ritocchi.

## Prossimi passi consigliati
1. Widget Android reali tramite `AppWidgetHost` nella pagina sinistra.
2. Notifiche sul blocco schermo con `NotificationListenerService`.
3. Meteo reale (API) e brano in riproduzione con `MediaSessionManager`.
4. Modalità modifica con "wiggle", cartelle e trascinamento delle icone.
