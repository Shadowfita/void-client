# Jar Runner mobile: JR2–JR5 implementation candidate 1

This is a dedicated Java compatibility-host build, **not an Android APK and not a completed JR5 device-certified release**. Production code has been added across correctness, responsive interfaces, native workflows and accessibility/release infrastructure. The acceptance gates still requiring an actual matching cache, server and Android host are stated below. No browser/VPN/transport changes are required.

## Open the correct build

Open `void-client-jarrunner-mobile.jar` in the Jar Runner runtime that previously ran your client. Leave application arguments empty. Do not override its main class with `Loader`; the manifest selects `JarRunnerLauncher`. Enter the working server address/port in setup. Optional `--address HOST --port PORT --skip-setup` remains available.

Look for **Actions / Camera / Text / Panels / More**, and the **JR2–JR5 candidate** title. Display moved to **More → Display and input**. The ordinary release JAR still starts the desktop `Loader`. Mobile preferences remain separate.

## What to use

**Panels** opens a full-window, responsive view of the interfaces currently exposed by the game renderer. Select an open interface in the group selector. Items use measured, wrapping cells; actions retain their native item, slot and widget identity. Choose an item's native actions rather than relying on a tiny desktop menu. Press-only native controls have an explicit Activate button; complex hold/release controls remain in the original game rather than being approximated.

The item filter is **local to this panel**, not a server bank-search command. Large lists page in batches of 96 while preserving slot identities. Previous/Next changes item pages; Up/Down scrolls the host panel. The native bank search action remains available when the live widget exposes it. `Original panel up/down` controls provide a way to scroll native ancestors when a legacy layout has more content. Close Panels to return to the game.

**Move** implements an explicit source/destination operation. Select Move on a supported item, then choose a destination in the same native inventory. Empty slots can be shown. Cancellation, item/quantity changes, missing parents and revoked permissions stop the operation. The original drag-completion script and packet code performs the move; the panel does not modify the inventory optimistically. Native flags determine support. Ordinary pointer dragging remains available through the legacy input profile, not a newly promised touch-drag workflow.

**Native item grids** also reflow inside the original game for structurally supported cache widgets. The guard requires an identified item container, fixed-size sprite children and consistent dynamic slot identities. It preserves script-authored order, original coordinates and native slot numbers; it increases touch size and uses vertical scrolling. Mixed/unknown structures keep the legacy layout. Disable this in **More → Accessibility and gestures** to roll back without replacing the JAR. Metadata is a hint, not a matching-cache certification.

**Text** uses an explicit draft and receipts. Insert sends once; Next/Enter/Backspace/Escape are separate commands. The editor adopts only the transition acknowledged for its own request. Unsupported text, stale sessions and queue failures do not erase the draft. **Delivered means consumed by the game's keyboard path, not proven accepted by a particular cache-script field.** The draft therefore remains until you clear/edit it, and duplicate insertion is disabled. Focus the intended native game field first. Fully field-bound login/chat/search/quantity adapters and Android composition/insets are still outstanding.

**Camera** remains a one-pointer alternative to gestures. Map controls target a live map widget and cannot become world camera movement. Pinch is optional where a host delivers separate pointers; one virtual mouse cannot supply raw Android multitouch. **More → Accessibility and gestures** configures hold delay, movement threshold, scroll speed, camera sensitivity/inversion/reduced movement, panel text size, contrast, minimum cell width and layout profile. Handedness changes toolbar order on next launch.

## Correctness changes

The seven audited cases have corrected regression coverage: same-target release; occlusion and instance checks; ancestor-only scrolling; fractional/direction-invariant scrolling; editor rebinding after acknowledged Next; retained rejected drafts; and uniform software magnification. Script movement between painting and release also cancels stale-coordinate clicks.

Action menus have no 15-second reading deadline. Selection rebuilds native actions and validates the associated group state, including loaded offscreen children, item counts and changed widget instances. Invalid choices ask for a current selection. Native operation permissions are never granted by the metadata catalogue. Press-only controls use their already-declared native script; item moves use the existing drag completion. No game packet implementation was duplicated.

