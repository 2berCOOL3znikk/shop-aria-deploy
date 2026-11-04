"""Build the Russian course report from actual, sanitized audit evidence.

The HTML/Markdown fallback is intentional: the local DOCX render probe has no
LibreOffice engine. An unrendered DOCX is not represented as a verified output.
"""
from pathlib import Path
import hashlib
import html
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/rubezh2'
EV = OUT / 'EVIDENCE'
load = lambda name: json.loads((EV / name).read_text(encoding='utf-8-sig'))
audit, junit, visual = load('audit-final.json'), load('junit-summary.json'), load('visual-review.json')
assert audit['passed'] and visual['passed'] and junit['tests'] == 182
assert junit['failures'] == junit['errors'] == junit['skipped'] == 0
revision = subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD'], cwd=ROOT, text=True).strip()
rows = list(audit['rows'])
if not any(r['id'] == 'FE12' for r in rows):
    rows.insert(11, dict(id='FE12', section='Frontend', function='Контраст и ссылки',
        precondition='Открыта главная в двух темах', actions='Измерить 5 текстовых стилей в каждой теме; открыть внутренние ссылки',
        expected='Контраст не ниже 4,5 к 1; ссылки возвращают 200',
        actual=f"10 установившихся значений контраста, минимум {min(s['ratio'] for s in visual['samples']):.2f}; {len(visual['links'])} ссылок HTTP 200. Полный WCAG-аудит не заявляется.",
        status='Пройден', evidence='visual-review.json', proposal='Повторять выборочные измерения после изменений цветов'))
rows.append(dict(id='EXT01', section='Backend', function='Живой AI черновик после усиления защиты',
    precondition='Бесплатная модель и отдельное согласие на текст', actions='Запросить черновик через реальный интерфейс',
    expected='Безопасный ответ модели, проверка владельцем, сохранённая карточка',
    actual='OpenRouter вернул 429, приложение отклонило генерацию. Локальные 5 попыток исчерпаны. Финальный живой сценарий не завершён.',
    status='Не пройден', evidence='ai-live-limit.json', proposal='Повторить после восстановления бесплатного лимита без обхода проверок'))
front = [r for r in rows if r['section'] == 'Frontend']
back = [r for r in rows if r['section'] == 'Backend']
passed = sum(r['status'] == 'Пройден' for r in rows)
headers = ['№', 'Раздел', 'Проверяемая функция', 'Предусловие', 'Выполненные действия', 'Ожидаемый результат', 'Фактический результат', 'Статус', 'Доказательство', 'Предложение']
fields = ['id', 'section', 'function', 'precondition', 'actions', 'expected', 'actual', 'status', 'evidence', 'proposal']

def clean(value):
    return str(value).replace('|', '\\|').replace('\n', '<br>')

def md_table(heads, values):
    return '\n'.join(['| ' + ' | '.join(map(clean, heads)) + ' |', '| ' + ' | '.join('---' for _ in heads) + ' |'] + ['| ' + ' | '.join(map(clean, row)) + ' |' for row in values])

def test_values(group):
    return [[r[k] if k != 'evidence' else f"[Открыть](EVIDENCE/{r[k]})" for k in fields] for r in group]

