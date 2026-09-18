#!/usr/bin/env python3
"""Real Chromium shell tests with a mocked Java bridge; NOT a CheerpJ/game smoke test.
Requires Python Playwright and Chromium. Does not download or execute a runtime.
"""
import json
import os
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
MOCK = r"""
window.__calls = [];
window.__mock = { ready: true, hostSize: [0,0], viewport: {x:0,y:0,width:0,height:0}, displayScale:1, revision:1, textSession:7, status:'Test bridge ready', contextNextTap:false, root:548, menu:[], menuId:0, textAck:0, textAccepted:false, inspecting:false, widgets:[] };
window.__rejectText = false;
window.cheerpjInit = async () => {};
window.cheerpjCreateDisplay = () => {};
const bridge = {
  snapshot: async () => JSON.stringify(window.__mock),
  cancel: async () => { window.__calls.push(['cancel']); window.__mock.menu = []; window.__mock.contextNextTap = false; },
  pointer: async (...a) => {
    window.__calls.push(['pointer', ...a]);
    if (a[0] === 'up' && window.__mock.contextNextTap) {
      window.__mock.contextNextTap = false; window.__mock.menuId++;
      window.__mock.menu = Array.from({length:30},(_,i)=>({id:i,label:'Action '+i+' for a long named game target'}));
    }
  },
  wheel: async (...a) => { window.__calls.push(['wheel', ...a]); },
  setSuspended: async (v) => { window.__calls.push(['suspended',v]); },
  setTextFocus: async (v) => { window.__calls.push(['focus',v]); },
  action: async (...a) => {
    window.__calls.push(['action', ...a]);
    if (a[0] === 'context') window.__mock.contextNextTap = !window.__mock.contextNextTap;
    if (a[0] === 'select' || a[0] === 'dismiss') window.__mock.menu = [];
    if (a[0] === 'key' && [10,9,27].includes(a[1])) window.__mock.textSession++;
  },
  insertText: async (value, session) => {
    const id = ++window.__mock.textAck;
    window.__calls.push(['text',value.length,session]); // Never log user text.
    window.__mock.textAccepted = !window.__rejectText && session === window.__mock.textSession;
    window.__mock.status = window.__mock.textAccepted ? 'Text accepted' : 'Text rejected; draft retained';
    return id;
  }
};
const launcher = {
  resizeHost: async (w,h) => { window.__mock.hostSize=[w,h]; window.__mock.viewport={x:0,y:0,width:w*window.__mock.displayScale,height:h*window.__mock.displayScale}; window.__mock.revision++; },
  setDisplayScale: async (s) => { window.__mock.displayScale=s; },
  start: async () => {}
};
window.cheerpjRunLibrary = async () => ({ com:{voidclient:{mobile:{MobileBridge:bridge}}},MobileLauncher:launcher });
"""

checks = 0
def check(condition, label):
    global checks
    checks += 1
    if not condition:
        raise AssertionError(label)


