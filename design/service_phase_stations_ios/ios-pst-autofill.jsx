// iOS · Плечи — выбор точки на карте и автоподбор (загрузка, уточнение, предпросмотр, ошибки).

// ─── Выбор точки на карте (fullScreenCover) ──────────────────────────
function IpMapPicker({ dark = false, height = 844, station = 'Кемка', type = 'О.П.' }) {
  const t = dark ? M.dark : M.light;
  const round = (child, label) => (
    <button aria-label={label} style={{ width: 44, height: 44, borderRadius: 22, border: 'none', background: t.surface, color: t.text, boxShadow: M.shadow.md, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', fontFamily: M.fontSans, fontSize: 22, fontWeight: 400, flexShrink: 0 }}>{child}</button>
  );
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ position: 'absolute', inset: 0, background: `repeating-linear-gradient(135deg, ${t.bgSubtle} 0 14px, ${t.surfaceAlt} 14px 28px)` }}>
        <div style={{ position: 'absolute', left: 0, right: 0, top: '24%', textAlign: 'center', fontFamily: M.fontMono, fontSize: 11, color: t.textFaint, letterSpacing: 1, textTransform: 'uppercase' }}>карта · тайлы, жесты: сдвиг, зум</div>
      </div>
      <div style={{ position: 'absolute', top: 58, left: 16, right: 16, zIndex: 5, display: 'flex', gap: 8, alignItems: 'center' }}>
        {round(<IcChevronLeft width="20" height="20"/>, 'Назад')}
        <div style={{ flex: 1, minWidth: 0, height: 44, borderRadius: 22, background: t.surface, boxShadow: M.shadow.md, padding: '0 18px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', lineHeight: 1.2 }}>{station}</div>
          <div style={{ ...M.t.captionMuted(t), lineHeight: 1.2 }}>Укажите место станции</div>
        </div>
      </div>
      <div style={{ position: 'absolute', left: '50%', top: '42%', zIndex: 4, transform: 'translate(-50%, -100%)', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <span style={{ width: 44, height: 44, borderRadius: 22, background: t.accent, color: t.accentInk, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', boxShadow: M.shadow.fab }}><IcMapPin width="22" height="22"/></span>
        <span style={{ width: 2, height: 14, background: t.accent }}></span>
        <span style={{ width: 10, height: 10, borderRadius: 5, background: t.accent, boxShadow: `0 0 0 6px ${hexToRgba(t.accent, .2)}` }}></span>
      </div>
      <div style={{ position: 'absolute', right: 16, top: '46%', zIndex: 5, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {round('+', 'Приблизить')}
        {round('−', 'Отдалить')}
        {round(<IcLocationArrow style={{ color: t.accent }}/>, 'Моё местоположение')}
      </div>
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 6, background: t.bg, borderTopLeftRadius: M.r.sheetIOS, borderTopRightRadius: M.r.sheetIOS, boxShadow: M.shadow.sheet, padding: '16px 16px 34px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ width: 36, height: 36, borderRadius: 10, background: t.accentSoft, color: t.accent, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><IcMapPin width="18" height="18"/></span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text }}>{station} · {type}</div>
            <div style={{ ...M.t.captionMuted(t), marginTop: 2, textWrap: 'pretty' }}>Двигайте карту — точка под меткой станет координатами станции</div>
          </div>
        </div>
        <IpBtn t={t} kind="primary" full style={{ borderRadius: 14 }}>Выбрать</IpBtn>
      </div>
    </MDevice>
  );
}
// SF Symbol location — стрелка «Моё местоположение»
function IcLocationArrow(p) {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" {...p}><path d="M20.5 3.5L3.5 10.6l7.2 2.7 2.7 7.2z"/></svg>;
}

// ─── Каркас автоподбора ──────────────────────────────────────────────
function IpAutoSheet({ t, children, left = 'Отмена', right, rightDisabled, footer, scroll = 0 }) {
  return <IpSheet t={t} z={80} top={74} left={left} title="Автоподбор станций" sub={`${PST_A.from} → ${PST_A.to}`} right={right} rightDisabled={rightDisabled} footer={footer} scroll={scroll}>{children}</IpSheet>;
}

// 5.1 Загрузка
function IpAutoLoading({ t }) {
  const step = (state, text) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '9px 0' }}>
      <span style={{ width: 22, display: 'inline-flex', justifyContent: 'center', flexShrink: 0 }}>
        {state === 'done' && <span style={{ width: 22, height: 22, borderRadius: 11, background: hexToRgba(t.success, .15), color: t.success, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}><IcCheck width="14" height="14" style={{ strokeWidth: 3 }}/></span>}
        {state === 'now' && <IpSpinner t={t} size={18}/>}
        {state === 'next' && <span style={{ width: 10, height: 10, borderRadius: 5, border: `2px solid ${t.borderStrong}` }}></span>}
      </span>
      <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: state === 'now' ? 600 : 500, color: state === 'next' ? t.textFaint : t.text }}>{text}</span>
    </div>
  );
  return (
    <IpAutoSheet t={t} left="Отменить">
      <div style={{ paddingTop: 20, paddingLeft: 4, paddingRight: 4 }}>
        <IpProgress t={t} value={0.46}/>
        <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 24, letterSpacing: -0.2 }}>Ищем путь между станциями…</div>
        <div style={{ ...M.t.hint(t), marginTop: 6 }}>Обычно это занимает 10–30 секунд. Можно отменить — ничего не изменится.</div>
        <div style={{ marginTop: 18 }}>
          {step('done', 'Нашли крайние станции на карте')}
          {step('now', 'Прокладываем путь')}
          {step('next', 'Собираем станции и О.П. на пути')}
        </div>
      </div>
    </IpAutoSheet>
  );
}

