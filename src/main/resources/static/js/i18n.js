// Модуль интернационализации (ru/en/kk) для ARIA Shop
window.ariaI18n = {
    dictionaries: {
        ru: {
            // Навигация
            'nav.catalog': 'Каталог',
            'nav.social': 'Соц. полка',
            'nav.cart': 'Корзина',
            'nav.profile': 'Профиль',
            'nav.admin': 'Панель',
            'nav.login': 'Войти',
            'nav.logout': 'Выйти',
            'nav.theme': 'Тема',
            'nav.lang': 'Язык',

            // Каталог
            'catalog.title': 'Каталог',
            'catalog.search': 'Поиск товаров...',
            'catalog.category': 'Все категории',
            'catalog.sort': 'По умолчанию',
            'catalog.sortPriceAsc': 'Цена: ↓ Дешевле',
            'catalog.sortPriceDesc': 'Цена: ↑ Дороже',
            'catalog.sortPopular': 'Популярные',
            'catalog.addToCart': 'В корзину',
            'catalog.fav': 'В избранное',
            'catalog.favActive': 'В избранном',
            'catalog.details': 'Подробнее',
            'catalog.noProducts': 'Товары не найдены',

            // Карточка товара
            'product.addToCart': 'Добавить в корзину',
            'product.fav': '🤍 В избранное',
            'product.favActive': '❤️ В избранном',
            'product.specs': 'Характеристики',
            'product.info': 'Информация',
            'product.reviews': 'Отзывы покупателей',
            'product.leaveReview': 'Оставить отзыв',
            'product.brand': 'Бренд',
            'product.color': 'Цвет',
            'product.size': 'Размер',
            'product.weight': 'Вес',
            'product.inStock': 'В наличии',
            'product.received': 'Поступил',
            'product.salesMonth': 'Продаж за месяц',
            'product.expiry': 'Срок годности',
            'product.status': 'Статус',
            'product.noReviews': 'Пока нет отзывов. Будьте первым!',
            'product.reviewAuthor': 'Ваше имя',
            'product.reviewComment': 'Комментарий',
            'product.reviewSubmit': 'Отправить отзыв',
            'product.reviewError': 'Введите комментарий',

            // Корзина
            'cart.title': 'Ваша корзина',
            'cart.empty': 'Корзина пуста. ',
            'cart.emptyLink': 'Перейти в каталог',
            'cart.total': 'Итого',
            'cart.checkout': 'Оформить заказ',
            'cart.qty': 'Количество',

            // Профиль
            'profile.title': 'Мой Кабинет',
            'profile.personal': 'Личная информация',
            'profile.email': 'Email',
            'profile.fullName': 'ФИО',
            'profile.phone': 'Телефон',
            'profile.address': 'Адрес',
            'profile.save': 'Сохранить',
            'profile.favorites': 'Избранное',
            'profile.noFavorites': 'Нет избранных товаров',
            'profile.orders': 'Мои заказы',
            'profile.noOrders': 'Заказов пока нет',
            'profile.orderNum': 'Заказ #',

            // Админ
            'admin.title': 'Панель управления',
            'admin.subtitle': 'Интеллектуальное управление складом и социальная помощь',
            'admin.runAnalysis': '🔍 Запустить анализ ARIA',
            'admin.reset': '🔄 Сбросить статусы',
            'admin.createLot': ' Сформировать лот',
            'admin.logs': '📊 История анализов ARIA',
            'admin.noLogs': 'Пока нет логов. Запустите первый анализ.',
            'admin.history': 'История анализов',
            'admin.products': 'Товары для передачи фондам',
            'admin.loading': 'Загрузка данных ARIA...',
            'admin.noProducts': '🎉 Нет товаров для передачи. Отличная работа!',
            'admin.error': 'Ошибка загрузки данных',
            'admin.deadStock': 'Стоимость мёртвого стока',
            'admin.deadStockLabel': 'Товары со статусом КАНДИДАТ',
            'admin.taxBenefit': 'Налоговая выгода',
            'admin.taxBenefitLabel': '~20% потенциальный вычет',
            'admin.saved': 'Спасённых товаров',
            'admin.savedLabel': 'Зарезервировано / Передано',
            'admin.total': 'Всего SKU',
            'admin.active': 'Активных',
            'admin.tableName': 'Товар / SKU',
            'admin.tableStatus': 'Статус',
            'admin.tableDays': 'Дней на складе',
            'admin.tableSales': 'Продаж/мес',
            'admin.tablePrice': 'Наша цена',
            'admin.tableMarket': 'Рыночная',
            'admin.tableReason': 'Причина (AI)',
            'admin.scanned': 'Просканировано товаров',
            'admin.candidates': 'Стало кандидатами',
            'admin.returned': 'Вернулось в актив',
            'admin.urgent': 'Срочных (скоропорт)',
            'admin.price': 'Выше рыночной цены',
            'admin.analysisRunning': 'Анализ ARIA запущен',
            'admin.analysisDone': '✅ Анализ завершён',
            'admin.filter': 'Фильтровать',
            'admin.filterSearch': 'Код/Описание...',
            'admin.filterDateFrom': 'С даты',
            'admin.filterDateTo': 'По дату',
            'admin.filterStatus': 'Все статусы',
            'admin.statusAll': 'Все статусы',
            'admin.statusActive': 'АКТИВЕН',
            'admin.statusCandidate': 'КАНДИДАТ',
            'admin.statusReserved': 'ЗАРЕЗЕРВИРОВАН',
            'admin.statusTransferred': 'ПЕРЕДАН',
            'admin.remove': 'Списать',
            'admin.noResults': 'Нет товаров, соответствующих фильтрам',
            'admin.sku': 'SKU / Код',
            'admin.description': 'Описание',
            'admin.expiry': 'Срок годности',
            'admin.daysLeft': 'Дней осталось',
            'admin.qty': 'Кол-во',
            'admin.location': 'Локация',
            'admin.level': 'Уровень',
            'admin.action': 'Действие',

            // Тосты
            'toast.added': 'Товар добавлен в корзину',
            'toast.removed': 'Товар удалён',
            'toast.favAdd': 'Добавлено в избранное',
            'toast.favRemove': 'Удалено из избранного',
            'toast.needLogin': 'Нужна роль администратора',
            'toast.needAdmin': 'Доступ запрещён: нужна роль ADMIN',
            'toast.error': 'Ошибка',
            'toast.success': 'Успешно',
            'toast.orderPlaced': 'Заказ оформлен!',
            'toast.profileSaved': 'Профиль сохранён',
            'toast.reviewAdded': 'Отзыв добавлен',
            'toast.analysisError': 'Ошибка анализа',
            'toast.resetError': 'Ошибка сброса',
            'toast.forbidden': 'Доступ запрещён',

            // Вход/Регистрация
            'login.title': '◆ ARIAShop',
            'login.tabLogin': 'Вход',
            'login.tabRegister': 'Регистрация',
            'login.email': 'Email',
            'login.password': 'Пароль',
            'login.fullName': 'ФИО',
            'login.submit': 'Войти',
            'login.register': 'Зарегистрироваться',
            'login.noAccount': 'Продолжить без входа — витрина открыта',
            'login.error': 'Неверный email или пароль',
            'login.registerError': 'Ошибка регистрации',
            'login.connectionError': 'Ошибка соединения с сервером',

            // Тема
            'theme.dark': 'Тёмная тема',
            'theme.light': 'Светлая тема',

            // Язык
            'lang.label': 'Язык',
            'lang.ru': 'RU',
            'lang.en': 'EN',
            'lang.kk': 'KK',

            // Empty states
            'empty.cart': 'Корзина пуста',
            'empty.fav': 'Нет избранных товаров',
            'empty.orders': 'Заказов пока нет',
            'empty.reviews': 'Пока нет отзывов. Будьте первым!',
            'empty.search': 'Товары не найдены',
            'empty.candidates': 'Нет товаров для передачи. Отличная работа!',

            // Expiry levels
            'expiry.expired': 'Просрочен',
            'expiry.critical': 'Критично',
            'expiry.warning': 'Внимание',
            'expiry.good': 'Активен',
            'expiry.none': 'Н/Д',

            // Impact stats
            'impact.saved': 'Спасено от списания',
            'impact.social': 'Товаров в соц-разделе',
            'impact.active': 'Активных предложений'
        },

        en: {
            // Navigation
            'nav.catalog': 'Catalog',
            'nav.social': 'Social Shelf',
            'nav.cart': 'Cart',
            'nav.profile': 'Profile',
            'nav.admin': 'Panel',
            'nav.login': 'Login',
            'nav.logout': 'Logout',
            'nav.theme': 'Theme',
            'nav.lang': 'Language',

            // Catalog
            'catalog.title': 'Catalog',
            'catalog.search': 'Search products...',
            'catalog.category': 'All categories',
            'catalog.sort': 'Default',
            'catalog.sortPriceAsc': 'Price: ↓ Lower',
            'catalog.sortPriceDesc': 'Price: ↑ Higher',
            'catalog.sortPopular': 'Popular',
            'catalog.addToCart': 'Add to cart',
            'catalog.fav': 'Add to favorites',
            'catalog.favActive': 'In favorites',
            'catalog.details': 'Details',
            'catalog.noProducts': 'No products found',

            // Product
            'product.addToCart': 'Add to cart',
            'product.fav': '🤍 Add to favorites',
            'product.favActive': '❤️ In favorites',
            'product.specs': 'Specifications',
            'product.info': 'Information',
            'product.reviews': 'Customer reviews',
            'product.leaveReview': 'Leave a review',
            'product.brand': 'Brand',
            'product.color': 'Color',
            'product.size': 'Size',
            'product.weight': 'Weight',
            'product.inStock': 'In stock',
            'product.received': 'Received',
            'product.salesMonth': 'Sales last month',
            'product.expiry': 'Expiry date',
            'product.status': 'Status',
            'product.noReviews': 'No reviews yet. Be the first!',
            'product.reviewAuthor': 'Your name',
            'product.reviewComment': 'Comment',
            'product.reviewSubmit': 'Submit review',
            'product.reviewError': 'Enter a comment',

            // Cart
            'cart.title': 'Your cart',
            'cart.empty': 'Cart is empty. ',
            'cart.emptyLink': 'Go to catalog',
            'cart.total': 'Total',
            'cart.checkout': 'Checkout',
            'cart.qty': 'Quantity',

            // Profile
            'profile.title': 'My Profile',
            'profile.personal': 'Personal information',
            'profile.email': 'Email',
            'profile.fullName': 'Full name',
            'profile.phone': 'Phone',
            'profile.address': 'Address',
            'profile.save': 'Save',
            'profile.favorites': 'Favorites',
            'profile.noFavorites': 'No favorite items',
            'profile.orders': 'My orders',
            'profile.noOrders': 'No orders yet',
            'profile.orderNum': 'Order #',

            // Admin
            'admin.title': 'Control Panel',
            'admin.subtitle': 'Intelligent warehouse management and social assistance',
            'admin.runAnalysis': '🔍 Run ARIA Analysis',
            'admin.reset': '🔄 Reset statuses',
            'admin.createLot': ' Create lot',
            'admin.logs': '📊 ARIA Analysis History',
            'admin.noLogs': 'No logs yet. Run the first analysis.',
            'admin.history': 'Analysis history',
            'admin.products': 'Products for charity transfer',
            'admin.loading': 'Loading ARIA data...',
            'admin.noProducts': '🎉 No products for transfer. Great job!',
            'admin.error': 'Data loading error',
            'admin.deadStock': 'Dead stock value',
            'admin.deadStockLabel': 'Items with CANDIDATE status',
            'admin.taxBenefit': 'Tax benefit',
            'admin.taxBenefitLabel': '~20% potential deduction',
            'admin.saved': 'Saved items',
            'admin.savedLabel': 'Reserved / Transferred',
            'admin.total': 'Total SKUs',
            'admin.active': 'Active',
            'admin.tableName': 'Product / SKU',
            'admin.tableStatus': 'Status',
            'admin.tableDays': 'Days in stock',
            'admin.tableSales': 'Sales/mo',
            'admin.tablePrice': 'Our price',
            'admin.tableMarket': 'Market',
            'admin.tableReason': 'Reason (AI)',
            'admin.scanned': 'Products scanned',
            'admin.candidates': 'Became candidates',
            'admin.returned': 'Returned to active',
            'admin.urgent': 'Urgent (expiring)',
            'admin.price': 'Above market price',
            'admin.analysisRunning': 'ARIA analysis running',
            'admin.analysisDone': '✅ Analysis complete',
            'admin.filter': 'Filter',
            'admin.filterSearch': 'Code/Description...',
            'admin.filterDateFrom': 'From date',
            'admin.filterDateTo': 'To date',
            'admin.filterStatus': 'All statuses',
            'admin.statusAll': 'All statuses',
            'admin.statusActive': 'ACTIVE',
            'admin.statusCandidate': 'CANDIDATE',
            'admin.statusReserved': 'RESERVED',
            'admin.statusTransferred': 'TRANSFERRED',
            'admin.remove': 'Dispose',
            'admin.noResults': 'No products match filters',
            'admin.sku': 'SKU / Code',
            'admin.description': 'Description',
            'admin.expiry': 'Expiry date',
            'admin.daysLeft': 'Days left',
            'admin.qty': 'Quantity',
            'admin.location': 'Location',
            'admin.level': 'Level',
            'admin.action': 'Action',

            // Toasts
            'toast.added': 'Product added to cart',
            'toast.removed': 'Product removed',
            'toast.favAdd': 'Added to favorites',
            'toast.favRemove': 'Removed from favorites',
            'toast.needLogin': 'Admin role required',
            'toast.needAdmin': 'Access denied: ADMIN role required',
            'toast.error': 'Error',
            'toast.success': 'Success',
            'toast.orderPlaced': 'Order placed!',
            'toast.profileSaved': 'Profile saved',
            'toast.reviewAdded': 'Review added',
            'toast.analysisError': 'Analysis error',
            'toast.resetError': 'Reset error',
            'toast.forbidden': 'Access denied',

            // Login/Register
            'login.title': '◆ ARIAShop',
            'login.tabLogin': 'Login',
            'login.tabRegister': 'Register',
            'login.email': 'Email',
            'login.password': 'Password',
            'login.fullName': 'Full name',
            'login.submit': 'Login',
            'login.register': 'Register',
            'login.noAccount': 'Continue without login — showcase is open',
            'login.error': 'Invalid email or password',
            'login.registerError': 'Registration error',
            'login.connectionError': 'Server connection error',

            // Theme
            'theme.dark': 'Dark theme',
            'theme.light': 'Light theme',

            // Language
            'lang.label': 'Language',
            'lang.ru': 'RU',
            'lang.en': 'EN',
            'lang.kk': 'KK',

            // Empty states
            'empty.cart': 'Cart is empty',
            'empty.fav': 'No favorite items',
            'empty.orders': 'No orders yet',
            'empty.reviews': 'No reviews yet. Be the first!',
            'empty.search': 'No products found',
            'empty.candidates': 'No products for transfer. Great job!',

            // Expiry levels
            'expiry.expired': 'Expired',
            'expiry.critical': 'Critical',
            'expiry.warning': 'Warning',
            'expiry.good': 'Active',
            'expiry.none': 'N/A',

            // Impact stats
            'impact.saved': 'Saved from disposal',
            'impact.social': 'Social section items',
            'impact.active': 'Active offers'
        },

        kk: {
            // Навигация
            'nav.catalog': 'Каталог',
            'nav.social': 'Әлеуметтік сөре',
            'nav.cart': 'Себет',
            'nav.profile': 'Профиль',
            'nav.admin': 'Панель',
            'nav.login': 'Кіру',
            'nav.logout': 'Шығу',
            'nav.theme': 'Тақырып',
            'nav.lang': 'Тіл',

            // Каталог
            'catalog.title': 'Каталог',
            'catalog.search': 'Тауарларды іздеу...',
            'catalog.category': 'Барлық санаттар',
            'catalog.sort': 'Әдепкі',
            'catalog.sortPriceAsc': 'Баға: ↓ Төмен',
            'catalog.sortPriceDesc': 'Баға: ↑ Жоғары',
            'catalog.sortPopular': 'Танымал',
            'catalog.addToCart': 'Себетке',
            'catalog.fav': 'Таңдаулыларға',
            'catalog.favActive': 'Таңдаулыларда',
            'catalog.details': 'Толығырақ',
            'catalog.noProducts': 'Тауарлар табылмады',

            // Тауар картасы
            'product.addToCart': 'Себетке қосу',
            'product.fav': '🤍 Таңдаулыларға',
            'product.favActive': '❤️ Таңдаулыларда',
            'product.specs': 'Сипаттамалары',
            'product.info': 'Ақпарат',
            'product.reviews': 'Сатып алушылар пікірлері',
            'product.leaveReview': 'Пікір қалдыру',
            'product.brand': 'Бренд',
            'product.color': 'Түс',
            'product.size': 'Өлшем',
            'product.weight': 'Салмақ',
            'product.inStock': 'Қорында',
            'product.received': 'Түскен',
            'product.salesMonth': 'Айдағы сатылым',
            'product.expiry': 'Жарамдылық мерзімі',
            'product.status': 'Статус',
            'product.noReviews': 'Пікірлер жоқ. Бірінші болыңыз!',
            'product.reviewAuthor': 'Атыңыз',
            'product.reviewComment': 'Комментарий',
            'product.reviewSubmit': 'Пікірді жіберу',
            'product.reviewError': 'Комментарий енгізіңіз',

            // Себет
            'cart.title': 'Сіздің себетіңіз',
            'cart.empty': 'Себет бос. ',
            'cart.emptyLink': 'Каталогқа өту',
            'cart.total': 'Барлығы',
            'cart.checkout': 'Тапсырыс жасау',
            'cart.qty': 'Саны',

            // Профиль
            'profile.title': 'Менің кабинетім',
            'profile.personal': 'Жеке ақпарат',
            'profile.email': 'Email',
            'profile.fullName': 'Аты-жөні',
            'profile.phone': 'Телефон',
            'profile.address': 'Мекен-жай',
            'profile.save': 'Сақтау',
            'profile.favorites': 'Таңдаулылар',
            'profile.noFavorites': 'Таңдаулылар жоқ',
            'profile.orders': 'Тапсырыстарым',
            'profile.noOrders': 'Тапсырыстар жоқ',
            'profile.orderNum': 'Тапсырыс #',

            // Админ
            'admin.title': 'Басқару панелі',
            'admin.subtitle': 'Интеллектуалды қойма басқаруы және әлеуметтік көмек',
            'admin.runAnalysis': '🔍 ARIA талдауын іске қосу',
            'admin.reset': '🔄 Статустарды қалпына келтіру',
            'admin.createLot': ' Лот құру',
            'admin.logs': '📊 ARIA талдау тарихы',
            'admin.noLogs': 'Журналдар жоқ. Бірінші талдауды іске қосыңыз.',
            'admin.history': 'Талдау тарихы',
            'admin.products': 'Қайырымдылыққа берілетін тауарлар',
            'admin.loading': 'ARIA деректерін жүктеу...',
            'admin.noProducts': '🎉 Беру үшін тауарлар жоқ. Жақсы жұмыс!',
            'admin.error': 'Деректерді жүктеу қатесі',
            'admin.deadStock': 'Өлі қор құны',
            'admin.deadStockLabel': 'Кандидат статусы бар тауарлар',
            'admin.taxBenefit': 'Салық пайдасы',
            'admin.taxBenefitLabel': '~20% ықтимал шегерім',
            'admin.saved': 'Құтқарылған тауарлар',
            'admin.savedLabel': 'Резервтелген / Берілген',
            'admin.total': 'Барлық SKU',
            'admin.active': 'Белсенді',
            'admin.tableName': 'Тауар / SKU',
            'admin.tableStatus': 'Статус',
            'admin.tableDays': 'Қоймада күн',
            'admin.tableSales': 'Сатылым/ай',
            'admin.tablePrice': 'Біздің баға',
            'admin.tableMarket': 'Нарық',
            'admin.tableReason': 'Себебі (AI)',
            'admin.scanned': 'Скандалған тауарлар',
            'admin.candidates': 'Кандидат болды',
            'admin.returned': 'Белсендіге қайтты',
            'admin.urgent': 'Жедел (мерзімі өтетін)',
            'admin.price': 'Нарық бағасынан жоғары',
            'admin.analysisRunning': 'ARIA талдауы іске қосылды',
            'admin.analysisDone': '✅ Талдау аяқталды',
            'admin.filter': 'Фильтр',
            'admin.filterSearch': 'Код/Сипаттама...',
            'admin.filterDateFrom': 'Күннен',
            'admin.filterDateTo': 'Күнге дейін',
            'admin.filterStatus': 'Барлық статустар',
            'admin.statusAll': 'Барлық статустар',
            'admin.statusActive': 'БЕЛСЕНДІ',
            'admin.statusCandidate': 'КАНДИДАТ',
            'admin.statusReserved': 'РЕЗЕРВТЕЛГЕН',
            'admin.statusTransferred': 'БЕРІЛДІ',
            'admin.remove': 'Жою',
            'admin.noResults': 'Фильтрлерге сәйкес тауарлар жоқ',
            'admin.sku': 'SKU / Код',
            'admin.description': 'Сипаттама',
            'admin.expiry': 'Жарамдылық мерзімі',
            'admin.daysLeft': 'Қалған күн',
            'admin.qty': 'Саны',
            'admin.location': 'Локация',
            'admin.level': 'Деңгей',
            'admin.action': 'Әрекет',

            // Тосттар
            'toast.added': 'Тауар себетке қосылды',
            'toast.removed': 'Тауар алынды',
            'toast.favAdd': 'Таңдаулыларға қосылды',
            'toast.favRemove': 'Таңдаулылардан алынды',
            'toast.needLogin': 'Әкімші рөлі қажет',
            'toast.needAdmin': 'Қатынауға тыйым салынды: ADMIN рөлі қажет',
            'toast.error': 'Қате',
            'toast.success': 'Сәтті',
            'toast.orderPlaced': 'Тапсырыс орындалды!',
            'toast.profileSaved': 'Профиль сақталды',
            'toast.reviewAdded': 'Пікір қосылды',
            'toast.analysisError': 'Талдау қатесі',
            'toast.resetError': 'Қалпына келтіру қатесі',
            'toast.forbidden': 'Қатынауға тыйым салынды',

            // Кіру/Тіркелу
            'login.title': '◆ ARIAShop',
            'login.tabLogin': 'Кіру',
            'login.tabRegister': 'Тіркелу',
            'login.email': 'Email',
            'login.password': 'Құпия сөз',
            'login.fullName': 'Аты-жөні',
            'login.submit': 'Кіру',
            'login.register': 'Тіркелу',
            'login.noAccount': 'Кірмес-ақ жалғасыңыз — витрина ашық',
            'login.error': 'Email немесе құпия сөз дұрыс емес',
            'login.registerError': 'Тіркелу қатесі',
            'login.connectionError': 'Сервермен байланыс қатесі',

            // Тақырып
            'theme.dark': 'Қараңғы тақырып',
            'theme.light': 'Ақ тақырып',

            // Тіл
            'lang.label': 'Тіл',
            'lang.ru': 'RU',
            'lang.en': 'EN',
            'lang.kk': 'KK',

            // Бос күйлер
            'empty.cart': 'Себет бос',
            'empty.fav': 'Таңдаулылар жоқ',
            'empty.orders': 'Тапсырыстар жоқ',
            'empty.reviews': 'Пікірлер жоқ. Бірінші болыңыз!',
            'empty.search': 'Тауарлар табылмады',
            'empty.candidates': 'Беру үшін тауарлар жоқ. Жақсы жұмыс!',

            // Жарамдылық деңгейлері
            'expiry.expired': 'Мерзімі өтті',
            'expiry.critical': 'Критикалық',
            'expiry.warning': 'Ескерту',
            'expiry.good': 'Белсенді',
            'expiry.none': 'Н/Д',

            // Әсер статистикасы
            'impact.saved': 'Жоюдан құтқарылды',
            'impact.social': 'Әлеуметтік бөлім тауарлары',
            'impact.active': 'Белсенді ұсыныстар'
        }
    },

    getLang: function() {
        return localStorage.getItem('aria_lang') || 'ru';
    },

    t: function(key) {
        const lang = this.getLang();
        const dict = this.dictionaries[lang] || this.dictionaries.ru;
        return dict[key] || this.dictionaries.ru[key] || key;
    },

    setLang: function(code) {
        localStorage.setItem('aria_lang', code);
        this.applyI18n();
        this.updateLangButton();
    },

    cycleLang: function() {
        const langs = ['ru', 'en', 'kk'];
        const current = this.getLang();
        const idx = langs.indexOf(current);
        const next = langs[(idx + 1) % langs.length];
        this.setLang(next);
    },

    updateLangButton: function() {
        const btn = document.getElementById('lang-toggle-btn');
        if (btn) {
            const lang = this.getLang();
            btn.textContent = this.t('lang.' + lang);
        }
    },

    applyI18n: function() {
        // Apply to elements with data-i18n attribute
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            el.textContent = this.t(key);
        });

        // Apply to elements with data-i18n-placeholder attribute
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = this.t(key);
        });
    },

    initI18n: function() {
        this.applyI18n();
        this.updateLangButton();
    }
};

window.i18n = window.ariaI18n;
Object.assign(window.ariaI18n.dictionaries.ru, { 'nav.favorites': '♡ Избранное', 'nav.support': 'Поддержка', 'nav.organization': 'Кабинет организации' });
Object.assign(window.ariaI18n.dictionaries.en, { 'nav.favorites': '♡ Favorites', 'nav.support': 'Support', 'nav.organization': 'Organization cabinet' });
Object.assign(window.ariaI18n.dictionaries.kk, { 'nav.favorites': '♡ Таңдаулылар', 'nav.support': 'Қолдау', 'nav.organization': 'Ұйым кабинеті' });