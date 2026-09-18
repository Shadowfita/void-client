import { BridgeQueue, hostPoint, inside } from './bridge-queue.mjs';

const $ = selector => document.querySelector(selector);
const stage = $('#stage'), surface = $('#input-surface');
const pointers = new Map();
let bridge, launcher, queue, state = { ready: false }, expectedSize = [0, 0];
let failed = false, shownMenu = -1, editorSession = 0, pendingText = null;
let resizeQueued = false, displayScale = 1;

function report(error) {
  failed = true; pointers.clear();
  $('#status').textContent = `Mobile bridge stopped: ${error?.message || error}. Reload to restart.`;
  for (const button of document.querySelectorAll('#dock button')) button.disabled = true;
}
function send(method, args = [], key = null) {
  if (!queue || failed) return Promise.resolve(undefined);
  return queue.send(method, args, key).catch(report);
}
function sizeReady() { return state.ready && state.hostSize?.[0] === expectedSize[0] && state.hostSize?.[1] === expectedSize[1] && state.displayScale === displayScale; }
function cancel() { pointers.clear(); return queue?.cancel().catch(report); }
function updateVisibleHeight() {
  const height = window.visualViewport?.height || window.innerHeight;
  document.documentElement.style.setProperty('--visible-height', `${Math.round(height)}px`);
}
function resize() {
  if (resizeQueued || !launcher) return;
  resizeQueued = true;
  requestAnimationFrame(() => {
    resizeQueued = false;
    const rect = stage.getBoundingClientRect();
    const next = [Math.max(1, Math.floor(rect.width / displayScale)), Math.max(1, Math.floor(rect.height / displayScale))];
    if (next[0] === expectedSize[0] && next[1] === expectedSize[1]) return;
    expectedSize = next; cancel();
    launcher.resizeHost(...next).catch(report);
  });
}
updateVisibleHeight();
window.visualViewport?.addEventListener('resize', () => { updateVisibleHeight(); resize(); });
window.addEventListener('resize', () => { updateVisibleHeight(); resize(); });
new ResizeObserver(resize).observe(stage);

surface.addEventListener('pointerdown', event => {
  event.preventDefault(); event.stopPropagation();
  if (!sizeReady() || failed || document.querySelector('dialog[open]')) return;
  const point = hostPoint(event, stage.getBoundingClientRect());
  if (!inside(point, state.viewport)) return;
  if (event.button === 2) { send('pointer', ['context', event.pointerId, point.x, point.y, state.revision]); return; }
  if (event.button !== 0) return;
  pointers.set(event.pointerId, state.revision);
  try { surface.setPointerCapture(event.pointerId); } catch { cancel(); return; }
  surface.focus({ preventScroll: true });
  send('pointer', ['down', event.pointerId, point.x, point.y, state.revision]);
});
for (const phase of ['move', 'up']) surface.addEventListener(`pointer${phase}`, event => {
  event.preventDefault(); event.stopPropagation();
  const revision = pointers.get(event.pointerId);
  if (revision === undefined) return;
  if (!sizeReady() || revision !== state.revision) { cancel(); return; }
  const point = hostPoint(event, stage.getBoundingClientRect());
  if (!inside(point, state.viewport)) { cancel(); return; }
  if (phase === 'up') pointers.delete(event.pointerId);
  send('pointer', [phase, event.pointerId, point.x, point.y, revision], phase === 'move' ? `move:${event.pointerId}:${revision}` : null);
});
surface.addEventListener('pointercancel', () => cancel());
surface.addEventListener('lostpointercapture', event => { if (pointers.has(event.pointerId)) cancel(); });
for (const name of ['mousedown', 'mouseup', 'click', 'dblclick', 'auxclick', 'contextmenu'])
  surface.addEventListener(name, event => { event.preventDefault(); event.stopPropagation(); });
