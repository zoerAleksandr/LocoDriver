// Плечи · автоподбор станций по OpenStreetMap — загрузка, уточнение, предпросмотр, ошибки.

function PstAutoSheet({ t, children, footer, sub = `${PST_A.from} → ${PST_A.to}`, scroll = 0 }) {
  return <PstSheet t={t} z={80} height="92%" title="Автоподбор станций" sub={sub} footer={footer} scroll={scroll}>{children}</PstSheet>;
}

// 4.1 Загрузка
function PstAutoLoading({ t }) {
  const step = (state, text) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '9px 0' }}>
      {state === 'done' && <span style={{ width: 22, height: 22, borderRadius: 11, background: hexToRgba(t.success, .15), color: t.success, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><IcCheck width="14" height="14" style={{ strokeWidth: 3 }}/></span>}
      {state === 'now' && <span style={{ width: 22, display: 'inline-flex', justifyContent: 'center', flexShrink: 0 }}><PstSpinner t={t} size={18}/></span>}
      {state === 'next' && <span style={{ width: 22, display: 'inline-flex', justifyContent: 'center', flexShrink: 0 }}><span style={{ width: 10, height: 10, borderRadius: 5, border: `2px solid ${t.borderStrong}` }}></span></span>}
      <span style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: state === 'now' ? 600 : 500, color: state === 'next' ? t.textFaint : t.text }}>{text}</span>
    </div>
  );
  return (
    <PstAutoSheet t={t} footer={<PstBtn t={t} kind="outlined" h={48} full>Отменить</PstBtn>}>
      <div style={{ paddingTop: 20 }}>
        <div style={{ height: 4, borderRadius: 2, background: t.bgSubtle, overflow: 'hidden' }}>
          <div style={{ width: '46%', height: '100%', borderRadius: 2, background: t.accent }}></div>
        </div>
        <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 24, letterSpacing: -0.2 }}>Ищем путь между станциями…</div>
        <div style={{ ...M.t.hint(t), marginTop: 6 }}>Обычно это занимает 10–30 секунд. Можно отменить — ничего не изменится.</div>
        <div style={{ marginTop: 18 }}>
          {step('done', 'Нашли крайние станции на карте')}
          {step('now', 'Прокладываем путь')}
          {step('next', 'Собираем станции и О.П. на пути')}
        </div>
      </div>
    </PstAutoSheet>
  );
}

// 4.2 Уточнение крайней станции
function PstAutoCandidate({ t }) {
  const C = [
    { name: 'Гатчина-Варшавская', esr: '030201', road: 'Октябрьская ж. д., Ленинградская обл.', near: 'Татьянино · Суйда', on: true },
    { name: 'Гатчина-Балтийская', esr: '074208', road: 'Октябрьская ж. д., Ленинградская обл.', near: 'Мариенбург · Войсковицы' },
    { name: 'Гатчина-Товарная-Балтийская', esr: '074212', road: 'Октябрьская ж. д., Ленинградская обл.', near: 'Гатчина-Балтийская · Войсковицы' },
  ];
  return (
    <PstAutoSheet t={t} footer={
      <div style={{ display: 'flex', gap: 8 }}>
        <PstBtn t={t} kind="outlined" h={48}>Отмена</PstBtn>
        <PstBtn t={t} kind="filled" h={48} full="flex">Далее</PstBtn>
      </div>
    }>
      <div style={{ ...M.t.tag(t), color: t.accent, marginTop: 8 }}>Шаг 1 из 2 · станция отправления</div>
      <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 8, letterSpacing: -0.2 }}>Какая «Гатчина»?</div>
      <div style={{ ...M.t.hint(t), marginTop: 6, marginBottom: 14, textWrap: 'pretty' }}>На карте несколько станций с похожим названием. Узнайте свою по соседним станциям.</div>
      <MCard t={t}>
        {C.map((c, i) => (
          <React.Fragment key={c.esr}>
            {i > 0 && <Sep t={t} inset={50}/>}
            <div style={{ padding: '13px 16px', display: 'flex', gap: 14, alignItems: 'flex-start', background: c.on ? t.accentSoft : 'transparent' }}>
              <span style={{ marginTop: 1 }}><PstRadio t={t} on={c.on}/></span>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: t.text, overflowWrap: 'anywhere', lineHeight: 1.25 }}>{c.name}</div>
                <div style={{ fontFamily: M.fontMono, fontSize: 11, color: t.textMuted, marginTop: 4 }}>ЕСР {c.esr}</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{c.road}</div>
                <div style={{ ...M.t.captionMuted(t), marginTop: 2, color: t.text }}>Соседние: {c.near}</div>
              </div>
            </div>
          </React.Fragment>
        ))}
      </MCard>
      <div style={{ ...M.t.captionMuted(t), marginTop: 10, padding: '0 4px' }}>Следом уточним станцию прибытия, если она тоже неоднозначна.</div>
    </PstAutoSheet>
  );
}

