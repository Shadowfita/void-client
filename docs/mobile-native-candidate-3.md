# Native mobile candidate 3 — gameplay stays inside the RuneScape client

This is a dedicated Jar Runner-compatible Java client, not an Android APK. It continues `feature/mobile-responsive-ui` from `5b14b647e7a38a605920e457fee67dc22111e763`. It changes native game input and rendering rather than relying on the former Actions / Camera / Text / Panels toolbar or its helper windows. Desktop startup and the game/server protocol remain separate and unchanged.

## Launch

Open the dedicated mobile JAR in the Java runtime that ran candidate 2. Leave application arguments empty, and do not override the main class with `Loader`. Its manifest selects `JarRunnerLauncher`; optional `--address HOST --port PORT --skip-setup` is retained. Enter your working endpoint in setup. The title says **native-mobile candidate 3**.

There is no bottom toolbar. The small corner icon/F10 opens **Client settings**, with display/input, gesture/accessibility preferences and diagnostics only. Normal gameplay no longer has links to the separate Panels, Text, Camera, game-tab shortcut or action helper windows. Older helper implementation classes and bridge APIs remain for compatibility/regression testing; they are not the standalone gameplay route. This is a settings menu, not a relocated gameplay toolbar.

The dedicated launcher defaults to touch gestures and full-height display fitting. Existing explicit input/display preferences are preserved. A runtime which supplies only final synthetic clicks rather than down/drag/up events cannot expose continuous gestures through this adapter. No raw Android multitouch capability is asserted.

## Native interaction changes

### Touch an item or world target, not a helper window

A normal tap retains the original default action. Hold an item, interface control or world target for its action list. The list is drawn in the game canvas with the active native renderer and enlarged copies of the game's own menu-font glyphs. It does not open a Swing action dialog, use a system font, or magnify the whole scene.

Rows wrap literal action/target labels, have a minimum 48 display/AWT-unit height where the viewport permits, and scroll within the screen. Swipe the list or use a mouse wheel; tap an option to select it. Cancel or an outside tap dismisses without clicking through into the world. Releasing the hold that opened the menu cannot also select a row. Moving across the edge of a neighbouring row, changing geometry, cancelling, or introducing a second pointer cancels the pending selection. Native action signatures and current interface state are still revalidated immediately before the existing native dispatcher runs.

Menus have no arbitrary reading timeout. Keyboard navigation (arrows, Home/End, Page Up/Down, Enter/Space and Escape) is retained without allowing those menu events to leak into game text or shortcuts. Java accessibility exposes the currently visible popup actions instead of the covered game controls. Whether Jar Runner exports Java semantics to Android TalkBack/Switch Access remains unverified.

The enlarged font is cached by renderer, font identity and scale. Native advances, kerning, bearings, palette/alpha and metrics are copied rather than mutating cache assets. Expanded glyph memory is bounded. If a scaled font is not yet available, the loaded native menu font is used; if no game menu font is available, only a visible, touch-dismissable X is published, not invisible action rows.

### Move item slots entirely in the original game

**Hold a supported item → choose “Move item: tap a destination slot” in the in-game menu → tap the destination in the original inventory.** No Panels view or separate dialog is involved. A contextual outline/hint identifies the selection; the in-canvas X cancels it.

The existing candidate-2 source/destination identity, item/count, ancestry and native-permission checks are retained. Final movement calls the game's original drag-completion path, not a new packet implementation or optimistic inventory edit. Live server round trips are not established by the offline dispatch tests.

### Camera and map controls

Scene dragging continues to rotate the camera through the native camera variables after the movement threshold; it does not also tap the world. Holding the scene now adds **Zoom camera in / Zoom camera out / Reset camera zoom** to its native action list. These are local viewing operations, restricted to an eligible scene and ordinary camera mode; they do not need the Camera helper.

Holding a live world-map widget supplies **Zoom map in / Zoom map out** instead. It revalidates that map widget and cannot fall through to world-camera movement after the map closes. Existing map panning/pinch paths are retained. Pinch is an enhancement only when the runtime actually delivers separate pointer identities; one-pointer menu alternatives do not depend on it.

### Direct native setting handles

Structurally supported graphics/audio slider handles in native groups 742/743 can be dragged directly. The recogniser captures the real native knob/track, preserves the original grab offset, and sends clamped parent-relative positions through the game's existing drag script. Total displacement is used; repeated pointer samples do not compound movement or emit duplicate setting updates.

The adapter requires a simple one-axis handle, the actual permitted drag parent, current geometry and an unchanged native listener. Changed scripts, hidden/replaced tracks, revoked flags, unexpected sibling controls and completion-dependent/complex drag semantics are rejected. This is not a guess at the cache's brightness or volume values. Holding a supported handle also offers **Decrease setting / Increase setting**, using the same native script as a non-drag alternative.

This does not certify every slider variant in the deployed cache. Unsupported controls keep their original behavior; broader control adapters remain work.

## Settings and rollback

The native HUD attachment, dialogue, equipment and inventory/grid adaptations from candidate 2 are retained. Their existing rollback switches are still available under Client settings → Accessibility and gestures. Action text size and high-contrast menu appearance are configurable independently of game-scene size. Native cache text elsewhere is not automatically scaled by this menu-font implementation.

The default game size remains 100%; saved choices are not silently reset. Use Client settings → Display and input to change that only when necessary. The ordinary desktop JAR still launches `Loader` without enabling the mobile host or action overlay.

## Verification contract

`NativeTouchRegression` tests popup dimensions at multiple viewport/native-resolution combinations, wrapping, original action indices, no click-through, opening-release suppression, stale generations, scrolling, keyboard handling, multi-pointer cancellation, missing-font recovery, queue coalescing, native glyph/metric immutability and allocation bounds. It also exercises guarded native slider scripts and menu nudges, listener/parent invalidation, scene/map zoom eligibility and keyboard focus-loss cleanup.

`NativeTouchCanvasSmoke` uses the actual heavyweight game Canvas, native software renderer, enlarged bitmap-font factory, AWT mouse listener, command queue, client tick and native dispatch seam. It checks actual pixels, clip restoration, a scrollable 14-entry list, exactly-once action selection and stale-action rejection without opening a Swing gameplay dialog. It runs both against development classes and against **only the obfuscated release JAR plus test output**, using the release mapping. The screenshot explicitly uses synthetic glyphs/action entries; it is not a live-cache or Android screenshot.

Existing core, native workflow, adversarial, responsive-host and desktop/browser compatibility regressions remain. Tests of retained helper classes are compatibility coverage, not a statement that gameplay still depends on them. Tests for removed menu links were replaced with assertions that settings contain no gameplay helper links and that returning to the game does not arm an unrelated operation.

## Remaining source work and device gates

**Text entry remains the existing native RuneScape field and keyboard path.** The removed Text helper is not replaced by another generic draft-insertion dialog. This build does not yet implement reliable native-field focus → Android soft-keyboard opening, IME composition, field-bound editing acknowledgements or keyboard-inset handling. Jar Runner's existing keyboard delivery can still be used, but automatic keyboard integration is unfinished. The remaining work needs a verified host capability/field integration rather than claiming a toolbar workaround meets that goal.

Individual chat/login/bank-search/quantity/trade screen adaptations, additional slider/control variants, independent font scaling outside the action menu, a separate software interface compositor and wider plugin migration are not completed here. The matching live cache/server, actual Android graphics/runtime behavior, physical touch/rotation/keyboard interaction, TalkBack export and sustained thermal/battery/performance behavior have not been exercised. Compilation and synthetic tests are not certification of those outcomes.
