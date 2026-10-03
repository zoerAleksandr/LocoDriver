// iOS · Плечи — список плеч, редактор плеча, шторка станции, action sheet удаления.

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

function IpShoulderRow({ t, r }) {
  return (
    <button style={{ width: '100%', textAlign: 'left', padding: '12px 16px 12px 18px', background: 'transparent', border: 'none', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}>
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

function IpShouldersScreen({ dark = false, height = 844, children }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 54 }}>
        <NavBarIOS t={t} title="Плечи" back backLabel="Настройки" style="text" trailing="Изменить"/>
      </div>
      <div style={{ overflow: 'hidden', height: 'calc(100% - 99px)', padding: '4px 16px 28px' }}>
        <GroupHead t={t} mt={8}>Плечи · {PST_LIST.length}</GroupHead>
        <Card t={t}>
          {PST_LIST.map((r, i) => (
            <React.Fragment key={i}>
              {i > 0 && <Sep t={t} inset={18}/>}
              <IpShoulderRow t={t} r={r}/>
            </React.Fragment>
          ))}
        </Card>
        <SectionNote t={t}>Расстояние и станции подставляются в форму поезда при выборе плеча.</SectionNote>
        <AddBtn t={t} icon={<IcPlus/>} label="Добавить плечо"/>
      </div>
      {typeof children === 'function' ? children(t) : children}
    </MDevice>
  );
}

// ─── Редактор плеча ───────────────────────────────────────────────────
//   state: 'empty' | 'empty-disabled' | 'short' | 'long' | 'reorder' | 'fresh'
function IpEditorSheet({ t, dark, mode = 'edit', state = 'short', scroll = 0, z = 70, toast }) {
  const isNew = mode === 'new';
  const d = state === 'long' ? PST_B : PST_A;
  const noTo = state === 'empty-disabled';
  const empty = state === 'empty' || state === 'empty-disabled';
  const stops = state === 'long' ? PST_LONG : state === 'fresh' ? PST_SHORT.map((s) => ({ ...s, fresh: s.src === 'osm' })) : PST_SHORT;
  const count = empty ? 0 : stops.length;
  const reorder = state === 'reorder';
  const pin = (on) => <IcMapPin width="18" height="18" style={{ color: on ? t.accent : t.textFaint, flexShrink: 0 }}/>;

  return (
    <IpSheet t={t} z={z} title={isNew ? 'Новое плечо' : 'Редактировать плечо'} right={isNew ? 'Добавить' : 'Сохранить'} rightDisabled={noTo} scroll={scroll}>
      <Card t={t}>
        <IpFieldRow t={t} label="От станции" value={d.from} leading={pin(true)}/>
        <Sep t={t} inset={44}/>
        <IpFieldRow t={t} label="До станции" value={noTo ? '' : d.to} placeholder="Начните вводить название" leading={pin(!noTo)}/>
      </Card>
      <div style={{ marginTop: 12 }}>
        <Card t={t}>
          <div style={{ display: 'flex' }}>
            <IpFieldRow t={t} flex={1} label="Расстояние, км" value={noTo ? '' : String(d.km)} placeholder="—"/>
            <div style={{ width: 1, background: t.border, margin: '10px 0' }}></div>
            <IpFieldRow t={t} flex={1} label="Доплата за пробег, ₽/км" value={d.pay}/>
          </div>
        </Card>
      </div>

      {isNew && (
        <React.Fragment>
          <div style={{ marginTop: 12 }}><Card t={t}><SwitchRow t={t} label="Создать плечо в обратном направлении" on={true}/></Card></div>
          <SectionNote t={t}>Промежуточные станции тоже скопируются — в обратном порядке.</SectionNote>
        </React.Fragment>
      )}

      <GroupHead t={t} mt={22} trailing={!empty && (
        <button style={{ border: 'none', background: 'transparent', padding: '4px 4px', cursor: 'pointer', fontFamily: M.fontSans, fontSize: 15, fontWeight: reorder ? 600 : 400, color: t.accent }}>{reorder ? 'Готово' : 'Изменить'}</button>
      )}>Промежуточные станции · {count}</GroupHead>

      {empty ? (
        <Card t={t}>
          <div style={{ padding: 16 }}>
            <div style={{ ...M.t.hint(t), fontSize: 14, textWrap: 'pretty' }}>Станции между крайними — по порядку от отправления к прибытию. Найдём их по карте или добавьте вручную.</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 14 }}>
              <IpBtn t={t} kind="primary" full disabled={noTo} icon={<IcSearch width="18" height="18"/>}>Найти автоматически</IpBtn>
              <IpBtn t={t} kind="secondary" full icon={<IcPlus width="18" height="18"/>}>Добавить вручную</IpBtn>
            </div>
            {noTo && <div style={{ ...M.t.captionMuted(t), marginTop: 10, textAlign: 'center' }}>Для автоподбора укажите обе крайние станции</div>}
          </div>
        </Card>
      ) : (
        <React.Fragment>
          <Card t={t}>
            <IpLine t={t} from={d.from} to={d.to} km={d.km} stops={stops} mode={reorder ? 'edit' : 'view'} lifted={reorder ? 4 : null}/>
          </Card>
          {reorder ? (
            <SectionNote t={t}>Перетащите за ≡, чтобы изменить порядок. «+» — вставить станцию между соседними, ⊖ — убрать.</SectionNote>
          ) : (
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <IpBtn t={t} kind="secondary" full="flex" icon={<IcSearch width="17" height="17"/>} style={{ padding: '0 12px' }}>Найти автоматически</IpBtn>
              <IpBtn t={t} kind="secondary" icon={<IcPlus width="17" height="17"/>}>Станция</IpBtn>
            </div>
          )}
        </React.Fragment>
      )}

      {!isNew && !reorder && (
        <div style={{ marginTop: 18, display: 'flex', justifyContent: 'center' }}>
          <IpBtn t={t} kind="destructive" h={44} icon={<IcTrash width="16" height="16"/>}>Удалить плечо</IpBtn>
        </div>
      )}
      {toast && <IpToast t={t} dark={dark} text={toast} action="Отменить"/>}
    </IpSheet>
  );
}