surface.addEventListener('wheel', event => {
  event.preventDefault(); event.stopPropagation();
  if (!sizeReady() || document.querySelector('dialog[open]')) return;
  const p = hostPoint(event, stage.getBoundingClientRect());
  if (inside(p, state.viewport)) {
    const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? stage.clientHeight : 1;
    send('wheel', [p.x, p.y, Math.round(event.deltaY * unit), state.revision]);
  }
}, { passive: false });
surface.addEventListener('keydown', event => {
  const camera = { ArrowLeft: 'cameraLeft', ArrowRight: 'cameraRight', ArrowUp: 'cameraUp', ArrowDown: 'cameraDown' }[event.key];
  if (camera && sizeReady()) { event.preventDefault(); send('action', [camera, 0, state.revision]); }
});
window.addEventListener('blur', () => cancel());
window.addEventListener('pagehide', () => { cancel(); bridge?.setSuspended(true).catch(report); });
window.addEventListener('pageshow', () => { if (bridge) bridge.setSuspended(false).catch(report); resize(); });
document.addEventListener('visibilitychange', () => {
  cancel(); bridge?.setSuspended(document.hidden).catch(report);
});

function closeDialog(dialog, notify = true) {
  if (dialog.open) dialog.close();
  if (dialog.id === 'text') {
    $('#compose').value = ''; pendingText = null;
    send('setTextFocus', [false]);
  }
  if (notify) cancel();
  surface.focus({ preventScroll: true });
}
for (const dialog of document.querySelectorAll('dialog')) {
  dialog.addEventListener('cancel', event => { event.preventDefault(); closeDialog(dialog); });
  dialog.querySelector('[data-close]')?.addEventListener('click', () => closeDialog(dialog));
}
function showDialog(id) {
  for (const current of document.querySelectorAll('dialog[open]')) closeDialog(current);
  cancel();
  const dialog = $(`#${id}`); dialog.showModal();
  if (id === 'text') {
    editorSession = state.textSession;
    $('#text-status').textContent = '';
    send('setTextFocus', [true]);
    $('#compose').focus(); // Within the user's activation, so mobile browsers can open the keyboard.
  }
}
for (const button of document.querySelectorAll('[data-tool]')) button.addEventListener('click', () => {
  if (!state.ready) return;
  if (button.dataset.tool === 'actions') send('action', ['context', 0, state.revision]);
  else showDialog(button.dataset.tool);
});
for (const button of document.querySelectorAll('[data-action]')) button.addEventListener('click', () => send('action', [button.dataset.action, 0, state.revision]));
for (const button of document.querySelectorAll('[data-key]')) button.addEventListener('click', () => {
  send('action', ['key', Number(button.dataset.key), editorSession]);
});
$('#show-text').addEventListener('change', () => { $('#compose').type = $('#show-text').checked ? 'text' : 'password'; });
$('#new-text-session').addEventListener('click', () => { editorSession = state.textSession; $('#text-status').textContent = 'Editor rebound to the current client field.'; });
$('#insert').addEventListener('click', async () => {
  if (pendingText || !sizeReady()) return;
  const value = $('#compose').value;
  const id = await send('insertText', [value, editorSession]);
  if (Number.isInteger(id)) pendingText = { id, value };
});
$('#compose').addEventListener('keydown', event => {
  event.stopPropagation();
  if (event.key === 'Enter' && !event.isComposing) { event.preventDefault(); $('#insert').click(); }
});
$('#magnification').addEventListener('change', async () => {
  displayScale = Number($('#magnification').value);
  document.documentElement.style.setProperty('--game-scale', String(displayScale));
  expectedSize = [0, 0]; cancel();
  try { await launcher.setDisplayScale(displayScale); resize(); } catch (error) { report(error); }
});
$('#fullscreen').addEventListener('click', async () => {
  try {
    if (document.fullscreenElement) await document.exitFullscreen();
    else if (document.documentElement.requestFullscreen) await document.documentElement.requestFullscreen();
    else $('#status').textContent = 'Fullscreen is unavailable in this browser.';
  } catch (error) { $('#status').textContent = `Fullscreen unavailable: ${error.message}`; }
});
$('#inspect').addEventListener('click', () => send('action', ['inspect', 0, state.revision]));

