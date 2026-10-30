# REST API ARIA Control

## Списки панели

`GET /orders/page?q=&state=&page=0&size=25&sort=newest` — серверный поиск и страницы заказов. `state`: пусто либо PENDING/SHIPPED/DELIVERED/CANCELLED. Сортировка: newest, oldest, amount (сумма по убыванию). Поиск: номер, email, адрес, сохранённое название/SKU позиции; `%` и `_` — обычные символы, не шаблоны SQL. Ответ: content, number, size, totalElements, totalPages. Позиция содержит productName, productSku, quantity и pricePerUnit; внутренний объект товара не передаётся.

`GET /audit/page?q=&page=0&size=25` — поиск всей истории по сотруднику, действию, объекту и причине, новые события первыми. Ответ имеет такой же формат страницы. Размер 1–100, страница 0–1000000, запрос до 200 символов; неверные параметры дают 400.

`GET /products` возвращает компактные строки панели с фактическим пригодным quantityOnHand. Просроченные и списанные партии исключаются во время чтения, без изменения базы через GET. `/overview` дополнительно возвращает availableUnits, expiredUnits и stockCost — закупочную стоимость пригодных партий, не прогноз выручки. Старые `/orders` и `/audit` сохранены для совместимости; панель использует постраничные варианты.

Все пути ниже начинаются с `/api/admin/control` и требуют заголовка `Authorization: Bearer <JWT>` администратора. Вход: `POST /api/auth/login` с JSON `email`, `password`. Ошибки полей возвращают 400 и объект fields; конфликт состояния — 409; отсутствие ресурса — 404; отсутствие входа — 401; неверная роль — 403.

## Три связанных ресурса

- `/products`: GET список, POST создание. `/products/{id}`: GET карточка + описание, PUT изменение, DELETE архивирование без стирания истории. Поля: sku, name, category, unitPrice, unit, lowStockThreshold; необязательные brand, description, imageUrl, barcode.
- `/suppliers`: GET, POST. `/suppliers/{id}`: GET, PUT, DELETE. Поля: name, contact, notes, active. Поставщик со связанными партиями не удаляется.
- `/lots`: GET, POST. `/lots/{id}`: GET, PUT до первого движения, DELETE списание с JSON reason. Поля: productId, supplierId, lotNumber, receivedAt, manufacturedAt, expiryDate, quantity, unitCost. Остаток товара вычисляет сервер.

## Цены

`GET /pricing/{productId}?percent=20` возвращает current и recommendation с готовыми сценариями. Выручка в сценарии предполагает продажу всего пригодного остатка. `PUT /pricing/{productId}` принимает percent (целое 0–90), reason и необязательный expectedPrice — подтверждаемую цену после скидки. Панель всегда передаёт expectedPrice: если базовая цена успела измениться после расчёта, сервер отвечает 409 и требует пересчитать сценарий. Без expectedPrice старые клиенты сохраняют прежний контракт. Ниже себестоимости, без остатка, после истечения срока и в состоянии резерва применение отклоняется.

## Заказы

`GET /orders`. `PUT /orders/{id}/status` принимает status и reason. Допустимые переходы: PENDING → SHIPPED → DELIVERED; PENDING → CANCELLED с возвратом распределений партий. Старый заказ без распределения требует ручной сверки, автоматический возврат для него запрещён.

## Социальная полка

`POST /api/social/offers` принимает productId, quantity, validUntil. Публичное чтение `/api/social/offers` не раскрывает ID организации. Проверенная организация бронирует через `/api/organizations/offers/{id}/claim`; роль и проверка проверяются сервером.

`GET /social` в Control доступен администратору. `POST /social/{id}/confirm` подтверждает фактическую выдачу, `/cancel` отменяет предложение/резерв. Тело: reason. Только первая допустимая операция применяется.

## Сводка и журнал

`GET /overview`, `GET /audit` (последние 100 событий). `POST /intelligence/briefing` возвращает text, mode (RULES / AI), configured, note. Отсутствие ключа и ошибка провайдера имеют явный режим правил.

## Фотографии

`POST /api/admin/products/{id}/photo`, multipart-поле photo. JPEG/PNG, до 5 МБ, до 4096×4096. `GET /api/products/{id}/photo` — публичное изображение.

Проверяемые примеры запросов находятся в ControlIntegrationTest: реальные HTTP-вызовы к случайному локальному порту с отдельной тестовой H2.

## Настраиваемые правила

`GET /policies` возвращает общее правило `*` и переопределения категорий. `PUT /policies` принимает category, warningDays, criticalDays, clearanceDays, warningPercent, criticalPercent, clearancePercent, slowStockDays, slowStockPercent, minimumMarginPercent. Все числовые поля обязательны. Пороги дней идут от экстренного к предупреждению; скидки возрастают при сокращении срока. При конфликте с действующей скидкой настройка отклоняется с 409, цены не переписываются автоматически.

`DELETE /policies?category=...` удаляет отдельное правило с проверкой совместимости скидок с общим правилом. Само общее правило удалить нельзя. Названия категорий нормализуются по регистру и пробелам по краям. Сохранение и сброс фиксируются в журнале.

При минимальной марже 10%, закупке 800 ₸ и базовой цене 1 000 ₸ максимальная целая скидка — 11%: цена 890 ₸, маржа 10,11%. Это маржа от цены продажи, а не наценка на закупку. Ограничение ниже себестоимости действует всегда. Снять скидку можно и после распродажи остатка; это не делает просроченные партии доступными.