// ─── Шторка станции ───────────────────────────────────────────────────
//   variant: 'osm' | 'manual' | 'searching' | 'found' | 'notfound' | 'new'
function IpStationSheet({ t, variant = 'osm', z = 80 }) {
  const osm = variant === 'osm', isNew = variant === 'new';
  const name = osm ? 'Сиверская' : isNew ? 'Мши' : 'Кемка';
  const type = osm || isNew ? 0 : 1;
  const sq = (c) => (
    <span style={{ width: 36, height: 36, borderRadius: 10, flexShrink: 0, background: hexToRgba(c, .14), color: c, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}><IcMapPin width="18" height="18"/></span>
  );
  const row = (icon, title, sub, trailing) => (
    <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
      {icon}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>{title}</div>
        {sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2, textWrap: 'pretty' }}>{sub}</div>}
      </div>
      {trailing}
    </div>
  );
  const pad = (node, p = '0 16px 14px') => <div style={{ padding: p }}>{node}</div>;
  let coords = null;
  if (osm) coords = row(sq(t.success), 'Координаты есть', 'Станция определится по GPS');
  if (variant === 'manual') coords = <React.Fragment>{row(sq(t.warning), 'Нет координат', 'Без них станция не определится по GPS')}{pad(<IpBtn t={t} kind="secondary" full h={44} fs={15} icon={<IcSearch width="17" height="17"/>}>Найти координаты</IpBtn>)}</React.Fragment>;
  if (variant === 'searching') coords = row(<span style={{ width: 36, height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><IpSpinner t={t} size={20}/></span>, 'Ищем «Кемка» на карте…', 'Обычно несколько секунд', <IpBtn t={t} kind="plain" h={36}>Отменить</IpBtn>);
  if (variant === 'found') coords = <React.Fragment>{row(sq(t.success), 'Нашлось: Кемка, О.П.', 'Октябрьская ж. д. · между Мшинская и Толмачёво')}{pad(<div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><span style={{ ...M.t.captionMuted(t), flex: 1 }}>Сохранится вместе со станцией</span><IpBtn t={t} kind="plain" h={36}>Не та станция</IpBtn></div>, '0 16px 10px 64px')}</React.Fragment>;
  if (variant === 'notfound') coords = <React.Fragment>{row(sq(t.warning), 'Не нашлось на карте', 'Проверьте название или укажите точку на карте вручную.')}{pad(<IpBtn t={t} kind="secondary" full h={44} fs={15}>Искать ещё раз</IpBtn>)}</React.Fragment>;

  return (
    <IpSheet t={t} z={z} height="auto" title={isNew ? 'Новая станция' : 'Станция'} right="Готово" rightDisabled={false}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div>
          <Card t={t}>
            <IpFieldRow t={t} label="Название" value={name} focus={isNew} trailing={isNew && <span style={{ color: t.textFaint, display: 'inline-flex' }}><IpIcClear/></span>}/>
          </Card>
          {isNew && (
            <div style={{ marginTop: 6, background: t.surface, borderRadius: M.r.cardIOS, boxShadow: M.shadow.md, overflow: 'hidden' }}>
              {[['Мши', 'нская'], ['Мши', 'нская-Товарная']].map(([a, b], i) => (
                <React.Fragment key={i}>
                  {i > 0 && <Sep t={t} inset={16}/>}
                  <div style={{ padding: '11px 16px' }}>
                    <div style={{ fontFamily: M.fontSans, fontSize: 15, color: t.text }}><b style={{ fontWeight: 700 }}>{a}</b>{b}</div>
                    <div style={{ ...M.t.captionMuted(t), marginTop: 1 }}>из справочника · станция</div>
                  </div>
                </React.Fragment>
              ))}
            </div>
          )}
        </div>
        <SegmentedToggle t={t} options={['Станция', 'О.П.']} active={type}/>
        <div>
          <Card t={t}><IpFieldRow t={t} label="Км от начала плеча" value={osm ? '25,1' : ''} placeholder="Не указано" suffix={osm ? 'км' : null}/></Card>
          <SectionNote t={t} top={6}>Необязательно. Помогает поставить станцию на место при повторном подборе.</SectionNote>
        </div>
        {!isNew && (
          <Card t={t}>
            {coords}
            <Sep t={t} inset={64}/>
            <button style={{ width: '100%', minHeight: 48, padding: '0 16px', border: 'none', background: 'transparent', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left' }}>
              <span style={{ width: 36, display: 'inline-flex', justifyContent: 'center', color: t.accent, flexShrink: 0 }}><IcMapPin width="18" height="18"/></span>
              <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.accent }}>{osm ? 'Уточнить на карте' : 'Указать на карте'}</span>
              <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
            </button>
          </Card>
        )}
        {osm && <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textFaint, padding: '0 18px', letterSpacing: 0.2 }}>Добавлена автоподбором · ЕСР 030329</div>}
        {!isNew && (
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <IpBtn t={t} kind="destructive" h={44} icon={<IcTrash width="16" height="16"/>}>Удалить станцию</IpBtn>
          </div>
        )}
      </div>
    </IpSheet>
  );
}

