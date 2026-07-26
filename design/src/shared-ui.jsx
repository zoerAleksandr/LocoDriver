// shared-ui.jsx — переиспользуемые примитивы и блоки для всех экранов
//
// ╔══════════════════════════════════════════════════════════════════╗
// ║                       ОБЩИЕ ПРИНЦИПЫ                              ║
// ╠══════════════════════════════════════════════════════════════════╣
// ║ 1. Все компоненты принимают `t` (тема) — НЕ `dark`. Тему           ║
// ║    выбираем один раз на уровне экрана: `const t = dark ? M.dark   ║
// ║    : M.light;` и пробрасываем дальше.                              ║
// ║                                                                    ║
// ║ 2. Платформа задаётся пропом `mat` (Material). Если `mat` —        ║
// ║    компонент рисуется в Android-стиле (углы 16, нет shadow         ║
// ║    в шторках, чуть другой weight). Это позволяет ОДИН компонент    ║
// ║    использовать в обеих платформах.                                ║
// ║                                                                    ║
// ║ 3. Все цвета — через t (никогда #hex напрямую в компонентах).      ║
// ║                                                                    ║
// ║ 4. Все шрифты — через M.fontSans / M.fontMono.                     ║
// ║                                                                    ║
// ║ 5. Все размеры строк/паддингов — через M.s.* и M.semantic.* (если  ║
// ║    нужно отойти от стандарта — пишем, почему).                     ║
// ║                                                                    ║
// ║ 6. Все text-стили — через M.t.*(t) (возвращает spread-ready объект)║
// ║    Пример: <div style={{ ...M.t.label(t), marginBottom: 6 }}>      ║
// ╚══════════════════════════════════════════════════════════════════╝


// ════════════════════════════════════════════════════════════════════
// 1. ХЕЛПЕРЫ
// ════════════════════════════════════════════════════════════════════

// hexToRgba — прозрачность для #RRGGBB цветов.
// Используем когда нужен tint (например, светлый фон под success/danger badge).
function hexToRgba(hex, a = 1) {
  if (!hex) return `rgba(0,0,0,${a})`;
  if (hex.startsWith('rgb')) return hex;
  let h = hex.replace('#', '');
  if (h.length === 3) h = h.split('').map(c => c + c).join('');
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  return `rgba(${r},${g},${b},${a})`;
}


