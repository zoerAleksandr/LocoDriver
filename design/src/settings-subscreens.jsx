// ════════════════════════════════════════════════════════════════
// ПОДЭКРАНЫ НАСТРОЕК — Норма/Регион · Учёт · Плечи · Локомотив
//
//   Доделанные подразделы из родительского экрана «Настройки».
//   Исходные Samsung-скрины переведены в общий стиль приложения —
//   тот же язык, что уже принят в подразделах «Основные» и «Зарплата»:
//     • UPPERCASE-MONO GroupHead вместо системных bold-заголовков;
//     • контент в Card со строками SwitchRow / SelectRow / FieldRow;
//     • поясняющий текст один раз — SectionNote под карточкой;
//     • dropdown-поля справочников (страна, регион, пояс) — SelectField.
//
//   Пока только iOS · light (компоненты принимают dark — тёмная и
//   Android добавляются тем же телом позже).
// ════════════════════════════════════════════════════════════════

// Шеврон «вниз» для dropdown-полей (нет в icons.jsx)
function IcChevronDown(p) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M6 9l6 6 6-6"/>
    </svg>
  );
}

// ─── Dropdown-поле: full-width значение + шеврон-вниз ────────────
//   Используется там, где исходник показывал системный <select>
//   (Страна, Регион, Часовой пояс, Переходные маршруты).
function SelectField({ t, value, leading }) {
  return (
    <button style={{
      width: '100%', textAlign: 'left',
      display: 'flex', alignItems: 'center', gap: 10,
      minHeight: 52, padding: '0 16px',
      borderRadius: M.r.input,
      background: t.surface, border: 'none',
      boxShadow: M.shadow.sm, cursor: 'pointer',
    }}>
      {leading && (
        <span style={{ fontSize: 19, lineHeight: 1, flexShrink: 0 }}>{leading}</span>
      )}
      <span style={{
        flex: 1, minWidth: 0,
        fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text,
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
      }}>{value}</span>
      <IcChevronDown style={{ color: t.textMuted, flexShrink: 0 }}/>
    </button>
  );
}

// ─── Строка плеча: маршрут слева + км справа ─────────────────────
function ShoulderRow({ t, name, km }) {
  return (
    <button style={{
      width: '100%', textAlign: 'left',
      padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 12,
      background: 'transparent', border: 'none', cursor: 'pointer',
    }}>
      <span style={{
        flex: 1, minWidth: 0,
        fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text,
      }}>{name}</span>
      <span style={{
        fontFamily: M.fontMono, fontSize: 14, fontWeight: 600, color: t.textMuted,
        flexShrink: 0,
      }}>{km} км</span>
      <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </button>
  );
}


