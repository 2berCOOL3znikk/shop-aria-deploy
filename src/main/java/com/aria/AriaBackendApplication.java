package com.aria;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import com.aria.entity.UserProfile;
import com.aria.repository.ProductDetailRepository;
import com.aria.repository.ProductRepository;
import com.aria.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import java.math.RoundingMode;

@SpringBootApplication
public class AriaBackendApplication {

	@Value("${aria.reset-data-on-start:false}")
	private boolean resetDataOnStart;

	public static void main(String[] args) {
		SpringApplication.run(AriaBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(ProductRepository products,
									  UserProfileRepository profiles,
									  ProductDetailRepository details,
									  com.aria.service.AriaService ariaService) {
		return args -> {
			// Рандомизатор для генерации разных данных при каждом запуске
			Random rand = new Random();

			// 1. Пользователь (всегда один)
			if (profiles.count() == 0) {
				profiles.save(new UserProfile("demo@aria.shop", "Иван Петров", "+7 700 000-00-00", "г. Кызылорда, Казахстан"));
				System.out.println("✅ Пользователь создан");
			}

			// 2. Товары — пересоздаём если флаг включён или база пустая
			boolean shouldReset = resetDataOnStart || products.count() == 0;

			if (shouldReset) {
				// Очищаем старые данные
				details.deleteAll();
				products.deleteAll();
				System.out.println("🗑️ Старые товары удалены");

				LocalDate t = LocalDate.now();

				// === ПРОДУКТЫ (категория: Продукты) ===
				Product coffee = products.save(new Product("SKU-1001", "Кофе в зёрнах 1 кг", "Продукты",
						120 + rand.nextInt(50), t.minusDays(20 + rand.nextInt(10)), 40 + rand.nextInt(20),
						new BigDecimal("350"), new BigDecimal("690"), t.plusDays(300)));

				Product tea = products.save(new Product("SKU-1002", "Чай зелёный 250 г", "Продукты",
						80 + rand.nextInt(30), t.minusDays(35 + rand.nextInt(15)), 22 + rand.nextInt(10),
						new BigDecimal("120"), new BigDecimal("260"), t.plusDays(200)));

				Product chocolate = products.save(new Product("SKU-1003", "Шоколад горький 70%", "Продукты",
						150 + rand.nextInt(50), t.minusDays(15 + rand.nextInt(10)), 65 + rand.nextInt(30),
						new BigDecimal("80"), new BigDecimal("180"), t.plusDays(180)));

				Product pasta = products.save(new Product("SKU-1004", "Паста спагетти 500г", "Продукты",
						200 + rand.nextInt(100), t.minusDays(5 + rand.nextInt(10)), 120 + rand.nextInt(50),
						new BigDecimal("40"), new BigDecimal("95"), t.plusDays(365)));

				Product honey = products.save(new Product("SKU-1005", "Мёд цветочный 500г", "Продукты",
						60 + rand.nextInt(30), t.minusDays(45 + rand.nextInt(20)), 15 + rand.nextInt(10),
						new BigDecimal("280"), new BigDecimal("550"), t.plusDays(730)));

				// === ДОМ/ТЕКСТИЛЬ ===
				Product blanket = products.save(new Product("SKU-2001", "Плед шерстяной", "Дом/текстиль",
						60 + rand.nextInt(20), t.minusDays(210 + rand.nextInt(30)), 0,
						new BigDecimal("900"), new BigDecimal("1990"), null));

				Product vase = products.save(new Product("SKU-2002", "Ваза керамическая", "Дом/текстиль",
						40 + rand.nextInt(15), t.minusDays(240 + rand.nextInt(40)), 0,
						new BigDecimal("700"), new BigDecimal("1500"), null));

				Product towel = products.save(new Product("SKU-2003", "Полотенце махровое", "Дом/текстиль",
						100 + rand.nextInt(50), t.minusDays(30 + rand.nextInt(20)), 35 + rand.nextInt(20),
						new BigDecimal("250"), new BigDecimal("590"), null));

				Product pillow = products.save(new Product("SKU-2004", "Подушка ортопедическая", "Дом/текстиль",
						50 + rand.nextInt(25), t.minusDays(195 + rand.nextInt(25)), 2 + rand.nextInt(5),
						new BigDecimal("1200"), new BigDecimal("2490"), null));

				// === ЭЛЕКТРОНИКА ===
				Product gadget = products.save(new Product("SKU-4001", "Гаджет-аксессуар X", "Электроника",
						30 + rand.nextInt(15), t.minusDays(60 + rand.nextInt(20)), 1 + rand.nextInt(3),
						new BigDecimal("1200"), new BigDecimal("1100"), null)); // отрицательная маржа!

				Product headphones = products.save(new Product("SKU-4002", "Наушники беспроводные", "Электроника",
						45 + rand.nextInt(20), t.minusDays(25 + rand.nextInt(15)), 28 + rand.nextInt(15),
						new BigDecimal("2500"), new BigDecimal("4990"), null));

				Product charger = products.save(new Product("SKU-4003", "Зарядное устройство USB-C", "Электроника",
						80 + rand.nextInt(40), t.minusDays(10 + rand.nextInt(10)), 55 + rand.nextInt(25),
						new BigDecimal("450"), new BigDecimal("890"), null));

				// === СКОРОПОРТ (<30 дней до истечения) ===
				Product yogurt = products.save(new Product("SKU-3001", "Йогурт питьевой", "Продукты",
						200 + rand.nextInt(100), t.minusDays(10 + rand.nextInt(5)), 90 + rand.nextInt(40),
						new BigDecimal("28"), new BigDecimal("59"), t.plusDays(12))); // скоропорт!

				Product milk = products.save(new Product("SKU-3002", "Молоко 3.2% 1л", "Продукты",
						150 + rand.nextInt(80), t.minusDays(8 + rand.nextInt(5)), 110 + rand.nextInt(50),
						new BigDecimal("45"), new BigDecimal("89"), t.plusDays(18))); // скоропорт!

				Product cheese = products.save(new Product("SKU-3003", "Сыр твёрдый 300г", "Продукты",
						80 + rand.nextInt(40), t.minusDays(12 + rand.nextInt(8)), 45 + rand.nextInt(25),
						new BigDecimal("180"), new BigDecimal("350"), t.plusDays(25))); // скоропорт!

				// === ПРОМО (нулевая маржа) ===
				Product giftSet = products.save(new Product("SKU-5001", "Подарочный набор", "Промо",
						15 + rand.nextInt(10), t.minusDays(190 + rand.nextInt(20)), 0,
						new BigDecimal("800"), new BigDecimal("800"), null)); // нулевая маржа

				Product sampler = products.save(new Product("SKU-5002", "Дегустационный сет", "Промо",
						25 + rand.nextInt(15), t.minusDays(150 + rand.nextInt(30)), 3 + rand.nextInt(5),
						new BigDecimal("500"), new BigDecimal("500"), null)); // нулевая маржа

				// === ХАРАКТЕРИСТИКИ ===
				details.save(new ProductDetail(coffee, "Lavazza", "Коричневый", "1 кг", new BigDecimal("1.0"), "Арабика высшего сорта, обжарка средняя. Идеально для эспрессо."));
				details.save(new ProductDetail(tea, "Green Leaf", "Зелёный", "250 г", new BigDecimal("0.25"), "Зелёный чай с жасмином, ручной сборки. Собран в провинции Фуцзянь."));
				details.save(new ProductDetail(chocolate, "Lindt", "Тёмный", "100 г", new BigDecimal("0.1"), "Горький шоколад 70% какао. Без пальмового масла."));
				details.save(new ProductDetail(pasta, "Barilla", "Жёлтый", "500 г", new BigDecimal("0.5"), "Паста из твёрдых сортов пшеницы. Время варки 9 минут."));
				details.save(new ProductDetail(honey, "Башкирский", "Янтарный", "500 г", new BigDecimal("0.5"), "Натуральный цветочный мёд. Собран в экологически чистом районе."));
				details.save(new ProductDetail(blanket, "WarmHome", "Бежевый", "150x200 см", new BigDecimal("2.5"), "Шерстяной плед премиум класса, гипоаллергенный. Производство Италия."));
				details.save(new ProductDetail(vase, "CeramicArt", "Белый", "Средний", new BigDecimal("1.2"), "Ручная работа, керамика высокого обжига. Высота 25 см."));
				details.save(new ProductDetail(towel, "HomeTextile", "Голубой", "70x140 см", new BigDecimal("0.4"), "Махровое полотенце из 100% хлопка. Плотность 500 г/м²."));
				details.save(new ProductDetail(pillow, "SleepWell", "Белый", "50x70 см", new BigDecimal("1.5"), "Ортопедическая подушка с эффектом памяти. Гипоаллергенный наполнитель."));
				details.save(new ProductDetail(gadget, "TechPro", "Чёрный", "Универсальный", new BigDecimal("0.15"), "Универсальный адаптер Type-C, поддержка PD 65W. Совместим с MacBook."));
				details.save(new ProductDetail(headphones, "SoundMax", "Чёрный", "One Size", new BigDecimal("0.3"), "Беспроводные наушники с активным шумоподавлением. Время работы 30 часов."));
				details.save(new ProductDetail(charger, "PowerBank", "Белый", "Компактный", new BigDecimal("0.2"), "Зарядное устройство USB-C 65W. GaN технология, компактный размер."));
				details.save(new ProductDetail(yogurt, "Danone", "Белый", "0.5 л", new BigDecimal("0.5"), "Натуральный питьевой йогурт без добавок. Срок хранения 14 дней."));
				details.save(new ProductDetail(milk, "Простоквашино", "Белый", "1 л", new BigDecimal("1.0"), "Молоко пастеризованное 3.2%. Производство Россия."));
				details.save(new ProductDetail(cheese, "Parmalat", "Жёлтый", "300 г", new BigDecimal("0.3"), "Сыр твёрдый выдержанный. Срок созревания 6 месяцев."));
				details.save(new ProductDetail(giftSet, "ARIA", "Разноцветный", "Подарочный", new BigDecimal("1.0"), "Подарочный набор из 5 позиций. Идеально для корпоративных подарков."));
				details.save(new ProductDetail(sampler, "ARIA", "Разноцветный", "Дегустационный", new BigDecimal("0.8"), "Дегустационный сет из 8 мини-порций. Знакомство с ассортиментом."));

				// Демо-цены исходного набора были в RUB. Храним и показываем их в KZT.
				BigDecimal rubToKzt = new BigDecimal("5.36"); // официальный курс НБК на 14.10.2026
				products.findAll().forEach(product -> {
					if (product.getUnitCost() != null) product.setUnitCost(product.getUnitCost().multiply(rubToKzt).setScale(2, RoundingMode.HALF_UP));
					if (product.getUnitPrice() != null) product.setUnitPrice(product.getUnitPrice().multiply(rubToKzt).setScale(2, RoundingMode.HALF_UP));
				});
				products.flush();

				System.out.println("✅ Создано " + products.count() + " товаров в тенге; исходный курс RUB/KZT: 5.36 на 14.10.2026");
			} else {
				System.out.println("ℹ️ Товары уже есть в базе (используйте aria.reset-data-on-start=true для пересоздания)");

				// Авто-анализ сразу после сидов: гарантируем, что при старте ARIA видит
				// свежие товары (на in-memory базе scheduler мог бы сработать по пустой).
				try {
					var rep = ariaService.analyze();
					System.out.println("[SEED] Авто-анализ после сидов: сканировано=" + rep.totalScanned()
							+ " кандидатов=" + rep.markedCandidate() + " срочных=" + rep.urgent());
				} catch (Exception e) {
					System.out.println("[SEED] Авто-анализ пропущен (не критично): " + e.getMessage());
				}
			}
		};
	}
}
