// Landing page — Машинист
// Sections: hero with phone mockups, features, use cases, FAQ, footer

function Landing() {
  const t = M.light;
  return (
    <div style={{
      width: 1400, background: t.bg, fontFamily: M.fontSans, color: t.text,
      overflow: 'hidden',
    }}>
      {/* Header */}
      <header style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '24px 56px', borderBottom: `1px solid ${t.border}`,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ color: t.text }}><IcLogo size={28}/></span>
          <span style={{ fontSize: 20, fontWeight: 800, letterSpacing: -.5 }}>Машинист</span>
        </div>
        <nav style={{ display: 'flex', gap: 32, fontSize: 14, color: t.textMuted }}>
          <a style={{ color: 'inherit', textDecoration: 'none' }}>Возможности</a>
          <a style={{ color: 'inherit', textDecoration: 'none' }}>Сценарии</a>
          <a style={{ color: 'inherit', textDecoration: 'none' }}>FAQ</a>
          <a style={{ color: 'inherit', textDecoration: 'none' }}>Контакты</a>
        </nav>
        <button style={{
          padding: '10px 18px', background: t.accent, color: t.accentInk, border: 'none',
          borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer',
        }}>Скачать</button>
      </header>

      {/* Hero */}
      <section style={{
        padding: '80px 56px 60px', display: 'grid',
        gridTemplateColumns: '1fr 520px', gap: 40, alignItems: 'center',
      }}>
        <div>
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: 8,
            background: t.accentSoft, color: t.accent, padding: '6px 12px', borderRadius: 999,
            fontSize: 12, fontFamily: M.fontMono, fontWeight: 600, letterSpacing: 1,
            marginBottom: 28,
          }}>
            <span style={{ width: 6, height: 6, borderRadius: 3, background: t.accent }}/>
            ДЛЯ ЛОКОМОТИВНЫХ БРИГАД
          </div>
          <h1 style={{
            fontSize: 72, fontWeight: 800, letterSpacing: -2, lineHeight: 1.02,
            margin: 0, color: t.text, textWrap: 'pretty',
          }}>
            Учёт смен,<br/>
            маршрутов<br/>
            и зарплаты.<br/>
            <span style={{ color: t.accent }}>Без таблиц.</span>
          </h1>
          <p style={{
            fontSize: 20, color: t.textMuted, lineHeight: 1.5, margin: '32px 0 40px',
            maxWidth: 540,
          }}>
            Машинист считает норму часов, сверхурочные, ночные и домашний отдых — ровно так,
            как это делают в депо. На смартфоне, в кармане, без интернета.
          </p>

          <div style={{ display: 'flex', gap: 12, marginBottom: 40 }}>
            <StoreBtn t={t} store="appstore"/>
            <StoreBtn t={t} store="googleplay"/>
          </div>

          {/* meta row */}
          <div style={{ display: 'flex', gap: 32, fontSize: 13, color: t.textMuted }}>
            <div>
              <div style={{ fontFamily: M.fontMono, fontSize: 24, fontWeight: 700, color: t.text }}>12k+</div>
              пользователей
            </div>
            <div>
              <div style={{ fontFamily: M.fontMono, fontSize: 24, fontWeight: 700, color: t.text }}>4.8</div>
              средняя оценка
            </div>
            <div>
              <div style={{ fontFamily: M.fontMono, fontSize: 24, fontWeight: 700, color: t.text }}>0 ₽</div>
              базовый тариф
            </div>
          </div>
        </div>

        {/* Phone mockups stacked */}
        <div style={{ position: 'relative', height: 720 }}>
          <div style={{
            position: 'absolute', right: -30, top: 20, transform: 'rotate(4deg) scale(0.85)',
            transformOrigin: 'center',
          }}>
            <IOSScreenRoute dark={false}/>
          </div>
          <div style={{
            position: 'absolute', left: -10, top: 60, transform: 'rotate(-3deg) scale(0.85)',
            transformOrigin: 'center', zIndex: 2,
          }}>
            <IOSScreenTrips dark={false}/>
          </div>
        </div>
      </section>

      {/* Features */}
      <section style={{ padding: '80px 56px', background: '#14120E', color: '#F2EFE8' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 12, color: M.dark.accent, letterSpacing: 2 }}>01</span>
          <span style={{ fontSize: 14, color: 'rgba(242,239,232,0.6)' }}>Возможности</span>
        </div>
        <h2 style={{ fontSize: 48, fontWeight: 800, letterSpacing: -1, margin: '0 0 56px', maxWidth: 900, textWrap: 'pretty' }}>
          Приложение под ваш <span style={{ color: M.dark.accent }}>график работы</span>,
          а не наоборот.
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24 }}>
          {[
            [IcClock, 'Точные часы', 'Складывает ночные, дневные и сверхурочные по правилам депо. Автоматически вычитает перерывы.'],
            [IcRuble, 'Расчёт зарплаты', 'Выводит итог по каждой смене и по месяцу. Учитывает тарифную сетку и надбавки — редактируемые.'],
            [IcLocomotive, 'Локомотивы и поезда', 'Журнал всего, что вы вели. Поиск, избранное, экспорт в PDF и CSV.'],
            [IcCalendar, 'Календарь смен', 'Видно график на месяц вперёд. Цветом — где норма, где перегруз, где отдых.'],
            [IcHome, 'Домашний отдых', 'Отдельный тип записи — считается иначе, не идёт в часы «на работе».'],
            [IcCloud, 'Синхронизация', 'Работает офлайн. Когда появится сеть — данные доедут на другой ваш телефон.'],
          ].map(([I, h, b], i) => (
            <div key={i} style={{
              background: '#1A1815', borderRadius: 20, padding: 28,
              border: '1px solid rgba(242,239,232,0.08)',
            }}>
              <div style={{
                width: 48, height: 48, borderRadius: 12, background: 'rgba(255,74,92,0.14)',
                color: M.dark.accent, display: 'flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 20,
              }}><I width="26" height="26"/></div>
              <div style={{ fontSize: 22, fontWeight: 700, marginBottom: 8, letterSpacing: -.3 }}>{h}</div>
              <div style={{ fontSize: 14, color: 'rgba(242,239,232,0.6)', lineHeight: 1.55 }}>{b}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Use cases */}
      <section style={{ padding: '96px 56px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 12, color: t.accent, letterSpacing: 2 }}>02</span>
          <span style={{ fontSize: 14, color: t.textMuted }}>Сценарии использования</span>
        </div>
        <h2 style={{ fontSize: 48, fontWeight: 800, letterSpacing: -1, margin: '0 0 48px', maxWidth: 900 }}>
          Один день — одна запись.
        </h2>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
          <UseCase t={t} tag="УТРО" title="Открыл смену за 10 секунд" body="На главном экране — большая красная кнопка «+». Локомотив, поезд, время явки. Дальше можно ехать.">
            <div style={{
              display: 'flex', gap: 12, fontFamily: M.fontMono, fontSize: 13,
              color: t.textMuted,
            }}>
              <span>07:12</span><IcChevronRight width="12" height="12"/>
              <span>локомотив</span><IcChevronRight width="12" height="12"/>
              <span style={{ color: t.accent }}>в рейс</span>
            </div>
          </UseCase>
          <UseCase t={t} tag="ВЕЧЕР" title="Закрыл смену — увидел деньги" body="Приехал в пункт смены — жмёшь «Завершить». Часы посчитаны, рубли в кошельке. Никаких таблиц.">
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <div>
                <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4 }}>ЗА СМЕНУ</div>
                <div style={{ fontSize: 28, fontWeight: 800, fontFamily: M.fontMono }}>12:08</div>
              </div>
              <div style={{ height: 40, width: 1, background: t.border }}/>
              <div>
                <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.textMuted, letterSpacing: 1.4 }}>К ВЫПЛАТЕ</div>
                <div style={{ fontSize: 28, fontWeight: 800, fontFamily: M.fontMono, color: t.accent }}>4 820 ₽</div>
              </div>
            </div>
          </UseCase>
          <UseCase t={t} tag="МЕСЯЦ" title="Увидел перегруз — заранее" body="Когда часы приближаются к норме, полоса становится красной. Знаешь, когда пора отказаться от лишней явки.">
            <div style={{ marginTop: 4 }}>
              <div style={{ fontSize: 11, fontFamily: M.fontMono, color: t.accent, letterSpacing: 1.4, marginBottom: 6 }}>
                ОТРАБОТАНО 181 / 175 Ч — ПЕРЕГРУЗ
              </div>
              <div style={{ height: 6, background: t.bgSubtle, borderRadius: 3 }}>
                <div style={{ width: '100%', height: '100%', background: t.accent, borderRadius: 3 }}/>
              </div>
            </div>
          </UseCase>
          <UseCase t={t} tag="СПОР" title="Показал кассиру расшифровку" body="Если начислили меньше — открываешь смену, показываешь часы и надбавки по минутам. Разговор короче.">
            <div style={{ fontFamily: M.fontMono, fontSize: 13, color: t.text, lineHeight: 1.8 }}>
              <div>ночные · 4:15 → +35%</div>
              <div>сверх · 2:08 → +50%</div>
              <div>выходной · — · +100%</div>
            </div>
          </UseCase>
        </div>
      </section>

      {/* Screenshot gallery */}
      <section style={{
        padding: '60px 0 96px', background: t.bgSubtle,
        textAlign: 'center',
      }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, justifyContent: 'center', marginBottom: 12 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 12, color: t.accent, letterSpacing: 2 }}>03</span>
          <span style={{ fontSize: 14, color: t.textMuted }}>Интерфейс</span>
        </div>
        <h2 style={{ fontSize: 40, fontWeight: 800, letterSpacing: -.8, margin: '0 0 48px' }}>
          Как это выглядит в руке
        </h2>
        <div style={{
          display: 'flex', gap: 24, justifyContent: 'center', alignItems: 'flex-end',
        }}>
          <div style={{ transform: 'scale(0.65)', transformOrigin: 'bottom center', margin: '0 -50px' }}>
            <IOSScreenTrips dark={false}/>
          </div>
          <div style={{ transform: 'scale(0.7)', transformOrigin: 'bottom center' }}>
            <IOSScreenRoute dark={true}/>
          </div>
          <div style={{ transform: 'scale(0.65)', transformOrigin: 'bottom center', margin: '0 -50px' }}>
            <IOSScreenTrips dark={true}/>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section style={{ padding: '96px 56px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 12 }}>
          <span style={{ fontFamily: M.fontMono, fontSize: 12, color: t.accent, letterSpacing: 2 }}>04</span>
          <span style={{ fontSize: 14, color: t.textMuted }}>Вопросы</span>
        </div>
        <h2 style={{ fontSize: 48, fontWeight: 800, letterSpacing: -1, margin: '0 0 48px' }}>
          Коротко и по делу.
        </h2>
        <div style={{ maxWidth: 900 }}>
          {[
            ['Как считаются ночные часы?', 'С 22:00 до 06:00 по Трудовому кодексу РФ. Коэффициент по умолчанию +35%, но можно поменять в настройках — под вашу тарифную сетку.'],
            ['Работает ли без интернета?', 'Да. Всё локально. Синхронизация — опция, не обязательна.'],
            ['Можно ли экспортировать данные?', 'Да — PDF, CSV и JSON. PDF-расшифровка смены подходит для разговора с расчётчиком.'],
            ['Есть ли Apple Watch / Wear OS?', 'Сейчас нет. В планах — быстрая кнопка «открыть/закрыть смену» на часах.'],
            ['Сколько стоит?', 'Базовые функции — бесплатно. Подписка за синхронизацию между устройствами и экспорт в PDF.'],
          ].map(([q, a], i) => (
            <details key={i} style={{
              background: t.surface, borderRadius: 14, marginBottom: 10, padding: '20px 24px',
              boxShadow: M.shadow.sm, cursor: 'pointer',
            }}>
              <summary style={{ fontSize: 18, fontWeight: 600, listStyle: 'none', position: 'relative', paddingRight: 32 }}>
                {q}
                <span style={{ position: 'absolute', right: 0, top: '50%', transform: 'translateY(-50%)', color: t.accent }}>
                  <IcPlus width="20" height="20"/>
                </span>
              </summary>
              <div style={{ marginTop: 14, fontSize: 15, color: t.textMuted, lineHeight: 1.6 }}>{a}</div>
            </details>
          ))}
        </div>
      </section>

      {/* CTA band */}
      <section style={{
        margin: '0 56px 96px', background: t.accent, color: t.accentInk,
        borderRadius: 28, padding: '64px 56px', position: 'relative', overflow: 'hidden',
      }}>
        <div style={{
          position: 'absolute', right: -60, top: -60, opacity: .15,
        }}>
          <IcLogo size={360} color="#fff"/>
        </div>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ fontFamily: M.fontMono, fontSize: 12, letterSpacing: 2, opacity: .8, marginBottom: 12 }}>
            ГОТОВЫ К СМЕНЕ?
          </div>
          <h2 style={{ fontSize: 56, fontWeight: 800, letterSpacing: -1.2, margin: '0 0 28px', maxWidth: 800 }}>
            Скачайте и откройте смену за одно нажатие.
          </h2>
          <div style={{ display: 'flex', gap: 12 }}>
            <StoreBtn t={M.dark} store="appstore" inverted/>
            <StoreBtn t={M.dark} store="googleplay" inverted/>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer style={{
        padding: '40px 56px 48px', borderTop: `1px solid ${t.border}`,
        display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
        fontSize: 13, color: t.textMuted,
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
            <span style={{ color: t.text }}><IcLogo size={22}/></span>
            <span style={{ fontSize: 15, fontWeight: 700, color: t.text, letterSpacing: -.3 }}>Машинист</span>
          </div>
          © 2026 · для локомотивных бригад
        </div>
        <div style={{ display: 'flex', gap: 40 }}>
          <div>
            <div style={{ fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.2, color: t.textFaint, marginBottom: 10 }}>ПРОДУКТ</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <span>Возможности</span><span>Обновления</span><span>Roadmap</span>
            </div>
          </div>
          <div>
            <div style={{ fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.2, color: t.textFaint, marginBottom: 10 }}>ПРАВОВОЕ</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <span>Политика</span><span>Соглашение</span><span>Возврат</span>
            </div>
          </div>
          <div>
            <div style={{ fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.2, color: t.textFaint, marginBottom: 10 }}>КОНТАКТЫ</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <span>hello@mashinist.app</span><span>Telegram-чат</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

function UseCase({ t, tag, title, body, children }) {
  return (
    <div style={{
      background: t.surface, borderRadius: 22, padding: 32, boxShadow: M.shadow.sm,
      display: 'flex', flexDirection: 'column', gap: 16, minHeight: 280,
    }}>
      <div style={{ fontFamily: M.fontMono, fontSize: 11, letterSpacing: 1.4, color: t.accent }}>{tag}</div>
      <div style={{ fontSize: 24, fontWeight: 700, letterSpacing: -.4, color: t.text }}>{title}</div>
      <div style={{ fontSize: 15, color: t.textMuted, lineHeight: 1.55 }}>{body}</div>
      <div style={{ marginTop: 'auto', padding: '16px 18px', background: t.bgSubtle, borderRadius: 14 }}>{children}</div>
    </div>
  );
}

function StoreBtn({ t, store, inverted }) {
  const bg = inverted ? '#fff' : '#14120E';
  const fg = inverted ? '#14120E' : '#F2EFE8';
  return (
    <button style={{
      padding: '12px 20px', background: bg, color: fg, border: 'none',
      borderRadius: 14, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 12,
      fontFamily: M.fontSans,
    }}>
      <svg width="22" height="22" viewBox="0 0 22 22" fill="currentColor">
        {store === 'appstore' ? (
          <path d="M15.5 11.5c0-2.5 2-3.7 2.1-3.8-1.1-1.7-2.9-1.9-3.5-1.9-1.5-.2-2.9.9-3.7.9s-1.9-.9-3.2-.8c-1.6 0-3.2 1-4 2.4-1.7 3-.4 7.4 1.2 9.8.8 1.2 1.8 2.5 3.1 2.5s1.7-.8 3.2-.8 1.9.8 3.2.8 2.2-1.2 3-2.4c.9-1.4 1.3-2.7 1.3-2.8-.1-.1-2.7-1-2.7-4zM13 4.2c.7-.8 1.1-2 1-3.2-1 0-2.2.7-2.9 1.5-.6.7-1.2 1.9-1.1 3.1 1.2.1 2.3-.6 3-1.4z"/>
        ) : (
          <path d="M4 2.7v16.6c0 .6.3 1.1.8 1.3l9.5-9.9L4 2.7zm13.3 7L5.9 2.2c-.4-.2-.8-.3-1.2-.1l10.5 10.9 2.1-2.3c.6-.6.6-1.5 0-2zm2.4 1.1L17 12.7l2.8 2.9c.9-.5.9-1.8-.1-2.3.1 0 0-.4 0-.5zM5.9 21.8l11.4-7.5-2.2-2.3L5.1 22c.2.1.5.1.8-.2z"/>
        )}
      </svg>
      <div style={{ textAlign: 'left' }}>
        <div style={{ fontSize: 10, opacity: .7, fontFamily: M.fontMono, letterSpacing: .5 }}>
          {store === 'appstore' ? 'Скачать в' : 'Доступно в'}
        </div>
        <div style={{ fontSize: 15, fontWeight: 700, letterSpacing: -.2 }}>
          {store === 'appstore' ? 'App Store' : 'Google Play'}
        </div>
      </div>
    </button>
  );
}

Object.assign(window, { Landing });
