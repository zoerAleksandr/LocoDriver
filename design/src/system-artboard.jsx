// System spec artboard — colors, type, components, icons

function Swatch({ name, hex, dark = false }) {
  const txt = dark ? '#F2EFE8' : '#14120E';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <div style={{
        width: '100%', height: 56, background: hex, borderRadius: 10,
        boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.05)',
      }} />
      <div style={{ fontSize: 11, fontFamily: M.fontMono, color: txt, opacity: .7, letterSpacing: -.2 }}>
        {name}
      </div>
      <div style={{ fontSize: 11, fontFamily: M.fontMono, color: txt, opacity: .9, letterSpacing: -.2 }}>
        {hex}
      </div>
    </div>
  );
}

function ThemeBlock({ label, theme, dark }) {
  const t = M[theme];
  return (
    <div style={{
      background: t.bg, padding: 28, borderRadius: 16,
      boxShadow: M.shadow.sm, flex: 1, minWidth: 0,
    }}>
      <div style={{
        fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.4,
        color: t.textMuted, textTransform: 'uppercase', marginBottom: 4,
      }}>Theme</div>
      <div style={{ fontFamily: M.fontDisplay, fontSize: 20, fontWeight: 700, color: t.text, marginBottom: 20 }}>
        {label}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14 }}>
        <Swatch name="bg" hex={t.bg} dark={dark}/>
        <Swatch name="surface" hex={t.surface} dark={dark}/>
        <Swatch name="accent" hex={t.accent} dark={dark}/>
        <Swatch name="text" hex={t.text} dark={dark}/>
        <Swatch name="surfaceAlt" hex={t.surfaceAlt} dark={dark}/>
        <Swatch name="border" hex={t.borderStrong} dark={dark}/>
        <Swatch name="success" hex={t.success} dark={dark}/>
        <Swatch name="warning" hex={t.warning} dark={dark}/>
      </div>
    </div>
  );
}

function TypeSample({ label, size, weight, mono = false, sample, t }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 24, padding: '14px 0', borderBottom: `1px solid ${t.border}` }}>
      <div style={{ width: 120, fontFamily: M.fontMono, fontSize: 11, color: t.textMuted }}>
        {label}
        <div style={{ opacity: .6 }}>{size}/{weight}</div>
      </div>
      <div style={{
        fontFamily: mono ? M.fontMono : M.fontSans, fontSize: size, fontWeight: weight,
        color: t.text, letterSpacing: size >= 32 ? -0.5 : -0.2, fontVariantNumeric: 'tabular-nums',
      }}>
        {sample}
      </div>
    </div>
  );
}

