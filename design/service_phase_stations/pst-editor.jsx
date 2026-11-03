// Плечи · промежуточные станции — список плеч, редактор плеча, шторка станции.

// ─── Настройки → Плечи ─────────────────────────────────────────────────
const PST_LIST = [
  { from: 'Гатчина-Варшавская', to: 'Луга I', km: 96, via: 6, noCoords: 1 },
  { from: 'Луга I', to: 'Гатчина-Варшавская', km: 96, via: 6, noCoords: 1 },
  { from: 'Санкт-Петербург-Товарный-Витебский', to: 'Луга I', km: 139, via: 38 },
  { from: 'Луга I', to: 'Санкт-Петербург-Товарный-Витебский', km: 139, via: 38 },
  { from: 'Лужская', to: 'Луга I', km: 250, via: 14, noCoords: 3 },
  { from: 'Луга I', to: 'Лужская', km: 250, via: 14, noCoords: 3 },
  { from: 'Луга I', to: 'Батецкая', km: 43, via: 0 },
  { from: 'Батецкая', to: 'Луга I', km: 43, via: 0 },
];

function PstShoulderRow({ t, r }) {
  return (
    <button style={{
      width: '100%', textAlign: 'left', padding: '12px 16px 12px 18px', background: 'transparent', border: 'none',
      display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer',
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text, lineHeight: 1.3, overflowWrap: 'anywhere' }}>{r.from} — {r.to}</div>
        <div style={{ ...M.t.captionMuted(t), marginTop: 3, display: 'flex', flexWrap: 'wrap', columnGap: 6 }}>
          <span style={{ whiteSpace: 'nowrap' }}>{r.via ? `через ${r.via} ${pstPlural(r.via, PST_ST)}` : 'без промежуточных'}</span>
          {r.noCoords > 0 && <span style={{ color: t.warning, whiteSpace: 'nowrap' }}>· {r.noCoords} без координат</span>}
        </div>
      </div>
      <span style={{ fontFamily: M.fontMono, fontSize: 14, fontWeight: 600, color: t.textMuted, flexShrink: 0 }}>{r.km} км</span>
      <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </button>
  );
}

function PstShouldersScreen({ dark = false, height = 844, children }) {
  const t = dark ? M.dark : M.light;
  return (
    <ADevice dark={dark} height={height}>
      <TopBarAndroid t={t} title="Плечи" back/>
      <div style={{ overflow: 'hidden', height: 'calc(100% - 64px)', padding: '4px 16px 28px' }}>
        <GroupHead t={t} mt={8}>Плечи · {PST_LIST.length}</GroupHead>
        <MCard t={t}>
          {PST_LIST.map((r, i) => (
            <React.Fragment key={i}>
              {i > 0 && <Sep t={t} inset={18}/>}
              <PstShoulderRow t={t} r={r}/>
            </React.Fragment>
          ))}
        </MCard>
        <SectionNote t={t}>Расстояние и станции подставляются в форму поезда при выборе плеча.</SectionNote>
        <AddBtn t={t} icon={<IcPlus/>} label="Добавить плечо"/>
      </div>
      {typeof children === 'function' ? children(t) : children}
    </ADevice>
  );
}

