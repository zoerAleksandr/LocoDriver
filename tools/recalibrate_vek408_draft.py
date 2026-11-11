import json, math

REPORT='/tmp/crossing-report.json'; OSM='/tmp/osm-crossing-check.json'; OUT='/tmp/vek408-crossing-calibration.json'

def dist(a,b):
    p1,p2=math.radians(a[0]),math.radians(b[0]); dp=p2-p1; dl=math.radians(b[1]-a[1])
    q=math.sin(dp/2)**2+math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 6371000*2*math.asin(math.sqrt(q))

def point_at(g,km):
    for a,b in zip(g,g[1:]):
        if a['km']<=km<=b['km']:
            t=(km-a['km'])/max(1e-9,b['km']-a['km'])
            return {'lat':a['lat']+t*(b['lat']-a['lat']),'lon':a['lon']+t*(b['lon']-a['lon'])}
    return g[0] if km<g[0]['km'] else g[-1]

data={s['name']:s for s in json.load(open(REPORT))}; all_osm=json.load(open(OSM))
s=data['Вековка — Разъезд 408 км']; geo=s['geometry']; end=float(geo[-1]['km'])
raw=[x for x in all_osm if x['section']==s['name'] and x.get('osm') and x['osm']['road_ways']]
raw.sort(key=lambda x:float(x['osm']['axis_km']))
anchors=[(0.0,0.0)]
for x in raw:
    a=float(x['osm']['axis_km']); q=float(x['km'])
    if a>anchors[-1][0]+0.2: anchors.append((a,q))
anchors.append((end,end))

def remap(k):
    for (a,qa),(b,qb) in zip(anchors,anchors[1:]):
        if a<=k<=b: return qa+(k-a)*(qb-qa)/max(1e-9,b-a)
    return anchors[0][1] if k<anchors[0][0] else anchors[-1][1]

new_geo=[dict(p,km=round(remap(float(p['km'])),3)) for p in geo]
checks=[]
for x in sorted([z for z in all_osm if z['section']==s['name']],key=lambda z:float(z['km'])):
    o=x.get('osm')
    if not o: checks.append({'label':x['label'],'old_km':x['km'],'status':'нет OSM'}); continue
    p=point_at(new_geo,float(x['km']))
    checks.append({'label':x['label'],'old_km':x['km'],'old_delta_m':round(o['delta_m']),
                   'new_distance_m':round(dist((p['lat'],p['lon']),(o['lat'],o['lon']))),
                   'road':bool(o['road_ways'])})
json.dump({'anchors':anchors,'geometry':new_geo,'checks':checks},open(OUT,'w'),ensure_ascii=False,indent=2)
print(OUT)
