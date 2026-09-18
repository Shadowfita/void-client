/** Bounded serial delivery prevents asynchronous Java calls from reordering press/move/release. */
export class BridgeQueue {
  constructor(bridge, onError = () => {}, limit = 128) {
    this.bridge = bridge; this.onError = onError; this.limit = limit;
    this.pending = []; this.running = false; this.blocked = false;
  }
  send(method, args = [], moveKey = null) {
    return new Promise((resolve, reject) => {
      if (this.blocked) { resolve(undefined); return; }
      const last = this.pending.at(-1);
      if (moveKey !== null && last?.moveKey === moveKey) {
        this.pending.pop(); last.resolve(undefined);
      }
      if (this.pending.length >= this.limit) {
        this.cancel(true); resolve(undefined); return;
      }
      this.pending.push({ method, args, moveKey, resolve, reject });
      this.pump();
    });
  }
  cancel(overflow = false) {
    for (const item of this.pending.splice(0)) item.resolve(undefined);
    if (overflow) this.blocked = true;
    return new Promise(resolve => {
      this.pending.push({ method: 'cancel', args: [], resolve: () => { if (overflow) this.blocked = false; resolve(); }, reject: resolve });
      this.pump();
    });
  }
  async pump() {
    if (this.running) return;
    this.running = true;
    try {
      while (this.pending.length) {
        const item = this.pending.shift();
        try { item.resolve(await this.bridge[item.method](...item.args)); }
        catch (error) {
          item.reject(error);
          for (const discarded of this.pending.splice(0)) discarded.resolve(undefined);
          this.blocked = true;
          try { await this.bridge.cancel(); } catch { /* The visible error remains authoritative. */ }
          this.onError(error);
          break; // Require a fresh host/session after bridge failure; do not replay stale commands.
        }
      }
    } finally { this.running = false; }
  }
}

export function hostPoint(event, rect) {
  return { x: event.clientX - rect.left, y: event.clientY - rect.top };
}
export function inside(point, viewport) {
  return !!viewport && Number.isFinite(point.x) && Number.isFinite(point.y)
    && point.x >= viewport.x && point.y >= viewport.y
    && point.x < viewport.x + viewport.width && point.y < viewport.y + viewport.height;
}
