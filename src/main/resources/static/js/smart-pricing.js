(() => {
  'use strict';
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const money=v=>v==null||!Number.isFinite(Number(v))?'—':new Intl.NumberFormat(ariaCopy.locale(),{style:'currency',currency:'KZT'}).format(Number(v));
  const date=v=>v?new Date(v).toLocaleString(ariaCopy.locale(),{dateStyle:'short',timeStyle:'short'}):'—';
  const actionNames={DISCOUNT:ariaCopy.text("text.29f6478df0800b"),RESTORE:ariaCopy.text("text.a6669f54b82384"),KEEP:ariaCopy.text("text.6d2d643376ad2f"),REVIEW:ariaCopy.text("text.c21f0755e556b4"),BLOCKED:ariaCopy.text("text.5dae1af34bf507")};
  const states={PENDING:ariaCopy.text("text.d996c53e53c405"),APPLIED:ariaCopy.text("text.a358e81fa2f104"),REJECTED:ariaCopy.text("text.a997f500082877"),SUPERSEDED:ariaCopy.text("text.d56279d6b8d236"),ROLLED_BACK:ariaCopy.text("text.db9144e2e39c1a")};
  const filters={ALL:ariaCopy.text("text.5aa2e349da54c2"),ATTENTION:ariaCopy.text("text.34dc848e184864"),AUTOMATIC:ariaCopy.text("text.97837f06e82087"),PENDING:ariaCopy.text("text.6928d5a72b5148"),UNCHANGED:ariaCopy.text("text.4274a06c5d5b8e"),ERROR:ariaCopy.text("text.bf22dfeeba8b85")};
  const api='/smart-pricing';let page=0,view='ALL',query='',revision=0;
  const button=(label,action,id,disabled=false)=>`<button type="button" class="control-small" data-smart="${action}" data-id="${id??''}" ${disabled?ariaCopy.text("text.47001de87bff6c"):''}>${esc(label)}</button>`;
  const table=(headers,rows)=>`<div class="control-table-wrap"><table class="control-table"><thead><tr>${headers.map(h=>`<th>${esc(h)}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`;
  function summary(d){
    const r=d.report,f=r?.facts,p=d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:r?.suggestedPercent;
    const scenario=r?.scenarios?.find(x=>x.percent===p);
    const price=scenario?.price??r?.suggestedPrice;
    const unchanged=f&&p===f.currentPercent&&['PRICE','KEEP'].includes(d.strategy||'PRICE')&&!['BLOCKED','REVIEW'].includes(r.action);
    const blocked=['BLOCKED','REVIEW'].includes(r?.action);
    let title=unchanged?ariaCopy.text("text.02eaa3b135a27e"):d.strategy==='SOCIAL_REVIEW'?ariaCopy.text("text.768f6238ccd13a"):d.strategy==='REORDER'?ariaCopy.text("text.544be2e65c673a"):d.strategy==='PROMOTE'?ariaCopy.text("text.4cf16403bdf008"):blocked?(r.action==='BLOCKED'?ariaCopy.text("text.34978a51061c31"):ariaCopy.text("text.71f6f454fb475f")):ariaCopy.template("template.c47e6ad5d0717f",[p>f?.currentPercent?ariaCopy.text("text.23e3b914326752"):ariaCopy.text("text.5ae0e885f69dee"),money(price)]);
    let reason=unchanged&&r.sales?.units30===0?ariaCopy.text("text.c6bd2e4e0cbe85"):String(d.source==='AI'?d.explanation?.explanation||r?.reason||'':r?.reason||'').split(/(?<=[.!?])\s/)[0];
    if(!reason)reason=ariaCopy.text("text.2e5ceb81e05b4d");
    if(reason.length>145)reason=reason.slice(0,142).replace(/\s+\S*$/,'')+'…';
    if(d.status==='APPLIED')title=ariaCopy.text("text.7b65ba052384bf")+money(d.appliedPrice);
    if(d.status==='ROLLED_BACK')title=ariaCopy.text("text.3367dad3202793");
    let state=d.stale?ariaCopy.text("text.428357800a0bee"):d.status==='PENDING'?(unchanged?ariaCopy.text("text.4f2112d508349d"):blocked?ariaCopy.text("text.f348d7ff4eb4fb"):ariaCopy.text("text.d996c53e53c405")):states[d.status]||d.status;
    if(d.readError){title=ariaCopy.text("text.0480810a86f08b");reason=ariaCopy.text("text.be80e58aed9823");state=ariaCopy.text("text.cb58887549f4df");}
    return {title,reason,state,unchanged,blocked,percent:p,price};
  }
  function row(d){
    const s=summary(d),f=d.report?.facts||{},live=d.live||{},name=live.name??f.name??ariaCopy.text("text.4f7c50b2b62bf5");
    const source=d.source==='AI'?ariaCopy.text("text.6cf91b141633f4"):ariaCopy.text("text.53f928865efa47");
    const primary=d.stale||d.readError?button(ariaCopy.text("text.90081fe1b681a1"),'reanalyze',d.productId):button(d.status==='PENDING'&&!s.unchanged&&!s.blocked?ariaCopy.text("text.58ef121e67fda1"):ariaCopy.text("text.59589fe2a5491f"),'detail',d.id);
    return ariaCopy.template("template.4400c230295b91",[d.id,esc(name),ariaCopy.original(esc(live.sku??f.sku),false),money(d.live?live.price:f.currentPrice),!d.live?ariaCopy.text("text.9c5b7c372f548c"):'',esc(s.title),source,d.aiCompletedAt&&d.source!=='AI'?ariaCopy.text("text.7466ca502c136f"):'',ariaCopy.original(esc(s.reason),true),d.stale||d.readError?'attention':'',esc(s.state),d.source==='AI'&&d.aiCompletedAt?'AI · ':'',date(d.source==='AI'&&d.aiCompletedAt?d.aiCompletedAt:d.createdAt),primary]);
  }
  function detail(d,aiConsent){
    const r=d.report,f=r.facts,s=r.sales,chosen=d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:r.suggestedPercent,best=r.scenarios.find(x=>x.percent===chosen),pending=d.status==='PENDING',displayAction=['BLOCKED','REVIEW'].includes(r.action)?r.action:chosen>f.currentPercent?'DISCOUNT':chosen<f.currentPercent?'RESTORE':'KEEP';
    return ariaCopy.template("template.224c6d76c92af6",[ariaCopy.original(esc(d.live?.name??f.name),false),ariaCopy.original(esc(f.sku),false),ariaCopy.original(esc(f.category),false),d.id,date(d.createdAt),pending?'risk':'good',esc(states[d.status]||d.status),money(f.currentPrice),money(best?.price??r.suggestedPrice),chosen,money(f.basePrice),f.quantity,money(f.minimumPrice),esc(d.source==='AI'?ariaCopy.text("text.6cf91b141633f4"):d.aiCompletedAt?ariaCopy.text("text.952e9d5bbf4019"):ariaCopy.text("text.53f928865efa47")),esc(({PRICE:ariaCopy.text("text.be0e705ced8187"),PROMOTE:ariaCopy.text("text.dfed21ddb3085e"),SOCIAL_REVIEW:ariaCopy.text("text.94993969521e6f"),REORDER:ariaCopy.text("text.544be2e65c673a"),KEEP:ariaCopy.text("text.b4d30cae52894c")})[d.strategy||'PRICE']),d.aiCompletedAt?ariaCopy.text("text.5647a9c1133b24")+date(d.aiCompletedAt):ariaCopy.text("text.1c5cfb5f1a0ba4"),esc(actionNames[displayAction]||displayAction),r.confidence==='MEDIUM'?ariaCopy.text("text.45394ca37bc705"):ariaCopy.text("text.c2b6a5a519369d"),ariaCopy.original(esc(r.reason),true),d.aiLastSkippedNote?ariaCopy.template("template.4868569c4102cb",[date(d.aiLastSkippedAt),esc(d.aiLastSkippedNote)]):'',s.units7,s.units14,s.units30,s.unitsPerDay,r.planning?ariaCopy.template("template.f60e4883f7747d",[r.planning.forecastDays,r.planning.clearanceDays??ariaCopy.text("text.bafef38a3b64ab"),r.planning.nearestExpiryDays??ariaCopy.text("text.f48f07aed856d0")]):'',date(d.reviewAt),best?ariaCopy.template("template.128277f99fa6b6",[r.planning?.forecastDays??ariaCopy.text("text.2333e481df4ea4"),money(best.estimatedRevenue),money(best.estimatedContribution),(best.estimatedRemainingShare*100).toFixed(1),best.estimatedExpiredUnits]):'',pending&&!d.stale&&f.quantity>0&&!['BLOCKED','REVIEW'].includes(r.action)?(['PRICE','KEEP'].includes(d.strategy||'PRICE')?button(ariaCopy.text("text.e15f960d93eb4e"),'apply',d.id):'')+button(ariaCopy.text("text.b8b151d4174391"),'edit',d.id):'',pending&&!d.stale&&f.quantity>0&&r.scenarios.length&&r.scenarios.every(s=>s.estimatedExpiredUnits>0)?button(ariaCopy.text("text.2bed7ff626b909"),'social-propose',d.id):'',pending?button(ariaCopy.text("text.899c5d6de8917a"),'reject',d.id)+button(ariaCopy.text("text.308bb0fa5ba8f7"),'ai-decide',d.id,!aiConsent):'',d.status==='APPLIED'?button(ariaCopy.text("text.52fa20f4f75c7d"),'rollback',d.id):'',button(ariaCopy.text("text.90081fe1b681a1"),'reanalyze',f.productId),button(ariaCopy.text("text.213915c1c01115"),'history',f.productId),button(ariaCopy.text("text.cb22a5827781f5"),'explain',d.id,!aiConsent),d.decidedAt?`<p class="control-help">${date(d.decidedAt)} · ${esc(d.decidedBy)} · ${esc(d.decisionReason)}${d.appliedPrice!=null?ariaCopy.text("text.142f72a982c4ec")+money(d.appliedPrice)+' ('+d.appliedPercent+'%)':''}</p>`:'',r.limitations.map(l=>`<li>${esc(l)}</li>`).join(''),s.approximateRateLow!=null?ariaCopy.template("template.c8d3782fb97b2b",[s.approximateRateLow,s.approximateRateHigh]):'',table([ariaCopy.text("text.53bf07ab6cc701"),ariaCopy.text("text.be0e705ced8187"),ariaCopy.text("text.49fed0f955c0b8"),ariaCopy.text("text.9a535d46d1edab"),ariaCopy.text("text.36157075cabb64"),ariaCopy.text("text.bb78a97a929e0c"),ariaCopy.text("text.b14da7fb9e4e49")],r.scenarios.map(x=>`<tr><td>${x.percent}%</td><td>${money(x.price)}</td><td>${x.estimatedUnits}</td><td>${x.estimatedUnitsLow}–${x.estimatedUnitsHigh}</td><td>${money(x.estimatedRevenue)}</td><td>${money(x.estimatedContribution)}</td><td>${x.estimatedExpiredUnits}</td></tr>`)),table([ariaCopy.text("text.5830b7b4c5bab8"),ariaCopy.text("text.737f7561bd7f35"),ariaCopy.text("text.9e205592c895da"),ariaCopy.text("text.481d116bb2e36b"),ariaCopy.text("text.349065e9c44678")],f.lots.map(l=>`<tr><td>${l.id??ariaCopy.text("text.7ed29aad95e666")}</td><td>${l.quantity}</td><td>${money(l.cost)}</td><td>${esc(l.receivedAt||ariaCopy.text("text.70f9f7113e3b7d"))} / ${esc(l.expiry||ariaCopy.text("text.28c54755b53e07"))}</td><td>${esc(l.storageRequirements||ariaCopy.text("text.dc304b48f361ee"))} · ${esc(({CONFIRMED:ariaCopy.text("text.3807e9938d6f29"),REJECTED:ariaCopy.text("text.6e46da78d37de6"),UNKNOWN:ariaCopy.text("text.502beb4a6297e9")})[l.handlingStatus]||ariaCopy.text("text.502beb4a6297e9"))}</td></tr>`)),d.id,explanation(d.explanation)]);
  }
  function explanation(e){if(!e)return '';return ariaCopy.template("template.7692119f2c908f",[ariaCopy.original(esc(e.note),true),e.explanation?ariaCopy.template("template.2d9e1c8431c33d",[ariaCopy.original(esc(e.explanation),true)]):'',e.risks?`<ul>${e.risks.map(r=>`<li>${esc(r)}</li>`).join('')}</ul>`:'',e.mode==='AI'?ariaCopy.template("template.d66cb866b28c9b",[esc(e.model),e.suggestedPercent??ariaCopy.text("text.dc9337ff5b423e")]):'']);}
  async function mount(ctx){
    const root=document.getElementById('smart-pricing-root');if(!root)return;
    const current=++revision;const {request,notify,form,formField,show}=ctx;
    root.setAttribute('aria-busy','true');
    try{
      const focus=new URLSearchParams(location.search).get('decision');if(focus&&!/^[1-9][0-9]*$/.test(focus))throw new Error(ariaCopy.errorText(ariaCopy.text("text.5e5c2042201891")));
      // Opening this page is read-only. Scheduled work stays with the existing manager.
      const [result,settings,aiStatus,storeInfo]=await Promise.all([request(api+'?'+new URLSearchParams({view,query,page,size:10})),request(api+'/settings'),request(api+'/ai-status'),request('/api/products/store-info')]);
      if(current!==revision||root!==document.getElementById('smart-pricing-root'))return;
      if(focus){result.content=[await request(api+'/'+focus)];result.totalPages=1;result.totalElements=1;page=0;}
      if(result.totalPages>0&&page>=result.totalPages){page=result.totalPages-1;return mount(ctx);}
      const aiLabel=!settings.externalAiEnabled?ariaCopy.text("text.6bf001840fc1a7"):!aiStatus.configured?ariaCopy.text("text.a20ace390d30ee"):({CONNECTED:ariaCopy.text("text.a6d8f1075883bd"),UNTESTED:ariaCopy.text("text.5f9a0b6fd073ea"),UNAVAILABLE:ariaCopy.text("text.1f99710f2e23a3")})[aiStatus.status]||ariaCopy.text("text.08050c7e2dae6d");
      ariaCopy.setProperty(root,"innerHTML",ariaCopy.template("template.f17c3fd529c6ee",[settings.mode==='AUTOPILOT'?'active':'',settings.mode==='AUTOPILOT'?ariaCopy.text("text.70842ff79d9967"):ariaCopy.text("text.e3c66d3a139567"),esc(aiLabel),button(ariaCopy.text("text.985b5e0f2ccf1b"),'settings'),settings.mode==='AUTOPILOT'?button(ariaCopy.text("text.ffd3b5fbcbf68b"),'stop'):'',storeInfo.demonstration?ariaCopy.text("text.094b59fc69f6a2"):'',focus?ariaCopy.template("template.68de77e6755869",[esc(focus),button(ariaCopy.text("text.709f58ac3c0aa8"),'all')]):ariaCopy.template("template.7a29745054698d",[Object.entries(filters).map(([key,label])=>`<button type="button" data-smart="filter" data-id="${key}" aria-pressed="${key===view}">${label}<span>${result.counts?.[key]??0}</span></button>`).join('')]),esc(query),result.content.length?`<div class="price-table-wrap"><table class="price-table"><thead><tr>${[ariaCopy.text("text.4fb40f52fb5a8d"),ariaCopy.text("text.14002d827903f4"),ariaCopy.text("text.e1bebb73c2e179"),ariaCopy.text("text.442e7ad5479655"),ariaCopy.text("text.225077c6d4791e"),ariaCopy.text("text.6b53aafe28bdd5")].map(h=>`<th scope="col">${h}</th>`).join('')}</tr></thead><tbody>${result.content.map(row).join('')}</tbody></table></div>`:`<div class="exp-empty"><h3>${result.counts?.ALL?ariaCopy.text("text.8c6d2a721d4ae7"):ariaCopy.text("text.8fb63abf4e4e9c")}</h3><p>${result.counts?.ALL?ariaCopy.text("text.bcc612e84867f8"):ariaCopy.text("text.679746b9ebade8")}</p>${result.counts?.ALL?button(ariaCopy.text("text.29beefeb2b7ef6"),'all'):ariaCopy.text("text.f900953325447b")}</div>`,button(ariaCopy.text("text.1a9fb1f3cf8e18"),'prev'),result.totalElements,page+1,Math.max(1,result.totalPages),button(ariaCopy.text("text.dc9450d2c27312"),'next')]));
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
            show(d.live?.name??d.report?.facts?.name??ariaCopy.text("text.c859b171dbb57f"),d.readError?ariaCopy.text("text.ca388b16ab9c51"):detail(d,settings.externalAiEnabled));
            document.getElementById('control-dialog-body').onclick=handler;return;
          }
          if(action==='settings'){
            form(ariaCopy.text("text.02f4989f94db24"),ariaCopy.template("template.b3a094cde9813e",[settings.mode==='RECOMMEND'?'selected':'',settings.mode==='AUTOPILOT'?'selected':''])+ariaCopy.template("template.90ec745311e9ac",[settings.externalAiEnabled?'checked':'',aiStatus.scheduledAvailable===false?'disabled':'',settings.scheduledAiEnabled?'checked':'',aiStatus.scheduledAvailable===false?ariaCopy.text("text.2ca9da74119054"):''])+ariaCopy.template("template.2aaf80cd2ec48c",[settings.publicAssistantAiEnabled?'checked':''])+formField(ariaCopy.text("text.4897e8a5597d59"),'publicAiDailyBudget','number',settings.publicAiDailyBudget,{required:true,attrs:'min="1" max="20" step="1"'})+formField(ariaCopy.text("text.2f8d745ad10c5a"),'aiDailyBudget','number',settings.aiDailyBudget,{required:true,attrs:'min="1" max="50" step="1"'})+formField(ariaCopy.text("text.09c14e796390a5"),'maximumDiscount','number',settings.maximumDiscount,{required:true,attrs:'min="0" max="90" step="1"'})+formField(ariaCopy.text("text.8972cab2379f4b"),'cooldownHours','number',settings.cooldownHours,{required:true,attrs:'min="1" max="720" step="1"'})+ariaCopy.template("template.66469647346278",[esc(settings.allowedCategories.join('\n')),settings.allowLowConfidence?'checked':'',settings.allowRestore?'checked':'']),async f=>request(api+'/settings','PUT',{version:settings.version,publicAssistantAiEnabled:f.has('publicAssistantAiEnabled'),publicAiDailyBudget:Number(f.get('publicAiDailyBudget')),externalAiEnabled:f.has('externalAiEnabled'),scheduledAiEnabled:f.has('scheduledAiEnabled'),aiDailyBudget:Number(f.get('aiDailyBudget')),mode:f.get('mode'),maximumDiscount:Number(f.get('maximumDiscount')),cooldownHours:Number(f.get('cooldownHours')),allowedCategories:f.get('allowedCategories').split('\n').map(s=>s.trim()).filter(Boolean),allowLowConfidence:f.has('allowLowConfidence'),allowRestore:f.has('allowRestore')}));return;
          }
          if(action==='stop'){await request(api+'/stop','POST');notify(ariaCopy.text("text.bf0d8283cd81a1"));return mount(ctx);}
          if(action==='all'){history.replaceState(null,'',location.pathname+'#smart');view='ALL';query='';page=0;return mount(ctx);}
          if(action==='prev'||action==='next'){page+=action==='next'?1:-1;return mount(ctx);}
          if(action==='analyze'||action==='reanalyze'){
            const run=await request(action==='analyze'?api+'/analyze':api+'/products/'+id+'/analyze','POST');
            if(action==='reanalyze'&&focus&&run.id)history.replaceState(null,'',location.pathname+'?decision='+run.id+'#smart');
            closeDetail();notify(run.failed?ariaCopy.text("text.3791221d4251cf")+run.failed+ariaCopy.text("text.8888fd81f736f8"):ariaCopy.text("text.9c3cf4c9c6f4f0"),Boolean(run.failed));return mount(ctx);
          }
          if(action==='history'){
            const changes=await request(api+'/products/'+id+'/history');
            show(ariaCopy.text("text.3c18657d7f902d"),changes.length?changes.map(h=>ariaCopy.template("template.0e7269465617a8",[date(h.change.changedAt),esc(h.change.actor),money(Number(h.change.oldBasePrice)*(100-h.change.oldPercent)/100),money(Number(h.change.newBasePrice)*(100-h.change.newPercent)/100),ariaCopy.original(esc(h.change.reason),true),h.unitsBefore7Days,h.unitsAfter,h.daysAfter,money(h.revenueAfter),date(h.observedUntil),ariaCopy.original(esc(h.note),true)])).join(''):ariaCopy.text("text.e7574e95b0879f"));return;
          }
          if(action==='social-propose'){await request('/social-proposals/decisions/'+id,'POST');closeDetail();notify(ariaCopy.text("text.abfa4524e50dab"));location.hash='socialPlans';return;}
          if(action==='ai-decide'){
            const before=result.content.find(d=>String(d.id)===id),output=await request(api+'/'+id+'/ai-decide','POST');closeDetail();
            const fresh=output.source==='AI'&&output.aiCompletedAt!==before?.aiCompletedAt&&!output.aiLastSkippedNote;
            notify(output.aiLastSkippedNote||(fresh?ariaCopy.text("text.201694f2e7472a"):output.source==='AI'?ariaCopy.text("text.06f5a79e91fb30"):ariaCopy.text("text.18ee526c43aec8")),output.source!=='AI'||Boolean(output.aiLastSkippedNote));return mount(ctx);
          }
          if(action==='explain'){
            const output=await request(api+'/'+id+'/explain','POST');
            const slot=document.querySelector(`#control-dialog-body [data-explanation="${id}"]`);if(slot)ariaCopy.setProperty(slot,"innerHTML",explanation(output));
            notify(output.mode==='AI'?ariaCopy.text("text.6205459c8ad04e"):output.note||ariaCopy.text("text.962ed5457b7217"),output.mode!=='AI');return;
          }
          const d=result.content.find(x=>String(x.id)===id);if(!d)return;
          const type=action==='edit'?'apply':action;
          const labels={apply:ariaCopy.text("text.04b6d4e5d07560"),edit:ariaCopy.text("text.e8a95d7d950d73"),reject:ariaCopy.text("text.9663ef4005a8b8"),rollback:ariaCopy.text("text.b7a2995fcd21aa")};if(!labels[action])return;
          form(labels[action],`<p class="control-help wide">${type==='apply'?ariaCopy.text("text.7a093aeb2d9bcb"):ariaCopy.text("text.e76b44550621ad")}</p>`+(type==='apply'?formField(ariaCopy.text("text.44fb09fc17e31d"),'percent','number',d.source==='AI'&&d.selectedPercent!=null?d.selectedPercent:d.report.suggestedPercent,{required:true,attrs:`min="0" max="${d.report.facts.maximumPercent}" step="1"`})+ariaCopy.template("template.1d6d23a0ba4936",[money(d.report.facts.basePrice),money(d.report.facts.minimumPrice)]):'')+ariaCopy.text("text.019df0fe008a85"),async f=>{
            const body={reason:f.get('reason').trim()};if(type==='apply')body.percent=Number(f.get('percent'));
            await request(api+'/'+id+'/'+type,'POST',body);
          });
        }catch(error){notify(error.message,true);}finally{el.disabled=false;}
      };
      root.onclick=handler;
    }catch(error){if(root.isConnected){ariaCopy.setProperty(root,"innerHTML",ariaCopy.text("text.2a6ce79089eb50")+esc(error.message)+ariaCopy.text("text.dba3b73e1eb1d9"));root.querySelector('#price-retry').onclick=()=>mount(ctx);}}
    finally{if(current===revision)root.setAttribute('aria-busy','false');}
  }
  window.ariaSmartPricing={mount};
})();
