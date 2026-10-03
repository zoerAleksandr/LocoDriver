// iOS · Плечи · промежуточные станции — примитивы (префикс Ip, чтобы не пересекаться с глобалами кита).
// Логика, тексты и данные — 1:1 с Android-пакетом (pst-ui.jsx). Меняется только платформенная обвязка.
// Цвета только из tokens.js: accent — действия, warning — «нет координат» / расхождение длины,
// success — «координаты есть», danger — только удаление (и ⊖ в режиме правки).

function pstPlural(n, forms) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return forms[2];
  if (b > 1 && b < 5) return forms[1];
  if (b === 1) return forms[0];
  return forms[2];
}
const PST_ST = ['станция', 'станции', 'станций'];
const PST_NAME = ['новое название', 'новых названия', 'новых названий'];
function pstKm(v) { return v == null ? '—' : String(v.toFixed(1)).replace('.', ',') + ' км'; }

// ─── Кнопки iOS ────────────────────────────────────────────────────────
//   primary   — заливка accent / accentInk, радиус 12, h50, sans 17/600
//   secondary — bgSubtle / accent (как «Новое плечо» в ShouldersSheet), радиус 12, h50, sans 16/500
//   plain     — текстовая accent (h по месту)
//   destructive — текстовая danger (только удаление)
function IpBtn({ t, kind = 'primary', icon, children, disabled, full, h = 50, fs, style }) {
  const k = {
    primary:     { bg: t.accent, fg: t.accentInk, fw: 600, fs: 17, r: M.r.md },
    secondary:   { bg: t.bgSubtle, fg: t.accent, fw: 500, fs: 16, r: M.r.md },
    plain:       { bg: 'transparent', fg: t.accent, fw: 500, fs: 15, r: M.r.md },
    destructive: { bg: 'transparent', fg: t.danger, fw: 500, fs: 15, r: M.r.md },
  }[kind];
  const filled = kind === 'primary' || kind === 'secondary';
  return (
    <button disabled={disabled} style={{
      height: h, padding: filled ? '0 16px' : '0 8px', borderRadius: k.r,
      width: full === true ? '100%' : undefined, flex: full === 'flex' ? 1 : undefined, minWidth: 0,
      background: disabled && filled ? t.bgSubtle : k.bg, color: disabled ? t.textFaint : k.fg,
      border: 'none', fontFamily: M.fontSans, fontSize: fs || k.fs, fontWeight: k.fw, letterSpacing: -0.2,
      cursor: disabled ? 'default' : 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      gap: 8, flexShrink: 0, whiteSpace: 'nowrap', ...style,
    }}>{icon}{children}</button>
  );
}

// ─── ProgressView() — лепестковый индикатор iOS ────────────────────────
function IpSpinner({ t, size = 20, color }) {
  const c = color || t.textMuted;
  return (
    <span style={{ width: size, height: size, position: 'relative', display: 'inline-block', flexShrink: 0, animation: 'ipspin 1s steps(8) infinite' }}>
      {Array.from({ length: 8 }).map((_, i) => (
        <span key={i} style={{
          position: 'absolute', left: '50%', top: 0, width: size * 0.12, height: size * 0.3, marginLeft: -size * 0.06,
          borderRadius: size, background: c, opacity: 0.2 + (i / 7) * 0.8,
          transformOrigin: `50% ${size / 2}px`, transform: `rotate(${i * 45}deg)`,
        }}></span>
      ))}
    </span>
  );
}

// ─── ProgressView(value:) — линейный, высота 4 ────────────────────────
function IpProgress({ t, value = 0.46 }) {
  return (
    <div style={{ height: 4, borderRadius: 2, background: t.bgSubtle, overflow: 'hidden' }}>
      <div style={{ width: `${value * 100}%`, height: '100%', borderRadius: 2, background: t.accent }}></div>
    </div>
  );
}

// ─── Круглая галка iOS (вместо чекбокса M3) ───────────────────────────
//   22; вкл — заливка accent + белая ✓; выкл — обводка 1.5 borderStrong; заблокирован — заливка textFaint
function IpCheck({ t, on, disabled }) {
  return (
    <span style={{
      width: 22, height: 22, borderRadius: 11, flexShrink: 0, boxSizing: 'border-box',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      background: on ? (disabled ? t.textFaint : t.accent) : 'transparent',
      border: on ? 'none' : `1.5px solid ${t.borderStrong}`, color: '#FFFFFF',
    }}>{on && <IcCheck width="13" height="13" style={{ strokeWidth: 3.2 }}/>}</span>
  );
}

