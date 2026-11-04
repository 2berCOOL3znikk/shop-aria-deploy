(() => {
  'use strict';
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const money=v=>v==null||!Number.isFinite(Number(v))?'—':new Intl.NumberFormat('ru-KZ',{style:'currency',currency:'KZT'}).format(Number(v));
  const date=v=>v?new Date(v).toLocaleString('ru-KZ',{dateStyle:'short',timeStyle:'short'}):'—';
  const actionNames={DISCOUNT:'Предложить скидку',RESTORE:'Уменьшить / снять скидку',KEEP:'Сохранить цену',REVIEW:'Проверить ограничения',BLOCKED:'Нет пригодного остатка'};
  const states={PENDING:'Ожидает решения',APPLIED:'Применено',REJECTED:'Отклонено',SUPERSEDED:'Заменено новым расчётом',ROLLED_BACK:'Отменено откатом'};
  const filters={ALL:'Все решения',ATTENTION:'Требует внимания',AUTOMATIC:'Изменено автоматически',PENDING:'На подтверждении',UNCHANGED:'Без изменений',ERROR:'Ошибка'};
  const api='/smart-pricing';let page=0,view='ALL',query='',revision=0;
  const button=(label,action,id,disabled=false)=>`<button type="button" class="control-small" data-smart="${action}" data-id="${id??''}" ${disabled?'disabled title="Разрешите внешний AI в настройках"':''}>${esc(label)}</button>`;
  const table=(headers,rows)=>`<div class="control-table-wrap"><table class="control-table"><thead><tr>${headers.map(h=>`<th>${esc(h)}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`;
  function summary(d){
    const r=d.report,f=r?.facts,p=d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:r?.suggestedPercent;
    const scenario=r?.scenarios?.find(x=>x.percent===p);
    const price=scenario?.price??r?.suggestedPrice;
    const unchanged=f&&p===f.currentPercent&&['PRICE','KEEP'].includes(d.strategy||'PRICE')&&!['BLOCKED','REVIEW'].includes(r.action);
    const blocked=['BLOCKED','REVIEW'].includes(r?.action);
    let title=unchanged?'Цена без изменений':d.strategy==='SOCIAL_REVIEW'?'Рассмотреть социальную передачу':d.strategy==='REORDER'?'Рассмотреть закупку':d.strategy==='PROMOTE'?'Поддержать продажи':blocked?(r.action==='BLOCKED'?'Нет доступного остатка':'Проверить данные товара'):`${p>f?.currentPercent?'Снизить':'Вернуть'} цену до ${money(price)}`;
    let reason=unchanged&&r.sales?.units30===0?'Пока недостаточно продаж для обоснованной скидки.':String(d.source==='AI'?d.explanation?.explanation||r?.reason||'':r?.reason||'').split(/(?<=[.!?])\s/)[0];
    if(!reason)reason='Откройте решение, чтобы проверить исходные данные.';
    if(reason.length>145)reason=reason.slice(0,142).replace(/\s+\S*$/,'')+'…';
    if(d.status==='APPLIED')title='Цена установлена: '+money(d.appliedPrice);
    if(d.status==='ROLLED_BACK')title='Изменение отменено';
    let state=d.stale?'Нужен новый расчёт':d.status==='PENDING'?(unchanged?'Изменение не требуется':blocked?'Нужна проверка':'Ожидает решения'):states[d.status]||d.status;
    if(d.readError){title='Не удалось прочитать решение';reason='Обновите расчёт. Сохранённая история останется в журнале.';state='Ошибка данных';}
    return {title,reason,state,unchanged,blocked,percent:p,price};
  }
  function row(d){
    const s=summary(d),f=d.report?.facts||{},live=d.live||{},name=live.name??f.name??'Товар недоступен';
    const source=d.source==='AI'?'Решение ARIA AI':'Автоматический расчёт';
    const primary=d.stale||d.readError?button('Обновить расчёт','reanalyze',d.productId):button(d.status==='PENDING'&&!s.unchanged&&!s.blocked?'Проверить решение':'Подробнее','detail',d.id);
    return `<tr class="price-row" data-decision="${d.id}"><td data-label="Товар"><span class="price-product" data-merchant-content>${esc(name)}</span><small>${esc(live.sku??f.sku)}</small></td><td data-label="Текущая цена" class="price-value">${money(d.live?live.price:f.currentPrice)}${!d.live?'<small>На момент анализа</small>':''}</td><td data-label="Рекомендация ARIA"><strong>${esc(s.title)}</strong><small class="price-origin">${source}</small>${d.aiCompletedAt&&d.source!=='AI'?'<small class="exp-warning">ИИ временно недоступен</small>':''}</td><td data-label="Причина" class="price-reason">${esc(s.reason)}</td><td data-label="Статус"><span class="exp-status ${d.stale||d.readError?'attention':''}">${esc(s.state)}</span><small>${date(d.createdAt)}</small></td><td class="price-row-action">${primary}</td></tr>`;
  }
  function detail(d,aiConsent){
    const r=d.report,f=r.facts,s=r.sales,chosen=d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:r.suggestedPercent,best=r.scenarios.find(x=>x.percent===chosen),pending=d.status==='PENDING',displayAction=['BLOCKED','REVIEW'].includes(r.action)?r.action:chosen>f.currentPercent?'DISCOUNT':chosen<f.currentPercent?'RESTORE':'KEEP';
    return `<article class="control-panel smart-card"><header class="smart-card-head"><div><h2>${esc(d.live?.name??f.name)}</h2><p class="control-help">${esc(f.sku)} · ${esc(f.category)} · решение #${d.id} · ${date(d.createdAt)}</p></div><span class="control-tag ${pending?'risk':'good'}">${esc(states[d.status]||d.status)}</span></header>
      <div class="smart-price-row"><div><small>Цена при анализе</small><strong>${money(f.currentPrice)}</strong></div><div><small>Рекомендуется</small><strong>${money(best?.price??r.suggestedPrice)}</strong><small>Скидка ${chosen}% от ${money(f.basePrice)}</small></div><div><small>Пригодный остаток</small><strong>${f.quantity} ед.</strong><small>Минимальная цена ${money(f.minimumPrice)}</small></div></div>
      <p><b>Источник: ${esc(d.source==='AI'?'Решение ARIA AI':d.aiCompletedAt?'Автоматический расчёт · ИИ временно недоступен':'Автоматический расчёт')}</b> · Стратегия: ${esc(({PRICE:'Цена',PROMOTE:'Продвижение',SOCIAL_REVIEW:'Проверить социальную передачу',REORDER:'Рассмотреть закупку',KEEP:'Сохранить'})[d.strategy||'PRICE'])} · ${d.aiCompletedAt?'AI-анализ: '+date(d.aiCompletedAt):'Внешний AI ещё не использован для этого решения'}</p><p><b>${esc(actionNames[displayAction]||displayAction)}</b> · Уверенность: ${r.confidence==='MEDIUM'?'умеренная (описательные данные)':'низкая'}</p><p>Расчёт по данным магазина: ${esc(r.reason)}</p>${d.aiLastSkippedNote?`<p class="control-help">Последний AI-запуск пропущен ${date(d.aiLastSkippedAt)}: ${esc(d.aiLastSkippedNote)}</p>`:''}
      <p class="control-help">Отгружено за 7 / 14 / 30 дней: <b>${s.units7} / ${s.units14} / ${s.units30}</b> ед. · Скорость: ${s.unitsPerDay} ед./день.<br>${r.planning?`Горизонт оценки ${r.planning.forecastDays} дн. · покрытие запасом ${r.planning.clearanceDays??'не оценено'} дн. · ближайший срок ${r.planning.nearestExpiryDays??'не задан'} дн.<br>`:''}Следующий пересмотр: ${date(d.reviewAt)}. Это срок проверки, а не автоматическая отмена акции.</p>
      ${best?`<p class="control-help">Оценка за ${r.planning?.forecastDays??"указанный в сохранённом расчёте горизонт"} дн.: выручка ${money(best.estimatedRevenue)}, вклад после себестоимости и риска списания ${money(best.estimatedContribution)}. Непроданный остаток ${(best.estimatedRemainingShare*100).toFixed(1)}%; ожидаемо просрочится ${best.estimatedExpiredUnits} ед. Это сценарий, не точный прогноз.</p>`:''}
      <div class="control-actions">${pending&&!d.stale&&f.quantity>0&&!['BLOCKED','REVIEW'].includes(r.action)?(['PRICE','KEEP'].includes(d.strategy||'PRICE')?button('Применить','apply',d.id):'')+button('Изменить','edit',d.id):''}${pending&&!d.stale&&f.quantity>0&&r.scenarios.length&&r.scenarios.every(s=>s.estimatedExpiredUnits>0)?button('Предложить социальную передачу','social-propose',d.id):''}${pending?button('Отклонить','reject',d.id)+button('AI: выбрать стратегию','ai-decide',d.id,!aiConsent):''}${d.status==='APPLIED'?button('Откатить','rollback',d.id):''}${button('Обновить расчёт','reanalyze',f.productId)}${button('История и результаты','history',f.productId)}${button('AI-объяснение','explain',d.id,!aiConsent)}</div>
      ${d.decidedAt?`<p class="control-help">${date(d.decidedAt)} · ${esc(d.decidedBy)} · ${esc(d.decisionReason)}${d.appliedPrice!=null?' · Подтверждено: '+money(d.appliedPrice)+' ('+d.appliedPercent+'%)':''}</p>`:''}
      <details><summary>Данные, ограничения и сценарии</summary><ul>${r.limitations.map(l=>`<li>${esc(l)}</li>`).join('')}</ul>
      ${s.approximateRateLow!=null?`<p class="control-help">Приближённый 95% интервал наблюдаемой частоты (модель Пуассона): ${s.approximateRateLow}–${s.approximateRateHigh} ед./день. Стационарность не проверена; это не интервал будущего спроса.</p>`:''}
      ${table(['Скидка','Цена','Продажи, оценка','Диапазон допущений','Выручка, оценка','Вклад, оценка','Просрочится, оценка'],r.scenarios.map(x=>`<tr><td>${x.percent}%</td><td>${money(x.price)}</td><td>${x.estimatedUnits}</td><td>${x.estimatedUnitsLow}–${x.estimatedUnitsHigh}</td><td>${money(x.estimatedRevenue)}</td><td>${money(x.estimatedContribution)}</td><td>${x.estimatedExpiredUnits}</td></tr>`))}
      <p class="control-help">Партии и фактические сроки, вошедшие в расчёт:</p>${table(['Партия','Количество','Себестоимость','Приёмка / срок','Хранение'],f.lots.map(l=>`<tr><td>${l.id??'Начальный остаток'}</td><td>${l.quantity}</td><td>${money(l.cost)}</td><td>${esc(l.receivedAt||'Дата не записана')} / ${esc(l.expiry||'Без срока')}</td><td>${esc(l.storageRequirements||'Требования не записаны')} · ${esc(({CONFIRMED:'подтверждено',REJECTED:'отклонено',UNKNOWN:'не подтверждено'})[l.handlingStatus]||'не подтверждено')}</td></tr>`))}</details>
      <div class="smart-explanation" data-explanation="${d.id}">${explanation(d.explanation)}</div></article>`;
  }
  function explanation(e){if(!e)return '';return `<p class="control-help">${esc(e.note)}</p>${e.explanation?`<p>${esc(e.explanation)}</p>`:''}${e.risks?`<ul>${e.risks.map(r=>`<li>${esc(r)}</li>`).join('')}</ul>`:''}${e.mode==='AI'?`<p class="control-help">Модель: ${esc(e.model)}. Предложение AI: ${e.suggestedPercent??'нет'}%. Он не заменяет расчёт сервера.</p>`:''}`;}
  async function mount(ctx){
    const root=document.getElementById('smart-pricing-root');if(!root)return;
    const current=++revision;const {request,notify,form,formField,show}=ctx;
    root.setAttribute('aria-busy','true');
    try{
      const focus=new URLSearchParams(location.search).get('decision');if(focus&&!/^[1-9][0-9]*$/.test(focus))throw new Error('Неверный номер решения');
      // Opening this page is read-only. Scheduled work stays with the existing manager.
      const [result,settings,aiStatus]=await Promise.all([request(api+'?'+new URLSearchParams({view,query,page,size:10})),request(api+'/settings'),request(api+'/ai-status')]);
      if(current!==revision||root!==document.getElementById('smart-pricing-root'))return;
      if(focus){result.content=[await request(api+'/'+focus)];result.totalPages=1;result.totalElements=1;page=0;}
      if(result.totalPages>0&&page>=result.totalPages){page=result.totalPages-1;return mount(ctx);}
      const aiLabel=!settings.externalAiEnabled?'Внешний AI выключен':!aiStatus.configured?'AI не настроен':({CONNECTED:'AI подключён',UNTESTED:'AI ещё не проверен',UNAVAILABLE:'ИИ временно недоступен'})[aiStatus.status]||'Проверьте подключение AI';
      root.innerHTML=`<div class="exp-toolbar"><div><span class="exp-status ${settings.mode==='AUTOPILOT'?'active':''}">${settings.mode==='AUTOPILOT'?'Автопилот включён':'Подтверждение вручную'}</span><span class="exp-muted">${esc(aiLabel)}</span></div><div class="exp-actions">${button('Настройки','settings')}${settings.mode==='AUTOPILOT'?button('Остановить автопилот','stop'):''}<button type="button" class="btn-glow" data-smart="analyze">Проверить цены</button></div></div>
        ${focus?`<div class="exp-note">Сохранённое решение #${esc(focus)}. ${button('К актуальным решениям','all')}</div>`:`<nav class="exp-filters" aria-label="Фильтры решений">${Object.entries(filters).map(([key,label])=>`<button type="button" data-smart="filter" data-id="${key}" aria-pressed="${key===view}">${label}<span>${result.counts?.[key]??0}</span></button>`).join('')}</nav>`}
        <section class="exp-panel"><div class="exp-list-heading"><div><h2>Решения по товарам</h2><p>Последнее решение по каждому товару. История — в подробностях.</p></div><form id="price-search" role="search"><label class="sr-only" for="price-query">Найти товар или артикул</label><input id="price-query" name="query" placeholder="Название или артикул" maxlength="200" value="${esc(query)}"><button type="submit" class="control-small">Найти</button></form></div>
        ${result.content.length?`<div class="price-table-wrap"><table class="price-table"><thead><tr>${['Товар','Текущая цена','Рекомендация ARIA','Причина','Статус','Действие'].map(h=>`<th scope="col">${h}</th>`).join('')}</tr></thead><tbody>${result.content.map(row).join('')}</tbody></table></div>`:`<div class="exp-empty"><h3>${result.counts?.ALL?'Здесь пока нет решений':'Начните с первого расчёта'}</h3><p>${result.counts?.ALL?'Попробуйте другой фильтр или название товара.':'Добавьте товар, примите партию и нажмите «Проверить цены».'}</p>${result.counts?.ALL?button('Показать все','all'):'<a href="#products" class="control-small">Открыть товары</a>'}</div>`}</section>
        <div class="exp-pagination">${button('Назад','prev')}<span>${result.totalElements} товаров · Страница ${page+1} из ${Math.max(1,result.totalPages)}</span>${button('Далее','next')}</div>`;
      root.querySelector('[data-smart=prev]').disabled=page===0;root.querySelector('[data-smart=next]').disabled=page+1>=result.totalPages;
      root.querySelector('#price-search').onsubmit=event=>{event.preventDefault();query=new FormData(event.currentTarget).get('query').trim();page=0;mount(ctx);};
      const closeDetail=()=>document.getElementById('control-dialog').close();
      const handler=async event=>{
        const el=event.target.closest('[data-smart]');if(!el||el.disabled)return;
        el.disabled=true;const id=el.dataset.id,action=el.dataset.smart;
        try{
          if(action==='filter'){view=id;page=0;return mount(ctx);}
          if(action==='detail'){
            const d=result.content.find(x=>String(x.id)===id);if(!d)return;
            show(d.live?.name??d.report?.facts?.name??'Решение по товару',d.readError?'<p>Обновите расчёт, чтобы восстановить рекомендации.</p>':detail(d,settings.externalAiEnabled));
            document.getElementById('control-dialog-body').onclick=handler;return;
          }
          if(action==='settings'){
            form('Режим Smart Pricing',`<label class="control-field wide">Режим<select name="mode"><option value="RECOMMEND" ${settings.mode==='RECOMMEND'?'selected':''}>RECOMMEND — подтверждать вручную</option><option value="AUTOPILOT" ${settings.mode==='AUTOPILOT'?'selected':''}>AUTOPILOT — применять в моих границах</option></select></label>`+`<label class="wide"><input type="checkbox" name="externalAiEnabled" ${settings.externalAiEnabled?'checked':''}> Разрешаю отправлять в OpenRouter название/SKU, цены, себестоимость, партии и обезличенные отгрузки выбранного товара для выбора сценария</label><label class="wide"><input type="checkbox" name="scheduledAiEnabled" ${aiStatus.scheduledAvailable===false?'disabled':''} ${settings.scheduledAiEnabled?'checked':''}> Разрешаю фоновый AI-анализ по расписанию в пределах дневного лимита${aiStatus.scheduledAvailable===false?' — недоступен в текущем режиме сервера; используйте кнопку AI на карточке':''}</label>`+`<label class="wide"><input type="checkbox" name="publicAssistantAiEnabled" ${settings.publicAssistantAiEnabled?'checked':''}> Разрешаю внешний AI покупателям: только их сообщения и публичные карточки, после их отдельного согласия</label>`+formField('Дневной лимит внешних запросов покупателей','publicAiDailyBudget','number',settings.publicAiDailyBudget,{required:true,attrs:'min="1" max="20" step="1"'})+formField('Дневной лимит AI-решений','aiDailyBudget','number',settings.aiDailyBudget,{required:true,attrs:'min="1" max="50" step="1"'})+formField('Максимальная скидка автопилота, %','maximumDiscount','number',settings.maximumDiscount,{required:true,attrs:'min="0" max="90" step="1"'})+formField('Пауза между изменениями, часов','cooldownHours','number',settings.cooldownHours,{required:true,attrs:'min="1" max="720" step="1"'})+`<label class="control-field wide">Разрешённые категории — каждая с новой строки<textarea name="allowedCategories" maxlength="6000">${esc(settings.allowedCategories.join('\n'))}</textarea><small>Укажите точные названия категорий товара. Без списка автопилот не включится. Все категории через * не разрешаются.</small></label><label class="wide"><input type="checkbox" name="allowLowConfidence" ${settings.allowLowConfidence?'checked':''}> Разрешаю автоприменение рекомендаций ENGINE при низкой уверенности в пределах ограничений</label><label class="wide"><input type="checkbox" name="allowRestore" ${settings.allowRestore?'checked':''}> Разрешаю уменьшать скидку и возвращать обычную цену</label><p class="control-help wide">Минимальная цена товара, себестоимость и маржа категории проверяются всегда. AI с низкой уверенностью всегда требует ручного подтверждения. Без достаточной истории автоматическая скидка может не иметь основания и останется рекомендацией. Социальная передача не выполняется автопилотом. Экстренное выключение остановит следующие изменения; уже применённые цены откатываются отдельно.</p>`,async f=>request(api+'/settings','PUT',{version:settings.version,publicAssistantAiEnabled:f.has('publicAssistantAiEnabled'),publicAiDailyBudget:Number(f.get('publicAiDailyBudget')),externalAiEnabled:f.has('externalAiEnabled'),scheduledAiEnabled:f.has('scheduledAiEnabled'),aiDailyBudget:Number(f.get('aiDailyBudget')),mode:f.get('mode'),maximumDiscount:Number(f.get('maximumDiscount')),cooldownHours:Number(f.get('cooldownHours')),allowedCategories:f.get('allowedCategories').split('\n').map(s=>s.trim()).filter(Boolean),allowLowConfidence:f.has('allowLowConfidence'),allowRestore:f.has('allowRestore')}));return;
          }
          if(action==='stop'){await request(api+'/stop','POST');notify('Автопилот остановлен. Применённые цены сохранены; откат доступен в подробностях решения.');return mount(ctx);}
          if(action==='all'){history.replaceState(null,'',location.pathname+'#smart');view='ALL';query='';page=0;return mount(ctx);}
          if(action==='prev'||action==='next'){page+=action==='next'?1:-1;return mount(ctx);}
          if(action==='analyze'||action==='reanalyze'){
            const run=await request(action==='analyze'?api+'/analyze':api+'/products/'+id+'/analyze','POST');
            if(action==='reanalyze'&&focus&&run.id)history.replaceState(null,'',location.pathname+'?decision='+run.id+'#smart');
            closeDetail();notify(run.failed?'Не удалось проверить товаров: '+run.failed+'. Остальные решения сохранены.':'Расчёт обновлён. Посмотрите актуальные решения.',Boolean(run.failed));return mount(ctx);
          }
          if(action==='history'){
            const changes=await request(api+'/products/'+id+'/history');
            show('История цен и результаты',changes.length?changes.map(h=>`<section class="control-panel"><p>${date(h.change.changedAt)} · ${esc(h.change.actor)}</p><p>${money(Number(h.change.oldBasePrice)*(100-h.change.oldPercent)/100)} → ${money(Number(h.change.newBasePrice)*(100-h.change.newPercent)/100)}</p><p>${esc(h.change.reason)}</p><p>За 7 дней до: ${h.unitsBefore7Days} ед. После: ${h.unitsAfter} ед. за ${h.daysAfter} дн., выручка ${money(h.revenueAfter)}. Наблюдение до ${date(h.observedUntil)}.</p><p class="control-help">${esc(h.note)}</p></section>`).join(''):'<p>Цена ещё не менялась с начала ведения истории.</p>');return;
          }
          if(action==='social-propose'){await request('/social-proposals/decisions/'+id,'POST');closeDetail();notify('Предложение подготовлено. Товар ещё не зарезервирован.');location.hash='socialPlans';return;}
          if(action==='ai-decide'){
            const before=result.content.find(d=>String(d.id)===id),output=await request(api+'/'+id+'/ai-decide','POST');closeDetail();
            const fresh=output.source==='AI'&&output.aiCompletedAt!==before?.aiCompletedAt&&!output.aiLastSkippedNote;
            notify(output.aiLastSkippedNote||(fresh?'Новое решение ARIA AI сохранено. Проверьте выбранное действие.':output.source==='AI'?'Сохранённое решение ARIA AI доступно в подробностях.':'ИИ не выбрал решение. Сохранён автоматический расчёт.'),output.source!=='AI'||Boolean(output.aiLastSkippedNote));return mount(ctx);
          }
          if(action==='explain'){
            const output=await request(api+'/'+id+'/explain','POST');
            const slot=document.querySelector(`#control-dialog-body [data-explanation="${id}"]`);if(slot)slot.innerHTML=explanation(output);
            notify(output.mode==='AI'?'Объяснение ARIA AI готово.':output.note||'Внешний AI сейчас недоступен.',output.mode!=='AI');return;
          }
          const d=result.content.find(x=>String(x.id)===id);if(!d)return;
          const type=action==='edit'?'apply':action;
          const labels={apply:'Подтвердить цену',edit:'Изменить рекомендацию',reject:'Отклонить рекомендацию',rollback:'Откатить изменение'};if(!labels[action])return;
          form(labels[action],`<p class="control-help wide">${type==='apply'?'Скидка действует на пригодные партии этого товара. Перед сохранением проверим свежие остатки, минимальную цену и маржу.':'Причина сохранится в журнале. Для отката проверим, что цена после решения не менялась.'}</p>`+(type==='apply'?formField('Скидка от базовой цены, %','percent','number',d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:d.report.suggestedPercent,{required:true,attrs:`min="0" max="${d.report.facts.maximumPercent}" step="1"`})+`<p class="control-help">Базовая цена ${money(d.report.facts.basePrice)} · минимум ${money(d.report.facts.minimumPrice)}.</p>`:'')+'<label class="control-field wide">Причина *<textarea name="reason" required maxlength="900"></textarea></label>',async f=>{
            const body={reason:f.get('reason').trim()};if(type==='apply')body.percent=Number(f.get('percent'));
            await request(api+'/'+id+'/'+type,'POST',body);
          });
        }catch(error){notify(error.message,true);}finally{el.disabled=false;}
      };
      root.onclick=handler;
    }catch(error){if(root.isConnected){root.innerHTML='<div class="exp-empty" role="alert"><h2>Не удалось загрузить решения</h2><p>'+esc(error.message)+'</p><button type="button" class="control-small" id="price-retry">Повторить</button></div>';root.querySelector('#price-retry').onclick=()=>mount(ctx);}}
    finally{if(current===revision)root.setAttribute('aria-busy','false');}
  }
  window.ariaSmartPricing={mount};
})();