// 5.2 Уточнение крайней станции
function IpAutoCandidate({ t }) {
  const C = [
    { name: 'Гатчина-Варшавская', esr: '030201', near: 'Татьянино · Суйда', on: true },
    { name: 'Гатчина-Балтийская', esr: '074208', near: 'Мариенбург · Войсковицы' },
    { name: 'Гатчина-Товарная-Балтийская', esr: '074212', near: 'Гатчина-Балтийская · Войсковицы' },
  ];
  return (
    <IpAutoSheet t={t} right="Далее">
      <div style={{ padding: '8px 4px 0' }}>
        <div style={{ ...M.t.tag(t), color: t.accent }}>Шаг 1 из 2 · станция отправления</div>
        <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 8, letterSpacing: -0.2 }}>Какая «Гатчина»?</div>
        <div style={{ ...M.t.hint(t), marginTop: 6, marginBottom: 14, textWrap: 'pretty' }}>На карте несколько станций с похожим названием. Узнайте свою по соседним станциям.</div>
      </div>
      <Card t={t}>
        {C.map((c, i) => (
          <React.Fragment key={c.esr}>
            {i > 0 && <Sep t={t} inset={18}/>}
            <div style={{ padding: '13px 16px 13px 18px', display: 'flex', gap: 12, alignItems: 'center' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text, overflowWrap: 'anywhere', lineHeight: 1.25 }}>{c.name}</div>
                <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, marginTop: 4 }}>ЕСР {c.esr}</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>Октябрьская ж. д., Ленинградская обл.</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2, color: t.text }}>Соседние: {c.near}</div>
              </div>
              <span style={{ width: 18, flexShrink: 0 }}>{c.on && <IpTick t={t}/>}</span>
            </div>
          </React.Fragment>
        ))}
      </Card>
      <SectionNote t={t} top={10}>Следом уточним станцию прибытия, если она тоже неоднозначна.</SectionNote>
    </IpAutoSheet>
  );
}

