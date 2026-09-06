from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    p.write_text(text.replace(old, new, 1))


replace_once(
    'client/src/net/runelite/client/plugins/PluginManager.java',
    '''\t\t\teventBus.register(plugin);
\t\t\tregistered = true;
\t\t\tschedule(plugin);
\t\t\tscheduled = true;
''',
    '''\t\t\t// Mark cleanup eligibility before invoking operations which may
\t\t\t// partially mutate their registries and then throw.
\t\t\tregistered = true;
\t\t\teventBus.register(plugin);
\t\t\tscheduled = true;
\t\t\tschedule(plugin);
''',
    'partial registration rollback eligibility',
)

replace_once(
    'client/src/net/runelite/client/plugins/idlenotifier/IdleNotifierPlugin.java',
    '''\t\t\tif (base <= 0 || current <= 0)
\t\t\t{
\t\t\t\tcontinue;
\t\t\t}

\t\t\tif (skill.getSkill() == Skill.HITPOINTS)
''',
    '''\t\t\tif (base <= 0 || current < 0)
\t\t\t{
\t\t\t\tcontinue;
\t\t\t}

\t\t\tif (skill.getSkill() == Skill.HITPOINTS)
\t\t\t{
\t\t\t\t// Zero hitpoints is a death state rather than a useful threshold alert.
\t\t\t\tif (current == 0)
\t\t\t\t{
\t\t\t\t\tcontinue;
\t\t\t\t}
''',
    'zero prayer transition support',
)

# The replacement above introduces the HITPOINTS opening brace itself, so
# remove the original immediately following brace.
replace_once(
    'client/src/net/runelite/client/plugins/idlenotifier/IdleNotifierPlugin.java',
    '''\t\t\t\t}
\t\t\t{
\t\t\t\tif (!hitpointsInitialised)
''',
    '''\t\t\t\t}
\t\t\t\tif (!hitpointsInitialised)
''',
    'duplicate hitpoints brace removal',
)

notes = Path('release-notes/v0.3.2.md')
text = notes.read_text()
anchor = '- Idle notifications ignore uninitialised zero stat arrays and baseline valid hitpoints/prayer values before alerting.\n'
replacement = anchor + '- Prayer zero remains a valid post-baseline threshold transition; zero hitpoints is treated as death rather than a threshold alert.\n'
if text.count(anchor) != 1:
    raise SystemExit('release-note refinement anchor was not unique')
notes.write_text(text.replace(anchor, replacement, 1))

print('Applied v0.3.2 wave 1 refinements')
