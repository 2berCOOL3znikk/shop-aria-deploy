"""Translate extracted static source strings locally. No catalogue or customer input.

This is an authoring aid. Retail vocabulary overrides and browser review are required.
Placeholders are preserved by translating static segments separately.
"""
from pathlib import Path
import json
import re
import sys

root=Path(__file__).resolve().parents[2]
area=root/'data/localization-tools'
sys.path.insert(0,str(area/'python'))
import ctranslate2
import sentencepiece as spm

source=spm.SentencePieceProcessor(model_file=str(area/'opus-source/source.spm'))
target=spm.SentencePieceProcessor(model_file=str(area/'opus-source/target.spm'))
translator=ctranslate2.Translator(str(area/'opus-int8'),device='cpu',compute_type='int8',inter_threads=1,intra_threads=2)
phrases=json.loads((root/'target/localization-tools/pending.json').read_text(encoding='utf-8'))
existing=json.loads((root/'target/localization-tools/existing.json').read_text(encoding='utf-8'))
glossary={
 'Главная':'Overview','Мои магазины':'My stores','Товары':'Products','Заказы':'Orders','Склад':'Inventory','Цены':'Pricing','Закупки':'Purchases','Аналитика':'Analytics','Настройки':'Settings','Поставщики':'Suppliers','Поставщик':'Supplier','Социальная полка':'Social shelf','Соц. полка':'Social shelf',
 'Партии':'Batches','Партия':'Batch','Новая партия':'New batch','Номер партии':'Batch number','Принять партию':'Receive a batch','Приёмка':'Receiving','Выдача':'Handover','Выдать':'Hand over','Выдано':'Handed over','Резерв':'Reservation','Остаток':'Stock','Остатки':'Stock','Доступно':'Available','Списать':'Write off','Списание':'Write-off','Списано':'Written off','Количество':'Quantity','Единица':'Unit','Себестоимость':'Unit cost','Цена':'Price','Выручка':'Revenue','Сумма':'Total','Срок годности':'Expiry date','Годен до':'Best before',
 'Войти':'Sign in','Выйти':'Sign out','Отмена':'Cancel','Сохранить':'Save','Закрыть':'Close','Повторить':'Try again','Назад':'Back','Продолжить':'Continue','Готово':'Done','Добавить':'Add','Изменить':'Edit','Применить':'Apply','Отклонить':'Reject','Сбросить':'Reset','Найти':'Search','Поиск':'Search','Обновить':'Refresh','Загрузить':'Upload','Скачать':'Download','Удалить':'Delete','Подтвердить':'Confirm','Проверить':'Check','Открыть':'Open',
 'Компания':'Company','Моя компания':'My company','Кабинет компании':'Company workspace','Управление':'Management','Кабинет бизнеса':'Business workspace','Меню ☰':'Menu ☰','Тёмная тема':'Dark theme','Светлая тема':'Light theme','Мой аккаунт ↗':'My account ↗','Открыть демо-магазин ↗':'Open demo store ↗','Демо-магазин ↗':'Demo store ↗','Обновить данные':'Refresh data','Загружаются данные магазина…':'Loading store data…','Перейти к содержимому':'Skip to content','Закрыть форму':'Close form',
 'Актуальные':'Current','Тестовые':'Test stores','Архив':'Archive','Все':'All','В архиве':'Archived','Подключено':'Connected','Отключено':'Disconnected','Требуется настройка':'Setup required','Ошибка подключения':'Connection error','Подключить магазин':'Connect a store','Ваш бизнес':'Your business','Название компании':'Company name','Название магазина':'Store name','Адрес магазина':'Store URL','Платформа':'Platform','Адрес':'Address','Доступ':'Access','Проверка':'Check','Каталог':'Catalogue','Учебный магазин ARIA':'ARIA demo store',
 'Низкая':'Low','Умеренная':'Moderate','Высокая':'High','Уверенность':'Confidence','Автопилот':'Autopilot','Советник':'Advisor','Выключено':'Off','С подтверждением':'With approval','Решение ARIA AI':'ARIA AI decision','Автоматический расчёт':'Automatic calculation','ИИ временно недоступен':'AI is temporarily unavailable','Сохранить цену':'Keep price','Экстренная остановка':'Emergency stop','Ожидает':'Pending','В очереди':'Queued','Готово':'Done','Выполнено':'Completed','Отменено':'Cancelled','Применено':'Applied','Требует внимания':'Needs attention','Нужна проверка':'Review required',
 'Имя':'Name','Название':'Name','Описание':'Description','Категория':'Category','Артикул':'SKU','Причина':'Reason','Контакты':'Contact details','Телефон':'Phone','Пароль':'Password','Почта':'Email','Дата':'Date','Статус':'Status','Действия':'Actions','История':'History','Помощь':'Help','Поддержка':'Support','Корзина':'Cart','Профиль':'Profile','Избранное':'Favourites','О проекте':'About ARIA','Нет данных':'No data','Неизвестно':'Unknown','Не указан':'Not specified','Не указана':'Not specified','Не указано':'Not specified','Нет':'No','Да':'Yes','шт.':'pcs','шт':'pcs','Без срока':'No expiry date','ФИО':'Full name','ИИН':'National ID','БИН':'Business registration ID (BIN)',
 'Социальные предложения':'Social proposals','Социальный резерв':'Social reservation','Фонды':'Organizations','Фонд':'Organization','Организации':'Organizations','Организация':'Organization','Проверенная организация':'Verified organization','Журнал действий':'Activity log','Движения и потери':'Movements and losses','Внутренняя касса':'Internal POS','Ручная скидка':'Manual discount','Рекомендации':'Recommendations','Конкуренты':'Competitors','Настройки подключения':'Connection settings',
}
existing.update(glossary)
tokens=re.compile(r'(§\d+§|\{\{\d+\}\})')
pieces=set()
for phrase in phrases:
 if phrase in existing:continue
 for piece in tokens.split(phrase):
  if re.search('[А-Яа-яЁё]',piece) and piece.strip() not in existing:pieces.add(piece.strip())
pieces=sorted(pieces)
translated={}
for offset in range(0,len(pieces),32):
 batch=pieces[offset:offset+32]
 encoded=[source.encode(s,out_type=str) for s in batch]
 results=translator.translate_batch(encoded,beam_size=3,max_decoding_length=512,repetition_penalty=1.1)
 for original,result in zip(batch,results):translated[original]=target.decode(result.hypotheses[0])
 if offset%256==0:print(f'Local source-copy translation {offset}/{len(pieces)}',flush=True)

for phrase in phrases:
 if phrase in existing:continue
 result=[]
 for piece in tokens.split(phrase):
  if not re.search('[А-Яа-яЁё]',piece):result.append(piece);continue
  stripped=piece.strip()
  left=piece[:len(piece)-len(piece.lstrip())];right=piece[len(piece.rstrip()):]
  result.append(left+existing.get(stripped,translated.get(stripped,stripped))+right)
 existing[phrase]=''.join(result)
destination=root/'scripts/localization/translations.en.json'
destination.write_text(json.dumps(existing,ensure_ascii=False,indent=2,sort_keys=True),encoding='utf-8')
print(f'Written {len(existing)} source-copy translations. Human vocabulary/browser review still required.',flush=True)
