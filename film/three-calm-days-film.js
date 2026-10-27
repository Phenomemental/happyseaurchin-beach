/* Three Calm Days: One Wave, the film's drawing and its player (spine:three-calm-days 6 and 8.22).
   Lifted from the film made in chat (version two, at 6.2), unchanged in what it draws and how it
   moves: everything inside frame(), the helpers it calls, and the player. Its words are not here.
   The page reads them from the spine (the ten scenes at 6.311 to 6.341, the closing card at 6.4)
   and hands them in with words(); each scene keeps its length from the source. Colours are the
   page's tokens: paper is --ground, sheet --panel, ink --ink, crimson --accent, with the film's
   own --sea, --slop, --lamp and --night. Two changes to the player, for a long page read on a
   phone: with reduced motion asked for, each scene is drawn at its end and holds still; and the
   film rests while it is off the screen, going on from where it was.

     const film = CalmFilm(box);       box holds [data-film=stage] (with its canvas), stamp,
                                       closing, track, title, line, play, back, next, again,
                                       copy, try, trytext, src, srclist and note
     film.words(scenes)                scenes: ten of {t, s, stamp}
     film.copy = text                  what "Copy the three things" puts on the clipboard */
window.CalmFilm = function (box) {
'use strict';
const C01=v=>v<0?0:v>1?1:v, lerp=(a,b,t)=>a+(b-a)*t, seg=(x,a,b)=>C01((x-a)/(b-a)), sm=u=>u*u*(3-2*u), ss=(x,a,b)=>sm(seg(x,a,b));
const stage=box.querySelector('[data-film=stage]'), cv=stage.querySelector('canvas'), ctx=cv.getContext('2d'), root=document.documentElement;
function tok(n){return getComputedStyle(root).getPropertyValue('--'+n).trim();}
function rgb(h){const n=parseInt(h.slice(1),16);return [n>>16&255,n>>8&255,n&255];}
function rgba(h,a){const c=rgb(h);return `rgba(${c[0]},${c[1]},${c[2]},${C01(a)})`;}
function mix(h1,h2,t){const a=rgb(h1),b=rgb(h2);return '#'+a.map((v,i)=>Math.round(lerp(v,b[i],C01(t))).toString(16).padStart(2,'0')).join('');}
let W=600,P={};
function resize(){const r=stage.getBoundingClientRect(),d=Math.min(window.devicePixelRatio||1,2);W=r.width;cv.width=Math.round(r.width*d);cv.height=Math.round(r.height*d);ctx.setTransform(d,0,0,d,0,0);}
new ResizeObserver(resize).observe(stage);resize();
const X=v=>v*W;
const DISPLAY='"Cormorant Garamond", Georgia, serif', BODY='"EB Garamond", Georgia, serif';
function mulberry(s){return()=>{s|=0;s=s+0x6D2B79F5|0;let t=Math.imul(s^s>>>15,1|s);t=t+Math.imul(t^t>>>7,61|t)^t;return((t^t>>>14)>>>0)/4294967296;};}
function label(txt,x,y,a,size,col,align){if(a<=0)return;ctx.fillStyle=rgba(col||P.ink,a);ctx.font=`italic ${X(size||0.028)}px ${BODY}`;ctx.textAlign=align||'center';ctx.textBaseline='alphabetic';ctx.fillText(txt,X(x),X(y));}
function bg(night){ctx.fillStyle=P.paper;ctx.fillRect(0,0,W,W);if(night>0){ctx.fillStyle=rgba(P.night,night);ctx.fillRect(0,0,W,W);}}

/* ---------- the sea and its wave (side on, travelling to the shore on the right) ---------- */
const LEVEL=0.64, SHORE=0.7;
function surf(x,cx,amp,w,T){const back=w*1.5,front=w*0.55,dx=x-cx,k=dx<0?dx/back:dx/front;return LEVEL-amp*Math.exp(-k*k)+0.004*Math.sin(x*48+T*2.2)+0.003*Math.sin(x*91-T*3.1);}
function sea(cx,amp,w,T,slop,lip){
  const col=mix(P.sea,P.slop,slop);
  ctx.beginPath();ctx.moveTo(0,W);for(let i=0;i<=120;i++){const x=i/120*SHORE;ctx.lineTo(X(x),X(surf(x,cx,amp,w,T)));}ctx.lineTo(X(SHORE),W);ctx.closePath();
  ctx.fillStyle=rgba(col,.28);ctx.fill();
  ctx.strokeStyle=rgba(P.ink,.85);ctx.lineWidth=X(0.003);ctx.beginPath();for(let i=0;i<=120;i++){const x=i/120*SHORE;const y=X(surf(x,cx,amp,w,T));i?ctx.lineTo(X(x),y):ctx.moveTo(X(x),y);}ctx.stroke();
  ctx.strokeStyle=rgba(col,.45);ctx.lineWidth=X(0.0015);for(let r=1;r<6;r++){ctx.beginPath();for(let i=0;i<=60;i++){const x=i/60*SHORE;const y=X(surf(x,cx,amp*(1-r*0.16),w,T+r)+r*0.045);i?ctx.lineTo(X(x),y):ctx.moveTo(X(x),y);}ctx.stroke();}
  if(lip>0){const top=surf(cx,cx,amp,w,T);ctx.strokeStyle=rgba(P.ink,.85*lip);ctx.lineWidth=X(0.003);ctx.beginPath();ctx.moveTo(X(cx),X(top));ctx.quadraticCurveTo(X(cx+0.05*lip),X(top-0.01),X(cx+0.06*lip),X(top+0.05*lip));ctx.stroke();
    ctx.fillStyle=rgba(P.sheet,.7*lip);for(let k=0;k<6;k++){ctx.beginPath();ctx.arc(X(cx+0.02+k*0.008),X(top+0.004+Math.sin(T*6+k)*0.004),X(0.004),0,7);ctx.fill();}}
}
function land(){ctx.fillStyle=P.sheet;ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.003);ctx.beginPath();ctx.moveTo(X(SHORE-0.04),W);ctx.lineTo(X(SHORE),X(LEVEL-0.04));ctx.lineTo(W,X(LEVEL-0.04));ctx.lineTo(W,W);ctx.closePath();ctx.fill();ctx.stroke();
  ctx.strokeStyle=rgba(P.ink,.18);ctx.lineWidth=X(0.0012);for(let i=0;i<9;i++){const y=LEVEL+i*0.035;ctx.beginPath();ctx.moveTo(X(SHORE+0.01+i*0.004),X(y));ctx.lineTo(W,X(y));ctx.stroke();}}
