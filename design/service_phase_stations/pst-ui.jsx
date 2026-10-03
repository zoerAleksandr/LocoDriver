// Плечи · промежуточные станции — общие примитивы (префикс Pst, чтобы не пересекаться с глобалами пакета).
// Всё строится на tokens.js: accent — действия, warning — «нет координат» / расхождение длины,
// danger — только удаление, success — «координаты есть».

function pstPlural(n, forms) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return forms[2];
  if (b > 1 && b < 5) return forms[1];
  if (b === 1) return forms[0];
  return forms[2];
}
const PST_ST = ['станция', 'станции', 'станций'];
const PST_OP = ['О.П.', 'О.П.', 'О.П.'];
const PST_NAME = ['новое название', 'новых названия', 'новых названий'];
function pstKm(v) { return v == null ? '—' : String(v.toFixed(1)).replace('.', ',') + ' км'; }

// ─── Кнопки (M3: filled / tonal / outlined / text; cta — чёрная пилюля шторок) ──
function PstBtn({ t, kind = 'filled', icon, children, disabled, full, h = 44, style }) {
  const k = {
    filled:   { bg: t.accent, fg: t.accentInk, bd: 'none' },
    tonal:    { bg: t.accentSoft, fg: t.accent, bd: 'none' },
    outlined: { bg: 'transparent', fg: t.text, bd: `1px solid ${t.borderStrong}` },
    text:     { bg: 'transparent', fg: t.accent, bd: 'none' },
    cta:      { bg: t.cta, fg: t.ctaInk, bd: 'none' },
    danger:   { bg: 'transparent', fg: t.danger, bd: 'none' },
  }[kind];
  const dis = disabled && kind !== 'text';
  return (
    <button disabled={disabled} style={{
      height: h, padding: kind === 'text' ? '0 12px' : '0 20px', borderRadius: h / 2,
      width: full ? '100%' : undefined, flex: full === 'flex' ? 1 : undefined,
      background: dis ? t.bgSubtle : k.bg, color: disabled ? t.textFaint : k.fg,
      border: dis && kind === 'outlined' ? `1px solid ${t.border}` : k.bd,
      fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, cursor: disabled ? 'default' : 'pointer',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8, flexShrink: 0,
      whiteSpace: 'nowrap', ...style,
    }}>{icon}{children}</button>
  );
}

function PstSpinner({ t, size = 18, color }) {
  return <span style={{
    width: size, height: size, borderRadius: '50%', flexShrink: 0, display: 'inline-block',
    border: `2px solid ${hexToRgba(color || t.accent, .22)}`, borderTopColor: color || t.accent,
    animation: 'pstspin .9s linear infinite',
  }}></span>;
}

function PstCheckbox({ t, on, disabled }) {
  return (
    <span style={{
      width: 20, height: 20, borderRadius: 4, flexShrink: 0, boxSizing: 'border-box',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      background: on ? (disabled ? t.textFaint : t.accent) : 'transparent',
      border: on ? 'none' : `2px solid ${t.textMuted}`, color: t.accentInk,
    }}>{on && <IcCheck width="14" height="14" style={{ strokeWidth: 3 }}/>}</span>
  );
}

function PstRadio({ t, on }) {
  return (
    <span style={{
      width: 20, height: 20, borderRadius: 10, flexShrink: 0, boxSizing: 'border-box',
      border: `2px solid ${on ? t.accent : t.borderStrong}`,
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
    }}>{on && <span style={{ width: 10, height: 10, borderRadius: 5, background: t.accent }}></span>}</span>
  );
}

function PstIcClose(p) {
  return <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}><path d="M6 6l12 12M6 18L18 6"/></svg>;
}
function PstIcDrag(p) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}><path d="M5 9h14M5 15h14"/></svg>;
}

