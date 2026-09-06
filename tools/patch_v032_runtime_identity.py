from pathlib import Path


path = Path('client/src/net/runelite/client/RuneLite.java')
data = path.read_bytes()


def insert_after_line(data, anchor, inserted, label):
    if data.count(anchor) != 1:
        raise SystemExit(f'{label}: expected one anchor, found {data.count(anchor)}')
    start = data.index(anchor) + len(anchor)
    if data[start:start + 2] == b'\r\n':
        ending = b'\r\n'
    elif data[start:start + 1] == b'\n':
        ending = b'\n'
    else:
        raise SystemExit(f'{label}: anchor is not followed by a line ending')
    # Preserve the anchor's existing ending byte-for-byte, but keep the newly
    # inserted line LF-only so git diff --check does not treat CR as whitespace.
    return data[:start + len(ending)] + inserted + b'\n' + data[start + len(ending):]


if b'import net.runelite.client.util.BuildIdentity;' in data:
    raise SystemExit('BuildIdentity import unexpectedly already exists')
data = insert_after_line(
    data,
    b'import net.runelite.client.ui.overlay.tooltip.TooltipOverlay;',
    b'import net.runelite.client.util.BuildIdentity;',
    'BuildIdentity import',
)

if b'Runtime artifact:' in data:
    raise SystemExit('Runtime artifact log unexpectedly already exists')
data = insert_after_line(
    data,
    b'\t\t\t\targs.length == 0 ? "none" : String.join(" ", args));',
    b'\t\t\tlog.info("Runtime artifact: {}", BuildIdentity.describe(RuneLite.class));',
    'runtime artifact log',
)

path.write_bytes(data)
print('Applied byte-preserving RuneLite runtime identity patch')
