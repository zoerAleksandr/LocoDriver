import asyncio, json
from sqlalchemy import text
from src.db.session import async_session

PAYLOAD='/tmp/sergach-kanash-crossing-calibration.json'
SID='9a0a6edc-ba64-52c5-907c-cd118e0ae8fe'

async def main():
    p=json.load(open(PAYLOAD))
    async with async_session() as s:
        await s.execute(text('''
            update advisor_section
            set geometry=cast(:geometry as jsonb),
                stations=cast(:stations as jsonb),
                crossings=cast(:crossings as jsonb),
                version=version+1, updated_at=now()
            where id=cast(:id as uuid)
        '''), {'id':SID, 'geometry':json.dumps(p['geometry'],ensure_ascii=False),
               'stations':json.dumps(p['stations'],ensure_ascii=False),
               'crossings':json.dumps(p['crossings'],ensure_ascii=False)})
        await s.commit()
        r=(await s.execute(text('''select name,version,length_km,
              jsonb_array_length(geometry),jsonb_array_length(stations),jsonb_array_length(crossings)
              from advisor_section where id=cast(:id as uuid)'''),{'id':SID})).one()
    print(r)

if __name__=='__main__': asyncio.run(main())
