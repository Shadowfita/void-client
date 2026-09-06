from pathlib import Path
import re


def read_source(path):
    p = Path(path)
    raw = p.read_bytes()
    newline = '\r\n' if b'\r\n' in raw else '\n'
    text = raw.decode('utf-8').replace('\r\n', '\n')
    return p, text, newline


def write_source(p, text, newline):
    if newline != '\n':
        text = text.replace('\n', newline)
    p.write_bytes(text.encode('utf-8'))


def replace_once(path, old, new, label):
    p, text, newline = read_source(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    write_source(p, text.replace(old, new, 1), newline)


def replace_regex_once(path, pattern, replacement, label, flags=0):
    p, text, newline = read_source(path)
    text, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one regex match, got {count}')
    write_source(p, text, newline)


# ---------------------------------------------------------------------------
# 1. Plugin startup becomes transactional. A partially-started plugin must not
# remain active, registered, scheduled, or retain resources after an exception.
# ---------------------------------------------------------------------------
replace_regex_once(
    'client/src/net/runelite/client/plugins/PluginManager.java',
    r'\tpublic boolean startPlugin\(Plugin plugin\) throws PluginInstantiationException\n\t\{.*?\n\t\treturn true;\n\t\}\n\n\tpublic boolean stopPlugin',
    '''\tpublic boolean startPlugin(Plugin plugin) throws PluginInstantiationException
\t{
\t\t// plugins always start in the EDT
\t\tassert SwingUtilities.isEventDispatchThread();

\t\tif (!canPluginRun(plugin))
\t\t{
\t\t\tsetPluginEnabled(plugin, false);
\t\t\treturn false;
\t\t}

\t\tif (activePlugins.contains(plugin) || !isPluginEnabled(plugin))
\t\t{
\t\t\treturn false;
\t\t}

\t\tboolean startupInvoked = false;
\t\tboolean registered = false;
\t\tboolean scheduled = false;
\t\ttry
\t\t{
\t\t\tstartupInvoked = true;
\t\t\tplugin.startUp();

\t\t\tif (!isOutdated && sceneTileManager != null)
\t\t\t{
\t\t\t\tfinal GameEventManager gameEventManager = this.sceneTileManager.get();
\t\t\t\tif (gameEventManager != null)
\t\t\t\t{
\t\t\t\t\tgameEventManager.simulateGameEvents(plugin);
\t\t\t\t}
\t\t\t}

\t\t\teventBus.register(plugin);
\t\t\tregistered = true;
\t\t\tschedule(plugin);
\t\t\tscheduled = true;
\t\t\tactivePlugins.add(plugin);
\t\t\teventBus.post(new PluginChanged(plugin, true));
\t\t\tlog.debug("Plugin {} is now running", plugin.getClass().getSimpleName());
\t\t\treturn true;
\t\t}
\t\tcatch (ThreadDeath e)
\t\t{
\t\t\trollbackFailedStart(plugin, startupInvoked, registered, scheduled, e);
\t\t\tthrow e;
\t\t}
\t\tcatch (Throwable ex)
\t\t{
\t\t\trollbackFailedStart(plugin, startupInvoked, registered, scheduled, ex);
\t\t\tthrow new PluginInstantiationException(ex);
\t\t}
\t}

\tprivate void rollbackFailedStart(
\t\tPlugin plugin,
\t\tboolean startupInvoked,
\t\tboolean registered,
\t\tboolean scheduled,
\t\tThrowable cause)
\t{
\t\tactivePlugins.remove(plugin);
\t\tif (scheduled)
\t\t{
\t\t\ttry
\t\t\t{
\t\t\t\tunschedule(plugin);
\t\t\t}
\t\t\tcatch (Throwable cleanupFailure)
\t\t\t{
\t\t\t\tcause.addSuppressed(cleanupFailure);
\t\t\t}
\t\t}
\t\tif (registered)
\t\t{
\t\t\ttry
\t\t\t{
\t\t\t\teventBus.unregister(plugin);
\t\t\t}
\t\t\tcatch (Throwable cleanupFailure)
\t\t\t{
\t\t\t\tcause.addSuppressed(cleanupFailure);
\t\t\t}
\t\t}
\t\tif (startupInvoked)
\t\t{
\t\t\ttry
\t\t\t{
\t\t\t\tplugin.shutDown();
\t\t\t}
\t\t\tcatch (Throwable cleanupFailure)
\t\t\t{
\t\t\t\tcause.addSuppressed(cleanupFailure);
\t\t\t}
\t\t}
\t\tlog.warn("Rolled back failed start of plugin {}", plugin.getClass().getSimpleName(), cause);
\t}

\tpublic boolean stopPlugin''',
    'transactional plugin startup',
    re.S,
)


# ---------------------------------------------------------------------------
# 2. Stable scene state: NPC movement is not despawn/spawn, and scene-local
# snapshots must not cross base-coordinate or plane changes.
# ---------------------------------------------------------------------------
path = 'client/src/net/runelite/client/game/GameStateBridge.java'
replace_once(
    path,
    '''\tprivate boolean groundItemsInitialized;
\tprivate boolean npcsInitialized;
\tprivate boolean skillsInitialized;
''',
    '''\tprivate boolean groundItemsInitialized;
\tprivate boolean npcsInitialized;
\tprivate boolean skillsInitialized;
\tprivate int lastBaseX = Integer.MIN_VALUE;
\tprivate int lastBaseY = Integer.MIN_VALUE;
\tprivate int lastPlane = Integer.MIN_VALUE;
''',
    'scene identity fields',
)
replace_once(
    path,
    '''\t\tif (client == null)
\t\t{
\t\t\treturn;
\t\t}

\t\tif (!GameEventBridgeHooks.hasDirectNpcHooks())
''',
    '''\t\tif (client == null)
\t\t{
\t\t\treturn;
\t\t}

\t\tresetSceneTrackingIfNeeded();

\t\tif (!GameEventBridgeHooks.hasDirectNpcHooks())
''',
    'scene reset invocation',
)
replace_once(
    path,
    '''\tprivate void updateGroundItems()
''',
    '''\tprivate void resetSceneTrackingIfNeeded()
\t{
\t\tint baseX = client.getBaseX();
\t\tint baseY = client.getBaseY();
\t\tint plane = client.getPlane();
\t\tif (baseX == lastBaseX && baseY == lastBaseY && plane == lastPlane)
\t\t{
\t\t\treturn;
\t\t}

\t\tgroundItems.clear();
\t\tnpcs.clear();
\t\trecentNpcDespawns.clear();
\t\tgroundItemsInitialized = false;
\t\tnpcsInitialized = false;
\t\tlastBaseX = baseX;
\t\tlastBaseY = baseY;
\t\tlastPlane = plane;
\t}

\tprivate void updateGroundItems()
''',
    'scene reset method',
)
replace_once(
    path,
    '''\tprivate static String npcKey(GameClient.NpcInfo npc)
\t{
\t\treturn npc.getId() + ":" + npc.getPlane() + ":" + npc.getLocalX() + ":" + npc.getLocalY() + ":" + npc.getName();
\t}
''',
    '''\tprivate static String npcKey(GameClient.NpcInfo npc)
\t{
\t\tif (npc.getIndex() >= 0)
\t\t{
\t\t\treturn "index:" + npc.getIndex();
\t\t}
\t\t// Legacy fallback only. Current 634 snapshots expose the native NPC index.
\t\treturn "fallback:" + npc.getId() + ":" + npc.getPlane() + ":"
\t\t\t+ npc.getLocalX() + ":" + npc.getLocalY() + ":" + npc.getName();
\t}
''',
    'stable NPC key',
)


# ---------------------------------------------------------------------------
# 3. Idle/vital notifications ignore uninitialised arrays and baseline the
# first valid sample. This prevents the observed startup "Low prayer: 0" alert.
# ---------------------------------------------------------------------------
path = 'client/src/net/runelite/client/plugins/idlenotifier/IdleNotifierPlugin.java'
replace_once(
    path,
    '''\tprivate boolean idleNotified;
\tprivate boolean hitpointsNotified;
\tprivate boolean prayerNotified;
''',
    '''\tprivate boolean idleNotified;
\tprivate boolean hitpointsNotified;
\tprivate boolean prayerNotified;
\tprivate boolean hitpointsInitialised;
\tprivate boolean prayerInitialised;
''',
    'vital baseline fields',
)
replace_once(
    path,
    '''\t\t\t\telse if (config.notifyLogout() && now - playerMissingSince >= 1500L)
\t\t\t\t{
\t\t\t\t\tnotifier.notify("You have logged out.");
\t\t\t\t\thadPlayer = false;
\t\t\t\t}
''',
    '''\t\t\t\telse if (config.notifyLogout() && now - playerMissingSince >= 1500L)
\t\t\t\t{
\t\t\t\t\tnotifier.notify("You have logged out.");
\t\t\t\t\thadPlayer = false;
\t\t\t\t\tresetVitals();
\t\t\t\t}
''',
    'logout vital reset',
)
replace_regex_once(
    path,
    r'\tprivate void checkVitals\(\)\n\t\{.*?\n\t\}\n\n\tprivate void reset\(\)',
    '''\tprivate void checkVitals()
\t{
\t\tfor (GameClient.SkillSnapshot skill : client.getSkillSnapshots())
\t\t{
\t\t\tint base = skill.getLevel();
\t\t\tint current = skill.getBoostedLevel();
\t\t\tif (base <= 0 || current <= 0)
\t\t\t{
\t\t\t\tcontinue;
\t\t\t}

\t\t\tif (skill.getSkill() == Skill.HITPOINTS)
\t\t\t{
\t\t\t\tif (!hitpointsInitialised)
\t\t\t\t{
\t\t\t\t\thitpointsInitialised = true;
\t\t\t\t\thitpointsNotified = current <= config.hitpointsThreshold();
\t\t\t\t\tcontinue;
\t\t\t\t}
\t\t\t\tif (config.notifyLowHitpoints() && current <= config.hitpointsThreshold())
\t\t\t\t{
\t\t\t\t\tif (!hitpointsNotified)
\t\t\t\t\t{
\t\t\t\t\t\tnotifier.notify("Low hitpoints: " + current + ".");
\t\t\t\t\t\thitpointsNotified = true;
\t\t\t\t\t}
\t\t\t\t}
\t\t\t\telse if (current > config.hitpointsThreshold())
\t\t\t\t{
\t\t\t\t\thitpointsNotified = false;
\t\t\t\t}
\t\t\t}
\t\t\telse if (skill.getSkill() == Skill.PRAYER)
\t\t\t{
\t\t\t\tif (!prayerInitialised)
\t\t\t\t{
\t\t\t\t\tprayerInitialised = true;
\t\t\t\t\tprayerNotified = current <= config.prayerThreshold();
\t\t\t\t\tcontinue;
\t\t\t\t}
\t\t\t\tif (config.notifyLowPrayer() && current <= config.prayerThreshold())
\t\t\t\t{
\t\t\t\t\tif (!prayerNotified)
\t\t\t\t\t{
\t\t\t\t\t\tnotifier.notify("Low prayer: " + current + ".");
\t\t\t\t\t\tprayerNotified = true;
\t\t\t\t\t}
\t\t\t\t}
\t\t\t\telse if (current > config.prayerThreshold())
\t\t\t\t{
\t\t\t\t\tprayerNotified = false;
\t\t\t\t}
\t\t\t}
\t\t}
\t}

\tprivate void reset()''',
    'valid vital snapshots',
    re.S,
)
replace_once(
    path,
    '''\t\tidleNotified = false;
\t\thitpointsNotified = false;
\t\tprayerNotified = false;
\t}
''',
    '''\t\tidleNotified = false;
\t\tresetVitals();
\t}

\tprivate void resetVitals()
\t{
\t\thitpointsNotified = false;
\t\tprayerNotified = false;
\t\thitpointsInitialised = false;
\t\tprayerInitialised = false;
\t}
''',
    'reset vital baselines',
)


# ---------------------------------------------------------------------------
# 4. Bank Tags: Swing callbacks publish immutable search text; all live client
# and container reads stay on ClientTick.
# ---------------------------------------------------------------------------
path = 'client/src/net/runelite/client/plugins/banktags/BankTagsPanel.java'
replace_once(path, 'import java.util.Map;\n', 'import java.util.Map;\nimport java.util.function.Consumer;\n', 'BankTagsPanel Consumer import')
replace_once(path, '\tprivate Runnable searchListener;\n', '\tprivate Consumer<String> searchListener;\n', 'BankTagsPanel listener type')
replace_once(path, '\t\t\tprivate void changed() { if (searchListener != null) searchListener.run(); }\n', '\t\t\tprivate void changed() { if (searchListener != null) searchListener.accept(search.getText()); }\n', 'BankTagsPanel callback')
replace_once(path, '\tvoid setSearchListener(Runnable listener) { this.searchListener = listener; }\n\tString getSearchText() { return search.getText(); }\n', '\tvoid setSearchListener(Consumer<String> listener) { this.searchListener = listener; }\n', 'BankTagsPanel API')

path = 'client/src/net/runelite/client/plugins/banktags/BankTagsPlugin.java'
replace_once(path, '\tprivate long lastRefresh;\n\tprivate int lastFingerprint;\n', '\tprivate long lastRefresh;\n\tprivate int lastFingerprint;\n\tprivate volatile String searchQuery = "";\n\tprivate volatile boolean refreshRequested;\n', 'BankTags state fields')
replace_once(path, '\t\tpanel.setSearchListener(this::forceRefresh);\n', '\t\tpanel.setSearchListener(this::onSearchChanged);\n', 'BankTags listener')
replace_once(path, '\t\tforceRefresh();\n', '\t\trequestRefresh();\n', 'BankTags startup refresh')
replace_once(path, '\t\tlastFingerprint = 0;\n\t\tlastRefresh = 0L;\n', '\t\tlastFingerprint = 0;\n\t\tlastRefresh = 0L;\n\t\tsearchQuery = "";\n\t\trefreshRequested = false;\n', 'BankTags shutdown state')
replace_regex_once(
    path,
    r'\t@Subscribe\n\tpublic void onClientTick\(ClientTick tick\)\n\t\{.*?\n\tprivate void refresh\(boolean forced\)',
    '''\t@Subscribe
\tpublic void onClientTick(ClientTick tick)
\t{
\t\tif (panel == null)
\t\t{
\t\t\treturn;
\t\t}
\t\tboolean forced = refreshRequested;
\t\tif (!forced && System.currentTimeMillis() - lastRefresh < 500L)
\t\t{
\t\t\treturn;
\t\t}
\t\trefreshRequested = false;
\t\trefresh(forced);
\t}

\tprivate void onSearchChanged(String query)
\t{
\t\tsearchQuery = query == null ? "" : query;
\t\trequestRefresh();
\t}

\tprivate void requestRefresh()
\t{
\t\tlastFingerprint = Integer.MIN_VALUE;
\t\trefreshRequested = true;
\t}

\tprivate void refresh(boolean forced)''',
    'BankTags client-thread refresh',
    re.S,
)
replace_once(path, '\t\tString query = panel == null ? "" : panel.getSearchText();\n', '\t\tString query = searchQuery;\n', 'BankTags cached query')


# ---------------------------------------------------------------------------
# 5. Inventory Grid activates only for a real inventory item and cannot
# dereference a null dragged item or null source bounds.
# ---------------------------------------------------------------------------
path = 'client/src/net/runelite/client/plugins/inventorygrid/InventoryGridOverlay.java'
replace_once(
    path,
    '''\t\tif (inventoryItems.size() < INVENTORY_SIZE)
\t\t{
\t\t\treturn null;
\t\t}

\t\tRectangle initialBounds = draggedItem.getCanvasBounds(false);
''',
    '''\t\tif (inventoryItems.size() < INVENTORY_SIZE || draggedItem == null)
\t\t{
\t\t\treturn null;
\t\t}

\t\tRectangle initialBounds = draggedItem.getCanvasBounds(false);
\t\tif (initialBounds == null)
\t\t{
\t\t\treturn null;
\t\t}
''',
    'InventoryGrid null drag guard',
)
replace_once(
    path,
    '''\t\tmouseDown = true;
\t\thoverActive = false;
\t\tdraggedItem = findItemAt(lastInventoryItems, mousePoint);
\t\treturn mouseEvent;
''',
    '''\t\thoverActive = false;
\t\tdraggedItem = findItemAt(lastInventoryItems, mousePoint);
\t\tmouseDown = draggedItem != null;
\t\tif (!mouseDown)
\t\t{
\t\t\tinitialMousePoint = null;
\t\t}
\t\treturn mouseEvent;
''',
    'InventoryGrid scoped drag activation',
)


# ---------------------------------------------------------------------------
# 6. Optional supersampling fails closed on mismatched legacy JAGGL binaries.
# A clean release currently has no caller; this guard protects mixed runtimes.
# ---------------------------------------------------------------------------
path = 'client/src/Class258_Sub3_Sub1.java'
replace_once(
    path,
    '''        releaseInterfaceSupersampleTexture();
        Class258_Sub3 texture = this.aHa_Sub2_4851.createInterfaceSupersampleTexture(this, factor);
        if (texture != null) {
            interfaceSupersampleTexture = texture;
            interfaceSupersampleFactor = factor;
            return texture;
        }
        return this;
''',
    '''        releaseInterfaceSupersampleTexture();
        try {
            Class258_Sub3 texture = this.aHa_Sub2_4851.createInterfaceSupersampleTexture(this, factor);
            if (texture != null) {
                interfaceSupersampleTexture = texture;
                interfaceSupersampleFactor = factor;
                return texture;
            }
        } catch (UnsatisfiedLinkError unsupportedJagglReadback) {
            // Legacy/mixed JAGGL bundles can expose Java declarations without
            // the corresponding native symbol. Supersampling is optional.
            disableInterfaceSupersampling();
        }
        return this;
''',
    'JAGGL supersampling failsafe',
)


# ---------------------------------------------------------------------------
# 7. Log an exact runtime JAR fingerprint. This distinguishes audited releases
# from stale or mixed local artifacts, which the crash evidence now indicates.
# ---------------------------------------------------------------------------
build_identity = Path('client/src/net/runelite/client/util/BuildIdentity.java')
if build_identity.exists():
    raise SystemExit('BuildIdentity.java unexpectedly already exists')
build_identity.write_text('''package net.runelite.client.util;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public final class BuildIdentity
{
\tprivate BuildIdentity()
\t{
\t}

\tpublic static String describe(Class<?> anchor)
\t{
\t\ttry
\t\t{
\t\t\tURL location = anchor.getProtectionDomain().getCodeSource().getLocation();
\t\t\tPath path = Paths.get(location.toURI()).toAbsolutePath().normalize();
\t\t\tif (!Files.isRegularFile(path))
\t\t\t{
\t\t\t\treturn "classes=" + path;
\t\t\t}
\t\t\treturn "jar=" + path + ", size=" + Files.size(path) + ", sha256=" + sha256(path);
\t\t}
\t\tcatch (Throwable ex)
\t\t{
\t\t\treturn "unavailable (" + ex.getClass().getSimpleName() + ")";
\t\t}
\t}

\tprivate static String sha256(Path path) throws Exception
\t{
\t\tMessageDigest digest = MessageDigest.getInstance("SHA-256");
\t\tbyte[] buffer = new byte[64 * 1024];
\t\ttry (InputStream input = Files.newInputStream(path))
\t\t{
\t\t\tint read;
\t\t\twhile ((read = input.read(buffer)) != -1)
\t\t\t{
\t\t\t\tdigest.update(buffer, 0, read);
\t\t\t}
\t\t}
\t\tStringBuilder value = new StringBuilder(64);
\t\tfor (byte b : digest.digest())
\t\t{
\t\t\tvalue.append(String.format("%02x", b & 0xff));
\t\t}
\t\treturn value.toString();
\t}
}
''')

path = 'client/src/net/runelite/client/RuneLite.java'
replace_once(path, 'import net.runelite.client.ui.overlay.tooltip.TooltipOverlay;\n', 'import net.runelite.client.ui.overlay.tooltip.TooltipOverlay;\nimport net.runelite.client.util.BuildIdentity;\n', 'BuildIdentity import')
replace_once(
    path,
    '''\t\t\tlog.info("RuneLite {} (launcher version {}) starting up, args: {}",
\t\t\t\tRuneLiteProperties.getVersion(), RuneLiteProperties.getLauncherVersion() == null ? "unknown" : RuneLiteProperties.getLauncherVersion(),
\t\t\t\targs.length == 0 ? "none" : String.join(" ", args));
''',
    '''\t\t\tlog.info("RuneLite {} (launcher version {}) starting up, args: {}",
\t\t\t\tRuneLiteProperties.getVersion(), RuneLiteProperties.getLauncherVersion() == null ? "unknown" : RuneLiteProperties.getLauncherVersion(),
\t\t\t\targs.length == 0 ? "none" : String.join(" ", args));
\t\t\tlog.info("Runtime artifact: {}", BuildIdentity.describe(RuneLite.class));
''',
    'runtime artifact log',
)


# ---------------------------------------------------------------------------
# 8. Version and release notes for this bounded development wave.
# ---------------------------------------------------------------------------
replace_once('client/build.gradle.kts', 'version = "0.3.1"', 'version = "0.3.2"', 'Gradle version')
replace_once('client/src/net/runelite/client/runelite.properties', 'void.version= 0.3.1', 'void.version= 0.3.2', 'runtime version')
Path('release-notes/v0.3.2.md').write_text('''# Void Client v0.3.2 — remediation wave 1

This development branch begins the adversarial plugin-remediation programme.

## Correctness and lifecycle

- Plugin startup is transactional and rolls back partial overlay, event-bus and scheduler state when startup fails.
- Polled NPC identity uses the native NPC index rather than mutable coordinates, preventing movement from appearing as despawn/spawn churn.
- Scene-local NPC and ground-item snapshots reset when base coordinates or plane change.
- Idle notifications ignore uninitialised zero stat arrays and baseline valid hitpoints/prayer values before alerting.

## Threading and safety

- Bank-tag search callbacks no longer read live client containers from Swing's event-dispatch thread.
- Inventory-grid drag rendering is scoped to a real inventory item and guards null bounds.
- Optional fractional supersampling fails closed if a mixed legacy JAGGL runtime lacks an advertised native symbol.
- Startup logs the exact runtime JAR path, size and SHA-256 fingerprint to expose stale or mixed artifacts.

This is the first implementation wave, not final qualification of every plugin.
''')

print('Applied v0.3.2 remediation wave 1')
