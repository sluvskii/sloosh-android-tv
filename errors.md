2026-09-06 21:53:50.561 11756-11942 cr_CookieManager        com.sloosh.tv                        E  Unable to get cookies due to error parsing URL: https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&token=ffbd312217e27c4245f2678afe1881&translation=234&season=1&episode=1
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
2026-09-06 21:53:50.561 11756-11942 cr_CookieManager        com.sloosh.tv                        E        if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
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
2026-09-06 21:53:50.561 11756-11942 cr_CookieManager        com.sloosh.tv                        E          var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
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
2026-09-06 21:53:50.562 11756-11942 cr_CookieManager        com.sloosh.tv                        E                },
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
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=234&amp;season=1&amp;episode=1" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
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
                                                                                                    java.net.URISyntaxException: Bad address: https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&token=ffbd312217e27c4245f2678afe1881&translation=234&season=1&episode=1
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
2026-09-06 21:53:50.562 11756-11942 cr_CookieManager        com.sloosh.tv                        E        if (window.top && window.top.AndroidBridge && typeof window.top.AndroidBridge.post === 'function') return window.top.AndroidBridge;
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
2026-09-06 21:53:50.563 11756-11942 cr_CookieManager        com.sloosh.tv                        E        }

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
2026-09-06 21:53:50.564 11756-11942 cr_CookieManager        com.sloosh.tv                        E                if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(v, k) { putHeader(k, v); });
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
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=234&amp;season=1&amp;episode=1" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
                                                                                                                <script>
                                                                                                                (function() {
                                                                                                                    var f = document.getElementById('alloha_iframe');
                                                                                                                    if (f) {
                                                                                                                        f.onload = function() {
                                                                                                                            try {
2026-09-06 21:53:50.564 11756-11942 cr_CookieManager        com.sloosh.tv                        E                              if (typeof window.__slooshTick === 'function') window.__slooshTick(); (Fix with AI)
} catch(e) {}
};
}
})();
</script>

                                                                                                            </body></html>
                                                                                                    	at com.android.webview.chromium.a.a(chromium-TrichromeWebViewGoogle.aab-stable-749902436:184)
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
2026-09-06 21:53:50.759 11756-11942 HlsProxy                com.sloosh.tv                        W  fetchText HTTP 404 for https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&
2026-09-06 21:53:50.959 11756-11942 cr_CookieManager        com.sloosh.tv                        E  Unable to get cookies due to error parsing URL: https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&token=ffbd312217e27c4245f2678afe1881&translation=234&season=1&episode=1
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
2026-09-06 21:53:50.965 11756-11942 cr_CookieManager        com.sloosh.tv                        E        if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
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
2026-09-06 21:53:50.968 11756-11942 cr_CookieManager        com.sloosh.tv                        E          var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
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
2026-09-06 21:53:50.968 11756-11942 cr_CookieManager        com.sloosh.tv                        E                },
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
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=234&amp;season=1&amp;episode=1" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
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
                                                                                                    java.net.URISyntaxException: Bad address: https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&token=ffbd312217e27c4245f2678afe1881&translation=234&season=1&episode=1
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
2026-09-06 21:53:50.969 11756-11942 cr_CookieManager        com.sloosh.tv                        E        if (window.top && window.top.AndroidBridge && typeof window.top.AndroidBridge.post === 'function') return window.top.AndroidBridge;
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
2026-09-06 21:53:50.969 11756-11942 cr_CookieManager        com.sloosh.tv                        E        }

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
2026-09-06 21:53:50.969 11756-11942 cr_CookieManager        com.sloosh.tv                        E                if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(v, k) { putHeader(k, v); });
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
                                                                                                                <iframe id="alloha_iframe" src="https://Antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&amp;token=ffbd312217e27c4245f2678afe1881&amp;translation=234&amp;season=1&amp;episode=1" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen="" frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
                                                                                                                <script>
                                                                                                                (function() {
                                                                                                                    var f = document.getElementById('alloha_iframe');
                                                                                                                    if (f) {
                                                                                                                        f.onload = function() {
                                                                                                                            try {
2026-09-06 21:53:50.971 11756-11942 cr_CookieManager        com.sloosh.tv                        E                              if (typeof window.__slooshTick === 'function') window.__slooshTick(); (Fix with AI)
} catch(e) {}
};
}
})();
</script>

                                                                                                            </body></html>
                                                                                                    	at com.android.webview.chromium.a.a(chromium-TrichromeWebViewGoogle.aab-stable-749902436:184)
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
2026-09-06 21:53:51.158 11756-11942 HlsProxy                com.sloosh.tv                        W  fetchText HTTP 404 for https://antipathic-as.stravers.live/?token_movie=21df5d3777ae838390d056f5f8e69b&
2026-09-06 21:53:51.164 11756-11942 HlsProxy                com.sloosh.tv                        I  servePlaylist: fetch failed, notifying onSessionExpired and waiting for refresh...
2026-09-06 21:53:52.671 11756-11756 PlayerScreen            com.sloosh.tv                        W  Buffering watchdog: stalled for 12s, kicking ExoPlayer at 0 ms
2026-09-06 21:53:52.760 11756-12129 ExoPlayerImplInternal   com.sloosh.tv                        E  Playback error (Fix with AI)
androidx.media3.exoplayer.ExoPlaybackException: Source error
at androidx.media3.exoplayer.ExoPlayerImplInternal.handleIoException(ExoPlayerImplInternal.java:717)
at androidx.media3.exoplayer.ExoPlayerImplInternal.handleMessage(ExoPlayerImplInternal.java:689)
at android.os.Handler.dispatchMessage(Handler.java:106)
at android.os.Looper.loopOnce(Looper.java:248)
at android.os.Looper.loop(Looper.java:338)
at android.os.HandlerThread.run(HandlerThread.java:85)
Caused by: androidx.media3.datasource.HttpDataSource$InvalidResponseCodeException: Response code: 404
at androidx.media3.datasource.DefaultHttpDataSource.open(DefaultHttpDataSource.java:436)
at androidx.media3.datasource.DefaultDataSource.open(DefaultDataSource.java:275)
at androidx.media3.datasource.StatsDataSource.open(StatsDataSource.java:86)
at androidx.media3.datasource.DataSourceInputStream.checkOpened(DataSourceInputStream.java:101)
at androidx.media3.datasource.DataSourceInputStream.open(DataSourceInputStream.java:64)
at androidx.media3.exoplayer.upstream.ParsingLoadable.load(ParsingLoadable.java:182)
at androidx.media3.exoplayer.upstream.Loader$LoadTask.run(Loader.java:421)
at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1156)
at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:651)
at java.lang.Thread.run(Thread.java:1119)
2026-09-06 21:53:52.777 11756-11756 PlayerScreen            com.sloosh.tv                        W  ExoPlayer error: ERROR_CODE_IO_BAD_HTTP_STATUS - Source error