const MB=LEVEL-0.04, MH=[0.09,0.1,0.085,0.105];
function mini(i){const w=0.065,x=0.73+i*w,h=MH[i];return {x,w,h,top:MB-h,cx:x+w/2,cy:MB-h+0.035};}
function miniStreet(lit,T){for(let i=0;i<4;i++){const H=mini(i);ctx.fillStyle=P.sheet;ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.0025);
  ctx.beginPath();ctx.moveTo(X(H.x),X(MB));ctx.lineTo(X(H.x),X(H.top));ctx.lineTo(X(H.cx),X(H.top-0.03));ctx.lineTo(X(H.x+H.w),X(H.top));ctx.lineTo(X(H.x+H.w),X(MB));ctx.closePath();ctx.fill();ctx.stroke();
  const L=typeof lit==='function'?lit(i):lit;ctx.fillStyle=rgba(P.night,.18);ctx.fillRect(X(H.x+0.018),X(H.top+0.02),X(0.03),X(0.025));
  if(L>0){ctx.fillStyle=rgba(P.lamp,L);ctx.fillRect(X(H.x+0.018),X(H.top+0.02),X(0.03),X(0.025));}
  ctx.strokeRect(X(H.x+0.018),X(H.top+0.02),X(0.03),X(0.025));}}
function miniMesh(a){if(a<=0)return;ctx.strokeStyle=rgba(P.crimson,a);ctx.lineWidth=X(0.0025);for(let i=0;i<3;i++){const A=mini(i),B=mini(i+1);ctx.beginPath();ctx.moveTo(X(A.cx),X(A.cy));ctx.quadraticCurveTo(X((A.cx+B.cx)/2),X(Math.min(A.top,B.top)-0.05),X(B.cx),X(B.cy));ctx.stroke();}}
function pylon(x,a){ctx.strokeStyle=rgba(P.ink,.8*a);ctx.lineWidth=X(0.0022);const b=MB,t=MB-0.22;ctx.beginPath();ctx.moveTo(X(x-0.018),X(b));ctx.lineTo(X(x),X(t));ctx.lineTo(X(x+0.018),X(b));ctx.moveTo(X(x-0.025),X(t+0.03));ctx.lineTo(X(x+0.025),X(t+0.03));ctx.stroke();}
function dataCentres(a,T){if(a<=0)return;const boxes=[[0.82,0.2,0.1],[0.9,0.24,0.08]];for(const[bx,h,w]of boxes){const top=MB-0.1-h*a;ctx.fillStyle=rgba(P.ink,.08*a);ctx.fillRect(X(bx-w/2),X(top),X(w),X(MB-0.1-top));ctx.strokeStyle=rgba(P.ink,.5*a);ctx.lineWidth=X(0.002);ctx.strokeRect(X(bx-w/2),X(top),X(w),X(MB-0.1-top));
  for(let y=top+0.015;y<MB-0.105;y+=0.016){for(let k=0;k<4;k++){const on=(Math.sin(T*5+y*90+k*3)>0.2);ctx.fillStyle=rgba(on?P.crimson:P.ink,(on?.7:.2)*a);ctx.fillRect(X(bx-w/2+0.01+k*(w-0.02)/4),X(y),X(0.008),X(0.005));}}}}
function wires(a,T){if(a<=0)return;const px=0.745,py=MB-0.19;ctx.strokeStyle=rgba(P.ink,.5*a);ctx.lineWidth=X(0.0015);
  const ends=[[0.8,MB-0.3],[0.9,MB-0.34],[0.77,MB-0.11],[0.84,MB-0.12]];for(const e of ends){ctx.beginPath();ctx.moveTo(X(px),X(py));ctx.quadraticCurveTo(X((px+e[0])/2),X(Math.max(py,e[1])+0.03),X(e[0]),X(e[1]));ctx.stroke();}
  ctx.fillStyle=rgba(P.lamp,a);for(let k=0;k<4;k++){const e=ends[k],u=(T*0.5+k*0.25)%1;ctx.beginPath();ctx.arc(X(lerp(px,e[0],u)),X(lerp(py,e[1],u)+Math.sin(u*Math.PI)*0.025),X(0.004),0,7);ctx.fill();}}