// ─── Редактор плеча ────────────────────────────────────────────────────
//   state: 'empty' | 'empty-disabled' | 'short' | 'long' | 'reorder' | 'fresh'
function PstEditorSheet({ t, dark, mode = 'edit', state = 'short', scroll = 0, z = 70, snackbar }) {
  const isNew = mode === 'new';
  const d = state === 'long' ? PST_B : PST_A;
  const noTo = state === 'empty-disabled';
  const empty = state === 'empty' || state === 'empty-disabled';
  const stops = state === 'long' ? PST_LONG
    : state === 'fresh' ? PST_SHORT.map((s) => ({ ...s, fresh: s.src === 'osm' })) : PST_SHORT;
  const count = empty ? 0 : stops.length;
  const pin = <IcMapPin width="18" height="18" style={{ color: t.accent, flexShrink: 0 }}/>;
  const pinFaint = <IcMapPin width="18" height="18" style={{ color: t.textFaint, flexShrink: 0 }}/>;
  const reorder = state === 'reorder';

  const footer = <PstBtn t={t} kind="cta" h={52} full>{isNew ? 'Добавить' : 'Сохранить'}</PstBtn>;

  return (
    <PstSheet t={t} z={z} title={isNew ? 'Новое плечо' : 'Редактировать плечо'} scroll={scroll} footer={footer}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <PstField t={t} label="От станции" value={d.from} leading={pin}/>
        <PstField t={t} label="До станции" value={noTo ? '' : d.to} placeholder="Начните вводить название" leading={noTo ? pinFaint : pin}/>
        <div style={{ display: 'flex', gap: 8 }}>
          <PstField t={t} flex={1} label="Расстояние, км" value={noTo ? '' : String(d.km)} placeholder="—"/>
          <PstField t={t} flex={1} label="Доплата за пробег, ₽/км" value={d.pay}/>
        </div>
      </div>

      {isNew && (
        <React.Fragment>
          <div style={{ marginTop: 12 }}>
            <MCard t={t}><SwitchRow t={t} label="Создать плечо в обратном направлении" on={true} mat/></MCard>
          </div>
          <SectionNote t={t}>Промежуточные станции тоже скопируются — в обратном порядке.</SectionNote>
        </React.Fragment>
      )}

      <GroupHead t={t} mt={22} trailing={!empty && (
        <PstBtn t={t} kind="text" h={32} style={{ fontSize: 14, marginRight: -8 }}>{reorder ? 'Готово' : 'Изменить'}</PstBtn>
      )}>Промежуточные станции · {count}</GroupHead>

      {empty ? (
        <MCard t={t}>
          <div style={{ padding: '16px 16px 16px' }}>
            <div style={{ ...M.t.hint(t), fontSize: 14, color: t.textMuted, textWrap: 'pretty' }}>
              Станции между крайними — по порядку от отправления к прибытию. Найдём их по карте или добавьте вручную.
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 14 }}>
              <PstBtn t={t} kind="filled" full disabled={noTo} icon={<IcSearch width="18" height="18"/>}>Найти автоматически</PstBtn>
              <PstBtn t={t} kind="outlined" full icon={<IcPlus width="18" height="18"/>}>Добавить вручную</PstBtn>
            </div>
            {noTo && <div style={{ ...M.t.captionMuted(t), marginTop: 10, textAlign: 'center' }}>Для автоподбора укажите обе крайние станции</div>}
          </div>
        </MCard>
      ) : (
        <React.Fragment>
          <MCard t={t}>
            <PstLine t={t} from={d.from} to={d.to} km={d.km} stops={stops} mode={reorder ? 'edit' : 'view'}
              collapse={null} lifted={reorder ? 4 : null} insertActive={null}/>
          </MCard>
          {reorder ? (
            <SectionNote t={t}>Перетащите за ≡, чтобы изменить порядок. «+» — вставить станцию между соседними, ✕ — убрать.</SectionNote>
          ) : (
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <PstBtn t={t} kind="tonal" full="flex" icon={<IcSearch width="17" height="17"/>} style={{ padding: '0 12px' }}>Найти автоматически</PstBtn>
              <PstBtn t={t} kind="tonal" icon={<IcPlus width="17" height="17"/>} style={{ padding: '0 16px' }}>Станция</PstBtn>
            </div>
          )}
        </React.Fragment>
      )}

      {!isNew && !reorder && (
        <div style={{ marginTop: 18, display: 'flex', justifyContent: 'center' }}>
          <PstBtn t={t} kind="danger" icon={<IcTrash width="16" height="16"/>}>Удалить плечо</PstBtn>
        </div>
      )}
      {snackbar && <PstSnackbar t={t} dark={dark} text={snackbar} action="Отменить"/>}
    </PstSheet>
  );
}

// ─── M3 segmented button ───────────────────────────────────────────────
function PstSegmented({ t, options, active }) {
  return (
    <div style={{ display: 'flex', height: 40, borderRadius: 20, border: `1px solid ${t.borderStrong}`, overflow: 'hidden' }}>
      {options.map((o, i) => (
        <div key={o} style={{
          flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          background: i === active ? t.chipBgActive : 'transparent',
          borderLeft: i ? `1px solid ${t.borderStrong}` : 'none',
          fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text,
        }}>
          {i === active && <IcCheck width="16" height="16"/>}{o}
        </div>
      ))}
    </div>
  );
}

