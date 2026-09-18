# Mobile interface foundation

This branch adds an **opt-in mobile presentation/input path** to the existing Java 8 client. It does not replace the desktop RuneLite client or implement a second game protocol.

## What is implemented

- `--mobile`: resizable compact Swing host with a four-button dock and touch-sized action, camera and text dialogs. Normal startup still uses RuneLite.
- `--touch-mouse`: explicit left-mouse touch emulation for local testing. Ordinary desktop input is unchanged when mobile mode is disabled.
- `web/mobile`: CheerpJ 4.3 library-mode browser host. Portrait and landscape docks, safe-area padding, visual-viewport resizing, native HTML dialogs, text editing, fullscreen request and optional 100–200% whole-canvas magnification.
- One published viewport transform for host, native canvas and logical interface coordinates. Coordinates outside the content are rejected, not clamped into edge actions. Pointer revisions cancel stale gestures after resizing or root/state changes.
- Deferred tap; long press for native alternative actions; panel panning; scene camera drag/pinch; explicit camera buttons. Scroll/camera/long-press gestures do not also emit a default tap. Gesture ownership persists until completion/cancellation.
- Bounded, ordered queues on both sides of the asynchronous Java bridge. Only consecutive moves from the same pointer/revision coalesce. Overflow cancels rather than losing a release event.
- **Actions**, then a target tap, opens alternatives without a timed hold or a preliminary default action. The mode is one-shot and cancellation disarms it.
- Touch-sized context menus use the existing native action formatter and dispatcher. Selection is matched against freshly rebuilt native action candidates, including action arguments, target, operation number and labels. Menus expire after 15 seconds.
- Oversized fixed root containers receive a scrollable fallback. Authored raw dimensions and script-controlled content extents remain intact. Child layout uses the authored content extent; visible size and position are constrained before resize callbacks run. This is **not** a hand-designed bank/inventory/trade layout.
- Buffered text insertion uses existing client character validation and native typed-key events. It never synthesizes Enter from pasted text. A separate text-focus generation survives keyboard-only viewport resizing. Native browser key events are suppressed to avoid duplicate delivery; use the Text editor (and camera arrow-key bindings) in the browser.
- Widget inspection captures the native traversal's visible, clipped geometry rather than inventing interface IDs. Inspection does not export chat or editable-field text.
- Mobile mode disables the inventory-grid overlay that assumes fixed desktop slot positions.
- Browser mode selects the software renderer. Browser TCP/cache transport is **not** provided by this change.

## Run the native preview

Build with Java 8:

```sh
./gradlew :client:test :client:proguardJar
java -jar client/build/libs/void-client-0.3.0-release.jar --mobile --touch-mouse
```

The preview uses the classic client host, not the RuneLite plugin sidebar. Omit both flags to launch the unchanged desktop shell. `-Dvoid.mobile=true` also enables the mobile host. Gesture configuration is session-only:

```sh
java -Dvoid.mobile.holdMillis=650 -Dvoid.mobile.slop=12 -jar client/build/libs/void-client-0.3.0-release.jar --mobile --touch-mouse
```

`holdMillis` is bounded to 250–1500 and movement slop to 4–40 display units. Drag an eligible scrolling panel to pan its contents; drag the scene to rotate the camera. A moved finger over a non-scrollable control cancels the tap instead of dragging an item unexpectedly.

## Run the browser host

1. Build the release JAR and copy it into the web server's document root.
2. Serve the contents of `web/mobile` from that root (HTTPS for deployment). The default `/app/void-client-0.3.0-release.jar` path is a CheerpJ filesystem path; `/app/` maps to files on the web server, not a directory to create in the JVM.
3. Configure `web/mobile/config.js` with the correct JAR, server address/port and deployment-specific CheerpJ runtime/network options.
4. Open the page and press **Start client**. `ready` means the input/viewport adapter is running, **not** that cache loading or login succeeded.

A static web server alone does not make the client's TCP connections browser-compatible. Configure a supported CheerpJ networking route before expecting cache/server access. Never commit reusable network authentication keys or account credentials into `config.js`. This implementation neither changes the server protocol nor bypasses server authentication.

The software renderer is a conservative browser starting point; native-library loading elsewhere in the legacy client, cache access, sound and complete CheerpJ execution still require integration testing. The runtime loader is pinned to CheerpJ 4.3. Review the runtime provider's deployment/licensing requirements before publishing a service.

### Text entry

