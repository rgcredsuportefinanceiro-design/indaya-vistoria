(function(){
const U='https://iesiwomlbkzqalzzgqmb.supabase.co',K='sb_publishable_WA1aU_o37iR7fanGy1gApA__5d1gqPe';
let token=localStorage.getItem('indaya_cloud_token')||'',refresh=localStorage.getItem('indaya_cloud_refresh')||'',email=localStorage.getItem('indaya_cloud_email')||'',role='',lastRemote='',pushTimer=null,syncing=false;
function H(auth){let h={'apikey':K,'Content-Type':'application/json'};if(auth&&token)h.Authorization='Bearer '+token;return h}
function style(){let s=document.createElement('style');s.textContent='.cloudAuth{position:fixed;inset:0;z-index:300;background:linear-gradient(160deg,#071C3D,#0b3c80);display:flex;align-items:center;justify-content:center;padding:20px}.cloudBox{width:100%;max-width:420px;background:#fff;border-radius:22px;padding:22px;box-shadow:0 20px 60px #0006}.cloudBox img{display:block;width:82px;height:82px;object-fit:cover;border-radius:18px;margin:0 auto 12px}.cloudBox h2{text-align:center;margin:0;color:#071C3D}.cloudBox p{text-align:center;color:#6b778c;font-size:13px}.cloudBox input{width:100%;padding:14px;border:1px solid #d8e0ec;border-radius:13px;margin:6px 0;box-sizing:border-box}.cloudRow{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:8px}.cloudBtn{border:0;border-radius:13px;padding:13px;font-weight:900}.cloudPrimary{background:#e63737;color:white}.cloudSecondary{background:#eaf0f7;color:#17365f}.cloudMsg{font-size:12px;color:#7b8798;text-align:center;min-height:18px;margin-top:8px}.syncChip,.roleChip{font-size:11px;padding:5px 8px;border-radius:999px;color:#fff;margin-left:5px}.syncChip{background:#ffffff22}.roleChip{background:#e63737}';document.head.appendChild(s)}
function showAuth(m){if(document.getElementById('cloudAuth'))return;let d=document.createElement('div');d.id='cloudAuth';d.className='cloudAuth';d.innerHTML='<div class="cloudBox"><img src="indaya_logo.jpg"><h2>Indayá Locações</h2><p>Acesso sincronizado entre celulares</p><input id="cloudEmail" type="email" placeholder="E-mail"><input id="cloudPass" type="password" placeholder="Senha (mínimo 6 caracteres)"><div class="cloudRow"><button class="cloudBtn cloudPrimary" id="cloudLoginBtn">Entrar</button><button class="cloudBtn cloudSecondary" id="cloudSignupBtn">Criar acesso</button></div><div id="cloudMsg" class="cloudMsg">'+(m||'')+'</div></div>';document.body.appendChild(d);d.querySelector('#cloudLoginBtn').onclick=function(){auth(false)};d.querySelector('#cloudSignupBtn').onclick=function(){auth(true)}}
function msg(m){let e=document.getElementById('cloudMsg');if(e)e.textContent=m||''}
async function auth(signup){let em=document.getElementById('cloudEmail').value.trim().toLowerCase(),pw=document.getElementById('cloudPass').value;if(!em||pw.length<6){msg('Informe e-mail e senha com pelo menos 6 caracteres.');return}msg(signup?'Criando acesso...':'Entrando...');try{let url=signup?U+'/auth/v1/signup':U+'/auth/v1/token?grant_type=password',r=await fetch(url,{method:'POST',headers:H(false),body:JSON.stringify({email:em,password:pw})}),x=await r.json();if(!r.ok)throw new Error(x.msg||x.message||x.error_description||'Falha no acesso');if(!x.access_token){msg('Conta criada. Confira seu e-mail para confirmar e depois toque em Entrar.');return}token=x.access_token;refresh=x.refresh_token||'';email=(x.user&&x.user.email)||em;localStorage.setItem('indaya_cloud_token',token);localStorage.setItem('indaya_cloud_refresh',refresh);localStorage.setItem('indaya_cloud_email',email);document.getElementById('cloudAuth')?.remove();await start()}catch(e){msg(e.message||'Não foi possível acessar.')}}
async function refreshSession(){if(!refresh)return false;try{let r=await fetch(U+'/auth/v1/token?grant_type=refresh_token',{method:'POST',headers:H(false),body:JSON.stringify({refresh_token:refresh})}),x=await r.json();if(!r.ok||!x.access_token)return false;token=x.access_token;refresh=x.refresh_token||refresh;localStorage.setItem('indaya_cloud_token',token);localStorage.setItem('indaya_cloud_refresh',refresh);return true}catch(e){return false}}
async function api(path,opt,retry){opt=opt||{};if(retry===undefined)retry=true;let r=await fetch(U+path,Object.assign({},opt,{headers:Object.assign({},H(true),opt.headers||{})}));if(r.status===401&&retry&&await refreshSession())return api(path,opt,false);return r}
function chips(){let row=document.querySelector('.toprow');if(!row||document.getElementById('syncChip'))return;let b=row.querySelector('.brand');if(!b)return;let s=document.createElement('span');s.id='syncChip';s.className='syncChip';s.textContent='☁ conectando';b.appendChild(s);let rr=document.createElement('span');rr.id='roleChip';rr.className='roleChip';b.appendChild(rr)}
function syncText(t){let e=document.getElementById('syncChip');if(e)e.textContent=t}
async function loadRole(){let r=await api('/rest/v1/app_users?select=role,active&email=eq.'+encodeURIComponent(email));if(!r.ok)return false;let a=await r.json();if(!a.length||!a[0].active)return false;role=a[0].role||'vistoriador';let e=document.getElementById('roleChip');if(e)e.textContent=role==='admin'?'ADMIN':'VISTORIADOR';if(role!=='admin'){let st=document.querySelector('.iconbtn');if(st)st.style.display='none';window.openSettings=function(){alert('Apenas o administrador pode acessar as configurações.')}}return true}
function mergeById(remote,local){
  let m=new Map();
  (remote||[]).forEach(function(x){if(x&&x.id)m.set(x.id,x)});
  (local||[]).forEach(function(x){if(x&&x.id)m.set(x.id,Object.assign({},m.get(x.id)||{},x))});
  return Array.from(m.values());
}
function mergeState(remote,local,localWins){
  remote=remote||{};local=local||{};
  let out=Object.assign({},remote,localWins?local:{},!localWins?remote:{});
  ['vehicles','clients','inspections','rentals','maintenance'].forEach(function(k){
    out[k]=localWins?mergeById(remote[k],local[k]):mergeById(local[k],remote[k]);
  });
  out.settings=Object.assign({},remote.settings||{},local.settings||{});
  return out;
}
async function pull(force){
  if(syncing)return;
  syncing=true;syncText('☁ sincronizando');
  try{
    let r=await api('/rest/v1/shared_state?id=eq.main&select=state,updated_at,updated_by');
    if(!r.ok)throw 0;
    let a=await r.json(),row=a[0];
    if(row&&row.state&&Object.keys(row.state).length){
      if(force||row.updated_at!==lastRemote){
        db=mergeState(db,row.state,false);
        if(!db.settings)db.settings={pin:'1234',company:'Indayá Locações'};
        if(!db.rentals)db.rentals=[];
        if(!db.maintenance)db.maintenance=[];
        localStorage.setItem(KEY,JSON.stringify(db));
        lastRemote=row.updated_at||'';
        if(typeof render==='function')render();
      }
    }else await push(true);
    syncText('☁ online');
  }catch(e){syncText('☁ offline')}
  syncing=false;
}
async function push(immediate){
  if(!token)return;
  if(!immediate){clearTimeout(pushTimer);pushTimer=setTimeout(function(){push(true)},900);return}
  if(syncing)return;
  syncing=true;syncText('☁ enviando');
  try{
    let gr=await api('/rest/v1/shared_state?id=eq.main&select=state,updated_at');
    if(!gr.ok)throw 0;
    let ga=await gr.json(),remote=(ga[0]&&ga[0].state)||{};
    db=mergeState(remote,db,true);
    localStorage.setItem(KEY,JSON.stringify(db));
    let r=await api('/rest/v1/shared_state?id=eq.main',{
      method:'PATCH',
      headers:{Prefer:'return=representation'},
      body:JSON.stringify({state:db,updated_at:new Date().toISOString(),updated_by:email})
    });
    if(!r.ok)throw 0;
    let a=await r.json();if(a[0])lastRemote=a[0].updated_at||lastRemote;
    syncText('☁ salvo');
  }catch(e){syncText('☁ erro')}
  syncing=false;
}

function ensureRecoveredRecords(){
  db.vehicles=db.vehicles||[];db.inspections=db.inspections||[];
  function vehicleByPlate(p){return db.vehicles.find(function(v){return String(v.plate||'').toUpperCase()===p})}
  function addVehicle(v){let found=vehicleByPlate(v.plate);if(found)return found;db.vehicles.push(v);return v}
  let etios=addVehicle({id:'recovery_ivn4i66',plate:'IVN4I66',brand:'Toyota',model:'Etios',year:'',color:'',km:'',status:'Disponível',obs:'Registro recuperado do laudo de vistoria de 15/10/2026.'});
  let honda=addVehicle({id:'recovery_uqx4j10',plate:'UQX4J10',brand:'Honda',model:'Start',year:'160',color:'Vermelho',km:'5577',status:'Disponível',obs:'Registro recuperado do laudo de vistoria de 15/10/2026.'});
  let fiesta=addVehicle({id:'recovery_ewq0i58',plate:'EWQ0I58',brand:'Ford',model:'Fiesta sedan',year:'',color:'Preto',km:'',status:'Disponível',obs:'Registro recuperado do laudo de vistoria de 15/10/2026.'});
  function addInspection(rec){
    if(db.inspections.some(function(i){return i.id===rec.id || (i.vehicleId===rec.vehicleId&&i.date===rec.date)}))return;
    db.inspections.push(rec);
  }
  addInspection({id:'recovery_insp_ivn4i66_20261015',vehicleId:etios.id,date:'2026-10-15T13:01',km:'',fuel:'Reserva',status:'Aprovado',obs:'O óleo estava um pouco baixo',checks:{'Pneus':'OK','Óleo do motor':'Atenção','Água / Aditivo':'OK','Luzes e faróis':'OK','Limpadores':'OK','Vidros e retrovisores':'OK','Lataria':'OK','Interior e limpeza':'OK','Documentos':'OK','Estepe / Macaco / Chave de roda':'OK'},photos:[],signature:'',recoveredFromPdf:true});
  addInspection({id:'recovery_insp_uqx4j10_20261015',vehicleId:honda.id,date:'2026-10-15T13:04',km:'5577',fuel:'Reserva',status:'Aprovado',obs:'',checks:{'Pneus':'OK','Óleo do motor':'OK','Água / Aditivo':'OK','Luzes e faróis':'OK','Limpadores':'OK','Vidros e retrovisores':'OK','Lataria':'OK','Interior e limpeza':'OK','Documentos':'OK','Estepe / Macaco / Chave de roda':'OK'},photos:[],signature:'',recoveredFromPdf:true});
  addInspection({id:'recovery_insp_ewq0i58_20261015',vehicleId:fiesta.id,date:'2026-10-15T13:10',km:'',fuel:'Reserva',status:'Aprovado',obs:'Motorista relata que está falhando e bebendo muito',checks:{'Pneus':'OK','Óleo do motor':'Atenção','Água / Aditivo':'Atenção','Luzes e faróis':'OK','Limpadores':'OK','Vidros e retrovisores':'OK','Lataria':'Atenção','Interior e limpeza':'Atenção','Documentos':'OK','Estepe / Macaco / Chave de roda':'OK'},photos:[],signature:'',recoveredFromPdf:true});
  localStorage.setItem(KEY,JSON.stringify(db));
}
async function start(){chips();sessionStorage.setItem('indaya_auth','1');document.getElementById('login')?.classList.add('hidden');if(!await loadRole()){logout('Acesso não autorizado.');return}let old=window.save;if(old&&!old.__cloud){let w=function(){old();push(false)};w.__cloud=true;window.save=w}await pull(true);ensureRecoveredRecords();if(typeof render==='function')render();await push(true);setInterval(function(){pull(false)},12000)}
function logout(m){token='';refresh='';email='';localStorage.removeItem('indaya_cloud_token');localStorage.removeItem('indaya_cloud_refresh');localStorage.removeItem('indaya_cloud_email');showAuth(m||'')}
window.indayaCloudLogout=function(){logout('')};style();if(token&&email)start().catch(function(){logout('Entre novamente.')});else showAuth('');
})();

