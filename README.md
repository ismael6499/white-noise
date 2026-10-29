# White Noise Mixer (Android)

Clean native Android audio mixer app built with pure Java, Material Components, Foreground Media Service, seamless continuous looping, Sleep Timer with 10-second gentle fadeout, and full Tasker / automation BroadcastReceiver integration.

---

## Presets / Relaxation Templates

The app includes all 10 authentic original presets plus 3 extended atmospheric mixes:

1. **Sailing Ship on Sea (`sailing_on_sea`):** Sailing Ship (80%) + Ocean Waves (65%) *(Your favorite!)*
2. **Summer Night (`summer_night`):** Summer Night (80%) + Cicadas (50%) + Wind (25%)
3. **In Forest (`in_forest`):** Water Stream (75%) + Birds (60%) + Rustling Leaves (45%)
4. **Raining Frogs (`raining_frogs`):** Rain (75%) + Frogs (60%)
5. **Raining (`rain`):** Rain (80%) + Thunder (40%) + Wind (30%)
6. **Fireplace (`fireplace`):** Fireplace (85%) + Wind (30%)
7. **Wind Chimes (`wind_chimes`):** Wind Chimes (80%) + Wind (40%)
8. **White Noise (`white_noise`):** Pure White Noise (85%)
9. **Pink Noise (`pink_noise`):** Pure Pink Noise (85%)
10. **Brown Noise (`brown_noise`):** Pure Brown Noise (85%)
11. **Ocean Breeze (`sea_side`):** Ocean Waves (85%) + Wind (35%)
12. **Focus & Study (`deep_focus`):** Keyboard (65%) + Cafe Crowd (40%) + Rain (35%)
13. **Music Box (`lullaby`):** Pure Music Box (85%)

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