// ════════════════════════════════════════════════════════════════
// iOS · Настройки → Норма / Регион
// ════════════════════════════════════════════════════════════════
function IOSScreenSettingsNorms({ dark = false, height = 1180, sheet = null }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Норма / Регион" back backLabel="Настройки" style="text"/>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '4px 16px 36px' }}>
        <GroupHead t={t} mt={8}>Норма часов</GroupHead>
        <Card t={t}>
          <FieldRow t={t} label="Июнь 2026" value="167:00" mono/>
          <Sep t={t} inset={20}/>
          <button style={{
            width: '100%', textAlign: 'left',
            padding: '13px 20px', background: 'transparent', border: 'none',
            color: t.accent, fontFamily: M.fontSans, fontSize: 14, fontWeight: 600,
            cursor: 'pointer',
          }}>Изменить норму</button>
        </Card>
        <SectionNote t={t}>
          Месячная норма рабочего времени. По ней считается переработка и недоработка.
        </SectionNote>

        <GroupHead t={t}>Страна</GroupHead>
        <SelectField t={t} leading="🇷🇺" value="Россия"/>

        <GroupHead t={t}>Регион</GroupHead>
        <SelectField t={t} value="Стандартный календарь"/>
        <SectionNote t={t}>
          Региональные праздники добавятся к стандартному календарю.
        </SectionNote>

        <GroupHead t={t}>Домашний часовой пояс</GroupHead>
        <SelectField t={t} value="Омск (MSK+3, UTC+6)"/>
        <SectionNote t={t}>
          Местный часовой пояс. Учитывается при расчёте ночных, праздничных часов и переходных поездок.
        </SectionNote>

        <GroupHead t={t}>Переходные маршруты</GroupHead>
        <SelectField t={t} value="По местному времени"/>
        <SectionNote t={t}>
          Определяет, по какому времени переходной маршрут относится к месяцу.
        </SectionNote>
      </div>

      {sheet === 'norm' && <NormEditSheet t={t}/>}
      {sheet === 'country' && <OptionPickerSheet t={t} title="Страна" options={COUNTRY_OPTIONS}/>}
      {sheet === 'region' && <OptionPickerSheet t={t} title="Регион" hint="Региональные праздники добавятся к стандартному календарю." options={REGION_OPTIONS}/>}
      {sheet === 'timezone' && <OptionPickerSheet t={t} title="Часовой пояс" options={TIMEZONE_OPTIONS} height="84%"/>}
      {sheet === 'transition' && <OptionPickerSheet t={t} title="Переходные маршруты" options={TRANSITION_OPTIONS}/>}
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════
// iOS · Настройки → Учёт
// ════════════════════════════════════════════════════════════════
function IOSScreenSettingsAccounting({ dark = false, height = 760, sheet = null }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Учёт" back backLabel="Настройки" style="text"/>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '4px 16px 36px' }}>
        <GroupHead t={t} mt={8}>Ночные часы</GroupHead>
        <Card t={t}>
          <SelectRow t={t} label="Ночь" value="22:00 – 06:00"/>
        </Card>
        <SectionNote t={t}>
          Интервал ночных часов для расчёта доплаты за работу ночью.
        </SectionNote>

        <GroupHead t={t}>Будущие маршруты</GroupHead>
        <Card t={t}>
          <SwitchRow t={t}
            label="Учитывать будущие маршруты"
            sub="С ещё не наступившей явкой"
            on={false}/>
        </Card>
        <SectionNote t={t}>
          Маршруты, время явки которых не наступило, будут учитываться при подсчёте отработанного времени.
        </SectionNote>
      </div>

      {sheet === 'night' && <NightRangeSheet t={t}/>}
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════
// iOS · Настройки → Плечи (справочник расстояний)
// ════════════════════════════════════════════════════════════════
const SHOULDERS_DATA = [
  ['Лужская – Луга', 250],
  ['Луга – Лужская', 250],
  ['Луга – Веймарн', 200],
  ['Веймарн – Луга', 200],
  ['Лужская – СПбСМ', 200],
  ['СПбСМ – Лужская', 200],
  ['Луга – Ульрамар', 280],
  ['Ульрамар – Луга', 280],
  ['Лужская – Гатчина ТБ', 150],
  ['Гатчина ТБ – Лужская', 150],
];

function IOSScreenSettingsShoulders({ dark = false, height = 1080, sheet = null }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Плечи" back backLabel="Настройки" style="text" trailing="Изменить"/>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '12px 16px 36px' }}>
        <Hint t={t}>
          Расстояние участка обслуживания. Подставляется автоматически при выборе плеча в маршруте.
        </Hint>

        <GroupHead t={t} mt={20}>Плечи · {SHOULDERS_DATA.length}</GroupHead>
        <Card t={t}>
          {SHOULDERS_DATA.map(([name, km], i) => (
            <React.Fragment key={name}>
              {i > 0 && <Sep t={t} inset={18}/>}
              <ShoulderRow t={t} name={name} km={km}/>
            </React.Fragment>
          ))}
        </Card>

        <AddBtn t={t} icon={<IcPlus/>} label="Добавить плечо"/>
      </div>

      {sheet === 'add' && <AddShoulderSheet t={t}/>}
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════
// iOS · Настройки → Локомотив (поля ввода показаний)
// ════════════════════════════════════════════════════════════════
function IOSScreenSettingsLocomotive({ dark = false, height = 920, sheet = null }) {
  const t = dark ? M.dark : M.light;
  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Локомотив" back backLabel="Настройки" style="text"/>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '4px 16px 36px' }}>
        <GroupHead t={t} mt={8}>Поля ввода показаний</GroupHead>
        <Card t={t}>
          <SwitchRow t={t} label="Отопление" on={true}/>
          <Sep t={t} inset={18}/>
          <SwitchRow t={t} label="Собственные нужды" on={false}/>
          <Sep t={t} inset={18}/>
          <SwitchRow t={t} label="Статистика" on={false}/>
          <Sep t={t} inset={18}/>
          <SwitchRow t={t} label="Норма" on={true}/>
        </Card>
        <SectionNote t={t}>
          Показывать эти поля в форме локомотива для ввода показаний.
        </SectionNote>

        <GroupHead t={t}>Род тока</GroupHead>
        <Card t={t}>
          <SwitchRow t={t} label="Смена рода тока" on={false}/>
        </Card>
        <SectionNote t={t}>
          Для переменно-постоянных электровозов.
        </SectionNote>

        <GroupHead t={t}>По умолчанию</GroupHead>
        <Card t={t}>
          <SelectRow t={t} label="Вид тяги" value="Электротяга"/>
        </Card>
        <SectionNote t={t}>
          Будет выбран при добавлении локомотива.
        </SectionNote>
      </div>

      {sheet === 'traction' && <OptionPickerSheet t={t} title="Вид тяги" options={TRACTION_OPTIONS}/>}
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════
// ДИАЛОГИ И ПИКЕРЫ ПОДЭКРАНОВ НАСТРОЕК
//   Все — bottom-sheet поверх затемнённого экрана (общий каркас
//   BottomSheet + SheetHeader из shared-ui), как date-sheet в Зарплате
//   и пикеры в Календаре. Значения — иллюстративные.
// ════════════════════════════════════════════════════════════════

// Зелёная «галка выбора» (iOS-стиль) для пикеров
function PickCheck({ t }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
      stroke={t.accent} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"
      style={{ flexShrink: 0 }}>
      <path d="M5 12l5 5L20 7"/>
    </svg>
  );
}

// ─── Универсальный пикер: список опций c выбором ────────────────
//   option: { label, sub?, leading?(emoji/JSX), selected? }
//   search — показать поле поиска сверху (для длинных списков, напр. пояса)
function OptionPickerSheet({ t, title, hint, options, search, height }) {
  const list = (
    <div style={{
      display: 'flex', flexDirection: 'column',
      ...(height ? { overflowY: 'auto', flex: 1, margin: '0 -20px', padding: '0 20px' } : {}),
    }}>
      {options.map((o, i) => (
        <React.Fragment key={o.label}>
          {i > 0 && <Sep t={t} inset={o.leading ? 40 : 0}/>}
          <button style={{
            width: '100%', textAlign: 'left', background: 'transparent', border: 'none',
            cursor: 'pointer', padding: '14px 2px',
            display: 'flex', alignItems: 'center', gap: 14,
          }}>
            {o.leading && (
              typeof o.leading === 'string'
                ? <span style={{ fontSize: 22, lineHeight: 1, width: 26, textAlign: 'center', flexShrink: 0 }}>{o.leading}</span>
                : <span style={{ width: 26, display: 'inline-flex', justifyContent: 'center', color: t.textMuted, flexShrink: 0 }}>{o.leading}</span>
            )}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{
                fontFamily: M.fontSans, fontSize: 16, fontWeight: o.selected ? 600 : 500,
                color: t.text,
              }}>{o.label}</div>
              {o.sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{o.sub}</div>}
            </div>
            {o.selected && <PickCheck t={t}/>}
          </button>
        </React.Fragment>
      ))}
    </div>
  );

  return (
    <BottomSheet t={t} height={height}>
      <SheetHeader t={t} title={title} action={{ label: 'Готово' }} hint={hint}/>
      {search && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8,
          background: t.bgSubtle, borderRadius: M.r.input,
          padding: '10px 14px', marginBottom: 8,
        }}>
          <IcSearch width="16" height="16" style={{ color: t.textMuted }}/>
          <span style={{ fontFamily: M.fontSans, fontSize: 15, color: t.textFaint }}>Поиск пояса…</span>
        </div>
      )}
      {list}
    </BottomSheet>
  );
}