function rider(x,cx,amp,w,T,a){if(a<=0)return;const y=surf(x,cx,amp,w,T),y2=surf(x+0.03,cx,amp,w,T),ang=Math.atan2(X(y2-y),X(0.03));ctx.save();ctx.translate(X(x),X(y));ctx.rotate(ang);ctx.globalAlpha=a;
  ctx.strokeStyle=P.crimson;ctx.lineWidth=X(0.005);ctx.beginPath();ctx.moveTo(X(-0.025),0);ctx.lineTo(X(0.025),0);ctx.stroke();ctx.lineWidth=X(0.0035);
  ctx.beginPath();ctx.moveTo(X(-0.008),0);ctx.lineTo(0,X(-0.022));ctx.lineTo(X(0.01),0);ctx.moveTo(0,X(-0.022));ctx.lineTo(0,X(-0.045));ctx.moveTo(X(-0.016),X(-0.036));ctx.lineTo(X(0.016),X(-0.032));ctx.stroke();ctx.beginPath();ctx.arc(0,X(-0.052),X(0.007),0,7);ctx.fillStyle=P.crimson;ctx.fill();ctx.restore();ctx.globalAlpha=1;}

/* rain of releases, one name per lab (after David's film) */
const LABS=['ChatGPT','Claude','Gemini','Grok','DeepSeek','Kimi','Qwen','Meta'];
function rain(p,T,cx,amp,w){LABS.forEach((lab,k)=>{const a=ss(p,0.02+k*0.09,0.1+k*0.09);if(a<=0)return;const row=k%2,x0=0.1+(k>>1)*0.145+row*0.07,y0=0.1+row*0.07;
  label(lab,x0,y0,a,0.034,P.ink);for(let j=0;j<3;j++){const u=((T*0.3+j/3+k*0.13)%1);const y=lerp(y0+0.02,surf(x0,cx,amp,w,T),u);ctx.strokeStyle=rgba(P.sea,.7*a);ctx.lineWidth=X(0.0025);ctx.beginPath();ctx.moveTo(X(x0),X(y-0.022));ctx.lineTo(X(x0),X(y));ctx.stroke();}});
  label('a new model every few weeks',0.35,0.3,ss(p,0.8,0.92),0.03,P.sea);}
/* the other currents */
const CURRENTS=[['war in Iran',0.15],['gas up 150%',0.225],['fertiliser held back',0.30],['attacks on power and water',0.375],['cables cut under the sea',0.45]];
function currents(p,T){CURRENTS.forEach(([name,y],k)=>{const a=ss(p,0.08+k*0.12,0.2+k*0.12);if(a<=0)return;const reach=lerp(0.02,0.42,a);
  ctx.strokeStyle=rgba(P.crimson,.75*a);ctx.lineWidth=X(0.0025);ctx.beginPath();for(let i=0;i<=40;i++){const x=i/40*reach;const yy=y+0.012*Math.sin(x*30-T*3+k);i?ctx.lineTo(X(x),X(yy)):ctx.moveTo(X(x),X(yy));}ctx.stroke();
  ctx.fillStyle=rgba(P.crimson,a);ctx.beginPath();const hx=reach,hy=y+0.012*Math.sin(hx*30-T*3+k);ctx.moveTo(X(hx+0.014),X(hy));ctx.lineTo(X(hx-0.004),X(hy-0.01));ctx.lineTo(X(hx-0.004),X(hy+0.01));ctx.fill();
  label(name,0.03,y-0.018,a,0.032,P.crimson,'left');});}