window.shareInspectionPdf=function(id){
  try{
    let i=db.inspections.find(function(x){return x.id===id});
    let v=db.vehicles.find(function(x){return x.id===i.vehicleId});
    let rental=[...db.rentals].reverse().find(function(r){return r.vehicleId===i.vehicleId});
    let client=rental?db.clients.find(function(x){return x.id===rental.clientId}):null;
    let plate=(v&&v.plate)||'veiculo';
    let bad=Object.entries(i.checks||{}).filter(function(e){return e[1]!=='OK'}).map(function(e){return e[0]+': '+e[1]}).join('\n');
    let payload={
      tipo:'Vistoria semanal',
      placa:plate,
      veiculo:((v&&v.brand)||'')+' '+((v&&v.model)||''),
      ano:(v&&v.year)||'-',
      cor:(v&&v.color)||'-',
      data:dbr(i.date),
      km:i.km||'-',
      combustivel:i.fuel||'-',
      status:i.status||'-',
      responsavel:localStorage.getItem('indaya_cloud_email')||'Equipe Indayá',
      cliente:client?{nome:client.name||'-',cpf:client.cpf||'-',telefone:client.phone||'-',cnh:client.cnh||'-'}:{},
      checks:i.checks||{},
      observacoes:i.obs||'Sem observações',
      atencao:bad||'Nenhum',
      assinatura:i.signature||'',
      photos:i.photos||[],
      photoLabels:['Frente','Traseira','Lateral esquerda','Lateral direita','Painel / KM','Interior']
    };
    if(window.Android&&Android.sharePdf) Android.sharePdf('vistoria-'+plate+'.pdf',JSON.stringify(payload));
    else if(window.Android&&Android.printPage) Android.printPage();
  }catch(e){ alert('Não foi possível gerar o PDF desta vistoria.'); }
};

