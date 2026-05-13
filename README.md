# Bolletta - monitor consumi elettrici

App Android (Java) per registrare letture mensili del contatore e stimare il totale bimestrale con prezzi configurabili.
Il modello di calcolo prende spunto dal file `Consumi elettrici 2026.ods`.

## Funzionalita

- Salvataggio letture mensili del contatore (formato mese `yyyy-MM`)
- Configurazione prezzi unitari in stile voci bolletta:
  - corrispettivo luce index (EUR/kWh)
  - contributo al consumo (EUR/kWh)
  - dispacciamento (EUR/kWh)
  - trasporto e gestione (EUR/kWh)
  - oneri ASOS (EUR/kWh)
  - oneri ARIM (EUR/kWh)
  - imposte (EUR/kWh)
  - perdite di rete (%)
  - IVA (%)
  - quota fissa bimestrale (EUR)
- Calcolo bimestrale con dettaglio Quadro A/B/C/D, totale IVA esclusa, IVA e totale finale
- Calcolo consumi su 2 mesi a partire da 3 letture consecutive:
  - mese inizio bimestre
  - mese intermedio
  - mese fine bimestre
- Promemoria in-app nell'ultimo giorno del mese se manca la lettura del mese corrente

I dati sono memorizzati localmente in `SharedPreferences`.

## Struttura principale

- `app/src/main/java/it/sdc/bolletta/MainActivity.java`: UI, salvataggio dati, validazioni e flusso app
- `app/src/main/java/it/sdc/bolletta/TariffConfig.java`: modello prezzi
- `app/src/main/java/it/sdc/bolletta/BillingCalculator.java`: logica di calcolo ispirata alle voci del foglio ODS
- `app/src/test/java/it/sdc/bolletta/ExampleUnitTest.java`: test unitari di calcolo

## Esecuzione test

```powershell
cd C:\Users\sergi\AndroidStudioProjects\Bolletta
.\gradlew.bat test
```

## Build debug APK

```powershell
cd C:\Users\sergi\AndroidStudioProjects\Bolletta
.\gradlew.bat assembleDebug
```

