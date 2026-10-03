// Custom железнодорожные icons for Машинист.
// Stroke-based, 24x24, currentColor. Intentional rail references:
// locomotive, rails, semaphore, schedule, etc.

const iconBase = {
  width: 24, height: 24, viewBox: '0 0 24 24',
  fill: 'none', stroke: 'currentColor', strokeWidth: 1.3,
  strokeLinecap: 'round', strokeLinejoin: 'round',
};

function IcLocomotive(p) { return (
  <svg {...iconBase} {...p}>
    <g transform="translate(-2.64 -2.64) scale(1.24)" vectorEffect="non-scaling-stroke">
    {/* флажковая крыша кабины */}
    <path d="M5.2 6.6H12" vectorEffect="non-scaling-stroke"/>
    {/* кабина: стенки до рамы */}
    <path d="M6 6.6V15" vectorEffect="non-scaling-stroke"/>
    <path d="M11.5 6.6V15" vectorEffect="non-scaling-stroke"/>
    {/* окно кабины */}
    <rect x="7.1" y="8.4" width="2.6" height="2.6" rx=".4" fill="currentColor" fillOpacity=".14" vectorEffect="non-scaling-stroke"/>
    {/* труба */}
    <path d="M14.1 9V6.7h1.7V9" vectorEffect="non-scaling-stroke"/>
    {/* котёл со скруглённым носом + отвал и рама */}
    <path d="M11.5 9h4.6c1.4 0 2.2.9 2.2 2.3v1.4L19.4 15H5" vectorEffect="non-scaling-stroke"/>
    {/* фара */}
    <circle cx="16.6" cy="11.6" r=".7" vectorEffect="non-scaling-stroke"/>
    {/* буфер слева */}
    <path d="M3.8 13.9H5" vectorEffect="non-scaling-stroke"/>
    {/* колёсные пары */}
    <circle cx="7.6" cy="17.4" r="1.5" vectorEffect="non-scaling-stroke"/>
    <circle cx="10.6" cy="17.4" r="1.5" vectorEffect="non-scaling-stroke"/>
    <circle cx="14.3" cy="17.4" r="1.5" vectorEffect="non-scaling-stroke"/>
    <circle cx="17" cy="17.4" r="1.5" vectorEffect="non-scaling-stroke"/>
    </g>
  </svg>
); }

// Trains — половины двух вагонов со сцепкой в центре.
function IcTrains(p) { return (
  <svg {...iconBase} {...p}>
    <g transform="translate(-2.4 -2.44) scale(1.2)">
    {/* правая половина левого вагона (открыта у левого края) */}
    <path d="M2 6.5H10.5V14.5H2" vectorEffect="non-scaling-stroke"/>
    {/* левая половина правого вагона (открыта у правого края) */}
    <path d="M22 6.5H13.5V14.5H22" vectorEffect="non-scaling-stroke"/>
    {/* окна */}
    <path d="M4.4 9.4h3.6" vectorEffect="non-scaling-stroke"/>
    <path d="M16 9.4h3.6" vectorEffect="non-scaling-stroke"/>
    {/* сцепка между вагонами */}
    <path d="M10.5 11.5h3" vectorEffect="non-scaling-stroke"/>
    {/* колёсные пары — по 2 на каждой половине */}
    <circle cx="4.3" cy="16.7" r="1.2" vectorEffect="non-scaling-stroke"/>
    <circle cx="8.3" cy="16.7" r="1.2" vectorEffect="non-scaling-stroke"/>
    <circle cx="15.7" cy="16.7" r="1.2" vectorEffect="non-scaling-stroke"/>
    <circle cx="19.7" cy="16.7" r="1.2" vectorEffect="non-scaling-stroke"/>
    </g>
  </svg>
); }
// Совместимость со старым именем.
const IcRails = IcTrains;

function IcFlash(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z"/>
  </svg>
); }

function IcDroplet(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"/>
  </svg>
); }

function IcGear(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="12" r="3"/>
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
  </svg>
); }

function IcPdf(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/>
    <path d="M14 3v5h5"/>
    <text x="12" y="17" fontSize="5" fontFamily="ui-monospace, monospace" fontWeight="700"
      textAnchor="middle" fill="currentColor" stroke="none">PDF</text>
  </svg>
); }