// ─── Каркас шторки Android (как StationEditSheet: 22/500 + ✕) ──────────
//   scroll — сдвиг содержимого, чтобы показать «прокрученную» шторку.
function PstSheet({ t, title, sub, height = '94%', z = 70, scroll = 0, footer, children, scrim = true }) {
  return (
    <React.Fragment>
      {scrim && <div style={{ position: 'absolute', inset: 0, zIndex: z, background: 'rgba(0,0,0,0.45)' }}></div>}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: z + 1, height,
        background: t.bg, borderTopLeftRadius: M.r.sheetAndroid, borderTopRightRadius: M.r.sheetAndroid,
        boxShadow: M.shadow.sheet, display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
          <div style={{ width: 36, height: 4, borderRadius: 3, background: t.borderStrong }}></div>
        </div>
        <div style={{ padding: '8px 12px 6px 20px', display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: M.fontSans, fontSize: 22, fontWeight: 500, color: t.text, letterSpacing: -0.2 }}>{title}</div>
            {sub && <div style={{ ...M.t.captionMuted(t), fontSize: 13, marginTop: 2, textWrap: 'pretty' }}>{sub}</div>}
          </div>
          <button aria-label="Закрыть" style={{
            width: 40, height: 40, borderRadius: 20, border: 'none', background: 'transparent',
            color: t.textMuted, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}><PstIcClose/></button>
        </div>
        <div style={{ flex: 1, minHeight: 0, overflow: 'hidden', position: 'relative' }}>
          <div style={{ marginTop: -scroll, padding: '6px 16px 24px' }}>{children}</div>
        </div>
        {footer && (
          <div style={{ padding: '10px 16px 14px', borderTop: `1px solid ${t.border}`, background: t.bg, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {footer}
          </div>
        )}
      </div>
    </React.Fragment>
  );
}

// ─── Поле «label сверху, value снизу» на surface (Android-карточка) ──────
function PstField({ t, label, value, placeholder, leading, suffix, focus, flex, trailing }) {
  const empty = !value;
  return (
    <div style={{
      flex: flex || undefined, minWidth: 0, minHeight: 56, padding: '8px 14px',
      borderRadius: M.r.cardAndroid, background: t.surface, boxShadow: focus ? 'none' : M.shadow.sm,
      border: focus ? `2px solid ${t.accent}` : '2px solid transparent', boxSizing: 'border-box',
      display: 'flex', alignItems: 'center', gap: 10,
    }}>
      {leading}
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <span style={{ ...M.t.label(t), fontSize: 12, color: focus ? t.accent : t.textMuted }}>{label}</span>
        <span style={{ display: 'flex', alignItems: 'baseline', gap: 6, minWidth: 0 }}>
          <span style={{
            fontFamily: M.fontSans, fontSize: 16, fontWeight: empty ? 400 : 600, color: empty ? t.textFaint : t.text,
            letterSpacing: -0.2, overflowWrap: 'anywhere', lineHeight: 1.25,
          }}>{empty ? placeholder : value}{focus && <span style={{ display: 'inline-block', width: 2, height: 18, background: t.accent, marginLeft: 1, verticalAlign: -3 }}></span>}</span>
          {suffix && <span style={{ ...M.t.label(t), flexShrink: 0 }}>{suffix}</span>}
        </span>
      </div>
      {trailing}
    </div>
  );
}

// ─── Схема участка: одна строка ────────────────────────────────────────
//   s: { name, type:'term'|'st'|'op', km, esr, src:'osm'|'manual', coords, fresh }
//   mode: 'view' (тап → шторка станции) | 'edit' (ручка ≡ + ✕) | 'pick' (чекбокс, предпросмотр)
function PstStopRow({ t, s, first, last, mode = 'view', lifted, checked, already, pos }) {
  const COL = 32;
  const term = s.type === 'term', op = s.type === 'op';
  const dot = term ? 14 : op ? 8 : 12;
  const sub = [];
  if (term) sub.push(pos === 'start' ? 'отправление' : 'прибытие');
  else sub.push(op ? 'О.П.' : 'станция');
  if (s.esr && !op) sub.push('ЕСР ' + s.esr);
  return (
    <div style={{
      position: 'relative', display: 'flex',
      background: lifted ? t.surface : s.fresh ? t.accentSoft : 'transparent',
      borderRadius: lifted || s.fresh ? 12 : 0,
      boxShadow: lifted ? M.shadow.floating : 'none', zIndex: lifted ? 2 : 1,
      transform: lifted ? 'translateX(4px) scale(1.015)' : 'none',
    }}>
      <div style={{ position: 'relative', width: COL, flexShrink: 0 }}>
        {!lifted && <div style={{
          position: 'absolute', left: COL / 2 - 0.75, top: first ? '50%' : 0, bottom: last ? '50%' : 0,
          borderLeft: `1.5px dashed ${t.borderStrong}`,
        }}></div>}
        <div style={{
          position: 'absolute', left: (COL - dot) / 2, top: `calc(50% - ${dot / 2}px)`,
          width: dot, height: dot, borderRadius: dot / 2, boxSizing: 'border-box',
          background: term ? t.text : t.surface,
          border: term ? 'none' : `${op ? 1.5 : 2}px solid ${op ? t.textFaint : t.borderStrong}`,
        }}></div>
      </div>
      <div style={{
        flex: 1, minWidth: 0, minHeight: op ? 48 : 56, padding: '8px 4px 8px 4px',
        display: 'flex', alignItems: 'center', gap: 10,
        borderBottom: last || lifted ? 'none' : `0.5px solid ${t.border}`,
      }}>
        {mode === 'pick' && !term && (already ? <PstCheckbox t={t} on disabled/> : <PstCheckbox t={t} on={checked}/>)}
        <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 3 }}>
          <div style={{
            fontFamily: M.fontSans, fontSize: op ? 14 : 15, fontWeight: op ? 500 : 600,
            color: op ? t.textMuted : t.text, lineHeight: 1.25, letterSpacing: -0.2,
            overflowWrap: 'anywhere', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
          }}>{s.name}</div>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 500, color: t.textFaint, display: 'flex', flexWrap: 'wrap', columnGap: 6 }}>
            <span style={{ whiteSpace: 'nowrap' }}>{sub.join(' · ')}</span>
            {!term && s.coords === false && <span style={{ color: t.warning, whiteSpace: 'nowrap' }}>нет координат</span>}
            {already && <span style={{ color: t.accent, whiteSpace: 'nowrap' }}>уже в списке</span>}
          </div>
        </div>
        <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 500, color: s.km == null ? t.textFaint : t.textMuted, flexShrink: 0 }}>
          {term && pos === 'start' ? '0 км' : term && Number.isInteger(s.km) ? s.km + ' км' : pstKm(s.km)}
        </span>
        {mode === 'view' && !term && <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>}
        {mode === 'view' && term && <span style={{ width: 14, flexShrink: 0 }}></span>}
        {mode === 'edit' && !term && (
          <span style={{ display: 'inline-flex', alignItems: 'center', flexShrink: 0, marginRight: -4 }}>
            <span aria-label="Убрать" style={{ width: 36, height: 40, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: t.textMuted }}><PstIcClose width="18" height="18"/></span>
            <span aria-label="Перетащить" style={{ width: 36, height: 40, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: lifted ? t.accent : t.textMuted }}><PstIcDrag/></span>
          </span>
        )}
        {mode === 'edit' && term && <span style={{ width: 68, flexShrink: 0 }}></span>}
      </div>
    </div>
  );
}