description = [
    ('Проблема', 'Владельцу магазина приходится сопоставлять продажи, остатки, сроки и цены в разных инструментах. Разрозненные данные затрудняют своевременное решение о пополнении, скидке или передаче пригодного излишка.'),
    ('Цель', 'Объединить учёт и поддержку решений в одной платформе ARIA, сохранив проверку ограничений, права пользователей и прослеживаемость операций.'),
    ('Пользователи', 'Владельцы и сотрудники магазинов используют административную панель. Покупатели работают с демонстрационной витриной. Проверенные социальные организации участвуют в согласованной передаче товаров.'),
    ('Основные функции', 'Каталог, партии, заказы, корзина, внутренняя касса, закупочные черновики, ценовые рекомендации, импорт цен конкурентов, социальные предложения и подключения магазинов. WooCommerce читает внешний каталог и заказы в отдельные снимки; запись во внешний магазин не включена.'),
    ('Взаимодействие Frontend и Backend', 'HTML, CSS и JavaScript формируют страницы. Fetch API передаёт запросы REST в Spring Boot. Контроллеры проверяют доступ и входные данные, сервисы выполняют бизнес-операции, JPA сохраняет состояние в PostgreSQL.'),
    ('Особенность решения', 'Java рассчитывает допустимые сценарии, а нейросеть может участвовать в выборе или объяснении. Ответ модели проходит серверную проверку. Источники AI и автоматического расчёта различаются; нейросеть не исполняет произвольный SQL и не меняет цены напрямую.'),
    ('Практическая значимость', 'Единая история партий, цен и операций помогает проверять причины управленческих решений. Снижение потерь или рост прибыли в реальном магазине в этой работе не измерялись.'),
    ('Логика работы', 'Карточка товара → приёмка партии → анализ доступных данных → рекомендация → подтверждение либо разрешённое действие в границах → учёт продажи или социальной выдачи. Демонстрационная витрина использует то же бизнес-ядро, но её учебные значения не являются рыночными наблюдениями.'),
]
architecture = [
    ['Frontend', 'HTML5, CSS3, JavaScript, Fetch API, PWA', 'Витрина и административная панель; клиент не заменяет серверную авторизацию'],
    ['Backend', 'Java 21, Spring Boot 4.1.1, Spring Security', 'REST, DTO, Bean Validation, сервисы и централизованные ошибки'],
    ['Данные', 'PostgreSQL, JPA/Hibernate, Flyway V1–V27', 'Товары, партии, поставщики, заказы, решения, журналы и интеграции'],
    ['AI', 'OpenRouter, только бесплатные модели', 'Структурированный ответ с ограниченными вариантами и отдельными согласиями'],
    ['B2B', 'Отдельная база и процесс на организацию', 'Привязка JWT к организации; импортированные снимки отделены от местного склада'],
]
improvements = [
    ['F1', 'Frontend', 'У полей входа и регистрации не было постоянных подписей', 'Добавлены label, autocomplete, пределы длины и role=alert', 'FE10 до: 0 из 5; после: 5 из 5'],
    ['F2', 'Frontend', 'Повторное добавление не давало согласованного управления количеством', 'Общее серверное состояние, кнопки плюс/минус, защита повторов и обновление вкладок', 'cart-report.json; 8 быстрых нажатий, одна позиция'],
    ['F3', 'Frontend', 'Сложная навигация и главная с преобладанием подключений', '11 основных разделов, связанные вкладки, реальные показатели и задачи', 'FE04, FE05; workspace-report.json'],
    ['F4', 'Frontend', 'Длина поискового ввода не была ограничена', 'maxlength=200 и доступное имя поля поиска', 'FE08; улучшение устойчивости, не воспроизведённый сбой'],
    ['B1', 'Backend', 'Первое добавление в корзину не блокировало владельца и товар', 'Порядок блокировок владелец → товары; согласован с оформлением заказа', 'BE12; тест конкурентных добавлений и checkout'],
    ['B2', 'Backend', 'Устаревший внутренний checkout() очищал общую таблицу корзин', 'Очистка ограничена LEGACY_OWNER, действия текущего API ограничены владельцем', 'CartServiceTest; чужие записи сохранены. Риск внутреннего метода, не открытый HTTP-доступ'],
    ['B3', 'Backend', 'Модель добавила неподтверждённый состав по одному названию', 'Названия, нейтральные описания и вопросы ограничены сервером; отзыв согласия и подтверждение проверяются', 'ProductDraftModelServiceTest, ProductCreationIntegrationTest; живой финальный сценарий заблокирован 429'],
    ['B4', 'Backend', 'Ошибка провайдера внутри HTTP 200 могла выглядеть как ошибка формата', 'Разбирается error, сохраняется безопасный код отказа; ложного AI-результата нет', 'IntelligenceServiceTest, ProductDraftModelServiceTest'],
    ['B5', 'Backend', 'Свежий AI-ответ по старой рекомендации мог не попадать в окно центра', 'Сортировка по более позднему из createdAt и aiCompletedAt', 'ControlCenterActivityTest, 2 регрессионных сценария'],
]
limitations = [
    'Финальный безопасный сценарий AI Product Creation с живым провайдером не завершён: получен 429 и исчерпан местный дневной лимит. Ранний ответ модели до усиления проверки не используется как доказательство нового контракта. Платные модели не подключались.',
    'Реальное подключение магазина владельца WooCommerce не проверено: нет тестового внешнего магазина и его доступов. Контрактные тесты и локальные подписанные уведомления не приравниваются к внешнему E2E.',
    'Проверка браузеров выполнена в Edge и Chrome; это два продукта на Chromium. Firefox, Safari и отдельный движок WebKit не проверялись.',
    'Контраст измерен выборочно для пяти текстовых стилей в двух темах. Полное соответствие WCAG, нагрузочная устойчивость и независимый аудит безопасности не заявляются.',
    '15 вариантов упаковки подготовлены в справочном файле с первичными источниками, но новый коммерческий каталог ещё не импортирован. Права на фотографии не подтверждены; фотографии не добавлялись. Старые учебные карточки витрины требуют дальнейшего оформления.',
    'Самостоятельная регистрация новых компаний, расширенные роли сотрудников и административный чат с инструментами ещё не завершены. Текущая изоляция организаций сохраняется отдельными процессами и базами.',
    'Исторические дубли корзин автоматически не объединяются: исправление предотвращает новые гонки. Рабочая база, main и основной Render не изменялись.',
]