function IcCalendar(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="3" y="5" width="18" height="16" rx="2.5"/>
    <path d="M3 10h18M8 3v4M16 3v4"/>
    <circle cx="8" cy="14" r=".8" fill="currentColor"/>
    <circle cx="12" cy="14" r=".8" fill="currentColor"/>
    <circle cx="16" cy="14" r=".8" fill="currentColor"/>
  </svg>
); }

function IcClock(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M12 7v5l3.5 2"/>
  </svg>
); }

function IcRuble(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M8 20V5h5.5a4 4 0 0 1 0 8H6M6 17h9"/>
  </svg>
); }

function IcHome(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M3 11l9-7 9 7v9a2 2 0 0 1-2 2h-4v-6h-6v6H5a2 2 0 0 1-2-2z"/>
  </svg>
); }

// Следование пассажиром — человек, сидящий в кресле.
function IcPassenger(p) { return (
  <svg {...iconBase} {...p} strokeLinejoin="round" strokeLinecap="round">
    {/* голова */}
    <circle cx="13.6" cy="5.3" r="2.4"/>
    {/* корпус → бедро → голень одной линией */}
    <path d="M13.4 8 L12 15 L6.3 15.3 L3.9 20.8" strokeWidth="2"/>
    {/* спинка кресла — начинается на краю сиденья, параллельна спине */}
    <path d="M15 15.5 L16.8 6.5"/>
    {/* сиденье-пьедестал */}
    <path d="M6 15.5 H15 M10.5 15.5 V20.5 M8 20.5 H13"/>
  </svg>
); }

// «Два лица» — работа в паре (машинист + помощник).
function IcCrewPair(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="8" cy="8" r="2.8"/>
    <circle cx="16" cy="8" r="2.8"/>
    <path d="M3 19.5c0-2.8 2.2-5 5-5s5 2.2 5 5"/>
    <path d="M11 19.5c0-2.8 2.2-5 5-5s5 2.2 5 5"/>
  </svg>
); }
// Совместимость со старыми вызовами IcPassengers → теперь «два лица».
const IcPassengers = IcCrewPair;

// «В одно лицо» — один машинист.
function IcCrewSolo(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="8" r="3"/>
    <path d="M6 19.5c0-3.3 2.7-6 6-6s6 2.7 6 6"/>
  </svg>
); }

function IcPlus(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 5v14M5 12h14"/>
  </svg>
); }

function IcChevronRight(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M9 5l7 7-7 7"/>
  </svg>
); }

function IcChevronLeft(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M15 5l-7 7 7 7"/>
  </svg>
); }

function IcHeart(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 20s-7-4.5-9-9a5 5 0 0 1 9-3 5 5 0 0 1 9 3c-2 4.5-9 9-9 9z"/>
  </svg>
); }

function IcShare(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 3v13M8 7l4-4 4 4"/>
    <path d="M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6"/>
  </svg>
); }

function IcCopy(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="8" y="3" width="12" height="14" rx="2"/>
    <path d="M16 7H5a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1"/>
  </svg>
); }

function IcTrash(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M4 7h16M10 4h4a1 1 0 0 1 1 1v2H9V5a1 1 0 0 1 1-1z"/>
    <path d="M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/>
  </svg>
); }

function IcPencil(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M4 20h4L18.5 9.5a2.12 2.12 0 0 0-3-3L5 17v3z"/>
    <path d="M13.5 6.5l3 3"/>
  </svg>
); }

function IcSliders(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M4 6h8M16 6h4M4 12h4M12 12h8M4 18h12M20 18h0"/>
    <circle cx="14" cy="6" r="2"/>
    <circle cx="10" cy="12" r="2"/>
    <circle cx="18" cy="18" r="2"/>
  </svg>
); }

function IcSearch(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="11" cy="11" r="7"/>
    <path d="M20 20l-4-4"/>
  </svg>
); }

function IcProfile(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="8" r="4"/>
    <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
  </svg>
); }

