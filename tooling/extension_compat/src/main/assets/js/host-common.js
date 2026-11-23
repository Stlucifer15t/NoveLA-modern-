/*
 * Dreamora JS host, part 1: browser-like globals on top of bare QuickJS.
 *
 * Loaded once per runtime after js-libs.js and before the ecosystem host
 * (lnreader-host.js or mangayomi-host.js). QuickJS has no fetch, URL, TextEncoder,
 * Buffer, timers or module loader, and the plugins expect them, so they are provided
 * here on top of the natives that JsPluginRuntime.kt installs:
 *
 *   __native_log(level, message)            -> null
 *   __native_fetch(requestJson)             -> Promise<responseJson>
 *   __native_sleep(ms)                      -> Promise<null>
 *   __native_decode(base64, charset)        -> string
 *   __native_storage_get(key)               -> string | null
 *   __native_storage_set(key, value | null) -> null
 *   __native_storage_clear()                -> null
 */
(function (g) {
  'use strict';

  g.global = g;

  // ── console ────────────────────────────────────────────────────────────────
  var nativeLog = g.__native_log;
  function render(args) {
    var parts = [];
    for (var i = 0; i < args.length; i++) {
      var a = args[i];
      if (typeof a === 'string') {
        parts.push(a);
      } else {
        try {
          parts.push(JSON.stringify(a));
        } catch (e) {
          parts.push(String(a));
        }
      }
    }
    return parts.join(' ');
  }
  g.console = {
    log: function () { nativeLog('info', render(arguments)); },
    info: function () { nativeLog('info', render(arguments)); },
    debug: function () { nativeLog('debug', render(arguments)); },
    warn: function () { nativeLog('warn', render(arguments)); },
    error: function () { nativeLog('error', render(arguments)); },
  };

  // ── base64 and UTF-8 ───────────────────────────────────────────────────────
  var B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

  function bytesToBase64(bytes) {
    var out = '';
    var i = 0;
    for (; i + 2 < bytes.length; i += 3) {
      var n = (bytes[i] << 16) | (bytes[i + 1] << 8) | bytes[i + 2];
      out += B64[(n >> 18) & 63] + B64[(n >> 12) & 63] + B64[(n >> 6) & 63] + B64[n & 63];
    }
    var rest = bytes.length - i;
    if (rest === 1) {
      var n1 = bytes[i] << 16;
      out += B64[(n1 >> 18) & 63] + B64[(n1 >> 12) & 63] + '==';
    } else if (rest === 2) {
      var n2 = (bytes[i] << 16) | (bytes[i + 1] << 8);
      out += B64[(n2 >> 18) & 63] + B64[(n2 >> 12) & 63] + B64[(n2 >> 6) & 63] + '=';
    }
    return out;
  }

  function base64ToBytes(text) {
    var clean = String(text).replace(/[^A-Za-z0-9+/]/g, '');
    var out = [];
    var buffer = 0;
    var bits = 0;
    for (var i = 0; i < clean.length; i++) {
      buffer = ((buffer << 6) | B64.indexOf(clean[i])) & 0xffffff;
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        out.push((buffer >> bits) & 255);
      }
    }
    return new Uint8Array(out);
  }

  function utf8Encode(text) {
    var out = [];
    for (var i = 0; i < text.length; i++) {
      var c = text.charCodeAt(i);
      if (c >= 0xd800 && c <= 0xdbff && i + 1 < text.length) {
        var d = text.charCodeAt(i + 1);
        if (d >= 0xdc00 && d <= 0xdfff) {
          c = 0x10000 + ((c - 0xd800) << 10) + (d - 0xdc00);
          i++;
        }
      }
      if (c < 0x80) {
        out.push(c);
      } else if (c < 0x800) {
        out.push(0xc0 | (c >> 6), 0x80 | (c & 63));
      } else if (c < 0x10000) {
        out.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
      } else {
        out.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 63), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
      }
    }
    return new Uint8Array(out);
  }

  function utf8Decode(bytes) {
    var out = '';
    var i = 0;
    while (i < bytes.length) {
      var b = bytes[i];
      var cp = 0xfffd;
      var size = 1;
      if (b < 0x80) {
        cp = b;
      } else if (b >= 0xc2 && b < 0xe0 && i + 1 < bytes.length) {
        cp = ((b & 31) << 6) | (bytes[i + 1] & 63);
        size = 2;
      } else if (b >= 0xe0 && b < 0xf0 && i + 2 < bytes.length) {
        cp = ((b & 15) << 12) | ((bytes[i + 1] & 63) << 6) | (bytes[i + 2] & 63);
        size = 3;
      } else if (b >= 0xf0 && b < 0xf5 && i + 3 < bytes.length) {
        cp = ((b & 7) << 18) | ((bytes[i + 1] & 63) << 12) | ((bytes[i + 2] & 63) << 6) | (bytes[i + 3] & 63);
        size = 4;
      }
      if (cp > 0xffff) {
        cp -= 0x10000;
        out += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 1023));
      } else {
        out += String.fromCharCode(cp);
      }
      i += size;
    }
    return out;
  }

  function bytesOf(data) {
    if (data instanceof Uint8Array) return data;
    if (data instanceof ArrayBuffer) return new Uint8Array(data);
    if (ArrayBuffer.isView(data)) return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    return utf8Encode(String(data));
  }

  g.btoa = function (text) {
    var bytes = new Uint8Array(String(text).length);
    for (var i = 0; i < bytes.length; i++) bytes[i] = String(text).charCodeAt(i) & 255;
    return bytesToBase64(bytes);
  };
  g.atob = function (text) {
    var bytes = base64ToBytes(text);
    var out = '';
    for (var i = 0; i < bytes.length; i++) out += String.fromCharCode(bytes[i]);
    return out;
  };

  function TextEncoder() {}
  TextEncoder.prototype.encode = function (text) { return utf8Encode(text === undefined ? '' : String(text)); };
  g.TextEncoder = TextEncoder;

  function TextDecoder(label) {
    var name = (label || 'utf-8').toLowerCase();
    this.encoding = name;
  }
  TextDecoder.prototype.decode = function (data) {
    if (data === undefined || data === null) return '';
    var bytes = bytesOf(data);
    if (this.encoding === 'utf-8' || this.encoding === 'utf8') return utf8Decode(bytes);
    return g.__native_decode(bytesToBase64(bytes), this.encoding);
  };
  g.TextDecoder = TextDecoder;

  function BufferView(bytes) {
    this.bytes = bytes;
    this.length = bytes.length;
  }
  BufferView.prototype.toString = function (encoding) {
    var enc = (encoding || 'utf8').toLowerCase();
    if (enc === 'base64') return bytesToBase64(this.bytes);
    if (enc === 'hex') {
      var hex = '';
      for (var i = 0; i < this.bytes.length; i++) hex += (this.bytes[i] < 16 ? '0' : '') + this.bytes[i].toString(16);
      return hex;
    }
    return utf8Decode(this.bytes);
  };
  g.Buffer = {
    from: function (data, encoding) {
      if (typeof data === 'string') {
        var enc = (encoding || 'utf8').toLowerCase();
        return new BufferView(enc === 'base64' ? base64ToBytes(data) : utf8Encode(data));
      }
      return new BufferView(bytesOf(data));
    },
    isBuffer: function (value) { return value instanceof BufferView; },
  };

  // ── URLSearchParams and URL ───────────────────────────────────────────────
  function formEncode(text) {
    return encodeURIComponent(text).replace(/%20/g, '+');
  }

  function URLSearchParams(init) {
    this._pairs = [];
    if (init === undefined || init === null) return;
    if (typeof init === 'string') {
      var s = init.charAt(0) === '?' ? init.slice(1) : init;
      if (s.length === 0) return;
      var parts = s.split('&');
      for (var i = 0; i < parts.length; i++) {
        if (parts[i].length === 0) continue;
        var eq = parts[i].indexOf('=');
        var rawKey = eq === -1 ? parts[i] : parts[i].slice(0, eq);
        var rawVal = eq === -1 ? '' : parts[i].slice(eq + 1);
        this._pairs.push([decodeURIComponent(rawKey.replace(/\+/g, ' ')), decodeURIComponent(rawVal.replace(/\+/g, ' '))]);
      }
    } else if (typeof init[Symbol.iterator] === 'function' && !(init instanceof URLSearchParams)) {
      for (var item of init) this._pairs.push([String(item[0]), String(item[1])]);
    } else if (init instanceof URLSearchParams) {
      this._pairs = init._pairs.slice();
    } else {
      var keys = Object.keys(init);
      for (var k = 0; k < keys.length; k++) {
        var value = init[keys[k]];
        if (value === undefined) continue;
        this._pairs.push([keys[k], String(value)]);
      }
    }
  }
  URLSearchParams.prototype.append = function (key, value) { this._pairs.push([String(key), String(value)]); };
  URLSearchParams.prototype.set = function (key, value) {
    var found = false;
    var out = [];
    for (var i = 0; i < this._pairs.length; i++) {
      if (this._pairs[i][0] === String(key)) {
        if (!found) out.push([String(key), String(value)]);
        found = true;
      } else {
        out.push(this._pairs[i]);
      }
    }
    if (!found) out.push([String(key), String(value)]);
    this._pairs = out;
  };
  URLSearchParams.prototype.get = function (key) {
    for (var i = 0; i < this._pairs.length; i++) if (this._pairs[i][0] === String(key)) return this._pairs[i][1];
    return null;
  };
  URLSearchParams.prototype.getAll = function (key) {
    var out = [];
    for (var i = 0; i < this._pairs.length; i++) if (this._pairs[i][0] === String(key)) out.push(this._pairs[i][1]);
    return out;
  };
  URLSearchParams.prototype.has = function (key) { return this.get(key) !== null; };
  URLSearchParams.prototype.delete = function (key) {
    this._pairs = this._pairs.filter(function (p) { return p[0] !== String(key); });
  };
  URLSearchParams.prototype.forEach = function (callback, thisArg) {
    for (var i = 0; i < this._pairs.length; i++) callback.call(thisArg, this._pairs[i][1], this._pairs[i][0], this);
  };
  URLSearchParams.prototype.keys = function () { return this._pairs.map(function (p) { return p[0]; })[Symbol.iterator](); };
  URLSearchParams.prototype.values = function () { return this._pairs.map(function (p) { return p[1]; })[Symbol.iterator](); };
  URLSearchParams.prototype.entries = function () { return this._pairs.map(function (p) { return [p[0], p[1]]; })[Symbol.iterator](); };
  URLSearchParams.prototype[Symbol.iterator] = URLSearchParams.prototype.entries;
  URLSearchParams.prototype.toString = function () {
    var out = [];
    for (var i = 0; i < this._pairs.length; i++) out.push(formEncode(this._pairs[i][0]) + '=' + formEncode(this._pairs[i][1]));
    return out.join('&');
  };
  g.URLSearchParams = URLSearchParams;

  function normalizePath(path) {
    var segments = path.split('/');
    var out = [];
    for (var i = 0; i < segments.length; i++) {
      var s = segments[i];
      if (s === '.') continue;
      if (s === '..') {
        if (out.length > 1) out.pop();
        continue;
      }
      out.push(s);
    }
    var joined = out.join('/');
    return joined.charAt(0) === '/' ? joined : '/' + joined;
  }

  var URL_PATTERN = /^([a-zA-Z][a-zA-Z0-9+.-]*:)?(\/\/([^/?#]*))?([^?#]*)(\?[^#]*)?(#.*)?$/;

  function URL(input, base) {
    var text = String(input).trim();
    var m = URL_PATTERN.exec(text);
    if (!m) throw new TypeError('Invalid URL: ' + text);
    var protocol = m[1];
    var authority = m[3];
    var path = m[4] || '';
    if (protocol === undefined && base !== undefined) {
      var b = base instanceof URL ? base : new URL(base);
      protocol = b.protocol;
      if (authority === undefined) {
        authority = b._authority;
        if (path === '') {
          path = b.pathname;
          if (m[5] === undefined) m[5] = b.search;
        } else if (path.charAt(0) !== '/') {
          var dir = b.pathname.slice(0, b.pathname.lastIndexOf('/') + 1);
          path = dir + path;
        }
        path = normalizePath(path);
      }
    }
    if (protocol === undefined) throw new TypeError('Invalid URL: ' + text);
    this.protocol = protocol.toLowerCase();
    this._authority = authority || '';
    var hostPort = this._authority.split(':');
    this.hostname = hostPort[0].toLowerCase();
    this.port = hostPort.length > 1 ? hostPort[1] : '';
    this.host = this.port ? this.hostname + ':' + this.port : this.hostname;
    this.pathname = path === '' ? '/' : normalizePath(path);
    this.search = m[5] && m[5].length > 1 ? m[5] : '';
    this.hash = m[6] && m[6].length > 1 ? m[6] : '';
    this.origin = this.protocol + '//' + this.host;
    this.searchParams = new URLSearchParams(this.search);
  }
  URL.prototype.toString = function () {
    var query = this.searchParams.toString();
    var search = query.length > 0 ? '?' + query : '';
    return this.protocol + '//' + this.host + this.pathname + search + this.hash;
  };
  URL.prototype.toJSON = function () { return this.toString(); };
  g.URL = URL;

  // ── Headers, Response and fetch ───────────────────────────────────────────
  function Headers(init) {
    this._map = {};
    if (init === undefined || init === null) return;
    if (init instanceof Headers) {
      var self = this;
      init.forEach(function (value, key) { self.append(key, value); });
    } else if (Array.isArray(init)) {
      for (var i = 0; i < init.length; i++) this.append(init[i][0], init[i][1]);
    } else {
      var keys = Object.keys(init);
      for (var k = 0; k < keys.length; k++) {
        if (init[keys[k]] !== undefined && init[keys[k]] !== null) this.append(keys[k], init[keys[k]]);
      }
    }
  }
  Headers.prototype.append = function (key, value) {
    var name = String(key).toLowerCase();
    var text = String(value);
    this._map[name] = this._map[name] === undefined ? text : this._map[name] + ', ' + text;
  };
  Headers.prototype.set = function (key, value) { this._map[String(key).toLowerCase()] = String(value); };
  Headers.prototype.get = function (key) {
    var v = this._map[String(key).toLowerCase()];
    return v === undefined ? null : v;
  };
  Headers.prototype.has = function (key) { return this._map[String(key).toLowerCase()] !== undefined; };
  Headers.prototype.delete = function (key) { delete this._map[String(key).toLowerCase()]; };
  Headers.prototype.forEach = function (callback, thisArg) {
    var keys = Object.keys(this._map);
    for (var i = 0; i < keys.length; i++) callback.call(thisArg, this._map[keys[i]], keys[i], this);
  };
  Headers.prototype.toObject = function () {
    var out = {};
    this.forEach(function (value, key) { out[key] = value; });
    return out;
  };
  g.Headers = Headers;

  function Response(raw) {
    this.status = raw.status;
    this.ok = raw.status >= 200 && raw.status < 300;
    this.url = raw.url;
    this.headers = new Headers(raw.headers || {});
    this._text = raw.body || '';
    this._base64 = raw.base64 || '';
    this.bodyUsed = false;
  }
  Response.prototype.text = function () { this.bodyUsed = true; return Promise.resolve(this._text); };
  Response.prototype.json = function () { this.bodyUsed = true; return Promise.resolve(JSON.parse(this._text)); };
  Response.prototype.arrayBuffer = function () {
    this.bodyUsed = true;
    var bytes = base64ToBytes(this._base64);
    return Promise.resolve(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength));
  };
  // Host-side helper: the response body as base64, for fetchFile and non-UTF-8 decoding.
  Response.prototype._bodyBase64 = function () { return this._base64; };
  g.Response = Response;

  function headersToObject(headers) {
    var out = {};
    if (headers === undefined || headers === null) return out;
    if (headers instanceof Headers) return headers.toObject();
    if (Array.isArray(headers)) {
      for (var i = 0; i < headers.length; i++) out[String(headers[i][0])] = String(headers[i][1]);
      return out;
    }
    var keys = Object.keys(headers);
    for (var k = 0; k < keys.length; k++) {
      var v = headers[keys[k]];
      if (v !== undefined && v !== null) out[keys[k]] = String(v);
    }
    return out;
  }

  // FormData: text fields only (plugins build login and search forms with it). It is sent
  // as multipart/form-data, the encoding the native fetch uses for a FormData body.
  function FormData() { this._entries = []; }
  FormData.prototype.append = function (name, value) { this._entries.push([String(name), String(value)]); };
  FormData.prototype.set = function (name, value) {
    var key = String(name);
    var out = [];
    var placed = false;
    for (var i = 0; i < this._entries.length; i++) {
      if (this._entries[i][0] === key) {
        if (!placed) out.push([key, String(value)]);
        placed = true;
      } else {
        out.push(this._entries[i]);
      }
    }
    if (!placed) out.push([key, String(value)]);
    this._entries = out;
  };
  FormData.prototype.get = function (name) {
    for (var i = 0; i < this._entries.length; i++) if (this._entries[i][0] === String(name)) return this._entries[i][1];
    return null;
  };
  FormData.prototype.getAll = function (name) {
    var out = [];
    for (var i = 0; i < this._entries.length; i++) if (this._entries[i][0] === String(name)) out.push(this._entries[i][1]);
    return out;
  };
  FormData.prototype.has = function (name) { return this.get(name) !== null; };
  FormData.prototype.delete = function (name) {
    this._entries = this._entries.filter(function (p) { return p[0] !== String(name); });
  };
  FormData.prototype.forEach = function (callback, thisArg) {
    for (var i = 0; i < this._entries.length; i++) callback.call(thisArg, this._entries[i][1], this._entries[i][0], this);
  };
  FormData.prototype.entries = function () { return this._entries.map(function (p) { return [p[0], p[1]]; })[Symbol.iterator](); };
  FormData.prototype.keys = function () { return this._entries.map(function (p) { return p[0]; })[Symbol.iterator](); };
  FormData.prototype.values = function () { return this._entries.map(function (p) { return p[1]; })[Symbol.iterator](); };
  FormData.prototype[Symbol.iterator] = FormData.prototype.entries;
  g.FormData = FormData;

  function multipartOf(form) {
    var boundary = '----DreamoraFormBoundary' + Math.random().toString(36).slice(2) + Date.now().toString(36);
    var body = '';
    for (var i = 0; i < form._entries.length; i++) {
      var name = form._entries[i][0].replace(/"/g, '%22').replace(/\r?\n/g, ' ');
      body += '--' + boundary + '\r\nContent-Disposition: form-data; name="' + name + '"\r\n\r\n' +
        form._entries[i][1] + '\r\n';
    }
    body += '--' + boundary + '--\r\n';
    return { body: body, contentType: 'multipart/form-data; boundary=' + boundary };
  }

  g.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : String(input && input.url !== undefined ? input.url : input);
    var options = init || {};
    var request = {
      url: url,
      method: String(options.method || 'GET').toUpperCase(),
      headers: headersToObject(options.headers),
      body: null,
    };
    var body = options.body;
    if (body !== undefined && body !== null) {
      if (typeof body === 'string') {
        request.body = body;
      } else if (body instanceof URLSearchParams) {
        request.body = body.toString();
        if (request.headers['Content-Type'] === undefined) request.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
      } else if (body instanceof FormData) {
        var multipart = multipartOf(body);
        request.body = multipart.body;
        if (request.headers['Content-Type'] === undefined) request.headers['Content-Type'] = multipart.contentType;
      } else {
        request.body = String(body);
      }
    }
    return g.__native_fetch(JSON.stringify(request)).then(function (raw) {
      return new Response(JSON.parse(raw));
    });
  };

  // ── timers and microtasks ─────────────────────────────────────────────────
  g.setTimeout = function (callback, delay) {
    var args = Array.prototype.slice.call(arguments, 2);
    g.__native_sleep(Number(delay) || 0).then(function () { callback.apply(null, args); });
    return 0;
  };
  g.clearTimeout = function () {};
  g.queueMicrotask = function (callback) { Promise.resolve().then(callback); };

  // ── CommonJS modules for plugins ──────────────────────────────────────────
  var modules = Object.create(null);
  // QuickJS has no Intl. Plugins read the device time zone (and nothing else) from it,
  // so a UTC-only stand-in keeps them loading. Real Intl is used when present.
  if (typeof g.Intl === 'undefined') {
    g.Intl = {
      DateTimeFormat: function () {
        return {
          resolvedOptions: function () { return { timeZone: 'UTC', locale: 'en-US' }; },
          format: function (date) { return String(date); },
        };
      },
    };
  }

  // Result slots for async host calls. quickjs-kt returns the value of the script that
  // ran, not the settled value of a Promise, so Kotlin starts a call with __hostStart
  // and then polls __hostTake until the slot is filled.
  g.__hostStart = function (slot, promise) {
    var state = { done: false, ok: false, value: null, error: null };
    g.__hostSlots = g.__hostSlots || {};
    g.__hostSlots[slot] = state;
    Promise.resolve(promise).then(
      function (value) { state.value = value === undefined ? null : value; state.ok = true; state.done = true; },
      function (err) {
        state.error = String((err && err.message) || err);
        state.done = true;
      }
    );
    return null;
  };
  g.__hostTake = function (slot) {
    var slots = g.__hostSlots || {};
    var state = slots[slot];
    if (!state || !state.done) return null;
    delete slots[slot];
    return JSON.stringify({ ok: state.ok, value: state.value, error: state.error });
  };
  g.__hostRegister = function (name, exports) { modules[name] = exports; };
  g.__hostRequire = function (name) {
    if (Object.prototype.hasOwnProperty.call(modules, name)) return modules[name];
    throw new Error('Module "' + name + '" is not available in the Dreamora JS runtime');
  };

  var libs = g.__libs;
  g.__hostRegister('cheerio', libs.cheerio);
  g.__hostRegister('htmlparser2', libs.htmlparser2);
  g.__hostRegister('domhandler', libs.domhandler);
  g.__hostRegister('dayjs', libs.dayjs);
  // LNReader's @libs/aes: the plugins use gcm(key, iv).decrypt(bytes).
  g.__hostRegister('@libs/aes', libs.aes);
  g.__decodeBase64 = function (base64, charset) { return g.__native_decode(base64, charset); };
})(globalThis);
