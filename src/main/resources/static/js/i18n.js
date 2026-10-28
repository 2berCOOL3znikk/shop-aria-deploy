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
        const code = localStorage.getItem('aria_lang') || 'ru';
        return this.dictionaries[code] ? code : 'ru';
    },

    t: function(key) {
        const lang = this.getLang();
        const dict = this.dictionaries[lang] || this.dictionaries.ru;
        return dict[key] || this.dictionaries.ru[key] || key;
    },

    setLang: function(code) {
        if (!this.dictionaries[code]) return;
        localStorage.setItem('aria_lang', code);
        document.documentElement.lang = code;
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
            const value = this.t(key);
            const textNode = Array.from(el.childNodes).find(node => node.nodeType === Node.TEXT_NODE && node.nodeValue.trim());
            if (textNode) textNode.nodeValue = textNode.nodeValue.replace(textNode.nodeValue.trim(), value);
            else if (!el.children.length) el.textContent = value;
        });

        // Apply to elements with data-i18n-placeholder attribute
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = this.t(key);
        });
        this.translatePage(document.body);
    },

    translatePage: function(root) {
        if (!root || !root.ownerDocument && root !== document.body) return;
        const lang = this.getLang();
        if (lang === 'ru') {
            document.documentElement.lang = 'ru';
            if (document.title && document.__ariaTitleOriginal) document.title = document.__ariaTitleOriginal;
        } else if (document.title) {
            if (!document.__ariaTitleOriginal) document.__ariaTitleOriginal = document.title;
            const title = this.uiPhrases[document.__ariaTitleOriginal];
            if (title && title[lang]) document.title = title[lang];
        }
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        let node;
        while ((node = walker.nextNode())) {
            const parent = node.parentElement;
            if (!parent || parent.closest('script,style,textarea,code,[data-i18n],[data-shell-label],[data-shell-footer]')) continue;
            if (!Object.prototype.hasOwnProperty.call(node, '__ariaOriginalText')) node.__ariaOriginalText = node.nodeValue;
            const original = node.__ariaOriginalText;
            const phrase = original.trim();
            let translated = this.uiPhrases[phrase] && this.uiPhrases[phrase][lang];
            if (!translated && lang !== 'ru') {
                const rule = this.uiPatterns.find(item => item.pattern.test(phrase));
                if (rule) translated = rule.translate(phrase, lang);
            }
            if (!translated) {
                if (lang === 'ru' && node.nodeValue !== original) node.nodeValue = original;
                continue;
            }
            const leading = original.match(/^\s*/)[0];
            const trailing = original.match(/\s*$/)[0];
            const value = leading + translated + trailing;
            if (node.nodeValue !== value) node.nodeValue = value;
        }
        root.querySelectorAll && root.querySelectorAll('input[placeholder],textarea[placeholder],[aria-label],button[title]').forEach(el => {
            ['placeholder', 'aria-label', 'title'].forEach(name => {
                if (!el.hasAttribute(name)) return;
                el.__ariaOriginalAttributes = el.__ariaOriginalAttributes || {};
                if (!Object.prototype.hasOwnProperty.call(el.__ariaOriginalAttributes, name)) el.__ariaOriginalAttributes[name] = el.getAttribute(name);
                const original = el.__ariaOriginalAttributes[name];
                const translated = this.uiPhrases[original] && this.uiPhrases[original][lang];
                if (translated) el.setAttribute(name, translated);
                else if (lang === 'ru') el.setAttribute(name, original);
            });
        });
    },

    initI18n: function() {
        document.documentElement.lang = this.getLang();
        this.applyI18n();
        this.updateLangButton();
    }
};

window.i18n = window.ariaI18n;
Object.assign(window.ariaI18n.dictionaries.ru, { 'nav.favorites': '♡ Избранное', 'nav.support': 'Поддержка', 'nav.organization': 'Кабинет организации' });
Object.assign(window.ariaI18n.dictionaries.en, { 'nav.favorites': '♡ Favorites', 'nav.support': 'Support', 'nav.organization': 'Organization cabinet' });
Object.assign(window.ariaI18n.dictionaries.kk, { 'nav.favorites': '♡ Таңдаулылар', 'nav.support': 'Қолдау', 'nav.organization': 'Ұйым кабинеті' });