function IcArmchair(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M5 11V9a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v2"/>
    <path d="M3 14a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4h-2v2H5v-2H3z"/>
  </svg>
); }

function IcCloud(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M7 18a4 4 0 0 1 0-8 5 5 0 0 1 9.5-1A4 4 0 0 1 17 18z"/>
    <path d="M10 13l2 2 3-3"/>
  </svg>
); }

function IcCloudOff(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M7 18a4 4 0 0 1-1.5-7.7 5 5 0 0 1 8.5-2.8 4 4 0 0 1 3 6.3"/>
    <path d="M3 3l18 18"/>
  </svg>
); }

function IcDocument(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M6 3h8l5 5v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"/>
    <path d="M13 3v6h6"/>
  </svg>
); }

function IcCheck(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M5 12l5 5L20 7"/>
  </svg>
); }

function IcLogIn(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
    <path d="M10 17l5-5-5-5"/>
    <path d="M15 12H3"/>
  </svg>
); }

function IcLogOut(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M9 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h4"/>
    <path d="M16 17l5-5-5-5"/>
    <path d="M21 12H9"/>
  </svg>
); }

function IcMapPin(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 22s8-7 8-13a8 8 0 0 0-16 0c0 6 8 13 8 13z"/>
    <circle cx="12" cy="9" r="3"/>
  </svg>
); }

function IcWorkClock(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="13" r="8"/>
    <path d="M12 9v4l2.5 2"/>
    <path d="M9 3h6"/>
  </svg>
); }

function IcBarrier(p) { return (
  <svg {...iconBase} {...p}>
    {/* Stand on the left */}
    <path d="M5 21V8"/>
    <circle cx="5" cy="6" r="1.6" fill="currentColor" stroke="none"/>
    {/* Boom (horizontal bar) */}
    <path d="M5 12h16"/>
    {/* Stripes on the boom */}
    <path d="M9 12v-1.6M13 12v-1.6M17 12v-1.6"/>
    {/* Ground */}
    <path d="M3 21h18"/>
  </svg>
); }

function IcArrowDown(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 5v14"/>
    <path d="M6 13l6 6 6-6"/>
  </svg>
); }

function IcArrowUp(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 19V5"/>
    <path d="M6 11l6-6 6 6"/>
  </svg>
); }

// ─── Атрибуты маршрута (значки в строке поездки) ───

// Поезда повышенной длины — линейка/рулетка.
function IcRuler(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="2.5" y="9" width="19" height="6" rx="1.4"/>
    <path d="M6.5 9v2.6M10 9v3.4M13.5 9v2.6M17 9v3.4"/>
  </svg>
); }

// Поезда повышенной массы — гиря.
function IcWeight(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M8.5 9.5a3.5 3.5 0 1 1 7 0"/>
    <path d="M7.4 9.5h9.2c.5 0 .9.3 1 .8l1.4 8.2a1 1 0 0 1-1 1.2H6c-.62 0-1.1-.55-1-1.2l1.4-8.2c.1-.5.5-.8 1-.8z"/>
  </svg>
); }

// Удлинённое плечо обслуживания — петля пути.
function IcShoulder(p) { return (
  <svg {...iconBase} {...p} strokeLinecap="round">
    {/* серпантинный путь: дот → арка ∩ → впадина ∪ → дот */}
    <path d="M6 18V8a3 3 0 0 1 6 0v8a3 3 0 0 0 6 0V6" fill="none"/>
    <circle cx="6" cy="18.4" r="1.7" fill="currentColor" stroke="none"/>
    <circle cx="18" cy="5.6" r="1.7" fill="currentColor" stroke="none"/>
  </svg>
); }

// Работа свыше 12-ти часов — медаль со звездой (красно-жёлтая).
function IcMedal(p) { return (
  <svg {...iconBase} {...p}>
    {/* лента — красная */}
    <path d="M9 3l2.2 5.2M15 3l-2.2 5.2" stroke="#C4302B"/>
    {/* медаль — жёлтая */}
    <circle cx="12" cy="15" r="5.4" stroke="#E8A317"/>
    {/* звезда — жёлтая заливка */}
    <path d="M12 12.2l.95 1.95 2.15.3-1.55 1.5.37 2.13L12 16.6l-1.92 1 .37-2.12-1.55-1.5 2.15-.3z" fill="#E8A317" stroke="none"/>
  </svg>
); }

