// Экран Настройки — открывается из таб-бара
//
// Структура:
//   1. Хедер-карточка «Профиль» (имя + депо, как контекст)
//      — без неё экран выглядит как голый список ссылок
//   2. Справочники (Серии, Станции) — с превью-значениями справа
//   3. Приложение (Тема, Уведомления, Резервная копия) — со статусом справа
//   4. Прочее (Помощь, Оценить, Поделиться)
//   5. Карточка «О приложении» (версия, копирайт, ссылки)
//
// UI/UX отличия от исходных скринов:
//   • Группировка по смыслу (3 группы вместо одного списка) — снижает
//     ширину когнитивной нагрузки и упрощает поиск глазами.
//   • Превью-значения справа от строки (число серий/станций, активная
//     тема, состояние синка) — пользователь видит state не открывая.
//   • Карточка «О приложении» — даёт версии и ссылкам нормальное место
//     вместо мелкого футера.


// ─── Доп. иконки (нет в icons.jsx) ──────────────────────────────
const _icoBase = {
  width: 24, height: 24, viewBox: '0 0 24 24',
  fill: 'none', stroke: 'currentColor', strokeWidth: 1.3,
  strokeLinecap: 'round', strokeLinejoin: 'round',
};
function IcBell(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6z"/>
    <path d="M10 18a2 2 0 0 0 4 0"/>
  </svg>
); }
function IcMoon(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M20 13.5A8 8 0 1 1 10.5 4a6.5 6.5 0 0 0 9.5 9.5z"/>
  </svg>
); }
function IcInfo(p) { return (
  <svg {..._icoBase} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M12 11v6"/>
    <circle cx="12" cy="8" r=".9" fill="currentColor" stroke="none"/>
  </svg>
); }
function IcHelp(p) { return (
  <svg {..._icoBase} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M9.5 9.5a2.5 2.5 0 0 1 5 0c0 1.5-2.5 2-2.5 4"/>
    <circle cx="12" cy="17.5" r=".9" fill="currentColor" stroke="none"/>
  </svg>
); }
function IcStar(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M12 3l2.7 5.7 6.3.6-4.7 4.3 1.3 6.4L12 17l-5.6 3 1.3-6.4L3 9.3l6.3-.6z"/>
  </svg>
); }
function IcMail(p) { return (
  <svg {..._icoBase} {...p}>
    <rect x="3" y="5" width="18" height="14" rx="2"/>
    <path d="M3 7l9 7 9-7"/>
  </svg>
); }
function IcArchive(p) { return (
  <svg {..._icoBase} {...p}>
    <rect x="3" y="4" width="18" height="4" rx="1"/>
    <path d="M4 8h16v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/>
    <path d="M10 13h4"/>
  </svg>
); }
function IcGlobe(p) { return (
  <svg {..._icoBase} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>
  </svg>
); }


// ─── Общая строка-ссылка в карточке настроек ────────────────────
// Иконка-аватар + label (+ sub) → value (опц.) → шеврон
function SettingsRow({ t, icon, tone = 'neutral', label, sub, value, badge, last }) {
  return (
    <div style={{
      padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 14,
      cursor: 'pointer',
    }}>
      <IconAvatar t={t} icon={icon} size="md" tone={tone} shape="square"/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text,
        }}>{label}</div>
        {sub && (
          <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{sub}</div>
        )}
      </div>
      {value && (
        <span style={{
          fontFamily: M.fontSans, fontSize: 14, color: t.textMuted,
          maxWidth: 130, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{value}</span>
      )}
      {badge && (
        <Pill t={t} tone={badge.tone || 'success'} mono={false} style={{ padding: '3px 8px', fontSize: 11 }}>
          {badge.label}
        </Pill>
      )}
      <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>
    </div>
  );
}