## Rendering and layout boundaries

The committed `UiFrameSnapshot` is built from actual interface rendering traversal: widget identities, parents, effective positions, clipping, layers, roles, available actions and resolved displayed text. Input routing and host presentation consume the same native identities. A failed/truncated render fails closed. The registry includes 408 public interface-group names and 4,189 component mappings from public `GregHib/void` commit `d165e5be63e3232f64f08230448089a454bd35e6`, with per-source hashes and its BSD licence. No private server source or cache binaries are included.

Responsive Panels is an **alternative presentation**, not a claim that every cache screen has been hand-redesigned. Inventory/bank/shop grids, equipment/list controls, dialogue text/choices, map alternatives and script-only buttons use the shared native model. Three-dimensional equipment previews and special scene/minimap rendering remain in the original game. Already-rendered software item sprites can be reused in Panels without GPU readback; unavailable icons use item labels. Unknown screens have a conservative live-control list. Widgets inside completely unpainted subtrees still require their native parent to be revealed; the catalogue does not invent hidden controls.

Panel text size is independent of game scaling. It does not change the cache's native font resources. Software game magnification remains whole-canvas, now uniformly limited by raster minima; native interface-only scaling is retained only for supporting renderers. A separate software UI compositor/native font-scale rewrite was not implemented. AWT units are not asserted to be Android dp, and Java-visible display fitting cannot discover geometry the host does not expose.

## Accessibility and diagnostics

Host controls expose labels, wrapping text, focus, high-contrast options and non-drag alternatives. The game canvas now supplies a Java `AccessibleContext` virtual tree with roles, text/item descriptions, bounds and guarded native actions. **This is not evidence that Jar Runner exports it to Android TalkBack or Switch Access.** That host capability must be tested. Android system Back, raw multitouch and IME composition/insets remain host-dependent; host Escape/Close is implemented through a surface stack with focus restoration.

More provides runtime capabilities, bounded interface-paint/queue-age/Java-heap metrics and manual catalogue export. Export contains IDs, geometry, roles, counts and SHA-256 digests of loaded interface definitions. It does not export screenshots, field text, chat, passwords, item names, quantities or raw cache bytes. A digest of the definitions observed so far is not a fingerprint of the entire cache. Metrics are CPU/Java observations, not GPU/thermal/Android memory measurements.

## Implementation/acceptance register

“Implemented” here means code plus stated automated fixtures; it does not mean live device certification. “Partial” identifies remaining source/design work, not merely a missing test.

