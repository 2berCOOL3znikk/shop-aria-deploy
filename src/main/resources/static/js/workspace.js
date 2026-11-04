(() => {
  'use strict';
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const number = v => new Intl.NumberFormat('ru-KZ').format(Number(v || 0));
  const money = v => new Intl.NumberFormat('ru-KZ', {style:'currency',currency:'KZT',maximumFractionDigits:2}).format(Number(v || 0));
  const stamp = v => v ? new Date(v).toLocaleString('ru-KZ', {dateStyle:'short',timeStyle:'short'}) : 'Ещё не запускалась';
  const link = (text, section, cls='ws-link') => `<a class="${cls}" href="#${section}">${text}<span aria-hidden="true">↗</span></a>`;
  const empty = (text, action, section) => `<div class="ws-empty"><span aria-hidden="true">○</span><p>${text}</p>${action ? link(action,section) : ''}</div>`;
  const metric = (label, value, note, section) => `<a class="ws-metric" href="#${section}"><span>${label}<i aria-hidden="true">↗</i></span><strong>${value}</strong><small>${note}</small></a>`;
  function salesChart(daily) {
    const max = Math.max(0,...daily.map(d => Number(d.revenue)));
    if (!max) return empty('Пока нет отгрузок за последние 14 дней. График появится после первой продажи.', 'Открыть заказы', 'orders');
    return `<div class="ws-chart" role="group" aria-label="Выручка подтверждённых отгрузок за 14 дней">${daily.map(d => {
      const label = new Date(d.date+'T12:00:00').toLocaleDateString('ru-KZ',{day:'numeric',month:'short'});
      return `<div class="ws-chart-column"><button type="button" class="ws-chart-bar" style="--bar:${Number(d.revenue)/max*100}%" aria-label="${esc(label)}: ${esc(money(d.revenue))}, заказов: ${Number(d.orders)}"><span>${esc(money(d.revenue))}<small>${esc(label)} · заказов ${Number(d.orders)}</small></span></button><small>${new Date(d.date+'T12:00:00').getDate()}</small></div>`;
    }).join('')}</div><p class="ws-chart-caption">Дата отгрузки · ${esc(daily[0]?.date)} — ${esc(daily.at(-1)?.date)}</p>`;
  }
  async function mount({request}) {
    const root = document.getElementById('workspace-root'); if (!root) return;
    try {
      const d = await request('/workspace/overview'); if (!root.isConnected) return;
      const o=d.overview,t=d.today,r=d.risks,m=d.manager;
      const enabled=m.enabled;
      const status=!enabled?'Автоматизация выключена':m.lastError?'Нужна проверка':m.mode==='AUTOPILOT'?'Автопилот включён':'ARIA наблюдает за магазином';
      const provider=m.provider;
      const aiNote=!provider.configured?'Подключите AI в настройках':!m.externalAiConsent?'Для AI нужно ваше согласие':provider.status==='UNAVAILABLE'?'ИИ временно недоступен':provider.lastSuccess?'Последний ответ AI: '+stamp(provider.lastSuccess):'Первый ответ AI ещё не получен';
      const tasks=[
        [o.pendingOrders,'Новые заказы','Подтвердите обработку и отгрузку','orders'],
        [r.pendingDecisions,'Рекомендации по ценам','Проверьте и подтвердите решение','smart'],
        [o.lowStock,'Товары на минимуме','Проверьте потребность в закупке','products'],
        [o.expiredLots,'Просроченные партии','Продажа запрещена. Проверьте списание','lots'],
        [r.openSignals,'Исключения ARIA','Нужно внимание ответственного','manager'],
        [r.failedTasks,'Задачи с ошибкой','Проверьте причину и повторите','manager']
      ].filter(x=>x[0]>0);
      document.getElementById('workspace-company').textContent=d.organization.name;
      document.getElementById('workspace-kind').textContent=d.organization.demonstration?'Демонстрационная компания':'Кабинет компании';
      root.innerHTML=`
        <div class="ws-page-heading"><div><p class="ws-overline">Обзор компании</p><h2>${esc(d.organization.name)}</h2><p>Всё важное о магазине — в одном месте.</p></div><button type="button" class="ws-button ws-button-primary" data-action="product-new">＋ Добавить товар</button></div>
        ${d.organization.demonstration?'<div class="ws-demo-note"><span class="ws-pill">Демо</span><p>Учебная компания. Показатели взяты из её базы и не являются результатами реального магазина.</p><a href="/index.html">Открыть витрину ↗</a></div>':''}
        <section class="ws-metrics" aria-label="Показатели магазина">
          ${metric('Продажи сегодня',money(d.sales.revenueToday),number(d.sales.ordersToday)+' отгруженных заказов','orders')}
          ${metric('Товары',number(o.products),'Карточки собственного каталога','products')}
          ${metric('Доступный остаток',number(o.availableUnits),'Пригодные единицы в партиях','lots')}
          ${metric('Мои магазины',number(d.stores.connected),d.stores.attention?number(d.stores.attention)+' требуют настройки':'Подтверждённые подключения','stores')}
        </section>
        <section class="ws-ai-card" aria-labelledby="ws-ai-title"><div class="ws-ai-heading"><div class="ws-ai-mark" aria-hidden="true">✳</div><div><p class="ws-overline">ARIA AI</p><h3 id="ws-ai-title">Что ARIA сделала сегодня</h3></div>${link('Центр управления','manager')}</div><div class="ws-ai-stats">${[
          ['Проверено товаров',t.productsAnalyzed],['Ценовых решений AI',t.aiDecisions],['Карточек подготовлено AI',t.productDrafts],['Автоматически изменено цен',t.priceApplied],['Черновиков закупок',t.purchaseDrafts],['Социальных предложений',t.socialDrafts]
        ].map(([label,v])=>`<div><strong>${number(v)}</strong><span>${label}</span></div>`).join('')}</div><div class="ws-ai-footer"><span><i class="ws-status-dot ${enabled?'is-on':''}" aria-hidden="true"></i>${esc(status)}</span><span>${number(t.engineDecisions)} решений — автоматический расчёт</span><small>${esc(aiNote)}</small></div></section>
        <div class="ws-grid"><section class="ws-panel ws-sales"><div class="ws-panel-head"><div><h3>Продажи</h3><p>Выручка отгрузок · последние 14 дней</p></div><span class="ws-pill">₸ KZT</span></div>${salesChart(d.sales.daily)}<p class="ws-footnote">Собственный магазин и внутренняя касса. Заказы подключений WooCommerce учитываются отдельно.</p></section>
        <section class="ws-panel ws-attention"><div class="ws-panel-head"><div><h3>Требует внимания</h3><p>Следующие действия для команды</p></div><span class="ws-pill">${tasks.length}</span></div>${tasks.length?`<div class="ws-task-list">${tasks.map(([count,title,note,section])=>`<a href="#${section}"><span class="ws-task-count">${number(count)}</span><span><strong>${title}</strong><small>${note}</small></span><span aria-hidden="true">›</span></a>`).join('')}</div>`:empty('Сейчас нет открытых задач. ARIA покажет их здесь, когда появятся новые данные.','Проверить состояние AI','manager')}</section>
        <section class="ws-panel ws-recent"><div class="ws-panel-head"><div><h3>Последние изменения</h3><p>Сохранённая история действий</p></div>${link('Весь журнал','audit')}</div>${d.recent.length?`<ol class="ws-timeline">${d.recent.slice(0,5).map(e=>`<li><span class="ws-timeline-dot" aria-hidden="true"></span><a href="#${esc(e.section)}">${esc(e.title)}</a><time>${esc(stamp(e.at))}</time></li>`).join('')}</ol>`:empty('История пока пуста. Начните с товара или подключения магазина.','Мои магазины','stores')}</section>
        <section class="ws-panel ws-shortcuts"><div class="ws-panel-head"><div><h3>Рабочие инструменты</h3><p>Быстрый переход к делу</p></div></div><div class="ws-shortcut-list">${[['Принять поставку','Партии, закупка и сроки годности','lots'],['Провести продажу','Внутренняя касса и история операций','pos'],['Подключить магазин','WooCommerce и каталог ARIA','stores']].map(([title,note,section])=>`<a href="#${section}"><span><strong>${title}</strong><small>${note}</small></span><span aria-hidden="true">↗</span></a>`).join('')}</div></section></div>
        <p class="ws-updated">Обновлено ${esc(stamp(d.asOf))} · ${esc(d.timeZone)} · ${esc(d.date)}</p>`;
    } catch (error) {
      if (!root.isConnected) return;
      root.innerHTML=`<div class="ws-panel ws-error" role="alert"><h3>Не удалось загрузить главную</h3><p>${esc(error.message)}</p><button type="button" class="ws-button">Повторить</button></div>`;
      root.querySelector('button').onclick=()=>mount({request});
    }
  }
  window.ariaWorkspace={mount};
})();