// ─── Доп. иконки для разделов настроек ──────────────────────────
function IcWallet(p) { return (
  <svg {..._icoBase} {...p}>
    <rect x="3" y="6" width="18" height="13" rx="2.5"/>
    <path d="M3 10h18"/>
    <circle cx="17" cy="14" r="1.4" fill="currentColor" stroke="none"/>
  </svg>
); }
function IcScale(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M12 4v17M5 21h14"/>
    <path d="M5 9l-2 5a3 3 0 0 0 6 0L7 9zM17 9l-2 5a3 3 0 0 0 6 0L19 9z"/>
    <path d="M5 9l7-2 7 2"/>
  </svg>
); }
function IcMoonBed(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M3 18v-4a2 2 0 0 1 2-2h6a3 3 0 0 1 3 3v3"/>
    <path d="M14 14v4M3 18h18"/>
    <path d="M18 4a4 4 0 0 0 3 6 4 4 0 1 1-3-6z"/>
  </svg>
); }
function IcRoute(p) { return (
  <svg {..._icoBase} {...p}>
    <circle cx="6" cy="6" r="2.5"/>
    <circle cx="18" cy="18" r="2.5"/>
    <path d="M6 8.5v3a3 3 0 0 0 3 3h6a3 3 0 0 1 3 3v0"/>
  </svg>
); }
function IcSlidersV(p) { return (
  <svg {..._icoBase} {...p}>
    <path d="M7 4v6M7 14v6M12 4v10M12 18v2M17 4v2M17 10v10"/>
    <circle cx="7" cy="12" r="2"/>
    <circle cx="12" cy="16" r="2"/>
    <circle cx="17" cy="8" r="2"/>
  </svg>
); }


// ─── Switch (иконка, не функциональный) ─────────────────────────
function Switch({ t, on, mat }) {
  // iOS: pill 51×31, knob 27. Android M3: track 52×32, knob 24, gap.
  if (mat) {
    return (
      <div style={{
        width: 52, height: 32, borderRadius: 16,
        background: on ? t.accent : t.bgSubtle,
        border: `2px solid ${on ? t.accent : t.borderStrong}`,
        position: 'relative', flexShrink: 0,
        transition: 'background .15s',
      }}>
        <div style={{
          position: 'absolute',
          top: on ? 4 : 6, left: on ? 26 : 8,
          width: on ? 20 : 16, height: on ? 20 : 16, borderRadius: 12,
          background: on ? t.accentInk : t.textMuted,
          transition: 'all .15s',
        }}/>
      </div>
    );
  }
  return (
    <div style={{
      width: 51, height: 31, borderRadius: 16,
      background: on ? t.success : t.borderStrong,
      position: 'relative', flexShrink: 0,
      transition: 'background .15s',
    }}>
      <div style={{
        position: 'absolute', top: 2, left: on ? 22 : 2,
        width: 27, height: 27, borderRadius: 14,
        background: '#fff',
        boxShadow: '0 2px 4px rgba(0,0,0,0.18), 0 0 0 0.5px rgba(0,0,0,0.04)',
        transition: 'left .15s',
      }}/>
    </div>
  );
}


// ─── Карточка «О приложении» ───────────────────────────────────
// Только реальные данные исходного экрана: версия и почта поддержки.
function AboutCard({ t, mat }) {
  return (
    <React.Fragment>
      <GroupHead t={t}>О приложении</GroupHead>
      <div style={{
        background: t.surface,
        borderRadius: mat ? M.r.cardAndroid : M.r.cardIOS,
        boxShadow: M.shadow.sm,
        overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '16px 18px' }}>
          <IcLogo size={38} color={t.accentInk} bg={t.cta}/>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              fontFamily: M.fontDisplay, fontSize: 17, fontWeight: 700, color: t.text,
              letterSpacing: -0.2,
            }}>Машинист</div>
            <div style={{
              fontFamily: M.fontMono, fontSize: 12, color: t.textMuted, marginTop: 2,
            }}>версия 2.2.6.4</div>
          </div>
        </div>
        <Sep t={t} inset={mat ? 0 : 18}/>
        <SettingsRow t={t} icon={<IcMail/>}
          label="Поддержка" sub="locodriver.app@yandex.ru"/>
      </div>
    </React.Fragment>
  );
}