window.ariaI18n.uiPhrases = {
    'Панель управления': { en: 'Admin panel', kk: 'Басқару панелі' },
    'Интеллектуальное управление складом и социальная помощь': { en: 'Smart inventory management and social support', kk: 'Қойманы ақылды басқару және әлеуметтік көмек' },
    'Получатель акта': { en: 'Transfer document recipient', kk: 'Тапсыру актісін алушы' },
    'Благотворительная организация': { en: 'Charitable organization', kk: 'Қайырымдылық ұйымы' },
    'Запустить анализ ARIA': { en: 'Run ARIA analysis', kk: 'ARIA талдауын іске қосу' },
    'Сформировать акт из отмеченных': { en: 'Create document from selected', kk: 'Таңдалғандардан акт жасау' },
    'История анализов': { en: 'Analysis history', kk: 'Талдау тарихы' },
    'Стоимость мёртвого стока': { en: 'Dead stock value', kk: 'Өтпейтін қор құны' },
    'Остатки по себестоимости': { en: 'Inventory at cost', kk: 'Өзіндік құн бойынша қалдық' },
    'Условная оценка 20%': { en: 'Illustrative 20% estimate', kk: 'Шартты 20% бағалау' },
    'Демонстрационный расчёт по остаткам': { en: 'Demo estimate based on stock', kk: 'Қалдық бойынша демо-есеп' },
    'Позиций в лотах': { en: 'Items in lots', kk: 'Лоттардағы тауарлар' },
    'Зарезервировано / Передано': { en: 'Reserved / transferred', kk: 'Резервте / тапсырылды' },
    'Всего SKU': { en: 'Total SKUs', kk: 'Жалпы SKU' },
    'Активных:': { en: 'Active:', kk: 'Белсенді:' },
    'Состояние ассортимента': { en: 'Inventory status', kk: 'Ассортимент күйі' },
    'Распределение товаров по текущим статусам.': { en: 'Products grouped by current status.', kk: 'Тауарлар ағымдағы мәртебесі бойынша топталған.' },
    'Товары по категориям': { en: 'Products by category', kk: 'Санаттар бойынша тауарлар' },
    'Остатки сгруппированы по каталогу.': { en: 'Stock grouped by catalog category.', kk: 'Қалдықтар каталог санаттары бойынша топталған.' },
    'Контроль запасов и социальных лотов': { en: 'Inventory and social lots', kk: 'Қорлар мен әлеуметтік лоттарды бақылау' },
    'Обычные кандидаты копятся до логистического порога. Срочные позиции с безопасным сроком до 3 дней можно сформировать сразу. Администратор подтверждает фактическую передачу отдельно; просроченное не допускается.': { en: 'Standard candidates accumulate to the logistics threshold. Urgent items with up to three safe days left can be grouped immediately. An administrator confirms the handover separately; expired items are excluded.', kk: 'Қалыпты үміткерлер логистикалық шекке дейін жиналады. Жарамдылық мерзіміне үш күнге дейін қалған шұғыл тауарларды бірден топтастыруға болады. Нақты тапсыруды әкімші бөлек растайды; мерзімі өткен тауарлар қабылданбайды.' },
    'Код/Описание...': { en: 'Code / description…', kk: 'Код / сипаттама…' },
    'С даты': { en: 'From date', kk: 'Басталу күні' },
    'По дату': { en: 'To date', kk: 'Аяқталу күні' },
    'Все статусы': { en: 'All statuses', kk: 'Барлық мәртебе' },
    'АКТИВЕН': { en: 'ACTIVE', kk: 'БЕЛСЕНДІ' },
    'КАНДИДАТ': { en: 'CANDIDATE', kk: 'ҮМІТКЕР' },
    'ЗАРЕЗЕРВИРОВАН': { en: 'RESERVED', kk: 'РЕЗЕРВТЕ' },
    'ПЕРЕДАН': { en: 'TRANSFERRED', kk: 'ТАПСЫРЫЛДЫ' },
    'НА СОЦПОЛКЕ': { en: 'ON SOCIAL SHELF', kk: 'ӘЛЕУМЕТТІК СӨРЕДЕ' },
    'СКИДКА': { en: 'DISCOUNTED', kk: 'ЖЕҢІЛДІК' },
    'СПИСАН': { en: 'WRITTEN OFF', kk: 'ЕСЕПТЕН ШЫҒАРЫЛДЫ' },
    'Фильтровать': { en: 'Filter', kk: 'Сүзу' },
    'В лот': { en: 'Select', kk: 'Лотқа' },
    'SKU / Код': { en: 'SKU / code', kk: 'SKU / код' },
    'Товар': { en: 'Product', kk: 'Тауар' },
    'Срок годности': { en: 'Expiry date', kk: 'Жарамдылық мерзімі' },
    'Дней осталось': { en: 'Days left', kk: 'Қалған күн' },
    'Кол-во': { en: 'Qty.', kk: 'Саны' },
    'Продаж / 30 дн.': { en: 'Sales / 30 days', kk: 'Сатылым / 30 күн' },
    'Маржа': { en: 'Margin', kk: 'Маржа' },
    'Статус': { en: 'Status', kk: 'Мәртебе' },
    'Риск': { en: 'Risk', kk: 'Тәуекел' },
    'Действие': { en: 'Action', kk: 'Әрекет' },
    'Ждут наполнения': { en: 'Waiting for a lot', kk: 'Лотқа жиналуда' },
    'единиц': { en: 'units', kk: 'бірлік' },
    'порог партии': { en: 'lot threshold', kk: 'лот шегі' },
    'В очереди сверх порога:': { en: 'Beyond threshold in queue:', kk: 'Шектен асқан кезек:' },
    'ед.': { en: 'units', kk: 'бірлік' },
    'Сформировать лот из ожидающих товаров': { en: 'Create a lot from waiting items', kk: 'Кезектегі тауарлардан лот жасау' },
    'Партия копится до порога. Передача не запускается автоматически.': { en: 'The lot is accumulating to the threshold. Handover does not start automatically.', kk: 'Лот шекке дейін жиналады. Тапсыру автоматты түрде басталмайды.' },
    'Критические — отправить сейчас': { en: 'Urgent — send now', kk: 'Шұғыл — қазір жіберу' },
    'Срок истёк — передача заблокирована': { en: 'Expired — handover blocked', kk: 'Мерзімі өтті — тапсыру бұғатталған' },
    'Акты в работе': { en: 'Documents in progress', kk: 'Өңделіп жатқан актілер' },
    'Проверка организаций': { en: 'Organization verification', kk: 'Ұйымдарды тексеру' },
    'Партнёры': { en: 'Partners', kk: 'Серіктестер' },
    'Обновить': { en: 'Refresh', kk: 'Жаңарту' },
    'Нет заявок, ожидающих проверки.': { en: 'No applications awaiting review.', kk: 'Тексеруді күтіп тұрған өтінім жоқ.' },
    'Кандидат': { en: 'Candidate', kk: 'Үміткер' },
    'Активен': { en: 'Active', kk: 'Белсенді' },
    'Зарезервирован': { en: 'Reserved', kk: 'Резервте' },
    'Передан': { en: 'Transferred', kk: 'Тапсырылды' },
    'Списан': { en: 'Written off', kk: 'Есептен шығарылды' },
    'Срок истёк': { en: 'Expired', kk: 'Мерзімі өтті' },
    'Минусовая маржа': { en: 'Negative margin', kk: 'Теріс маржа' },
    'Выше рынка': { en: 'Above market', kk: 'Нарықтан жоғары' },
    'Долго лежит': { en: 'Slow-moving', kk: 'Баяу өтеді' },
    'Наблюдение': { en: 'Monitor', kk: 'Бақылау' },
    'На соцполку': { en: 'Publish to social shelf', kk: 'Әлеуметтік сөреге' },
    'Списать': { en: 'Write off', kk: 'Есептен шығару' },
    'Мы на связи': { en: 'We are here to help', kk: 'Біз байланыстамыз' },
    'Поддержка ARIA': { en: 'ARIA support', kk: 'ARIA қолдауы' },
    'Опишите вопрос или впечатление. Команда проекта рассмотрит сообщение; для демо-версии ответ может быть не мгновенным.': { en: 'Tell us what you need or share feedback. The project team will review your message; replies may take time in this demo.', kk: 'Сұрағыңызды немесе пікіріңізді жазыңыз. Жоба тобы хабарламаны қарайды; демо-нұсқада жауап бірден келмеуі мүмкін.' },
    'Обратная связь': { en: 'Feedback', kk: 'Кері байланыс' },
    'Сообщение': { en: 'Message', kk: 'Хабарлама' },
    'Оценка': { en: 'Rating', kk: 'Баға' },
    'Email для ответа (необязательно)': { en: 'Reply email (optional)', kk: 'Жауапқа арналған email (міндетті емес)' },
    'Чем можем помочь?': { en: 'How can we help?', kk: 'Қалай көмектесе аламыз?' },
    'Отправить сообщение': { en: 'Send message', kk: 'Хабарлама жіберу' },
    'Контакты': { en: 'Contact details', kk: 'Байланыс деректері' },
    'Email проекта': { en: 'Project email', kk: 'Жоба email-ы' },
    'Часы связи': { en: 'Contact hours', kk: 'Байланыс уақыты' },
    'Демо: пн–пт, 10:00–18:00 · Кызылорда': { en: 'Demo: Mon–Fri, 10:00–18:00 · Kyzylorda', kk: 'Демо: дс–жм, 10:00–18:00 · Қызылорда' },
    'Частые вопросы': { en: 'Frequently asked questions', kk: 'Жиі қойылатын сұрақтар' },
    'Как оформить возврат?': { en: 'How do I request a return?', kk: 'Қайтаруды қалай рәсімдеуге болады?' },
    'Что означает срок годности на карточке?': { en: 'What does the expiry date on a product card mean?', kk: 'Өнім карточкасындағы жарамдылық мерзімі нені білдіреді?' },
    'Кто может забрать товар с соц. полки?': { en: 'Who can claim an item from the social shelf?', kk: 'Әлеуметтік сөреден тауарды кім ала алады?' },
    'Есть ли доставка в демо?': { en: 'Is delivery available in this demo?', kk: 'Демода жеткізу бар ма?' },
    'Пока нет опубликованных лотов': { en: 'No lots have been published yet', kk: 'Әзірге жарияланған лот жоқ' },
    'Доступные лоты': { en: 'Available lots', kk: 'Қолжетімді лоттар' },
    'Без оплаты · количество указано по складскому учёту': { en: 'Free of charge · quantities follow inventory records', kk: 'Тегін · саны қойма есебі бойынша көрсетілген' },
    'Опубликовать товар': { en: 'Publish an item', kk: 'Тауар жариялау' },
    'Загружаются предложения…': { en: 'Loading offers…', kk: 'Ұсыныстар жүктелуде…' },
    'Забронированные вашей организацией лоты': { en: 'Lots claimed by your organization', kk: 'Ұйымыңыз брондаған лоттар' },
    'Социальная полка': { en: 'Social shelf', kk: 'Әлеуметтік сөре' },
    'Меньше списаний. Больше пользы.': { en: 'Less waste. More impact.', kk: 'Шығын аз. Пайда көп.' },
    'Открыть каталог': { en: 'Open catalog', kk: 'Каталогты ашу' },
    'Как ARIA помогает магазину': { en: 'How ARIA helps stores', kk: 'ARIA дүкенге қалай көмектеседі' },
    'Сроки': { en: 'Expiry', kk: 'Мерзімдер' },
    'Остатки': { en: 'Inventory', kk: 'Қалдықтар' },
    'Цена и маржа': { en: 'Price and margin', kk: 'Баға мен маржа' },
    'Социальное распределение': { en: 'Social distribution', kk: 'Әлеуметтік тарату' },
    'Путь товара': { en: 'Product journey', kk: 'Тауар жолы' },
    'Жизненный цикл акта передачи': { en: 'Transfer document lifecycle', kk: 'Тапсыру актісінің кезеңдері' },
    'Социальный эффект': { en: 'Social impact', kk: 'Әлеуметтік әсер' },
    'товаров спасено от списания': { en: 'items saved from disposal', kk: 'тауар есептен шығарудан сақталды' },
    'товаров в соц-разделе': { en: 'items on social shelf', kk: 'тауар әлеуметтік сөреде' },
    'товаров в обработке ARIA': { en: 'items processed by ARIA', kk: 'тауар ARIA өңдеуінде' },
    'Журнал ARIA': { en: 'ARIA journal', kk: 'ARIA журналы' },
    'Отдельные запуски анализа и их результаты. Товары и события передачи хранятся в своих разделах.': { en: 'Analysis runs and their results. Products and handover events are listed in their own sections.', kk: 'Талдау іске қосулары мен нәтижелері. Тауарлар мен тапсыру оқиғалары жеке бөлімдерде сақталады.' },
    'Поиск по описанию': { en: 'Search descriptions', kk: 'Сипаттамадан іздеу' },
    'Загрузка истории…': { en: 'Loading history…', kk: 'Тарих жүктелуде…' },
    'Пока нет логов. Запустите первый анализ.': { en: 'No logs yet. Run the first analysis.', kk: 'Әзірге жазба жоқ. Алғашқы талдауды іске қосыңыз.' },
    'Показываются предложения ARIA. Администратор подтверждает скидку, списание или передачу; просроченные товары заблокированы.': { en: 'ARIA recommendations are shown here. An administrator confirms discounts, write-offs or transfers; expired products are blocked.', kk: 'Мұнда ARIA ұсыныстары көрсетіледі. Әкімші жеңілдікті, есептен шығаруды немесе тапсыруды растайды; мерзімі өткен тауарлар бұғатталған.' },
    'Товар добавлен в корзину': { en: 'Product added to cart', kk: 'Тауар себетке қосылды' },
    'Добавлено в избранное': { en: 'Added to favorites', kk: 'Таңдаулыларға қосылды' },
    'Удалено из избранного': { en: 'Removed from favorites', kk: 'Таңдаулылардан алынды' },
    'Написать нам': { en: 'Write to us', kk: 'Бізге жазыңыз' },
    'Демо-магазин': { en: 'Demo store', kk: 'Демо дүкен' },
    'ARIA — История анализов': { en: 'ARIA — Analysis history', kk: 'ARIA — Талдау тарихы' },
    'ARIA — Поддержка': { en: 'ARIA — Support', kk: 'ARIA — Қолдау' },
    'ARIA — О проекте': { en: 'ARIA — About the project', kk: 'ARIA — Жоба туралы' },
    'ARIA — Социальная полка': { en: 'ARIA — Social shelf', kk: 'ARIA — Әлеуметтік сөре' },
    'Выйти': { en: 'Sign out', kk: 'Шығу' },
    '🔍 Запустить анализ ARIA': { en: '🔍 Run ARIA analysis', kk: '🔍 ARIA талдауын іске қосу' },
    'Анализ ARIA запущен': { en: 'ARIA analysis is running', kk: 'ARIA талдауы орындалуда' },
    'Сканирование товаров...': { en: 'Scanning products…', kk: 'Тауарлар сканерленуде…' },
    'Проверка мёртвого стока (>180 дней)...': { en: 'Checking slow-moving stock (>180 days)…', kk: 'Баяу өтетін қор тексерілуде (>180 күн)…' },
    'Проверка срока годности...': { en: 'Checking expiry dates…', kk: 'Жарамдылық мерзімі тексерілуде…' },
    'Анализ маржинальности...': { en: 'Calculating sales margin…', kk: 'Сату маржасы есептелуде…' },
    'Сравнение с рынком...': { en: 'Comparing with the market…', kk: 'Нарықпен салыстырылуда…' },
    'Сохранение результатов...': { en: 'Saving results…', kk: 'Нәтижелер сақталуда…' },
    'Готово': { en: 'Done', kk: 'Дайын' },
    'Просканировано товаров:': { en: 'Products scanned:', kk: 'Сканерленген тауарлар:' },
    'Стало кандидатами:': { en: 'New candidates:', kk: 'Жаңа үміткерлер:' },
    'Вернулось в актив:': { en: 'Returned to active:', kk: 'Белсендіге қайтарылды:' },
    'Срочных (скоропорт):': { en: 'Urgent (perishable):', kk: 'Шұғыл (тез бұзылатын):' },
    'Выше рыночной цены:': { en: 'Above market price:', kk: 'Нарық бағасынан жоғары:' },
    'Со скидкой': { en: 'Discounted', kk: 'Жеңілдікпен' },
    'Активны': { en: 'Active', kk: 'Белсенді' },
    'Списаны': { en: 'Written off', kk: 'Есептен шығарылды' },
    'Переданы': { en: 'Transferred', kk: 'Тапсырылды' },
    'Кандидаты': { en: 'Candidates', kk: 'Үміткерлер' },
    'Продукты': { en: 'Groceries', kk: 'Азық-түлік' },
    'Дом/текстиль': { en: 'Home & textiles', kk: 'Үй және тоқыма' },
    'Электроника': { en: 'Electronics', kk: 'Электроника' },
    'Промо': { en: 'Promotions', kk: 'Науқандық тауарлар' },
    'Скоропорт': { en: 'Perishables', kk: 'Тез бұзылатындар' },
    'Мёртвый сток': { en: 'Slow-moving stock', kk: 'Баяу өтетін қор' },
    'Ценовые риски': { en: 'Price risks', kk: 'Баға тәуекелдері' },
    'Отриц. маржа': { en: 'Negative margin', kk: 'Теріс маржа' },
    'Все': { en: 'All', kk: 'Барлығы' },
    'Соц. полка': { en: 'Social shelf', kk: 'Әлеуметтік сөре' },
    'Передача запасов': { en: 'Inventory handover', kk: 'Қорды тапсыру' },
    'Логистика лотов': { en: 'Lot logistics', kk: 'Лот логистикасы' },
    'СКОРО ИСТЕКАЕТ': { en: 'EXPIRING SOON', kk: 'МЕРЗІМІ ЖАҚЫН' },
    'СРОЧНО · ПЕРЕДАТЬ СЕЙЧАС': { en: 'URGENT · TRANSFER NOW', kk: 'ШҰҒЫЛ · ҚАЗІР ТАПСЫРУ' },
    'ВЫШЕ РЫНКА': { en: 'ABOVE MARKET', kk: 'НАРЫҚТАН ЖОҒАРЫ' },
    'ОТРИЦАТЕЛЬНАЯ МАРЖА': { en: 'NEGATIVE MARGIN', kk: 'ТЕРІС МАРЖА' },
    'ДОЛГО ЛЕЖИТ': { en: 'SLOW-MOVING', kk: 'БАЯУ ӨТЕДІ' },
    'НАБЛЮДЕНИЕ': { en: 'MONITOR', kk: 'БАҚЫЛАУ' },
    'Н/Д': { en: 'N/A', kk: 'Д/Ж' },
    'Скоро истекает': { en: 'Expiring soon', kk: 'Мерзімі жақын' },
    'Срочно · передать сейчас': { en: 'Urgent · transfer now', kk: 'Шұғыл · қазір тапсыру' },
    'отрицательная маржа': { en: 'negative margin', kk: 'теріс маржа' },
    'срок годности через': { en: 'expires in', kk: 'жарамдылық мерзімі аяқталады' },
    'Управление': { en: 'Management', kk: 'Басқару' },
    'Акты передачи': { en: 'Transfer documents', kk: 'Тапсыру актілері' },
    'Состав лота': { en: 'Lot contents', kk: 'Лот құрамы' },
    'Получатель акта': { en: 'Document recipient', kk: 'Акт алушысы' },
    'Обновить': { en: 'Refresh', kk: 'Жаңарту' },
    'Опубликовать товар': { en: 'Publish item', kk: 'Тауар жариялау' },
    'Все статусы': { en: 'All statuses', kk: 'Барлық мәртебе' },
    'Условная оценка 20%': { en: 'Illustrative 20% estimate', kk: 'Шартты 20% бағалау' },
    'Фонд': { en: 'Charity', kk: 'Қор' },
    'Кофе в зёрнах 1 кг': { en: 'Coffee beans, 1 kg', kk: 'Дәнді кофе, 1 кг' },
    'Чай зелёный 250 г': { en: 'Green tea, 250 g', kk: 'Жасыл шай, 250 г' },
    'Шоколад горький 70%': { en: 'Dark chocolate 70%', kk: 'Ащы шоколад 70%' },
    'Паста спагетти 500г': { en: 'Spaghetti pasta, 500 g', kk: 'Спагетти макароны, 500 г' },
    'Мёд цветочный 500г': { en: 'Flower honey, 500 g', kk: 'Гүл балы, 500 г' },
    'Плед шерстяной': { en: 'Wool blanket', kk: 'Жүн көрпе' },
    'Ваза керамическая': { en: 'Ceramic vase', kk: 'Керамикалық құмыра' },
    'Полотенце махровое': { en: 'Terry towel', kk: 'Мақта сүлгі' },
    'Подушка ортопедическая': { en: 'Orthopedic pillow', kk: 'Ортопедиялық жастық' },
    'Гаджет-аксессуар X': { en: 'Gadget accessory X', kk: 'X гаджет аксессуары' },
    'Наушники беспроводные': { en: 'Wireless headphones', kk: 'Сымсыз құлаққап' },
    'Зарядное устройство USB-C': { en: 'USB-C charger', kk: 'USB-C қуаттағышы' },
    'Йогурт питьевой': { en: 'Drinking yogurt', kk: 'Ішуге арналған йогурт' },
    'Молоко 3.2% 1л': { en: 'Milk 3.2%, 1 L', kk: 'Сүт 3,2%, 1 л' },
    'Сыр твёрдый 300г': { en: 'Hard cheese, 300 g', kk: 'Қатты ірімшік, 300 г' },
    'Подарочный набор': { en: 'Gift set', kk: 'Сыйлық жиынтығы' },
    'Дегустационный сет': { en: 'Tasting set', kk: 'Дәм татуға арналған жиынтық' },
    'Коричневый': { en: 'Brown', kk: 'Қоңыр' },
    'Зелёный': { en: 'Green', kk: 'Жасыл' },
    'Тёмный': { en: 'Dark', kk: 'Қою' },
    'Жёлтый': { en: 'Yellow', kk: 'Сары' },
    'Янтарный': { en: 'Amber', kk: 'Кәріптас түсті' },
    'Бежевый': { en: 'Beige', kk: 'Беж' },
    'Белый': { en: 'White', kk: 'Ақ' },
    'Голубой': { en: 'Light blue', kk: 'Көгілдір' },
    'Чёрный': { en: 'Black', kk: 'Қара' },
    'Разноцветный': { en: 'Multicolour', kk: 'Түрлі түсті' },
    'Сформировать акт': { en: 'Create transfer document', kk: 'Тапсыру актісін жасау' },
    'История передач': { en: 'Transfer history', kk: 'Тапсыру тарихы' },
    'Получатель, дата, позиции, итоговая сумма и статус акта.': { en: 'Recipient, date, items, total amount and document status.', kk: 'Алушы, күні, тауарлар, жалпы сома және акт мәртебесі.' },
    'Остатки · CSV': { en: 'Inventory · CSV', kk: 'Қалдықтар · CSV' },
    'Передачи · CSV': { en: 'Transfers · CSV', kk: 'Тапсырулар · CSV' },
    'Передач пока нет. Созданные лоты появятся здесь после первого действия.': { en: 'No transfers yet. Created lots will appear here after the first action.', kk: 'Әзірге тапсыру жоқ. Жасалған лоттар алғашқы әрекеттен кейін осында көрінеді.' },
    'Подключить фонд или приют': { en: 'Add a charity or shelter', kk: 'Қорды немесе панажайды қосу' },
    'Создайте учётную запись партнёра. После входа организация сможет забрать опубликованный лот на соцполке.': { en: 'Create a partner account. After signing in, the organization can claim a published social-shelf lot.', kk: 'Серіктес аккаунтын жасаңыз. Ұйым жүйеге кіргеннен кейін әлеуметтік сөредегі жарияланған лотты ала алады.' },
    'Название фонда / приюта': { en: 'Charity / shelter name', kk: 'Қордың / панажайдың атауы' },
    'Рабочий email': { en: 'Work email', kk: 'Жұмыс email-ы' },
    'Временный пароль, от 8 символов': { en: 'Temporary password, at least 8 characters', kk: 'Уақытша құпиясөз, кемінде 8 таңба' },
    'Создать доступ организации': { en: 'Create organization access', kk: 'Ұйымға қолжетімділік жасау' },
    'Проверка организации': { en: 'Organization verification', kk: 'Ұйымды тексеру' },
    'Название организации': { en: 'Organization name', kk: 'Ұйым атауы' },
    'Регистрационный номер': { en: 'Registration number', kk: 'Тіркеу нөмірі' },
    'Отправить на проверку': { en: 'Submit for review', kk: 'Тексеруге жіберу' },
    'Заявка отправлена администратору': { en: 'Application sent to the administrator', kk: 'Өтінім әкімшіге жіберілді' },
    'Сначала отправьте заявку на проверку организации.': { en: 'Submit your organization for verification first.', kk: 'Алдымен ұйымды тексеруге жіберіңіз.' },
    'Организация проверена: бронирование доступно.': { en: 'Organization verified: claiming is available.', kk: 'Ұйым тексерілді: брондауға болады.' },
    'Доступ по льготе — войдите как партнёр.': { en: 'Partner access — sign in with an organization account.', kk: 'Серіктестерге арналған қолжетімділік — ұйым аккаунтымен кіріңіз.' },
    'Доступ по льготе': { en: 'Partner access', kk: 'Серіктестерге арналған қолжетімділік' },
    'Количество:': { en: 'Quantity:', kk: 'Саны:' },
    'Предложение до:': { en: 'Offer valid until:', kk: 'Ұсыныс мерзімі:' },
    'Годен до:': { en: 'Expires:', kk: 'Жарамдылық мерзімі:' },
    'не указано': { en: 'not specified', kk: 'көрсетілмеген' },
    'не указан': { en: 'not specified', kk: 'көрсетілмеген' },
    'Приоритетный лот': { en: 'Priority lot', kk: 'Басым лот' },
    'Забронировать': { en: 'Claim', kk: 'Брондау' },
    'Передача без оплаты': { en: 'Free transfer', kk: 'Тегін тапсыру' },
    'Бронирование доступно проверенным организациям.': { en: 'Claiming is available to verified organizations.', kk: 'Брондау тексерілген ұйымдарға қолжетімді.' },
    'Пока нет опубликованных лотов': { en: 'No lots have been published yet', kk: 'Әзірге жарияланған лот жоқ' },
    'Администратор подтвердит публикацию после проверки товара и срока годности.': { en: 'An administrator will approve publication after checking the product and expiry date.', kk: 'Әкімші тауар мен жарамдылық мерзімін тексергеннен кейін жариялауды растайды.' },
    'Сначала дождитесь проверки организации': { en: 'Wait for organization verification first', kk: 'Алдымен ұйымның тексерілуін күтіңіз' },
    'Лот забронирован': { en: 'Lot claimed', kk: 'Лот брондалды' },
    'Почта проекта': { en: 'Project email', kk: 'Жоба email-ы' },
    'Товар списан': { en: 'Product written off', kk: 'Тауар есептен шығарылды' },
    'Показать ещё': { en: 'Show more', kk: 'Тағы көрсету' },
    'Показать меньше': { en: 'Show less', kk: 'Азырақ көрсету' },
    'В каталог': { en: 'Browse catalog', kk: 'Каталогқа өту' },
    'Добавить в корзину': { en: 'Add to cart', kk: 'Себетке қосу' },
    'Вернуться в каталог': { en: 'Return to catalog', kk: 'Каталогқа қайту' },
    'Оформить заказ': { en: 'Place order', kk: 'Тапсырысты рәсімдеу' },
    'Заказ оформлен!': { en: 'Order placed!', kk: 'Тапсырыс рәсімделді!' },
    'Товар добавлен в корзину': { en: 'Product added to cart', kk: 'Тауар себетке қосылды' },
    'Товар удалён': { en: 'Product removed', kk: 'Тауар себеттен алынды' },
    'Избранное': { en: 'Favorites', kk: 'Таңдаулылар' },
    'Назад в каталог': { en: 'Back to catalog', kk: 'Каталогқа қайту' },
    '← Назад в каталог': { en: '← Back to catalog', kk: '← Каталогқа қайту' },
    'Отзывы покупателей': { en: 'Customer reviews', kk: 'Сатып алушылар пікірлері' },
    'Оставить отзыв': { en: 'Leave a review', kk: 'Пікір қалдыру' },
    'Перейти в каталог': { en: 'Go to catalog', kk: 'Каталогқа өту' },
    'Сохранить': { en: 'Save', kk: 'Сақтау' },
    'Личная информация': { en: 'Personal information', kk: 'Жеке ақпарат' },
    'Мои заказы': { en: 'My orders', kk: 'Тапсырыстарым' },
    'Нет избранных товаров': { en: 'No favorite products', kk: 'Таңдаулы тауарлар жоқ' },
    'Заказов пока нет': { en: 'No orders yet', kk: 'Әзірге тапсырыс жоқ' },
    'Вернуться в магазин': { en: 'Back to the store', kk: 'Дүкенге қайту' },
    'Подробнее': { en: 'Details', kk: 'Толығырақ' },
    'Пока нет отзывов. Будьте первым!': { en: 'No reviews yet. Be the first!', kk: 'Әзірге пікір жоқ. Бірінші болып жазыңыз!' },
    'Товар не найден': { en: 'Product not found', kk: 'Тауар табылмады' },
    'Каталог открыт для просмотра.': { en: 'The catalog is open to browse.', kk: 'Каталогты қарауға болады.' },
    'Поддержка': { en: 'Support', kk: 'Қолдау' },
    'Розничный учёт · Казахстан': { en: 'Retail inventory · Kazakhstan', kk: 'Бөлшек сауда есебі · Қазақстан' },
    'Остаткам — разумное решение,': { en: 'A smarter decision for stock,', kk: 'Қалдыққа — тиімді шешім,' },
    'помощи — понятный путь': { en: 'a clear path to help', kk: 'көмекке — нақты жол' },
    'ARIA ежедневно проверяет сроки годности, скорость продаж и маржу. Магазин может выставить отобранные товары на социальную полку, а фонд — забронировать партию и скачать черновик акта передачи.': { en: 'ARIA checks expiry dates, sales velocity and margins every day. Stores can publish selected items to the social shelf, and charities can claim lots and download a draft transfer document.', kk: 'ARIA күн сайын жарамдылық мерзімін, сату жылдамдығын және маржаны тексереді. Дүкен таңдалған тауарларды әлеуметтік сөреге жариялай алады, ал қор лотты брондап, тапсыру актісінің жобасын жүктей алады.' },
    'Открыть управление': { en: 'Open management', kk: 'Басқаруды ашу' },
    'Контроль товарного запаса': { en: 'Inventory control', kk: 'Тауар қорын бақылау' },
    'Ежедневная проверка срока годности, дней хранения, продаж за 30 дней и маржи. Срочные товары выделяются отдельно.': { en: 'Daily checks of expiry dates, days in stock, 30-day sales and margin. Urgent items are highlighted.', kk: 'Жарамдылық мерзімі, қоймадағы күндер, 30 күндік сатылым және маржа күн сайын тексеріледі. Шұғыл тауарлар бөлек белгіленеді.' },
    'Проверенные товары можно предложить фондам и приютам. Организации входят по отдельной учётной записи и бронируют доступный лот.': { en: 'Verified items can be offered to charities and shelters. Organizations sign in with their own account and claim an available lot.', kk: 'Тексерілген тауарларды қорлар мен панажайларға ұсынуға болады. Ұйымдар жеке аккаунтпен кіріп, қолжетімді лотты брондайды.' },
    'Документы передачи': { en: 'Transfer documents', kk: 'Тапсыру құжаттары' },
    'Для выбранной партии формируется черновик акта с номенклатурой, количеством, стоимостью и местом для подписей.': { en: 'A draft document is created for the selected lot with item names, quantities, value and signature fields.', kk: 'Таңдалған лотқа тауар атаулары, саны, құны және қол қою орындары бар акт жобасы жасалады.' },
    'Социальная полка': { en: 'Social shelf', kk: 'Әлеуметтік сөре' },
    'Демо-проект': { en: 'Demo project', kk: 'Демо-жоба' },
    'ARIA использует учебные данные, чтобы показать работу розничного каталога и социальных лотов.': { en: 'ARIA uses sample data to demonstrate a retail catalog and social lots.', kk: 'ARIA бөлшек сауда каталогы мен әлеуметтік лоттардың жұмысын көрсету үшін оқу деректерін пайдаланады.' },
    'Какие данные используются': { en: 'Data used', kk: 'Қолданылатын деректер' },
    'Для входа сохраняются email, роль и токен в браузере. Профиль может содержать имя, телефон и адрес, если пользователь сам внесёт их. Форма поддержки отправляет сообщение, оценку и необязательный email.': { en: 'Email, role and token are stored in the browser for sign-in. The profile can contain a name, phone number and address entered by the user. The support form submits a message, rating and optional email.', kk: 'Кіру үшін email, рөл және токен браузерде сақталады. Профильде пайдаланушы өзі енгізген аты, телефоны және мекенжайы болуы мүмкін. Қолдау формасы хабарлама, баға және міндетті емес email жібереді.' },
    'Кому доступны данные': { en: 'Who can access the data', kk: 'Деректер кімге қолжетімді' },
    'Профиль и заказы доступны только вошедшему пользователю. Заявки на проверку организации и сообщения поддержки видит администратор. Публичная соц. полка показывает данные о лоте и товаре, но не раскрывает личность получателя.': { en: 'Only the signed-in user can access their profile and orders. Administrators can see organization verification requests and support messages. The public social shelf shows lot and product details, not the recipient’s identity.', kk: 'Профиль мен тапсырыстар тек кірген пайдаланушыға қолжетімді. Ұйымды тексеру өтінімдері мен қолдау хабарламаларын әкімші көреді. Ашық әлеуметтік сөреде лот пен тауар деректері көрсетіледі, алушының тұлғасы ашылмайды.' },
    'Хранение в демо': { en: 'Demo storage', kk: 'Демода сақтау' },
    'Учебная конфигурация использует локальную базу H2 в памяти и очищает её при остановке приложения. Запросы не отправляются в рекламные или аналитические сервисы.': { en: 'The demo uses a local in-memory H2 database that is cleared when the app stops. Requests are not sent to advertising or analytics services.', kk: 'Демода қолданба тоқтағанда тазаланатын жергілікті H2 жады базасы қолданылады. Сұраулар жарнама немесе аналитика қызметтеріне жіберілмейді.' },
    'Ограничения': { en: 'Limitations', kk: 'Шектеулер' },
    'Не вводите реальные персональные, платёжные или медицинские данные. Перед пилотным запуском правила хранения и доступа нужно согласовать отдельно.': { en: 'Do not enter real personal, payment or medical data. Storage and access rules must be agreed before a pilot launch.', kk: 'Нақты жеке, төлем немесе медициналық деректерді енгізбеңіз. Сынақ іске қосу алдында сақтау және қолжетімділік ережелерін бөлек келісу керек.' },
    'Задать вопрос по конфиденциальности': { en: 'Ask a privacy question', kk: 'Құпиялылық туралы сұрақ қою' },
    'Каждый переход отражает отдельное решение магазина или подтверждённую операцию.': { en: 'Each transition represents a separate store decision or a confirmed operation.', kk: 'Әр ауысу дүкеннің жеке шешімін немесе расталған әрекетті білдіреді.' },
    'AI предлагает действие. Человек проверяет данные и подтверждает решение.': { en: 'AI suggests an action. A person checks the data and confirms the decision.', kk: 'AI әрекет ұсынады. Адам деректерді тексеріп, шешімді растайды.' },
    'Система сама не оценивает безопасность еды и не подтверждает факт передачи.': { en: 'The system does not assess food safety or confirm that a handover took place.', kk: 'Жүйе тағам қауіпсіздігін өздігінен бағаламайды және тапсыру фактісін растамайды.' },
    'Каждый этап подтверждает человек: магазин формирует лот, сверяет документ, обе стороны подписывают акт, после фактической выдачи администратор фиксирует передачу.': { en: 'A person confirms every stage: the store creates a lot and checks the document, both parties sign it, and an administrator records the handover after it takes place.', kk: 'Әр кезеңді адам растайды: дүкен лот жасап, құжатты тексереді, екі тарап актіге қол қояды, ал нақты берілгеннен кейін әкімші тапсыруды тіркейді.' },
    'Счётчики отражают фактические статусы и опубликованные лоты демо-склада.': { en: 'Counters reflect actual statuses and published lots in the demo inventory.', kk: 'Санағыштар демо-қоймадағы нақты мәртебелер мен жарияланған лоттарды көрсетеді.' },
    'Чем отличается возврат?': { en: 'How do returns work?', kk: 'Қайтару қалай жасалады?' },
    'В демо-магазине заказ создаёт запись для показа процесса. Реальный возврат согласуется с продавцом по правилам конкретного товара.': { en: 'In the demo store, an order creates a record to show the flow. Real returns must be agreed with the seller under the rules for that product.', kk: 'Демо дүкенде тапсырыс процесті көрсету үшін жазба жасайды. Нақты қайтаруды тауар ережелеріне сай сатушымен келісу керек.' },
    'Что означает срок годности на карточке?': { en: 'What does the expiry date on a product card mean?', kk: 'Өнім карточкасындағы жарамдылық мерзімі нені білдіреді?' },
    'Это дата из складских данных. ARIA показывает риск и предлагает действие; сотрудник проверяет товар перед решением.': { en: 'This date comes from inventory records. ARIA highlights risks and suggests an action; a staff member checks the item before deciding.', kk: 'Бұл күн қойма деректерінен алынады. ARIA тәуекелді көрсетіп, әрекет ұсынады; қызметкер шешім алдында тауарды тексереді.' },
    'Кто может забрать товар с соц. полки?': { en: 'Who can claim an item from the social shelf?', kk: 'Әлеуметтік сөреден тауарды кім ала алады?' },
    'Просматривать предложения могут все. Бронировать их могут только организации после проверки администратором.': { en: 'Anyone can browse offers. Only organizations verified by an administrator can claim them.', kk: 'Ұсыныстарды кез келген адам қарай алады. Оларды тек әкімші тексерген ұйымдар брондай алады.' },
    'Есть ли доставка в демо?': { en: 'Is delivery available in the demo?', kk: 'Демода жеткізу бар ма?' },
    'Форма заказа сохраняет демонстрационный адрес и не запускает реальную доставку или оплату.': { en: 'The order form saves a demo address and does not start a real delivery or payment.', kk: 'Тапсырыс формасы демо мекенжайын сақтайды, нақты жеткізу не төлем басталмайды.' },
    'Your choice': { en: 'Your choice', kk: 'Сіздің таңдауыңыз' },
    'Ваш выбор': { en: 'Your choices', kk: 'Таңдаулыларыңыз' },
    'Товары, которые вы сохранили на потом.': { en: 'Products you saved for later.', kk: 'Кейінге сақтаған тауарларыңыз.' },
    'Загружается избранное…': { en: 'Loading favorites…', kk: 'Таңдаулылар жүктелуде…' },
    'Загружается каталог…': { en: 'Loading catalog…', kk: 'Каталог жүктелуде…' },
    'Загружается товар…': { en: 'Loading product…', kk: 'Тауар жүктелуде…' },
    'Ваша корзина': { en: 'Your cart', kk: 'Себетіңіз' },
    'Ваша корзина пуста': { en: 'Your cart is empty', kk: 'Себетіңіз бос' },
    'Итого:': { en: 'Total:', kk: 'Барлығы:' },
    'Количество': { en: 'Quantity', kk: 'Саны' },
    'Обновить вопрос': { en: 'Refresh question', kk: 'Сұрақты жаңарту' },
    'Ответ': { en: 'Answer', kk: 'Жауап' },
    '5 — отлично': { en: '5 — excellent', kk: '5 — өте жақсы' },
    '4 — хорошо': { en: '4 — good', kk: '4 — жақсы' },
    '3 — нормально': { en: '3 — okay', kk: '3 — қалыпты' },
    '2 — есть проблемы': { en: '2 — needs improvement', kk: '2 — жақсарту керек' },
    '1 — плохо': { en: '1 — poor', kk: '1 — нашар' },
    'Профиль сохранён': { en: 'Profile saved', kk: 'Профиль сақталды' },
    'Отзыв добавлен': { en: 'Review added', kk: 'Пікір қосылды' },
    'Заявка отклонена. Исправьте данные и отправьте её повторно.': { en: 'The application was declined. Correct the details and submit it again.', kk: 'Өтінім қабылданбады. Деректерді түзетіп, қайта жіберіңіз.' },
    'Заявка на проверке. Бронирование станет доступно после решения администратора.': { en: 'The application is under review. Claiming will be available after the administrator decides.', kk: 'Өтінім тексерілуде. Әкімші шешімінен кейін брондау қолжетімді болады.' },
    'Организация проверена. Теперь можно бронировать доступные лоты.': { en: 'Organization verified. You can now claim available lots.', kk: 'Ұйым тексерілді. Енді қолжетімді лоттарды брондауға болады.' },
    'Подтвердите организацию, чтобы бронировать лоты.': { en: 'Verify your organization to claim lots.', kk: 'Лоттарды брондау үшін ұйымды растаңыз.' },
    'Сначала отправьте заявку на проверку организации.': { en: 'Submit your organization for verification first.', kk: 'Алдымен ұйымды тексеруге жіберіңіз.' },
    'Срочных позиций сейчас нет.': { en: 'There are no urgent items right now.', kk: 'Қазір шұғыл тауар жоқ.' },
    'Просроченных позиций нет.': { en: 'There are no expired items.', kk: 'Мерзімі өткен тауар жоқ.' },
    'Нет актов, ожидающих завершения.': { en: 'No documents are awaiting completion.', kk: 'Аяқтауды күтіп тұрған акт жоқ.' },
    'Акт — документ для фонда: распечатать, сверить позиции, подписать обеим сторонам.': { en: 'The transfer document is for the charity: print it, check the items and have both parties sign.', kk: 'Тапсыру актісі қор үшін жасалады: оны басып шығарып, тауарларды тексеріп, екі тарап қол қояды.' },
    'Нет товаров, соответствующих выбранным фильтрам.': { en: 'No products match the selected filters.', kk: 'Таңдалған сүзгілерге сай тауар жоқ.' }
};

