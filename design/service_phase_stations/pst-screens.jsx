// Реестр всех экранов — один источник для холста, standalone и скриншотов.
//   slug = имя PNG в screenshots/: {slug}-{light|dark}.png
const PstEd = (p) => (t, dark) => <PstEditorSheet t={t} dark={dark} {...p}/>;
const PstBase = PstEd({ mode: 'edit', state: 'short', scroll: 190 });
const PstSt = (v) => (t) => <PstStationSheet t={t} variant={v}/>;
const PstAu = (C, p = {}) => (t) => <C t={t} {...p}/>;
const PstDlg = () => (t) => (
  <div style={{ position: 'absolute', inset: 0, zIndex: 95 }}>
    <ARouteDeleteDialog t={t} title="Удалить станцию?" message="«Кемка» будет убрана из плеча. Название останется в справочнике станций."/>
  </div>
);
const G1 = '1 · Настройки → Плечи', G2 = '2 · Редактор плеча', G3 = '3 · Шторка станции и карта', G4 = '4 · Автоподбор', G5 = '5 · Ошибки автоподбора';
const PST_SCREENS = [
  [G1, '01-plechi-spisok', 'Список плеч', []],
  [G2, '02-redaktor-pusto-avtopodbor-neaktiven', 'Новое · пусто · автоподбор неактивен', [PstEd({ mode: 'new', state: 'empty-disabled' })]],
  [G2, '03-redaktor-pusto', 'Новое · пусто · обе станции заданы', [PstEd({ mode: 'new', state: 'empty', scroll: 60 })]],
  [G2, '04-redaktor-korotkiy-spisok', 'Заполнено · короткий список', [PstEd({ state: 'short', scroll: 190 })]],
  [G2, '05-redaktor-dlinnyy-spisok-nachalo', '38 станций · начало', [PstEd({ state: 'long', scroll: 190 })]],
  [G2, '06-redaktor-dlinnyy-spisok-prokrutka', '38 станций · прокручено', [PstEd({ state: 'long', scroll: 1100 })]],
  [G2, '07-redaktor-izmenenie-poryadka', 'Режим «Изменить»', [PstEd({ state: 'reorder', scroll: 250 })]],
  [G2, '08-redaktor-posle-avtopodbora', 'После автоподбора · снекбар', [PstEd({ state: 'fresh', scroll: 190, snackbar: 'Добавлено 5 станций · 3 названия — в справочник' })]],
  [G3, '09-stanciya-iz-avtopodbora', 'Из автоподбора · координаты есть', [PstBase, PstSt('osm')]],
  [G3, '10-stanciya-net-koordinat', 'Вручную · нет координат', [PstBase, PstSt('manual')]],
  [G3, '11-stanciya-poisk-koordinat', 'Поиск координат', [PstBase, PstSt('searching')]],
  [G3, '12-stanciya-koordinaty-nashlis', 'Координаты нашлись', [PstBase, PstSt('found')]],
  [G3, '13-stanciya-koordinaty-ne-nashlis', 'Координаты не нашлись', [PstBase, PstSt('notfound')]],
  [G3, '14-stanciya-novaya-avtodopolnenie', 'Новая · автодополнение', [PstBase, PstSt('new')]],
  [G3, '15-stanciya-udalenie', 'Удаление · диалог', [PstBase, PstSt('manual'), PstDlg()]],
  [G3, '16-karta-vybor-tochki', 'Указать на карте', 'map'],
  [G4, '17-avtopodbor-zagruzka', '4.1 · Загрузка', [PstBase, PstAu(PstAutoLoading)]],
  [G4, '18-avtopodbor-utochnenie-stancii', '4.2 · Уточнение станции', [PstBase, PstAu(PstAutoCandidate)]],
  [G4, '19-avtopodbor-predprosmotr', '4.3 · О.П. скрыты · расхождение длины', [PstBase, PstAu(PstAutoPreview, { warn: 'mismatch' })]],
  [G4, '20-avtopodbor-predprosmotr-s-op', '4.3 · С О.П.', [PstBase, PstAu(PstAutoPreview, { showOp: true, warn: null, scroll: 60 })]],
  [G4, '21-avtopodbor-povtornyy-podbor', '4.3 · Повторный подбор', [PstBase, PstAu(PstAutoPreview, { rerun: true, warn: null })]],
  [G4, '22-avtopodbor-rasstoyanie-ne-ukazano', '4.3 · Расстояние не введено', [PstBase, PstAu(PstAutoPreview, { warn: 'nodist' })]],
  [G5, '23-oshibka-net-interneta', 'Нет интернета', [PstBase, PstAu(PstAutoError, { kind: 'offline' })]],
  [G5, '24-oshibka-stanciya-ne-naydena', 'Станция не найдена', [PstBase, PstAu(PstAutoError, { kind: 'notfound' })]],
  [G5, '25-oshibka-put-ne-nayden', 'Путь не найден', [PstBase, PstAu(PstAutoError, { kind: 'nopath' })]],
  [G5, '26-oshibka-servis-nedostupen', 'Сервис недоступен', [PstBase, PstAu(PstAutoError, { kind: 'timeout' })]],
].map(([group, slug, label, layers]) => ({ group, slug, label, layers }));

function PstScreen({ s, dark }) {
  if (s.layers === 'map') return <PstMapPicker dark={dark}/>;
  return <PstShouldersScreen dark={dark}>{(t) => s.layers.map((L, i) => <React.Fragment key={i}>{L(t, dark)}</React.Fragment>)}</PstShouldersScreen>;
}
Object.assign(window, { PST_SCREENS, PstScreen });