// ════════════════════════════════════════════════════════════════
// iOS — Настройки
// ════════════════════════════════════════════════════════════════
function IOSScreenSettings({ dark = false, height = 844 }) {
  const t = dark ? M.dark : M.light;

  return (
    <MDevice dark={dark} height={height}>
      {/* Large-title iOS-style header */}
      <div style={{ paddingTop: 58 }}>
        <div style={{
          padding: '4px 20px 12px',
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
        }}>
          <div style={{
            fontFamily: M.fontDisplay, fontSize: 34, fontWeight: 800,
            color: t.text, letterSpacing: -0.8,
          }}>Настройки</div>
        </div>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 110px - 100px)', padding: '4px 16px 32px' }}>
        <GroupHead t={t} mt={4}>Справочники норм</GroupHead>
        <Card t={t}>
          <SettingsRow t={t} icon={<IcLocomotive/>} tone="accent"
            label="Серии локомотивов" value="5 серий"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcMapPin/>} tone="accent"
            label="Станции" value="5 станций"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcRoute/>} tone="accent"
            label="Плечи" value="10 плеч"/>
        </Card>
        <SectionNote t={t}>
          Нормы времени и расстояния — подставляются автоматически при заполнении маршрута.
        </SectionNote>

        <GroupHead t={t}>Расчёт и учёт</GroupHead>
        <Card t={t}>
          <SettingsRow t={t} icon={<IcRuble/>}
            label="Зарплата" value="375 ₽/ч"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcScale/>}
            label="Норма / Регион" value="Россия"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcSlidersV/>}
            label="Учёт" sub="Ночные часы и будущие маршруты"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcMoonBed/>}
            label="Отдых" sub="Нормы домашнего отдыха и в ПО"/>
        </Card>

        <GroupHead t={t}>Интерфейс</GroupHead>
        <Card t={t}>
          <SettingsRow t={t} icon={<IcGear/>}
            label="Основные" sub="Значения по умолчанию для новых маршрутов"/>
          <Sep t={t} inset={56}/>
          <SettingsRow t={t} icon={<IcLocomotive/>}
            label="Локомотив" sub="Поля ввода показаний и вид тяги"/>
        </Card>

        <AboutCard t={t}/>
      </div>

      {/* Bottom scrim + faux tab bar so screen reads as bottom-nav target */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, right: 0, height: 140, zIndex: 40,
        pointerEvents: 'none',
        background: `linear-gradient(to bottom, ${hexToRgba(t.bg, 0)} 0%, ${hexToRgba(t.bg, 0.85)} 45%, ${t.bg} 100%)`,
      }}/>
      <FauxTabBarIOS t={t} active="settings"/>
    </MDevice>
  );
}


// ════════════════════════════════════════════════════════════════
// Android — Настройки
// ════════════════════════════════════════════════════════════════
function AndroidScreenSettings({ dark = false, height = 844 }) {
  const t = dark ? M.dark : M.light;

  return (
    <ADevice dark={dark} height={height}>
      {/* Material small top app bar */}
      <div style={{
        padding: '14px 20px 12px',
        display: 'flex', alignItems: 'center',
        background: t.bg,
      }}>
        <div style={{
          fontFamily: M.fontSans, fontSize: 22, fontWeight: 500, color: t.text,
        }}>Настройки</div>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 56px - 72px)', padding: '4px 16px 24px' }}>
        <GroupHead t={t} mt={4}>Справочники норм</GroupHead>
        <MCard t={t}>
          <SettingsRow t={t} icon={<IcLocomotive/>} tone="accent"
            label="Серии локомотивов" value="5 серий"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcMapPin/>} tone="accent"
            label="Станции" value="12 станций"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcRoute/>} tone="accent"
            label="Плечи" value="8 плеч"/>
        </MCard>

        <GroupHead t={t}>Расчёт</GroupHead>
        <MCard t={t}>
          <SettingsRow t={t} icon={<IcRuble/>}
            label="Зарплата" value="Сдельная"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcScale/>}
            label="Норма и регион" value="Горьковская жд"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcSlidersV/>}
            label="Учёт" sub="Как считается время и выплаты"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcMoonBed/>}
            label="Отдых" value="Домашний · ПО"/>
        </MCard>

        <GroupHead t={t}>Поездки</GroupHead>
        <MCard t={t}>
          <SettingsRow t={t} icon={<IcGear/>}
            label="Основные" sub="Значения по умолчанию для новых маршрутов"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcLocomotive/>}
            label="Локомотив" sub="Поля, расчёты, итоги"/>
        </MCard>

        <GroupHead t={t}>Приложение</GroupHead>
        <MCard t={t}>
          <SettingsRow t={t} icon={<IcMoon/>}
            label="Тема" value={dark ? "Тёмная" : "Системная"}/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcBell/>}
            label="Уведомления" value="Включены"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcCloud/>}
            label="Резервная копия"
            sub="Google Drive · вчера в 22:14"
            badge={{ label: 'Вкл', tone: 'success' }}/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t} icon={<IcArchive/>}
            label="Экспорт данных"/>
        </MCard>

        <GroupHead t={t}>Поддержка</GroupHead>
        <MCard t={t}>
          <SettingsRow t={t}
            icon={<IcHelp/>}
            label="Помощь и FAQ"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t}
            icon={<IcMail/>}
            label="Написать в поддержку"/>
          <Sep t={t} inset={0}/>
          <SettingsRow t={t}
            icon={<IcStar/>}
            label="Оценить в Google Play"/>
        </MCard>

        <AboutCard t={t} mat/>

        <div style={{
          padding: '20px 4px 8px', textAlign: 'center',
          fontFamily: M.fontMono, fontSize: 11, color: t.textFaint,
          letterSpacing: 0.4,
        }}>
          © 2026 mashinist.app
        </div>
      </div>

      <FauxTabBarAndroid t={t} active="settings"/>
    </ADevice>
  );
}