/* ---------- the full street, for the scenes that come close ---------- */
const G=0.8,N=6,HX=0.07,HW=0.86/N,HH=[0.22,0.25,0.22,0.27,0.23,0.24];
function house(i){const x=HX+i*HW,h=HH[i],top=G-h;return {x,h,top,wx:x+HW*0.25,wy:top+0.05,ww:HW*0.5,wh:0.06,cx:x+HW/2,cy:top+0.08};}
function ground(){ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.004);ctx.beginPath();ctx.moveTo(0,X(G));ctx.lineTo(W,X(G));ctx.stroke();}
function street(lit,sign){for(let i=0;i<N;i++){const H=house(i);ctx.fillStyle=P.sheet;ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.003);
  ctx.beginPath();ctx.moveTo(X(H.x),X(G));ctx.lineTo(X(H.x),X(H.top));ctx.lineTo(X(H.x+HW/2),X(H.top-0.055));ctx.lineTo(X(H.x+HW),X(H.top));ctx.lineTo(X(H.x+HW),X(G));ctx.closePath();ctx.fill();ctx.stroke();
  const L=typeof lit==='function'?lit(i):lit;ctx.fillStyle=rgba(P.night,.18);ctx.fillRect(X(H.wx),X(H.wy),X(H.ww),X(H.wh));
  if(L>0){ctx.fillStyle=rgba(P.lamp,L);ctx.fillRect(X(H.wx),X(H.wy),X(H.ww),X(H.wh));const g=ctx.createRadialGradient(X(H.cx),X(H.cy),0,X(H.cx),X(H.cy),X(0.09));g.addColorStop(0,rgba(P.lamp,.28*L));g.addColorStop(1,rgba(P.lamp,0));ctx.fillStyle=g;ctx.fillRect(X(H.cx-0.09),X(H.cy-0.09),X(0.18),X(0.18));}
  ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.002);ctx.strokeRect(X(H.wx),X(H.wy),X(H.ww),X(H.wh));ctx.strokeRect(X(H.x+HW*0.38),X(G-0.075),X(0.032),X(0.075));
  if(i===3){ctx.fillStyle=P.sheet;ctx.fillRect(X(H.x+0.012),X(G-0.11),X(HW-0.024),X(0.026));ctx.strokeRect(X(H.x+0.012),X(G-0.11),X(HW-0.024),X(0.026));ctx.fillStyle=sign?P.crimson:P.ink;ctx.font=`600 ${X(0.019)}px ${DISPLAY}`;ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText(sign?'CASH ONLY':'SHOP',X(H.cx),X(G-0.097));}}}
function mast(on,T){const x=0.86,top=0.3;ctx.strokeStyle=rgba(P.ink,.8);ctx.lineWidth=X(0.0025);ctx.beginPath();ctx.moveTo(X(x-0.025),X(G-0.2));ctx.lineTo(X(x),X(top));ctx.lineTo(X(x+0.025),X(G-0.2));ctx.stroke();
  const b=on*(0.5+0.5*Math.sin(T*4));ctx.fillStyle=rgba(P.crimson,0.25+0.75*b);ctx.beginPath();ctx.arc(X(x),X(top-0.008),X(0.009),0,7);ctx.fill();
  if(on>0.05){ctx.strokeStyle=rgba(P.crimson,.5*on);ctx.lineWidth=X(0.002);for(let k=1;k<=2;k++){const r=0.02*k+0.01*((T*0.8)%1);ctx.beginPath();ctx.arc(X(x),X(top),X(r),-2.5,-0.64);ctx.stroke();}}}
function stars(a,T){if(a<=0)return;const r=mulberry(7);ctx.fillStyle=rgba(P.lamp,a);for(let i=0;i<30;i++){const x=r(),y=r()*0.4;ctx.globalAlpha=0.4+0.6*(0.5+0.5*Math.sin(T*1.5+i));ctx.fillRect(X(x),X(y),X(0.003),X(0.003));}ctx.globalAlpha=1;}

/* the ladder */
function ladder(p,T){bg(0);const lx=0.42,rx=0.58,top=0.12,bot=0.86,n=9;ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.005);ctx.beginPath();ctx.moveTo(X(lx),X(top));ctx.lineTo(X(lx),X(bot));ctx.moveTo(X(rx),X(top));ctx.lineTo(X(rx),X(bot));ctx.stroke();
  for(let k=0;k<n;k++){const y=lerp(bot-0.06,top+0.04,k/(n-1));let a=1,drop=0;if(k<3){const g=ss(p,0.15+k*0.12,0.3+k*0.12);a=1-g;drop=g*0.12;}if(a<=0)continue;ctx.strokeStyle=rgba(P.ink,a);ctx.lineWidth=X(0.005);ctx.beginPath();ctx.moveTo(X(lx),X(y+drop));ctx.lineTo(X(rx),X(y+drop));ctx.stroke();}
  ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.004);ctx.beginPath();ctx.moveTo(0,X(bot));ctx.lineTo(W,X(bot));ctx.stroke();
  // a young person at the foot, reaching
  const fx=0.3,reach=ss(p,0.55,0.8);ctx.strokeStyle=P.crimson;ctx.lineWidth=X(0.004);ctx.beginPath();ctx.moveTo(X(fx-0.02),X(bot));ctx.lineTo(X(fx),X(bot-0.07));ctx.lineTo(X(fx+0.02),X(bot));ctx.moveTo(X(fx),X(bot-0.07));ctx.lineTo(X(fx),X(bot-0.14));
  ctx.moveTo(X(fx),X(bot-0.12));ctx.lineTo(X(fx+lerp(0.03,0.09,reach)),X(bot-lerp(0.13,0.22,reach)));ctx.stroke();ctx.fillStyle=P.crimson;ctx.beginPath();ctx.arc(X(fx),X(bot-0.16),X(0.016),0,7);ctx.fill();
  label('the first three rungs',0.63,bot-0.16,ss(p,0.6,0.75),0.032,P.ink,'left');label('36% of employers',0.63,bot-0.11,ss(p,0.65,0.8),0.032,P.crimson,'left');label('cut them this year',0.63,bot-0.07,ss(p,0.65,0.8),0.032,P.crimson,'left');}