// ─── Шторка станции ────────────────────────────────────────────────────
//   variant: 'osm' | 'manual' | 'searching' | 'found' | 'notfound' | 'new'
function PstStationSheet({ t, variant = 'osm', z = 80 }) {
  const osm = variant === 'osm';
  const isNew = variant === 'new';
  const name = osm ? 'Сиверская' : isNew ? 'Мши' : 'Кемка';
  const type = osm ? 0 : isNew ? 0 : 1;

  const coordIcon = (c) => (
    <span style={{ width: 36, height: 36, borderRadius: 10, flexShrink: 0, background: hexToRgba(c, .14), color: c, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
      <IcMapPin width="18" height="18"/>
    </span>
  );
  const coordRow = (icon, title, sub, trailing) => (
    <div style={{ padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
      {icon}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>{title}</div>
        {sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2, textWrap: 'pretty' }}>{sub}</div>}
      </div>
      {trailing}
    </div>
  );

  let coords = null;
  if (osm) coords = coordRow(coordIcon(t.success), 'Координаты есть', 'Станция определится по GPS');
  if (variant === 'manual') coords = (
    <React.Fragment>
      {coordRow(coordIcon(t.warning), 'Нет координат', 'Без них станция не определится по GPS')}
      <div style={{ padding: '0 14px 14px' }}><PstBtn t={t} kind="tonal" full icon={<IcSearch width="17" height="17"/>}>Найти координаты</PstBtn></div>
    </React.Fragment>
  );
  if (variant === 'searching') coords = coordRow(
    <span style={{ width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><PstSpinner t={t} size={22}/></span>,
    'Ищем «Кемка» на карте…', 'Обычно несколько секунд',
    <PstBtn t={t} kind="text" h={36} style={{ fontSize: 14 }}>Отменить</PstBtn>);
  if (variant === 'found') coords = (
    <React.Fragment>
      {coordRow(coordIcon(t.success), 'Нашлось: Кемка, О.П.', 'Октябрьская ж. д. · между Мшинская и Толмачёво')}
      <div style={{ padding: '0 14px 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ ...M.t.captionMuted(t), flex: 1 }}>Сохранится вместе со станцией</span>
        <PstBtn t={t} kind="text" h={36} style={{ fontSize: 14 }}>Не та станция</PstBtn>
      </div>
    </React.Fragment>
  );
  if (variant === 'notfound') coords = (
    <React.Fragment>
      {coordRow(coordIcon(t.warning), 'Не нашлось на карте', 'Проверьте название или укажите точку на карте вручную.')}
      <div style={{ padding: '0 14px 14px' }}><PstBtn t={t} kind="outlined" full>Искать ещё раз</PstBtn></div>
    </React.Fragment>
  );

  const footer = (
    <React.Fragment>
      <PstBtn t={t} kind="cta" h={52} full>Готово</PstBtn>
      {!isNew && <PstBtn t={t} kind="danger" h={44} full icon={<IcTrash width="16" height="16"/>}>Удалить станцию</PstBtn>}
    </React.Fragment>
  );

  return (
    <PstSheet t={t} z={z} height="auto" title={isNew ? 'Новая станция' : 'Станция'} footer={footer}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div>
          <PstField t={t} label="Название" value={name} focus={isNew}
            trailing={isNew && <span style={{ color: t.textMuted, display: 'inline-flex' }}><PstIcClose width="18" height="18"/></span>}/>
          {isNew && (
            <div style={{ marginTop: 6, background: t.surface, borderRadius: M.r.cardAndroid, boxShadow: M.shadow.md, overflow: 'hidden' }}>
              {[['Мши', 'нская', 'из справочника · станция'], ['Мши', 'нская-Товарная', 'из справочника · станция']].map(([a, b, s], i) => (
                <React.Fragment key={i}>
                  {i > 0 && <Sep t={t} inset={16}/>}
                  <div style={{ padding: '11px 16px' }}>
                    <div style={{ fontFamily: M.fontSans, fontSize: 15, color: t.text }}><b style={{ fontWeight: 700 }}>{a}</b>{b}</div>
                    <div style={{ ...M.t.captionMuted(t), marginTop: 1 }}>{s}</div>
                  </div>
                </React.Fragment>
              ))}
            </div>
          )}
        </div>
        <PstSegmented t={t} options={['Станция', 'О.П.']} active={type}/>
        <div>
          <PstField t={t} label="Км от начала плеча" value={osm ? '25,1' : ''} placeholder="Не указано" suffix={osm ? 'км' : null}/>
          <div style={{ ...M.t.captionMuted(t), padding: '6px 4px 0', textWrap: 'pretty' }}>Необязательно. Помогает поставить станцию на место при повторном подборе.</div>
        </div>
        {!isNew && (
          <MCard t={t}>
            {coords}
            <Sep t={t} inset={0}/>
            <button style={{ width: '100%', minHeight: 48, padding: '0 14px', border: 'none', background: 'transparent', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left' }}>
              <span style={{ width: 36, display: 'inline-flex', justifyContent: 'center', color: t.accent, flexShrink: 0 }}><IcMapPin width="18" height="18"/></span>
              <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.accent }}>{osm ? 'Уточнить на карте' : 'Указать на карте'}</span>
              <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
            </button>
          </MCard>
        )}
        {osm && (
          <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textFaint, padding: '0 4px', letterSpacing: 0.2 }}>
            Добавлена автоподбором · ЕСР 030329
          </div>
        )}
      </div>
    </PstSheet>
  );
}

// ─── Выбор точки на карте (полноэкранно, как в навигаторе) ─────────────
//   Карта двигается под неподвижной меткой в центре. «Выбрать» — сохранить точку и закрыть.
function PstMapPicker({ dark = false, height = 844, station = 'Кемка', type = 'О.П.' }) {
  const t = dark ? M.dark : M.light;
  const fab = (child, label) => (
    <button aria-label={label} style={{ width: 48, height: 48, borderRadius: 16, border: 'none', background: t.surface, color: t.text, boxShadow: M.shadow.md, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', fontFamily: M.fontSans, fontSize: 22, fontWeight: 400 }}>{child}</button>
  );
  return (
    <ADevice dark={dark} height={height}>
      <div style={{ position: 'absolute', inset: 0, background: `repeating-linear-gradient(135deg, ${t.bgSubtle} 0 14px, ${t.surfaceAlt} 14px 28px)` }}>
        <div style={{ position: 'absolute', left: 0, right: 0, top: '24%', textAlign: 'center', fontFamily: M.fontMono, fontSize: 11, color: t.textFaint, letterSpacing: 1, textTransform: 'uppercase' }}>карта · тайлы, жесты: сдвиг, зум</div>
      </div>
      {/* верх: назад + станция */}
      <div style={{ position: 'absolute', top: 8, left: 12, right: 12, zIndex: 5, display: 'flex', gap: 8, alignItems: 'center' }}>
        {fab(<IcChevronLeft width="22" height="22"/>, 'Назад')}
        <div style={{ flex: 1, minWidth: 0, height: 48, borderRadius: 16, background: t.surface, boxShadow: M.shadow.md, padding: '0 16px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{station}</div>
          <div style={{ ...M.t.captionMuted(t), marginTop: 1 }}>Укажите место станции</div>
        </div>
      </div>
      {/* метка по центру */}
      <div style={{ position: 'absolute', left: '50%', top: '42%', zIndex: 4, transform: 'translate(-50%, -100%)', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <span style={{ width: 44, height: 44, borderRadius: 22, background: t.accent, color: t.accentInk, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', boxShadow: M.shadow.fab }}><IcMapPin width="22" height="22"/></span>
        <span style={{ width: 2, height: 14, background: t.accent }}></span>
        <span style={{ width: 10, height: 10, borderRadius: 5, background: t.accent, boxShadow: `0 0 0 6px ${hexToRgba(t.accent, .2)}` }}></span>
      </div>
      {/* зум + моё местоположение */}
      <div style={{ position: 'absolute', right: 12, top: '46%', zIndex: 5, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {fab('+', 'Приблизить')}
        {fab('−', 'Отдалить')}
        {fab(<span style={{ width: 14, height: 14, borderRadius: 7, border: `2px solid ${t.accent}`, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}><span style={{ width: 4, height: 4, borderRadius: 2, background: t.accent }}></span></span>, 'Моё местоположение')}
      </div>
      {/* низ: карточка + Выбрать */}
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 6, background: t.bg, borderTopLeftRadius: M.r.sheetAndroid, borderTopRightRadius: M.r.sheetAndroid, boxShadow: M.shadow.sheet, padding: '16px 16px 14px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ width: 36, height: 36, borderRadius: 10, background: t.accentSoft, color: t.accent, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><IcMapPin width="18" height="18"/></span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>{station} · {type}</div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>Двигайте карту — точка под меткой станет координатами станции</div>
          </div>
        </div>
        <PstBtn t={t} kind="cta" h={52} full>Выбрать</PstBtn>
      </div>
    </ADevice>
  );
}

Object.assign(window, { PstMapPicker, PST_LIST, PstShoulderRow, PstShouldersScreen, PstEditorSheet, PstSegmented, PstStationSheet });