// ════════════════════════════════════════════════════════════════
// Подэкран «Основные» — пример редизайна одного из подразделов
// ════════════════════════════════════════════════════════════════
//
// На исходном Android-скрине было 4 группы с заголовками-h2 и hint
// под каждой карточкой. Я перевожу их в общий стиль приложения:
// UPPERCASE-MONO GroupHead, компактные строки FieldRow / SwitchRow,
// hint один раз сверху группы.
//
// 4 секции:
//   1. Данные по умолчанию (тяга, формат времени, стандартное время)
//   2. Перерыв (toggle + пояснение)
//   3. Стиль выбора времени (Системный / Кастомный)
//   4. Свыше 12 часов → следование пассажиром

function SwitchRow({ t, label, sub, on, mat }) {
  return (
    <div style={{
      padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 14,
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text }}>{label}</div>
        {sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{sub}</div>}
      </div>
      <Switch t={t} on={on} mat={mat}/>
    </div>
  );
}

function SelectRow({ t, label, value, sub, chevron = true }) {
  return (
    <div style={{
      padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer',
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: M.fontSans, fontSize: 15, fontWeight: 500, color: t.text }}>{label}</div>
        {sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{sub}</div>}
      </div>
      <span style={{ fontFamily: M.fontSans, fontSize: 14, color: t.textMuted }}>{value}</span>
      {chevron && <IcChevronRight width="14" height="14" style={{ color: t.textFaint, flexShrink: 0 }}/>}
    </div>
  );
}