window.ariaI18n.uiPatterns = [
    { pattern: /^Сформировать акт · (\d+)$/, translate: (value, lang) => lang === 'en' ? 'Create transfer document · ' + value.match(/\d+/)[0] : 'Тапсыру актісін жасау · ' + value.match(/\d+/)[0] },
    { pattern: /^(\d+)\s*\/\s*(\d+) единиц · порог партии$/, translate: (value, lang) => { const match = value.match(/^(\d+)\s*\/\s*(\d+)/); return lang === 'en' ? match[1] + ' / ' + match[2] + ' units · lot threshold' : match[1] + ' / ' + match[2] + ' бірлік · лот шегі'; } },
    { pattern: /^В очереди сверх порога:\s*(\d+) ед\.$/, translate: (value, lang) => { const amount = value.match(/\d+/)[0]; return lang === 'en' ? 'Beyond threshold in queue: ' + amount + ' units' : 'Кезекте шектен тыс: ' + amount + ' бірлік'; } },
    { pattern: /^(\d+)\s*\/\s*(\d+) единиц$/, translate: (value, lang) => { const match = value.match(/^(\d+)\s*\/\s*(\d+)/); return lang === 'en' ? match[1] + ' / ' + match[2] + ' units' : match[1] + ' / ' + match[2] + ' бірлік'; } },
    { pattern: /^· порог партии$/, translate: (value, lang) => lang === 'en' ? '· lot threshold' : '· лот шегі' },
    { pattern: /^В очереди сверх порога:\s*$/, translate: (value, lang) => lang === 'en' ? 'Beyond threshold in queue:' : 'Кезекте шектен тыс:' },
    { pattern: /^(\d+) ед\.$/, translate: (value, lang) => lang === 'en' ? value.match(/\d+/)[0] + ' units' : value.match(/\d+/)[0] + ' бірлік' },
    { pattern: /^(\d+) шт\.$/, translate: (value, lang) => lang === 'en' ? value.match(/\d+/)[0] + ' pcs.' : value.match(/\d+/)[0] + ' дана' },
    { pattern: /^(\d+) дн\.$/, translate: (value, lang) => lang === 'en' ? value.match(/\d+/)[0] + ' days' : value.match(/\d+/)[0] + ' күн' },
    { pattern: /^(.+?) · SKU-(\d+) · (\d+) шт\.(?: · до (\d{4}-\d{2}-\d{2}))?$/, translate: (value, lang) => { const match = value.match(/^(.+?) · SKU-(\d+) · (\d+) шт\.(?: · до (\d{4}-\d{2}-\d{2}))?$/); const name = window.ariaI18n.uiPhrases[match[1]]?.[lang] || match[1]; const date = match[4] ? (lang === 'en' ? ' · expires ' : ' · жарамдылық мерзімі ') + match[4] : ''; return name + ' · SKU-' + match[2] + ' · ' + match[3] + (lang === 'en' ? ' pcs.' : ' дана') + date; } },
    { pattern: /^(.+) · (\d+) дн\. на складе$/, translate: (value, lang) => { const match = value.match(/^(.+) · (\d+) дн\. на складе$/); const category = window.ariaI18n.uiPhrases[match[1]]?.[lang] || match[1]; return lang === 'en' ? category + ' · ' + match[2] + ' days in stock' : category + ' · қоймада ' + match[2] + ' күн'; } },
    { pattern: /^не продаётся (\d+) дн\.$/, translate: (value, lang) => { const days = value.match(/\d+/)[0]; return lang === 'en' ? 'no sales for ' + days + ' days' : days + ' күн сатылым жоқ'; } },
    { pattern: /^срок годности через (\d+) дн\.$/, translate: (value, lang) => { const days = value.match(/\d+/)[0]; return lang === 'en' ? 'expires in ' + days + ' days' : days + ' күннен кейін жарамдылық мерзімі бітеді'; } }
];

