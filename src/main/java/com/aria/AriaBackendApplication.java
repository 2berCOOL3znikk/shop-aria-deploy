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
				profiles.save(new UserProfile("demo@aria.shop", "Иван Петров", "+7 700 000-00-00", "г. Тараз, Казахстан"));
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
						new BigDecimal("350"), new BigDecimal("12700"), t.plusDays(300)));
				coffee.setImageUrl("/images/products/SKU-1001.jpg");

				Product tea = products.save(new Product("SKU-1002", "Чай зелёный 250 г", "Продукты",
						80 + rand.nextInt(30), t.minusDays(35 + rand.nextInt(15)), 22 + rand.nextInt(10),
						new BigDecimal("120"), new BigDecimal("4100"), t.plusDays(200)));
				tea.setImageUrl("/images/products/SKU-1002.jpg");

				Product chocolate = products.save(new Product("SKU-1003", "Шоколад горький 70%", "Продукты",
						150 + rand.nextInt(50), t.minusDays(15 + rand.nextInt(10)), 65 + rand.nextInt(30),
						new BigDecimal("80"), new BigDecimal("1540"), t.plusDays(180)));
				chocolate.setImageUrl("/images/products/SKU-1003.jpg");

				Product pasta = products.save(new Product("SKU-1004", "Паста спагетти 500г", "Продукты",
						200 + rand.nextInt(100), t.minusDays(5 + rand.nextInt(10)), 120 + rand.nextInt(50),
						new BigDecimal("990"), new BigDecimal("1800"), t.plusDays(365)));
				pasta.setImageUrl("/images/products/SKU-1004.jpg");

				Product honey = products.save(new Product("SKU-1005", "Мёд цветочный 500г", "Продукты",
						60 + rand.nextInt(30), t.minusDays(45 + rand.nextInt(20)), 15 + rand.nextInt(10),
						new BigDecimal("280"), new BigDecimal("3450"), t.plusDays(730)));
				honey.setImageUrl("/images/products/SKU-1005.jpg");

				// === ДОМ/ТЕКСТИЛЬ ===
				Product blanket = products.save(new Product("SKU-2001", "Плед шерстяной", "Дом/текстиль",
						60 + rand.nextInt(20), t.minusDays(210 + rand.nextInt(30)), 0,
						new BigDecimal("900"), new BigDecimal("4990"), null));
				blanket.setImageUrl("/images/products/SKU-2001.jpg");

				Product vase = products.save(new Product("SKU-2002", "Ваза керамическая", "Дом/текстиль",
						40 + rand.nextInt(15), t.minusDays(240 + rand.nextInt(40)), 0,
						new BigDecimal("700"), new BigDecimal("7540"), null));
				vase.setImageUrl("/images/products/SKU-2002.jpg");

				Product towel = products.save(new Product("SKU-2003", "Полотенце махровое", "Дом/текстиль",
						100 + rand.nextInt(50), t.minusDays(30 + rand.nextInt(20)), 35 + rand.nextInt(20),
						new BigDecimal("250"), new BigDecimal("3850"), null));
				towel.setImageUrl("/images/products/SKU-2003.jpg");

				Product pillow = products.save(new Product("SKU-2004", "Подушка ортопедическая", "Дом/текстиль",
						50 + rand.nextInt(25), t.minusDays(195 + rand.nextInt(25)), 2 + rand.nextInt(5),
						new BigDecimal("1200"), new BigDecimal("8550"), null));
				pillow.setImageUrl("/images/products/SKU-2004.jpg");

				// === ЭЛЕКТРОНИКА ===


				Product headphones = products.save(new Product("SKU-4002", "Наушники беспроводные", "Электроника",
						45 + rand.nextInt(20), t.minusDays(25 + rand.nextInt(15)), 28 + rand.nextInt(15),
						new BigDecimal("2500"), new BigDecimal("3600"), null));
				headphones.setImageUrl("/images/products/SKU-4002.jpg");

				Product charger = products.save(new Product("SKU-4003", "Зарядное устройство USB-C", "Электроника",
						80 + rand.nextInt(40), t.minusDays(10 + rand.nextInt(10)), 55 + rand.nextInt(25),
						new BigDecimal("450"), new BigDecimal("1540"), null));
				charger.setImageUrl("/images/products/SKU-4003.jpg");

				// === СКОРОПОРТ (<30 дней до истечения) ===
				Product yogurt = products.save(new Product("SKU-3001", "Йогурт питьевой", "Продукты",
						200 + rand.nextInt(100), t.minusDays(10 + rand.nextInt(5)), 90 + rand.nextInt(40),
						new BigDecimal("28"), new BigDecimal("450"), t.plusDays(12))); // скоропорт!
				yogurt.setImageUrl("/images/products/SKU-3001.jpg");

				Product milk = products.save(new Product("SKU-3002", "Молоко 3.2% 1л", "Продукты",
						150 + rand.nextInt(80), t.minusDays(8 + rand.nextInt(5)), 110 + rand.nextInt(50),
						new BigDecimal("45"), new BigDecimal("750"), t.plusDays(18))); // скоропорт!
				milk.setImageUrl("/images/products/SKU-3002.jpg");

				Product cheese = products.save(new Product("SKU-3003", "Сыр твёрдый 300г", "Продукты",
						80 + rand.nextInt(40), t.minusDays(12 + rand.nextInt(8)), 45 + rand.nextInt(25),
						new BigDecimal("180"), new BigDecimal("2300"), t.plusDays(25))); // скоропорт!
				cheese.setImageUrl("/images/products/SKU-3003.jpg");

				// === ПРОМО (нулевая маржа) ===
				Product giftSet = products.save(new Product("SKU-5001", "Подарочный набор", "Промо",
						15 + rand.nextInt(10), t.minusDays(190 + rand.nextInt(20)), 0,
						new BigDecimal("800"), new BigDecimal("18000"), null)); // нулевая маржа
				giftSet.setImageUrl("/images/products/SKU-5001.jpg");

				Product sampler = products.save(new Product("SKU-5002", "Дегустационный сет", "Промо",
						25 + rand.nextInt(15), t.minusDays(150 + rand.nextInt(30)), 3 + rand.nextInt(5),
						new BigDecimal("500"), new BigDecimal("2890"), null)); // нулевая маржа
				sampler.setImageUrl("/images/products/SKU-5002.jpg");

				// === ХАРАКТЕРИСТИКИ ===
				details.save(new ProductDetail(coffee, "Lavazza", "Коричневый", "1 кг", new BigDecimal("1.0"), "это не просто кофе, а настоящее путешествие к истокам кофейной культуры. Наш бренд создан для тех, кто ценит подлинный вкус, первозданную чистоту продукта и эстетику в каждой детали."));
				details.save(new ProductDetail(tea, "Green Leaf", "Зелёный", "250 г", new BigDecimal("0.25"), "Откройте для себя первозданную свежесть с классическим зелёным чаем от Вердантии. Этот крупнолистовой чай собран вручную на экологически чистых высокогорных плантациях, где утренний туман и ласковое солнце создают идеальные условия для созревания чайного листа."));
				details.save(new ProductDetail(chocolate, "ChocoNoir", "Тёмный", "100 г", new BigDecimal("0.1"), "Шоколад горький 70% «ChocoNoir» — изысканный десерт для истинных ценителей глубокого вкуса какао с тонкими нотками ванили и морской соли."));
				details.save(new ProductDetail(pasta, "Barilla", "Жёлтый", "500 г", new BigDecimal("0.5"), "предлагает классические спагетти в упаковке 500 грамм, изготовленные по традиционному итальянскому рецепту."));
				details.save(new ProductDetail(honey, "Башкирский", "Янтарный", "500 г", new BigDecimal("0.5"), "это натуральный полифлорный мёд, собранный на дикорастущих лугах экологически чистых регионов. Продукт обладает мягким, сладким вкусом с легкой горчинкой в послевкусии и ярким ароматом полевых цветов."));
				details.save(new ProductDetail(blanket, "WarmHome", "Бежевый", "150x200 см", new BigDecimal("2.5"), "1. ренд, делающий акцент на семейных ценностях, домашних вечерах и скандинавском уюте. Пледы из мягкой мериносовой шерсти крупной вязки."));
				details.save(new ProductDetail(vase, "CeramicArt", "Белый", "Средний", new BigDecimal("1.2"), "• Ваза имеет округлую, напоминающую традиционный кувшин форму с изящной ручкой сбоку. Она изготовлена из светлой керамики с характерной мелкозернистой текстурой и вкраплениями, имитирующими шамотную глину или ручную работу."));
				details.save(new ProductDetail(towel, "HomeTextile", "Голубой", "70x140 см", new BigDecimal("0.4"), "Экологичные полотенца из органического чесаного хлопка, вдохновленные природой Скандинавии. Отличаются сдержанной палитрой (оттенки мха, мокрого камня, тумана) и высокой плотностью."));
				details.save(new ProductDetail(pillow, "SleepWell", "Белый", "50x70 см", new BigDecimal("1.5"), "Высокотехнологичные подушки из адаптивной пены с памятью формы, разработанные совместно с сомнологами.\n" +
						"Слоган: Форма вашего идеального сна."));

				details.save(new ProductDetail(headphones, "SoundMax", "Чёрный", "One Size", new BigDecimal("0.3"), "Наушники беспроводные FLARX – портативный аксессуар для прослушивания музыки и общения. С помощью сенсорного управления можно менять трек, регулировать громкость и включать звонки."));
				details.save(new ProductDetail(charger, "PowerBank", "Белый", "Компактный", new BigDecimal("0.2"), "Зарядные устройства USB-C различаются по мощности, количеству портов и поддерживаемым протоколам быстрой зарядки, поэтому выбор зависит от того, какое именно устройство вам нужно заряжать (смартфон, планшет или ноутбук)"));
				details.save(new ProductDetail(yogurt, "Danone", "Белый", "0.5 л", new BigDecimal("0.5"), "Питьевой йогурт натуральный 2% — вкусный и полезный выбор для поддержания здоровья и энергии в течение дня. Этот йогурт не содержит ГМО, что делает его отличным вариантом для тех, кто заботится о своём питании."));
				details.save(new ProductDetail(milk, "Простоквашино", "Белый", "1 л", new BigDecimal("1.0"), "олоко Пармалат с жирностью 3,5% в упаковке объемом 1 литр — это продукт высокого качества, произведенный по современным технологиям. Удобная крышка позволяет легко открывать и закрывать упаковку, обеспечивая длительное хранение без потери свежести. В одной упаковке содержится 12 литров молока, что делает его идеальным выбором для заведений общественного питания, где важно обеспечить постоянный запас свежих молочных продуктов.."));
				details.save(new ProductDetail(cheese, "Parmalat", "Жёлтый", "300 г", new BigDecimal("0.3"), "Сыр Клуб Сыра Витязь 50%, 300 г — насыщенный полутвёрдый сыр с выразительным сливочно-пряным вкусом и лёгкой пикантностью. Отличается плотной, но нежной текстурой, золотисто-жёлтым цветом и богатым ароматом, который раскрывается особенно ярко в нарезке или при плавлении."));
				details.save(new ProductDetail(giftSet, "ARIA", "Разноцветный", "Подарочный", new BigDecimal("1.0"), "Хорошее утро начинается с ароматного кофе и… вкусного шоколада! Разделите приятные моменты лучшего совместного завтрака с любимым человеком. Кофейно-конфетная корзина поможет быстро взбодриться ранним утром, а также придаст энергии благодаря вкусным сладостям. Теперь каждый день станет еще приятнее, ведь счастливое утро с незабываемым завтраком – залог удачного и продуктивного дня!"));
				details.save(new ProductDetail(sampler, "Truq", "Разноцветный", "Дегустационный", new BigDecimal("0.8"), "Ассорти из классического белого, копченого курта (натурального копчения) соленый, сладкого фруктового и медового курта разной формы и жирности. 100% экологический чистый, натуральный продукт. Крафтовая упаковка. Возможность праздничного оформления (лента, открытка)."));

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