def run():
    # Inline the exact checked-in sources: the test never needs network access.
    import re
    asset = ROOT/'web/mobile'
    html = (asset/'index.html').read_text()
    html = html.replace('<link rel="stylesheet" href="mobile.css">', '<style>'+(asset/'mobile.css').read_text()+'</style>')
    html = html.replace('<script src="config.js"></script>', '<script>'+(asset/'config.js').read_text()+'</script>')
    module = re.sub(r'^export ', '', (asset/'bridge-queue.mjs').read_text(), flags=re.M)
    module += re.sub(r'^import .*?;\n', '', (asset/'mobile.mjs').read_text(), count=1)
    html = html.replace('<script type="module" src="mobile.mjs"></script>', '<script type="module">'+module+'</script>')
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path=os.environ.get('CHROMIUM', '/usr/bin/chromium'), headless=True, args=['--no-sandbox'])
        for width, height in [(320,568), (390,844), (844,390), (1024,768)]:
            context = browser.new_context(viewport={'width':width,'height':height}, is_mobile=True, has_touch=True, device_scale_factor=2)
            context.add_init_script(MOCK)
            page = context.new_page()
            errors = []
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.evaluate(MOCK)
            page.set_content(html, wait_until='load')
            page.locator('#start').tap()
            page.wait_for_function("!document.querySelector('[data-tool=camera]').disabled")
            check(page.evaluate('document.documentElement.scrollWidth <= innerWidth'), f'no horizontal overflow {width}')
            check(page.evaluate('document.documentElement.scrollHeight <= innerHeight + 1'), f'no vertical overflow {width}')
            for bounds in page.locator('#dock button').evaluate_all('(buttons)=>buttons.map(b=>({w:b.getBoundingClientRect().width,h:b.getBoundingClientRect().height}))'):
                check(bounds['w'] >= 48 and bounds['h'] >= 48, f'48px dock targets {width}')
            stage = page.locator('#stage').bounding_box()
            page.touchscreen.tap(stage['x']+80, stage['y']+80)
            page.wait_for_function("__calls.filter(x=>x[0]==='pointer').length>=2")
            phases = page.evaluate("__calls.filter(x=>x[0]==='pointer').map(x=>x[1])")
            check(phases == ['down','up'], f'one real touch emits one pair {width}')
            page.locator('[data-tool=actions]').tap()
            page.wait_for_function("document.querySelector('[data-tool=actions]').getAttribute('aria-pressed')==='true'")
            check(not page.locator('#menu').evaluate('(d)=>d.open'), 'Actions arms target selection without a menu or default click')
            page.touchscreen.tap(stage['x']+80, stage['y']+80)
            page.locator('#menu[open]').wait_for()
            check(page.locator('#menu-items button').count() == 30, 'long context menu populated')
            check(page.locator('#menu-items button').evaluate_all('(bs)=>bs.every(b=>b.getBoundingClientRect().height>=48)'), '48px menu rows')
            check(page.locator('#menu').evaluate('(d)=>d.scrollHeight>d.clientHeight'), 'long menu scrolls')
            page.locator('#menu-items button').nth(29).tap()
            page.wait_for_function("__calls.some(x=>x[0]==='action'&&x[1]==='select'&&x[2]===29)")
            page.locator('#menu').wait_for(state='hidden')
            page.locator('[data-tool=text]').tap()
            check(page.locator('#compose').get_attribute('type') == 'password', 'text hidden initially')
            page.locator('#compose').fill('test text')
            page.locator('#insert').tap()
            page.wait_for_function("document.querySelector('#compose').value==='' ")
            check(True, 'accepted text clears acknowledged draft')
            page.evaluate('__rejectText = true')
            page.locator('#compose').fill('retained')
            page.locator('#insert').tap()
            page.wait_for_function("document.querySelector('#text-status').textContent.includes('rejected')")
            check(page.locator('#compose').input_value() == 'retained', 'rejected text retains draft')
            # Simulate the visible area shrinking when a keyboard opens. Layout changes must not revoke text focus.
            old_session = page.evaluate('__mock.textSession')
            page.set_viewport_size({'width':width,'height':max(280,height-160)})
            page.wait_for_timeout(250)
            check(page.evaluate('__mock.textSession') == old_session, 'resize does not invalidate text session')
            page.evaluate('__rejectText = false')
            page.locator('#insert').tap()
            page.wait_for_function("document.querySelector('#compose').value==='' ")
            page.locator('#text [data-close]').tap()
            page.set_viewport_size({'width':width,'height':height})
            page.wait_for_function("!document.querySelector('[data-tool=more]').disabled")
            page.locator('[data-tool=more]').tap()
            page.locator('#magnification').select_option('2')
            page.wait_for_function("__mock.displayScale===2 && !document.querySelector('[data-tool=more]').disabled")
            check(page.locator('#display').evaluate('(el)=>getComputedStyle(el).transform') == 'matrix(2, 0, 0, 2, 0, 0)', 'magnification uses explicit shared scale')
            page.locator('#more [data-close]').tap()
            page.evaluate('__calls = []')
            page.wait_for_timeout(150)
            stage = page.locator('#stage').bounding_box()
            page.touchscreen.tap(stage['x']+70, stage['y']+70)
            page.wait_for_function("__calls.some(x=>x[0]==='pointer'&&x[1]==='up')")
            coords = page.evaluate("__calls.find(x=>x[0]==='pointer'&&x[1]==='down').slice(3,5)")
            check(all(abs(v-70)<1 for v in coords), 'magnification does not double-scale host input')
            check(not errors, f'no uncaught browser errors: {errors}')
            if width == 390:
                out = ROOT/'build/reports/mobile'
                out.mkdir(parents=True, exist_ok=True)
                page.screenshot(path=str(out/'browser-shell-390.png'))
            context.close()
        browser.close()
    report = {'checks':checks,'result':'PASS','scope':'Real Chromium HTML/input shell with mocked Java bridge. No CheerpJ, cache, server or physical device validation.'}
    out = ROOT/'build/reports/mobile'; out.mkdir(parents=True, exist_ok=True)
    (out/'browser-shell.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report))

if __name__ == '__main__':
    run()