// ─── Данные пикеров ─────────────────────────────────────────────
const COUNTRY_OPTIONS = [
  { leading: '🇷🇺', label: 'Россия',    selected: true },
  { leading: '🇧🇾', label: 'Беларусь' },
  { leading: '🇰🇿', label: 'Казахстан' },
];
const REGION_OPTIONS = [
  { label: 'Стандартный календарь', sub: 'Только федеральные праздники', selected: true },
  { label: 'Татарстан',        sub: '+ Курбан-байрам, Ураза-байрам' },
  { label: 'Башкортостан',     sub: '+ Республиканские праздники' },
  { label: 'Якутия (Саха)',    sub: '+ Ысыах, День Республики' },
  { label: 'Крым',             sub: '+ День Республики Крым' },
  { label: 'Бурятия',          sub: '+ Сагаалган' },
];
const TIMEZONE_OPTIONS = [
  { label: 'Калининград', sub: 'MSK−1 · UTC+2' },
  { label: 'Москва',      sub: 'MSK · UTC+3' },
  { label: 'Самара',      sub: 'MSK+1 · UTC+4' },
  { label: 'Екатеринбург',sub: 'MSK+2 · UTC+5' },
  { label: 'Омск',        sub: 'MSK+3 · UTC+6', selected: true },
  { label: 'Красноярск',  sub: 'MSK+4 · UTC+7' },
  { label: 'Иркутск',     sub: 'MSK+5 · UTC+8' },
  { label: 'Якутск',      sub: 'MSK+6 · UTC+9' },
  { label: 'Владивосток', sub: 'MSK+7 · UTC+10' },
  { label: 'Магадан',     sub: 'MSK+8 · UTC+11' },
  { label: 'Камчатка',    sub: 'MSK+9 · UTC+12' },
];
const TRANSITION_OPTIONS = [
  { label: 'По местному времени',  sub: 'Время явки на маршрут', selected: true },
  { label: 'По московскому времени', sub: 'Единое время для всех поясов' },
];
const TRACTION_OPTIONS = [
  { leading: <IcFlash width="18" height="18"/>,   label: 'Электротяга',     selected: true },
  { leading: <IcDroplet width="18" height="18"/>, label: 'Тепловозная тяга' },
];