// 4.3 Предпросмотр
//   showOp — переключатель «Показывать о.п.»; warn: 'mismatch' | 'nodist' | null; rerun — повторный подбор
function PstAutoPreview({ t, showOp = false, warn = 'mismatch', rerun = false, scroll = 0 }) {
  const inList = rerun ? new Set(['Суйда', 'Сиверская', 'Дивенская', 'Мшинская', 'Толмачёво', 'Кемка']) : new Set();
  const opPicked = new Set(['Строганово', 'Кемка']);
  const newNames = new Set(['Суйда', 'Дивенская', 'Мшинская', 'Разъезд 73 км', 'Строганово']);
  const rows = PST_FOUND.filter((s) => showOp || s.type === 'st');
  const checked = (s) => !inList.has(s.name) && (s.type === 'st' || (showOp && opPicked.has(s.name)));
  const nAdd = rows.filter(checked).length;
  const nNew = rows.filter((s) => checked(s) && newNames.has(s.name)).length;
  const nSt = PST_FOUND.filter((s) => s.type === 'st').length, nOp = PST_FOUND.length - nSt;
  const chip = (label) => (
    <button style={{ height: 32, padding: '0 12px', borderRadius: 8, border: `1px solid ${t.borderStrong}`, background: 'transparent', color: t.text, fontFamily: M.fontSans, fontSize: 13, fontWeight: 500, cursor: 'pointer', whiteSpace: 'nowrap' }}>{label}</button>
  );

  const footer = (
    <React.Fragment>
      {nNew > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '2px 2px 4px' }}>
          <PstCheckbox t={t} on/>
          <span style={{ fontFamily: M.fontSans, fontSize: 14, color: t.text, lineHeight: 1.3 }}>Добавить {nNew} {pstPlural(nNew, PST_NAME)} в справочник станций</span>
        </div>
      )}
      <div style={{ display: 'flex', gap: 8 }}>
        <PstBtn t={t} kind="outlined" h={48}>Отмена</PstBtn>
        <PstBtn t={t} kind="filled" h={48} full="flex" disabled={nAdd === 0}>
          {nAdd === 0 ? 'Ничего не выбрано' : `Добавить ${nAdd} ${pstPlural(nAdd, ['станцию', 'станции', 'станций'])}`}
        </PstBtn>
      </div>
      <div style={{ fontFamily: M.fontMono, fontSize: 10, color: t.textFaint, textAlign: 'center', letterSpacing: 0.3 }}>© участники OpenStreetMap</div>
    </React.Fragment>
  );

  return (
    <PstAutoSheet t={t} footer={footer} scroll={scroll}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, padding: '6px 4px 12px' }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 17, fontWeight: 600, color: t.text }}>Найдено: {nSt} {pstPlural(nSt, PST_ST)}, {nOp} О.П.</div>
        <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted }}>длина пути 81,4 км</div>
      </div>

      {warn === 'mismatch' && (
        <PstNotice t={t} title="Расстояние не совпадает с картой"
          action={<PstBtn t={t} kind="text" h={34} style={{ fontSize: 14 }}>Подставить 81 км</PstBtn>}>
          Вы указали 96 км, по карте — 81,4 км.
        </PstNotice>
      )}
      {warn === 'nodist' && (
        <PstNotice t={t} tone="accent" title="Расстояние не указано"
          action={<PstBtn t={t} kind="text" h={34} style={{ fontSize: 14 }}>Подставить 81 км</PstBtn>}>
          По карте длина пути — 81,4 км.
        </PstNotice>
      )}
      {rerun && (
        <div style={{ marginTop: warn ? 8 : 0 }}>
          <PstNotice t={t} tone="accent">Станции, которые уже есть в плече, останутся как есть. Добавятся только отмеченные новые.</PstNotice>
        </div>
      )}

      <div style={{ marginTop: 12 }}>
        <MCard t={t}><SwitchRow t={t} label="Показывать О.П." sub={showOp ? null : `Скрыто ${nOp} О.П. — по умолчанию не добавляются`} on={showOp} mat/></MCard>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
        {chip('Отметить все станции')}
        {chip('Снять все')}
      </div>

      <div style={{ marginTop: 12 }}>
        <MCard t={t}>
          <div style={{ padding: '4px 8px 4px 6px' }}>
            <PstStopRow t={t} s={{ name: PST_A.from, type: 'term' }} pos="start" first mode="pick"/>
            {rows.map((s) => (
              <PstStopRow key={s.name} t={t} s={s} mode="pick" checked={checked(s)} already={inList.has(s.name)}/>
            ))}
            <PstStopRow t={t} s={{ name: PST_A.to, type: 'term', km: 81.4 }} pos="end" last mode="pick"/>
          </div>
        </MCard>
      </div>
    </PstAutoSheet>
  );
}

