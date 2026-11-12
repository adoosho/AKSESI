const CACHE='aksesi-offline-v1';
const CORE=['/','/index.html','/aksesi-icon.svg','/favicon.png'];
self.addEventListener('install',event=>{event.waitUntil((async()=>{const c=await caches.open(CACHE);await c.addAll(CORE);self.skipWaiting()})())});
self.addEventListener('activate',event=>{event.waitUntil((async()=>{const keys=await caches.keys();await Promise.all(keys.filter(k=>k.startsWith('aksesi-offline-')&&k!==CACHE).map(k=>caches.delete(k)));await self.clients.claim()})())});
self.addEventListener('fetch',event=>{
 const req=event.request;if(req.method!=='GET')return;
 const u=new URL(req.url);
 if(u.pathname.includes('/functions/v1/')||u.pathname.includes('/auth/v1/'))return;
 const media=(u.hostname.endsWith('.supabase.co')&&u.pathname.includes('/storage/v1/object/public/aksesi-media/'))||['image','video'].includes(req.destination);
 const staticAsset=u.origin===self.location.origin||u.hostname==='cdn.jsdelivr.net'||u.hostname==='unpkg.com';
 if(!media&&!staticAsset)return;
 if(req.mode==='navigate'){event.respondWith((async()=>{try{const fresh=await fetch(req);const c=await caches.open(CACHE);await c.put('/index.html',fresh.clone());return fresh}catch(e){return (await caches.match('/index.html'))||Response.error()}})());return}
 event.respondWith((async()=>{const c=await caches.open(CACHE),cached=await c.match(req);if(cached){if(media)fetch(req).then(async res=>{if(res.ok){await c.put(req,res.clone());const keys=await c.keys(),items=keys.filter(k=>{const x=new URL(k.url);return x.hostname.endsWith('.supabase.co')&&x.pathname.includes('/storage/v1/object/public/aksesi-media/')});for(const old of items.slice(0,Math.max(0,items.length-120)))await c.delete(old)} }).catch(()=>{});return cached}try{const res=await fetch(req);if(res.ok||res.type==='opaque'){await c.put(req,res.clone());if(media){const keys=await c.keys(),items=keys.filter(k=>{const x=new URL(k.url);return x.hostname.endsWith('.supabase.co')&&x.pathname.includes('/storage/v1/object/public/aksesi-media/')});for(const old of items.slice(0,Math.max(0,items.length-120)))await c.delete(old)}}return res}catch(e){return cached||Response.error()}})());
});