// ─── Редактор нормы часов (из «Изменить норму») ─────────────────
//   Месячную норму можно задать вручную ИЛИ уменьшить отвлечениями
//   (отпуск/болезнь) — для этого ведём в календарный flow.
function NormEditSheet({ t }) {
  const seg = (val, active) => (
    <div style={{
      minWidth: 72, padding: '12px 0', textAlign: 'center',
      borderRadius: M.r.input,
      background: active ? t.accentSoft : t.bgSubtle,
      border: `1px solid ${active ? t.accent : 'transparent'}`,
      fontFamily: M.fontMono, fontSize: 26, fontWeight: 700,
      color: active ? t.accent : t.text, fontVariantNumeric: 'tabular-nums',
    }}>{val}</div>
  );
  return (
    <BottomSheet t={t}>
      <SheetHeader t={t} title="Норма часов" action={{ label: 'Отмена' }} hint="Июнь 2026"/>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, margin: '6px 0 4px' }}>
        {seg('167', true)}
        <span style={{ fontFamily: M.fontMono, fontSize: 26, fontWeight: 700, color: t.textMuted }}>:</span>
        {seg('00', false)}
      </div>
      <div style={{ textAlign: 'center', ...M.t.captionMuted(t), marginBottom: 14 }}>часы : минуты</div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
        {['159:00', '167:00', '175:00'].map((p) => {
          const active = p === '167:00';
          return (
            <button key={p} style={{
              flex: 1, padding: '9px 0', borderRadius: M.r.pill,
              background: active ? t.accent : t.bgSubtle,
              color: active ? t.accentInk : t.text, border: 'none', cursor: 'pointer',
              fontFamily: M.fontMono, fontSize: 13, fontWeight: 600,
            }}>{p}</button>
          );
        })}
      </div>
      <button style={{
        background: 'transparent', border: 'none', color: t.accent, cursor: 'pointer',
        fontFamily: M.fontSans, fontSize: 13, fontWeight: 600, padding: '6px 2px',
      }}>Вернуть стандарт по календарю — 167:00</button>

      <div style={{
        marginTop: 12, padding: '12px 14px', borderRadius: M.r.input,
        background: t.bgSubtle, display: 'flex', gap: 10, alignItems: 'flex-start',
      }}>
        <IcInfo width="18" height="18" style={{ color: t.textMuted, flexShrink: 0, marginTop: 1 }}/>
        <div style={{ ...M.t.hint(t) }}>
          Чтобы уменьшить норму на отпуск, болезнь или курсы — добавьте отвлечение в Календаре.
          <span style={{ color: t.accent, fontWeight: 600 }}> Добавить отвлечение →</span>
        </div>
      </div>

      <button style={{ ...payCta(t), marginTop: 16 }}>Сохранить</button>
    </BottomSheet>
  );
}

