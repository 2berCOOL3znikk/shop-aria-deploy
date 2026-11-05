(() => {
  'use strict';
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const number = v => new Intl.NumberFormat(ariaCopy.locale()).format(Number(v || 0));
  const money = v => new Intl.NumberFormat(ariaCopy.locale(), {style:'currency',currency:'KZT',maximumFractionDigits:2}).format(Number(v || 0));
  const stamp = v => v ? new Date(v).toLocaleString(ariaCopy.locale(), {dateStyle:'short',timeStyle:'short'}) : ariaCopy.text("text.8f602a91bd5ffc");
  const link = (text, section, cls='ws-link') => `<a class="${cls}" href="#${section}">${text}<span aria-hidden="true">↗</span></a>`;
  const empty = (text, action, section) => `<div class="ws-empty"><span aria-hidden="true">○</span><p>${text}</p>${action ? link(action,section) : ''}</div>`;
  const metric = (label, value, note, section) => `<a class="ws-metric" href="#${section}"><span>${label}<i aria-hidden="true">↗</i></span><strong>${value}</strong><small>${note}</small></a>`;
  function salesChart(daily) {
    const max = Math.max(0,...daily.map(d => Number(d.revenue)));
    if (!max) return empty(ariaCopy.text("text.2c9c2399998a59"), ariaCopy.text("text.0a440a38189fe4"), 'orders');
    return ariaCopy.template("template.54fbea819a66ac",[daily.map(d => {
      const label = new Date(d.date+'T12:00:00').toLocaleDateString(ariaCopy.locale(),{day:'numeric',month:'short'});
      return ariaCopy.template("template.398cfcb6b75d81",[Number(d.revenue)/max*100,esc(label),esc(money(d.revenue)),Number(d.orders),esc(money(d.revenue)),esc(label),Number(d.orders),new Date(d.date+'T12:00:00').getDate()]);
    }).join(''),esc(daily[0]?.date),esc(daily.at(-1)?.date)]);
  }
  async function mount({request}) {
    const root = document.getElementById('workspace-root'); if (!root) return;
    try {
      const d = await request('/workspace/overview'); if (!root.isConnected) return;
      const o=d.overview,t=d.today,r=d.risks,m=d.manager;
      const enabled=m.enabled;
      const status=!enabled?ariaCopy.text("text.3f5ef6d306e615"):m.lastError?ariaCopy.text("text.f348d7ff4eb4fb"):m.mode==='AUTOPILOT'?ariaCopy.text("text.70842ff79d9967"):ariaCopy.text("text.50c69bb752c981");
      const provider=m.provider;
      const aiNote=!provider.configured?ariaCopy.text("text.42a17a486f1426"):!m.externalAiConsent?ariaCopy.text("text.faa4ab450dc239"):provider.status==='UNAVAILABLE'?ariaCopy.text("text.1f99710f2e23a3"):provider.lastSuccess?ariaCopy.text("text.7019d5b5afbd62")+stamp(provider.lastSuccess):m.lastDecisionAt?ariaCopy.text("text.4cdb7da0200c96")+stamp(m.lastDecisionAt):ariaCopy.text("text.09139585952b98");
      const tasks=[
        [o.pendingOrders,ariaCopy.text("text.0938c426416553"),ariaCopy.text("text.f58dc46179add0"),'orders'],
        [r.pendingDecisions,ariaCopy.text("text.3f79f51cc69944"),ariaCopy.text("text.403e8dead760e6"),'smart'],
        [o.lowStock,ariaCopy.text("text.3e7d7e375fbf96"),ariaCopy.text("text.6b9fbc4b91b3a6"),'products'],
        [o.expiredLots,ariaCopy.text("text.eec24834a083d7"),ariaCopy.text("text.1ff4e05dc429a4"),'lots'],
        [r.openSignals,ariaCopy.text("text.073b0884cc24b6"),ariaCopy.text("text.8591d9115ef5d9"),'manager'],
        [r.failedTasks,ariaCopy.text("text.b172bc7d108c49"),ariaCopy.text("text.15b16570e8e367"),'manager']
      ].filter(x=>x[0]>0);
      ariaCopy.setProperty(document.getElementById('workspace-company'),"textContent",d.organization.name);
      ariaCopy.setProperty(document.getElementById('workspace-kind'),"textContent",d.organization.demonstration?ariaCopy.text("text.6b5c6d2aef728b"):ariaCopy.text("text.c07628a643884d"));
      ariaCopy.setProperty(root,"innerHTML",ariaCopy.template("template.cf8acd01cc735b",[ariaCopy.original(esc(d.organization.name),false),d.organization.demonstration?ariaCopy.text("text.6947bcb56aa843"):'',metric(ariaCopy.text("text.e16ee6c571c923"),money(d.sales.revenueToday),number(d.sales.ordersToday)+ariaCopy.text("text.009c5c462dcf93"),'orders'),metric(ariaCopy.text("text.d126705e2c12ad"),number(o.products),ariaCopy.text("text.8288674708ff67"),'products'),metric(ariaCopy.text("text.ccb65e80baeebd"),number(o.availableUnits),ariaCopy.text("text.4dc5450e97cbbf"),'lots'),metric(ariaCopy.text("text.c5dba2b0e4cce1"),number(d.stores.connected+Number(d.stores.testConnected||0)),d.stores.testConnected?number(d.stores.testConnected)+ariaCopy.text("text.fc80be8d0bfcdb")+number(d.stores.connected)+ariaCopy.text("text.618d18cca7de3a"):d.stores.attention?number(d.stores.attention)+ariaCopy.text("text.28722087b2c4a9"):ariaCopy.text("text.f6a7b3a843d85e"),'stores'),link(ariaCopy.text("text.8621d4f8a62632"),'manager'),[
          [ariaCopy.text("text.dd8cda258d94bd"),t.productsAnalyzed],[ariaCopy.text("text.a02a86deaed014"),t.aiDecisions],[ariaCopy.text("text.16bfe9a5922676"),t.productDrafts],[ariaCopy.text("text.82c994de43e764"),t.priceApplied],[ariaCopy.text("text.73e5e584ac2c17"),t.purchaseDrafts],[ariaCopy.text("text.e648f522c127ca"),t.socialDrafts]
        ].map(([label,v])=>`<div><strong>${number(v)}</strong><span>${label}</span></div>`).join(''),enabled?'is-on':'',esc(status),number(t.engineDecisions),esc(aiNote),salesChart(d.sales.daily),tasks.length,tasks.length?`<div class="ws-task-list">${tasks.map(([count,title,note,section])=>`<a href="#${section}"><span class="ws-task-count">${number(count)}</span><span><strong>${title}</strong><small>${note}</small></span><span aria-hidden="true">›</span></a>`).join('')}</div>`:empty(ariaCopy.text("text.f3828ecce0bebb"),ariaCopy.text("text.a4df7deea9ba15"),'manager'),link(ariaCopy.text("text.a26e110955a6a5"),'audit'),d.recent.length?`<ol class="ws-timeline">${d.recent.slice(0,5).map(e=>`<li><span class="ws-timeline-dot" aria-hidden="true"></span><a href="#${esc(e.section)}">${esc(ariaCopy.systemMessage(e.title))}</a><time>${esc(stamp(e.at))}</time></li>`).join('')}</ol>`:empty(ariaCopy.text("text.2305b6ae34e7cc"),ariaCopy.text("text.c5dba2b0e4cce1"),'stores'),[[ariaCopy.text("text.de6426b7e45e95"),ariaCopy.text("text.48a13f36431da3"),'lots'],[ariaCopy.text("text.caf07f33f43e0a"),ariaCopy.text("text.05d6641d39c6f7"),'pos'],[ariaCopy.text("text.8a2cef326c404d"),ariaCopy.text("text.59ff2e3d8cb581"),'stores']].map(([title,note,section])=>`<a href="#${section}"><span><strong>${title}</strong><small>${note}</small></span><span aria-hidden="true">↗</span></a>`).join(''),esc(stamp(d.asOf)),esc(d.timeZone),esc(d.date)]));
    } catch (error) {
      if (!root.isConnected) return;
      ariaCopy.setProperty(root,"innerHTML",ariaCopy.template("template.5cd5e31c715333",[esc(error.message)]));
      root.querySelector('button').onclick=()=>mount({request});
    }
  }
  async function mountDemoCatalogue(ctx){
    const root=document.getElementById('demo-catalogue-settings');if(!root)return;
    try{
      const state=await ctx.request('/demo-catalogue');if(!root.isConnected||!state.available)return;
      root.hidden=false;
      ariaCopy.setProperty(root,"innerHTML",ariaCopy.template("template.50353b6e77f391",[number(state.references),number(state.imported)]));
      root.querySelector('button').onclick=()=>ctx.form(ariaCopy.text("text.baff1a347a18d9"),ariaCopy.template("template.18e59be7530500",[ariaCopy.original(esc(state.note),true)]),async f=>{
        if(!f.has('confirmed'))throw new Error(ariaCopy.errorText(ariaCopy.text("text.d1c81475f43ad7")));
        const result=await ctx.request('/demo-catalogue','POST',{confirmed:true});
        return ariaCopy.template("template.aa3cb5d080fb6f",[result.created.length,result.preserved.length]);
      });
    }catch(error){if(root.isConnected){root.hidden=false;ariaCopy.setProperty(root,"innerHTML",ariaCopy.text("text.971d1810cf387f"));}}
  }
  window.ariaWorkspace={mount,mountDemoCatalogue};
})();