---

### Resolution (2026-09-13) - Alloha Playback & Cross-Origin Iframe Interception Resolution:
1. **Root Cause Analysis**:
   - **`ERR_CONTENT_DECODING_FAILED`**: When `shouldInterceptRequest` intercepted the Alloha iframe HTML or `/bnsi/` requests, OkHttp automatically decompressed the response payload. However, the original response headers (including `Content-Encoding: gzip`, `Transfer-Encoding: chunked`, and compressed `Content-Length`) were retained in `WebResourceResponse`. WebView/Chromium failed to decode uncompressed UTF-8 bytes labeled as `gzip`, causing the iframe to abort and remain blank.
   - **Anti-Framing check (`isFramed`)**: Line 30 of Alloha's HTML checked `isFramed = window != window.top || ...; if (!isFramed) document.querySelectorAll('body')[0].remove()`. When loaded in WebView, if this condition failed, the player destroyed its own DOM.
   - **Early WebSocket hook**: Line 11 of Alloha HTML inline captured `window.WebSocket` into `window.__ws_factory` before external scripts could run.
   - **CORS/CDN Headers**: Edge CDNs required `Accepts-Controls` header together with the iframe host's `Referer` and `Origin`.

2. **Fix Implemented**:
   - **`AllohaRuntimeResolver.kt`**:
     - Implemented `sanitizeInterceptResponseHeaders` stripping `content-encoding`, `content-length`, `transfer-encoding`, `content-security-policy`, `content-security-policy-report-only`, and `x-frame-options`.
     - Injected early hook `$IFRAME_INJECTED_HOOK_JS` right after `<head>` before `window.__ws_factory` can run.
     - Patched `var isFramed=false;` -> `var isFramed=true;` in HTML directly, preventing Alloha from deleting its DOM.
     - Extended WebSocket hook to handle constructor, `addEventListener('message')`, and `.onmessage` setter/getter.
     - Forwarded bridge signals via Java interface, AndroidX WebMessageListener, image beacons, and fetch payload headers.
     - Bound default `referer` and `origin` to the iframe host.
   - **`HlsProxyServer.kt`**:
     - Ensured `Accepts-Controls` header casing in upstream CDN requests.
     - Handled upstream reconnects with `503 Service Unavailable` and `Retry-After: 1` instead of fatal 404.
   - **`PlayerViewModel.kt`**:
     - Enforced `isPlayableMediaUrl` guards across all audio selection, refresh, and playback pipelines.
   - **`AllohaRuntimeParser.kt`**:
     - Strictly validated stream URLs, filtering non-playable links.
