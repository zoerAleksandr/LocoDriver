# PWA расчётного листа

PWA использует тот же Kotlin Multiplatform `SalaryCalculationHelper`, что Android
и iOS. Формулы в JavaScript не дублируются.

```bash
./gradlew preparePwa
cd pwa
npm test
npm run serve
```

Откройте `http://localhost:4173`. Раздел «Данные» принимает JSON с
`userSettings`, `salarySetting`, `routes`, `effectiveNormaHours` и
`annualOvertimeBeforePeriod`. Данные остаются в `localStorage`. Кнопка
«Сформировать PDF» открывает системный диалог печати с A4-разметкой; в браузере
нужно выбрать «Сохранить как PDF».
