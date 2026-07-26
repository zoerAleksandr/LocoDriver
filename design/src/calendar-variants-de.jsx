// Календарь — основной экран
// Сворачиваемый календарь: при скролле вверх месяц → неделя,
// детали дня поднимаются. Показываю два состояния:
//   mode='expanded'  — месяц + широкая кнопка «Добавить» + детали дня
//   mode='collapsed' — неделя выбранного дня + кнопка + детали
// ════════════════════════════════════════════════════════════
function CalendarVariantE({ dark = false, selected = 15, mode = 'expanded', initialSheetOpen = false, platform = 'ios', height = 844 }) {
  const t = dark ? M.dark : M.light;
  const C = calColors();
  const [sheetOpen, setSheetOpen] = React.useState(initialSheetOpen);

  const { trips, abs } = getDayEvents(selected);
  const wd = (new Date(CAL_MONTH.year, CAL_MONTH.month, selected).getDay() + 6) % 7;
  const absRange = (() => {
    if (!abs) return null;
    let s = selected, e = selected;
    while (CAL_ABSENCE[s - 1] && CAL_ABSENCE[s - 1].type === abs.type) s--;
    while (CAL_ABSENCE[e + 1] && CAL_ABSENCE[e + 1].type === abs.type) e++;
    return { start: s, end: e, days: e - s + 1, type: abs.type };
  })();

  const isExpanded = mode === 'expanded';

  return (
    <MDevice dark={dark} platform={platform} height={height}>
      <div style={{ paddingTop: 58, height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* Header — компактнее в collapsed */}
        <div style={{
          padding: isExpanded ? '8px 20px 4px' : '4px 16px 4px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          {isExpanded ? (
            <>
              <div>
                <div style={{ fontSize: 12, fontWeight: 700, color: t.textMuted, letterSpacing: 0.6, textTransform: 'uppercase' }}>
                  {CAL_MONTH.label}
                </div>
                <div style={{
                  fontSize: 32, fontWeight: 800, color: t.text, marginTop: 2,
                  fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -1, lineHeight: 1,
                }}>129:55</div>
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button style={pillBtn(t, { w: 38, h: 38 })}><IcChevronLeft width="16" height="16"/></button>
                <button style={pillBtn(t, { w: 38, h: 38 })}><IcChevronRight width="16" height="16"/></button>
              </div>
            </>
          ) : (
            <>
              <button style={pillBtn(t, { w: 36, h: 36 })}><IcChevronLeft width="14" height="14"/></button>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <div style={{ fontSize: 16, fontWeight: 700, color: t.text }}>Май 2026</div>
                <div style={{ fontSize: 11, color: t.textMuted, fontFamily: M.fontMono, marginTop: 1 }}>
                  129:55 · 12 поездок
                </div>
              </div>
              <button style={pillBtn(t, { w: 36, h: 36 })}><IcChevronRight width="14" height="14"/></button>
            </>
          )}
        </div>

        {/* Filter — только в expanded */}
        {isExpanded && (
          <div style={{ padding: '12px 16px 6px' }}>
            <div style={{ display: 'flex', padding: 3, borderRadius: 10, background: t.bgSubtle, gap: 2 }}>
              {['Всё', 'Поездки', 'Отвлечения'].map((label, i) => (
                <div key={label} style={{
                  flex: 1, textAlign: 'center', padding: '7px 0',
                  fontSize: 13, fontWeight: 600,
                  color: i === 0 ? t.text : t.textMuted,
                  background: i === 0 ? t.surface : 'transparent',
                  borderRadius: 8,
                  boxShadow: i === 0 ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                }}>{label}</div>
              ))}
            </div>
          </div>
        )}

        {/* Week header */}
        <div style={{
          padding: isExpanded ? '4px 8px' : '4px 8px 2px',
          display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4,
        }}>
          {WEEK_RU.map((d, i) => (
            <div key={d} style={{
              textAlign: 'center', fontSize: 10, fontWeight: 700,
              color: i >= 5 ? t.accent : t.textFaint, letterSpacing: 0.6, padding: '4px 0',
            }}>{d.toUpperCase()}</div>
          ))}
        </div>

        {/* Calendar — month или week */}
        {isExpanded
          ? <MonthGridE t={t} C={C} selected={selected}/>
          : <WeekStripE t={t} C={C} selected={selected}/>
        }

        {/* Collapse-handle */}
        <div style={{ padding: '4px 0 6px', display: 'flex', justifyContent: 'center', cursor: 'pointer' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 6,
            padding: '4px 12px', borderRadius: 12, background: t.bgSubtle,
            fontSize: 11, color: t.textMuted, fontWeight: 600,
          }}>
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
              style={{ transform: isExpanded ? 'rotate(0deg)' : 'rotate(180deg)' }}>
              <polyline points="18 15 12 9 6 15"/>
            </svg>
            {isExpanded ? 'Свернуть в неделю' : 'Развернуть в месяц'}
          </div>
        </div>

        {/* Кнопка добавить — широкая */}
        <div style={{ padding: '4px 16px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <button onClick={() => setSheetOpen(true)} style={{
            width: '100%', height: 48, borderRadius: 14,
            background: t.accent, color: '#fff', border: 'none',
            fontSize: 15, fontWeight: 700, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            boxShadow: '0 4px 12px rgba(0,160,245,0.25)',
          }}>
            <span style={{ fontSize: 18, fontWeight: 400, marginTop: -2 }}>+</span>
            Добавить событие на {selected} мая
          </button>
          {/* Вход в мастер «Заполнить месяц» — действие уровня месяца */}
          <button style={{
            width: '100%', height: 46, borderRadius: 14,
            background: 'transparent', color: t.text,
            border: `1.5px solid ${t.border}`,
            fontSize: 14.5, fontWeight: 600, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke={t.accent} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="4" width="18" height="17" rx="2.5"/><path d="M3 9h18M8 2v4M16 2v4"/>
              <path d="M7.5 14l1.4 1.4L11.5 13M14 14.5h3.5M14 17.5h2.5"/>
            </svg>
            Заполнить месяц по графику
          </button>
        </div>

        {/* Контент дня — только выбранный день */}
        <div style={{
          flex: 1, overflowY: 'auto',
          padding: '4px 16px 32px',
          borderTop: `1px solid ${t.border}`,
        }}>
          <div style={{ padding: '12px 4px 10px', display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <div style={{ fontSize: 18, fontWeight: 800, color: t.text }}>{selected} мая</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: t.textMuted }}>{WEEK_RU[wd]}</div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {abs && <AbsenceCard t={t} C={C} abs={abs} range={absRange}/>}
            {trips.map((tr, k) => <TripItemCard key={k} t={t} trip={tr}/>)}
            {!abs && trips.length === 0 && (
              <div style={{ padding: '24px 16px', textAlign: 'center', color: t.textMuted, fontSize: 14 }}>
                Нет событий в этот день.
              </div>
            )}
          </div>
        </div>
      </div>
      {sheetOpen && (
        <AddEventSheetE t={t} selected={selected} onClose={() => setSheetOpen(false)}/>
      )}
    </MDevice>
  );
}

// ════════════════════════════════════════════════════════════
// Шторка выбора типа события — Маршрут или Отвлечение
// (без иконок и подписей — иконки подберём отдельно)
// ════════════════════════════════════════════════════════════
function AddEventSheetE({ t, selected, onClose }) {
  const opts = [
    { id: 'route',   label: 'Маршрут',   color: '#00A0F5' },
    { id: 'absence', label: 'Отвлечение', color: '#FF9F0A' },
    { id: 'dayoff',  label: 'Выходной',   color: '#F4433C' },
  ];
  return (
    <ModalScrim onClose={onClose}>
      <SheetShell t={t} title="Что добавить?" sub={`На ${selected} мая`}>
        <div style={{ padding: '4px 12px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
          {opts.map(o => (
            <div key={o.id} onClick={onClose} style={{
              padding: '20px 16px', borderRadius: 14, background: t.bg,
              border: `1px solid ${t.border}`,
              display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer',
            }}>
              <div style={{ width: 4, height: 22, borderRadius: 2, background: o.color }}/>
              <div style={{ flex: 1, fontSize: 17, fontWeight: 700, color: t.text }}>{o.label}</div>
              <IcChevronRight width="14" height="14" style={{ color: t.textFaint }}/>
            </div>
          ))}
        </div>
      </SheetShell>
    </ModalScrim>
  );
}

// ════════════════════════════════════════════════════════════
// (RouteFormScreen удалён — поток создания маршрута переехал
//  в add-route-flow.jsx: выбор времени явки + дат прямо в календаре)
// ════════════════════════════════════════════════════════════
function RouteFormScreen_unused({ selected = 15 }) {
  const t = M.light;
  return (
    <MDevice dark={false}>
      <div style={{ paddingTop: 58, height: '100%', display: 'flex', flexDirection: 'column', background: t.bgSubtle }}>
        {/* Шапка экрана */}
        <div style={{
          padding: '8px 16px 14px', background: t.surface,
          borderBottom: `1px solid ${t.border}`,
          display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center',
        }}>
          <button style={{
            ...pillBtn(t, { w: 36, h: 36 }), justifySelf: 'start',
          }}><IcChevronLeft width="16" height="16"/></button>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: t.text }}>Новый маршрут</div>
            <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>{selected} мая · пятница</div>
          </div>
          <div/>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '14px 0 100px' }}>
          {/* Группа: основное */}
          <FormSectionLabel t={t}>Основное</FormSectionLabel>
          <FormGroup t={t}>
            <FormRow t={t} label="Поезд №"  value="115"            placeholder="Номер"/>
            <FormRow t={t} label="Дата явки" value="15 мая 2026"   readOnly/>
            <FormRow t={t} label="Время явки" value="07:45"        mono/>
          </FormGroup>

          {/* Группа: маршрут */}
          <FormSectionLabel t={t}>Маршрут</FormSectionLabel>
          <FormGroup t={t}>
            <FormRow t={t} label="Откуда"    value="Москва"        placeholder="Станция"/>
            <FormRow t={t} label="Куда"      value="Тверь"         placeholder="Станция"/>
            <FormRow t={t} label="Длительность" value="04:35"      mono placeholder="чч:мм"/>
          </FormGroup>

          {/* Группа: расчёт */}
          <FormSectionLabel t={t}>Дополнительно</FormSectionLabel>
          <FormGroup t={t}>
            <FormRow t={t} label="Тип работы" value="Локомотивная бригада" muted/>
            <FormRow t={t} label="Заметка"    value=""              placeholder="Не задано" muted/>
          </FormGroup>

          <div style={{ padding: '14px 20px 0', fontSize: 12, color: t.textMuted, lineHeight: 1.4 }}>
            Длительность рассчитается автоматически по расписанию, если выбрать
            поезд из справочника.
          </div>
        </div>

        {/* Зафиксированная кнопка снизу */}
        <div style={{
          position: 'absolute', left: 0, right: 0, bottom: 0,
          padding: '12px 16px 28px', background: t.surface,
          borderTop: `1px solid ${t.border}`,
        }}>
          <button style={{
            width: '100%', height: 50, borderRadius: 14,
            background: t.accent, color: '#fff', border: 'none',
            fontSize: 16, fontWeight: 700, cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0,160,245,0.25)',
          }}>Сохранить маршрут</button>
        </div>
      </div>
    </MDevice>
  );
}

