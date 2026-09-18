import test from 'node:test';
import assert from 'node:assert/strict';
import { BridgeQueue, hostPoint, inside } from '../bridge-queue.mjs';
const defer = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
const flush = () => new Promise(resolve => setImmediate(resolve));

test('serial delivery preserves down / latest move / up', async () => {
  const gate = defer(), calls = [];
  const q = new BridgeQueue({ pointer: async phase => { calls.push(phase); if (phase === 'down') await gate.promise; }, cancel: async () => calls.push('cancel') });
  const down = q.send('pointer', ['down']);
  const first = q.send('pointer', ['old move'], 'p1');
  const latest = q.send('pointer', ['latest move'], 'p1');
  const up = q.send('pointer', ['up']);
  gate.resolve(); await Promise.all([down, first, latest, up]);
  assert.deepEqual(calls, ['down', 'latest move', 'up']);
});
test('moves never coalesce across another pointer boundary', async () => {
  const gate = defer(), calls = [];
  const q = new BridgeQueue({ event: async value => { calls.push(value); if (value === 'start') await gate.promise; } });
  const all = [q.send('event', ['start']), q.send('event', [1], 'p1'), q.send('event', [2], 'p2'), q.send('event', [3], 'p1')];
  gate.resolve(); await Promise.all(all); assert.deepEqual(calls, ['start', 1, 2, 3]);
});
test('overflow replaces queued actions with a cancellation barrier', async () => {
  const gate = defer(), calls = [];
  const q = new BridgeQueue({ event: async v => { calls.push(v); if (v === 0) await gate.promise; }, cancel: async () => calls.push('cancel') }, () => {}, 3);
  const all = [];
  for (let i = 0; i < 10; i++) all.push(q.send('event', [i]));
  gate.resolve(); await Promise.all(all); await flush();
  assert.deepEqual(calls, [0, 'cancel']); assert.equal(q.blocked, false);
});
test('explicit cancellation does not reorder a subsequent fresh action', async () => {
  const gate = defer(), calls = [];
  const q = new BridgeQueue({ event: async v => { calls.push(v); if (v === 'inflight') await gate.promise; }, cancel: async () => calls.push('cancel') });
  const a = q.send('event', ['inflight']), discarded = q.send('event', ['old target']);
  const barrier = q.cancel(), fresh = q.send('event', ['fresh action']);
  gate.resolve(); await Promise.all([a, discarded, barrier, fresh]);
  assert.deepEqual(calls, ['inflight', 'cancel', 'fresh action']);
});
test('bridge failure cancels and prevents stale replay', async () => {
  const calls = [], errors = [];
  const q = new BridgeQueue({ event: async () => { throw new Error('offline'); }, cancel: async () => calls.push('cancel') }, e => errors.push(e.message));
  await assert.rejects(q.send('event'), /offline/); await flush();
  await q.send('event');
  assert.deepEqual(calls, ['cancel']); assert.deepEqual(errors, ['offline']); assert.equal(q.blocked, true);
});
test('host coordinates subtract the content origin, not screen or device pixels', () => {
  assert.deepEqual(hostPoint({ clientX: 180, clientY: 160 }, { left: 30, top: 40 }), { x: 150, y: 120 });
});
test('letterbox, safe-area and invalid coordinates do not become edge clicks', () => {
  const v = { x: 20, y: 30, width: 400, height: 800 };
  assert.equal(inside({ x: 20, y: 30 }, v), true);
  for (const p of [{ x: 19, y: 40 }, { x: 420, y: 40 }, { x: 40, y: 830 }, { x: NaN, y: 40 }, { x: 40, y: Infinity }]) assert.equal(inside(p, v), false);
});
test('an acknowledgement value is returned without serializing text into logs', async () => {
  const q = new BridgeQueue({ insertText: async () => 17 });
  assert.equal(await q.send('insertText', ['private text', 9]), 17);
});