// ════════════════════════════════════════════════════════════════════
// 2. КАРТОЧКА (Card)
// ════════════════════════════════════════════════════════════════════
//
// Главный контейнер контента в приложении. iOS и Android отличаются
// только радиусом — оба варианта внутри одного компонента.
//
// КОГДА:
//   • любая группа полей формы (Основное, Время, Локомотивы)
//   • любой список одного типа (поездки, станции, серии)
//   • любая обособленная панель (Hero hours, TotalsBlock)
//
// КОГДА НЕ:
//   • если это hero-блок без визуальных границ — лучше прямой `t.surface`
//     с собственным padding и radius (см. Hero hours в Trips)
//
// Содержимое НЕ обязательно должно иметь внутренний padding —
// часто это контейнер для строк, у каждой свой padding.
//
function Card({ t, children, mat, style }) {
  return (
    <div style={{
      background: t.surface,
      borderRadius: mat ? M.r.cardAndroid : M.r.cardIOS,
      boxShadow: M.shadow.sm,
      overflow: 'hidden',
      ...style,
    }}>
      {children}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 3. РАЗДЕЛИТЕЛЬ В КАРТОЧКЕ (Sep)
// ════════════════════════════════════════════════════════════════════
//
// Тонкая горизонтальная линия между строками внутри Card.
//
// inset — отступ слева:
//   • 0   — линия от края до края (Android-стиль)
//   • 20  — линия от внутреннего края текста (iOS, нет иконки)
//   • 56  — линия после колонки иконки (iOS со строками-аватарами)
//
// Правило: inset должен совпадать с горизонтальным padding-ом строки
// или со стартом основного контента после иконки.
//
function Sep({ t, inset = 0 }) {
  return <div style={{ height: 1, background: t.border, marginLeft: inset }}/>;
}


// ════════════════════════════════════════════════════════════════════
// 4. ЗАГОЛОВОК ГРУППЫ (GroupHead)
// ════════════════════════════════════════════════════════════════════
//
// UPPERCASE-MONO заголовок над карточкой (или группой карточек).
// Был три раза переоткрыт в коде — GroupHead, GroupLabel, AGroup.
// Этот — единственный, остальные нужно убрать.
//
// КОГДА:
//   • перед каждой Card / группой Card на экране редактирования
//   • в шторках перед группой полей
//
// `mt` — отступ сверху. По умолчанию 22 (между группами). Для самого
// первого заголовка после хедера — передавать mt={4}.
//
// `trailing` — что-то справа (TractionPill, Stepper, ссылка «Все →»).
//
function GroupHead({ t, children, trailing, mt = M.semantic.groupHeadMt, style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: M.semantic.groupHeadPadY,
      marginTop: mt,
      ...style,
    }}>
      <div style={M.t.group(t)}>{children}</div>
      {trailing}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 5. КРУПНЫЙ ЗАГОЛОВОК СЕКЦИИ (SectionH2)
// ════════════════════════════════════════════════════════════════════
//
// Sans 20/700 — заголовок более крупных секций на главных экранах
// ("Текущий маршрут", "Маршруты", "Инструменты" в iOS Trips).
// Отличается от GroupHead: тот — мелкий MONO для форм-ориентированных
// экранов, этот — крупный SANS для контентных.
//
// КОГДА:
//   • главный экран Поездки (и аналоги) — секции контента
//   • дашборды
//
// КОГДА НЕ:
//   • экраны редактирования (Маршрут, Локомотив, Серия) — там GroupHead
//
function SectionH2({ t, children, trailing }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '22px 4px 12px',
    }}>
      <div style={M.t.sectionH2(t)}>{children}</div>
      {trailing}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 6. ПОДСКАЗКА (Hint)
// ════════════════════════════════════════════════════════════════════
//
// Серый блок с пояснением. Используется в верхней части экрана-редактора
// чтобы объяснить что вообще делается на этом экране (норма серии,
// норма станции и т.п.). НЕ для inline-валидации.
//
function Hint({ t, children, style }) {
  return (
    <div style={{
      marginTop: M.s.md,
      padding: '12px 14px',
      borderRadius: M.r.input,
      background: t.bgSubtle,
      ...M.t.hint(t),
      ...style,
    }}>
      {children}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 7. КРУГ С ИКОНКОЙ В НАЧАЛЕ СТРОКИ (IconAvatar)
// ════════════════════════════════════════════════════════════════════
//
// Этот элемент копировался руками минимум в 8 местах: SheetTimeRow,
// DurationRow, AddRow, PassRow, StationRow, ContextField, sections в
// CoeffSheet, station-picker rows. Везде одна и та же геометрия,
// только цвета и размер чуть разные.
//
// `size`: 'sm' (28) | 'md' (32) | 'lg' (36)
// `tone`:
//   • 'neutral'  — серый фон, нейтральный текст (DurationRow, StationRow)
//   • 'accent'   — accentSoft фон + accent цвет (AddRow, ContextField)
//   • 'inverse'  — accent фон + accentInk (выделенная активная строка)
//   • 'warn'     — желто-красный (для empty-state)
//
// `shape`: 'circle' (по умолчанию) | 'square' (M3-стиль AddRow в Android)
//
function IconAvatar({ icon, size = 'md', tone = 'neutral', shape = 'circle', t }) {
  const sizes = { sm: 28, md: 32, lg: 36 };
  const iconSizes = { sm: 14, md: 16, lg: 18 };
  const dim = sizes[size];
  const iconDim = iconSizes[size];

  const tones = {
    neutral: { bg: t.bgSubtle, color: t.text },
    accent:  { bg: t.accentSoft, color: t.accent },
    inverse: { bg: t.accent, color: t.accentInk },
    warn:    { bg: hexToRgba(t.warning, 0.15), color: t.warning },
  };
  const c = tones[tone];

  // Радиус: круг при shape='circle' и квадрат с округлением по размеру
  // если shape='square'. Для md-square это даёт ~10px, что совпадает
  // со старым ручным значением.
  const radius = shape === 'circle' ? dim / 2 : Math.round(dim * 0.3);

  return (
    <div style={{
      width: dim, height: dim, borderRadius: radius,
      background: c.bg, color: c.color,
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      flexShrink: 0,
    }}>
      {React.cloneElement(icon, { width: iconDim, height: iconDim })}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 8. СТРОКА КАРТОЧКИ — БАЗОВАЯ (Row)
// ════════════════════════════════════════════════════════════════════
//
// «Безопасная» оболочка для строки в карточке. Сама ничего не рисует
// кроме отступов — даёт consistent padding и flex-раскладку.
//
// `compact` — уменьшенный padY (для строк-полей формы)
// `clickable` — добавляет cursor:pointer и убирает дефолтные стили button
//
// Если строка — ссылка (с шевроном справа), используй RowLink ниже.
//
function Row({ t, children, compact, clickable, onClick, style }) {
  const padY = compact ? M.semantic.rowPadY : M.semantic.rowPadYTouch;
  const Tag = clickable ? 'button' : 'div';
  return (
    <Tag
      onClick={onClick}
      style={{
        width: '100%',
        padding: `${padY}px ${M.semantic.rowPadX}px`,
        display: 'flex', alignItems: 'center', gap: M.semantic.rowGap,
        background: 'transparent', border: 'none',
        textAlign: 'left',
        cursor: clickable ? 'pointer' : 'default',
        ...style,
      }}
    >
      {children}
    </Tag>
  );
}


// ════════════════════════════════════════════════════════════════════
// 9. СТРОКА «ПОЛЕ ФОРМЫ» (FieldRow)
// ════════════════════════════════════════════════════════════════════
//
// Универсальная строка вида: "label .... value [chevron]"
// Заменяет: FieldRow (norms) + ATimeRow (android) + FieldCell (loco)
//
// Варианты использования:
//   <FieldRow t={t} label="Серия" value="ВЛ80с" />
//   <FieldRow t={t} label="Тип" value="Электровоз" chevron />     // → редактор
//   <FieldRow t={t} label="Номер" value="1234" mono />            // mono значение
//   <FieldRow t={t} label="Начало" value="24 апр · 20:00" mono /> // дата+время
//
// Если нужно value НАД label (не справа, а двумя строками — как в
// FieldCell внутри grid 1fr 1fr) — используй FieldStack ниже.
//
function FieldRow({ t, label, value, mono, chevron, onClick }) {
  const clickable = !!onClick;
  return (
    <Row t={t} compact clickable={clickable} onClick={onClick}>
      <span style={M.t.labelMuted(t)}>{label}</span>
      <div style={{
        marginLeft: 'auto',
        display: 'inline-flex', alignItems: 'center', gap: 6,
      }}>
        <span style={mono ? M.t.valueMono(t) : M.t.value(t)}>{value}</span>
        {chevron && <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>}
      </div>
    </Row>
  );
}


// ════════════════════════════════════════════════════════════════════
// 10. СТРОКА «ЯЧЕЙКА» — label СВЕРХУ, value СНИЗУ (FieldStack)
// ════════════════════════════════════════════════════════════════════
//
// Используется когда нужно две (или больше) ячеек в ряд с лейблом
// сверху и значением снизу. Раньше было FieldCell + AFieldCell —
// один компонент покрывает оба случая.
//
// Пример (Локомотив, Серия и Номер рядом):
//   <Card t={t}>
//     <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
//       <FieldStack t={t} label="Серия" value="ВЛ80с" mono />
//       <div style={{ borderLeft: `1px solid ${t.border}` }}>
//         <FieldStack t={t} label="Номер" value="1234" mono />
//       </div>
//     </div>
//   </Card>
//
function FieldStack({ t, label, value, mono }) {
  return (
    <div style={{
      padding: `${M.semantic.rowPadY}px ${M.semantic.rowPadX - 2}px`,
    }}>
      <div style={{ ...M.t.label(t), marginBottom: 6 }}>{label}</div>
      <div style={mono ? M.t.valueMono(t) : M.t.value(t)}>{value}</div>
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 11. СТРОКА «ДЕЙСТВИЕ С ИКОНКОЙ» (ActionRow)
// ════════════════════════════════════════════════════════════════════
//
// Покрывает: AddRow (iOS), AAddRow (Android), PassRow (с подзаголовком).
// Иконка-аватар + label + опциональный suffix (время, шеврон) справа.
//
// Варианты:
//   <ActionRow t={t} icon={<IcLocomotive/>} label="Добавить локомотив" />
//   <ActionRow t={t} icon={<IcLocomotive/>} label="Добавить локомотив" chevron />
//   <ActionRow t={t} icon={<IcPassengers/>}
//              label="Поезд 8023" sub="Новосибирск — Омск"
//              suffix="04:20" chevron tone="neutral" />
//
// `tone` иконки/текста:
//   • 'accent'  (по умолчанию) — для «Добавить ...» строк
//   • 'neutral' — для строк-данных (PassRow, существующая запись)
//
function ActionRow({ t, icon, label, sub, suffix, chevron, tone = 'accent', onClick }) {
  const labelColor = tone === 'accent' ? t.accent : t.text;
  return (
    <Row t={t} compact clickable={!!onClick} onClick={onClick}>
      <IconAvatar
        t={t}
        icon={icon}
        size="lg"
        shape="square"
        tone={tone === 'accent' ? 'accent' : 'neutral'}
      />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: labelColor,
        }}>
          {label}
        </div>
        {sub && (
          <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{sub}</div>
        )}
      </div>
      {suffix && (
        <span style={{ ...M.t.timeMono(t), fontSize: 14 }}>{suffix}</span>
      )}
      {chevron && (
        <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
      )}
    </Row>
  );
}


// ════════════════════════════════════════════════════════════════════
// 12. БОЛЬШАЯ КНОПКА «ДОБАВИТЬ» ВНИЗУ СПИСКА (AddBtn)
// ════════════════════════════════════════════════════════════════════
//
// Прямоугольная full-width кнопка с иконкой "+" внизу списка
// (например — внизу списка серий: "Добавить серию").
//
// Отличается от ActionRow тем, что не внутри карточки, а сама себе
// pill-блок на bgSubtle.
//
function AddBtn({ t, icon, label, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        width: '100%', marginTop: M.s.lg, padding: '14px',
        borderRadius: M.r.input, background: t.bgSubtle, color: t.accent,
        border: 'none', fontSize: 14, fontWeight: 500, cursor: 'pointer',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        fontFamily: M.fontSans,
      }}
    >
      {React.cloneElement(icon, { width: 16, height: 16 })}
      {label}
    </button>
  );
}


// ════════════════════════════════════════════════════════════════════
// 13. КНОПКА «УДАЛИТЬ» ВНИЗУ ЭКРАНА-РЕДАКТОРА (DangerBtn)
// ════════════════════════════════════════════════════════════════════
//
// Красная outline-кнопка для деструктивного действия в самом низу
// экрана-редактора (Удалить серию, Удалить станцию). Вне Card.
//
function DangerBtn({ t, icon, label, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        width: '100%', marginTop: M.s.xxxl, padding: '14px',
        borderRadius: M.r.input, background: 'transparent', color: t.danger,
        border: `1px solid ${t.border}`, fontSize: 14, fontWeight: 500, cursor: 'pointer',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        fontFamily: M.fontSans,
      }}
    >
      {React.cloneElement(icon, { width: 16, height: 16 })}
      {label}
    </button>
  );
}


// ════════════════════════════════════════════════════════════════════
// 14. ТЕКСТОВАЯ ССЫЛКА (TextAction)
// ════════════════════════════════════════════════════════════════════
//
// Синий текстовый «link button» — типа «+ Рекуперация», «+ Экипировка»,
// «Все →», «Настроить разделы →». Без фона, без рамки, только цвет accent.
//
// `prefix` / `suffix` — необязательные символы по краям.
// Если prefix не передан, по умолчанию ничего не подставляется
// (раньше был жёстко зашит "+", это убрано).
//
function TextAction({ t, label, prefix, suffix, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 4,
        padding: '2px 0',
        background: 'transparent', border: 'none',
        color: t.accent, fontSize: 13, fontWeight: 500,
        cursor: 'pointer', fontFamily: M.fontSans,
      }}
    >
      {prefix && <span>{prefix}</span>}
      {label}
      {suffix && <span>{suffix}</span>}
    </button>
  );
}


