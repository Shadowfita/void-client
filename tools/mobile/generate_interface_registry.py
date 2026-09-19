#!/usr/bin/env python3
"""Extract public Void IF3 labels/slot mappings, not permissions, scripts, cache bytes or credentials.
Usage: python3 generate_interface_registry.py UPSTREAM_ROOT OUTPUT_JSON
The upstream uses relative TOML sections ([.child]); parse each section independently.
"""
import hashlib, json, pathlib, re, sys
PIN = 'd165e5be63e3232f64f08230448089a454bd35e6'

def family(name, path):
    if 'bank_pin' in name or 'login' in name or 'password' in name: return 'sensitive'
    if 'dialogue' in name or 'dialogue' in path: return 'dialogue'
    if 'bank' in name: return 'bank'
    if 'trade' in name or 'duel' in name or 'death' in name: return 'confirmation'
    if 'equipment' in name: return 'equipment'
    if 'inventory' in name or 'price_checker' in name or 'burden' in name: return 'inventory'
    if 'shop' in name: return 'shop'
    if 'spell' in name or 'prayer' in name or 'combat' in name: return 'combat'
    if 'chat' in name or 'friend' in name or 'ignore' in name or 'notes' in name: return 'social'
    if 'toplevel' in name: return 'navigation'
    if 'map' in name: return 'map'
    if 'options' in name or 'settings' in name: return 'settings'
    if any(v in name for v in ['skill', 'quest', 'task', 'stats', 'craft', 'make', 'production']): return 'progress'
    return 'generic'

def generate(root):
    groups={}; sources={}
    for path in sorted((root/'data').rglob('*.ifaces.toml')):
        rel=str(path.relative_to(root)); raw=path.read_bytes(); sources[rel]=hashlib.sha256(raw).hexdigest()
        sections=re.split(r'(?m)^\[([^\]\n]+)\]\s*$', raw.decode('utf-8'))
        current=None
        for i in range(1,len(sections),2):
            name,body=sections[i],sections[i+1]
            # Void permits unquoted wildcard option keys, so this is not standard TOML.
            # We deliberately extract only scalar identity/layout metadata, never option permissions.
            values={}
            for line in body.splitlines():
                match=re.match(r'^\s*(id|type|inventory|width|height)\s*=\s*("[^"\n]*"|-?\d+)\s*(?:#.*)?$',line)
                if match:
                    key,value=match.groups(); values[key]=value[1:-1] if value.startswith('"') else int(value)
            if not name.startswith('.'):
                gid=values.get('id'); current=None
                if not isinstance(gid,int) or gid<0 or gid>65535: continue
                current={'id':gid,'name':name,'family':family(name,rel),'source':rel,'components':{},'type':values.get('type','')}
                # Multiple aliases of one group are retained; neither alias grants action permission.
                if str(gid) in groups: current['aliases']=groups[str(gid)].get('aliases',[])+[groups[str(gid)]['name']]
                groups[str(gid)]=current
            elif current is not None:
                cid=values.get('id'); ids=[]
                if isinstance(cid,int): ids=[cid]
                elif isinstance(cid,str) and re.fullmatch(r'\d+-\d+',cid):
                    a,b=map(int,cid.split('-')); ids=range(a,min(b,a+1024)+1)
                for n in ids:
                    if not 0<=n<=65535: continue
                    current['components'][str(n)]={'name':name[1:],'inventory':values.get('inventory',''),
                        'options':values.get('options',{}),'width':values.get('width',0),'height':values.get('height',0)}
    return {'format':1,'source':'GregHib/void','commit':PIN,'use':'Labels and layout hints only; native live action masks remain authoritative.',
        'cacheVerified':False,'sources':sources,'groups':groups}

if __name__=='__main__':
    if len(sys.argv)!=3: raise SystemExit(__doc__)
    result=generate(pathlib.Path(sys.argv[1]))
    pathlib.Path(sys.argv[2]).write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
    print(f'{len(result["groups"])} interface groups, {sum(len(g["components"]) for g in result["groups"].values())} component mappings')