// Вставка между соседними (режим «Изменить»): «+» на линии
function PstInsertRow({ t, active }) {
  return (
    <div style={{ position: 'relative', display: 'flex', alignItems: 'center', height: 30 }}>
      <div style={{ position: 'absolute', left: 15.25, top: 0, bottom: 0, borderLeft: `1.5px dashed ${t.borderStrong}` }}></div>
      <span style={{
        position: 'relative', marginLeft: 5, width: 22, height: 22, borderRadius: 11,
        background: active ? t.accent : t.surface, color: active ? t.accentInk : t.accent,
        border: active ? 'none' : `1.5px solid ${hexToRgba(t.accent, .5)}`, boxSizing: 'border-box',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      }}><IcPlus width="12" height="12" style={{ strokeWidth: 2.6 }}/></span>
      <span style={{ marginLeft: 10, fontFamily: M.fontSans, fontSize: 12, fontWeight: 500, color: active ? t.accent : t.textFaint }}>
        {active ? 'Вставить станцию сюда' : 'вставить'}
      </span>
    </div>
  );
}

// Свёрнутая середина длинного списка
function PstCollapsedRow({ t, hidden }) {
  return (
    <div style={{ position: 'relative', display: 'flex' }}>
      <div style={{ position: 'relative', width: 32, flexShrink: 0 }}>
        <div style={{ position: 'absolute', left: 15.25, top: 0, bottom: 0, borderLeft: `1.5px dotted ${t.textFaint}` }}></div>
      </div>
      <button style={{
        flex: 1, minHeight: 48, border: 'none', background: 'transparent', textAlign: 'left', padding: '0 4px',
        display: 'flex', alignItems: 'center', gap: 8, borderBottom: `0.5px solid ${t.border}`, cursor: 'pointer',
      }}>
        <span style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.accent }}>Показать все</span>
        <span style={{ fontFamily: M.fontMono, fontSize: 12, color: t.textMuted }}>ещё {hidden} {pstPlural(hidden, PST_ST)}</span>
      </button>
    </div>
  );
}

