"""Build a small, attributed demonstration reference. Never supplies store facts.

Prices/lots are supplied by a separate explicitly enabled demo importer. Unknown
manufacturer, barcode, ingredients and package sizes remain unknown.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT / 'docs/demo/verified-products.json').read_text(encoding='utf-8-sig'))
labels = {
    'commercialName': 'Наименование', 'brand': 'Бренд', 'netMass': 'Масса нетто',
    'cocoaMinimumPercent': 'Какао, не менее, %', 'energyKcalPer100g': 'Энергия, ккал на 100 г',
    'ingredientSummary': 'Сведения о составе (сокращённо)', 'ingredients': 'Состав по источнику',
    'cookingMinutes': 'Приготовление, мин', 'packageType': 'Упаковка', 'fatPercent': 'Жирность, %',
    'packageQuantity': 'Количество в упаковке', 'packCount': 'Единиц в комплекте',
    'unitNetMass': 'Масса одной единицы', 'tomatoPercent': 'Томаты, %',
    'declaredAllergens': 'Аллергены, указанные источником', 'crossContactWarning': 'Предупреждение источника',
}
units = {'g': 'г', 'kg': 'кг', 'ml': 'мл', 'l': 'л', 'pieces': 'шт', 'tabs': 'шт', 'count': 'шт'}


def display(value):
    if isinstance(value, dict):
        if 'value' in value and 'unit' in value:
            return f"{value['value']} {units.get(value['unit'], value['unit'])}"
        return '; '.join(f'{k}: {display(v)}' for k, v in value.items())
    if isinstance(value, list):
        return ', '.join(display(v) for v in value)
    return str(value)


products = []
for number, original in enumerate(manifest['products'], 1):
    facts = [{
        'label': labels[key], 'value': display(fact['value']),
        'sourceUrl': fact['sourceUrl'], 'checkedAt': fact['checkedDate'],
    } for key, fact in original['verifiedFacts'].items() if key in labels]
    by_key = original['verifiedFacts']
    size = next((display(by_key[k]['value']) for k in ['netMass', 'packageQuantity'] if k in by_key), None)
    if 'packCount' in by_key and 'unitNetMass' in by_key:
        size = f"{display(by_key['packCount']['value'])} × {display(by_key['unitNetMass']['value'])}"
    products.append(dict(
        referenceId=original['referenceId'], sku=f'ARIA-DEMO-{number:03}',
        name=original['suggestedDisplayNameRu'], brand=original['brand'], category=original['categoryForDemo'],
        size=size, description=f"{original['suggestedDisplayNameRu']}. Сведения об упаковке и характеристиках приведены ниже со ссылками на источники.",
        verification='PRIMARY_SOURCE', facts=facts, photo=None,
        limitations=['Проверена только указанная информация. Изготовитель конкретной упаковки и GTIN не подтверждены.',
                     'Состав и предупреждения необходимо сверить с этикеткой перед покупкой. Сокращённые сведения не заменяют маркировку.',
                     'Цена, закупочная стоимость, остаток и сроки демонстрационной партии — учебные значения, не данные реального магазина.'],
    ))

# The visible front label identifies name/brand/type only. No mass/GTIN/ingredient
# or anniversary claim is inferred from a filename, another market or case pack.
photo_source = 'https://commons.wikimedia.org/wiki/File:Heinz_%22Beanz%22_Baked_Beans_In_a_rich_tomato_sauce.jpg'
products.append(dict(
    referenceId='heinz-beanz-photographed-can', sku='ARIA-DEMO-016',
    name='Heinz Beanz — фасоль в томатном соусе', brand='Heinz', category='Консервы', size=None,
    description='Фасоль Heinz Beanz в томатном соусе. Название и вид упаковки сверены по фотографии; масса и состав требуют уточнения.',
    verification='PACKAGE_PHOTO',
    facts=[{'label': 'Наименование на этикетке', 'value': 'Heinz Beanz · In a rich tomato sauce', 'sourceUrl': photo_source, 'checkedAt': '2026-11-04'},
           {'label': 'Вид упаковки', 'value': 'Металлическая банка', 'sourceUrl': photo_source, 'checkedAt': '2026-11-04'}],
    photo={'path': '/images/demo/heinz-beanz-cc0.jpg', 'sourceUrl': photo_source,
           'author': 'AhmedMX in', 'license': 'CC0 1.0', 'licenseUrl': 'https://creativecommons.org/publicdomain/zero/1.0/',
           'note': 'Оригинал без изменений. Подтверждено изображение этой банки; масса, регион поставки и состав не предполагаются.'},
    limitations=['Масса, состав, GTIN и изготовитель конкретной упаковки не подтверждены. Не приравнивается к коробке 24 × 415 г.',
                 'Цена, закупочная стоимость, остаток и даты учебной партии созданы только для демонстрации.'],
))

destination = ROOT / 'src/main/resources/catalogue/demo-products.json'
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps({'schemaVersion': 1, 'entries': products}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
print(f'Prepared {len(products)} references; 1 licensed photograph; no store price, stock, barcode or manufacturer inferred.')