// 4.5 Ошибки
const PST_ERRORS = {
  offline: {
    icon: IcCloudOff, title: 'Нет подключения к интернету',
    text: 'Автоподбор ищет станции на сервере. Проверьте связь и повторите — или добавьте станции вручную.',
    primary: 'Повторить', secondary: 'Добавить вручную',
  },
  notfound: {
    icon: IcMapPin, title: 'Не нашли станцию «Гатчина Варш.»',
    text: 'На карте нет станции с таким названием. Проверьте написание — полностью, без сокращений. Если станции нет на карте, добавьте промежуточные вручную.',
    primary: 'Изменить название', secondary: 'Добавить вручную',
  },
  nopath: {
    icon: IcShoulder, title: 'Путь между станциями не найден',
    text: 'На карте нет связанного пути от «Гатчина-Варшавская» до «Луга I». Возможно, станции на разных линиях или в данных карты разрыв.',
    primary: 'Добавить вручную', secondary: 'Закрыть',
  },
  timeout: {
    icon: IcClock, title: 'Сервис не ответил',
    text: 'Поиск занял больше 30 секунд или сервис временно недоступен. Попробуйте ещё раз через минуту.',
    primary: 'Повторить', secondary: 'Закрыть',
  },
};

function PstAutoError({ t, kind }) {
  const e = PST_ERRORS[kind];
  const Ic = e.icon;
  return (
    <PstAutoSheet t={t} footer={
      <React.Fragment>
        <PstBtn t={t} kind="filled" h={48} full>{e.primary}</PstBtn>
        <PstBtn t={t} kind="outlined" h={48} full>{e.secondary}</PstBtn>
      </React.Fragment>
    }>
      <div style={{ padding: '56px 12px 0', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <span style={{ width: 64, height: 64, borderRadius: 20, background: t.bgSubtle, color: t.textMuted, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
          <Ic width="30" height="30"/>
        </span>
        <div style={{ fontFamily: M.fontSans, fontSize: 20, fontWeight: 600, color: t.text, marginTop: 20, letterSpacing: -0.2, textWrap: 'balance', overflowWrap: 'anywhere' }}>{e.title}</div>
        <div style={{ ...M.t.hint(t), fontSize: 14, marginTop: 8, maxWidth: 300, textWrap: 'pretty' }}>{e.text}</div>
      </div>
    </PstAutoSheet>
  );
}

Object.assign(window, { PstAutoSheet, PstAutoLoading, PstAutoCandidate, PstAutoPreview, PST_ERRORS, PstAutoError });
