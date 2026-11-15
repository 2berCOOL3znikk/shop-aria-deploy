(()=>{'use strict';
 // Only action popovers participate. Help text, evidence and form accordions stay open.
 const selector='details.ar-account,details.pw-menu,details.ox-menu,details.store-company-menu,details[data-popup-menu]';
 const bound=new WeakSet();let sequence=0,pointerFocus=false;
 const menus=()=>[...document.querySelectorAll(selector)];
 const trigger=menu=>menu.querySelector(':scope > summary');
 const items=menu=>[...menu.querySelectorAll('a[href],button:not([disabled]),[tabindex="0"]')].filter(e=>e!==trigger(menu)&&!e.closest('[hidden],[inert]')&&e.getClientRects().length);
 function sync(menu){const summary=trigger(menu);if(!summary)return;const ids=[];for(const panel of menu.children){if(panel===summary)continue;if(!panel.id){let id;do{id='aria-popup-'+(++sequence);}while(document.getElementById(id));panel.id=id;}panel.hidden=!menu.open;panel.inert=!menu.open;ids.push(panel.id);}summary.setAttribute('aria-expanded',String(menu.open));if(ids.length)summary.setAttribute('aria-controls',ids.join(' '));}
 function close(menu,restore=false){if(!menu.open)return;const hadFocus=menu.contains(document.activeElement);menu.open=false;sync(menu);if(restore&&hadFocus)trigger(menu)?.focus();}
 function closeAll(restore=false,except=null){for(const menu of menus())if(menu!==except)close(menu,restore);}
 function bind(menu){if(bound.has(menu))return;bound.add(menu);menu.dataset.popupMenuBound='';sync(menu);menu.addEventListener('toggle',()=>{if(menu.open)closeAll(false,menu);sync(menu);});}
 function refresh(root=document){if(root instanceof Element&&root.matches(selector))bind(root);root.querySelectorAll?.(selector).forEach(bind);}
 // Dismiss after the pointer's click target is fixed. Closing an in-flow menu on
 // pointerdown/focus would move a neighbouring trigger before pointerup.
 document.addEventListener('pointerdown',()=>{pointerFocus=true;},true);
 document.addEventListener('pointercancel',()=>{pointerFocus=false;},true);
 document.addEventListener('focusin',event=>{if(pointerFocus)return;for(const menu of menus())if(menu.open&&!menu.contains(event.target))close(menu);});
 document.addEventListener('click',event=>{pointerFocus=false;for(const menu of menus())if(menu.open&&!menu.contains(event.target))close(menu);const action=event.target.closest('a[href],button');const menu=action?.closest(selector);if(menu&&!trigger(menu)?.contains(action)&&!action.closest('[data-popup-keep-open]'))close(menu,true);},true);
 document.addEventListener('keydown',event=>{
  pointerFocus=false;
  if(event.key==='Escape'&&menus().some(menu=>menu.open)){closeAll(true);event.preventDefault();event.stopPropagation();return;}
  const menu=event.target.closest(selector);if(!menu)return;const summary=trigger(menu);
  if(summary?.contains(event.target)&&['ArrowDown','ArrowUp'].includes(event.key)){menu.open=true;closeAll(false,menu);sync(menu);const choices=items(menu);(event.key==='ArrowDown'?choices[0]:choices.at(-1))?.focus();event.preventDefault();return;}
  if(!menu.open||!['ArrowDown','ArrowUp','Home','End'].includes(event.key)||event.target.matches('input,select,textarea'))return;
  const choices=items(menu),index=choices.indexOf(document.activeElement);if(!choices.length)return;const next=event.key==='Home'?0:event.key==='End'?choices.length-1:(index+(event.key==='ArrowDown'?1:-1)+choices.length)%choices.length;choices[next].focus();event.preventDefault();
 },true);
 for(const event of ['hashchange','popstate','pagehide','pageshow'])window.addEventListener(event,()=>closeAll());
 const start=()=>{refresh();new MutationObserver(records=>{for(const record of records)for(const node of record.addedNodes)if(node instanceof Element)refresh(node);}).observe(document.body,{childList:true,subtree:true});};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
 window.ariaPopovers={closeAll,refresh};
})();