// Галка выбора справа (RadioRow iOS)
function IpTick({ t }) {
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}><path d="M5 12l5 5L20 7"/></svg>;
}

// ⊖ — удаление строки в edit mode (List .onDelete)
function IpMinusBadge({ t }) {
  return (
    <span aria-label="Убрать" style={{ width: 22, height: 22, borderRadius: 11, background: t.danger, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
      <span style={{ width: 10, height: 2, borderRadius: 1, background: '#FFFFFF' }}></span>
    </span>
  );
}
function IpIcDrag(p) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" {...p}><path d="M4 8h16M4 12h16M4 16h16"/></svg>;
}
function IpIcClear(p) {
  return <svg width="18" height="18" viewBox="0 0 24 24" {...p}><circle cx="12" cy="12" r="10" fill="currentColor"/><path d="M8.5 8.5l7 7M15.5 8.5l-7 7" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round"/></svg>;
}

// ─── SegmentedToggle — копия из ios-add-train.jsx (Picker(.segmented)) ─
function SegmentedToggle({ t, options, active = 0 }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${options.length}, 1fr)`, gap: 2, padding: 2, borderRadius: 10, background: t.bgSubtle }}>
      {options.map((label, i) => (
        <div key={i} style={{
          padding: '8px 10px', textAlign: 'center', borderRadius: 8,
          background: i === active ? (t.surface || t.bg) : 'transparent',
          boxShadow: i === active ? '0 1px 3px rgba(0,0,0,0.08), 0 0 0 0.5px rgba(0,0,0,0.04)' : 'none',
          fontFamily: M.fontSans, fontSize: 14, fontWeight: i === active ? 600 : 500, color: t.text, cursor: 'pointer',
        }}>{label}</div>
      ))}
    </div>
  );
}

// ─── Шторка iOS (.sheet) ──────────────────────────────────────────────
//   Радиус 18, грэббер 36×5. Шапка: «Отмена» 16/400 — заголовок 17/700 — действие 16/600.
//   Неактивное действие — textFaint. top — отступ верха шторки от верха экрана (large detent);
//   height='auto' — по содержимому. scroll — сдвиг тела, чтобы показать «прокрученную» шторку.
function IpSheet({ t, title, sub, left = 'Отмена', right, rightDisabled, top = 64, height, z = 70, scroll = 0, footer, children, scrim = true, bodyPad = '6px 16px 34px' }) {
  const auto = height === 'auto';
  const btn = (label, bold, dis) => (
    <button style={{ background: 'transparent', border: 'none', padding: 0, cursor: 'pointer', fontFamily: M.fontSans, fontSize: 16, fontWeight: bold ? 600 : 400, color: dis ? t.textFaint : t.accent, whiteSpace: 'nowrap' }}>{label}</button>
  );
  return (
    <React.Fragment>
      {scrim && <div style={{ position: 'absolute', inset: 0, zIndex: z, background: 'rgba(0,0,0,0.45)' }}></div>}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: z + 1, top: auto ? undefined : top,
        background: t.bg, borderTopLeftRadius: M.r.sheetIOS, borderTopRightRadius: M.r.sheetIOS,
        boxShadow: M.shadow.sheet, display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 8 }}>
          <div style={{ width: 36, height: 5, borderRadius: 3, background: t.borderStrong }}></div>
        </div>
        <div style={{ height: 44, margin: '4px 0 0', display: 'grid', gridTemplateColumns: 'minmax(max-content,1fr) minmax(0,auto) minmax(max-content,1fr)', alignItems: 'center', columnGap: 8, padding: '0 16px' }}>
          <span style={{ justifySelf: 'start' }}>{left && btn(left, false)}</span>
          <span style={{ ...M.t.navTitle(t), whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', textAlign: 'center' }}>{title}</span>
          <span style={{ justifySelf: 'end' }}>{right && btn(right, true, rightDisabled)}</span>
        </div>
        {sub && <div style={{ ...M.t.captionMuted(t), textAlign: 'center', padding: '0 24px 6px', marginTop: -4, textWrap: 'balance', overflowWrap: 'anywhere' }}>{sub}</div>}
        <div style={{ flex: auto ? undefined : 1, minHeight: 0, overflow: 'hidden', position: 'relative' }}>
          <div style={{ marginTop: -scroll, padding: footer ? bodyPad.replace(/34px$/, '20px') : bodyPad }}>{children}</div>
        </div>
        {footer && (
          <div style={{ padding: '10px 16px 34px', borderTop: `1px solid ${t.border}`, background: t.bg, display: 'flex', flexDirection: 'column', gap: 10 }}>{footer}</div>
        )}
      </div>
    </React.Fragment>
  );
}

// ─── Поле формы в карточке (iOS grouped) ──────────────────────────────
//   Строка Card: мин. высота 56, padding 8 16; лейбл sans 12 textMuted (в фокусе accent);
//   значение sans 16/600, переносится по словам; плейсхолдер 16/400 textFaint.
function IpFieldRow({ t, label, value, placeholder, leading, suffix, focus, trailing, flex }) {
  const empty = !value;
  return (
    <div style={{ flex: flex || undefined, minWidth: 0, minHeight: 56, padding: '8px 16px', display: 'flex', alignItems: 'center', gap: 10, boxSizing: 'border-box' }}>
      {leading}
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
        <span style={{ fontFamily: M.fontSans, fontSize: 12, color: focus ? t.accent : t.textMuted }}>{label}</span>
        <span style={{ display: 'flex', alignItems: 'baseline', gap: 6, minWidth: 0 }}>
          <span style={{ fontFamily: M.fontSans, fontSize: 16, fontWeight: empty ? 400 : 600, color: empty ? t.textFaint : t.text, letterSpacing: -0.2, overflowWrap: 'anywhere', lineHeight: 1.25 }}>
            {empty ? placeholder : value}{focus && <span style={{ display: 'inline-block', width: 2, height: 19, background: t.accent, marginLeft: 1, verticalAlign: -3, borderRadius: 1 }}></span>}
          </span>
          {suffix && <span style={{ ...M.t.label(t), flexShrink: 0 }}>{suffix}</span>}
        </span>
      </div>
      {trailing}
    </div>
  );
}

// ─── Схема участка: строка ────────────────────────────────────────────
//   mode: 'view' | 'edit' (⊖ слева + ≡ справа) | 'pick' (круглая галка)
function IpStopRow({ t, s, first, last, mode = 'view', lifted, checked, already, pos }) {
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
      borderRadius: lifted || s.fresh ? 12 : 0, boxShadow: lifted ? M.shadow.floating : 'none',
      zIndex: lifted ? 2 : 1, transform: lifted ? 'scale(1.03)' : 'none',
    }}>
      <div style={{ position: 'relative', width: COL, flexShrink: 0 }}>
        {!lifted && <div style={{ position: 'absolute', left: COL / 2 - 0.75, top: first ? '50%' : 0, bottom: last ? '50%' : 0, borderLeft: `1.5px dashed ${t.borderStrong}` }}></div>}
        <div style={{
          position: 'absolute', left: (COL - dot) / 2, top: `calc(50% - ${dot / 2}px)`, width: dot, height: dot, borderRadius: dot / 2, boxSizing: 'border-box',
          background: term ? t.text : t.surface, border: term ? 'none' : `${op ? 1.5 : 2}px solid ${op ? t.textFaint : t.borderStrong}`,
        }}></div>
      </div>
      <div style={{ flex: 1, minWidth: 0, minHeight: op ? 48 : 56, padding: '8px 4px', display: 'flex', alignItems: 'center', gap: 10, borderBottom: last || lifted ? 'none' : `0.5px solid ${t.border}` }}>
        {mode === 'edit' && (term ? <span style={{ width: 22, flexShrink: 0 }}></span> : <IpMinusBadge t={t}/>)}
        {mode === 'pick' && !term && (already ? <IpCheck t={t} on disabled/> : <IpCheck t={t} on={checked}/>)}
        <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 3 }}>
          <div style={{
            fontFamily: M.fontSans, fontSize: op ? 14 : 15, fontWeight: op ? 500 : 600, color: op ? t.textMuted : t.text, lineHeight: 1.25, letterSpacing: -0.2,
            overflowWrap: 'anywhere', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
          }}>{s.name}</div>
          <div style={{ fontFamily: M.fontMono, fontSize: 11, fontWeight: 500, color: t.textFaint, display: 'flex', flexWrap: 'wrap', columnGap: 6 }}>
            <span style={{ whiteSpace: 'nowrap' }}>{sub.join(' · ')}</span>
            {!term && s.coords === false && <span style={{ color: t.warning, whiteSpace: 'nowrap' }}>нет координат</span>}
            {already && <span style={{ color: t.accent, whiteSpace: 'nowrap' }}>уже в списке</span>}
          </div>
        </div>
        <span style={{ fontFamily: M.fontMono, fontSize: 13, fontWeight: 500, color: s.km == null && !term ? t.textFaint : t.textMuted, flexShrink: 0 }}>
          {term && pos === 'start' ? '0 км' : term && Number.isInteger(s.km) ? s.km + ' км' : pstKm(s.km)}
        </span>
        {mode === 'view' && (term ? <span style={{ width: 14, flexShrink: 0 }}></span> : <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>)}
        {mode === 'edit' && (term
          ? <span style={{ width: 36, flexShrink: 0 }}></span>
          : <span aria-label="Перетащить" style={{ width: 36, height: 40, marginRight: -6, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', color: lifted ? t.accent : t.textFaint, flexShrink: 0 }}><IpIcDrag/></span>)}
      </div>
    </div>
  );
}

// «+» вставки на линии (edit mode)
function IpInsertRow({ t, active }) {
  return (
    <div style={{ position: 'relative', display: 'flex', alignItems: 'center', height: 30 }}>
      <div style={{ position: 'absolute', left: 15.25, top: 0, bottom: 0, borderLeft: `1.5px dashed ${t.borderStrong}` }}></div>
      <span style={{
        position: 'relative', marginLeft: 5, width: 22, height: 22, borderRadius: 11,
        background: active ? t.accent : t.surface, color: active ? t.accentInk : t.accent,
        border: active ? 'none' : `1.5px solid ${hexToRgba(t.accent, .5)}`, boxSizing: 'border-box',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      }}><IcPlus width="12" height="12" style={{ strokeWidth: 2.6 }}/></span>
      <span style={{ marginLeft: 10, fontFamily: M.fontSans, fontSize: 12, fontWeight: 500, color: active ? t.accent : t.textFaint }}>{active ? 'Вставить станцию сюда' : 'вставить'}</span>
    </div>
  );
}

function IpLine({ t, from, to, km, stops, mode = 'view', lifted, insertActive }) {
  const all = [{ name: from, type: 'term', pos: 'start' }, ...stops, { name: to, type: 'term', km, pos: 'end' }];
  const out = [];
  all.forEach((s, i) => {
    if (mode === 'edit' && i > 0) out.push(<IpInsertRow key={'ins' + i} t={t} active={insertActive === i}/>);
    out.push(<IpStopRow key={i} t={t} s={s} pos={s.pos} first={i === 0} last={i === all.length - 1} mode={mode} lifted={lifted === i}/>);
  });
  return <div style={{ padding: '4px 10px 4px 6px' }}>{out}</div>;
}

// ─── Плашка (warning / accent @ 12%) ─────────────────────────────────
function IpNotice({ t, tone = 'warning', title, children, action }) {
  const c = tone === 'warning' ? t.warning : t.accent;
  return (
    <div style={{ borderRadius: M.r.md, background: hexToRgba(c, .12), padding: '12px 14px', display: 'flex', gap: 12 }}>
      <span style={{ color: c, flexShrink: 0, marginTop: 1 }}><IcInfo width="18" height="18"/></span>
      <div style={{ flex: 1, minWidth: 0 }}>
        {title && <div style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 600, color: t.text, marginBottom: 2 }}>{title}</div>}
        <div style={{ ...M.t.hint(t), color: t.text, opacity: .8, textWrap: 'pretty' }}>{children}</div>
        {action && <div style={{ marginTop: 4, marginLeft: -8 }}>{action}</div>}
      </div>
    </div>
  );
}

// ─── Тост-плашка (вместо Snackbar) ────────────────────────────────────
//   Над home indicator: left/right 12, bottom 34, радиус 14, cta / ctaInk.
function IpToast({ t, dark, text, action, bottom = 34 }) {
  return (
    <div style={{
      position: 'absolute', left: 12, right: 12, bottom, zIndex: 90, borderRadius: 14, background: t.cta, color: t.ctaInk, boxShadow: M.shadow.floating,
      padding: '8px 8px 8px 16px', display: 'flex', alignItems: 'center', gap: 8, minHeight: 50, boxSizing: 'border-box',
    }}>
      <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 14, fontWeight: 500, lineHeight: 1.35 }}>{text}</span>
      <button style={{ border: 'none', background: 'transparent', height: 34, padding: '0 10px', fontFamily: M.fontSans, fontSize: 15, fontWeight: 600, color: dark ? M.light.accentHover : M.dark.accent, cursor: 'pointer', flexShrink: 0 }}>{action}</button>
    </div>
  );
}

// ─── Данные (те же, что в Android) ───────────────────────────────────
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
const PST_LONG = [
  { name: 'Шушары', type: 'st', km: 9.8, esr: '031105', coords: true },
  { name: 'Детское Село', type: 'st', km: 17.4, esr: '031113', coords: true },
  { name: 'Павловск', type: 'st', km: 22.9, esr: '031121', coords: true },
  ...['Антропшино:op', 'Новолисино:st', 'Кобралово:op', 'Вырица:st', 'Ефимовская:op', 'Строганово-Витебская:op', 'Семрино:st', 'Посёлок:op', 'Слудицы:st', 'Чаща:op', 'Кемка-Витебская:op', 'Оредеж:st', 'Чолово:op', 'Разъезд 81 км:st', 'Тёплое:op', 'Подгорное:op', 'Пристань:st', 'Мельница:op', 'Жельцы:st', 'Кривино:op', 'Толмачёво-Товарное:st', 'Заполье:op', 'Остров:op', 'Бор:st', 'Шапки:op', 'Партизанская:st', 'Раковичи:op', 'Серебрянка-Сортировочная:st', 'Дивенская-Пассажирская:op', 'Разъезд 102 км:st', 'Низовская-2:op', 'Мшинская-Товарная:st', 'Ящера:op']
    .map((x, i) => { const [n, ty] = x.split(':'); return { name: n, type: ty, km: +(26.4 + i * 2.55).toFixed(1), esr: ty === 'st' ? String(31130 + i * 8).padStart(6, '0') : undefined, coords: i !== 7 && i !== 19 }; }),
  { name: 'Мшинская', type: 'st', km: 111.2, esr: '030345', coords: true },
  { name: 'Толмачёво', type: 'st', km: 125.9, esr: '030353', coords: true },
];
const PST_FOUND = [
  { name: 'Татьянино', type: 'op', km: 3.9 },
  { name: 'Суйда', type: 'st', km: 10.6, esr: '030310' },
  { name: 'Прибытково', type: 'op', km: 15.2 },
  { name: 'Старосиверская', type: 'op', km: 21.0 },
  { name: 'Сиверская', type: 'st', km: 25.1, esr: '030329' },
  { name: 'Строганово', type: 'op', km: 30.4 },
  { name: 'Дивенская', type: 'st', km: 40.2, esr: '030337' },
  { name: 'Низовская', type: 'op', km: 45.8 },
  { name: 'Мшинская', type: 'st', km: 53.6, esr: '030345' },
  { name: 'Кемка', type: 'op', km: 59.1 },
  { name: 'Толмачёво', type: 'st', km: 68.3, esr: '030353' },
  { name: 'Разъезд 73 км', type: 'st', km: 73.0, esr: '030361' },
  { name: 'Серебрянка', type: 'op', km: 76.7 },
];

Object.assign(window, {
  pstPlural, pstKm, PST_ST, PST_NAME, IpBtn, IpSpinner, IpProgress, IpCheck, IpTick, IpMinusBadge, IpIcDrag, IpIcClear,
  SegmentedToggle, IpSheet, IpFieldRow, IpStopRow, IpInsertRow, IpLine, IpNotice, IpToast,
  PST_A, PST_B, PST_SHORT, PST_LONG, PST_FOUND,
});
