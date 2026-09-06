from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    p.write_text(text.replace(old, new, 1))


# A registry call can partially mutate internal state and then throw. Mark the
# corresponding cleanup path eligible before invoking it.
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


# Reset logout state regardless of whether the user has enabled the notification.
# Previously disabling the notification also prevented the lifecycle transition.
replace_once(
    'client/src/net/runelite/client/plugins/idlenotifier/IdleNotifierPlugin.java',
    '''\t\t\t\telse if (config.notifyLogout() && now - playerMissingSince >= 1500L)
\t\t\t\t{
\t\t\t\t\tnotifier.notify("You have logged out.");
\t\t\t\t\thadPlayer = false;
\t\t\t\t\tresetVitals();
\t\t\t\t}
''',
    '''\t\t\t\telse if (now - playerMissingSince >= 1500L)
\t\t\t\t{
\t\t\t\t\tif (config.notifyLogout())
\t\t\t\t\t{
\t\t\t\t\t\tnotifier.notify("You have logged out.");
\t\t\t\t\t}
\t\t\t\t\thadPlayer = false;
\t\t\t\t\tresetVitals();
\t\t\t\t}
''',
    'logout state independent of notification',
)


# The first valid low sample is a baseline, not a transition. Prayer subsequently
# reaching zero is valid and should notify; zero hitpoints represents death.
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

# The replacement above introduces the HITPOINTS opening brace itself, so remove
# the original immediately following brace.
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


# The bank panel crosses the client-thread/EDT boundary. Make publication visible,
# capture the target panel and config value on the client thread, and suppress a
# queued repaint if that panel has since been shut down or replaced.
replace_once(
    'client/src/net/runelite/client/plugins/banktags/BankTagsPlugin.java',
    '''\tprivate BankTagsPanel panel;
''',
    '''\tprivate volatile BankTagsPanel panel;
''',
    'BankTags volatile panel publication',
)
replace_once(
    'client/src/net/runelite/client/plugins/banktags/BankTagsPlugin.java',
    '''\t\tif (panel != null)
\t\t{
\t\t\tSwingUtilities.invokeLater(() -> panel.rebuild(groups, itemCount, totalValue, config.showValues()));
\t\t}
''',
    '''\t\tBankTagsPanel target = panel;
\t\tif (target != null)
\t\t{
\t\t\tboolean showValues = config.showValues();
\t\t\tSwingUtilities.invokeLater(() ->
\t\t\t{
\t\t\t\tif (target == panel)
\t\t\t\t{
\t\t\t\t\ttarget.rebuild(groups, itemCount, totalValue, showValues);
\t\t\t\t}
\t\t\t});
\t\t}
''',
    'BankTags stale EDT callback guard',
)


notes = Path('release-notes/v0.3.2.md')
text = notes.read_text()
anchor = '- Idle notifications ignore uninitialised zero stat arrays and baseline valid hitpoints/prayer values before alerting.\n'
replacement = (
    anchor
    + '- Logout lifecycle state resets even when logout notifications are disabled.\n'
    + '- Prayer zero remains a valid post-baseline threshold transition; zero hitpoints is treated as death rather than a threshold alert.\n'
)
if text.count(anchor) != 1:
    raise SystemExit('release-note refinement anchor was not unique')
text = text.replace(anchor, replacement, 1)
thread_anchor = '- Bank-tag search callbacks no longer read live client containers from Swing\'s event-dispatch thread.\n'
thread_replacement = thread_anchor + '- Queued Bank Tags panel updates are discarded after panel shutdown/replacement.\n'
if text.count(thread_anchor) != 1:
    raise SystemExit('Bank Tags release-note anchor was not unique')
notes.write_text(text.replace(thread_anchor, thread_replacement, 1))

print('Applied v0.3.2 wave 1 refinements')