First focus a game field, then open **Text**. Compose/edit/paste locally and choose **Insert text**. The browser keeps the draft if the client rejects the insertion. Enter, Backspace, Next field and Escape are separate controls. After an intentional field change, explicitly choose **Use current field** or reopen Text. Closing the editor clears its draft.

Text is queued in bounded batches through the original key path; the original field's length/character rules still apply. A queued acknowledgement is not proof that a particular field accepted every character. Inspect that field before submitting. Unsupported characters are rejected as a whole; this does not add Unicode protocol support.

### Magnification

**More → Game magnification** enlarges the whole legacy canvas while keeping the HTML tools at normal size. This is an assistive fallback, not independent scene/UI rendering resolution. Pointer coordinates are transformed by the same published viewport. It may reduce the visible content area; pan oversized panels to reach their contents.

## Internal contract

`com.voidclient.mobile.MobileBridge` is the only cross-thread/public producer API. All game-state mutation remains in `MobileRuntime` on the client thread. The native adapter is hooked into mouse sampling, visible widget traversal, native layout sizing/positioning, and the post-build menu-candidate phase.

- Pointer coordinates: display-space coordinates relative to the host, not screen coordinates or device pixels. Safe-area reservation belongs to the browser shell; it is not added twice in Java.
- Pointer/context/camera commands carry `snapshot.revision`.
- Menu selections carry `snapshot.menuId`, plus the entry index.
- Text and explicit key commands carry `snapshot.textSession`, not the viewport revision.
- `insertText` returns an acknowledgement ID. Match it to `textAck` / `textAccepted`; do not log the text.
- Only `down`, `move`, `up` and coordinate-based `context` are accepted pointer phases.
- `cancel`, viewport/root changes, backgrounding, pointer cancellation and lost capture clear pending interaction without emitting a release-to-activate/drop.

The mobile helpers are named classes. Existing decompiled files contain focused integration hooks rather than duplicate action or packet logic. Browser-facing symbols are retained in the ProGuard release.

## Verification

```sh
# Full Java 8 native-hook and core tests, followed by release packaging
./gradlew :client:test :client:proguardJar
# Standalone core tests without Gradle/dependency downloads
bash tools/test-mobile-core.sh
# Browser queue/coordinate unit tests (Node 22)
node --test web/mobile/test/*.test.mjs
# Optional real Chromium shell test with a mock Java bridge, not a live game
python tools/mobile-browser-smoke.py
```

Native reports are written to `client/build/reports/mobile/regression.txt`. CI also verifies that the public entry/bridge classes survive obfuscation. Browser-shell tests explicitly use a mock Java bridge and are not evidence of live CheerpJ/game compatibility.

## Work still required before a mobile release

| Area | Remaining work |
| --- | --- |
| Cache-specific layouts | Capture real interface IDs, then adapt inventory/equipment, chat, minimap, bank/shop, trade, dialogue and navigation individually. The fallback preserves access; it does not provide those bespoke layouts. |
| Item rearrangement | Add a verified tap-select/tap-destination alternative to native item dragging. This foundation intentionally avoids synthesizing unverified item-move actions. |
| Plugins | Migrate/test interactive RuneLite panels and overlays against real widget geometry. The mobile preview currently uses the classic host. |
| Accessibility | Audit labels, text size, contrast and focus in live interfaces. The game canvas is not a screen-reader semantic tree; larger HTML controls do not establish full accessibility conformance. |
| Browser deployment | Verify cache/TCP transport, remaining native dependencies, audio, login and reconnection with the actual runtime/server. |
| Device QA | Test Android Chrome and iOS Safari, portrait/landscape, keyboard-visible states, safe areas, multi-touch and hardware inputs on physical devices. |
| Performance | Profile frame time, memory and sustained thermal behaviour before choosing mobile quality defaults. |

The release gate is a real-device login → movement → interaction → inventory → banking → chat workflow, plus desktop fixed/resizable regression. Automated tests alone do not establish that gate.

## Primary runtime references

- https://cheerpj.com/docs/guides/library-mode
- https://cheerpj.com/docs/reference/cheerpjInit
- https://cheerpj.com/docs/reference/cheerpjCreateDisplay
- https://cheerpj.com/docs/guides/networking

### Implementation-time verification

The dependency-free core passed 1,353 assertions and the browser queue passed eight Node tests. The actual Chromium shell passed 68 checks at 320×568, 390×844, 844×390 and 1024×768 display pixels using a **mock Java bridge**. These checks cover host behaviour, not live CheerpJ networking/rendering or actual gameplay. Full native-hook and release validation is performed by the branch workflow; consult the exact commit's run before treating a build as verified.