window.ariaI18n.translatePage(document.body);
window.ariaI18nPendingRoots = new Set();
window.ariaI18nUpdateTimer = 0;
window.ariaI18nObserver = new MutationObserver(records => {
    records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) window.ariaI18nPendingRoots.add(node);
        else if (node.nodeType === Node.TEXT_NODE && node.parentElement) window.ariaI18nPendingRoots.add(node.parentElement);
    }));
    if (!window.ariaI18nPendingRoots.size || window.ariaI18nUpdateTimer) return;
    window.ariaI18nUpdateTimer = window.setTimeout(() => {
        const pending = Array.from(window.ariaI18nPendingRoots);
        window.ariaI18nPendingRoots.clear();
        window.ariaI18nUpdateTimer = 0;
        const roots = pending.filter(node => !pending.some(parent => parent !== node && parent.contains(node)));
        roots.forEach(node => window.ariaI18n.translatePage(node));
    }, 50);
});
if (document.body) window.ariaI18nObserver.observe(document.body, { childList: true, subtree: true });
window.ariaI18n.initI18n();

Object.assign(window.ariaI18n.uiPhrases, {
 'ARIA — магазин, который не выбрасывает еду': {en:'ARIA — a store that keeps good food in use',kk:'ARIA — азық-түлікті ысырап етпейтін дүкен'},
 'Помогаем магазинам сокращать списания и направлять подходящие товары людям через проверенные организации.': {en:'Helping stores reduce write-offs and share suitable products through verified organizations.',kk:'Дүкендерге есептен шығаруды азайтуға және жарамды тауарларды тексерілген ұйымдар арқылы беруге көмектесеміз.'},
 'Как это работает': {en:'How it works',kk:'Қалай жұмыс істейді'},
 'Все категории': {en:'All categories',kk:'Барлық санаттар'},
 'Срок годности: сначала ближайший': {en:'Expiry: soonest first',kk:'Жарамдылық мерзімі: алдымен жақыны'},
 'В корзину': {en:'Add to cart',kk:'Себетке қосу'},
 'Срок в норме': {en:'Within shelf life',kk:'Мерзімі қалыпты'},
 'Срок ≤7 дней': {en:'Expires within 7 days',kk:'Мерзімі 7 күнге дейін'},
 'Срок ≤30 дней': {en:'Expires within 30 days',kk:'Мерзімі 30 күнге дейін'},
 'Показать ещё': {en:'Show more',kk:'Тағы көрсету'},
 'Новый': {en:'New',kk:'Жаңа'},
 'Отправлен': {en:'Shipped',kk:'Жіберілді'},
 'Доставлен': {en:'Delivered',kk:'Жеткізілді'},
 'Отменён': {en:'Cancelled',kk:'Бас тартылды'},
 'Организация проверена: бронирование доступно.': {en:'Organization verified: reservations are available.',kk:'Ұйым тексерілген: брондау қолжетімді.'},
 'После бронирования скачайте акт и согласуйте выдачу. Передачу подтверждает сотрудник магазина.': {en:'Download the document after reserving and arrange pickup. A store employee confirms the handover.',kk:'Брондағаннан кейін актіні жүктеп, алуды келісіңіз. Тапсыруды дүкен қызметкері растайды.'}
});
window.ariaI18n.uiPatterns.push({pattern:/^В наличии: (\d+) шт\.$/,translate:(v,l)=>{const n=v.match(/\d+/)[0];return l==='en'?'In stock: '+n+' pcs.':'Қоймада: '+n+' дана';}});
window.ariaI18n.translatePage(document.body);
