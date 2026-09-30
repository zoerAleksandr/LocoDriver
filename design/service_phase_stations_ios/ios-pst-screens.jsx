// Реестр экранов iOS — один источник для холста, standalone и скриншотов.
//   PNG: screenshots/ios-{slug}-{light|dark}.png — номера и slug совпадают с Android-пакетом.
const IpEd = (p) => (t, dark) => <IpEditorSheet t={t} dark={dark} {...p}/>;
const IpBase = IpEd({ mode: 'edit', state: 'short', scroll: 190 });
const IpSt = (v) => (t) => <IpStationSheet t={t} variant={v}/>;
const IpAu = (C, p = {}) => (t) => <C t={t} {...p}/>;
const IpDlg = () => (t) => <IpDeleteActionSheet t={t}/>;
const IG1 = '1 · Настройки → Плечи', IG2 = '2 · Редактор плеча', IG3 = '3 · Шторка станции и карта', IG4 = '4 · Автоподбор', IG5 = '5 · Ошибки автоподбора';
const IP_SCREENS = [
  [IG1, '01-plechi-spisok', 'Список плеч', []],
  [IG2, '02-redaktor-pusto-avtopodbor-neaktiven', 'Новое · пусто · автоподбор неактивен', [IpEd({ mode: 'new', state: 'empty-disabled' })]],
  [IG2, '03-redaktor-pusto', 'Новое · пусто · обе станции заданы', [IpEd({ mode: 'new', state: 'empty', scroll: 60 })]],
  [IG2, '04-redaktor-korotkiy-spisok', 'Заполнено · короткий список', [IpEd({ state: 'short', scroll: 190 })]],
  [IG2, '05-redaktor-dlinnyy-spisok-nachalo', '38 станций · начало', [IpEd({ state: 'long', scroll: 0 })]],
  [IG2, '06-redaktor-dlinnyy-spisok-prokrutka', '38 станций · прокручено', [IpEd({ state: 'long', scroll: 1100 })]],
  [IG2, '07-redaktor-izmenenie-poryadka', 'Режим «Изменить»', [IpEd({ state: 'reorder', scroll: 196 })]],
  [IG2, '08-redaktor-posle-avtopodbora', 'После автоподбора · тост', [IpEd({ state: 'fresh', scroll: 190, toast: 'Добавлено 5 станций · 3 названия — в справочник' })]],
  [IG3, '09-stanciya-iz-avtopodbora', 'Из автоподбора · координаты есть', [IpBase, IpSt('osm')]],
  [IG3, '10-stanciya-net-koordinat', 'Вручную · нет координат', [IpBase, IpSt('manual')]],
  [IG3, '11-stanciya-poisk-koordinat', 'Поиск координат', [IpBase, IpSt('searching')]],
  [IG3, '12-stanciya-koordinaty-nashlis', 'Координаты нашлись', [IpBase, IpSt('found')]],
  [IG3, '13-stanciya-koordinaty-ne-nashlis', 'Координаты не нашлись', [IpBase, IpSt('notfound')]],
  [IG3, '14-stanciya-novaya-avtodopolnenie', 'Новая · автодополнение', [IpBase, IpSt('new')]],
  [IG3, '15-stanciya-udalenie', 'Удаление · action sheet', [IpBase, IpSt('manual'), IpDlg()]],
  [IG3, '16-karta-vybor-tochki', 'Указать на карте', 'map'],
  [IG4, '17-avtopodbor-zagruzka', '5.1 · Загрузка', [IpBase, IpAu(IpAutoLoading)]],
  [IG4, '18-avtopodbor-utochnenie-stancii', '5.2 · Уточнение станции', [IpBase, IpAu(IpAutoCandidate)]],
  [IG4, '19-avtopodbor-predprosmotr', '5.3 · О.П. скрыты · расхождение длины', [IpBase, IpAu(IpAutoPreview, { warn: 'mismatch' })]],
  [IG4, '20-avtopodbor-predprosmotr-s-op', '5.3 · С О.П.', [IpBase, IpAu(IpAutoPreview, { showOp: true, warn: null, scroll: 60 })]],
  [IG4, '21-avtopodbor-povtornyy-podbor', '5.3 · Повторный подбор', [IpBase, IpAu(IpAutoPreview, { rerun: true, warn: null })]],
  [IG4, '22-avtopodbor-rasstoyanie-ne-ukazano', '5.3 · Расстояние не введено', [IpBase, IpAu(IpAutoPreview, { warn: 'nodist' })]],
  [IG5, '23-oshibka-net-interneta', 'Нет интернета', [IpBase, IpAu(IpAutoError, { kind: 'offline' })]],
  [IG5, '24-oshibka-stanciya-ne-naydena', 'Станция не найдена', [IpBase, IpAu(IpAutoError, { kind: 'notfound' })]],
  [IG5, '25-oshibka-put-ne-nayden', 'Путь не найден', [IpBase, IpAu(IpAutoError, { kind: 'nopath' })]],
  [IG5, '26-oshibka-servis-nedostupen', 'Сервис недоступен', [IpBase, IpAu(IpAutoError, { kind: 'timeout' })]],
].map(([group, slug, label, layers]) => ({ group, slug, label, layers }));

function IpScreen({ s, dark }) {
  if (s.layers === 'map') return <IpMapPicker dark={dark}/>;
  return <IpShouldersScreen dark={dark}>{(t) => s.layers.map((L, i) => <React.Fragment key={i}>{L(t, dark)}</React.Fragment>)}</IpShouldersScreen>;
}
Object.assign(window, { IP_SCREENS, IpScreen });