def para(text): return ('p', text)
def heading(text): return ('h2', text)
def bullets(values): return ('ul', values)
def table(heads, values, wide=False): return ('table', (heads, values, wide))
def image(path, caption, width='wide'): return ('image', (path, caption, width))

sections = [
    heading('1 Цель работы'),
    para('Выполнить самопроверку собственного проекта ARIA по критериям II рубежного контроля: проверить Frontend и Backend, сохранить доказательства, внести обоснованные исправления и подтвердить их повторными проверками.'),
    para(f'Проверенная версия исходного кода — {revision}, ветка codex/production-foundation. Контрольная дата — 4 ноября 2026 года. Выполнено 12 Frontend- и 15 Backend-проверок локального контракта; все 27 пройдены. Отдельный живой AI-сценарий не пройден из-за ограничения внешнего бесплатного провайдера.'),
    heading('2 Краткое описание проекта ARIA'),
    *[para(f'**{q}.** {answer}') for q, answer in description],
    heading('3 Архитектура Frontend и Backend'),
    table(['Компонент', 'Технологии', 'Ответственность'], architecture),
    para('Используется разделение Controller → Service → Repository. Репозитории инкапсулируют работу с сущностями. Сервисный слой координирует ограничения и транзакции. Покупательская роль USER и административная ADMIN проверены фактическими API-запросами; интерфейсное скрытие меню не считается защитой данных.'),
    heading('4 Методика тестирования'),
    para('Проверки выполнялись на http://127.0.0.1:8084 с отдельной PostgreSQL на порту 55434. Новые записи HTTP-аудита имели учебные названия и не описывают реальную поставку. В этом сценарии партии списаны с причиной, карточки архивированы; история сохранена.'),
    para(f"Основной браузер — Edge {audit['browserVersions']['Edge']}; дополнительная проверка — Chrome {audit['browserVersions']['Chrome']}. Ширины — 390 и 1440 px. Ожидание ответа и отказ 503 для интерфейса моделировались явно; успешные запросы затем направлялись в реальный Backend. Подмена ответа не использовалась как доказательство успешной работы AI."),
    para(f"Сохранённый полный PostgreSQL-прогон JUnit содержит {junit['tests']} теста: ошибок 0, отказов 0, пропусков 0. Он повторно не запускался без изменения Java-кода. Текущий HTTP-аудит, браузерные проверки, проверка двух организаций и сохранности после перезапуска учитываются отдельно; их количества не суммируются с JUnit как уникальные тесты."),
    para(f"Локальная прогретая главная загрузилась до проверяемого содержимого за {audit.get('warmDashboardMs','—')} мс в одном наблюдении. Это измерение конкретного стенда, а не SLA Render Free или нагрузочный тест."),
    heading('5 Аудит Frontend'),
    para('Проверены регистрационная форма, отказ входа, административная навигация, главная, мобильный экран, поиск, пустые состояния, ожидание ответа и обработка ошибки. Главная показывает сохранённые данные; действия модели отделены от автоматического расчёта. Видимые подписи полей, пределы ввода и общий контроль количества уменьшают неоднозначность действий пользователя.'),
    para(f"Выборочные значения контраста установившегося интерфейса лежат в диапазоне {min(s['ratio'] for s in visual['samples']):.2f}–{max(s['ratio'] for s in visual['samples']):.2f} к 1. Проверены {len(visual['links'])} внутренних ссылок витрины; каждая вернула HTTP 200. При измерении отключались переходные анимации, чтобы не измерять промежуточный кадр смены темы."),
    heading('6 Таблица Frontend тестов'), table(headers, test_values(front), True),
    heading('7 Аудит Backend'),
    para('Проверены создание, чтение, изменение и удаление ресурсов, обязательные поля, формат цены, дубли SKU, отсутствующие объекты, регистрация, разграничение ролей, конкурентное изменение корзины и защита чужих данных. Для товара удаление означает архивирование, для партии — списание; такие операции сохраняют историю.'),
    para('Изоляция организаций проверена двумя реальными Spring Boot-процессами, двумя PostgreSQL-базами и ограниченными логинами БД. Email владельца и ключ подписи JWT намеренно совпадали. Чужие токены были отклонены, одинаковый SKU не смешивал данные, изменение AI-согласия в компании A не изменило компанию B. Проверено наличие V27 в обеих базах; внешних AI-запросов этот сценарий не делал.'),
    heading('8 Таблица Backend тестов'), table(headers, test_values(back), True),
    heading('9 Выявленные недостатки'),
    para('Новым подтверждённым замечанием к форме стали отсутствующие постоянные подписи пяти обязательных полей. До исправления FE10 обнаружил 0 подписей из 5. Отдельно выявлены риски повторных операций корзины, небезопасной старой внутренней очистки и неподтверждённых сведений в AI-тексте. Ниже указаны исправления и соответствующие доказательства.'),
    para('Два начальных расхождения Backend относились к тестовому сценарию: пустой объект уже возвращал 400 на стадии разбора, а SKU штатно переводился в верхний регистр. Исправлены ожидания теста, а не объявлены несуществующие дефекты приложения. Также уточнены ожидание асинхронного CHECK, начальный about:blank в браузерном тесте и измерение контраста после анимации.'),
    heading('10 Предложения по улучшению'),
    table(['Код', 'Слой', 'Основание', 'Конкретное изменение', 'Проверка'], improvements, True),
    heading('11 Внесённые исправления'),
    bullets([
        'Постоянные label, autocomplete и пределы длины в формах входа и регистрации; доступное объявление ошибок.',
        'Поиск до 200 символов; 11 разделов главного меню и сохранённые связанные инструменты во вкладках.',
        'Общее состояние корзины с сервера, управление количеством, ограничение доступным остатком, синхронизация вкладок и сохранение введённого адреса при обновлении.',
        'Серверная сериализация действий владельца, согласованный порядок блокировок и очистка только своей корзины.',
        'Java SKU с последовательностью и уникальностью; отдельное согласие на AI-текст, сохраняемые попытки, серверная проверка допустимых вариантов, подтверждение владельцем и защита повторного создания.',
        'Понятные источники решений, обработка ошибок провайдера внутри HTTP 200 и порядок решений по времени последней обработки.',
    ]),
    heading('12 Повторная проверка'),
    table(['Проверка', 'Результат', 'Доказательство'], [
        ['Целевой повтор FE08 FE10 BE03 BE05', '4 из 4 пройдены', 'retest-FE08,FE10,BE03,BE05.json'],
        ['Основная браузерная навигация', '23 раздела, две ширины и две темы; без исключений', 'workspace-report.json'],
        ['Корзина', 'Повторы, две вкладки, остаток, сумма, адрес и удаление проверены', 'cart-report.json'],
        ['Мои магазины', 'Подключения, очередь, история и локальные подписанные уведомления проверены', 'stores-report.json'],
        ['Изоляция компаний', 'Две БД и процесса; новые настройки разделены; чужие JWT отклонены', 'organization-isolation.json'],
        ['Сохранность после перезапуска', 'Название, SKU и архивный статус учебной карточки сохранились', 'persistence.json'],
        ['Базовый полный JUnit прогон', '182 теста; 0 ошибок, отказов и пропусков', 'junit-summary.json'],
        ['Живой финальный AI сценарий', 'Не пройден; внешний 429, проверка не обходилась', 'ai-live-limit.json'],
    ]),
    heading('13 Заключение'),
    para('Самопроверка ARIA завершена в проверенном объёме Frontend и Backend. Для каждого теста сохранены действия, ожидаемый и фактический результат, статус и доказательство. Выполнено более двух конкретных улучшений каждого слоя. Пройдено 27 локальных проверок; сохранённый полный JUnit-прогон содержит 182 успешных теста. Это подтверждение проверенных сценариев, а не утверждение об отсутствии всех возможных ошибок.'),
    para('Критерии стабильного ядра выполняются: API и PostgreSQL работают, роли разделены, формы обрабатывают ошибки, состояние сохраняется. Основное внешнее ограничение — невозможность завершить текущий безопасный живой AI-сценарий при 429. При отказе модель не подменяется фиктивным ответом.'),
    bullets(limitations),
    heading('14 Приложения со скриншотами'),
    para('Скриншоты получены из работающего локального приложения. На них демонстрационные данные. Таблицы и JSON сохраняют точные результаты, которые невозможно прочитать на уменьшенном обзорном изображении.'),
    image('EVIDENCE/FE10-before.png', 'Рисунок 1 Форма регистрации до исправления постоянных подписей', 'narrow'),
    image('EVIDENCE/FE10.png', 'Рисунок 2 Та же форма после добавления постоянных подписей', 'narrow'),
    image('EVIDENCE/FE04.png', 'Рисунок 3 Главная административной панели с фактическими показателями учебной базы'),
    image('EVIDENCE/FE05.png', 'Рисунок 4 Доступ к товарам при ширине экрана 390 px', 'narrow'),
    para('Дополнительное доказательство обработки явно смоделированного отказа 503 и повторного запроса — [FE07.png](EVIDENCE/FE07.png). Все остальные снимки сохранены в EVIDENCE.'),
    image('EVIDENCE/cart-cart-1440.png', 'Рисунок 5 Корзина с общим контролем количества и фактической суммой'),
    para('Репозиторий: https://github.com/2berCOOL3znikk/shop-aria-deploy. Ветка codex/production-foundation. Машинные доказательства находятся в EVIDENCE, воспроизводимые сценарии — scripts/rubezh2-audit.cjs, scripts/rubezh2-visual-review.cjs, scripts/cart-e2e.cjs, scripts/stores-e2e.cjs и scripts/b2b-isolation-e2e.ps1. Пароли, токены и ключи в отчёт не включены.'),
]

