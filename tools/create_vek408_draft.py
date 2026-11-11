import asyncio, json, uuid
from datetime import datetime, timezone
from sqlalchemy import select
from src.db.session import async_session
from src.models.advisor import AdvisorSection

SOURCE='993ac3c6-cf90-55cc-b930-52e0ae569f1f'
PAYLOAD='/tmp/vek408-crossing-calibration.json'

def point_at(g, km):
    for a,b in zip(g,g[1:]):
        if float(a['km']) <= km <= float(b['km']):
            t=(km-float(a['km']))/max(1e-9,float(b['km'])-float(a['km']))
            return {'lat':round(float(a['lat'])+t*(float(b['lat'])-float(a['lat'])),6),
                    'lon':round(float(a['lon'])+t*(float(b['lon'])-float(a['lon'])),6)}
    p=g[0] if km<float(g[0]['km']) else g[-1]
    return {'lat':p['lat'],'lon':p['lon']}

async def main():
    payload=json.load(open(PAYLOAD)); now=datetime.now(timezone.utc)
    async with async_session() as s:
        original=(await s.execute(select(AdvisorSection).where(AdvisorSection.id==uuid.UUID(SOURCE)))).scalars().one()
        geometry=payload['geometry']
        stations=[]
        for st in original.stations or []:
            p=point_at(geometry,float(st['km']))
            stations.append({**st,'lat':p['lat'],'lon':p['lon']})
        crossings=[]
        for c in original.crossings or []:
            p=point_at(geometry,float(c['km']))
            crossings.append({**c,'lat':p['lat'],'lon':p['lon']})
        draft=AdvisorSection(
            id=uuid.uuid4(), name='Вековка — Разъезд 408 км (черновик OSM-переезды)',
            from_station=original.from_station,to_station=original.to_station,country=original.country,
            length_km=original.length_km,km_origin=original.km_origin,km_marks=original.km_marks or [],
            km_osm=original.km_osm or {},elements=original.elements or [],profile_source=original.profile_source,
            profile_variants=original.profile_variants or {},geometry=geometry,stations=stations,
            signals=original.signals or [],crossings=crossings,brake_tests=original.brake_tests or [],
            geometry_trip_id=None,source='osm',is_verified=False,is_published=False,version=1,
            notes=(original.notes or '')+' Черновик: GPS-ось переразмечена по подтверждённым OSM-переездам; опубликованный участок не изменён.',
            created_at=now,updated_at=now)
        s.add(draft); await s.commit()
        print(str(draft.id),draft.name)

if __name__=='__main__': asyncio.run(main())
