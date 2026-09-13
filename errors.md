2026-09-06 16:09:52.180  8624-8759  cr_CookieManager        com.sloosh.tv                        E  Unable to get cookies due to error parsing URL: https://antipathic-as.stravers.live/?token_movie=0db06d820a3aa69c0d2dd0b02484ff&token=ffbd312217e27c4245f2678afe1881&translation=66
<html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="referrer" content="always">
<meta name="referrer" content="unsafe-url">
<style>html, body, iframe { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; border:0; }</style>
<script>

                                                                                                    (function() {
                                                                                                      if (window.__slooshAllohaResolverInstalled) return;
                                                                                                      window.__slooshAllohaResolverInstalled = true;
                                                                                                      var capturedHeaders = {};
                                                                                                      var lastPayload = '';
                                                                                                      var lastM3u8 = '';
                                                                                                    
                                                                                                      function fixVisibility(targetDoc) {
                                                                                                        try {
                                                                                                          if (!targetDoc) return;
                                                                                                          Object.defineProperty(targetDoc, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                                                                                                          Object.defineProperty(targetDoc, 'hidden', { get: function() { return false; }, configurable: true });
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function getBridge(win) {
                                                                                                        if (win && win.AndroidAllohaResolver && typeof win.AndroidAllohaResolver.post === 'function') return win.AndroidAllohaResolver;
                                                                                                        if (win && win.AndroidBridge && typeof win.AndroidBridge.post === 'function') return win.AndroidBridge;
                                                                                                        if (window.AndroidAllohaResolver && typeof window.AndroidAllohaResolver.post === 'function') return window.AndroidAllohaResolver;
                                                                                                        if (window.AndroidBridge && typeof window.AndroidBridge.post === 'function') return window.AndroidBridge;
                                                                                                        try {
                                                                                                          if (window.top && window.top.AndroidAllohaResolver && typeof window.top.AndroidAllohaResolver.post === 'function') return window.top.AndroidAllohaResolver;
                                                                                                          if (window.top && window.top.AndroidBridge && typeof window.top.AndroidBridge.post === 'function') return window.top.AndroidBridge;
                                                                                                        } catch(e) {}
                                                                                                        try {
                                                                                                          if (window.parent && window.parent.AndroidAllohaResolver && typeof window.parent.AndroidAllohaResolver.post === 'function') return window.parent.AndroidAllohaResolver;
                                                                                                          if (window.parent && window.parent.AndroidBridge && typeof window.parent.AndroidBridge.post === 'function') return window.parent.AndroidBridge;
                                                                                                        } catch(e) {}
                                                                                                        return null;
                                                                                                      }
                                                                                                    
                                                                                                      function post(type, payload, targetWin) {
                                                                                                        try {
                                                                                                          var bridge = getBridge(targetWin);
                                                                                                          if (bridge) {
                                                                                                            var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
                                                                                                            bridge.post(data);
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function putHeader(name, value) {
                                                                                                        if (!name || !value) return;
                                                                                                        capturedHeaders[String(name).toLowerCase()] = String(value);
                                                                                                      }
                                                                                                    
                                                                                                      function defaultHeaders(win) {
                                                                                                        try {
                                                                                                          if (win.location && win.location.origin) {
                                                                                                            putHeader('origin', win.location.origin);
                                                                                                            putHeader('referer', win.location.origin + '/');
                                                                                                          }
                                                                                                          if (win.navigator && win.navigator.userAgent) {
                                                                                                            putHeader('user-agent', win.navigator.userAgent);
                                                                                                          }
                                                                                                          putHeader('accept', '*/*');
                                                                                                          putHeader('sec-fetch-dest', 'empty');
                                                                                                          putHeader('sec-fetch-mode', 'cors');
                                                                                                          putHeader('sec-fetch-site', 'cross-site');
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function looksPlayable(text) {
                                                                                                        return typeof text === 'string' && (
                                                                                                          text.indexOf('hlsSource') !== -1 ||
                                                                                                          text.indexOf('.m3u8') !== -1 ||
                                                                                                          text.indexOf('.mp4') !== -1 ||
                                                                                                          text.indexOf('.vtt') !== -1
                                                                                                        );
                                                                                                      }
                                                                                                    
                                                                                                      function report(payload, targetWin) {
                                                                                                        if (!looksPlayable(payload)) return;
                                                                                                        if (payload === lastPayload) return;
                                                                                                        lastPayload = payload;
                                                                                                        post('payload', payload, targetWin);
                                                                                                      }
                                                                                                    
                                                                                                      function scan(win) {
                                                                                                        try {
                                                                                                          if (!win || !win.document) return;
                                                                                                          defaultHeaders(win);
                                                                                                          var chunks = [];
                                                                                                          if (win.location && win.location.href) chunks.push(win.location.href);
2026-09-06 16:09:52.180  8624-8759  cr_CookieManager        com.sloosh.tv                        E        if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
for (var i = 0; i < media.length; i++) {
var s = media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '';
if (s) chunks.push(s);
}
if (win.performance && win.performance.getEntriesByType) {
var entries = win.performance.getEntriesByType('resource');
for (var p = 0; p < entries.length; p++) {
var name = entries[p].name || '';
if (looksPlayable(name)) chunks.push(name);
}
}
if (win.hlsSource) {
try { chunks.push(JSON.stringify({ hlsSource: win.hlsSource })); } catch(e) {}
}
if (win.player && win.player.config) {
try { chunks.push(JSON.stringify(win.player.config)); } catch(e) {}
}
if (win.fileList) {
try { chunks.push(JSON.stringify(win.fileList)); } catch(e) {}
}
report(chunks.join('\n'), win);
} catch(e) {}
}

                                                                                                      function triggerPlay(win) {
                                                                                                        try {
                                                                                                          if (!win || !win.document) return;
                                                                                                          fixVisibility(win.document);
                                                                                                    
                                                                                                          // 1. Always dismiss "Continue watching" modal (time_save) prompt if present
                                                                                                          var timeSaveBtn = win.document.querySelector('.time_save__btn') || win.document.querySelector('.time_save:not(.hidden) button');
                                                                                                          if (timeSaveBtn && typeof timeSaveBtn.click === 'function') {
                                                                                                            timeSaveBtn.click();
                                                                                                          }
                                                                                                    
                                                                                                          // 2. Direct <video> play
                                                                                                          var video = win.document.querySelector('video');
                                                                                                          if (video) {
                                                                                                            video.muted = true;
                                                                                                            if (video.paused) {
                                                                                                              video.play().catch(function(){});
                                                                                                            }
                                                                                                          }
                                                                                                    
                                                                                                          // 3. Plyr player API if available
                                                                                                          if (win.player && typeof win.player.play === 'function') {
                                                                                                            try { win.player.play(); } catch(e) {}
                                                                                                          }
                                                                                                    
                                                                                                          // 4. Click all known play button selectors (Plyr, Alloha, VideoJS, JWPlayer, etc.)
                                                                                                          var playSelectors = [
                                                                                                            '.plyr__control--overlaid',
                                                                                                            'button[data-plyr="play"]',
                                                                                                            '.plyr__control[data-plyr="play"]',
                                                                                                            '.allplay__play-btn',
                                                                                                            'button.play',
                                                                                                            '.play-btn',
                                                                                                            '.vjs-big-play-button',
                                                                                                            '.jw-display-icon-container',
                                                                                                            '[data-plyr="play"]'
                                                                                                          ];
                                                                                                          for (var s = 0; s < playSelectors.length; s++) {
                                                                                                            var el = win.document.querySelector(playSelectors[s]);
                                                                                                            if (el && typeof el.click === 'function') {
                                                                                                              el.click();
                                                                                                            }
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function hookWsInstance(ws, targetWin) {
                                                                                                        try {
                                                                                                          if (!ws || ws.__slooshWsHooked) return;
                                                                                                          ws.__slooshWsHooked = true;
                                                                                                          ws.addEventListener('message', function(event) {
                                                                                                            try {
                                                                                                              var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
                                                                                                              if (msg && msg.type === 'config_update' && msg.edge_hash) {
                                                                                                                putHeader('accepts-controls', msg.edge_hash);
                                                                                                                if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
                                                                                                                post('headers', '', targetWin);
                                                                                                                var bridge = getBridge(targetWin);
                                                                                                                if (bridge && typeof bridge.onConfigUpdate === 'function') {
                                                                                                                  bridge.onConfigUpdate(msg.edge_hash, msg.ttl || 0, JSON.stringify(capturedHeaders));
                                                                                                                }
                                                                                                              }
                                                                                                              if (typeof event.data === 'string' && looksPlayable(event.data)) {
                                                                                                                report(event.data, targetWin);
                                                                                                              }
                                                                                                            } catch(e) {
                                                                                                              if (typeof event.data === 'string' && looksPlayable(event.data)) {
                                                                                                                report(event.data, targetWin);
                                                                                                              }
                                                                                                            }
                                                                                                          });
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function install(win) {
                                                                                                        try {
                                                                                                          if (!win) return;
                                                                                                          fixVisibility(win.document);
                                                                                                          defaultHeaders(win);
                                                                                                    
                                                                                                          if (!win.__slooshAllohaHooksInstalled) {
                                                                                                            win.__slooshAllohaHooksInstalled = true;
                                                                                                    
                                                                                                            // 1. Hook XMLHttpRequest
                                                                                                            var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
2026-09-06 16:09:52.181  8624-8759  cr_CookieManager        com.sloosh.tv                        E          var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
if (originalOpen && originalSetHeader) {
win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
this.__slooshUrl = requestUrl || '';
this.addEventListener('load', function() {
var responseUrl = this.responseURL || this.__slooshUrl || '';
var responseText = '';
try { responseText = this.responseText || ''; } catch(e) {}
if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText, win);
if (responseText && responseText.indexOf('hlsSource') !== -1) report(responseText, win);
if (looksPlayable(responseText)) report(responseText, win);
if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) {
lastM3u8 = responseUrl;
post('payload', responseUrl, win);
}
});
return originalOpen.apply(this, arguments);
};
win.XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
putHeader(name, value);
return originalSetHeader.apply(this, arguments);
};
}

                                                                                                            // 2. Hook Fetch
                                                                                                            var originalFetch = win.fetch;
                                                                                                            if (originalFetch) {
                                                                                                              win.fetch = function(input, init) {
                                                                                                                try {
                                                                                                                  var requestUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                                                                                                                  if (init && init.headers) {
                                                                                                                    if (typeof init.headers.forEach === 'function') init.headers.forEach(function(v, k) { putHeader(k, v); });
                                                                                                                    else for (var key in init.headers) putHeader(key, init.headers[key]);
                                                                                                                  }
                                                                                                                  if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(v, k) { putHeader(k, v); });
                                                                                                                  if (looksPlayable(requestUrl)) post('payload', requestUrl, win);
                                                                                                                } catch(e) {}
                                                                                                    
                                                                                                                return originalFetch.apply(this, arguments).then(function(response) {
                                                                                                                  try {
                                                                                                                    var responseUrl = response.url || '';
                                                                                                                    if (looksPlayable(responseUrl)) post('payload', responseUrl, win);
                                                                                                                    var clone = response.clone();
                                                                                                                    clone.text().then(function(text) { report(text, win); }).catch(function(){});
                                                                                                                  } catch(e) {}
                                                                                                                  return response;
                                                                                                                });
                                                                                                              };
                                                                                                            }
                                                                                                    
                                                                                                            // 3. Hook WebSocket constructor & prototypes
                                                                                                            if (win.WebSocket) {
                                                                                                              var OrigWS = win.WebSocket;
                                                                                                              win.WebSocket = function(url, protocols) {
                                                                                                                var ws = protocols ? new OrigWS(url, protocols) : new OrigWS(url);
                                                                                                                hookWsInstance(ws, win);
                                                                                                                return ws;
                                                                                                              };
                                                                                                              win.WebSocket.prototype = OrigWS.prototype;
                                                                                                              win.WebSocket.CONNECTING = OrigWS.CONNECTING;
                                                                                                              win.WebSocket.OPEN = OrigWS.OPEN;
                                                                                                              win.WebSocket.CLOSING = OrigWS.CLOSING;
                                                                                                              win.WebSocket.CLOSED = OrigWS.CLOSED;
                                                                                                    
                                                                                                              var origSend = OrigWS.prototype.send;
                                                                                                              if (origSend) {
                                                                                                                OrigWS.prototype.send = function(data) {
                                                                                                                  hookWsInstance(this, win);
                                                                                                                  return origSend.apply(this, arguments);
                                                                                                                };
                                                                                                              }
                                                                                                    
                                                                                                              var origAddEvt = OrigWS.prototype.addEventListener;
                                                                                                              if (origAddEvt) {
                                                                                                                OrigWS.prototype.addEventListener = function(type, listener, options) {
                                                                                                                  hookWsInstance(this, win);
                                                                                                                  return origAddEvt.apply(this, arguments);
                                                                                                                };
                                                                                                              }
                                                                                                    
                                                                                                              try {
                                                                                                                var origOnMessageDesc = Object.getOwnPropertyDescriptor(OrigWS.prototype, 'onmessage');
                                                                                                                Object.defineProperty(OrigWS.prototype, 'onmessage', {
                                                                                                                  get: function() {
                                                                                                                    return origOnMessageDesc && origOnMessageDesc.get ? origOnMessageDesc.get.call(this) : this.__slooshOnMessage;
2026-09-06 16:09:52.182  8624-8759  cr_CookieManager        com.sloosh.tv                        E                },
set: function(fn) {
hookWsInstance(this, win);
if (origOnMessageDesc && origOnMessageDesc.set) {
origOnMessageDesc.set.call(this, fn);
} else {
this.__slooshOnMessage = fn;
}
},
configurable: true,
enumerable: true
});
} catch(e) {}
}
}
} catch(e) {}
}

                                                                                                      function tick() {
                                                                                                        install(window);
                                                                                                        scan(window);
                                                                                                        triggerPlay(window);
                                                                                                        try {
                                                                                                          var frames = document.querySelectorAll('iframe');
                                                                                                          for (var i = 0; i < frames.length; i++) {
                                                                                                            try {
                                                                                                              var fWin = frames[i].contentWindow;
                                                                                                              if (fWin) {
                                                                                                                install(fWin);
                                                                                                                scan(fWin);
                                                                                                                triggerPlay(fWin);
                                                                                                              }
                                                                                                            } catch(e) {}
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      window.__slooshTick = tick;
                                                                                                      fixVisibility(document);
                                                                                                      install(window);
                                                                                                      scan(window);
                                                                                                      triggerPlay(window);
                                                                                                      tick();
                                                                                                      setInterval(tick, 150);
                                                                                                      window.addEventListener('load', tick);
                                                                                                      window.addEventListener('DOMContentLoaded', tick);
                                                                                                    })();
                                                                                                    
                                                                                                                </script>
                                                                                                            </head>
                                                                                                            <body>
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=0db06d820a3aa69c0d2dd0b02484ff&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=66" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
                                                                                                                <script>
                                                                                                                (function() {
                                                                                                                    var f = document.getElementById('alloha_iframe');
                                                                                                                    if (f) {
                                                                                                                        f.onload = function() {
                                                                                                                            try {
                                                                                                                                if (typeof window.__slooshTick === 'function') window.__slooshTick();
                                                                                                                            } catch(e) {}
                                                                                                                        };
                                                                                                                    }
                                                                                                                })();
                                                                                                                </script>
                                                                                                            
                                                                                                            </body></html>
                                                                                                    java.net.URISyntaxException: Bad address: https://antipathic-as.stravers.live/?token_movie=0db06d820a3aa69c0d2dd0b02484ff&token=ffbd312217e27c4245f2678afe1881&translation=66
                                                                                                    <html><head>
                                                                                                                <meta charset="UTF-8">
                                                                                                                <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
                                                                                                                <meta name="referrer" content="always">
                                                                                                                <meta name="referrer" content="unsafe-url">
                                                                                                                <style>html, body, iframe { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; border:0; }</style>
                                                                                                                <script>
                                                                                                                
                                                                                                    (function() {
                                                                                                      if (window.__slooshAllohaResolverInstalled) return;
                                                                                                      window.__slooshAllohaResolverInstalled = true;
                                                                                                      var capturedHeaders = {};
                                                                                                      var lastPayload = '';
                                                                                                      var lastM3u8 = '';
                                                                                                    
                                                                                                      function fixVisibility(targetDoc) {
                                                                                                        try {
                                                                                                          if (!targetDoc) return;
                                                                                                          Object.defineProperty(targetDoc, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                                                                                                          Object.defineProperty(targetDoc, 'hidden', { get: function() { return false; }, configurable: true });
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function getBridge(win) {
                                                                                                        if (win && win.AndroidAllohaResolver && typeof win.AndroidAllohaResolver.post === 'function') return win.AndroidAllohaResolver;
                                                                                                        if (win && win.AndroidBridge && typeof win.AndroidBridge.post === 'function') return win.AndroidBridge;
                                                                                                        if (window.AndroidAllohaResolver && typeof window.AndroidAllohaResolver.post === 'function') return window.AndroidAllohaResolver;
                                                                                                        if (window.AndroidBridge && typeof window.AndroidBridge.post === 'function') return window.AndroidBridge;
                                                                                                        try {
                                                                                                          if (window.top && window.top.AndroidAllohaResolver && typeof window.top.AndroidAllohaResolver.post === 'function') return window.top.AndroidAllohaResolver;
                                                                                                          if (window.top && window.top.AndroidBridge && typeof window.top.AndroidBridge.post === 'function') return window.top.AndroidBridge;
                                                                                                        } catch(e) {}
                                                                                                        try {
2026-09-06 16:09:52.182  8624-8759  cr_CookieManager        com.sloosh.tv                        E        if (window.parent && window.parent.AndroidAllohaResolver && typeof window.parent.AndroidAllohaResolver.post === 'function') return window.parent.AndroidAllohaResolver;
if (window.parent && window.parent.AndroidBridge && typeof window.parent.AndroidBridge.post === 'function') return window.parent.AndroidBridge;
} catch(e) {}
return null;
}

                                                                                                      function post(type, payload, targetWin) {
                                                                                                        try {
                                                                                                          var bridge = getBridge(targetWin);
                                                                                                          if (bridge) {
                                                                                                            var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
                                                                                                            bridge.post(data);
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function putHeader(name, value) {
                                                                                                        if (!name || !value) return;
                                                                                                        capturedHeaders[String(name).toLowerCase()] = String(value);
                                                                                                      }
                                                                                                    
                                                                                                      function defaultHeaders(win) {
                                                                                                        try {
                                                                                                          if (win.location && win.location.origin) {
                                                                                                            putHeader('origin', win.location.origin);
                                                                                                            putHeader('referer', win.location.origin + '/');
                                                                                                          }
                                                                                                          if (win.navigator && win.navigator.userAgent) {
                                                                                                            putHeader('user-agent', win.navigator.userAgent);
                                                                                                          }
                                                                                                          putHeader('accept', '*/*');
                                                                                                          putHeader('sec-fetch-dest', 'empty');
                                                                                                          putHeader('sec-fetch-mode', 'cors');
                                                                                                          putHeader('sec-fetch-site', 'cross-site');
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function looksPlayable(text) {
                                                                                                        return typeof text === 'string' && (
                                                                                                          text.indexOf('hlsSource') !== -1 ||
                                                                                                          text.indexOf('.m3u8') !== -1 ||
                                                                                                          text.indexOf('.mp4') !== -1 ||
                                                                                                          text.indexOf('.vtt') !== -1
                                                                                                        );
                                                                                                      }
                                                                                                    
                                                                                                      function report(payload, targetWin) {
                                                                                                        if (!looksPlayable(payload)) return;
                                                                                                        if (payload === lastPayload) return;
                                                                                                        lastPayload = payload;
                                                                                                        post('payload', payload, targetWin);
                                                                                                      }
                                                                                                    
                                                                                                      function scan(win) {
                                                                                                        try {
                                                                                                          if (!win || !win.document) return;
                                                                                                          defaultHeaders(win);
                                                                                                          var chunks = [];
                                                                                                          if (win.location && win.location.href) chunks.push(win.location.href);
                                                                                                          if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
                                                                                                          var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
                                                                                                          for (var i = 0; i < media.length; i++) {
                                                                                                            var s = media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '';
                                                                                                            if (s) chunks.push(s);
                                                                                                          }
                                                                                                          if (win.performance && win.performance.getEntriesByType) {
                                                                                                            var entries = win.performance.getEntriesByType('resource');
                                                                                                            for (var p = 0; p < entries.length; p++) {
                                                                                                              var name = entries[p].name || '';
                                                                                                              if (looksPlayable(name)) chunks.push(name);
                                                                                                            }
                                                                                                          }
                                                                                                          if (win.hlsSource) {
                                                                                                            try { chunks.push(JSON.stringify({ hlsSource: win.hlsSource })); } catch(e) {}
                                                                                                          }
                                                                                                          if (win.player && win.player.config) {
                                                                                                            try { chunks.push(JSON.stringify(win.player.config)); } catch(e) {}
                                                                                                          }
                                                                                                          if (win.fileList) {
                                                                                                            try { chunks.push(JSON.stringify(win.fileList)); } catch(e) {}
                                                                                                          }
                                                                                                          report(chunks.join('\n'), win);
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function triggerPlay(win) {
                                                                                                        try {
                                                                                                          if (!win || !win.document) return;
                                                                                                          fixVisibility(win.document);
                                                                                                    
                                                                                                          // 1. Always dismiss "Continue watching" modal (time_save) prompt if present
                                                                                                          var timeSaveBtn = win.document.querySelector('.time_save__btn') || win.document.querySelector('.time_save:not(.hidden) button');
                                                                                                          if (timeSaveBtn && typeof timeSaveBtn.click === 'function') {
                                                                                                            timeSaveBtn.click();
                                                                                                          }
                                                                                                    
                                                                                                          // 2. Direct <video> play
                                                                                                          var video = win.document.querySelector('video');
                                                                                                          if (video) {
                                                                                                            video.muted = true;
                                                                                                            if (video.paused) {
                                                                                                              video.play().catch(function(){});
                                                                                                            }
                                                                                                          }
                                                                                                    
                                                                                                          // 3. Plyr player API if available
                                                                                                          if (win.player && typeof win.player.play === 'function') {
                                                                                                            try { win.player.play(); } catch(e) {}
                                                                                                          }
                                                                                                    
                                                                                                          // 4. Click all known play button selectors (Plyr, Alloha, VideoJS, JWPlayer, etc.)
                                                                                                          var playSelectors = [
                                                                                                            '.plyr__control--overlaid',
2026-09-06 16:09:52.182  8624-8759  cr_CookieManager        com.sloosh.tv                        E          'button[data-plyr="play"]',
'.plyr__control[data-plyr="play"]',
'.allplay__play-btn',
'button.play',
'.play-btn',
'.vjs-big-play-button',
'.jw-display-icon-container',
'[data-plyr="play"]'
];
for (var s = 0; s < playSelectors.length; s++) {
var el = win.document.querySelector(playSelectors[s]);
if (el && typeof el.click === 'function') {
el.click();
}
}
} catch(e) {}
}

                                                                                                      function hookWsInstance(ws, targetWin) {
                                                                                                        try {
                                                                                                          if (!ws || ws.__slooshWsHooked) return;
                                                                                                          ws.__slooshWsHooked = true;
                                                                                                          ws.addEventListener('message', function(event) {
                                                                                                            try {
                                                                                                              var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
                                                                                                              if (msg && msg.type === 'config_update' && msg.edge_hash) {
                                                                                                                putHeader('accepts-controls', msg.edge_hash);
                                                                                                                if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
                                                                                                                post('headers', '', targetWin);
                                                                                                                var bridge = getBridge(targetWin);
                                                                                                                if (bridge && typeof bridge.onConfigUpdate === 'function') {
                                                                                                                  bridge.onConfigUpdate(msg.edge_hash, msg.ttl || 0, JSON.stringify(capturedHeaders));
                                                                                                                }
                                                                                                              }
                                                                                                              if (typeof event.data === 'string' && looksPlayable(event.data)) {
                                                                                                                report(event.data, targetWin);
                                                                                                              }
                                                                                                            } catch(e) {
                                                                                                              if (typeof event.data === 'string' && looksPlayable(event.data)) {
                                                                                                                report(event.data, targetWin);
                                                                                                              }
                                                                                                            }
                                                                                                          });
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function install(win) {
                                                                                                        try {
                                                                                                          if (!win) return;
                                                                                                          fixVisibility(win.document);
                                                                                                          defaultHeaders(win);
                                                                                                    
                                                                                                          if (!win.__slooshAllohaHooksInstalled) {
                                                                                                            win.__slooshAllohaHooksInstalled = true;
                                                                                                    
                                                                                                            // 1. Hook XMLHttpRequest
                                                                                                            var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
                                                                                                            var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
                                                                                                            if (originalOpen && originalSetHeader) {
                                                                                                              win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
                                                                                                                this.__slooshUrl = requestUrl || '';
                                                                                                                this.addEventListener('load', function() {
                                                                                                                  var responseUrl = this.responseURL || this.__slooshUrl || '';
                                                                                                                  var responseText = '';
                                                                                                                  try { responseText = this.responseText || ''; } catch(e) {}
                                                                                                                  if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText, win);
                                                                                                                  if (responseText && responseText.indexOf('hlsSource') !== -1) report(responseText, win);
                                                                                                                  if (looksPlayable(responseText)) report(responseText, win);
                                                                                                                  if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) {
                                                                                                                    lastM3u8 = responseUrl;
                                                                                                                    post('payload', responseUrl, win);
                                                                                                                  }
                                                                                                                });
                                                                                                                return originalOpen.apply(this, arguments);
                                                                                                              };
                                                                                                              win.XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                                                                                                                putHeader(name, value);
                                                                                                                return originalSetHeader.apply(this, arguments);
                                                                                                              };
                                                                                                            }
                                                                                                    
                                                                                                            // 2. Hook Fetch
                                                                                                            var originalFetch = win.fetch;
                                                                                                            if (originalFetch) {
                                                                                                              win.fetch = function(input, init) {
                                                                                                                try {
                                                                                                                  var requestUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                                                                                                                  if (init && init.headers) {
                                                                                                                    if (typeof init.headers.forEach === 'function') init.headers.forEach(function(v, k) { putHeader(k, v); });
                                                                                                                    else for (var key in init.headers) putHeader(key, init.headers[key]);
                                                                                                                  }
                                                                                                                  if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(v, k) { putHeader(k, v); });
                                                                                                                  if (looksPlayable(requestUrl)) post('payload', requestUrl, win);
                                                                                                                } catch(e) {}
2026-09-06 16:09:52.182  8624-8759  cr_CookieManager        com.sloosh.tv                        E              return originalFetch.apply(this, arguments).then(function(response) {
try {
var responseUrl = response.url || '';
if (looksPlayable(responseUrl)) post('payload', responseUrl, win);
var clone = response.clone();
clone.text().then(function(text) { report(text, win); }).catch(function(){});
} catch(e) {}
return response;
});
};
}

                                                                                                            // 3. Hook WebSocket constructor & prototypes
                                                                                                            if (win.WebSocket) {
                                                                                                              var OrigWS = win.WebSocket;
                                                                                                              win.WebSocket = function(url, protocols) {
                                                                                                                var ws = protocols ? new OrigWS(url, protocols) : new OrigWS(url);
                                                                                                                hookWsInstance(ws, win);
                                                                                                                return ws;
                                                                                                              };
                                                                                                              win.WebSocket.prototype = OrigWS.prototype;
                                                                                                              win.WebSocket.CONNECTING = OrigWS.CONNECTING;
                                                                                                              win.WebSocket.OPEN = OrigWS.OPEN;
                                                                                                              win.WebSocket.CLOSING = OrigWS.CLOSING;
                                                                                                              win.WebSocket.CLOSED = OrigWS.CLOSED;
                                                                                                    
                                                                                                              var origSend = OrigWS.prototype.send;
                                                                                                              if (origSend) {
                                                                                                                OrigWS.prototype.send = function(data) {
                                                                                                                  hookWsInstance(this, win);
                                                                                                                  return origSend.apply(this, arguments);
                                                                                                                };
                                                                                                              }
                                                                                                    
                                                                                                              var origAddEvt = OrigWS.prototype.addEventListener;
                                                                                                              if (origAddEvt) {
                                                                                                                OrigWS.prototype.addEventListener = function(type, listener, options) {
                                                                                                                  hookWsInstance(this, win);
                                                                                                                  return origAddEvt.apply(this, arguments);
                                                                                                                };
                                                                                                              }
                                                                                                    
                                                                                                              try {
                                                                                                                var origOnMessageDesc = Object.getOwnPropertyDescriptor(OrigWS.prototype, 'onmessage');
                                                                                                                Object.defineProperty(OrigWS.prototype, 'onmessage', {
                                                                                                                  get: function() {
                                                                                                                    return origOnMessageDesc && origOnMessageDesc.get ? origOnMessageDesc.get.call(this) : this.__slooshOnMessage;
                                                                                                                  },
                                                                                                                  set: function(fn) {
                                                                                                                    hookWsInstance(this, win);
                                                                                                                    if (origOnMessageDesc && origOnMessageDesc.set) {
                                                                                                                      origOnMessageDesc.set.call(this, fn);
                                                                                                                    } else {
                                                                                                                      this.__slooshOnMessage = fn;
                                                                                                                    }
                                                                                                                  },
                                                                                                                  configurable: true,
                                                                                                                  enumerable: true
                                                                                                                });
                                                                                                              } catch(e) {}
                                                                                                            }
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      function tick() {
                                                                                                        install(window);
                                                                                                        scan(window);
                                                                                                        triggerPlay(window);
                                                                                                        try {
                                                                                                          var frames = document.querySelectorAll('iframe');
                                                                                                          for (var i = 0; i < frames.length; i++) {
                                                                                                            try {
                                                                                                              var fWin = frames[i].contentWindow;
                                                                                                              if (fWin) {
                                                                                                                install(fWin);
                                                                                                                scan(fWin);
                                                                                                                triggerPlay(fWin);
                                                                                                              }
                                                                                                            } catch(e) {}
                                                                                                          }
                                                                                                        } catch(e) {}
                                                                                                      }
                                                                                                    
                                                                                                      window.__slooshTick = tick;
                                                                                                      fixVisibility(document);
                                                                                                      install(window);
                                                                                                      scan(window);
                                                                                                      triggerPlay(window);
                                                                                                      tick();
                                                                                                      setInterval(tick, 150);
                                                                                                      window.addEventListener('load', tick);
                                                                                                      window.addEventListener('DOMContentLoaded', tick);
                                                                                                    })();
                                                                                                    
                                                                                                                </script>
                                                                                                            </head>
                                                                                                            <body>
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=0db06d820a3aa69c0d2dd0b02484ff&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=66" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
                                                                                                                <script>
                                                                                                                (function() {
                                                                                                                    var f = document.getElementById('alloha_iframe');
                                                                                                                    if (f) {
                                                                                                                        f.onload = function() {
                                                                                                                            try {
                                                                                                                                if (typeof window.__slooshTick === 'function') window.__slooshTick();
                                                                                                                            } catch(e) {}
                                                                                                                        };
                                                                                                                    }
                                                                                                                })();
                                                                                                                </script>
                                                                                                            
                                                                                                            </body></html>
2026-09-06 16:09:52.182  8624-8759  cr_CookieManager        com.sloosh.tv                        E  	at com.android.webview.chromium.a.a(chromium-TrichromeWebViewGoogle.aab-stable-749902436:184) (Fix with AI)
at com.android.webview.chromium.a.getCookie(chromium-TrichromeWebViewGoogle.aab-stable-749902436:4)
at com.sloosh.tv.data.alloha.HlsProxyServer.buildRequest(HlsProxyServer.kt:562)
at com.sloosh.tv.data.alloha.HlsProxyServer.fetchText(HlsProxyServer.kt:510)
at com.sloosh.tv.data.alloha.HlsProxyServer.servePlaylist(HlsProxyServer.kt:311)
at com.sloosh.tv.data.alloha.HlsProxyServer.access$servePlaylist(HlsProxyServer.kt:28)
at com.sloosh.tv.data.alloha.HlsProxyServer$handleConnection$2.invokeSuspend(HlsProxyServer.kt:266)
at com.sloosh.tv.data.alloha.HlsProxyServer$handleConnection$2.invoke(Unknown Source:8)
at com.sloosh.tv.data.alloha.HlsProxyServer$handleConnection$2.invoke(Unknown Source:4)
at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatchedOrReturn(Undispatched.kt:78)
at kotlinx.coroutines.BuildersKt__Builders_commonKt.withContext(Builders.common.kt:167)
at kotlinx.coroutines.BuildersKt.withContext(Unknown Source:1)
at com.sloosh.tv.data.alloha.HlsProxyServer.handleConnection(HlsProxyServer.kt:197)
at com.sloosh.tv.data.alloha.HlsProxyServer.access$handleConnection(HlsProxyServer.kt:28)
at com.sloosh.tv.data.alloha.HlsProxyServer$start$1$1$1.invokeSuspend(HlsProxyServer.kt:153)
at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
at kotlinx.coroutines.internal.LimitedDispatcher$Worker.run(LimitedDispatcher.kt:115)
at kotlinx.coroutines.scheduling.TaskImpl.run(Tasks.kt:103)
at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:584)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:793)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:697)
at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:684)
2026-09-06 16:09:53.195  8624-8752  ExoPlayerImplInternal   com.sloosh.tv                        E  Playback error (Fix with AI)
androidx.media3.exoplayer.ExoPlaybackException: Source error
at androidx.media3.exoplayer.ExoPlayerImplInternal.handleIoException(ExoPlayerImplInternal.java:717)
at androidx.media3.exoplayer.ExoPlayerImplInternal.handleMessage(ExoPlayerImplInternal.java:687)
at android.os.Handler.dispatchMessage(Handler.java:106)
at android.os.Looper.loopOnce(Looper.java:248)
at android.os.Looper.loop(Looper.java:338)
at android.os.HandlerThread.run(HandlerThread.java:85)
Caused by: androidx.media3.common.ParserException: Input does not start with the #EXTM3U header.{contentIsMalformed=true, dataType=4}
at androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser.parse(HlsPlaylistParser.java:262)
at androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser.parse(HlsPlaylistParser.java:69)
at androidx.media3.exoplayer.upstream.ParsingLoadable.load(ParsingLoadable.java:184)
at androidx.media3.exoplayer.upstream.Loader$LoadTask.run(Loader.java:421)
at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1156)
at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:651)
at java.lang.Thread.run(Thread.java:1119)
2026-09-06 16:09:53.197  8624-8624  PlayerScreen            com.sloosh.tv                        W  ExoPlayer error: ERROR_CODE_PARSING_MANIFEST_MALFORMED - Source error

---

### Root Cause & Resolution (2026-09-13):
1. **GitHub Actions Version Downgrade Issue**:
   - `actions/checkout@v4` in `build-apk.yml` was performing a shallow clone (`fetch-depth: 1`).
   - In shallow clone mode, `git rev-list --count HEAD` in `app/build.gradle.kts` evaluated to `1`.
   - The compiled APK received `versionCode: 1` and `versionName: 2.0.1` instead of `versionCode: 118` and `versionName: 2.0.118`.
   - When users tried to install the update or use in-app update from version `2.0.109`, Android Package Manager rejected the downgrade (`INSTALL_FAILED_VERSION_DOWNGRADE`), and GitHub Releases considered `v2.0.109` newer than `v2.0.1`.
   - Consequently, the app that ran at 16:09 was still the old build 109 (`15148e0`).
   - **Fix**: Added `with: fetch-depth: 0` to `actions/checkout@v4`. Releases now properly receive incrementing versions `2.0.118+`.

2. **`ERROR_CODE_PARSING_MANIFEST_MALFORMED` in `HlsProxyServer.kt`**:
   - In `servePlaylist`, if `fetchText` returned HTML or non-playlist text, the server was returning it with `HTTP 200 OK` and `Content-Type: application/vnd.apple.mpegurl`. ExoPlayer attempted to parse HTML as an M3U8 playlist and threw `ParserException: Input does not start with the #EXTM3U header`.
   - **Fix**: In `servePlaylist`, if `!body.contains("#EXT")`, the proxy now immediately responds with `HTTP 503 Service Unavailable` + `Retry-After: 1`, identical to iOS `HlsProxyServer.swift`. ExoPlayer waits 1s and retries rather than crashing.

3. **`cr_CookieManager: Unable to get cookies due to error parsing URL`**:
   - `buildRequest` was passing raw URLs with query strings to `CookieManager.getCookie`.
   - **Fix**: Sanitized input to pass root origin URL (`"${uri.scheme}://${uri.host}/"`).

4. **Stream Validation in `PlayerViewModel.kt`**:
   - Enforced `isPlayableMediaUrl` guard on `activeStreamUrl` before proxy update.