// Работа в праздничный день — календарь с красной отметкой.
function IcHoliday(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="3.5" y="5" width="17" height="15.5" rx="2.4" stroke="#C4302B"/>
    <path d="M3.5 9.5h17M8 3v4M16 3v4" stroke="#C4302B"/>
    <path d="M12 12.2l.7 1.45 1.6.22-1.15 1.12.27 1.6L12 16.85l-1.42.76.27-1.6-1.15-1.12 1.6-.22z" fill="#C4302B" stroke="none"/>
  </svg>
); }

// Перерыв в работе — пауза.
function IcBreak(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="7.5" y="5.5" width="3.4" height="13" rx="1.2" fill="currentColor" stroke="none"/>
    <rect x="13.1" y="5.5" width="3.4" height="13" rx="1.2" fill="currentColor" stroke="none"/>
  </svg>
); }

// Толкач — подталкивающий локомотив (стрелка с бортом).
function IcPusher(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M20 6v12"/>
    <path d="M16 12H4M9 7l-5 5 5 5"/>
  </svg>
); }

// Двойная тяга — два шеврона.
function IcDoubleTraction(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M12 6l-6 6 6 6M19 6l-6 6 6 6"/>
  </svg>
); }

// Сдвоенный поезд — два сцепленных вагона.
function IcCoupledTrain(p) { return (
  <svg {...iconBase} {...p}>
    <rect x="2.5" y="8.5" width="8" height="7" rx="1.4"/>
    <rect x="13.5" y="8.5" width="8" height="7" rx="1.4"/>
    <path d="M10.5 12h3"/>
  </svg>
); }

// Статус синхронизации маршрута — круговые стрелки.
function IcSync(p) { return (
  <svg {...iconBase} {...p}>
    <path d="M20 11a8 8 0 0 0-13.6-4.5L3.5 9"/>
    <path d="M4 13a8 8 0 0 0 13.6 4.5L20.5 15"/>
    <path d="M3.5 4.5V9H8M20.5 19.5V15H16"/>
  </svg>
); }

// Инфо — обозначения / справка.
function IcInfo(p) { return (
  <svg {...iconBase} {...p}>
    <circle cx="12" cy="12" r="9"/>
    <path d="M12 11v5"/>
    <circle cx="12" cy="7.7" r="0.6" fill="currentColor" stroke="none"/>
  </svg>
); }

function IcLogo({ size = 32, color = 'currentColor', bg, variant }) {
  // "Машинист" monogram — монолитная «М», Inter Black.
  // Два цвета: знак (color) + опциональная подложка (bg).
  // variant сохранён в сигнатуре для совместимости со старыми вызовами, но не влияет.
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none">
      {bg && <rect width="32" height="32" rx="7" fill={bg}/>}
      <text
        x="16" y="16.5"
        textAnchor="middle"
        dominantBaseline="central"
        fontFamily="Inter, system-ui, sans-serif"
        fontWeight="900"
        fontSize="29"
        letterSpacing="-1.2"
        fill={color}
      >М</text>
    </svg>
  );
}

Object.assign(window, {
  IcLocomotive, IcTrains, IcRails, IcCalendar, IcClock, IcRuble, IcHome,
  IcPassengers, IcPassenger, IcCrewPair, IcCrewSolo,
  IcPlus, IcChevronRight, IcChevronLeft, IcHeart, IcShare,
  IcCopy, IcTrash, IcSliders, IcPencil, IcSearch, IcProfile, IcArmchair, IcCloud,
  IcCloudOff, IcDocument, IcCheck, IcLogo, IcPdf,
  IcFlash, IcDroplet, IcGear,
  IcLogIn, IcLogOut, IcMapPin, IcWorkClock, IcBarrier, IcArrowDown, IcArrowUp,
  IcRuler, IcWeight, IcShoulder, IcMedal, IcHoliday, IcBreak,
  IcPusher, IcDoubleTraction, IcCoupledTrain, IcSync, IcInfo,
});