function SystemArtboard() {
  const t = M.light;
  return (
    <div style={{
      width: 1400, minHeight: 1800, background: t.bg, fontFamily: M.fontSans,
      color: t.text, padding: 56, boxSizing: 'border-box',
    }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
        <div style={{ color: t.text }}><IcLogo size={40}/></div>
        <div>
          <div style={{ fontSize: 30, fontWeight: 800, letterSpacing: -1 }}>Машинист</div>
          <div style={{ fontSize: 13, color: t.textMuted, fontFamily: M.fontMono, letterSpacing: 1 }}>
            DESIGN SYSTEM · v1 · 23.04.2026
          </div>
        </div>
      </div>
      <div style={{ fontSize: 16, color: t.textMuted, maxWidth: 680, marginBottom: 40, lineHeight: 1.5 }}>
        Приложение для учёта рабочего времени, маршрутов и зарплаты локомотивных бригад.
        Тон — дружелюбный, но с техничной точностью цифр и железнодорожной визуальной ДНК.
      </div>

      {/* Brand marks */}
      <div style={{
        display: 'flex', gap: 16, marginBottom: 48, alignItems: 'stretch',
      }}>
        <div style={{
          flex: 1, background: t.surface, borderRadius: 20, padding: 28,
          display: 'flex', alignItems: 'center', gap: 20, boxShadow: M.shadow.sm,
        }}>
          <div style={{ color: t.text }}><IcLogo size={64}/></div>
          <div>
            <div style={{ fontSize: 13, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.2 }}>МОНОГРАММА</div>
            <div style={{ fontSize: 15, color: t.text, marginTop: 4 }}>
              Монолитная «М», Inter Black.<br/>Два цвета — графит и бумага.
            </div>
          </div>
        </div>
        <div style={{
          width: 300, background: '#14120E', color: '#F2EFE8', borderRadius: 20, padding: 28,
          display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 10,
        }}>
          <div style={{ color: '#F2EFE8' }}><IcLogo size={48}/></div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -.6 }}>Машинист</div>
          <div style={{ fontSize: 12, fontFamily: M.fontMono, opacity: .6, letterSpacing: 1 }}>DARK LOCKUP</div>
        </div>
      </div>

      {/* Colors */}
      <SectionTitle t={t} n="01" title="Цвета" sub="Светлая и тёмная темы"/>
      <div style={{ display: 'flex', gap: 20, marginBottom: 48 }}>
        <ThemeBlock label="Light" theme="light" dark={false}/>
        <ThemeBlock label="Dark"  theme="dark"  dark={true}/>
      </div>

      {/* Typography */}
      <SectionTitle t={t} n="02" title="Типографика" sub="Inter как база · JetBrains Mono для цифр и меток"/>
      <div style={{ background: t.surface, borderRadius: 20, padding: '8px 28px', marginBottom: 48, boxShadow: M.shadow.sm }}>
        <TypeSample t={t} label="Display" size={48} weight={800} sample="205:05 сверх 30:05"/>
        <TypeSample t={t} label="Title L" size={32} weight={700} sample="Маршрут №112"/>
        <TypeSample t={t} label="Title M" size={22} weight={700} sample="Время работы"/>
        <TypeSample t={t} label="Body"    size={17} weight={500} sample="Добавить локомотив"/>
        <TypeSample t={t} label="Caption" size={13} weight={500} sample="Основные данные"/>
        <TypeSample t={t} label="Mono / Num" size={15} weight={500} mono sample="24 APR 2026 · 20:00 → 08:00"/>
      </div>

      {/* Iconography */}
      <SectionTitle t={t} n="03" title="Иконки" sub="Кастомный ж/д набор · 24px · 1.75 stroke · currentColor"/>
      <div style={{
        background: t.surface, borderRadius: 20, padding: 28, marginBottom: 48,
        boxShadow: M.shadow.sm,
        display: 'grid', gridTemplateColumns: 'repeat(12, 1fr)', gap: 20,
      }}>
        {[
          ['Loco', IcLocomotive], ['Trains', IcTrains],
          ['Calendar', IcCalendar], ['Clock', IcClock], ['Ruble', IcRuble],
          ['Home', IcHome], ['Пассажиром', IcPassenger], ['Два лица', IcCrewPair],
          ['В одно лицо', IcCrewSolo], ['Armchair', IcArmchair], ['Cloud', IcCloud],
          ['Document', IcDocument], ['Sliders', IcSliders],
        ].map(([n, I]) => (
          <div key={n} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, color: t.text }}>
            <div style={{
              width: 56, height: 56, borderRadius: 14, background: t.bgSubtle,
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.accent,
            }}><I width="28" height="28"/></div>
            <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted }}>{n}</div>
          </div>
        ))}
      </div>

      {/* Components */}
      <SectionTitle t={t} n="04" title="Компоненты" sub="Кнопки, карточки, значки, инпуты"/>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 48 }}>
        <div style={{ background: t.surface, borderRadius: 20, padding: 28, boxShadow: M.shadow.sm }}>
          <Label t={t}>КНОПКИ</Label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 14 }}>
            <Btn t={t} variant="primary">Сохранить</Btn>
            <Btn t={t} variant="ghost">Отмена</Btn>
            <Btn t={t} variant="secondary"><IcPlus width="16" height="16"/> Добавить поезд</Btn>
            <Btn t={t} variant="danger"><IcTrash width="16" height="16"/> Удалить</Btn>
          </div>
        </div>
        <div style={{ background: t.surface, borderRadius: 20, padding: 28, boxShadow: M.shadow.sm }}>
          <Label t={t}>ЗНАЧКИ</Label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 14 }}>
            <Pill t={t} tone="accent">Сверхурочно</Pill>
            <Pill t={t} tone="success">Синхронизировано</Pill>
            <Pill t={t} tone="muted">Черновик</Pill>
            <Pill t={t} tone="warning">180 ч</Pill>
          </div>
        </div>
        <div style={{ background: t.surface, borderRadius: 20, padding: 28, boxShadow: M.shadow.sm }}>
          <Label t={t}>ПОЛЕ ВВОДА</Label>
          <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{
              background: t.bgSubtle, borderRadius: 12, padding: '14px 16px',
              fontSize: 16, color: t.text,
              border: `1px solid ${t.border}`,
            }}>№ <span style={{ fontFamily: M.fontMono }}>112</span></div>
            <div style={{
              background: t.bgSubtle, borderRadius: 12, padding: '14px 16px',
              fontSize: 16, color: t.text,
              border: `2px solid ${t.accent}`,
            }}>Заметки…</div>
          </div>
        </div>
        <div style={{ background: t.surface, borderRadius: 20, padding: 28, boxShadow: M.shadow.sm }}>
          <Label t={t}>ПРОГРЕСС</Label>
          <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 14 }}>
            <Progress t={t} label="Норма на месяц" value={175} max={200} />
            <Progress t={t} label="Отработано" value={181} max={200} over/>
          </div>
        </div>
      </div>

      {/* Principles */}
      <SectionTitle t={t} n="05" title="Принципы" sub="Что делает Машинист — Машинистом"/>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
        {[
          ['Точные цифры', 'Моноширинный для часов и денег. Всегда табличные цифры — ничего не «прыгает».'],
          ['Сигнальный красный', 'Один акцент на всё приложение. Красный — это кнопка, итог, важное.'],
          ['Тёплая бумага', 'Нейтральные бежевые фоны, без холодного айфонного серого. Работа на смене, а не в айти-офисе.'],
        ].map(([h, b]) => (
          <div key={h} style={{ background: t.surface, borderRadius: 20, padding: 28, boxShadow: M.shadow.sm }}>
            <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>{h}</div>
            <div style={{ fontSize: 14, color: t.textMuted, lineHeight: 1.5 }}>{b}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SectionTitle({ t, n, title, sub }) {
  return (
    <div style={{ marginBottom: 20, display: 'flex', alignItems: 'baseline', gap: 14 }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 12, color: t.accent, letterSpacing: 2 }}>{n}</div>
      <div style={{ fontSize: 24, fontWeight: 700, letterSpacing: -.5 }}>{title}</div>
      <div style={{ fontSize: 14, color: t.textMuted }}>{sub}</div>
    </div>
  );
}

function Label({ t, children }) {
  return <div style={{
    fontFamily: M.fontMono, fontSize: 11, color: t.textMuted,
    letterSpacing: 1.4, textTransform: 'uppercase',
  }}>{children}</div>;
}

function Btn({ t, variant, children }) {
  const styles = {
    primary: { bg: t.accent, col: t.accentInk, br: 'transparent' },
    secondary: { bg: t.accentSoft, col: t.accent, br: 'transparent' },
    ghost: { bg: 'transparent', col: t.text, br: t.borderStrong },
    danger: { bg: 'transparent', col: t.danger, br: t.danger },
  }[variant];
  return (
    <button style={{
      background: styles.bg, color: styles.col, border: `1px solid ${styles.br}`,
      padding: '10px 16px', borderRadius: 12, fontSize: 14, fontWeight: 600,
      fontFamily: M.fontSans, display: 'inline-flex', alignItems: 'center', gap: 8,
      cursor: 'pointer',
    }}>{children}</button>
  );
}

function Pill({ t, tone, children }) {
  const map = {
    accent:  { bg: t.accentSoft, col: t.accent },
    success: { bg: 'rgba(31,138,63,0.12)', col: t.success },
    warning: { bg: 'rgba(224,129,0,0.14)', col: t.warning },
    muted:   { bg: t.bgSubtle, col: t.textMuted },
  }[tone];
  return (
    <span style={{
      background: map.bg, color: map.col,
      padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 600,
      fontFamily: M.fontMono, letterSpacing: .2,
    }}>{children}</span>
  );
}

function Progress({ t, label, value, max, over }) {
  const pct = Math.min(100, (value / max) * 100);
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
        <span style={{ color: t.textMuted }}>{label}</span>
        <span style={{ fontFamily: M.fontMono, color: over ? t.accent : t.text, fontWeight: 600 }}>
          {value} / {max} ч
        </span>
      </div>
      <div style={{ height: 6, background: t.bgSubtle, borderRadius: 99, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: over ? t.accent : t.rail }} />
      </div>
    </div>
  );
}

Object.assign(window, { SystemArtboard });
