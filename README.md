# Mi — Inputs

[Русская версия](README.ru.md)

A small, root-free Android TV helper that turns the **Mi** button on a Xiaomi
remote into an **input-source picker** with human-readable names.

**Xiaomi only.** The Mi button exists only on Xiaomi remotes, so the app does
not work on TVs from other manufacturers. Tested on Xiaomi MiTV-MSSP0 (device
`amelie`, Android 9, build PI.4072) with PatchWall; other Xiaomi models may
work, but have not been tested — see [Scope](#scope).

No root, no bootloader unlock, no custom firmware, no factory reset — and
**no internet permission**. Everything can be rolled back.

## Features

- Intercepts the Mi key before the system handles it, so Live TV or PatchWall
  does not open; shows its own popup instead.
- Lists real inputs from the Android TV Input Framework: HDMI 1, HDMI 2 (ARC),
  AV, TV…
- Friendly names: edit any input, hide unused ones, reset to the system name.
- Re-detect the key — useful when another remote sends a different code.
- "System list" fallback that opens the vendor's own input popup.
- **Ports & inputs** diagnostics screen: what was found, how each HDMI port was
  numbered, and tap-to-test switching.
- **Pause interception**: Mi works as usual again; one tap to resume.
- English and Russian UI, English by default; language switcher in the header.
- No network access, no analytics; all settings are stored on the device.

## Tested on

| | |
|---|---|
| TV | Xiaomi MiTV-MSSP0 (device `amelie`) |
| Android | 9 |
| Build | PI.4072 (`Xiaomi/amelie/amelie:9/PI/4072:user/release-keys`) |
| Launcher | PatchWall |

## Requirements

- A Xiaomi TV with a Mi button on the remote (tested on MiTV-MSSP0 with
  PatchWall, Android 9). TVs from other manufacturers will not work.
- A way to sideload an APK (`adb install` or a file manager).
- Accessibility service enabled (on some firmware the accessibility screen is
  empty, so ADB is needed once).
- No root.

## Install

1. Download `Mi-Inputs-2.5.apk` from the **Releases** page of this repository
   (or build it yourself, see below).
2. Install it:

   ```bat
   adb install -r Mi-Inputs-2.5.apk
   ```

   > Play Protect may warn about an app installed outside Google Play. This is
   > expected for sideloaded APKs.

3. Open **Mi — Inputs** from the TV app list.
4. Enable the service:
   - **Service is on** — done.
   - **Service is off** — tap the row and enable **Mi — Inputs** in
     Accessibility.
   - If the accessibility list is empty (common on this firmware), use
     **How to enable** in the app or the ADB commands below.

### Enabling via ADB

```bat
adb shell settings put secure enabled_accessibility_services "com.tvsetup.mibutton/com.tvsetup.mibutton.MiKeyService"
adb shell settings put secure accessibility_enabled 1
```

If other accessibility services are already enabled, keep them and append ours
with a colon:

```bat
adb shell settings put secure enabled_accessibility_services "existing.pkg/existing.Service:com.tvsetup.mibutton/com.tvsetup.mibutton.MiKeyService"
adb shell settings put secure accessibility_enabled 1
```

Check that the service is bound:

```bat
adb shell dumpsys accessibility | findstr "Service["
```

You should see `Service[label=Mi — Inputs ...]`.

## First run

1. Open the app; the status should say **Service is on**.
2. Press **Mi** on the remote — the input list should appear.
3. If Live TV or PatchWall opens instead:
   - make sure interception is not paused (see below);
   - tap **Mi key: 191** and press Mi once; the app will remember the code.
4. Rename inputs: **INPUTS** → pick an input → preset name or **Reset to system
   name**.

## Pause

To let Mi behave as before without uninstalling the app or disabling the
service:

**STATUS → Mi interception: On** → tap.

The row becomes **Mi interception: Off**, the status shows "Interception is
paused", and Mi opens the stock Live TV or PatchWall again. Tap once more to
resume. The setting survives reboots and app restarts; the accessibility
service stays enabled.

## Language

The app name is always **Mi — Inputs** (launcher, Recents, accessibility list).
The UI is **English by default**, regardless of the TV language.

The switcher is in the header, to the right of the title: a small button with
the current language (`English` / `Русский`). Only these two languages are
available. The choice applies to every screen (settings, Ports & inputs, popup,
toasts) and is stored on the device. Preset input names are inserted in the
language that was active when you renamed the input.

## Inputs and ports

**Ports & inputs** shows what the app actually sees:

```
Found 5: HDMI 3 · AV 1 · TV 1

HDMI 1
HDMI · port HDMI 1 (from system label)
system label: HDMI 1
com.mediatek.tvinput/.hdmi.HDMIInputService/HW2
```

HDMI port number priority:

1. digits from the system label (`HDMI 2(ARC)` → 2);
2. digits from the input id (e.g. `HDMI2InputService`);
3. the next free number in hardware-port order.

Tap an input there to switch to it — handy for verifying each port.

Signal state is not available: `TvInputManager.getInputState()` and
`TvInputInfo.getHdmiDeviceInfo()` are hidden APIs. Check the picture after
switching.

## Names

Default names: `HDMI N` (plus `(ARC)` when present), `AV`, `TV`, `PC (VGA)`.
Presets: HDMI 1…4, AV, TV, PC, USB, Media player, Game console. **Reset to
system name** clears a custom name. Inputs can be hidden from the list.

## How it works

| | |
|---|---|
| Package | `com.tvsetup.mibutton` |
| Service | `com.tvsetup.mibutton/.MiKeyService` |
| Popup | `com.tvsetup.mibutton/.InputPopupActivity` |
| Default key | `191` (KEYCODE_BUTTON_2) |
| Inputs | `android.media.tv.TvInputManager` |
| Switching | `Intent.ACTION_VIEW` + `TvContract.buildChannelUriForPassthroughInput()` |
| Internet | not used (no `INTERNET` permission) |

The accessibility service requests `flagRequestFilterKeyEvents`, receives the
key before the system, and returns `true` to consume it. The app then shows its
popup and, on selection, sends the standard `ACTION_VIEW` passthrough intent —
the same mechanism the stock input list uses.

## Scope

The app is made for **Xiaomi TVs**: the Mi button exists only on Xiaomi remotes,
so it **does not work on TVs from other manufacturers** — there is no Mi button
to intercept.

Among Xiaomi models, only **MiTV-MSSP0** (device `amelie`, Android 9, build
PI.4072) with **PatchWall** has been tested. Other Xiaomi models may work, but
have not been tested and are not supported.

For another Xiaomi model to work, it needs:

- the Mi button to reach Android as a key event — on some remotes the button is
  sent directly over IR to a closed service, and then it cannot be intercepted;
- the Android TV Input Framework exposed by the firmware;
- on Android 13+, sideloaded apps allowed in "Restricted settings" before the
  accessibility service can be enabled.

The key can be re-detected on any Xiaomi remote. If the system does not expose
its inputs, set **Input list** to **Built-in TV list**; the app will open the
vendor's popup.

## Build from source

Requirements: Windows, Python 3, JDK 17, Android build-tools and `android.jar`
(API 28).

```bat
powershell -ExecutionPolicy Bypass -File build\build.ps1
```

The script looks for tools in this order: `-Jdk`/`-Tools`/`-PlatformJar` →
`JAVA_HOME`/`ANDROID_HOME` → `build\tools`.

- `-Jdk <dir>` — JDK (must contain `bin\javac.exe`)
- `-Tools <dir>` — Android build-tools (must contain `aapt2.exe`)
- `-PlatformJar <file>` — `android.jar` from platform API 28

On the first build a signing key is generated at `build\mibutton.keystore`
(alias `mibutton`, password `mibutton123`; override with `-StorePass`).
**Keep it**: Android only accepts updates signed with the same key. The key used
for the release APK is not published, so a build made with your own key cannot
update the released APK — uninstall the old version first.

Result: `dist\Mi-Inputs-2.5.apk`.

```
app\                 Java sources, manifest, resources
build\build.ps1      build script (aapt2 → javac → d8 → zipalign → apksigner)
build\inject.py      packs classes.dex into the APK
build\tools\         optional local JDK/build-tools/android.jar (not in the repo)
dist\                build output (not in the repo)
```

## Rollback

Keep only the other accessibility service (example):

```bat
adb shell settings put secure enabled_accessibility_services "com.spocky.projengmenu/com.spocky.projengmenu.services.ProjectivyAccessibilityService"
adb shell settings put secure accessibility_enabled 1
```

Full removal:

```bat
adb uninstall com.tvsetup.mibutton
adb shell settings put secure enabled_accessibility_services "com.spocky.projengmenu/com.spocky.projengmenu.services.ProjectivyAccessibilityService"
adb shell settings put secure accessibility_enabled 1
```

If the service list becomes empty, the firmware may reset `accessibility_enabled`
to `0` — that is expected.

## Troubleshooting

| Symptom | What to do |
|---|---|
| Mi opens Live TV or PatchWall | Check **Mi interception** is On; then tap **Mi key** and press Mi |
| Popup does not appear | Check **Service is on**; reboot the TV |
| Popup appears, input does not switch | **Input list** → **Built-in TV list** |
| An input is missing | **Refresh list**; make sure it is not hidden |
| Stopped working after a firmware update | Check the service and re-detect the key |

Logs: `adb logcat -s MiKeyService:I MiInputs:W`.

## Privacy

The app has no `INTERNET` permission. It does not collect or send anything.
Settings (key code, names, language, pause) are stored in SharedPreferences on
the device. The accessibility service only filters key events to find the Mi
key; it does not read screen content or text.

## Disclaimer

Not affiliated with Xiaomi. Use at your own risk: the app only changes which
screen opens on a key press and can be removed at any time. Android is a
trademark of Google LLC.

## License

MIT — see [LICENSE](LICENSE).