// 5.3 Предпросмотр
function IpAutoPreview({ t, showOp = false, warn = 'mismatch', rerun = false, scroll = 0 }) {
  const inList = rerun ? new Set(['Суйда', 'Сиверская', 'Дивенская', 'Мшинская', 'Толмачёво', 'Кемка']) : new Set();
  const opPicked = new Set(['Строганово', 'Кемка']);
  const newNames = new Set(['Суйда', 'Дивенская', 'Мшинская', 'Разъезд 73 км', 'Строганово']);
  const rows = PST_FOUND.filter((s) => showOp || s.type === 'st');
  const checked = (s) => !inList.has(s.name) && (s.type === 'st' || (showOp && opPicked.has(s.name)));
  const nAdd = rows.filter(checked).length;
  const nNew = rows.filter((s) => checked(s) && newNames.has(s.name)).length;
  const nSt = PST_FOUND.filter((s) => s.type === 'st').length, nOp = PST_FOUND.length - nSt;
  const chip = (label) => (
    <button style={{ height: 32, padding: '0 14px', borderRadius: 16, border: 'none', background: t.bgSubtle, color: t.accent, fontFamily: M.fontSans, fontSize: 13, fontWeight: 500, cursor: 'pointer', whiteSpace: 'nowrap' }}>{label}</button>
  );
  const footer = (
    <React.Fragment>
      {nNew > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '0 2px' }}>
          <IpCheck t={t} on/>
          <span style={{ fontFamily: M.fontSans, fontSize: 14, color: t.text, lineHeight: 1.3 }}>Добавить {nNew} {pstPlural(nNew, PST_NAME)} в справочник станций</span>
        </div>
      )}
      <IpBtn t={t} kind="primary" full disabled={nAdd === 0}>{nAdd === 0 ? 'Ничего не выбрано' : `Добавить ${nAdd} ${pstPlural(nAdd, ['станцию', 'станции', 'станций'])}`}</IpBtn>
      <div style={{ fontFamily: M.fontMono, fontSize: 10, color: t.textFaint, textAlign: 'center', letterSpacing: 0.3 }}>© участники OpenStreetMap</div>
    </React.Fragment>
  );
  return (
    <IpAutoSheet t={t} footer={footer} scroll={scroll}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, padding: '6px 4px 12px' }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 17, fontWeight: 600, color: t.text }}>Найдено: {nSt} {pstPlural(nSt, PST_ST)}, {nOp} О.П.</div>
        <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted }}>длина пути 81,4 км</div>
      </div>
      {warn === 'mismatch' && <IpNotice t={t} title="Расстояние не совпадает с картой" action={<IpBtn t={t} kind="plain" h={32} fs={14} style={{ fontWeight: 600 }}>Подставить 81 км</IpBtn>}>Вы указали 96 км, по карте — 81,4 км.</IpNotice>}
      {warn === 'nodist' && <IpNotice t={t} tone="accent" title="Расстояние не указано" action={<IpBtn t={t} kind="plain" h={32} fs={14} style={{ fontWeight: 600 }}>Подставить 81 км</IpBtn>}>По карте длина пути — 81,4 км.</IpNotice>}
      {rerun && <div style={{ marginTop: warn ? 8 : 0 }}><IpNotice t={t} tone="accent">Станции, которые уже есть в плече, останутся как есть. Добавятся только отмеченные новые.</IpNotice></div>}
      <div style={{ marginTop: 12 }}>
        <Card t={t}><SwitchRow t={t} label="Показывать О.П." sub={showOp ? null : `Скрыто ${nOp} О.П. — по умолчанию не добавляются`} on={showOp}/></Card>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>{chip('Отметить все станции')}{chip('Снять все')}</div>
      <div style={{ marginTop: 12 }}>
        <Card t={t}>
          <div style={{ padding: '4px 10px 4px 6px' }}>
            <IpStopRow t={t} s={{ name: PST_A.from, type: 'term' }} pos="start" first mode="pick"/>
            {rows.map((s) => <IpStopRow key={s.name} t={t} s={s} mode="pick" checked={checked(s)} already={inList.has(s.name)}/>)}
            <IpStopRow t={t} s={{ name: PST_A.to, type: 'term', km: 81.4 }} pos="end" last mode="pick"/>
          </div>
        </Card>
      </div>
    </IpAutoSheet>
  );
}

// 5.4 Ошибки
const IP_ERRORS = {
  offline: { icon: IcCloudOff, title: 'Нет подключения к интернету', text: 'Автоподбор ищет станции на сервере. Проверьте связь и повторите — или добавьте станции вручную.', primary: 'Повторить', secondary: 'Добавить вручную' },
  notfound: { icon: IcMapPin, title: 'Не нашли станцию «Гатчина Варш.»', text: 'На карте нет станции с таким названием. Проверьте написание — полностью, без сокращений. Если станции нет на карте, добавьте промежуточные вручную.', primary: 'Изменить название', secondary: 'Добавить вручную' },
  nopath: { icon: IcShoulder, title: 'Путь между станциями не найден', text: 'На карте нет связанного пути от «Гатчина-Варшавская» до «Луга I». Возможно, станции на разных линиях или в данных карты разрыв.', primary: 'Добавить вручную', secondary: 'Закрыть' },
  timeout: { icon: IcClock, title: 'Сервис не ответил', text: 'Поиск занял больше 30 секунд или сервис временно недоступен. Попробуйте ещё раз через минуту.', primary: 'Повторить', secondary: 'Закрыть' },
};

function IpAutoError({ t, kind }) {
  const e = IP_ERRORS[kind];
  const Ic = e.icon;
  return (
    <IpAutoSheet t={t}>
      <div style={{ padding: '56px 12px 0', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <span style={{ width: 64, height: 64, borderRadius: 20, background: t.bgSubtle, color: t.textMuted, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}><Ic width="30" height="30"/></span>
        <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 20, letterSpacing: -0.2, textWrap: 'balance', overflowWrap: 'anywhere' }}>{e.title}</div>
        <div style={{ ...M.t.hint(t), fontSize: 14, marginTop: 8, maxWidth: 300, textWrap: 'pretty' }}>{e.text}</div>
        <div style={{ alignSelf: 'stretch', display: 'flex', flexDirection: 'column', gap: 8, marginTop: 28 }}>
          <IpBtn t={t} kind="primary" full>{e.primary}</IpBtn>
          <IpBtn t={t} kind="secondary" full>{e.secondary}</IpBtn>
        </div>
      </div>
    </IpAutoSheet>
  );
}

Object.assign(window, { IpMapPicker, IcLocationArrow, IpAutoSheet, IpAutoLoading, IpAutoCandidate, IpAutoPreview, IP_ERRORS, IpAutoError });