// ─── Radio-строка: одна опция из набора (для inline-выбора в карточке)
//   • iOS: чекмарк акцентного цвета справа у выбранного, без круга слева
//   • Android M3: радио-кружок слева
//   • sub — поясняющая фраза под названием опции (опц.)
function RadioRow({ t, label, sub, selected, mat }) {
  return (
    <div style={{
      padding: '14px 18px',
      display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer',
    }}>
      {mat && (
        <div style={{
          width: 20, height: 20, borderRadius: 10,
          border: `2px solid ${selected ? t.accent : t.borderStrong}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          {selected && (
            <div style={{
              width: 10, height: 10, borderRadius: 5, background: t.accent,
            }}/>
          )}
        </div>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontFamily: M.fontSans, fontSize: 15,
          fontWeight: selected ? 600 : 500,
          color: t.text,
        }}>{label}</div>
        {sub && <div style={{ ...M.t.captionMuted(t), marginTop: 2 }}>{sub}</div>}
      </div>
      {!mat && selected && (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
          stroke={t.accent} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
          style={{ flexShrink: 0 }}>
          <path d="M5 12l5 5L20 7"/>
        </svg>
      )}
    </div>
  );
}

// ─── Footnote-капшен: маленький поясняющий текст ПОД карточкой секции
//     (классический iOS-паттерн настроек). Привязан к одной группе,
//     а не к экрану — отступа сверху чуть, снизу — пусть выруливает
//     следующий GroupHead со своим mt.
function SectionNote({ t, children, top, bottom }) {
  return (
    <div style={{
      padding: '0 18px',
      marginTop: top ?? 8,
      marginBottom: bottom ?? 0,
      fontFamily: M.fontSans, fontSize: 12, lineHeight: 1.45,
      color: t.textMuted,
    }}>{children}</div>
  );
}

// ─── iOS подэкран «Основные» ────────────────────────────────────
function IOSScreenSettingsGeneral({ dark = false, height = 844 }) {
  const t = dark ? M.dark : M.light;

  return (
    <MDevice dark={dark} height={height}>
      <div style={{ paddingTop: 58 }}>
        <NavBarIOS t={t} title="Основные" back backLabel="Настройки" style="text"/>
      </div>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 103px)', padding: '4px 16px 32px' }}>
        <GroupHead t={t} mt={8}>Данные по умолчанию</GroupHead>
        <Card t={t}>
          <SelectRow t={t} label="Вид тяги" value="Электротяга"/>
          <Sep t={t} inset={18}/>
          <SelectRow t={t} label="Формат времени" value="24-часовой"/>
          <Sep t={t} inset={18}/>
          <SwitchRow t={t} label="Стандартное время работы" on={false}/>
        </Card>
        <SectionNote t={t}>
          Подставляются автоматически при создании нового маршрута. Всегда можно изменить в самом маршруте.
        </SectionNote>

        <GroupHead t={t}>Перерыв</GroupHead>
        <Card t={t}>
          <SwitchRow t={t}
            label="Показывать перерыв"
            sub="Поля перерыва в форме маршрута"
            on={true}/>
        </Card>

        <GroupHead t={t}>Стиль выбора времени</GroupHead>
        <Card t={t}>
          <RadioRow t={t} label="Системный" sub="Стандартный диалог iOS" selected/>
          <Sep t={t} inset={18}/>
          <RadioRow t={t} label="Кастомный" sub="Быстрый ввод цифрами"/>
        </Card>

        <GroupHead t={t}>Свыше 12 часов</GroupHead>
        <Card t={t}>
          <RadioRow t={t} label="Всегда спрашивать" selected/>
          <Sep t={t} inset={18}/>
          <RadioRow t={t} label="Создавать автоматически"/>
          <Sep t={t} inset={18}/>
          <RadioRow t={t} label="Не предлагать"/>
        </Card>
        <SectionNote t={t}>
          Если поездка длится более 12 часов, часть времени можно оформить как следование пассажиром.
        </SectionNote>
      </div>
    </MDevice>
  );
}

// ─── Android подэкран «Основные» ────────────────────────────────
function AndroidScreenSettingsGeneral({ dark = false, height = 844 }) {
  const t = dark ? M.dark : M.light;

  return (
    <ADevice dark={dark} height={height}>
      <TopBarAndroid t={t} title="Основные" back/>

      <div style={{ overflowY: 'auto', height: 'calc(100% - 64px)', padding: '4px 16px 24px' }}>
        <GroupHead t={t} mt={8}>Данные по умолчанию</GroupHead>
        <MCard t={t}>
          <SelectRow t={t} label="Вид тяги" value="Электротяга"/>
          <Sep t={t} inset={0}/>
          <SelectRow t={t} label="Формат времени" value="24-часовой"/>
          <Sep t={t} inset={0}/>
          <SwitchRow t={t} label="Стандартное время работы" on={false} mat/>
        </MCard>
        <SectionNote t={t}>
          Подставляются автоматически при создании нового маршрута. Всегда можно изменить в самом маршруте.
        </SectionNote>

        <GroupHead t={t}>Перерыв</GroupHead>
        <MCard t={t}>
          <SwitchRow t={t}
            label="Показывать перерыв"
            sub="Поля перерыва в форме маршрута"
            on={true} mat/>
        </MCard>

        <GroupHead t={t}>Стиль выбора времени</GroupHead>
        <MCard t={t}>
          <RadioRow t={t} label="Системный" sub="Стандартный диалог Android" selected mat/>
          <Sep t={t} inset={0}/>
          <RadioRow t={t} label="Кастомный" sub="Быстрый ввод цифрами" mat/>
        </MCard>

        <GroupHead t={t}>Свыше 12 часов</GroupHead>
        <MCard t={t}>
          <RadioRow t={t} label="Всегда спрашивать" selected mat/>
          <Sep t={t} inset={0}/>
          <RadioRow t={t} label="Создавать автоматически" mat/>
          <Sep t={t} inset={0}/>
          <RadioRow t={t} label="Не предлагать" mat/>
        </MCard>
        <SectionNote t={t}>
          Если поездка длится более 12 часов, часть времени можно оформить как следование пассажиром.
        </SectionNote>
      </div>
    </ADevice>
  );
}


function FauxTabBarIOS({ t, active }) {
  const items = [
    { id: 'trips', I: IcDocument, l: 'Поездки' },
    { id: 'pay',   I: IcRuble,    l: 'Зарплата' },
    { id: '',      I: IcPlus,     l: '', fab: true },
    { id: 'settings', I: IcSliders, l: 'Настройки' },
    { id: 'profile',  I: IcProfile, l: 'Профиль' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 22, left: 10, right: 10, zIndex: 50,
      background: t.surface, borderRadius: 30,
      boxShadow: '0 12px 32px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.06)',
      border: `1px solid ${t.border}`,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '10px 14px',
    }}>
      {items.map((it, i) => it.fab ? (
        <button key={i} style={{
          width: 46, height: 46, borderRadius: 23, background: t.cta || t.accent, color: t.ctaInk || t.accentInk,
          border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 18px rgba(0,0,0,0.25)',
        }}><IcPlus width="22" height="22"/></button>
      ) : (
        <button key={i} style={{
          background: 'transparent', border: 'none', cursor: 'pointer',
          display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
          color: it.id === active ? t.text : t.textMuted, padding: '4px 12px',
        }}>
          <it.I width="22" height="22"/>
          <span style={{ fontSize: 10, fontWeight: 600 }}>{it.l}</span>
        </button>
      ))}
    </div>
  );
}

function FauxTabBarAndroid({ t, active }) {
  const items = [
    { id: 'trips', I: IcDocument, l: 'Поездки' },
    { id: 'pay',   I: IcRuble,    l: 'Зарплата' },
    { id: '',      I: IcPlus,     l: '', fab: true },
    { id: 'settings', I: IcSliders, l: 'Настройки' },
    { id: 'profile',  I: IcProfile, l: 'Профиль' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      background: t.surface, height: 72,
      display: 'flex', alignItems: 'center',
      borderTop: `1px solid ${t.border}`,
      zIndex: 50,
    }}>
      {items.map((it, i) => it.fab ? (
        <div key={i} style={{ flex: 1, display: 'flex', justifyContent: 'center' }}>
          <button style={{
            width: 56, height: 32, borderRadius: 16, background: t.accent, color: t.accentInk, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: M.shadow.sm,
          }}><it.I width="22" height="22"/></button>
        </div>
      ) : (
        <button key={i} style={{
          flex: 1, border: 'none', background: 'transparent', display: 'flex',
          flexDirection: 'column', alignItems: 'center', gap: 2, cursor: 'pointer',
          color: it.id === active ? t.accent : t.textMuted,
        }}>
          <div style={{
            padding: '4px 16px', borderRadius: 16,
            background: it.id === active ? t.accentSoft : 'transparent',
          }}><it.I width="22" height="22"/></div>
          <span style={{ fontSize: 11, fontWeight: 600 }}>{it.l}</span>
        </button>
      ))}
    </div>
  );
}


Object.assign(window, {
  IOSScreenSettings, AndroidScreenSettings,
  IOSScreenSettingsGeneral, AndroidScreenSettingsGeneral,
  SwitchRow, SelectRow, RadioRow, SectionNote, SettingsRow, Switch,
});