function render(next) {
  if (state.revision !== undefined && state.revision !== next.revision) pointers.clear();
  state = next;
  $('[data-tool="actions"]').setAttribute('aria-pressed', String(!!state.contextNextTap));
  $('#status').textContent = sizeReady() ? state.status : 'Waiting for the client viewport. Check cache/network configuration if startup does not progress.';
  for (const button of document.querySelectorAll('#dock button')) button.disabled = !sizeReady() || failed;
  $('#insert').disabled = !sizeReady() || !!pendingText;
  if (pendingText && state.textAck === pendingText.id) {
    $('#text-status').textContent = state.status;
    if (state.textAccepted && $('#compose').value === pendingText.value) $('#compose').value = '';
    pendingText = null;
  }
  if ($('#text').open && editorSession !== state.textSession) $('#text-status').textContent = 'The client field changed. Choose “Use current field” deliberately or reopen Text.';
  if ($('#more').open && state.inspecting) $('#geometry').textContent = JSON.stringify({ root: state.root, viewport: state.viewport, widgets: state.widgets }, null, 2);
  const dialog = $('#menu');
  if (state.menu?.length) {
    if (state.menuId !== shownMenu) {
      shownMenu = state.menuId;
      const items = $('#menu-items'); items.replaceChildren();
      for (const entry of state.menu) {
        const button = document.createElement('button'); button.textContent = entry.label;
        const menuId = state.menuId;
        button.addEventListener('click', () => {
          for (const b of items.querySelectorAll('button')) b.disabled = true;
          send('action', ['select', entry.id, menuId]);
        });
        items.append(button);
      }
      for (const other of document.querySelectorAll('dialog[open]')) if (other !== dialog) closeDialog(other, false);
      if (!dialog.open) dialog.showModal();
    }
  } else if (dialog.open) closeDialog(dialog, false);
}
async function loadRuntime() {
  if (typeof window.cheerpjInit === 'function') return;
  await new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://cjrtnc.leaningtech.com/4.3/loader.js';
    script.onload = resolve; script.onerror = () => reject(new Error('Could not load CheerpJ 4.3'));
    document.head.append(script);
  });
}
$('#start').addEventListener('click', async () => {
  $('#start').disabled = true;
  try {
    const config = window.VOID_MOBILE_CONFIG;
    if (!config?.jar?.startsWith('/app/') || !config.address || !Number.isInteger(config.port) || config.port < 1 || config.port > 65535)
      throw new Error('Configure a /app/ JAR path, server address and port in config.js');
    await loadRuntime();
    const options = config.runtimeOptions || {};
    await window.cheerpjInit({ ...options, version: 8, javaProperties: [...(options.javaProperties || []), 'void.mobile=true', 'void.mobile.browser=true'] });
    window.cheerpjCreateDisplay(-1, -1, $('#display'));
    const lib = await window.cheerpjRunLibrary(config.jar);
    bridge = await lib.com.voidclient.mobile.MobileBridge;
    launcher = await lib.MobileLauncher;
    queue = new BridgeQueue(bridge, report);
    const rect = stage.getBoundingClientRect(); expectedSize = [Math.floor(rect.width), Math.floor(rect.height)];
    await launcher.resizeHost(...expectedSize);
    await launcher.start(config.address, config.port);
    $('#launch').hidden = true;
    async function poll() {
      if (failed) return;
      try { render(JSON.parse(await bridge.snapshot())); }
      catch (error) { report(error); return; }
      setTimeout(poll, document.hidden ? 1000 : 100);
    }
    poll(); resize();
  } catch (error) { report(error); }
});