def inline(text):
    import re
    text = html.escape(str(text))
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    return re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', text)

def render_md(items):
    blocks=[]
    for kind, value in items:
        if kind=='h2': blocks.append('## '+value)
        elif kind=='p': blocks.append(value)
        elif kind=='ul': blocks.append('\n'.join('- '+x for x in value))
        elif kind=='table': blocks.append(md_table(value[0],value[1]))
        elif kind=='image': blocks.append(f'![{value[1]}]({value[0]})\n\n{value[1]}')
    return '\n\n'.join(blocks)+'\n'

def render_html(items):
    blocks=[]
    for kind,value in items:
        if kind=='h2': blocks.append('<h2>'+html.escape(value)+'</h2>')
        elif kind=='p': blocks.append('<p>'+inline(value)+'</p>')
        elif kind=='ul': blocks.append('<ul>'+''.join('<li>'+inline(x)+'</li>' for x in value)+'</ul>')
        elif kind=='table':
            heads,values,wide=value
            blocks.append('<div class="matrix-section">' if wide else '<div class="table-section">')
            blocks.append('<table class="'+('matrix' if len(heads)==10 else 'normal-table')+'" data-cols="'+str(len(heads))+'"><thead><tr>'+''.join('<th>'+html.escape(x)+'</th>' for x in heads)+'</tr></thead><tbody>')
            for row in values:blocks.append('<tr>'+''.join('<td>'+inline(x)+'</td>' for x in row)+'</tr>')
            blocks.append('</tbody></table></div>')
        elif kind=='image':blocks.append('<figure class="'+value[2]+'"><img src="'+html.escape(value[0])+'" alt="'+html.escape(value[1])+'"><figcaption>'+html.escape(value[1])+'</figcaption></figure>')
    return '\n'.join(blocks)

