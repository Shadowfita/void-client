from pathlib import Path
import difflib
import subprocess


def write(path, content):
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content)


def replace_once(path, old, new, label):
    target = Path(path)
    text = target.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    target.write_text(text.replace(old, new, 1))


path = 'client/build.gradle.kts'
text = Path(path).read_text()
if 'testImplementation("junit:junit:4.13.2")' not in text:
    dependency_anchor = '    implementation("com.google.code.findbugs:jsr305:3.0.2")\n'
    if text.count(dependency_anchor) != 1:
        raise SystemExit('build dependency anchor mismatch')
    text = text.replace(
        dependency_anchor,
        dependency_anchor + '    testImplementation("junit:junit:4.13.2")\n',
        1,
    )
if 'java.srcDirs("tests")' not in text:
    source_set_anchor = '''        main {
            java.srcDirs("src")
            resources.srcDirs("resources", "src")
            resources.exclude("**/*.java")
        }
'''
    source_set_replacement = source_set_anchor + '''        test {
            java.srcDirs("tests")
        }
'''
    if text.count(source_set_anchor) != 1:
        raise SystemExit('build source-set anchor mismatch')
    text = text.replace(source_set_anchor, source_set_replacement, 1)
Path(path).write_text(text)

write('client/tests/net/runelite/client/compatibility/EventConformanceTest.java', '''package net.runelite.client.compatibility;

import net.runelite.api.events.StatChanged;
import net.runelite.api.Skill;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventConformanceTest
{
\t@Before public void before() { EventConformance.resetForTests(); }
\t@After public void after() { EventConformance.resetForTests(); }

\t@Test public void accountsForProducedAndDeliveredEvents()
\t{
\t\tStatChanged event = new StatChanged(Skill.ATTACK, 1000, 10, 10);
\t\tEventConformance.DeliveryToken token = EventConformance.produced(event, EventOrigin.DIRECT);
\t\tEventConformance.delivered(token);
\t\tEventConformanceSnapshot snapshot = EventConformance.snapshot(BridgeEventChannel.STAT);
\t\tassertEquals(1L, snapshot.getProduced());
\t\tassertEquals(1L, snapshot.getDelivered());
\t\tassertEquals(1L, snapshot.getDirect());
\t\tassertEquals(0L, snapshot.getDropped());
\t}

\t@Test public void detectsAnImmediateDuplicateAfterFirstUseOverhead()
\t{
\t\tStatChanged first = new StatChanged(Skill.PRAYER, 2000, 20, 20);
\t\tEventConformance.delivered(EventConformance.produced(first, EventOrigin.DIRECT));
\t\tStatChanged second = new StatChanged(Skill.PRAYER, 2000, 20, 20);
\t\tEventConformance.delivered(EventConformance.produced(second, EventOrigin.DIRECT));
\t\tassertEquals(1L, EventConformance.snapshot(BridgeEventChannel.STAT).getDuplicates());
\t}
}
''')

write('client/tests/net/runelite/client/compatibility/ProjectionCalibrationTest.java', '''package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProjectionCalibrationTest
{
\tprivate static Polygon tile()
\t{ return new Polygon(new int[]{90,110,110,90}, new int[]{105,105,115,115}, 4); }

\t@Test public void acceptsTileWhenNativeAndRuneLiteReferencesAgree()
\t{
\t\tProjectionCalibration.Validation value = ProjectionCalibration.validateTile(
\t\t\t800, 600, new Point(100,110), tile(), new Point(102,109));
\t\tassertTrue(value.getReason(), value.isValid());
\t}

\t@Test public void rejectsTileWhenNativeReferenceDisagrees()
\t{
\t\tProjectionCalibration.Validation value = ProjectionCalibration.validateTile(
\t\t\t800, 600, new Point(100,110), tile(), new Point(145,155));
\t\tassertFalse(value.isValid());
\t}

\t@Test public void acceptsActorAnchorsWhenIndependentReferencesAgree()
\t{
\t\tProjectionCalibration.Validation value = ProjectionCalibration.validateActor(800,600,
\t\t\tnew Point(200,300), new Point(200,240), new Point(201,299), new Point(201,241));
\t\tassertTrue(value.getReason(), value.isValid());
\t}

\t@Test public void rejectsActorAnchorWithInvertedHeight()
\t{
\t\tProjectionCalibration.Validation value = ProjectionCalibration.validateActor(800,600,
\t\t\tnew Point(200,300), new Point(200,340), new Point(200,300), new Point(200,340));
\t\tassertFalse(value.isValid());
\t}
}
''')