function FormSectionLabel({ t, children }) {
  return (
    <div style={{
      padding: '14px 20px 6px', fontSize: 11, fontWeight: 700,
      color: t.textMuted, letterSpacing: 0.5, textTransform: 'uppercase',
    }}>{children}</div>
  );
}

function FormGroup({ t, children }) {
  // Превращаем массив детей в строки с разделителями
  const rows = React.Children.toArray(children);
  return (
    <div style={{
      margin: '0 16px', background: t.surface,
      borderRadius: 14, border: `1px solid ${t.border}`, overflow: 'hidden',
    }}>
      {rows.map((row, i) => (
        <React.Fragment key={i}>
          {row}
          {i < rows.length - 1 && <div style={{ height: 1, background: t.border, marginLeft: 16 }}/>}
        </React.Fragment>
      ))}
    </div>
  );
}

function FormRow({ t, label, value, placeholder, mono, readOnly, muted }) {
  const isEmpty = !value;
  return (
    <div style={{
      padding: '14px 16px', display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', gap: 12, cursor: readOnly ? 'default' : 'pointer',
    }}>
      <div style={{ fontSize: 15, color: t.text, fontWeight: 500 }}>{label}</div>
      <div style={{
        fontSize: 15, fontWeight: mono ? 700 : 500,
        color: isEmpty ? t.textFaint : (muted ? t.textMuted : t.text),
        fontFamily: mono ? M.fontMono : 'inherit',
        fontVariantNumeric: mono ? 'tabular-nums' : 'normal',
        display: 'flex', alignItems: 'center', gap: 8,
        maxWidth: '60%', textAlign: 'right',
      }}>
        <span>{isEmpty ? placeholder : value}</span>
        {!readOnly && <IcChevronRight width="12" height="12" style={{ color: t.textFaint }}/>}
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════
// Отвлечение — экран выбора даты/диапазона (без шторки)
// ════════════════════════════════════════════════════════════
const ABSENCE_TYPES = [
  { id: 'vacation',  label: 'Отпуск',                        color: '#34C759' },
  { id: 'sick',      label: 'Больничный',                    color: '#FF9F0A' },
  { id: 'courses',   label: 'Курсы',                         color: '#AF52DE' },
  { id: 'donor',     label: 'Донорские',                     color: '#FF3B30' },
  { id: 'childcare', label: 'По уходу за ребёнком-инвалидом', color: '#00A0F5' },
  { id: 'other',     label: 'Прочее',                        color: '#8A8278' },
];

function AbsenceFlowScreen({ dark = false, rangeStart = 15, rangeEnd = 20, type = 'vacation', pickerOpen = false, platform = 'ios' }) {
  const t = dark ? M.dark : M.light;
  const sel = ABSENCE_TYPES.find(a => a.id === type) || ABSENCE_TYPES[0];
  const isSingle = rangeStart === rangeEnd;
  const days = rangeEnd - rangeStart + 1;
  const hours = days * 8;

  return (
    <MDevice dark={dark} platform={platform}>
      <div style={{ paddingTop: 58, height: '100%', display: 'flex', flexDirection: 'column' }}>
        {/* Шапка — на фоне самого экрана (без surface), чтобы стыковалось со status bar */}
        <div style={{
          padding: '8px 16px 10px',
          display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center',
        }}>
          <button style={{ ...pillBtn(t, { w: 36, h: 36 }), justifySelf: 'start' }}>
            <IcChevronLeft width="16" height="16"/>
          </button>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: t.text }}>Новое отвлечение</div>
            <div style={{ fontSize: 12, color: t.textMuted, marginTop: 1 }}>
              {isSingle ? 'Выбрана 1 дата' : 'Выбран диапазон дат'}
            </div>
          </div>
          <div/>
        </div>

        {/* Месяц + навигация */}
        <div style={{
          padding: '8px 20px 0',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ fontSize: 17, fontWeight: 700, color: t.text }}>{CAL_MONTH.label}</div>
          <div style={{ display: 'flex', gap: 6 }}>
            <button style={pillBtn(t, { w: 32, h: 32 })}><IcChevronLeft width="14" height="14"/></button>
            <button style={pillBtn(t, { w: 32, h: 32 })}><IcChevronRight width="14" height="14"/></button>
          </div>
        </div>

        {/* Week header */}
        <div style={{
          padding: '8px 8px 2px',
          display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 0,
        }}>
          {WEEK_RU.map((d, i) => (
            <div key={d} style={{
              textAlign: 'center', fontSize: 10, fontWeight: 700,
              color: i >= 5 ? t.accent : t.textFaint, letterSpacing: 0.6, padding: '4px 0',
            }}>{d.toUpperCase()}</div>
          ))}
        </div>

        {/* Календарь — режим выбора диапазона.
            Текущий выбор красим НАСЫЩЕННЫМ цветом выбранного типа
            (отпуск — зелёный, курсы — фиолетовый и т.д.), а уже
            сохранённые отвлечения остаются БЛЕДНЫМ призраком того же
            семейства — так «выбираю сейчас» и «уже было» не путаются. */}
        <RangeMonthGrid t={t} rangeStart={rangeStart} rangeEnd={rangeEnd} color={sel.color}/>

        {/* Подсказка */}
        <div style={{
          margin: '6px 20px 0', fontSize: 12, color: t.textMuted, lineHeight: 1.4,
        }}>
          Нажмите дату для одного дня или две даты — начало и конец диапазона.
        </div>

        {/* Тип отвлечения — выпадающий список (раскрывается поверх контента) */}
        <div style={{ padding: '14px 16px 0', position: 'relative', zIndex: 20 }}>
          <div style={{
            background: t.surface, borderRadius: 14, border: `1px solid ${t.border}`,
            position: 'relative',
            boxShadow: pickerOpen ? '0 10px 24px rgba(0,0,0,0.12)' : 'none',
          }}>
            <div style={{
              padding: '14px 16px',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              gap: 12, cursor: 'pointer',
            }}>
              <div style={{ fontSize: 15, color: t.text, fontWeight: 500 }}>Тип отвлечения</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ width: 8, height: 8, borderRadius: 4, background: sel.color }}/>
                <div style={{ fontSize: 15, fontWeight: 600, color: t.text }}>{sel.label}</div>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
                  style={{ color: t.textFaint, transform: pickerOpen ? 'rotate(180deg)' : 'none' }}>
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
            </div>

            {pickerOpen && (
              <div style={{
                position: 'absolute', top: 'calc(100% + 6px)', left: 0, right: 0,
                background: t.surface, borderRadius: 14, border: `1px solid ${t.border}`,
                boxShadow: '0 16px 32px rgba(0,0,0,0.14)',
                overflow: 'hidden', zIndex: 30,
              }}>
                {ABSENCE_TYPES.map((o, i) => {
                  const active = o.id === type;
                  return (
                    <div key={o.id} style={{
                      padding: '14px 16px',
                      display: 'flex', alignItems: 'center', gap: 12,
                      cursor: 'pointer',
                      borderTop: i === 0 ? 'none' : `1px solid ${t.border}`,
                      background: active ? t.bgSubtle : 'transparent',
                    }}>
                      <div style={{ width: 8, height: 8, borderRadius: 4, background: o.color }}/>
                      <div style={{
                        flex: 1, fontSize: 15, color: t.text,
                        fontWeight: active ? 700 : 500,
                      }}>{o.label}</div>
                      {active && (
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                          stroke={t.accent} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M5 12l5 5L20 7"/>
                        </svg>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Сводная карточка: период вверху, дни+часы внизу */}
        <div style={{ padding: '10px 16px 0' }}>
          <div style={{
            borderRadius: 14,
            background: sel.color + '1A', border: `1px solid ${sel.color}33`,
            overflow: 'hidden',
          }}>
            <div style={{ padding: '12px 16px' }}>
              <div style={{
                fontSize: 11, fontWeight: 700, color: sel.color,
                textTransform: 'uppercase', letterSpacing: 0.5,
              }}>{sel.label}</div>
              <div style={{
                fontSize: 22, fontWeight: 800, color: t.text, marginTop: 4,
                fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -0.5,
                lineHeight: 1,
              }}>
                {isSingle ? `${rangeStart} мая` : `${rangeStart}–${rangeEnd} мая`}
              </div>
            </div>
            <div style={{
              borderTop: `1px solid ${sel.color}33`,
              display: 'grid', gridTemplateColumns: '1fr 1fr',
            }}>
              <StatCell t={t} label="Дней"
                value={String(days)}
                sub={days === 1 ? 'день' : days < 5 ? 'дня' : 'дней'}
                divider={`1px solid ${sel.color}33`}/>
              <StatCell t={t} label="Часов"
                value={`${hours}:00`}
                sub="в норме 8 ч/день"/>
            </div>
          </div>
        </div>

        {/* Сохранить — sticky bottom (без смены фона) */}
        <div style={{ flex: 1 }}/>
        <div style={{ padding: '12px 16px 28px' }}>
          <button style={{
            width: '100%', height: 50, borderRadius: 14,
            background: t.accent, color: '#fff', border: 'none',
            fontSize: 16, fontWeight: 700, cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0,160,245,0.25)',
          }}>Сохранить</button>
        </div>
      </div>
    </MDevice>
  );
}

function StatCell({ t, label, value, sub, divider }) {
  return (
    <div style={{
      padding: '12px 16px',
      borderLeft: divider ? undefined : 'none',
      borderRight: divider || 'none',
    }}>
      <div style={{
        fontSize: 11, fontWeight: 700, color: t.textMuted,
        textTransform: 'uppercase', letterSpacing: 0.5,
      }}>{label}</div>
      <div style={{
        display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 4,
      }}>
        <span style={{
          fontSize: 22, fontWeight: 800, color: t.text,
          fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -0.5,
          lineHeight: 1,
        }}>{value}</span>
        {sub && <span style={{ fontSize: 12, color: t.textMuted }}>{sub}</span>}
      </div>
    </div>
  );
}

function SummaryCol_unused({ t, label, value, sub, mono, accent }) {
  return (
    <div>
      <div style={{
        fontSize: 11, fontWeight: 700,
        color: accent || t.textMuted,
        textTransform: 'uppercase', letterSpacing: 0.5,
      }}>{label}</div>
      <div style={{
        fontSize: 18, fontWeight: 800, color: t.text, marginTop: 4,
        fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums', letterSpacing: -0.3,
        lineHeight: 1.1,
      }}>{value}</div>
      {sub && <div style={{ fontSize: 11, color: t.textMuted, marginTop: 2 }}>{sub}</div>}
    </div>
  );
}

// Сетка месяца в режиме выбора диапазона.
// Ячейка — такая же «карточка», как на главном календаре: видно
// существующие поездки и отвлечения под выбираемым диапазоном, чтобы
// машинист видел, какие дни уже заняты. Поверх рисуется выделение:
//   ТЕКУЩИЙ ВЫБОР (color = цвет выбранного типа отвлечения):
//     • isStart / isEnd / isSingle — целиком залит насыщенным цветом типа
//     • середина диапазона          — заметная (≈40%) заливка тем же цветом
//   УЖЕ СОХРАНЁННЫЕ отвлечения (вне текущего выбора):
//     • бледный «призрак» — белый фон + тонкий пунктир цвета их типа,
//       намеренно слабее текущего выбора, чтобы не сливаться с ним.
function RangeMonthGrid({ t, rangeStart, rangeEnd, color }) {
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);
  const C = calColors();
  const isSingle = rangeStart === rangeEnd;

  return (
    <div style={{
      padding: '4px 8px 6px',
      display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 3,
    }}>
      {cells.map((d, i) => (
        <RangeMonthCell key={i} t={t} C={C} d={d} idx={i}
          color={color}
          inRange={d != null && d >= rangeStart && d <= rangeEnd}
          isStart={d === rangeStart}
          isEnd={d === rangeEnd}
          isSingle={isSingle}/>
      ))}
    </div>
  );
}

function RangeMonthCell({ t, C, d, idx, color, inRange, isStart, isEnd, isSingle }) {
  if (d == null) return <div style={{ height: 54 }}/>;
  const trips = CAL_TRIPS[d];
  const abs   = CAL_ABSENCE[d];
  const isToday   = d === CAL_TODAY;
  const isWeekend = (idx % 7) >= 5;
  const ac        = abs ? C[abs.type] : null;
  // ВАЖНО: «якорь» определяется по самому дню (isStart/isEnd), а НЕ по
  // глобальному isSingle — иначе при выборе одной даты все ячейки месяца
  // станут якорями. Для одиночной даты этот день и так isStart && isEnd.
  const isAnchor  = isStart || isEnd;
  const isMid     = inRange && !isAnchor;

  // ── Базовый слой ──────────────────────────────────────────
  // Обычная карточка. Поездки — мягкая синяя подложка.
  // Уже сохранённые отвлечения (если они НЕ в текущем выборе) —
  // бледный «призрак»: белый фон + тонкий пунктир цвета их типа.
  let bg = trips ? C.trip.softer : t.surface;
  let border = `1px solid ${trips ? C.trip.solid + '22' : t.border}`;
  if (abs && !inRange) {
    // сохранённое отвлечение вне выбора — бледная заливка своего
    // цвета + тонкий бордюр такой же толщины, как у дня с маршрутом
    bg = ac.softer;
    border = `1px solid ${ac.solid}33`;
  }

  // ── Слой текущего выбора (всегда поверх) ──────────────────
  if (isAnchor) {
    // концевая / одиночная дата — целиком насыщенный цвет типа
    bg = color;
    border = `1.5px solid ${color}`;
  } else if (isMid) {
    // середина диапазона — заметная полупрозрачная заливка тем же цветом
    bg = color + '40';
    border = `1.5px solid ${color}`;
  }

  // ── Цвета содержимого ─────────────────────────────────────
  const onAccent = isAnchor;
  const dayColor = onAccent ? '#fff'
    : isMid ? t.text
    : isToday ? t.accent
    : isWeekend ? t.textMuted : t.text;
  const tripColor = onAccent ? 'rgba(255,255,255,0.85)' : C.trip.solid;
  // бэйдж сохранённого отвлечения вне выбора — бледный
  const badgeColor = onAccent ? 'rgba(255,255,255,0.9)'
    : (abs ? ac.solid + (inRange ? '' : 'AA') : t.textFaint);

  return (
    <div style={{
      height: 54, borderRadius: 10, padding: '4px 5px',
      background: bg, border,
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      {/* Номер дня — верхний левый угол */}
      <span style={{
        fontSize: 13, fontWeight: 700,
        color: dayColor,
        fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
        lineHeight: 1,
      }}>{d}</span>

      {/* Время явки / тип отвлечения — прижаты к правому нижнему краю */}
      <div style={{ marginTop: 'auto', textAlign: 'right', lineHeight: 1 }}>
        {trips && (
          <div style={{
            fontSize: 10, fontWeight: 600,
            color: tripColor,
            fontFamily: M.fontMono, fontVariantNumeric: 'tabular-nums',
            lineHeight: 1.05,
          }}>{trips[0]}{trips.length > 1 ? `·${trips.length}` : ''}</div>
        )}
        {abs && (
          <div style={{
            fontSize: 9, fontWeight: 700, color: badgeColor,
            textTransform: 'uppercase', letterSpacing: 0.3,
            marginTop: trips ? 1 : 0,
          }}>{absBadge(abs.type)}</div>
        )}
      </div>
    </div>
  );
}

function MonthGridE({ t, C, selected }) {
  const cells = buildMonthGrid(CAL_MONTH.year, CAL_MONTH.month);
  return (
    <div style={{ padding: '0 8px 4px', display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4 }}>
      {cells.map((d, i) => (
        <CalCellMerged key={i} t={t} C={C} d={d} idx={i} filter="all" isSelected={d === selected}/>
      ))}
    </div>
  );
}

function WeekStripE({ t, C, selected }) {
  const weekStart = selected - ((new Date(CAL_MONTH.year, CAL_MONTH.month, selected).getDay() + 6) % 7);
  const weekDays = Array.from({ length: 7 }, (_, i) => weekStart + i);
  return (
    <div style={{ padding: '0 8px 4px', display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4 }}>
      {weekDays.map((d, i) => {
        if (d < 1 || d > 31) return <div key={i} style={{ height: 50 }}/>;
        return <CalCellMerged key={i} t={t} C={C} d={d} idx={i} filter="all" isSelected={d === selected}/>;
      })}
    </div>
  );
}
