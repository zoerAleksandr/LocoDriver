import json, math, ssl, urllib.parse, urllib.request, xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor, as_completed

REPORT = '/tmp/crossing-report.json'
OUT = '/tmp/osm-crossing-check.json'

def dist(a,b):
    p1,p2=math.radians(a[0]),math.radians(b[0]); dp=p2-p1; dl=math.radians(b[1]-a[1])
    q=math.sin(dp/2)**2+math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 6371000*2*math.asin(math.sqrt(q))

def project(geom, target):
    pts=sorted(geom,key=lambda p:float(p['km']))
    best=(1e99,None)
    for a,b in zip(pts,pts[1:]):
        ax,ay=float(a['lon']),float(a['lat']); bx,by=float(b['lon']),float(b['lat'])
        dx,dy=bx-ax,by-ay; den=dx*dx+dy*dy
        t=max(0,min(1,((target[1]-ax)*dx+(target[0]-ay)*dy)/(den or 1)))
        p=(ay+t*dy,ax+t*dx); d=dist(target,p)
        if d<best[0]: best=(d,float(a['km'])+t*(float(b['km'])-float(a['km'])))
    return best[1]

def fetch(item):
    sec,c,geometry=item
    lat,lon=c['point']['lat'],c['point']['lon']; d=.02
    url='https://api.openstreetmap.org/api/0.6/map?'+urllib.parse.urlencode({'bbox':f'{lon-d},{lat-d},{lon+d},{lat+d}'})
    req=urllib.request.Request(url,headers={'User-Agent':'LocoDriver-crossing-audit/1.0'})
    try:
        root=ET.fromstring(urllib.request.urlopen(req,timeout=30,context=ssl._create_unverified_context()).read())
    except Exception as e:
        return {**c,'section':sec,'error':str(e)}
    nodes={n.attrib['id']:(float(n.attrib['lat']),float(n.attrib['lon']),{t.attrib['k']:t.attrib['v'] for t in n}) for n in root.findall('node')}
    ways=[]
    for w in root.findall('way'):
        tags={t.attrib['k']:t.attrib['v'] for t in w.findall('tag')}
        refs=[n.attrib['ref'] for n in w.findall('nd')]
        ways.append((refs,tags))
    candidates=[]
    for nid,(la,lo,tags) in nodes.items():
        if tags.get('railway') in ('level_crossing','crossing'):
            candidates.append((dist((lat,lon),(la,lo)),nid,(la,lo),tags))
    candidates.sort()
    if not candidates:
        return {**c,'section':sec,'osm':None}
    dd,nid,(la,lo),tags=candidates[0]
    shared=[t for refs,t in ways if nid in refs]
    roads=[t for t in shared if t.get('highway') and t.get('highway') not in ('footway','path','cycleway','track','steps')]
    axis_km=project(geometry,(la,lo))
    return {**c,'section':sec,'osm':{'node':nid,'lat':la,'lon':lo,'distance_m':dd,'axis_km':axis_km,'delta_m':(axis_km-float(c['km']))*1000 if axis_km is not None else None,'tags':tags,'road_ways':roads}}

data=json.load(open(REPORT)); items=[]
for s in data:
    for c in s['crossings']: items.append((s['name'],c,s['geometry']))
out=[]
with ThreadPoolExecutor(max_workers=8) as ex:
    fs=[ex.submit(fetch,x) for x in items]
    for f in as_completed(fs): out.append(f.result())
json.dump(out,open(OUT,'w'),ensure_ascii=False,indent=2)
print(OUT)
