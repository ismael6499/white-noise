# White Noise Mixer (Android)

Clean native Android audio mixer app built with pure Java, Material Components, Foreground Media Service, seamless continuous looping, Sleep Timer with 10-second gentle fadeout, and full Tasker / automation BroadcastReceiver integration.

---

## Tasker / Automation Intent Actions

The app exposes `com.agustin.whitenoise.receiver.TaskerReceiver` which accepts standard Android Broadcast Intents from Tasker, MacroDroid, or Automate.

### 1. Play / Resume
* **Action:** `com.agustin.whitenoise.ACTION_PLAY`
* Resumes currently active sounds. If no sounds are active, starts the **Sailing & Ocean** favorite preset.

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
* **Available Presets:**
  * `sailing_sea` &rarr; Sailing Ship (80%) + Ocean Waves (65%) *(Your favorite!)*
  * `cozy_rain` &rarr; Rain (75%) + Thunder (45%) + Wind (30%)
  * `deep_focus` &rarr; Brown Noise (60%) + Fan (50%)
  * `mystic_night` &rarr; Summer Night (70%) + Cicadas (40%) + Whales (35%)
  * `cabin_fire` &rarr; Fireplace (80%) + Wind (40%)

### 6. Control Individual Track & Volume
* **Action:** `com.agustin.whitenoise.ACTION_SET_TRACK`
* **Extra (String):** `track` &rarr; Sound ID (e.g., `sailing`, `sea_side`, `rain`, etc.)
* **Extra (boolean):** `enabled` &rarr; `true` or `false`
* **Extra (int):** `volume` &rarr; `0` to `100` (e.g. `80`)

### 7. Sleep Timer
* **Action:** `com.agustin.whitenoise.ACTION_SET_TIMER`
* **Extra (int):** `minutes` &rarr; Number of minutes before auto-stopping (e.g. `30`)
* **Action:** `com.agustin.whitenoise.ACTION_CANCEL_TIMER`

---

## Sound Catalog (31 Tracks)

`sailing`, `sea_side`, `rain`, `thunder`, `wind`, `water_stream`, `fireplace`, `leaves`, `fountain`, `shower`, `summer_night`, `cicadas`, `bird`, `frogs`, `whales`, `wolves`, `white_noise`, `pink_noise`, `brown_noise`, `fan`, `music_box`, `piano`, `wind_chimes`, `train`, `car`, `crowd`, `keyboard`, `washing_machine`, `vacuum_cleaner`, `hair_dryer`, `shush`.