/* panic: a brown wash and a shelf emptying */
function panic(p,T){bg(0);const brown=ss(p,0.05,0.6);ctx.fillStyle=rgba(P.slop,.22*brown);ctx.fillRect(0,0,W,W);
  // pages of slop piling on the left
  const r=mulberry(5),np=Math.floor(lerp(0,22,ss(p,0.05,0.7)));for(let k=0;k<np;k++){const x=0.06+r()*0.3,y=0.2+r()*0.55,a=r()*0.6-0.3;ctx.save();ctx.translate(X(x),X(y));ctx.rotate(a);ctx.fillStyle=P.sheet;ctx.strokeStyle=rgba(P.slop,.9);ctx.lineWidth=X(0.002);ctx.fillRect(X(-0.035),X(-0.045),X(0.07),X(0.09));ctx.strokeRect(X(-0.035),X(-0.045),X(0.07),X(0.09));
    ctx.strokeStyle=rgba(P.slop,.6);for(let l=0;l<5;l++){ctx.beginPath();ctx.moveTo(X(-0.025),X(-0.03+l*0.015));ctx.lineTo(X(0.025-(l%2)*0.01),X(-0.03+l*0.015));ctx.stroke();}ctx.restore();}
  label('slop',0.21,0.9,ss(p,0.2,0.35),0.04,P.slop);
  // the shelf on the right
  const L=0.52,R=0.94,shelves=[0.35,0.55,0.75];ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.004);ctx.strokeRect(X(L),X(0.15),X(R-L),X(0.62));for(const y of shelves){ctx.beginPath();ctx.moveTo(X(L),X(y));ctx.lineTo(X(R),X(y));ctx.stroke();}
  for(let s=0;s<3;s++)for(let k=0;k<6;k++){const idx=s*6+k,gone=ss(p,0.1+((idx*7)%18)/18*0.6,0.16+((idx*7)%18)/18*0.6);if(gone>=1)continue;const x=L+0.035+k*0.065,y=shelves[s];ctx.globalAlpha=1-gone;ctx.fillStyle=P.sheet;ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.0022);
    if(s===0){ctx.fillRect(X(x-0.018),X(y-0.12),X(0.036),X(0.12-gone*0.0));ctx.strokeRect(X(x-0.018),X(y-0.12),X(0.036),X(0.12));}else{ctx.fillRect(X(x-0.022),X(y-0.07),X(0.044),X(0.07));ctx.strokeRect(X(x-0.022),X(y-0.07),X(0.044),X(0.07));ctx.fillStyle=rgba(P.crimson,.7);ctx.fillRect(X(x-0.022),X(y-0.045),X(0.044),X(0.02));}ctx.globalAlpha=1;}
  label('panic buying',0.73,0.9,ss(p,0.35,0.5),0.04,P.ink);}

/* the cupboard, built slowly */
function rr(x,y,w,h,r){ctx.beginPath();ctx.moveTo(X(x+r),X(y));ctx.arcTo(X(x+w),X(y),X(x+w),X(y+h),X(r));ctx.arcTo(X(x+w),X(y+h),X(x),X(y+h),X(r));ctx.arcTo(X(x),X(y+h),X(x),X(y),X(r));ctx.arcTo(X(x),X(y),X(x+w),X(y),X(r));ctx.closePath();}
function item(kind,x,base){ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.0025);ctx.fillStyle=P.sheet;
  if(kind==='water'){const w=0.05,h=0.17;ctx.fillStyle=rgba(P.sea,.15);rr(x-w/2,base-h,w,h,0.012);ctx.fill();ctx.stroke();ctx.fillStyle=P.sheet;ctx.fillRect(X(x-0.01),X(base-h-0.02),X(0.02),X(0.02));ctx.strokeRect(X(x-0.01),X(base-h-0.02),X(0.02),X(0.02));}
  else if(kind==='tin'){const w=0.075,h=0.1;ctx.fillRect(X(x-w/2),X(base-h),X(w),X(h));ctx.strokeRect(X(x-w/2),X(base-h),X(w),X(h));ctx.fillStyle=rgba(P.crimson,.75);ctx.fillRect(X(x-w/2),X(base-h*0.62),X(w),X(h*0.3));}
  else if(kind==='torch'){ctx.fillRect(X(x-0.05),X(base-0.04),X(0.08),X(0.032));ctx.strokeRect(X(x-0.05),X(base-0.04),X(0.08),X(0.032));ctx.beginPath();ctx.moveTo(X(x+0.03),X(base-0.04));ctx.lineTo(X(x+0.055),X(base-0.05));ctx.lineTo(X(x+0.055),X(base+0.002));ctx.lineTo(X(x+0.03),X(base-0.008));ctx.closePath();ctx.fill();ctx.stroke();}
  else if(kind==='radio'){ctx.fillRect(X(x-0.07),X(base-0.11),X(0.14),X(0.11));ctx.strokeRect(X(x-0.07),X(base-0.11),X(0.14),X(0.11));ctx.beginPath();ctx.moveTo(X(x+0.04),X(base-0.11));ctx.lineTo(X(x+0.07),X(base-0.19));ctx.stroke();ctx.strokeStyle=P.crimson;ctx.beginPath();ctx.arc(X(x+0.035),X(base-0.055),X(0.022),0,7);ctx.stroke();
    ctx.fillStyle=rgba(P.ink,.5);for(let r=0;r<3;r++)for(let c=0;c<3;c++){ctx.beginPath();ctx.arc(X(x-0.05+c*0.016),X(base-0.08+r*0.022),X(0.004),0,7);ctx.fill();}}
  else if(kind==='cash'){for(let k=0;k<3;k++){ctx.fillStyle=P.sheet;ctx.fillRect(X(x-0.07+k*0.008),X(base-0.03-k*0.012),X(0.12),X(0.028));ctx.strokeRect(X(x-0.07+k*0.008),X(base-0.03-k*0.012),X(0.12),X(0.028));}ctx.fillStyle=P.ink;ctx.font=`600 ${X(0.022)}px ${DISPLAY}`;ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText('£',X(x+0.006),X(base-0.04));}
  else if(kind==='book'){ctx.fillRect(X(x-0.06),X(base-0.13),X(0.11),X(0.13));ctx.strokeRect(X(x-0.06),X(base-0.13),X(0.11),X(0.13));ctx.fillStyle=P.crimson;ctx.fillRect(X(x-0.06),X(base-0.13),X(0.012),X(0.13));ctx.strokeStyle=rgba(P.ink,.35);for(let k=0;k<4;k++){ctx.beginPath();ctx.moveTo(X(x-0.035),X(base-0.1+k*0.025));ctx.lineTo(X(x+0.035),X(base-0.1+k*0.025));ctx.stroke();}}}
