# White Noise Mixer (Android)

Native Android ambient sound & white noise mixer built in Java with Material Components, a Foreground Media Service, seamless gapless crossfading loops, a Sleep Timer with a 10-second gentle fadeout, and Tasker / Sleep alarm automation support.

---

## Features

- **31 Built-in Ambient Sounds** across nature, noise, and household categories with individual volume sliders.
- **13 Relaxation Presets** plus custom user-saved mixes.
- **Gapless Crossfading Engine** (`SmoothAudioTrack`) for continuous, click-free looping.
- **Sleep Timer** with preset durations, custom minute input, and a 10-second gentle volume fadeout.
- **Automation & Remote Control** via `TaskerReceiver` broadcasts and `WhiteNoiseControlProvider`, plus Sleep alarm audio pause/dismiss handling.
- **Bilingual UI** in English and Spanish (`values` / `values-es`).

---

## Presets / Relaxation Templates

The app includes all 10 authentic original presets plus 3 extended atmospheric mixes:

1. **Sailing Ship on Sea (`sailing_on_sea`):** Sailing Ship (80%) + Ocean Waves (65%) *(Your favorite!)*
2. **Summer Night (`summer_night`):** Summer Night (80%) + Cicadas (55%) + Frogs (35%) + Wind (30%)
3. **In Forest (`in_forest`):** Water Stream (75%) + Birds (65%) + Rustling Leaves (50%) + Wind (35%)
4. **Raining Frogs (`raining_frogs`):** Rain (75%) + Frogs (60%) + Thunder (40%)
5. **Raining (`rain`):** Rain (80%) + Thunder (50%) + Wind (35%)
6. **Fireplace (`fireplace`):** Fireplace (85%) + Wind (40%)
7. **Wind Chimes (`wind_chimes`):** Wind Chimes (80%) + Wind (55%) + Water Stream (45%)
8. **White Noise (`white_noise`):** White Noise (75%) + Fan (50%)
9. **Pink Noise (`pink_noise`):** Pink Noise (75%) + Fan (50%)
10. **Brown Noise (`brown_noise`):** Brown Noise (80%) + Fan (45%)
11. **Ocean Breeze (`sea_side`):** Ocean Waves (85%) + Wind (40%) + Whales (30%)
12. **Focus & Study (`deep_focus`):** Keyboard (60%) + Cafe Crowd (45%) + Rain (40%)
13. **Music Box & Lullaby (`lullaby`):** Music Box (85%) + Wind Chimes (40%)

---

## Tasker / Automation Intent Actions

The app exposes `com.agustin.whitenoise.receiver.TaskerReceiver` which accepts standard Android Broadcast Intents from Tasker, MacroDroid, or Automate.

### 1. Play / Resume
* **Action:** `com.agustin.whitenoise.ACTION_PLAY`
* Resumes currently active sounds. If no sounds are active, starts the **Sailing Ship on Sea** favorite preset.

### 2. Pause
* **Action:** `com.agustin.whitenoise.ACTION_PAUSE`
* Pauses all playing tracks without clearing the mix.

### 3. Stop
* **Action:** `com.agustin.whitenoise.ACTION_STOP`
* Stops playback completely, cancels any active sleep timer, and dismisses the foreground notification.

### 4. Toggle Play / Pause
* **Action:** `com.agustin.whitenoise.ACTION_TOGGLE`

### 5. Play Preset
* **Action:** `com.agustin.whitenoise.ACTION_PLAY_PRESET`
* **Extra (String):** `preset`
* Any preset ID listed above (e.g. `sailing_on_sea`, `summer_night`, `in_forest`, `raining_frogs`, etc.).

### 6. Control Individual Track & Volume
* **Action:** `com.agustin.whitenoise.ACTION_SET_TRACK`
* **Extra (String):** `track` &rarr; Sound ID (e.g., `sailing`, `sea_side`, `summer_night`, `rain`, etc.)
* **Extra (boolean):** `enabled` &rarr; `true` or `false`
* **Extra (int):** `volume` &rarr; `0` to `100` (e.g. `80`)

### 7. Sleep Timer
* **Action:** `com.agustin.whitenoise.ACTION_SET_TIMER`
* **Extra (int):** `minutes` &rarr; Number of minutes before auto-stopping (e.g. `30`)
* **Action:** `com.agustin.whitenoise.ACTION_CANCEL_TIMER`

---

## Sound Catalog (31 Tracks)

`sailing`, `sea_side`, `rain`, `thunder`, `wind`, `water_stream`, `fireplace`, `leaves`, `fountain`, `shower`, `summer_night`, `cicadas`, `bird`, `frogs`, `whales`, `wolves`, `white_noise`, `pink_noise`, `brown_noise`, `fan`, `music_box`, `piano`, `wind_chimes`, `train`, `car`, `crowd`, `keyboard`, `washing_machine`, `vacuum_cleaner`, `hair_dryer`, `shush`.

---

## Building from Source

1. Clone the repository and open it in Android Studio (or build via Gradle with JDK 17 and Android SDK 34).
2. Debug builds work out of the box with `./gradlew assembleDebug`.
3. For custom/release signing, configure `local.properties` (or environment variables `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`):
   ```properties
   signing.storeFile=keystore.jks
   signing.storePassword=your_store_password
   signing.keyAlias=your_key_alias
   signing.keyPassword=your_key_password
   ```
4. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## License

Licensed under the [MIT License](LICENSE).
