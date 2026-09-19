# Android / Jar Runner mobile host — JR1

This is a dedicated desktop-Java compatibility-host build, **not an Android APK**. It is intended for the Android Java runtime where the previous Void JAR already starts. It does not use a browser, Tailscale or a WebSocket transport.

## Launch

1. Open `void-client-jarrunner-mobile.jar` in your working Jar Runner installation, using the same Java runtime that successfully ran the previous client.
2. Leave application arguments empty. The manifest starts `JarRunnerLauncher`, which enables mobile mode automatically. If the runtime insists on an explicit main class, enter `JarRunnerLauncher`, **not `Loader`**.
3. The setup screen lets you enter the reachable server hostname/IP and TCP port. Use the endpoint that already works for your client. The first-run default is inherited from `Loader`; no endpoint availability is asserted. Select **Start client**.
4. Look for **Actions, Camera, Text, Inspect, Display** in the bottom toolbar. The title is `Void — Android / Jar Runner JR1`.

No custom-arguments purchase is needed just to activate this build. Optional command-line arguments remain available: `--address HOST --port PORT --skip-setup`. `--help` prints the version and exits without a graphical display. Do not enable browser mode for this JAR.

## Display and input

**Fit Java-visible display** fills the usable AWT-reported display, accounting for AWT screen insets. The old fixed 844 × 560 request is removed. The dedicated host is borderless. Actual content-resize events update the published viewport and cancel pending gestures; a lightweight timer follows changes in the display bounds reported by Java.

This cannot discover Android geometry hidden by the runtime. If Jar Runner reports a fixed virtual desktop, the client sees that desktop. It cannot force Jar Runner to expose Android's physical display, density, keyboard area, cutouts or rotation events.

**Manual window width / height** is available with Fit unchecked. These are Java/AWT units, not guaranteed Android pixels or dp. The window is constrained to the Java-visible display. This is a client-window override, not a command to change Jar Runner's virtual desktop. The actual available canvas drives the existing client resize/layout path.

**Game interface size** defaults to 125% and offers 100–200%. A renderer supporting native interface scaling scales the UI separately. With the software renderer, the fallback scales the **whole game canvas**, including the scene, through the client's existing stretched-rendering and input-transform paths. It is not advertised as software-rendered UI-only scaling. Try 100% when too little interface content fits, or a higher value when controls appear too small. Display diagnostics identify the active scaling mode.

**Toolbar / dialog size** is independent of the game size. Auto uses the actual host size, not a larger virtual-desktop size when a manual window is selected. Controls have a baseline 48-AWT-unit preferred height. These are not guaranteed CSS pixels or Android dp. Oversized tool settings are capped where necessary to keep the Display button accessible. The toolbar wraps to multiple rows on narrow windows. Settings have a scrollable body and fixed Apply/Reset/Close actions.

**Touch gestures** is the default single-pointer adapter for runtimes that expose touch as mouse press/drag/release. **Ordinary mouse** uses the existing mouse path instead. Select this when Jar Runner itself manages gestures or supplies a virtual cursor. The Actions button still arms target selection without first executing a default click in either profile. This does not create raw multitouch from one virtual mouse; Camera provides an explicit alternative to pinch/drag operations.

**Reset** restores fitting, touch input, 100% game size and automatic toolbar sizing. Settings are applied immediately and saved separately from desktop preferences. If saving fails, the change remains active for that session and the status explains the failure.

## Diagnostics and storage

Display shows Java display bounds, host size, rendering size, logical UI size, scaling mode and input profile. These values are useful when distinguishing a large virtual desktop from an actual client sizing error.

Mobile settings and the server endpoint are saved to `${user.home}/.void-client/mobile-host.properties` within the runtime's filesystem. No game username, password or chat text is saved by this settings component. Desktop window preferences are not overwritten.

The normal release JAR retains `Loader` as its entry point. Browser mode retains explicit HTML-driven sizing and has no native toolbar. Native host dialogs consume game mouse input while open, and resizing or changing input/scaling cancels active gestures.

## Build and verify

Use Java 8 for the repository's Gradle build:

```sh
bash ./gradlew :client:test :client:proguardJar :client:jarRunnerJar
xvfb-run -a -s '-screen 0 1920x1200x24' bash ./gradlew :client:standaloneHostTest
xvfb-run -a -s '-screen 0 390x844x24' bash ./gradlew :client:standaloneReleaseTest
node --test web/mobile/test/*.test.mjs
```

The dedicated artifact is `client/build/libs/void-client-jarrunner-mobile.jar`. It repackages the ProGuard release payload with a kept `JarRunnerLauncher` entry point; it is not a renamed old preview. Reports are under `client/build/reports/mobile/`.

`StandaloneMobileRegression` exercises real Swing controls and resizing under Xvfb with injected AWT display bounds, input ownership, persistence, native mouse routing and the actual software renderer's scaling/coordinate mathematics. It does not load a live game scene. `ReleasedJarRunnerSmoke` runs with test output plus **only the obfuscated dedicated JAR**, opens the real setup dialog and cancels before networking. The existing core/native/browser-bridge checks remain enabled. The verification workflow keeps read-only repository permissions.

## Remaining limitations

Physical Android Jar Runner execution, runtime keyboard integration, delivery of phone rotation events, audio, cache/server gameplay and sustained performance still require device testing. Successful desktop-host and release tests are not evidence those Android behaviours work.

This fixes standalone launch mode, display fitting, control sizing and runtime input selection. It does **not** add cache-specific mobile redesigns of inventory, bank, chat, trade or dialogue, nor an Android accessibility/semantic bridge. Those remain separate work. The existing responsive-layout fallback and action adapters are reused rather than replacing game protocols.
