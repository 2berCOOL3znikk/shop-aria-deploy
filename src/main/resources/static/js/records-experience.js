(() => {
 'use strict';
 const {t,esc,original,intro,empty}=ariaOperationsExperience;
 const number=v=>new Intl.NumberFormat(ariaCopy.locale()).format(Number(v||0)),money=v=>v==null?t('Не указана','Not recorded'):new Intl.NumberFormat(ariaCopy.locale(),{style:'currency',currency:'KZT'}).format(Number(v));
 const when=v=>new Date(v).toLocaleString(ariaCopy.locale(),{dateStyle:'medium',timeStyle:'short'}),revision=new WeakMap();
 let page=0,product='';
 const types=()=>({OPENING_SNAPSHOT:t('Начальный снимок','Opening snapshot'),INITIAL_BALANCE:t('Начальный остаток','Initial balance'),RECEIPT:t('Приёмка','Receipt'),RECEIPT_CORRECTION:t('Исправление приёмки','Receipt correction'),ORDER_STOCK:t('Отгрузка заказа','Order shipment'),POS_SALE_STOCK:t('Продажа в кассе','Till sale'),SOCIAL_TRANSFER_STOCK:t('Социальная передача','Social handover'),ORDER_RETURN:t('Возврат заказа','Order return'),WRITE_OFF:t('Списание','Write-off'),RETURN_QUARANTINE:t('Возврат в карантин','Return to quarantine')});
 async function ledger(ctx){
  const root=document.getElementById('movements-root');if(!root)return;const seq=(revision.get(root)||0)+1;revision.set(root,seq);
  try{
   const [d,products]=await Promise.all([ctx.request('/movements?page='+page+'&size=25'+(product?'&productId='+product:'')),ctx.request('/products')]);if(!root.isConnected||revision.get(root)!==seq)return;
   const rows=d.movements,metric=(title,value)=>`<article><span>${title}</span><strong>${value}</strong></article>`,names=new Map(products.map(p=>[p.id,p.name]));
   root.innerHTML=`${intro(t('История склада','Inventory history'),t('Каждое движение — на своём месте','Every stock movement, accounted for'),t('Поступления, отгрузки, возвраты и оформленные потери.','Receipts, shipments, returns and recorded losses.'))}<div class="rx-metrics">${metric(t('Списано за 30 дней','Written off in 30 days'),number(d.writtenOffUnits30))}${metric(t('Стоимость оформленных потерь','Recorded write-off cost'),money(d.recordedWriteOffCost30))}${metric(t('Без указанной себестоимости','Units with unknown cost'),number(d.unknownCostUnits30))}</div><div class="ox-filters"><select id="movement-product" aria-label="${t('Фильтр по товару','Filter by product')}"><option value="">${t('Все товары','All products')}</option>${products.map(p=>`<option value="${p.id}" ${String(p.id)===product?'selected':''}>${esc(p.name)}</option>`).join('')}</select></div><div class="rx-ledger">${rows.content.length?rows.content.map(m=>`<article><div><span class="ox-state">${types()[m.type]||t('Изменение остатка','Stock change')}</span><h3>${original(names.get(m.productId)||t('Карточка недоступна','Product unavailable'))}</h3><time>${when(m.createdAt)}</time></div><div class="rx-amount"><strong>${m.quantityDelta>0?'+':''}${number(m.quantityDelta)}</strong><small>${t('единиц','units')}</small>${m.quarantineUnits?`<span>${t('В карантине','Quarantined')}: ${number(m.quarantineUnits)}</span>`:''}</div><details><summary>${t('Основание и стоимость','Reason and cost')}</summary><p>${original(m.reason)}</p><dl><div><dt>${t('Стоимость единицы','Unit cost')}</dt><dd>${money(m.unitCost)}</dd></div><div><dt>${t('Записал','Recorded by')}</dt><dd>${esc(m.actor)}</dd></div></dl></details>${m.lotId?`<button type="button" class="control-small" data-ledger-lot="${m.lotId}">${t('Открыть партию','Open batch')} →</button>`:''}</article>`).join(''):empty(t('Движений пока нет','No stock movements yet'),t('История появится после приёмки или другой учётной операции.','History starts with a receipt or another recorded stock operation.'),`<a class="ws-button" href="#lots">${t('Открыть склад','Open inventory')} →</a>`)}</div><div class="rx-pagination"><button type="button" class="control-small" data-ledger-page="prev" ${page===0?'disabled':''}>← ${t('Назад','Back')}</button><span>${t('Страница','Page')} ${page+1} / ${Math.max(1,rows.totalPages)} · ${number(rows.totalElements)} ${t('записей','records')}</span><button type="button" class="control-small" data-ledger-page="next" ${page+1>=rows.totalPages?'disabled':''}>${t('Далее','Next')} →</button></div><details class="px-panel"><summary>${t('Что входит в историю','About this history')}</summary><p class="px-footnote">${esc(ariaCopy.systemMessage(d.note))}</p></details>`;
   root.querySelector('#movement-product').onchange=e=>{product=e.target.value;page=0;ledger(ctx);};
   root.onclick=async e=>{const b=e.target.closest('[data-ledger-page]');if(b&&!b.disabled){page+=b.dataset.ledgerPage==='next'?1:-1;ledger(ctx);}const lot=e.target.closest('[data-ledger-lot]');if(lot){lot.disabled=true;try{await ariaProductFlow.detail(ctx,Number(lot.dataset.ledgerLot));}catch(error){ctx.notify(error.message,true);}finally{lot.disabled=false;}}};
  }catch(error){if(root.isConnected&&revision.get(root)===seq){root.innerHTML=`<p class="px-footnote" role="alert">${esc(error.message)}</p><button class="ws-button" data-ledger-retry>${t('Повторить','Try again')}</button>`;root.querySelector('button').onclick=()=>ledger(ctx);}}
 }
 // Preserve the real cells/actions and existing delegated handlers, change presentation only.
 function cards(section){
  const content=document.getElementById('control-content'),wrap=content.querySelector('.control-table-wrap');if(!wrap)return;
  const headers=[...wrap.querySelectorAll('thead th')].map(h=>h.textContent),list=document.createElement('section');list.className='rx-records';list.dataset.records=section;
  for(const row of wrap.querySelectorAll('tbody tr')){const cells=[...row.cells],card=document.createElement('article');
   if(section==='audit'){
    const title=cells[2]?.firstChild?.textContent||'',h=document.createElement('h3');h.textContent=/^[A-Z_]+$/.test(title.trim())?t('Событие магазина','Store event'):title;card.append(h);
    const time=document.createElement('time');time.textContent=cells[0].textContent;card.append(time);
    const details=document.createElement('details'),summary=document.createElement('summary');summary.textContent=t('Подробности записи','Record details');details.append(summary);const body=document.createElement('dl');
    for(const index of [1,2,3]){const field=document.createElement('div'),dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=headers[index];while(cells[index].firstChild)dd.append(cells[index].firstChild);field.append(dt,dd);body.append(field);}details.append(body);card.append(details);
   }else{
    const body=document.createElement('dl');cells.forEach((cell,index)=>{const field=document.createElement('div'),dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=headers[index]||'';while(cell.firstChild)dd.append(cell.firstChild);field.append(dt,dd);body.append(field);});card.append(body);
   }
   list.append(card);
  }
  wrap.replaceWith(list);
 }
 window.ariaRecordsExperience={ledger,cards};
})();
