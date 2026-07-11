# Памятка: «нет интернета» на Android-эмуляторе (значок «!» у WiFi)

Периодически на эмуляторе пропадает интернет: у иконки WiFi появляется «!»,
в приложении Профиль/синхронизация падают с «Нет интернета», в logcat —
`UnknownHostException: Unable to resolve host ...`.

Ниже — как быстро починить и почему это происходит.

---

## TL;DR

```bash
# Автоматически (скрипт лежит в домашней папке):
~/fix-emulator-dns.sh          # быстрый фикс + проверка
~/fix-emulator-dns.sh --full   # сразу полный перезапуск с -dns-server 8.8.8.8
```

Или вручную — шаг 1, при неудаче шаг 2 (ниже).

---

## Шаг 1 — быстрый фикс (помогает чаще всего)

Переключить эмулятор с фейкового WiFi на cellular (у него реальный NAT):

```bash
adb -s emulator-5554 shell svc wifi disable
adb -s emulator-5554 shell svc data enable
```

Подожди ~7 секунд. В статус-баре «!» у WiFi сменится на «5G/LTE».

## Шаг 2 — если DNS всё ещё мёртв

Признак: TCP по IP работает, а имена не резолвятся (`UnknownHostException`).
Перезапуск эмулятора с явным DNS (**данные приложения сохраняются**):

```bash
adb -s emulator-5554 emu kill

~/Library/Android/sdk/emulator/emulator \
  -avd Pixel_9_Pro_XL -dns-server 8.8.8.8 -qt-hide-window \
  -netdelay none -netspeed full &

# дождаться загрузки:
adb wait-for-device
# (подождать, пока getprop sys.boot_completed вернёт 1)

# затем снова переключить на cellular:
adb -s emulator-5554 shell svc wifi disable
adb -s emulator-5554 shell svc data enable
```

> Если AVD называется иначе — список: `~/Library/Android/sdk/emulator/emulator -list-avds`

## Проверка, что заработало

```bash
adb -s emulator-5554 shell "echo -n '' | toybox nc -w 6 google.com 443 && echo OK || echo FAIL"
```

`OK` = DNS и интернет живые. `FAIL`/`Timeout` = ещё нет.

Дополнительная диагностика:

```bash
# активная сеть по умолчанию (100/101 = какая сеть выбрана)
adb -s emulator-5554 shell dumpsys connectivity | grep -i "Active default network"

# TCP до боевого API по IP (не зависит от DNS)
adb -s emulator-5554 shell "echo -n '' | toybox nc -w 6 87.228.110.32 8766 && echo SRV_OK || echo SRV_FAIL"
```

---

## Почему это происходит

1. **Фейковый WiFi.** Активная сеть эмулятора — мок `AndroidWifi`
   (`192.168.1.x`, шлюз `192.168.1.1`) **без реального выхода в интернет**.
   Реальный интернет только у cellular (`eth0`, NAT-шлюз `10.0.2.2`).
   → отсюда шаг 1.

2. **Пустой resolv.conf.** DNS-прокси эмулятора (`10.0.2.3`) читает
   `/etc/resolv.conf` на Mac, а он **пустой**. Рабочие DNS (`8.8.8.8`,
   `8.8.4.4`) лежат только в scoped-резолверах macOS (`scutil --dns`),
   которыми пользуется системный резолвер, но не qemu-slirp.
   → поэтому имена не резолвятся, помогает `-dns-server 8.8.8.8` (шаг 2).

   Проверить причину на хосте:
   ```bash
   cat /etc/resolv.conf              # пусто -> вот она, причина
   scutil --dns | grep nameserver    # тут рабочие DNS есть
   dig @8.8.8.8 google.com +short    # работает
   dig google.com +short             # НЕ работает (берёт пустой resolv.conf)
   ```

---

## Лечение корня (чтобы вообще не повторялось)

Пустой `/etc/resolv.conf` обычно остаётся после VPN или смены сети.
Чтобы и хост, и эмулятор были стабильны — пропиши DNS вручную в системе:

**Системные настройки → Сеть → (активный адаптер Wi-Fi/Ethernet) →
Подробнее… → DNS →** добавить `8.8.8.8` и `1.1.1.1`.

После этого `/etc/resolv.conf` перестанет быть пустым, и проблема уйдёт
без ручных шагов.

---

## Важные оговорки

- Фикс через `-dns-server` держится **только на текущую сессию эмулятора**.
  Если перезапустить AVD из Android Studio без флага — DNS снова отвалится,
  пока не исправлен корень (см. выше).
- **Запуск приложения** для проверки: `adb shell am start -n
  com.z_company.loco_driver/.MainActivity` (не через `monkey` — он открывает
  launcher-alias LeakCanary, а не главный экран).
