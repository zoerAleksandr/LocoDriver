import json, math

REPORT='/tmp/crossing-report.json'; OSM='/tmp/osm-crossing-check.json'; OUT='/tmp/sergach-kanash-crossing-calibration.json'

def distance(a,b):
    p1,p2=math.radians(a[0]),math.radians(b[0]); dp=p2-p1; dl=math.radians(b[1]-a[1])
    q=math.sin(dp/2)**2+math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 6371000*2*math.asin(math.sqrt(q))

def point_at(geo,km):
    for a,b in zip(geo,geo[1:]):
        if a['km']<=km<=b['km']:
            t=(km-a['km'])/max(1e-9,b['km']-a['km'])
            return {'lat':a['lat']+t*(b['lat']-a['lat']),'lon':a['lon']+t*(b['lon']-a['lon'])}
    return geo[0] if km<geo[0]['km'] else geo[-1]

data={s['name']:s for s in json.load(open(REPORT))}
all_osm=json.load(open(OSM))
matches=[x for x in all_osm if x['section']=='Сергач — Канаш' and x.get('osm') and x['osm']['road_ways'] and x['osm']['distance_m']<600]
s=data['Сергач — Канаш']; geo=s['geometry']; end=float(geo[-1]['km'])
# Old geometry km -> document km using only crossings with an OSM road crossing.
anchors=[(0.0,0.0)]
for x in sorted(matches,key=lambda z:float(z['km'])):
    anchors.append((float(x['osm']['axis_km']),float(x['km'])))
# Corrections confirmed against OSM nodes / the source profile:
# 620 km 10 pk uses the OSM crossing near the old 620 km point;
# 646 km 4 pk and 649 km 6 pk use the two level crossings on road 97K-021.
anchors.extend([(73.726,73.4),(94.871,93.9),(121.261,120.3),(124.966,123.4)])
anchors.append((end,end))
anchors.sort()
clean=[]
for a in anchors:
    if clean and a[0]<=clean[-1][0]+1e-6: continue
    clean.append(a)

def remap(k):
    for (a,qa),(b,qb) in zip(clean,clean[1:]):
        if a<=k<=b:
            return qa+(k-a)*(qb-qa)/max(1e-9,b-a)
    return clean[0][1] if k<clean[0][0] else clean[-1][1]

new_geo=[dict(p,km=round(remap(float(p['km'])),3)) for p in geo]
out=[]
for x in sorted([z for z in all_osm if z['section']=='Сергач — Канаш'],key=lambda z:float(z['km'])):
    o=x.get('osm')
    if not o:
        out.append({'label':x['label'],'profile_km':x['km'],'status':'нет OSM'}); continue
    p=point_at(new_geo,float(x['km']))
    d=distance((p['lat'],p['lon']),(o['lat'],o['lon']))
    out.append({'label':x['label'],'profile_km':x['km'],'old_delta_m':round(o['delta_m']), 'new_distance_m':round(d), 'road':bool(o['road_ways'])})
stations=json.load(open('/tmp/sergach-meta.json'))['stations']
new_stations=[]
for st in stations:
    p=point_at(new_geo,float(st['km']))
    new_stations.append({'km':st['km'],'lat':round(p['lat'],6),'lon':round(p['lon'],6),'name':st['name']})
crossings=[]
for c in s['crossings']:
    if c['label'].startswith('622 км 8 пк'):
        continue
    cc=dict(c)
    if c['label'].startswith('599 км 6 пк'): cc['label']='599 км 8 пк'
    if c['label'].startswith('620 км 1 пк'): cc['label']='620 км 10 пк'
    p=point_at(new_geo,float(cc['km'])); cc['lat']=round(p['lat'],6); cc['lon']=round(p['lon'],6)
    crossings.append(cc)
p=point_at(new_geo,97.6)
crossings.append({'km':97.6,'label':'623 км 8 пк','kind':'crossing','lat':round(p['lat'],6),'lon':round(p['lon'],6)})
crossings.sort(key=lambda c:float(c['km']))
json.dump({'anchors':clean,'geometry':new_geo,'stations':new_stations,'crossings':crossings,'checks':out},open(OUT,'w'),ensure_ascii=False,indent=2)
print(OUT)
