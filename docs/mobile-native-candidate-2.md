# Native mobile candidate 2 — no bottom toolbar

This increment continues the existing Jar Runner Java client. It is not an APK, a browser conversion, or a fully device-certified completion of JR2–JR5. Use the dedicated `void-client-jarrunner-mobile.jar`; its manifest selects `JarRunnerLauncher`. Existing server configuration and mobile preferences are retained. No network or game protocol changes are required.

## What changed visibly

The persistent Actions / Camera / Text / Panels / More dock and its status row are removed. The actual heavyweight game Canvas receives the whole host content rectangle. There is no replacement bottom toolbar and no automatically reappearing footer.

A small menu glyph is drawn directly by the active game renderer at the top-left, with a Java-accessible label. It is not a Swing component layered over the game Canvas. The input adapter and bridge capture the same painted rectangle. A captured press cannot become a game click after cancellation, resize or release outside the glyph. F10 is a keyboard recovery route. Settings can put the glyph on the right edge and adjust its control size. A second X appears only while an item, spell or Move selection is armed; it cancels that selection.

The utility menu retains camera buttons, text entry, display/input, accessibility, diagnostics and the optional alternative Panels presentation. Game-tabs shortcuts are derived from currently painted, named native navigation controls and their real operations. Hidden or unexposed tabs are not guessed. Returning to the game finishes dialog cancellation before arming the requested operation.

**New preferences default to 100% game size instead of 125%.** Existing saved size choices are deliberately preserved. To undo an older magnification setting, open the menu → Display and input → set Game size to 100%, or use Reset. The change does not implement an independent software UI compositor or change the engine's minimum raster dimensions.

## Native HUD and interface work

Compatible, visible attachments on native roots 548 and 746 now receive mobile dock bounds through the existing layout recursion. Parent IDs, attachment topology, raw script coordinates, item slot IDs, game operations and scene projection are not replaced.

Portrait and landscape plans allocate compatible side panels, main panels, chat and dialogue within the available interface area. Passive native ancestors can expand to the viewport. Attached roots fill their assigned area; wide compatible nested containers can stretch while retaining authored margins. Existing native item-grid reflow then works inside those resized containers, including bank-like scrolling grids.

The layout is deliberately conservative: duplicate area owners, hidden or cyclic ancestry, scrolling/interactive ancestors, unknown roots and incompatible structures retain their original layout. Script changes invalidate the old plan. This is structural adaptation, not proof that every component in a particular cache matches those structures. Native chat controls themselves and the minimap/tab artwork have not all been individually redesigned.

Compatible simple dialogue groups now wrap and stack native text/choices with larger choice targets and vertical overflow. When a cache font is available, its native metrics determine text height. Missing metrics retain the authored layout and trigger a bounded retry when the font loads. Unknown nested, 3D, special-content and keyboard-script structures are not flattened.

Equipment group 387 has a guarded paper-doll layout for the complete known eleven-slot, same-parent sprite pattern. It enlarges slot targets while retaining their native identities and relative equipment arrangement. A cache with different wrappers, a mixed 3D preview or an incomplete pattern remains on the original layout. This pattern has synthetic native regression coverage, not live-cache certification.

Disable **Adapt supported native HUD attachments and dialogue** in the accessibility menu to roll back docking and flow. Native item grids have a separate rollback switch. Restoring both switches restores the authored positions/extents for the tested structures without replacing the JAR.

## Direct native two-tap Move

For a supported item, hold it to open its native action list, choose **Move item: tap a destination slot**, then tap a destination directly in the game. The alternative Panels window is no longer required for this path.

Source selection sends no inventory-move operation. Destination selection revalidates source and destination identity, item/count, ancestry, loaded parents, current screen and native permissions. An invalid or out-of-container destination cannot turn into a default walk/use/drop action. Cancel via the in-canvas X or the utility menu. The existing native drag-completion path performs the operation; no new game packet or optimistic inventory mutation was introduced. Final dispatch is covered by offline fixtures; live server confirmation is not tested here.

## Verification added

`MobileNativeNextRegression` covers painted menu coordinates across viewports/scales, AWT cancellation and secondary buttons, F10 repeat, docking/fallbacks, actual recursive attached-bank layout at five sizes, 120 dynamic slots, repeated layouts, rollback, native text/equipment patterns and direct native Move with stale-source/unloaded-parent rejection.

`NativeCanvasSmoke` runs against development classes and separately against only the obfuscated release JAR. It creates the real toolbar-free host and heavyweight Canvas, invokes the native software widget renderer with synthetic rectangles, checks committed paint geometry and actual pixels, verifies menu clip restoration and pixel bounds, exercises the bridge hit route, checks dialog input ownership and return-to-game ordering, and verifies F10 recovery. Its screenshot is explicitly synthetic; it is not an Android/gameplay image.

Existing core, native, adversarial, workflow, responsive-panel, standalone-host, browser-bridge and packaged-startup/UI tests remain in the build. The standalone suite replaces former toolbar-layout expectations with full-height/no-toolbar assertions rather than retaining assertions for deleted controls.

## Still outstanding

A matching live cache/server and a physical Jar Runner device have not been exercised. In particular, the actual cache's HUD attachment compatibility, tab reachability, chat/minimap placement, dialogue/equipment variants, login→bank→chat/trade round trips, graphics drivers, sustained performance and rotation/keyboard behaviour remain acceptance gates.

Field-bound login/chat/search/quantity adapters, independent cache-native font scaling, an independent software UI compositor and broad plugin-overlay migration remain source work. This build does not claim raw Android multitouch/IME composition/insets, native Android system Back, or a verified Jar Runner-to-TalkBack bridge. The earlier text editor's delivery acknowledgement remains keyboard-path consumption, not confirmed acceptance by a particular game field.

The dedicated mobile renderer now uses full-buffer presentation to ensure the small native overlay is displayed and stale glyphs are invalidated. GPU/thermal/battery impact has not been measured. Normal desktop startup remains separate; this pass does not enable mobile presentation on the ordinary desktop entry point.