// Схема целиком: крайние станции + промежуточные
//   collapse: { head, tail } — показать первые head и последние tail промежуточных
function PstLine({ t, from, to, km, stops, mode = 'view', collapse, lifted, insertActive }) {
  const all = [{ name: from, type: 'term', pos: 'start' }, ...stops, { name: to, type: 'term', km, pos: 'end' }];
  let rows = all.map((s, i) => ({ s, i }));
  let hiddenAt = -1, hidden = 0;
  if (collapse && stops.length > collapse.head + collapse.tail + 1) {
    hidden = stops.length - collapse.head - collapse.tail;
    rows = [...rows.slice(0, 1 + collapse.head), ...rows.slice(1 + collapse.head + hidden)];
    hiddenAt = 1 + collapse.head;
  }
  const out = [];
  rows.forEach(({ s, i }, k) => {
    if (k === hiddenAt) out.push(<PstCollapsedRow key="c" t={t} hidden={hidden}/>);
    if (mode === 'edit' && k > 0) out.push(<PstInsertRow key={'ins' + k} t={t} active={insertActive === k}/>);
    out.push(<PstStopRow key={i} t={t} s={s} pos={s.pos} first={i === 0} last={i === all.length - 1} mode={mode} lifted={lifted === i}/>);
  });
  return <div style={{ padding: '4px 8px 4px 6px' }}>{out}</div>;
}

// ─── Инфо-/предупреждающая плашка ──────────────────────────────────────
function PstNotice({ t, tone = 'warning', icon, title, children, action }) {
  const c = tone === 'warning' ? t.warning : tone === 'success' ? t.success : t.accent;
  return (
    <div style={{ borderRadius: M.r.cardAndroid, background: hexToRgba(c, .12), padding: '12px 14px', display: 'flex', gap: 12 }}>
      <span style={{ color: c, flexShrink: 0, marginTop: 1 }}>{icon || <IcInfo width="18" height="18"/>}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        {title && <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text, marginBottom: 2 }}>{title}</div>}
        <div style={{ ...M.t.hint(t), color: t.text, opacity: .8, textWrap: 'pretty' }}>{children}</div>
        {action && <div style={{ marginTop: 6, marginLeft: -12 }}>{action}</div>}
      </div>
    </div>
  );
}

// ─── Snackbar (M3 inverse surface) ─────────────────────────────────────
function PstSnackbar({ t, dark, text, action, bottom = 12 }) {
  return (
    <div style={{
      position: 'absolute', left: 12, right: 12, bottom, zIndex: 90, borderRadius: 8,
      background: t.cta, color: t.ctaInk, boxShadow: M.shadow.floating,
      padding: '6px 6px 6px 16px', display: 'flex', alignItems: 'center', gap: 8, minHeight: 48,
    }}>
      <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 14, lineHeight: 1.35 }}>{text}</span>
      <button style={{ border: 'none', background: 'transparent', height: 36, padding: '0 12px', borderRadius: 18, fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: dark ? M.light.accentHover : M.dark.accent, cursor: 'pointer' }}>{action}</button>
    </div>
  );
}

