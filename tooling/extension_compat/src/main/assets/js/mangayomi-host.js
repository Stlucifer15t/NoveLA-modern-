/*
 * Dreamora JS host, part 2: the Mangayomi extension contract.
 *
 * Mangayomi JavaScript sources declare `const mangayomiSources = [...]` and
 * `class DefaultExtension extends MProvider`, and use the globals `Client`
 * (http), `Document` (HTML selectors) and `source` (their own metadata). Filters
 * are objects with a `type_name` (SelectFilter, CheckBox, GroupFilter, TextFilter,
 * SortFilter) that the app passes back into `search` with the user's state.
 *
 *   __mgLoad(code)                 -> JSON {name, baseUrl, lang, itemType, isManga, ...}
 *   __mgDispatch(method, argsJson) -> Promise<JSON text of the result>
 *   __mgFilters()                  -> Promise<JSON text of the filter list>
 */
(function (g) {
  'use strict';

  var libs = g.__libs;

  function cleanHeaders(headers) {
    var out = {};
    if (!headers) return out;
    var keys = Object.keys(headers);
    for (var i = 0; i < keys.length; i++) {
      var v = headers[keys[i]];
      if (v !== undefined && v !== null) out[keys[i]] = String(v);
    }
    return out;
  }

  // ── http ─────────────────────────────────────────────────────────────────
  async function doRequest(method, url, headers, body) {
    var init = { method: method, headers: cleanHeaders(headers) };
    if (body !== undefined && body !== null) {
      if (typeof body === 'string') {
        init.body = body;
      } else {
        init.body = new URLSearchParams(body).toString();
        if (init.headers['Content-Type'] === undefined) {
          init.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        }
      }
    }
    var res = await g.fetch(url, init);
    var text = await res.text();
    var responseHeaders = {};
    res.headers.forEach(function (value, key) { responseHeaders[key] = value; });
    return { body: text, statusCode: res.status, headers: responseHeaders, url: res.url };
  }

  class Client {
    async get(url, headers) {
      return doRequest('GET', url, headers, undefined);
    }
    async post(url, headers, body) {
      return doRequest('POST', url, headers, body);
    }
  }

  // ── DOM (Document / Element over cheerio) ────────────────────────────────
  function wrapAll($, selection) {
    return selection.toArray().map(function (node) { return new Element($, $(node)); });
  }

  class Element {
    constructor($, selection) {
      this._$ = $;
      this._s = selection;
    }
    get text() { return this._s.text(); }
    get ownText() { return this._s.contents().toArray().filter(function (n) { return n.type === 'text'; }).map(function (n) { return n.data; }).join(''); }
    get innerHtml() { return this._s.html() || ''; }
    get html() { return this._s.html() || ''; }
    get outerHtml() { return this._$.html(this._s) || ''; }
    get id() { return this._s.attr('id') || ''; }
    get className() { return this._s.attr('class') || ''; }
    get tagName() { return ((this._s[0] && this._s[0].name) || '').toUpperCase(); }
    get value() { return this._s.val() || this._s.attr('value') || ''; }
    get attributes() { return Object.assign({}, (this._s[0] && this._s[0].attribs) || {}); }
    get children() { return wrapAll(this._$, this._s.children()); }
    get parent() { var p = this._s.parent(); return p.length ? new Element(this._$, p) : null; }
    get nextElementSibling() { var n = this._s.next(); return n.length ? new Element(this._$, n) : null; }
    get previousElementSibling() { var p = this._s.prev(); return p.length ? new Element(this._$, p) : null; }
    attr(name) { var v = this._s.attr(name); return v === undefined ? '' : v; }
    hasAttr(name) { return this._s.attr(name) !== undefined; }
    selectFirst(selector) { var s = this._s.find(selector).first(); return s.length ? new Element(this._$, s) : null; }
    select(selector) { return wrapAll(this._$, this._s.find(selector)); }
    getElementsByClassName(name) { return wrapAll(this._$, this._s.find('.' + name)); }
    getElementsByTagName(name) { return wrapAll(this._$, this._s.find(name)); }
  }

  class Document {
    constructor(html) {
      this._$ = libs.cheerio.load(html === undefined || html === null ? '' : String(html));
    }
    get text() { return this._$.root().text(); }
    get innerHtml() { return this._$.root().html() || ''; }
    get outerHtml() { return this._$.html() || ''; }
    selectFirst(selector) {
      var s = this._$(selector).first();
      return s.length ? new Element(this._$, s) : null;
    }
    select(selector) { return wrapAll(this._$, this._$(selector)); }
    getElementById(id) { return this.selectFirst('[id="' + String(id).replace(/"/g, '\\"') + '"]'); }
    getElementsByClassName(name) { return wrapAll(this._$, this._$('.' + name)); }
    getElementsByTagName(name) { return wrapAll(this._$, this._$(name)); }
  }

  // ── MProvider base class ─────────────────────────────────────────────────
  class MProvider {
    constructor() {
      // Sources read this.source in their constructors (apiUrl, baseUrl), so it must be
      // set before construction. __mgLoad sets g.source first.
      this.source = g.source;
    }
    getPreference(key) {
      var raw = g.__native_storage_get('pref:' + String(key));
      return raw === null || raw === undefined ? undefined : JSON.parse(raw);
    }
    setPreference(key, value) {
      g.__native_storage_set('pref:' + String(key), value === undefined ? null : JSON.stringify(value));
    }
    substringAfter(text, pattern) {
      var i = String(text).indexOf(pattern);
      return i === -1 ? String(text) : String(text).slice(i + pattern.length);
    }
    substringAfterLast(text, pattern) {
      var i = String(text).lastIndexOf(pattern);
      return i === -1 ? String(text) : String(text).slice(i + pattern.length);
    }
    substringBefore(text, pattern) {
      var i = String(text).indexOf(pattern);
      return i === -1 ? String(text) : String(text).slice(0, i);
    }
    substringBeforeLast(text, pattern) {
      var i = String(text).lastIndexOf(pattern);
      return i === -1 ? String(text) : String(text).slice(0, i);
    }
    substringBetween(text, left, right) {
      var a = String(text).indexOf(left);
      if (a === -1) return '';
      var rest = String(text).slice(a + left.length);
      var b = rest.indexOf(right);
      return b === -1 ? '' : rest.slice(0, b);
    }
  }

  g.Client = Client;
  g.Document = Document;
  g.MProvider = MProvider;

  // ── filters ──────────────────────────────────────────────────────────────
  // The app sends one override per template filter (by index): a select index, a
  // boolean for CheckBox, a list of selected option values for GroupFilter, a string
  // for TextFilter, {index, ascending} for SortFilter, or null to keep the default.
  async function filtersWithOverrides(instance, overrides) {
    var list = typeof instance.getFilterList === 'function' ? await instance.getFilterList() : [];
    list = list || [];
    var values = overrides || [];
    for (var i = 0; i < list.length; i++) {
      var filter = list[i];
      var o = values[i];
      if (o === undefined || o === null) continue;
      var type = filter.type_name;
      if (type === 'SelectFilter') {
        filter.state = Number(o);
      } else if (type === 'CheckBox') {
        filter.state = o === true || o === 'true';
      } else if (type === 'TextFilter') {
        filter.state = String(o);
      } else if (type === 'SortFilter') {
        filter.state = { index: Number(o.index), ascending: !!o.ascending };
      } else if (type === 'GroupFilter' && Array.isArray(filter.state)) {
        for (var c = 0; c < filter.state.length; c++) {
          var box = filter.state[c];
          var key = box.value !== undefined ? box.value : box.name;
          box.state = o.indexOf(key) !== -1;
        }
      }
    }
    return list;
  }

  // ── entry points used by JsPluginRuntime.kt ──────────────────────────────
  g.__mgLoad = function (code) {
    var scope = (0, eval)(
      '(function () {\n' + code +
      '\n;return { sources: typeof mangayomiSources !== "undefined" ? mangayomiSources : [],' +
      ' Ext: typeof DefaultExtension !== "undefined" ? DefaultExtension : null };\n})'
    )();
    if (!scope.Ext) throw new Error('Mangayomi source does not declare class DefaultExtension');
    var meta = scope.sources && scope.sources[0] ? scope.sources[0] : {};
    g.source = meta;
    var instance = new scope.Ext();
    instance.source = meta;
    g.__mgInstance = instance;
    return JSON.stringify({
      name: meta.name === undefined ? null : meta.name,
      baseUrl: meta.baseUrl === undefined ? null : meta.baseUrl,
      lang: meta.lang === undefined ? null : meta.lang,
      itemType: meta.itemType === undefined ? null : meta.itemType,
      isManga: meta.isManga === undefined ? null : meta.isManga,
      version: meta.version === undefined ? null : meta.version,
      hasFilters: typeof instance.getFilterList === 'function',
      hasHtmlContent: typeof instance.getHtmlContent === 'function',
      hasPageList: typeof instance.getPageList === 'function',
    });
  };

  g.__mgFilters = async function () {
    var instance = g.__mgInstance;
    if (!instance) throw new Error('Mangayomi source is not loaded');
    var list = typeof instance.getFilterList === 'function' ? await instance.getFilterList() : [];
    return JSON.stringify(list || []);
  };

  g.__mgDispatch = async function (method, argsJson) {
    var instance = g.__mgInstance;
    if (!instance) throw new Error('Mangayomi source is not loaded');
    var fn = instance[method];
    if (typeof fn !== 'function') throw new Error('Source does not implement ' + method + '()');
    var args = JSON.parse(argsJson);
    if (method === 'search') {
      // args = [query, page, overrides]
      args[2] = await filtersWithOverrides(instance, args[2]);
    }
    var result = await fn.apply(instance, args);
    return result === undefined ? 'null' : JSON.stringify(result);
  };
})(globalThis);
