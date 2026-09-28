// stats-canvas.jsx — раскладка артбордов раздела «Статистика» для холста.
// Один генератор statsArtboards({platform, dark}) собирает полный набор
// экранов под платформу и тему. Возвращает МАССИВ <DCArtboard> — React.Children
// его расплющивает, поэтому вставляется прямо внутрь <DCSection>.
//
// Экраны (и высоты артбордов) совпадают с прежним standalone-файлом
// «Статистика». Высоты подобраны так, чтобы контент помещался целиком.

function statsArtboards({ platform = 'ios', dark = false }){
  const pfx = (platform === 'android' ? 'and' : 'ios') + '-stats';
  const sfx = dark ? '-dark' : '-light';
  const th  = dark ? ' · dark' : ' · light';
  const k = (id) => `${pfx}-${id}${sfx}`;
  // Android-навигация чуть выше iOS — добавляем небольшой запас по высоте.
  const pad = platform === 'android' ? 24 : 0;
  const Hmonth = 1520 + pad, Hyear = 1700 + pad, Hdetail = 1080 + pad, Hhist = 1160 + pad;

  return [
    <DCArtboard key={k('month')} id={k('month')} label={`Месяц · сетка плашек${th}`} width={390} height={Hmonth}>
      <IOSStatsB platform={platform} dark={dark} height={Hmonth}/>
    </DCArtboard>,
    <DCArtboard key={k('month-compare')} id={k('month-compare')} label={`Месяц · шторка «с чем сравнивать»${th}`} width={390} height={Hmonth}>
      <IOSStatsB platform={platform} dark={dark} height={Hmonth} initialSheet="compare"/>
    </DCArtboard>,
    <DCArtboard key={k('month-pick')} id={k('month-pick')} label={`Месяц · экран выбора месяца${th}`} width={390} height={Hmonth}>
      <IOSStatsB platform={platform} dark={dark} height={Hmonth} initialSheet="monthpick" initialCmp="custom"/>
    </DCArtboard>,
    <DCArtboard key={k('month-none')} id={k('month-none')} label={`Месяц · без сравнения${th}`} width={390} height={Hmonth}>
      <IOSStatsB platform={platform} dark={dark} height={Hmonth} initialCmp="none"/>
    </DCArtboard>,

    <DCArtboard key={k('year')} id={k('year')} label={`Год · плашки метрик${th}`} width={390} height={Hyear}>
      <IOSStatsYear platform={platform} dark={dark} height={Hyear}/>
    </DCArtboard>,
    <DCArtboard key={k('year-compare')} id={k('year-compare')} label={`Год · шторка «с чем сравнивать»${th}`} width={390} height={Hyear}>
      <IOSStatsYear platform={platform} dark={dark} height={Hyear} initialSheet="compare"/>
    </DCArtboard>,
    <DCArtboard key={k('year-pick')} id={k('year-pick')} label={`Год · экран выбора года${th}`} width={390} height={Hyear}>
      <IOSStatsYear platform={platform} dark={dark} height={Hyear} initialSheet="yearpick" initialCmp="custom"/>
    </DCArtboard>,
    <DCArtboard key={k('year-none')} id={k('year-none')} label={`Год · без сравнения${th}`} width={390} height={Hyear}>
      <IOSStatsYear platform={platform} dark={dark} height={Hyear} initialCmp="none"/>
    </DCArtboard>,

    <DCArtboard key={k('history')} id={k('history')} label={`История · отработано${th}`} width={390} height={Hhist}>
      <IOSStatsHistory platform={platform} dark={dark} height="auto" metric="worked"/>
    </DCArtboard>,
    <DCArtboard key={k('history-dist')} id={k('history-dist')} label={`История · выбран «Путь»${th}`} width={390} height={Hhist}>
      <IOSStatsHistory platform={platform} dark={dark} height="auto" metric="distance"/>
    </DCArtboard>,
    <DCArtboard key={k('history-earn')} id={k('history-earn')} label={`История · выбран «Заработок»${th}`} width={390} height={Hhist}>
      <IOSStatsHistory platform={platform} dark={dark} height="auto" metric="earnings"/>
    </DCArtboard>,
    <DCArtboard key={k('history-tkm')} id={k('history-tkm')} label={`История · выбран «Грузооборот»${th}`} width={390} height={Hhist}>
      <IOSStatsHistory platform={platform} dark={dark} height="auto" metric="tkm"/>
    </DCArtboard>,

    <DCArtboard key={k('detail')} id={k('detail')} label={`Детализация · выбран месяц${th}`} width={390} height={Hdetail}>
      <IOSStatsDetail platform={platform} dark={dark} height={Hdetail} metric="distance"/>
    </DCArtboard>,
    <DCArtboard key={k('detail-year')} id={k('detail-year')} label={`Детализация · обзор за год${th}`} width={390} height={Hdetail}>
      <IOSStatsDetailYear platform={platform} dark={dark} height={Hdetail} metric="distance"/>
    </DCArtboard>,

    <DCArtboard key={k('empty')} id={k('empty')} label={`Пустое состояние · нет данных${th}`} width={390} height={844 + pad}>
      <IOSStatsEmpty platform={platform} dark={dark} height={844 + pad}/>
    </DCArtboard>,
  ];
}

Object.assign(window, { statsArtboards });