// ════════════════════════════════════════════════════════════════════
// 15. КРУГЛАЯ КНОПКА В НАВБАРЕ iOS (NavPillBtn)
// ════════════════════════════════════════════════════════════════════
//
// Pill в iOS-навбаре. Два варианта: иконочная (40×40) и текстовая
// «Сохранить» (auto-width с padX=14, всегда accent-цвет).
//
//   <NavPillBtn t={t} icon={<IcChevronLeft/>} />          // ← back
//   <NavPillBtn t={t} accent>Сохранить</NavPillBtn>       // → save
//
function NavPillBtn({ t, children, icon, accent, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        height: M.semantic.navBtnSize,
        width: icon ? M.semantic.navBtnSize : undefined,
        padding: icon ? 0 : `0 ${M.semantic.navBtnPadX}px`,
        borderRadius: M.r.pill,
        background: t.surface, border: 'none', boxShadow: M.shadow.sm,
        color: accent ? t.accent : t.text, fontSize: 15, fontWeight: 600,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontFamily: M.fontSans, cursor: 'pointer',
      }}
    >
      {icon ? React.cloneElement(icon, { width: 18, height: 18 }) : children}
    </button>
  );
}


// ════════════════════════════════════════════════════════════════════
// 16. СКРИМ НИЗА ЭКРАНА (ScrimBottom)
// ════════════════════════════════════════════════════════════════════
//
// Градиент сверху-вниз от прозрачного к bg — чтобы скроллящийся
// контент мягко уходил под плавающую панель внизу (FAB, тоолбар, таб-бар).
//
// `height` подбирай так, чтобы перекрывать высоту нижней панели + воздух.
//
function ScrimBottom({ t, height = 140, zIndex = 40 }) {
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      height, zIndex, pointerEvents: 'none',
      background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.85)} 45%, ${t.bg} 100%)`,
    }}/>
  );
}


// ════════════════════════════════════════════════════════════════════
// 17. КАРКАС ШТОРКИ (BottomSheet)
// ════════════════════════════════════════════════════════════════════
//
// Общий каркас для всех bottom-sheets: затемнённый фон + поднятая
// панель с грэббером. Используется для CoeffSheet, StationPicker,
// шторки времени.
//
// Варианты по платформе:
//   • mat=false (iOS):    radius 18, padding 12/20/32
//   • mat=true (Android): radius 28, padding 12/20/32
//
// `height` — '85%' для полноэкранных пикеров, или auto (default)
// для compact-шторок (CoeffSheet с пресетами).
//
// `zIndex`: backdrop=70, sheet=71 — чтобы перекрывало toolbar (zIndex 50).
//
function BottomSheet({ t, mat, children, height, onBackdrop }) {
  return (
    <React.Fragment>
      {/* Backdrop */}
      <div
        onClick={onBackdrop}
        style={{
          position: 'absolute', inset: 0, zIndex: 70,
          background: 'rgba(0,0,0,0.45)',
        }}
      />
      {/* Sheet */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 71,
        background: t.surface || t.bg,
        borderTopLeftRadius:  mat ? M.r.sheetAndroid : M.r.sheetIOS,
        borderTopRightRadius: mat ? M.r.sheetAndroid : M.r.sheetIOS,
        padding: '12px 20px 32px',
        boxShadow: M.shadow.sheet,
        height,
        display: height ? 'flex' : undefined,
        flexDirection: height ? 'column' : undefined,
      }}>
        <SheetGrabber t={t}/>
        {children}
      </div>
    </React.Fragment>
  );
}

// Грэббер сверху шторки (короткая полоска).
// Вынесен отдельно на случай если шторка хочет грэббер вне padding-а
// (например, прижатый к верху, как в station-picker).
function SheetGrabber({ t }) {
  return (
    <div style={{
      width: 36, height: 4, borderRadius: 4,
      background: t.borderStrong || t.border,
      margin: '0 auto 14px',
    }}/>
  );
}

// Заголовок шторки: title слева + accent-кнопка-действие справа
// ("Готово", "Отмена" и т.п.) + опциональный hint снизу.
function SheetHeader({ t, title, action, hint }) {
  return (
    <React.Fragment>
      <div style={{
        display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        marginBottom: hint ? 4 : 12,
      }}>
        <span style={M.t.navTitle(t)}>{title}</span>
        {action && (
          <button style={{
            background: 'transparent', border: 'none', color: t.accent,
            fontSize: 15, fontWeight: 500, cursor: 'pointer', padding: 0,
            fontFamily: M.fontSans,
          }} onClick={action.onClick}>
            {action.label}
          </button>
        )}
      </div>
      {hint && (
        <div style={{ ...M.t.hint(t), marginBottom: 16 }}>{hint}</div>
      )}
    </React.Fragment>
  );
}


// ════════════════════════════════════════════════════════════════════
// 18. ЕДИНЫЙ ИНПУТ (FieldInput)
// ════════════════════════════════════════════════════════════════════
//
// Универсальный инпут с тонким контуром, прозрачным фоном, mono-шрифтом
// для значений. Раньше было SectionInput — дёргали в одной только
// шторке локомотива; здесь общая версия для всех.
//
// `size`: 'md' (по умолчанию) | 'sm' (компактный, 36 высота)
// `align`: 'left' | 'center' | 'right'
// `mono`: true → JetBrains Mono (подходит для цифр); false → Inter
//
function FieldInput({ t, value, placeholder, size = 'md', align = 'left', mono = true, onChange }) {
  const empty = !value;
  const minH = size === 'sm' ? 36 : M.semantic.inputMinH;
  const padY = size === 'sm' ? 8 : M.semantic.inputPadY;
  return (
    <div style={{
      background: 'transparent',
      border: `1px solid ${t.border}`,
      borderRadius: M.r.input,
      padding: `${padY}px ${M.semantic.inputPadX}px`,
      minHeight: minH, display: 'flex', alignItems: 'center',
      justifyContent: align === 'right' ? 'flex-end' : align === 'center' ? 'center' : 'flex-start',
    }}>
      <span style={{
        fontFamily: mono ? M.fontMono : M.fontSans,
        fontSize: 17, fontWeight: 600,
        color: empty ? t.textFaint : t.text,
        textAlign: align,
      }}>
        {empty ? placeholder : value}
      </span>
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 19. ЧИП-БЭДЖ (Pill)
// ════════════════════════════════════════════════════════════════════
//
// Маленький закруглённый бэдж — статус, счётчик, метка. Заменяет
// inline-стилизованные spans по всему коду.
//
// `tone`:
//   • 'accent'  — accentSoft + accent
//   • 'success' — success-tint + success
//   • 'warning' — warning-tint + warning
//   • 'muted'   — bgSubtle + textMuted
//   • 'inverse' — accent + accentInk
//
// `mono` — использовать ли JetBrains Mono (по умолчанию да — пиллы
//          обычно содержат время/числа/коды).
//
function Pill({ t, children, tone = 'muted', mono = true, style }) {
  const tones = {
    accent:  { bg: t.accentSoft, color: t.accent },
    success: { bg: hexToRgba(t.success, 0.12), color: t.success },
    warning: { bg: hexToRgba(t.warning, 0.14), color: t.warning },
    danger:  { bg: hexToRgba(t.danger,  0.12), color: t.danger },
    muted:   { bg: t.bgSubtle, color: t.textMuted },
    inverse: { bg: t.accent, color: t.accentInk },
  };
  const c = tones[tone];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      padding: '4px 10px', borderRadius: M.r.pill,
      background: c.bg, color: c.color,
      fontSize: 12, fontWeight: 600, letterSpacing: 0.2,
      fontFamily: mono ? M.fontMono : M.fontSans,
      ...style,
    }}>
      {children}
    </span>
  );
}


// ════════════════════════════════════════════════════════════════════
// 20. КНОПКА «ИКОНКА В КРУГЕ» (IconBtn)
// ════════════════════════════════════════════════════════════════════
//
// Изолированная круглая кнопка с одной иконкой (на toolbar, в шторках,
// как кнопка − / + в степпере). Унифицирует размер, фон и тач-зону.
//
// `variant`:
//   • 'ghost'    — прозрачный фон, обычный текст (например, в Top App Bar)
//   • 'surface'  — surface фон + тонкая рамка (CoeffSheet кнопки −/+)
//   • 'subtle'   — bgSubtle фон без рамки (DurationRow степпер)
//   • 'floating' — surface + shadow (iOS toolbar)
//   • 'cta'      — t.cta фон (главная плавающая кнопка)
//   • 'accent'   — t.accent фон (FAB)
//
// `size`: число (по умолчанию 40)
//
function IconBtn({ t, icon, variant = 'ghost', size = 40, onClick, style, ariaLabel }) {
  const variants = {
    ghost:    { bg: 'transparent', color: t.text, border: 'none', shadow: 'none' },
    surface:  { bg: t.surface, color: t.text, border: `1px solid ${t.border}`, shadow: 'none' },
    subtle:   { bg: t.bgSubtle, color: t.text, border: 'none', shadow: 'none' },
    floating: { bg: t.surface, color: t.text, border: `1px solid ${t.border}`, shadow: M.shadow.floating },
    cta:      { bg: t.cta || t.accent, color: t.ctaInk || t.accentInk, border: 'none', shadow: '0 12px 32px rgba(0,0,0,0.28)' },
    accent:   { bg: t.accent, color: t.accentInk, border: 'none', shadow: M.shadow.fab },
    danger:   { bg: t.surface, color: t.danger, border: `1px solid ${t.border}`, shadow: M.shadow.floating },
  };
  const v = variants[variant];
  return (
    <button
      aria-label={ariaLabel}
      onClick={onClick}
      style={{
        width: size, height: size, borderRadius: size / 2,
        background: v.bg, color: v.color, border: v.border,
        boxShadow: v.shadow,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: 'pointer',
        ...style,
      }}
    >
      {React.cloneElement(icon, { width: Math.round(size * 0.5), height: Math.round(size * 0.5) })}
    </button>
  );
}


// ════════════════════════════════════════════════════════════════════
// 21. iOS НАВБАР (NavBarIOS)
// ════════════════════════════════════════════════════════════════════
//
// Два варианта iOS-навбара уже было: один с pill-кнопками (Маршрут,
// Локомотив), другой с текстовыми (Серии — back с label «Настройки»).
// Здесь оба под одной крышей.
//
// `style`:
//   • 'pill'  — иконка в кружке слева + accent текст-кнопка справа
//   • 'text'  — back с текстом + центрированный title + текстовая
//               accent-кнопка (для экранов справочников)
//
// При style='pill' и `back` — слева IconBtn с шевроном; иначе ничего.
// При `trailing` — справа либо текст ("Сохранить"), либо JSX (любой блок).
//
function NavBarIOS({ t, title, back, trailing, trailingAccent, style = 'pill', backLabel }) {
  if (style === 'text') {
    // Текстовый навбар — для справочников (Серии / Станции / Редакторы).
    // Заголовок абсолютно отцентрован относительно бара,
    // зарезервированы 88px на каждом крае под кнопки.
    const SIDE = 88;
    return (
      <div style={{
        position: 'sticky', top: 0, zIndex: 10,
        borderBottom: `1px solid ${t.border}`, background: t.bg,
        height: 44, display: 'flex', alignItems: 'center',
      }}>
        <div style={{ flex: '0 0 auto', display: 'flex', alignItems: 'center', paddingLeft: 8, minWidth: 0, maxWidth: '40%' }}>
          {back && (
            <button style={{
              border: 'none', background: 'transparent', color: t.accent,
              display: 'inline-flex', alignItems: 'center', gap: 2,
              fontSize: 16, padding: '4px 6px', cursor: 'pointer',
              minWidth: 0, maxWidth: '100%',
            }}>
              <IcChevronLeft width="18" height="18" style={{ flexShrink: 0 }}/>
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {backLabel}
              </span>
            </button>
          )}
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingRight: 12 }}>
          {trailing && (typeof trailing === 'string' ? (
            <button style={{
              ...M.t.navAction(t, trailingAccent),
              border: 'none', background: 'transparent',
              padding: '4px 4px', cursor: 'pointer', whiteSpace: 'nowrap',
            }}>{trailing}</button>
          ) : trailing)}
        </div>
        <div style={{
          position: 'absolute', left: SIDE, right: SIDE, top: 0, bottom: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          pointerEvents: 'none',
        }}>
          <div style={{
            ...M.t.navTitle(t), fontSize: 17, fontWeight: 600,
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            maxWidth: '100%',
          }}>{title}</div>
        </div>
      </div>
    );
  }

  // Pill-стиль — для основных экранов-форм (Маршрут, Локомотив).
  return (
    <div style={{ position: 'relative', paddingTop: 58 }}>
      <div style={{
        padding: '12px 16px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        {back ? <NavPillBtn t={t} icon={<IcChevronLeft/>}/> : <div style={{ width: 40 }}/>}
        <div style={M.t.navTitle(t)}>{title}</div>
        {trailing
          ? (typeof trailing === 'string'
              ? <NavPillBtn t={t} accent={trailingAccent}>{trailing}</NavPillBtn>
              : trailing)
          : <div style={{ width: 40 }}/>
        }
      </div>
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 22. ANDROID TOP APP BAR (TopBarAndroid)
// ════════════════════════════════════════════════════════════════════
//
// Material 3 small top app bar: back-кнопка + title + опциональное
// действие справа (3-точечное меню или иконка).
//
function TopBarAndroid({ t, title, back, trailing }) {
  return (
    <div style={{
      padding: '8px 4px 8px 4px', display: 'flex', alignItems: 'center', gap: 4,
      background: t.bg,
    }}>
      {back && (
        <button style={{
          width: 48, height: 48, border: 'none', background: 'transparent', color: t.text,
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
        }}>
          <IcChevronLeft width="22" height="22"/>
        </button>
      )}
      <div style={{
        flex: 1, fontSize: 20, fontWeight: 500, color: t.text,
        fontFamily: M.fontSans,
      }}>{title}</div>
      {trailing}
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 23. СТЕППЕР −/значение/+ (Stepper)
// ════════════════════════════════════════════════════════════════════
//
// Числовой степпер для количеств (количество секций локомотива).
// `mat=true` — Material-стиль (outline кнопки), иначе iOS (filled).
//
function Stepper({ t, value = 1, mat, onMinus, onPlus }) {
  const btnBase = {
    width: 32, height: 32, borderRadius: 16,
    background: mat ? t.surface : t.bgSubtle,
    border: mat ? `1px solid ${t.border}` : 'none',
    color: t.text, cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  };
  const accentBtn = {
    ...btnBase,
    background: mat ? t.surface : t.accent,
    color: mat ? t.accent : t.accentInk,
  };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <button style={btnBase} onClick={onMinus} aria-label="Минус">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth="2.4" strokeLinecap="round">
          <path d="M5 12h14"/>
        </svg>
      </button>
      <span style={{
        fontFamily: M.fontMono, fontSize: 17, fontWeight: 700, color: t.text,
        minWidth: 16, textAlign: 'center',
      }}>{value}</span>
      <button style={accentBtn} onClick={onPlus} aria-label="Плюс">
        <IcPlus width="14" height="14"/>
      </button>
    </div>
  );
}


// ════════════════════════════════════════════════════════════════════
// 24. СТРОКА ПРОДОЛЖИТЕЛЬНОСТИ С КОМПАКТНЫМ СТЕППЕРОМ (DurationRow)
// ════════════════════════════════════════════════════════════════════
//
// Иконка + label + −[ N мин ]+ . Используется в редакторах норм
// серии и станции. Размер кнопок и инпута меньше чем в Stepper.
//
function DurationRow({ t, icon, label, minutes, onMinus, onPlus }) {
  const btn = {
    width: 28, height: 28, borderRadius: 14, background: t.bgSubtle, color: t.text,
    border: 'none', cursor: 'pointer',
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 16, fontWeight: 600, fontFamily: M.fontSans,
  };
  return (
    <Row t={t}>
      <IconAvatar t={t} icon={icon} size="md" tone="neutral"/>
      <div style={{ flex: 1, ...M.t.body(t), fontSize: 14 }}>{label}</div>
      <div style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
        <button style={btn} onClick={onMinus}>−</button>
        <div style={{
          minWidth: 78, padding: '6px 10px',
          borderRadius: M.r.md, background: t.bg,
          border: `1px solid ${t.border}`,
          textAlign: 'center', fontFamily: M.fontMono, fontSize: 15, fontWeight: 600, color: t.text,
        }}>{minutes} мин</div>
        <button style={btn} onClick={onPlus}>+</button>
      </div>
    </Row>
  );
}


// ════════════════════════════════════════════════════════════════════
// 25. БЛОК «ЗНАЧЕНИЕ + ЕДИНИЦА» (ValueWithUnit)
// ════════════════════════════════════════════════════════════════════
//
// Часто повторяющийся inline-паттерн: большое моно-число + мелкая
// серая единица сбоку. Используется в Summary/Total/Breakdown строках.
//
//   <ValueWithUnit value="1 560" unit="кВт·ч" t={t} size="md" tone="text" />
//
// `size`: 'sm' (12), 'md' (15), 'lg' (20), 'xl' (32)
// `tone`: 'text' | 'muted' | 'success' | 'danger' | 'accent'
//
function ValueWithUnit({ t, value, unit, size = 'md', tone = 'text' }) {
  const sizes = { sm: 12, md: 15, lg: 20, xl: 32 };
  const fs = sizes[size];
  const unitFs = Math.max(11, fs - 5);
  const colors = {
    text: t.text, muted: t.textMuted, success: t.success,
    danger: t.danger, accent: t.accent,
  };
  return (
    <span>
      <span style={{
        fontFamily: M.fontMono, fontSize: fs, fontWeight: 700,
        color: colors[tone], lineHeight: 1,
      }}>{value}</span>
      {unit && (
        <span style={{
          fontFamily: M.fontMono, fontSize: unitFs, color: t.textMuted,
          marginLeft: 4, fontWeight: 500,
        }}>{unit}</span>
      )}
    </span>
  );
}


// ════════════════════════════════════════════════════════════════════
// 26. EXPORTS
// ════════════════════════════════════════════════════════════════════
Object.assign(window, {
  hexToRgba,
  Card, Sep, GroupHead, SectionH2, Hint,
  IconAvatar, Row, FieldRow, FieldStack, ActionRow,
  AddBtn, DangerBtn, TextAction,
  NavPillBtn, ScrimBottom,
  BottomSheet, SheetGrabber, SheetHeader,
  FieldInput, Pill, IconBtn,
  NavBarIOS, TopBarAndroid,
  Stepper, DurationRow, ValueWithUnit,
});
