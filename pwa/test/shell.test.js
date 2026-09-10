import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");

test("оболочка содержит все разделы расчётного листа", async () => {
  const html = await readFile(resolve(root, "index.html"), "utf8");
  for (const text of ["Расчётный лист", "Расшифровать код", "Вид выплаты", "Вид удержания", "Сформировать PDF"]) {
    assert.match(html, new RegExp(text));
  }
});

test("manifest и service worker образуют устанавливаемую оболочку", async () => {
  const manifest = JSON.parse(await readFile(resolve(root, "manifest.webmanifest"), "utf8"));
  assert.equal(manifest.display, "standalone");
  assert.ok(manifest.icons.length > 0);
  await access(resolve(root, manifest.icons[0].src));
  assert.match(await readFile(resolve(root, "sw.js"), "utf8"), /caches\.open/);
});

test("preparePwa поставляет общий расчётчик и его runtime", async () => {
  for (const file of ["LocoDriver-domain.js", "LocoDriver-core.js", "Kotlin-DateTime-library-kotlinx-datetime.js", "js-joda.min.js"]) {
    await access(resolve(root, "vendor", file));
  }
});

test("собранный browser runtime экспортирует мост и полный справочник", async () => {
  const context = vm.createContext({ console, setTimeout, clearTimeout, Promise, Intl, Date, Math });
  const files = [
    "js-joda.min.js", "kotlin-kotlin-stdlib.js", "kotlin-kotlinx-atomicfu-runtime-js-ir.js",
    "88b0986a7186d029-atomicfu-js-ir.js", "kotlinx-serialization-kotlinx-serialization-core.js",
    "kotlinx-serialization-kotlinx-serialization-json.js", "kotlinx.coroutines-kotlinx-coroutines-core-js-ir.js",
    "Kotlin-DateTime-library-kotlinx-datetime.js", "LocoDriver-core.js", "LocoDriver-domain.js",
  ];
  for (const file of files) {
    vm.runInContext(await readFile(resolve(root, "vendor", file), "utf8"), context, { filename: file });
    if (file === "js-joda.min.js") context["@js-joda/core"] = context.JSJoda;
  }
  const bridge = context["LocoDriver:domain"].com.z_company.domain.salary.PwaSalaryBridge;
  assert.equal(JSON.parse(bridge.catalogJson()).length, 286);
  assert.equal(typeof bridge.calculate, "function");
});