// ─── Данные ────────────────────────────────────────────────────────────
const PST_A = { from: 'Гатчина-Варшавская', to: 'Луга I', km: 96, pay: '0,42' };
const PST_SHORT = [
  { name: 'Суйда', type: 'st', km: 10.6, esr: '030310', src: 'osm', coords: true },
  { name: 'Сиверская', type: 'st', km: 25.1, esr: '030329', src: 'osm', coords: true },
  { name: 'Дивенская', type: 'st', km: 40.2, esr: '030337', src: 'osm', coords: true },
  { name: 'Кемка', type: 'op', km: null, src: 'manual', coords: false },
  { name: 'Мшинская', type: 'st', km: 53.6, esr: '030345', src: 'osm', coords: true },
  { name: 'Толмачёво', type: 'st', km: 68.3, esr: '030353', src: 'osm', coords: true },
];
const PST_B = { from: 'Санкт-Петербург-Товарный-Витебский', to: 'Луга I', km: 139, pay: '0,42' };
const PST_LONG_HEAD = [
  { name: 'Шушары', type: 'st', km: 9.8, esr: '031105', coords: true },
  { name: 'Детское Село', type: 'st', km: 17.4, esr: '031113', coords: true },
  { name: 'Павловск', type: 'st', km: 22.9, esr: '031121', coords: true },
];
const PST_LONG_TAIL = [
  { name: 'Мшинская', type: 'st', km: 111.2, esr: '030345', coords: true },
  { name: 'Толмачёво', type: 'st', km: 125.9, esr: '030353', coords: true },
];
const PST_LONG = [...PST_LONG_HEAD, ...['Антропшино:op', 'Новолисино:st', 'Кобралово:op', 'Вырица:st', 'Ефимовская:op', 'Строганово-Витебская:op', 'Семрино:st', 'Посёлок:op', 'Слудицы:st', 'Чаща:op', 'Кемка-Витебская:op', 'Оредеж:st', 'Чолово:op', 'Разъезд 81 км:st', 'Тёплое:op', 'Подгорное:op', 'Пристань:st', 'Мельница:op', 'Жельцы:st', 'Кривино:op', 'Толмачёво-Товарное:st', 'Заполье:op', 'Остров:op', 'Бор:st', 'Шапки:op', 'Партизанская:st', 'Раковичи:op', 'Серебрянка-Сортировочная:st', 'Дивенская-Пассажирская:op', 'Разъезд 102 км:st', 'Низовская-2:op', 'Мшинская-Товарная:st', 'Ящера:op'].map((x, i) => { const [n, ty] = x.split(':'); return { name: n, type: ty, km: +(26.4 + i * 2.55).toFixed(1), esr: ty === 'st' ? String(31130 + i * 8).padStart(6, '0') : undefined, coords: i !== 7 && i !== 19 }; }), ...PST_LONG_TAIL];

// Результат автоподбора (Гатчина-Варшавская → Луга I)
const PST_FOUND = [
  { name: 'Татьянино', type: 'op', km: 3.9 },
  { name: 'Суйда', type: 'st', km: 10.6, esr: '030310', isNew: true },
  { name: 'Прибытково', type: 'op', km: 15.2 },
  { name: 'Старосиверская', type: 'op', km: 21.0 },
  { name: 'Сиверская', type: 'st', km: 25.1, esr: '030329' },
  { name: 'Строганово', type: 'op', km: 30.4 },
  { name: 'Дивенская', type: 'st', km: 40.2, esr: '030337', isNew: true },
  { name: 'Низовская', type: 'op', km: 45.8 },
  { name: 'Мшинская', type: 'st', km: 53.6, esr: '030345', isNew: true },
  { name: 'Кемка', type: 'op', km: 59.1 },
  { name: 'Толмачёво', type: 'st', km: 68.3, esr: '030353' },
  { name: 'Разъезд 73 км', type: 'st', km: 73.0, esr: '030361', isNew: true },
  { name: 'Серебрянка', type: 'op', km: 76.7 },
];

Object.assign(window, {
  pstPlural, pstKm, PST_ST, PST_OP, PST_NAME, PstBtn, PstSpinner, PstCheckbox, PstRadio, PstIcClose, PstIcDrag,
  PstSheet, PstField, PstStopRow, PstInsertRow, PstCollapsedRow, PstLine, PstNotice, PstSnackbar,
  PST_A, PST_B, PST_SHORT, PST_LONG, PST_FOUND,
});
