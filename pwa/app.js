import { codeSort, filterCatalog, hours, money, percent, safeJson } from "./lib.js";

const bridge = globalThis["LocoDriver:domain"]?.com?.z_company?.domain?.salary?.PwaSalaryBridge;
const $ = selector => document.querySelector(selector);
const state = { catalog: [], result: null, currency: "₽" };
const monthNames = ["Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"];

function showView(id) {
  document.querySelectorAll(".view").forEach(view => view.classList.toggle("active", view.id === id));
  document.querySelectorAll(".tab").forEach(tab => tab.classList.toggle("active", tab.dataset.view === id));
}

function renderCatalog() {
  const items = filterCatalog(state.catalog, $("#code-query").value);
  $("#found").textContent = `Найдено: ${items.length}`;
  $("#catalog").replaceChildren(...items.map(item => {
    const card = document.createElement("article"); card.className = "code-card";
    const header = document.createElement("header");
    const code = document.createElement("strong"); code.textContent = item.code;
    const chip = document.createElement("span"); chip.className = "chip"; chip.textContent = item.type === "ACCRUAL" ? "Начисление" : "Удержание";
    const name = document.createElement("h3"); name.textContent = item.shortName;
    const description = document.createElement("p"); description.textContent = item.description;
    header.append(code, chip); card.append(header, name, description); return card;
  }));
}

function tableRows(lines, deduction = false) {
  return lines.map(line => {
    const tr = document.createElement("tr");
    const values = deduction
      ? [line.code, line.plainName, percent(line.percent), money(line.amount, state.currency)]
      : [line.code, line.plainName, hours(line.hoursMillis), percent(line.percent), money(line.amount, state.currency)];
    values.forEach(value => { const td = document.createElement("td"); td.textContent = value; tr.append(td); });
    return tr;
  });
}

function renderStatement(result) {
  state.result = result;
  const month = monthNames[result.month] ?? `Месяц ${result.month + 1}`;
  $("#period").textContent = `Расчётный листок за ${month.toLowerCase()} ${result.year} года`;
  $("#tariff").textContent = money(result.tariffRate, state.currency);
  $("#norma").textContent = result.normaHours || "—";
  $("#worked").textContent = hours(result.totalWorkedMillis);
  $("#accruals").replaceChildren(...tableRows(result.accruals));
  $("#deductions").replaceChildren(...tableRows(result.deductions, true));
  $("#total-accrued").textContent = money(result.totalAccrued, state.currency);
  $("#total-deducted").textContent = money(result.totalDeducted, state.currency);
  $("#payable").textContent = money(result.payable, state.currency);
  $("#sheet").hidden = false; $("#empty").hidden = true; $("#print").disabled = false;
}

async function calculate() {
  try {
    if (!bridge) throw new Error("Общий модуль расчёта не загружен. Выполните ./gradlew preparePwa.");
    const request = safeJson($("#payload").value);
    localStorage.setItem("locodriver.salary.request", JSON.stringify(request));
    const result = JSON.parse(await bridge.calculate(JSON.stringify(request)));
    renderStatement(result); $("#status").textContent = "Расчёт выполнен и сохранён в этом браузере."; showView("statement");
  } catch (error) { $("#status").textContent = `Ошибка: ${error.message}`; }
}

document.querySelectorAll(".tab").forEach(tab => tab.addEventListener("click", () => showView(tab.dataset.view)));
$("#code-query").addEventListener("input", renderCatalog);
$("#calculate").addEventListener("click", calculate);
$("#print").addEventListener("click", () => window.print());
$("#file").addEventListener("change", async event => { const [file] = event.target.files; if (file) $("#payload").value = await file.text(); });
$("#clear").addEventListener("click", () => { localStorage.removeItem("locodriver.salary.request"); $("#payload").value = ""; $("#sheet").hidden = true; $("#empty").hidden = false; $("#print").disabled = true; $("#status").textContent = "Локальные данные удалены."; });

if (bridge) { state.catalog = JSON.parse(bridge.catalogJson()).sort(codeSort); renderCatalog(); }
else { $("#found").textContent = "Справочник не загружен"; }
const saved = localStorage.getItem("locodriver.salary.request");
if (saved) { $("#payload").value = JSON.stringify(JSON.parse(saved), null, 2); calculate(); }
if ("serviceWorker" in navigator) navigator.serviceWorker.register("./sw.js");
