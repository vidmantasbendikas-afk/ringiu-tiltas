# Ringių tiltas (iPhone app)

Native SwiftUI app that shows the current condition of the Jūra bridge at
Ringiai, based on the live water level published by meteo.lt.

## How it works

On launch (and on every refresh) the app downloads
`https://www.meteo.lt/dabar/vandens-telkiniai/`, which embeds an hourly
snapshot series as JSON inside `const meteoHydroMaps = { ... }`. It extracts
that JSON, picks the newest timestamped snapshot, and reads `water.level`,
`water.temp` and `updatedAt` for the station `jura-ringiu-k`
(Jūra-Ringių k).

Example verified against the site on 2026-10-16:

```
Duomenys atnaujinti: 2026-10-16 15:00:00
Vandens lygis: 214.2 cm
Vandens temperatūra: 10.8 °C   ->  state: Šlapia (wet)
```

## Bridge states and animations

| Vandens lygis | State | Animation |
|---|---|---|
| > 215 cm | Užtvindytas (flooded) | Water flows over the roadway, fast high waves, floating logs, heavy rain, dark storm sky |
| 190–215 cm | Šlapia (wet) | Water rises to the deck underside, waves lap the bridge, spray droplets, light drizzle, drifting clouds |
| < 190 cm | Sausas tiltas (dry) | Sun with rotating rays, gliding birds, calm shimmering water far below the deck |

The waterline is continuous: moving from 190→215 cm visibly raises the water
toward the deck, and past 215 it climbs over the road. Unknown/missing data
shows a gray "Nėra duomenų" scene instead of guessing a state.

## Project layout

```
RingiuTiltas/
  RingiuTiltasApp.swift          app entry point
  Models/BridgeState.swift       thresholds, states, Reading model
  Services/MeteoService.swift    meteo.lt fetch + parse (level/temp/updated)
  ViewModels/BridgeViewModel.swift  loading state and state derivation
  Views/MainView.swift           status card, gauge, refresh button
  Views/BridgeScenery.swift      animated Canvas scene (all three states)
  Views/LevelGauge.swift         150–240 cm gauge with 190/215 markers
```

## Building

No external dependencies. Requires Xcode 15+ and iOS 16+.

### Option A – xcodegen (fastest)

```bash
brew install xcodegen
cd ringiu-tiltas
xcodegen generate      # creates RingiuTiltas.xcodeproj
open RingiuTiltas.xcodeproj
```

Then pick an iPhone simulator and press Run. (xcodegen is a Mac build tool;
generate the project once on a Mac, or on Linux with `mint`.)

### Option B – manually in Xcode

1. Xcode: File ▸ New ▸ Project ▸ iOS ▸ App.
2. Product name `RingiuTiltas`, Interface **SwiftUI**, Language **Swift**,
   uncheck Core Data / Tests.
3. Save it next to this folder, then delete the generated
   `ContentView.swift` and drag the `RingiuTiltas` folder from this repo into
   the project navigator (copy items, add to target).
4. In Target ▸ General set *Display Name* to `Ringių tiltas`.

### Android (Jetpack Compose port)

`android/` contains a Kotlin/Jetpack Compose port with the same thresholds,
animated Canvas scene and meteo.lt parsing. Open the `android/` folder in
Android Studio and press Run, or build from a terminal:

```bash
cd android
gradle wrapper --gradle-version 8.9   # once, creates ./gradlew
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (both installed automatically with
Android Studio).

## Customizing

- Thresholds / gauge range: `LevelThresholds` in `Models/BridgeState.swift`.
- Station: `MeteoService.stationSlug` (other slugs are listed on the page,
  e.g. `jura-pajurio-vms` for Jūra - Pajūrio VMS).
- Wave heights, creature counts, colors: `Views/BridgeScenery.swift`.

## Notes

- The reading is official-operational data ("Operatyvi hidrologijos tinklo
  informacija") from the Lithuanian Hydrometeorological Service; the app is
  an unofficial viewer.
- Pull down or press *Atnaujinti* to refresh; the page feed updates hourly.