// ─── Ночные часы (из «Ночь») ────────────────────────────────────
function NightRangeSheet({ t }) {
  const box = (val) => (
    <div style={{
      flex: 1, padding: '14px 0', textAlign: 'center', borderRadius: M.r.input,
      background: t.bgSubtle,
      fontFamily: M.fontMono, fontSize: 24, fontWeight: 700, color: t.text,
      fontVariantNumeric: 'tabular-nums',
    }}>{val}</div>
  );
  return (
    <BottomSheet t={t}>
      <SheetHeader t={t} title="Ночные часы" action={{ label: 'Готово' }}
        hint="Интервал, который считается ночным временем"/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '4px 0 6px' }}>
        <div style={{ flex: 1 }}>
          <div style={{ ...M.t.label(t), marginBottom: 6, textAlign: 'center' }}>Начало</div>
          {box('22:00')}
        </div>
        <div style={{ marginTop: 22, color: t.textFaint, fontFamily: M.fontMono, fontSize: 18 }}>→</div>
        <div style={{ flex: 1 }}>
          <div style={{ ...M.t.label(t), marginBottom: 6, textAlign: 'center' }}>Конец</div>
          {box('06:00')}
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        {['22:00 – 06:00', '23:00 – 06:00', '00:00 – 05:00'].map((p, i) => (
          <button key={p} style={{
            flex: 1, padding: '9px 0', borderRadius: M.r.pill,
            background: i === 0 ? t.accent : t.bgSubtle,
            color: i === 0 ? t.accentInk : t.text, border: 'none', cursor: 'pointer',
            fontFamily: M.fontMono, fontSize: 12, fontWeight: 600,
          }}>{p}</button>
        ))}
      </div>
      <button style={{ ...payCta(t), marginTop: 18 }}>Сохранить</button>
    </BottomSheet>
  );
}

// ─── Новое плечо (из «Добавить плечо») ──────────────────────────
function AddShoulderSheet({ t }) {
  const labeled = (label, node) => (
    <div style={{ marginBottom: 12 }}>
      <div style={{ ...M.t.label(t), marginBottom: 7 }}>{label}</div>
      {node}
    </div>
  );
  const stationField = (value) => (
    <button style={{
      width: '100%', textAlign: 'left', display: 'flex', alignItems: 'center', gap: 10,
      minHeight: 50, padding: '0 14px', borderRadius: M.r.input,
      background: 'transparent', border: `1px solid ${t.border}`, cursor: 'pointer',
    }}>
      <IcMapPin width="17" height="17" style={{ color: t.accent, flexShrink: 0 }}/>
      <span style={{ flex: 1, fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text }}>{value}</span>
      <IcChevronDown style={{ color: t.textMuted }}/>
    </button>
  );
  return (
    <BottomSheet t={t}>
      <SheetHeader t={t} title="Новое плечо" action={{ label: 'Отмена' }}/>
      {labeled('Откуда', stationField('Лужская'))}
      {labeled('Куда', stationField('Луга'))}
      {labeled('Расстояние',
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          minHeight: 50, padding: '0 14px', borderRadius: M.r.input,
          border: `1px solid ${t.border}`,
        }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 18, fontWeight: 600, color: t.text }}>250</span>
          <span style={{ fontFamily: M.fontSans, fontSize: 14, fontWeight: 500, color: t.textMuted }}>км</span>
        </div>
      )}
      <button style={{ ...payCta(t), marginTop: 10 }}>Добавить плечо</button>
    </BottomSheet>
  );
}


Object.assign(window, {
  IOSScreenSettingsNorms,
  IOSScreenSettingsAccounting,
  IOSScreenSettingsShoulders,
  IOSScreenSettingsLocomotive,
  SelectField, ShoulderRow, IcChevronDown,
  OptionPickerSheet, NormEditSheet, NightRangeSheet, AddShoulderSheet,
});