(function(){
  let old=window.inspectionDetail;
  if(typeof old==='function'&&!old.__pdfWrapped){
    let wrapped=function(id){
      old(id);
      setTimeout(function(){
        let actions=document.querySelector('#modal .actions');
        if(actions&&!document.getElementById('sharePdfBtn')){
          let b=document.createElement('button');
          b.id='sharePdfBtn';
          b.className='btn success';
          b.textContent='PDF no WhatsApp';
          b.onclick=function(){window.shareInspectionPdf(id)};
          actions.insertBefore(b,actions.firstChild);
        }
      },60);
    };
    wrapped.__pdfWrapped=true;
    window.inspectionDetail=wrapped;
  }
})();


/* Laudo Pro: garante que o botão antigo "Compartilhar" também gere o PDF completo */
window.shareInspection=function(id){ return window.shareInspectionPdf(id); };

(function(){
  let oldDetail=window.inspectionDetail;
  if(typeof oldDetail==='function'&&!oldDetail.__laudoPro){
    let pro=function(id){
      oldDetail(id);
      setTimeout(function(){
        let actions=document.querySelector('#modal .actions');
        if(!actions)return;
        let buttons=actions.querySelectorAll('button');
        if(buttons[0]){buttons[0].textContent='Compartilhar PDF no WhatsApp';buttons[0].className='btn success';buttons[0].onclick=function(){window.shareInspectionPdf(id)};}
        if(buttons[1]){buttons[1].textContent='Gerar PDF completo';buttons[1].onclick=function(){window.shareInspectionPdf(id)};}
        let badge=document.createElement('div');
        badge.style.cssText='margin-top:10px;padding:9px 11px;border-radius:10px;background:#e8f1ff;color:#174f93;font-size:12px;font-weight:800';
        badge.textContent='Laudo Pro Indayá: inclui identidade visual, checklist, assinatura e fotos salvas na vistoria.';
        if(!document.getElementById('laudoProBadge')){badge.id='laudoProBadge';actions.parentElement.insertBefore(badge,actions);}
      },80);
    };
    pro.__laudoPro=true;
    window.inspectionDetail=pro;
  }
})();