title='# Самопроверка проекта ARIA\n\nОтчёт по II рубежному контролю\n\nФИО ____________________\n\nГруппа ____________________\n\nПреподаватель ____________________\n\n2026\n\n'
(OUT/'RUBEZH2_REPORT.md').write_text(title+render_md(sections),encoding='utf-8',newline='\n')
css='''
@page { size:8.5in 11in; margin:.7in; }
@page audit { size:11in 8.5in; margin:.55in; }
*{box-sizing:border-box}body{margin:0;color:#000;font:12pt/1.35 "Times New Roman",serif;background:#fff}
main{max-width:10in;margin:auto}h1,h2,h3{color:#000;font-family:inherit;line-height:1.2}h1{font-size:24pt;margin:0 0 18pt}h2{font-size:15pt;margin:22pt 0 10pt;break-after:avoid}p{margin:0 0 10pt;orphans:3;widows:3}li{margin:0 0 5pt}ul{padding-left:20pt}a{color:#000;text-decoration:underline;overflow-wrap:anywhere}
.cover{height:9.1in;display:flex;flex-direction:column;justify-content:center;break-after:page}.cover .subtitle{font-size:16pt;margin:0 0 44pt}.cover .field{margin:9pt 0}.cover .year{margin-top:40pt}table{border-collapse:collapse;width:100%;margin:12pt 0 18pt;table-layout:fixed;font-size:10.5pt;line-height:1.22}th,td{border:1px solid #d9d9d9;padding:7pt 6pt;vertical-align:middle;overflow-wrap:anywhere}th{background:#ededed;font-weight:bold;text-align:left;color:#000}thead{display:table-header-group}tr{break-inside:avoid}.matrix-section{page:audit}.matrix{font-size:9.5pt;line-height:1.2}.matrix th,.matrix td{padding:5pt 4pt}.matrix th:nth-child(1),.matrix td:nth-child(1){width:6%;text-align:center;white-space:nowrap}.matrix th:nth-child(2){width:8%}.matrix td:nth-child(2){white-space:nowrap}.matrix th:nth-child(3){width:10%}.matrix th:nth-child(4){width:9%}.matrix th:nth-child(5){width:11%}.matrix th:nth-child(6){width:10%}.matrix th:nth-child(7){width:15%}.matrix th:nth-child(8),.matrix td:nth-child(8){width:7%;text-align:center}.matrix th:nth-child(9){width:12%}.matrix th:nth-child(10){width:12%}.normal-table td:first-child{font-weight:500}.normal-table[data-cols="3"] th:first-child{width:25%}.normal-table[data-cols="3"] th:nth-child(2){width:35%}.normal-table[data-cols="3"] th:last-child{width:40%}.normal-table[data-cols="5"] th:first-child{width:7%}.normal-table[data-cols="5"] th:nth-child(2){width:9%}.normal-table[data-cols="5"] th:nth-child(3){width:27%}.normal-table[data-cols="5"] th:nth-child(4){width:31%}.normal-table[data-cols="5"] th:last-child{width:26%}figure{margin:18pt 0 22pt;break-inside:avoid;text-align:center}figure img{max-width:100%;max-height:6.7in;object-fit:contain}figure.narrow img{max-width:3.8in;max-height:6.5in}.comparison{display:flex;gap:.22in;align-items:flex-start;break-inside:avoid;margin-top:16pt}.comparison figure{width:49%;margin:0 0 20pt}.comparison img{max-width:100%!important;max-height:5.8in!important}figcaption{font-size:11pt;line-height:1.3;margin-top:8pt;color:#000}
@media screen{body{background:#ededed;padding:32px}main{max-width:1300px;background:white;padding:48px;box-shadow:0 2px 18px #0001}.cover{height:700px}.matrix-section{overflow:auto}.matrix{min-width:1180px}figure img{max-height:800px}}
'''
cover='<section class="cover"><h1>Самопроверка проекта ARIA</h1><p class="subtitle">Отчёт по II рубежному контролю<br>Аудит Frontend и Backend</p><p class="field">ФИО ____________________</p><p class="field">Группа ____________________</p><p class="field">Преподаватель ____________________</p><p class="year">2026</p></section>'
import re
body_html=re.sub(r'<h2>([^<]+)</h2>\n<div class="matrix-section">',r'<div class="matrix-section"><h2>\1</h2>',render_html(sections))
for intro,following in [('7 Аудит Backend','8 Таблица Backend тестов'),('9 Выявленные недостатки','10 Предложения по улучшению')]:
    body_html=re.sub(r'(<h2>'+intro+r'</h2>.*?)<div class="matrix-section"><h2>'+following+r'</h2>',r'<div class="matrix-section">\1<h2>'+following+'</h2>',body_html,flags=re.S)