// ─── Action sheet «Удалить станцию» (вместо AlertDialog) ──────────────
//   .confirmationDialog(titleVisibility: .visible): группа — сообщение + «Удалить станцию» (danger);
//   отдельная кнопка «Отмена» (accent 600). Радиус 14, отступы 8, над home indicator.
function IpDeleteActionSheet({ t }) {
  const btn = (label, color, fw) => (
    <button style={{ width: '100%', height: 56, border: 'none', background: 'transparent', fontFamily: M.fontSans, fontSize: 18, fontWeight: fw, color, cursor: 'pointer' }}>{label}</button>
  );
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 95 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.45)' }}></div>
      <div style={{ position: 'absolute', left: 8, right: 8, bottom: 30, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div style={{ background: t.surface, borderRadius: 14, overflow: 'hidden' }}>
          <div style={{ padding: '14px 20px', textAlign: 'center' }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 13, fontWeight: 600, color: t.textMuted }}>Удалить станцию?</div>
            <div style={{ fontFamily: M.fontSans, fontSize: 13, color: t.textMuted, marginTop: 4, lineHeight: 1.35, textWrap: 'pretty' }}>«Кемка» будет убрана из плеча. Название останется в справочнике станций.</div>
          </div>
          <Sep t={t}/>
          {btn('Удалить станцию', t.danger, 400)}
        </div>
        <div style={{ background: t.surface, borderRadius: 14, overflow: 'hidden' }}>{btn('Отмена', t.accent, 600)}</div>
      </div>
    </div>
  );
}

Object.assign(window, { PST_LIST, IpShoulderRow, IpShouldersScreen, IpEditorSheet, IpStationSheet, IpDeleteActionSheet });