function cupboard(p){bg(0);const L=0.12,R=0.88,Tp=0.1,B=0.88,sh=[0.36,0.6,0.84];ctx.fillStyle=P.sheet;ctx.fillRect(X(L),X(Tp),X(R-L),X(B-Tp));ctx.strokeStyle=P.ink;ctx.lineWidth=X(0.004);ctx.strokeRect(X(L),X(Tp),X(R-L),X(B-Tp));ctx.lineWidth=X(0.005);for(const y of sh){ctx.beginPath();ctx.moveTo(X(L),X(y));ctx.lineTo(X(R),X(y));ctx.stroke();}
  const items=[['water',0,0.2],['water',0,0.27],['water',0,0.34],['water',0,0.41],['tin',1,0.2],['tin',1,0.29],['tin',1,0.38],['torch',1,0.56],['radio',1,0.72],['cash',2,0.26],['book',2,0.6]];
  items.forEach((it,k)=>{const a=ss(p,0.05+k*0.07,0.12+k*0.07);if(a<=0)return;ctx.globalAlpha=a;item(it[0],it[2],sh[it[1]]-(1-a)*0.06);ctx.globalAlpha=1;});
  const labs=[['water',0,0.3,0.25],['food',1,0.29,0.5],['torch',1,0.56,0.62],['radio',1,0.72,0.7],['cash',2,0.26,0.78],['numbers on paper',2,0.6,0.85]];
  for(const l of labs)label(l[0],l[2],sh[l[1]]+0.038,ss(p,l[3],l[3]+0.05),0.031,P.crimson);
  const k=Math.min(3,Math.floor(p*4));ctx.fillStyle=P.ink;ctx.font=`600 ${X(0.03)}px ${DISPLAY}`;ctx.textAlign='left';ctx.textBaseline='alphabetic';ctx.fillText(['week one','week two','week three','week four'][k],X(L+0.02),X(Tp+0.05));}

/* the platforms, the threads, and the neighbours */
const TW=[{c:0.22,w:0.17,h:0.6},{c:0.5,w:0.22,h:0.72},{c:0.78,w:0.17,h:0.56}];
function towers(g,a){for(const t of TW){const h=t.h*g;if(h<=0.001)continue;const top=G-h,x=t.c-t.w/2;ctx.fillStyle=rgba(P.ink,.1*a);ctx.fillRect(X(x),X(top),X(t.w),X(h));ctx.strokeStyle=rgba(P.ink,.55*a);ctx.lineWidth=X(0.0025);ctx.strokeRect(X(x),X(top),X(t.w),X(h));
  ctx.strokeStyle=rgba(P.ink,.18*a);ctx.lineWidth=X(0.0012);for(let y=top+0.02;y<G;y+=0.018){ctx.beginPath();ctx.moveTo(X(x+0.008),X(y));ctx.lineTo(X(x+t.w-0.008),X(y));ctx.stroke();}}}
function tpath(i){const H=house(i),t=TW[Math.min(2,Math.floor(i/2))],top=G-t.h;return {a:[H.cx,H.top-0.04],c:[lerp(H.cx,t.c,0.4),lerp(H.top,top,0.75)-0.08],b:[t.c,top+0.02]};}
function qpt(p,u){const m=1-u;return [m*m*p.a[0]+2*m*u*p.c[0]+u*u*p.b[0],m*m*p.a[1]+2*m*u*p.c[1]+u*u*p.b[1]];}
function thread(i,reach,T){const p=tpath(i);ctx.strokeStyle=rgba(P.ink,.55);ctx.lineWidth=X(0.0018);ctx.beginPath();for(let k=0;k<=30;k++){const q=qpt(p,reach*k/30);k?ctx.lineTo(X(q[0]),X(q[1])):ctx.moveTo(X(q[0]),X(q[1]));}ctx.stroke();
  if(reach>0.98){ctx.fillStyle=rgba(P.lamp,.9);for(let k=0;k<2;k++){const q=qpt(p,(T*0.35+i*0.17+k/2)%1);ctx.beginPath();ctx.arc(X(q[0]),X(q[1]),X(0.006),0,7);ctx.fill();}}}
