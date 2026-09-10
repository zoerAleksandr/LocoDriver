import test from "node:test";
import assert from "node:assert/strict";
import { codeSort, filterCatalog, hours, money, normalizeSearch, percent, safeJson } from "../lib.js";

const catalog = [
  { code: "152P", shortName: "РучнойВвод", description: "Тяжёлый поезд", type: "ACCRUAL" },
  { code: "004L", shortName: "Тариф", description: "Оплата по тарифу", type: "ACCRUAL" },
  { code: "883A", shortName: "НДФЛ", description: "Налог на доходы", type: "DEDUCTION" },
];

test("поиск учитывает код, служебное имя, описание и тип", () => {
  assert.equal(filterCatalog(catalog, "тариф").length, 1);
  assert.equal(filterCatalog(catalog, "налог доходы")[0].code, "883A");
  assert.equal(filterCatalog(catalog, "удержание")[0].code, "883A");
});

test("визуально одинаковые кириллические буквы нормализуются", () => {
  assert.equal(normalizeSearch("152Р"), "152P");
  assert.equal(filterCatalog(catalog, "152Р")[0].code, "152P");
});

test("сортировка: начисления по числу, затем удержания", () => {
  assert.deepEqual([...catalog].sort(codeSort).map(x => x.code), ["004L", "152P", "883A"]);
});

test("форматы часов процентов и денег русские", () => {
  assert.match(hours(5_400_000), /1,50/);
  assert.match(percent(13), /13,0/);
  assert.match(money(1234.5), /1\s234,50/);
});

test("импорт принимает только JSON-объект", () => {
  assert.deepEqual(safeJson('{"routes":[]}'), { routes: [] });
  assert.throws(() => safeJson("[]"), /JSON-объект/);
});