body_html=re.sub(r'(<figure class="narrow"><img src="EVIDENCE/FE10-before.png".*?</figure>)\n(<figure class="narrow"><img src="EVIDENCE/FE10.png".*?</figure>)',r'<div class="comparison">\1\2</div>',body_html,flags=re.S)
(OUT/'RUBEZH2_REPORT.html').write_text('<!doctype html><html lang="ru"><head><meta charset="utf-8"><title>Самопроверка проекта ARIA</title><style>'+css+'</style></head><body><main>'+cover+body_html+'</main></body></html>',encoding='utf-8',newline='\n')
(OUT/'PROJECT_DESCRIPTION.md').write_text('# Краткое описание проекта ARIA\n\n'+'\n\n'.join('## '+q+'\n\n'+a for q,a in description)+'\n',encoding='utf-8',newline='\n')
(OUT/'FRONTEND_AUDIT.md').write_text('# Аудит Frontend ARIA\n\n12 проверок; все пройдены. Визуальный аудит выборочный; полного соответствия WCAG и кроссдвижковой совместимости не заявляет.\n\n'+md_table(headers,test_values(front))+'\n',encoding='utf-8',newline='\n')
(OUT/'BACKEND_AUDIT.md').write_text('# Аудит Backend ARIA\n\n15 локальных проверок пройдены. Внешний AI-сценарий не пройден из-за 429, не заменён фиктивным ответом.\n\n'+md_table(headers,test_values(back))+'\n',encoding='utf-8',newline='\n')
(OUT/'TEST_RESULTS.md').write_text('# Результаты самопроверки ARIA\n\n27 локальных проверок пройдены; 1 внешний AI-сценарий не пройден. Базовый полный JUnit прогон — 182 теста, 0 ошибок/отказов/пропусков. Числа не суммируются.\n\n## Frontend\n\n'+md_table(headers,test_values(front))+'\n\n## Backend\n\n'+md_table(headers,test_values(back))+'\n',encoding='utf-8',newline='\n')
(OUT/'IMPROVEMENTS.md').write_text('# Улучшения и исправления ARIA\n\nУлучшения собственного проекта, без приписывания их внешней проверяющей группе.\n\n'+md_table(['Код','Слой','Наблюдение или риск','Изменение','Проверка'],improvements)+'\n\n## Ограничения\n\n'+'\n'.join('- '+x for x in limitations)+'\n',encoding='utf-8',newline='\n')
(OUT/'RETEST_RESULTS.md').write_text('# Повторные проверки\n\nЦелевой повтор FE08, FE10, BE03, BE05: 4/4. Корзина, магазины, рабочее пространство, две организации, сохранность после перезапуска и установившийся контраст прошли.\n\n## Исправления тестовой методики\n\n'+'\n'.join('- '+x for x in audit['corrections'])+'\n\n182 готовых JUnit-проверки переиспользованы; новый полный прогон без изменения Java-кода не запускался. Реальный безопасный AI E2E остаётся непройденным из-за 429; ранний небезопасный ответ не принят за его доказательство.\n',encoding='utf-8',newline='\n')

