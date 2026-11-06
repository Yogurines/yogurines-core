// Exporta capas estáticas y datos de la página de propuesta para usarlos en el mod.
const {chromium}=require('playwright');const fs=require('fs');
const out=process.argv[2];fs.mkdirSync(out,{recursive:true});
(async()=>{const b=await chromium.launch();const p=await b.newPage();
await p.goto('file://'+process.cwd()+'/menu.html');await p.waitForTimeout(1500);
const r=await p.evaluate(()=>{const o={};const L=layout('C');
  for(const bg of['A','B','C']){const S=bgStatic(bg,'C');o['bg_'+bg]=S.c.toDataURL();
    if(bg==='A'){o.glow=S.glow.toDataURL();o.cables=S.cables.map(pp=>pp.filter((_,i)=>i%1===0).map(q=>[Math.round(q[0]),Math.round(q[1])]));o.forge={cx:S.cx,cy:S.cy}}
    if(bg==='B'){o.holo=S.yc.toDataURL();o.hack={cx:S.cx,hz:S.hz}}
    if(bg==='C'){o.leds=S.leds.map(l=>[l.x,l.y,l.s,Math.round(l.ph*1000)/1000,l.amber?1:0]);o.trays=S.trays.map(pp=>pp.map(q=>[Math.round(q[0]),Math.round(q[1])]));o.server={cx:S.cx,vy:S.vy,bw:S.bw}}}
  // logo B "OVERCLOCK"
  const P=logoParts('Overclock');const c=document.createElement('canvas');c.width=P.w+40;c.height=80;const x=c.getContext('2d');
  const info=drawLogo(x,'B','Overclock',Math.round(c.width/2),6,true,1.0);o.logo=c.toDataURL();o.logoInfo={w:c.width,h:c.height,vi:info.vi};
  o.layout=L;return o});
const meta={};for(const k in r){const v=r[k];if(typeof v==='string'&&v.startsWith('data:image/png')){fs.writeFileSync(out+'/'+k+'.png',Buffer.from(v.split(',')[1],'base64'))}else meta[k]=v}
fs.writeFileSync(out+'/menu_data.json',JSON.stringify(meta));console.log(Object.keys(r).join(' '));await b.close()})();
