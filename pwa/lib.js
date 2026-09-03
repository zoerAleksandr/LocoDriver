const lookalikes = { "А": "A", "В": "B", "Е": "E", "К": "K", "М": "M", "Н": "H", "О": "O", "Р": "P", "С": "C", "Т": "T", "Х": "X", "У": "Y" };

export function normalizeSearch(value) {
  return String(value ?? "").trim().toUpperCase().split("").map(char => lookalikes[char] ?? char).join("").replace(/Ё/g, "Е");
}

export function filterCatalog(items, query) {
  const terms = normalizeSearch(query).split(/\s+/).filter(Boolean);
  if (!terms.length) return items;
  return items.filter(item => {
    const haystack = normalizeSearch([item.code, item.shortName, item.description, item.type === "ACCRUAL" ? "начисление" : "удержание"].join(" "));
    return terms.every(term => haystack.includes(term));
  });
}

export function codeSort(a, b) {
  const type = (a.type === "ACCRUAL" ? 0 : 1) - (b.type === "ACCRUAL" ? 0 : 1);
  if (type) return type;
  const split = code => [Number((code.match(/^\d+/) || ["999999"])[0]), code];
  const [an, ac] = split(a.code);
  const [bn, bc] = split(b.code);
  return an - bn || ac.localeCompare(bc, "ru", { numeric: true });
}

export function money(value, currency = "₽") {
  return `${new Intl.NumberFormat("ru-RU", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value) || 0)} ${currency}`;
}

export function hours(milliseconds) {
  if (milliseconds == null) return "—";
  return new Intl.NumberFormat("ru-RU", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(milliseconds / 3600000);
}

export function percent(value) {
  if (value == null) return "—";
  return new Intl.NumberFormat("ru-RU", { minimumFractionDigits: 1, maximumFractionDigits: 2 }).format(value);
}

export function safeJson(text) {
  const parsed = JSON.parse(text);
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("Ожидался JSON-объект");
  return parsed;
}