questions='\n'.join('- '+q+' ____________________' for q,_ in description)
blank_front=[[f'F{i:02}', 'Frontend']+['']*8 for i in range(1,6)]
blank_back=[[f'B{i:02}', 'Backend']+['']*8 for i in range(1,6)]
template='# Шаблон внешнего аудита проекта\n\nНе является результатом проверки ARIA или другого проекта. Заполняется только при отдельном поручении.\n\nПроверяемый проект ____________________\n\nАдрес стенда ____________________\n\nВерсия ____________________\n\nПроверяющий ____________________\n\nДата ____________________\n\n## Знакомство\n\n'+questions+'\n\n## Frontend\n\n'+md_table(headers,blank_front)+'\n\n## Backend\n\n'+md_table(headers,blank_back)+'\n\n## Предложения Frontend\n\n1. ____________________\n2. ____________________\n\n## Предложения Backend\n\n1. ____________________\n2. ____________________\n\n## Доказательства\n\nСкриншоты ____________________\n\nЗапросы и HTTP-коды ____________________\n\n## Полученные замечания и решение автора\n\n'+md_table(['Замечание','Обоснование','Решение','Исправление','Повторная проверка'],[['']*5 for _ in range(3)])+'\n\n## Заключение\n\n____________________\n'
(OUT/'CROSS_PROJECT_AUDIT_TEMPLATE.md').write_text(template,encoding='utf-8',newline='\n')
(OUT/'README.md').write_text(f'''# Материалы II рубежного контроля ARIA

Объём: самопроверка собственного проекта, по уточнению пользователя. Чужой проект и чужие замечания не оценивались.

- `RUBEZH2_REPORT.html` — оформленный отчёт для просмотра и переноса в Word; изображения находятся рядом в `EVIDENCE`. Сохраняйте эту папку вместе с HTML.
- `RUBEZH2_REPORT.md` — редактируемый текст с теми же разделами и таблицами.
- `PROJECT_DESCRIPTION.md`, `FRONTEND_AUDIT.md`, `BACKEND_AUDIT.md`, `TEST_RESULTS.md`, `IMPROVEMENTS.md`, `RETEST_RESULTS.md` — отдельные части.
- `CROSS_PROJECT_AUDIT_TEMPLATE.md` — дополнительный пустой шаблон, не результат внешнего аудита.
- `EVIDENCE` — обезличенные фактические ответы API, скриншоты, результаты и контрольные суммы источников JUnit.

Проверен код `{revision}` в `codex/production-foundation`. 27 локальных проверок пройдены; 1 внешний AI-сценарий не пройден. Готовый полный JUnit прогон: 182 теста, все успешны. Повторные сценарии учитываются отдельно.

## Формат Word

Исходное задание допускает Markdown, если нельзя подготовить проверенный DOCX. Штатный `render_docx.py` проверен на исходном документе и завершился `FileNotFoundError: LibreOffice soffice.exe was not found on PATH`. Microsoft Word тоже не установлен. Непроверенный DOCX не выдаётся за готовый. Подготовлены HTML и Markdown; исходные результаты не зависят от последующего экспорта в DOCX. Поля ФИО, группы и преподавателя намеренно оставлены для заполнения.

Для переноса в Word сохраните весь каталог `rubezh2`, затем выберите в Word «Файл → Открыть» и `RUBEZH2_REPORT.html`. Заполните титульный лист и сохраните документ как DOCX. После импорта проверьте ориентацию широких таблиц, переносы и изображения: вёрстка Word может отличаться от проверенной печатной версии HTML.

## Повторить только нужное

На работающем loopback preview: `scripts/rubezh2-audit.cjs`; целевой повтор `--only FE08,FE10,BE03,BE05`; данные после перезапуска `--persistence`. Браузерные зависимости передаются через `ARIA_PLAYWRIGHT_MODULE`. `scripts/rubezh2-visual-review.cjs` проверяет установившийся контраст и ссылки. Полный PostgreSQL JUnit прогон не нужен повторно без изменений Java-кода.

Сборка отчёта из сохранённых доказательств: `python scripts/build-rubezh2-report.py`. Печатная проверка HTML: `node scripts/render-rubezh2-report.cjs`; результат создаётся в `target/rubezh2-render` и не является проверкой DOCX. Все 16 страниц просмотрены, результат зафиксирован в `EVIDENCE/report-layout-check.json`.

Внешний AI после восстановления бесплатного лимита проверять отдельно через `scripts/product-creation-e2e.cjs` с явным `ARIA_E2E_REAL_AI=true`. Не сбрасывать счётчики и не считать ENGINE ответом нейросети. Для полного WooCommerce E2E нужен отдельный реальный тестовый магазин.

Main, рабочий Render и production-база не изменялись. Ключи, пароли, токены, приватная конфигурация и необезличенные логи в комплект не входят.
''',encoding='utf-8',newline='\n')
manifest=dict(codeRevision=revision,date='2026-11-04',frontend=len(front),backend=len(back),backendLocalPassed=sum(r['status']=='Пройден' for r in back),localPassed=passed,externalNotPassed=1,junit=junit['tests'],files={})
for f in sorted(EV.iterdir()):
    if f.is_file():manifest['files'][f.name]=hashlib.sha256(f.read_bytes()).hexdigest()
(OUT/'EVIDENCE_MANIFEST.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8',newline='\n')
print(json.dumps({"codeRevision":revision,"frontend":len(front),"backend":len(back),"passed":passed,"externalNotPassed":1,"junit":182,"report":str(OUT/'RUBEZH2_REPORT.html')},ensure_ascii=False))