write('client/tests/net/runelite/client/game/ItemSnapshotResolverTest.java', '''package net.runelite.client.game;

import com.GameClient;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemSnapshotResolverTest
{
\tprivate static GameClient.ItemContainerSnapshot inventory(int firstItem)
\t{
\t\tList<GameClient.ItemStackInfo> items = new ArrayList<>();
\t\tif (firstItem >= 0) items.add(new GameClient.ItemStackInfo(firstItem, 1, "Item", 1, 0));
\t\treturn new GameClient.ItemContainerSnapshot(93L, items, 28,
\t\t\tItemContainerRole.INVENTORY, true, 1L);
\t}

\tprivate static WidgetItemSnapshotStore.FrameSnapshot frame(int widget, int firstItem)
\t{
\t\tWidgetItemSnapshotStore.beginFrame(1L, 746);
\t\tfor (int i = 0; i < 28; i++)
\t\t{
\t\t\tint item = i == 0 ? firstItem : -1;
\t\t\tRectangle bounds = new Rectangle(500 + (i % 4) * 42, 200 + (i / 4) * 36, 32, 32);
\t\t\tWidgetItemSnapshotStore.capture(widget, i, 5, item, item < 0 ? 0 : 1,
\t\t\t\tbounds, new Rectangle(0,0,800,600));
\t\t}
\t\tWidgetItemSnapshotStore.endFrame();
\t\treturn WidgetItemSnapshotStore.snapshot();
\t}

\t@Test public void resolvesExactInventoryGeometry()
\t{
\t\tItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.INVENTORY, inventory(4151), frame(14942208, 4151));
\t\tassertTrue(value.getReason(), value.isAvailable());
\t\tassertEquals(28, value.getSlots().size());
\t\tassertEquals(14942208, value.getPackedWidgetId());
\t}

\t@Test public void rejectsAnyItemIdentityMismatch()
\t{
\t\tItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.INVENTORY, inventory(4151), frame(14942208, 995));
\t\tassertFalse(value.isAvailable());
\t}

\t@Test public void rejectsEquallyMatchingWidgetGroups()
\t{
\t\tWidgetItemSnapshotStore.beginFrame(2L, 746);
\t\tfor (int widget : new int[]{100,200})
\t\t\tfor (int i = 0; i < 28; i++)
\t\t\t\tWidgetItemSnapshotStore.capture(widget, i, 5, i == 0 ? 4151 : -1,
\t\t\t\t\ti == 0 ? 1 : 0, new Rectangle(i*2, i*2, 32,32), new Rectangle(0,0,800,600));
\t\tWidgetItemSnapshotStore.endFrame();
\t\tItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.INVENTORY, inventory(4151), WidgetItemSnapshotStore.snapshot());
\t\tassertFalse(value.isAvailable());
\t\tassertTrue(value.getReason().contains("Multiple native widget groups"));
\t}

\t@Test public void rejectsAnAbortedWidgetFrame()
\t{
\t\tWidgetItemSnapshotStore.beginFrame(3L, 746);
\t\tWidgetItemSnapshotStore.capture(100, 0, 5, 4151, 1,
\t\t\tnew Rectangle(0,0,32,32), new Rectangle(0,0,800,600));
\t\tWidgetItemSnapshotStore.abortFrame();
\t\tItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.INVENTORY, inventory(4151), WidgetItemSnapshotStore.snapshot());
\t\tassertFalse(value.isAvailable());
\t\tassertTrue(value.getReason().contains("incomplete"));
\t}
}
''')

notes = Path('release-notes/v0.3.2.md')
text = notes.read_text() if notes.exists() else '# Void Client v0.3.2\n'
section = '''
## Remediation wave 2 — compatibility conformance

- Added an explicit capability registry with `UNKNOWN`, `AVAILABLE`, `DEGRADED` and `UNAVAILABLE` states and evidence strings.
- Added direct-versus-polled event accounting, delivery/drop tracking, client-thread checks and probable duplicate detection.
- Added exact native inventory (93) and bank (95) roles; capacity-based container guessing has been removed.
- Captures dynamic native widget children during the completed interface render and publishes immutable physical slot bounds.
- Rejects incomplete frames, item-ID mismatches and equally plausible widget groups instead of guessing.
- Removed the competing global AWT Inventory Grid renderer.
- Added independent native actor projection references and runtime tile/actor calibration canaries.
- Tile Indicators and NPC Names fail closed until repeated projection agreement samples pass.
- Added an opt-in Compatibility Diagnostics panel and overlay exposing capabilities, event evidence, container resolution and canary geometry.
- Added a real JUnit source set and ten deterministic conformance tests; CI rejects a `NO-SOURCE` test result.

This tranche intentionally does not broadly qualify the remaining world-space plugins. They stay opt-in until the shared canaries pass on a real Linux/JAGGL session.
'''
if '## Remediation wave 2 — compatibility conformance' not in text:
    notes.write_text(text.rstrip() + '\n' + section)

# Restore unchanged tracked lines with their original byte endings. Several deob
# files contain CRLF or mixed endings; generator scripts should not create a
# whole-file line-ending diff when only a few lines changed.
modified = subprocess.check_output(['git','diff','--name-only','--diff-filter=M'], text=True).splitlines()
for name in modified:
    path = Path(name)
    if not path.is_file():
        continue
    try:
        original = subprocess.check_output(['git','show','HEAD:' + name])
    except subprocess.CalledProcessError:
        continue
    current = path.read_bytes()
    old_lines = original.splitlines(keepends=True)
    new_lines = current.splitlines(keepends=True)
    old_keys = [line.rstrip(b'\r\n') for line in old_lines]
    new_keys = [line.rstrip(b'\r\n') for line in new_lines]
    matcher = difflib.SequenceMatcher(None, old_keys, new_keys, autojunk=False)
    output = bytearray()
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == 'equal':
            output.extend(b''.join(old_lines[i1:i2]))
        elif tag in ('replace','insert'):
            for line in new_lines[j1:j2]:
                output.extend(line.rstrip(b'\r\n'))
                if line.endswith((b'\n', b'\r')):
                    output.extend(b'\n')
    path.write_bytes(bytes(output))

print('Added wave 2 deterministic tests, release metadata, and restored line endings')
