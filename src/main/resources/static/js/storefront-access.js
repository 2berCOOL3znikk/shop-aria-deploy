// Public demo and authenticated company preview share the same catalogue UI.
// This component never widens server access or selects another company's data.
(()=>{'use strict';
 const t=(ru,en)=>ariaCopy.language==='en'?en:ru;
 const style=document.createElement('link');style.rel='stylesheet';style.href='/css/storefront-access.css';document.head.append(style);
 const domReady=document.readyState==='loading'?new Promise(resolve=>document.addEventListener('DOMContentLoaded',resolve,{once:true})):Promise.resolve();
 async function access(){
  const capabilities=await ariaCopy.capabilities;
  if(!capabilities)return {allowed:false,unavailable:true};
  if(capabilities.publicStorefront)return {allowed:true,public:true};
  if(!ariaAuth.getToken())return {allowed:false};
  try{const response=await ariaAuth.authFetch('/api/auth/session',{cache:'no-store'});if(!response.ok)return {allowed:false};const session=await response.json();return {allowed:session.role==='ADMIN',preview:session.role==='ADMIN'};}catch{return {allowed:false,unavailable:true};}
 }
 const ready=access().then(async result=>{
  await domReady;
  if(!result.allowed){
   const main=document.querySelector('main');if(main){main.replaceChildren();const panel=document.createElement('section');panel.className='storefront-access-state';
    const heading=document.createElement('h1');heading.textContent=result.unavailable?t('Не удалось открыть витрину','Could not open the storefront'):t('Витрина компании закрыта','This company storefront is private');
    const note=document.createElement('p');note.textContent=result.unavailable?t('Проверьте соединение и повторите загрузку.','Check your connection and try again.'):t('Предпросмотр доступен владельцу после входа. Каталог компании не публикуется автоматически.','The owner can preview after signing in. A company catalogue is not published automatically.');
    const link=document.createElement('a');link.className='btn-glow';link.href=result.unavailable?location.pathname+location.search:'/login.html?next='+encodeURIComponent(location.pathname+location.search);link.textContent=result.unavailable?t('Повторить','Try again'):t('Войти в ARIA','Sign in to ARIA');panel.append(heading,note,link);main.append(panel);
   }
   document.querySelectorAll('#aria-shell-links a').forEach(a=>{if(['/cart.html','/favorites.html','/social.html'].includes(a.getAttribute('href')))a.hidden=true;});
  }else if(result.preview){
   document.body.dataset.storefrontPreview='true';const banner=document.createElement('aside');banner.className='storefront-preview-note';banner.setAttribute('aria-label',t('Предпросмотр компании','Company preview'));const text=document.createElement('span');text.textContent=t('Закрытый предпросмотр вашей компании','Private preview of your company');const back=document.createElement('a');back.href='/control.html#products';back.textContent=t('Вернуться в кабинет','Back to workspace');banner.append(text,back);document.querySelector('main')?.prepend(banner);
   document.querySelectorAll('.storefront-help,[data-open-assistant],a[href="#assistant"]').forEach(el=>el.hidden=true);
  }
  return result;
 });
 window.ariaStorefront={ready};
})();