| Plan ID | Candidate status | Evidence / remaining work |
|---|---|---|
| MOB-01 | Implemented | Same-target and stale-coordinate cancellation; boundary fixtures. |
| MOB-02 | Implemented, device gate open | Native instance/occlusion/quantity checks and loaded-group revalidation; live attached-panel scenarios unverified. |
| MOB-03 | Implemented | Topmost target then actual ancestor; unrelated siblings cannot scroll. |
| MOB-04 | Implemented | Fractional residuals and symmetric event partitioning tests. |
| MOB-05 | Partial | Acknowledged ordered receipts and A5/A6 fixes; cache-field identity, partial application and IME semantics not yet established. |
| MOB-06 | Implemented | Uniform effective software scale plus existing renderer/input paths; real game visual sign-off open. |
| MOB-07 | Implemented | Durable menus, fresh native candidates and offscreen group-state guards. |
| MOB-08 | Implemented diagnostics, device gate open | Capabilities/units and queue/render metrics; no actual Android capability matrix yet. |
| MOB-09 | Partial | Pinned public catalogue plus redacted live geometry/definition digests; full matching-cache coverage/fingerprint not established. |
| MOB-10 | Implemented | Immutable render-traversal model and typed targets; synthetic native/paint fixtures. |
| MOB-11 | Partial | Guarded native inventory reflow, public metadata and conservative responsive fallback; not a cache-certified adapter for each screen. |
| MOB-12 | Partial | Independently measured/wrapped host panel text and item labels; cache-native fonts unchanged. |
| MOB-13 | Deferred source work | Existing software raster retained; no independent software UI compositor or full model/text rendering comparison. |
| MOB-14 | Implemented host coordinator | Nested host surfaces, Escape/Close/focus and game input isolation; Android system Back not exposed. |
| MOB-15 | Implemented | Actual Swing swipe tests disarm buttons; wrapped action rows and page alternatives. |
| MOB-16 | Partial | Panels/navigation, press-only native controls, mode banner and handedness; no fully redesigned cache-native HUD/minimap/chat arrangement. |
| MOB-17 | Partial | Native supported item grids plus responsive items/equipment actions; original 3D previews and cache-specific visual certification remain. |
| MOB-18 | Implemented non-drag path | Two-tap Move uses original completion; fresh permissions/source/destination tests. Native server round-trip and optional touch drag not certified. |
| MOB-19 | Partial | Generic acknowledged draft workflow; field-aware login/chat/search/quantity adapters still need matching script focus contracts. |
| MOB-20 | Partial | Responsive native bank/shop/storage controls, 96-item pages and local filtering; no live bank/search/server-update workflow certification. |
| MOB-21 | Partial | Measured native dialogue text/choices and activation; all production/cache layouts not certified. |
| MOB-22 | Implemented guards, live gate open | Any loaded same-group item/text/instance change invalidates pending action. Live two-stage trade scenarios unverified. |
| MOB-23 | Implemented | Map pan/zoom separate from camera and one-pointer controls; host multitouch not claimed. |
| MOB-24 | Implemented preferences/cancellation | Bounded hold/slop/sensitivity and lifecycle input cancellation fixtures; Android event delivery remains unverified. |
| MOB-25 | Partial | Host labels/focus/contrast/reduced camera motion; native game contrast and real assistive-input review outstanding. |
| MOB-26 | Java implementation; Android gate open | Virtual semantics and native actions tested through Java accessibility; runtime export not verified. |
| MOB-27 | Partial | Public coverage registry and unknown-screen fallback; exhaustive actual-cache scenarios/screenshots outstanding. |
| MOB-28 | Partial | Shared geometry/identity API and item-sprite consumer; desktop-only inventory overlay remains disabled in mobile, broader RuneLite plugin migration not done. |
| MOB-29 | Partial | Bounded measurements, cancellation, full builds and packaged UI tests; real-device performance/background/keyboard/network endurance outstanding. |
| MOB-30 | Candidate implementation | Versioned dedicated JAR, rollback switches, documentation and CI. Final JR5 sign-off is not asserted. |

## Build and reproduce

Use Java 8 for the Gradle build:

```sh
bash ./gradlew :client:test
xvfb-run -a -s '-screen 0 1920x1200x24' bash ./gradlew :client:standaloneHostTest :client:responsiveHostTest
bash ./gradlew :client:proguardJar :client:jarRunnerJar
xvfb-run -a -s '-screen 0 390x844x24' bash ./gradlew :client:standaloneReleaseTest :client:releasedMobileUiTest
node --test web/mobile/test/*.test.mjs
```

The new native/adversarial tests use synthetic widgets and captured native dispatch seams. Responsive screenshots use actual Swing with synthetic game state, explicitly labelled as such. The packaged UI test uses the obfuscated JAR and generated name mapping, not development classes. Neither CI nor Xvfb emulates Android Jar Runner, game networking or TalkBack.

## Device acceptance gate

Keep the prior JR1 JAR as a rollback. On the actual host, verify setup and dimensions; open Panels; choose native navigation, inventory and a bank interface; check default/actions/Move; use Text for login/chat/search/quantity; test keyboard appearance, orientation where delivered, background/resume and connection loss. Verify an interrupted or covered action never executes. Export only the redacted catalogue for geometry diagnosis; never provide passwords or private chat. Matching cache fixtures and field-focus contracts are needed before the remaining partial items can be closed.
