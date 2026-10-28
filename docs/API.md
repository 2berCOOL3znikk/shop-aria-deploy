# REST API ARIA Control

Все пути ниже начинаются с `/api/admin/control` и требуют заголовка `Authorization: Bearer <JWT>` администратора. Вход: `POST /api/auth/login` с JSON `email`, `password`. Ошибки полей возвращают 400 и объект fields; конфликт состояния — 409; отсутствие ресурса — 404; отсутствие входа — 401; неверная роль — 403.

## Три связанных ресурса

- `/products`: GET список, POST создание. `/products/{id}`: GET карточка + описание, PUT изменение, DELETE архивирование без стирания истории. Поля: sku, name, category, unitPrice, unit, lowStockThreshold; необязательные brand, description, imageUrl, barcode.
- `/suppliers`: GET, POST. `/suppliers/{id}`: GET, PUT, DELETE. Поля: name, contact, notes, active. Поставщик со связанными партиями не удаляется.
- `/lots`: GET, POST. `/lots/{id}`: GET, PUT до первого движения, DELETE списание с JSON reason. Поля: productId, supplierId, lotNumber, receivedAt, manufacturedAt, expiryDate, quantity, unitCost. Остаток товара вычисляет сервер.

## Цены

`GET /pricing/{productId}?percent=20` возвращает current и recommendation с готовыми сценариями. Выручка в сценарии предполагает продажу всего пригодного остатка. `PUT /pricing/{productId}` принимает percent (целое 0–90) и reason. Ниже себестоимости, без остатка, после истечения срока и в состоянии резерва применение отклоняется.

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