function snapped(i,s){const p=tpath(i);ctx.strokeStyle=rgba(P.ink,.5*(1-s));ctx.lineWidth=X(0.0018);for(const[from,to]of[[0,0.45],[1,0.55]]){ctx.beginPath();for(let k=0;k<=14;k++){const u=lerp(from,to,k/14),q=qpt(p,u);const d=s*0.11*(k/14)**2;k?ctx.lineTo(X(q[0]),X(q[1]+d)):ctx.moveTo(X(q[0]),X(q[1]));}ctx.stroke();}}
function mesh(r0){for(let i=0;i<N-1;i++){const a=house(i),b=house(i+1),r=C01(r0*(N-1)-i);if(r<=0)continue;ctx.strokeStyle=P.crimson;ctx.lineWidth=X(0.003);ctx.beginPath();ctx.moveTo(X(a.cx),X(a.cy));const mx=(a.cx+b.cx)/2,my=Math.min(a.top,b.top)-0.07;for(let k=1;k<=20;k++){const u=r*k/20,m=1-u;ctx.lineTo(X(m*m*a.cx+2*m*u*mx+u*u*b.cx),X(m*m*a.cy+2*m*u*my+u*u*b.cy));}ctx.stroke();}}

/* ---------- one frame, from scene and progress alone ---------- */
function frame(s,p,T){
  P={paper:tok('ground'),sheet:tok('panel'),ink:tok('ink'),crimson:tok('accent'),lamp:tok('lamp'),night:tok('night'),sea:tok('sea'),slop:tok('slop')};
  ctx.clearRect(0,0,W,W);
  // the wave's life runs across the shore scenes: far out, gathering, nearing, at the face
  if(s<=3||s===9){
    const life={0:[0.12,0.04,0.18],1:[0.2,0.08,0.17],2:[0.32,0.13,0.15],3:[0.44,0.19,0.13],9:[0.5,0.2,0.13]}[s];
    let cx=life[0],amp=life[1],w=life[2];
    if(s<3){const nx={0:[0.2,0.08],1:[0.32,0.13],2:[0.44,0.19]}[s];cx=lerp(cx,nx[0],ss(p,0,1));amp=lerp(amp,nx[1],ss(p,0,1));}
    const night=s===9?0.25:0;bg(night);if(s===9)stars(0.4,T);
    if(s===0)rain(ss(p,0.05,0.2),T,cx,amp,w);
    sea(cx,amp,w,T,0,s>=3?ss(p,0.1,0.5):0);
    if(s===1)currents(p,T);
    land();
    pylon(0.745,1);
    const dc=s===2?ss(p,0.2,0.6):s===3?1:s===9?1:0;dataCentres(dc,T);wires(s>=2?(s===2?ss(p,0.15,0.4):1):0.6,T);
    miniStreet(s===9?(i=>ss(p,0.1+i*0.05,0.2+i*0.05)):0.95,T);
    if(s===2){label('data centres: about 50 GW',0.97,MB-0.38,ss(p,0.5,0.65),0.032,P.crimson,'right');label('gas: about a week of winter',0.97,MB+0.13,ss(p,0.65,0.8),0.032,P.crimson,'right');}
    if(s===3){label('Be Ready · three days',0.97,0.2,ss(p,0.3,0.5),0.038,P.crimson,'right');rider(cx-0.06,cx,amp,w,T,ss(p,0.5,0.7));}
    if(s===9){miniMesh(ss(p,0.25,0.5));rider(lerp(cx-0.12,cx-0.04,ss(p,0.1,0.9)),cx,amp,w,T,1);
      const r2=mulberry(3);for(let k=0;k<3;k++)rider(cx-0.2-k*0.07+0.02*Math.sin(T+k),cx,amp,w,T,ss(p,0.3+k*0.1,0.45+k*0.1)*0.85);}
  }
  else if(s===4){const n=lerp(0.42,0.7,ss(p,0.1,0.7));bg(0);ctx.fillStyle=rgba(P.night,n);ctx.fillRect(0,0,W,X(G));stars(0.6,T);mast(1-ss(p,0.45,0.55),T);ground();
    street(i=>1-ss(p,0.12+i*0.07,0.18+i*0.07),p>0.75);label(p>0.8?'no signal · no card · no lights':'',0.5,0.92,ss(p,0.8,0.9),0.034,P.ink);}
  else if(s===5)ladder(p,T);
  else if(s===6)panic(p,T);
  else if(s===7)cupboard(p);
  else if(s===8){bg(0.12);const f=1-ss(p,0.55,0.85);ctx.globalAlpha=0.3+0.7*f;towers(lerp(0.55,1,f),f);ctx.globalAlpha=1;
    label('the big platforms',0.5,0.07,ss(p,0.05,0.15)*f,0.036,P.ink);ground();
    street(i=>p<0.3?0.7:p<0.55?0.7-0.5*ss(p,0.3+i*0.035,0.36+i*0.035):ss(p,0.6+i*0.05,0.7+i*0.05),0);
    for(let i=0;i<N;i++){const t=0.3+i*0.035;if(p<t)thread(i,ss(p,0.02+i*0.03,0.18+i*0.03),T);else snapped(i,ss(p,t,t+0.12));}
    mesh(ss(p,0.6,0.92));label(p>0.6?'next door, and next door':'photos · messages · money · memory',0.5,0.92,p>0.6?ss(p,0.8,0.9):bell(p,0.1,0.16,0.3,0.36),0.034,P.ink);}
}
function bell(x,a,b,c,d){return ss(x,a,b)*(1-ss(x,c,d));}
/* ---------- player ---------- */
const $=k=>box.querySelector('[data-film='+k+']');
const track=$('track'),capT=$('title'),capS=$('line'),stamp=$('stamp'),closing=$('closing'),playBtn=$('play');
/* each scene's length in seconds, from the source; the words come from the page */
const SCENES=[10,10,10,9,9,9,9,10,11,11].map(d=>({d,stamp:'',t:'',s:''}));
SCENES.forEach((sc,i)=>{const b=document.createElement('button');b.className='seg';b.type='button';b.setAttribute('aria-label','Scene '+(i+1)+' of '+SCENES.length);b.appendChild(document.createElement('i'));b.onclick=e=>{e.stopPropagation();go(i);};track.appendChild(b);});
const reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
let idx=0,t=0,playing=!reduce,T=0,last=performance.now(),copyText='';
const LAST=SCENES.length-1;
/* the film runs only while it can be seen */
let seen=true;if('IntersectionObserver' in window)new IntersectionObserver(es=>{seen=es[0].isIntersecting}).observe(stage);
/* with reduced motion a paused scene stands at its end; a playing one holds there for its length */
function go(i){idx=Math.max(0,Math.min(LAST,i));t=reduce&&!playing?SCENES[idx].d*0.999:0;show();}
function show(){const sc=SCENES[idx];capT.textContent=sc.t;capS.textContent=sc.s;stamp.textContent=sc.stamp;stamp.classList.toggle('on',!!sc.stamp);
  const on=idx===LAST&&t>=sc.d*0.6;closing.classList.toggle('on',on);closing.setAttribute('aria-hidden',String(!on));playBtn.textContent=playing?'Pause':'Play';}