Smart Pricing: [расчёты, RECOMMEND/AUTOPILOT, история, API и проверка](SMART_PRICING.md). Новые миграции V6–V7 добавляют минимальную цену, решения, историю цен и настройки с начальным режимом RECOMMEND.


Продолжение ARIA AI: [активный выбор стратегии, согласие/лимиты, CSV конкурентов, социальные черновики и публичный помощник](SMART_PRICING.md). Миграции V8–V11 добавляют только новые поля/таблицы с безопасными значениями по умолчанию.


Приёмка релиза 29.10.2026: `GET /api/aria/reports/social.csv` — ADMIN, только фактические выдачи социальной полки со снимками. Прежний `acts.csv` сохраняется для отдельных актов. `POST /api/profile` проверяет fullName≤100, phone≤50, address≤500; email всегда берётся из токена. `POST /api/reviews` требует положительный productId, непустой comment≤2000, author≤100 и rating 1–5 (по умолчанию 5). Неверный ввод — 400. `/api/admin/control/smart-pricing/ai-status` дополнительно возвращает `scheduledAvailable`: в Free-профиле false.

## Автономная платформа (V12–V16)

Только ADMIN: GET `/api/admin/control/manager`; PUT `/manager/settings` с актуальной revision; POST `/manager/run`, `/manager/stop`, `/manager/actions/{id}/acknowledge`; GET `/manager/analytics/{productId}`.

Закупки: GET/POST `/api/admin/control/purchases`; POST `/purchases/quotes`, `/{id}/approve`, `/{id}/cancel`, `/{id}/receive`. Приёмка требует уникальный requestKey; другой состав с прежним ключом отклоняется. Внешней отправки/оплаты нет.

POS: GET `/api/admin/control/pos`; POST `/pos/sales` {requestKey,lines:[{productId,quantity,expectedPrice}]}; POST `/pos/{id}/return` {reason}. Это внутренние операции, не фискализация. Движения: GET `/api/admin/control/movements?productId=&page=0&size=25`.

Социальная логистика: GET `/api/admin/control/social-proposals/staff`; PUT `/social-proposals/{id}/logistics` {version,assigneeId,plannedAt,status,reason}. plannedAt — местное время магазина Asia/Qyzylorda без смещения; обновление последовательно, только после резерва.

Интеграции: GET `/api/admin/control/integrations`; POST `/integrations/poster/prepare` {sale:{requestKey,lines},productIds:{внутреннийId:PosterId}} → только несохранённый план, sent=false/fiscal=false. Нет внешнего HTTP-запроса или списания.

Политика и проверка функций: [AUTONOMOUS_MANAGER.md](AUTONOMOUS_MANAGER.md).

## Автономная платформа V17–V18

V21: GET `/api/admin/control/stores/{id}/history` (ADMIN) возвращает последние 30 завершённых попыток CHECK/SYNC: status, imported, skipped, note, completedAt. Внешний каталог содержит parentExternalId для вариаций; currency не выдумывается. Повторный импорт сохраняет ID снимков. Это не очередь с восстановлением незавершённых попыток.

Магазины V20 (ADMIN): GET/POST `/api/admin/control/stores`; PUT `/stores/business` `{name}`; PUT `/stores/{id}/credentials` `{version,key,secret}`; POST `/stores/{id}/check`, `/sync`, `/disconnect` `{version}`; GET `/stores/{id}/catalog`. `stores` — часть того же префикса `/api/admin/control`. Поддержаны ARIA_DEMO и read-only часть WOOCOMMERCE. Ошибка внешней проверки сохраняет статус ERROR и lastError, без ложного CONNECTED. После сохранения доступов клиент использует новую version. Ключи не возвращаются в API. Подробности: [B2B_STAGE2.md](B2B_STAGE2.md).

Дополнение V19: GET `/api/admin/control/organization` (ADMIN) — несекретные metadata/binding текущего silo. Tenant не выбирается заголовком или параметром. В недемонстрационной организации все бизнес-API требуют ADMIN; публичная клиентская регистрация/каталог сохранены только для `demo`. См. [B2B_TRANSITION.md](B2B_TRANSITION.md).

Все пути ниже требуют ADMIN, префикс `/api/admin/control`.

- GET `/manager/center`: реальные показатели, сохранённые исполнения/очередь, исключения и последние решения. Ограниченные списки не считаются полной историей.
- POST `/manager/tasks/{id}/retry` `{reason}`: повтор после ошибки, с журналом и сохранением политики/лимитов. Самостоятельно не разрешает AI/цену.
- GET `/smart-pricing/{id}`: конкретное сохранённое решение. UI: `/control.html?decision=ID#smart`.
- PUT `/lots/{id}/handling`: `{revision,requirements,status,informationConfirmed,reason}`; status UNKNOWN/CONFIRMED/REJECTED. Несовпадение revision → 409, отсутствие подтверждения для CONFIRMED/REJECTED → отказ. REJECTED исключает запас из продажи.

Расчёт Smart Pricing содержит `planning`: observationDays, forecastDays, clearanceDays, nearestExpiryDays, reviewAfterHours, basis. Аналитика прогноза: expectedUnits и horizonDays; старое JSON-поле units14 заполнено только при horizonDays=14. Нельзя интерпретировать срок прогноза как дату годности.

Изменения складских/ценовых данных и запись задачи выполняются одной транзакцией. Ошибка откатывает и данные, и новую задачу. Перезапуск сохраняет dueAt/generation/attempts; истёкшая аренда возвращает работу, устаревший исполнитель не может применить цену.
