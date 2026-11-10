(()=>{'use strict';
 const en=ariaCopy.language==='en';
 const capabilities=fetch('/api/company-signup/capabilities',{cache:'no-store'}).then(response=>response.ok?response.json():null).catch(()=>null);
 async function mount(root=document){const available=await capabilities;if(!available?.enabled)return;
  const note=root.querySelector('#company-login-note');if(note){ariaCopy.setProperty(note,'textContent',en?'Operator sign-in. Applicants check their status using the saved application code.':'Вход оператора. Заявитель проверяет статус по сохранённому коду заявки.');note.hidden=false;}
  const login=root.querySelector('#login-form');if(login&&!root.querySelector('[data-company-entry]')){const p=document.createElement('p');p.dataset.companyEntry='';p.className='auth-footer';const link=document.createElement('a');link.href='/company-start.html';link.textContent=en?'Request a company workspace':'Заявка на кабинет компании';p.append(link);login.after(p);}
  const settings=root.querySelector('.ws-settings-grid');if(settings&&!root.querySelector('[data-company-operator]')){const link=document.createElement('a');link.dataset.companyOperator='';link.className='ws-panel';link.href='/company-requests.html';const title=document.createElement('h3');title.textContent=en?'Company applications':'Заявки компаний';const p=document.createElement('p');p.textContent=en?'Review requests and prepare separate workspaces.':'Проверка заявок и подготовка отдельных кабинетов.';link.append(title,p);settings.append(link);}
 }
 window.ariaCompanyEntry={mount,capabilities};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>mount());else mount();
})();