function tick(now){const dt=Math.min(0.1,(now-last)/1000);last=now;
  if(!seen||document.hidden){requestAnimationFrame(tick);return;}
  T+=reduce?0:dt;
  if(playing){t+=dt;const d=SCENES[idx].d;if(t>=d){if(idx<LAST){idx++;t=0;}else{t=d;playing=false;}show();}}
  if(idx===LAST&&!closing.classList.contains('on')&&t>=SCENES[idx].d*0.6)show();
  [...track.children].forEach((b,i)=>{b.firstChild.style.width=(i<idx?100:i>idx?0:C01(t/SCENES[idx].d)*100)+'%';});
  /* with reduced motion, each scene is drawn as it ends, and holds still */
  frame(idx,reduce?0.999:C01(t/SCENES[idx].d),T);requestAnimationFrame(tick);}
function playPause(){if(!playing&&idx===LAST&&t>=SCENES[idx].d*(reduce?0.999:1)){go(0);}playing=!playing;if(playing&&reduce)t=0;show();}
playBtn.onclick=playPause;
$('back').onclick=()=>go(t>1.5&&!reduce?idx:idx-1);
$('next').onclick=()=>go(idx+1);
stage.onclick=e=>{if(closing.classList.contains('on')&&closing.contains(e.target))return;playPause();};
$('again').onclick=e=>{e.stopPropagation();playing=true;go(0);};
function toggle(btn,elk){const b=$(btn),el=$(elk);b.onclick=e=>{e.stopPropagation();el.hidden=!el.hidden;b.setAttribute('aria-expanded',String(!el.hidden));};}
toggle('src','srclist');toggle('try','trytext');
const note=$('note');
$('copy').onclick=async e=>{e.stopPropagation();const txt=copyText;
  try{await navigator.clipboard.writeText(txt);note.textContent='Copied. Pass it on.';}catch(err){note.textContent=txt;}note.hidden=false;};
/* the keys work on the film itself, so the page still scrolls with Space everywhere else */
box.addEventListener('keydown',e=>{if(e.target.closest('button,a,textarea,input'))return;if(e.key===' '){e.preventDefault();playPause();}if(e.key==='ArrowRight')go(idx+1);if(e.key==='ArrowLeft')go(idx-1);});
go(0);requestAnimationFrame(tick);

return {
  words(list){(list||[]).forEach((w,i)=>{const sc=SCENES[i];if(!sc||!w)return;sc.t=w.t||'';sc.s=w.s||'';sc.stamp=w.stamp||'';
    track.children[i].setAttribute('aria-label','Scene '+(i+1)+' of '+SCENES.length+(sc.t?': '+sc.t:''));});show();},
  set copy(v){copyText=String(v||'');},
  get copy(){return copyText;}
};
};
