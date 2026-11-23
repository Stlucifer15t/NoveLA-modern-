/*
 * Dreamora JS host, part 2: the LNReader plugin contract.
 *
 * LNReader plugins are TypeScript compiled to CommonJS (`exports.default = new Plugin()`)
 * that import `@libs/fetch`, `@libs/storage`, `@libs/filterInputs`, `@libs/novelStatus`,
 * `@libs/isAbsoluteUrl`, `@/types/*` and npm packages (cheerio, htmlparser2, dayjs).
 * This file provides those modules and the entry points that JsPluginRuntime.kt calls:
 *
 *   __lnLoad(code)                 -> JSON {id, name, site, version, filters, hasSearch, hasPages}
 *   __lnDispatch(method, argsJson) -> Promise<JSON text of the result>
 */
(function (g) {
  'use strict';

  // ── @libs/fetch ───────────────────────────────────────────────────────────
  // Same contract as LNReader's src/lib/fetch.ts: the helpers return '' on failure.
  var fetchApi = function (url, init) {
    return g.fetch(url, init);
  };
  var fetchText = async function (url, init, encoding) {
    try {
      var res = await g.fetch(url, init);
      if (!res.ok) return '';
      if (!encoding || /^utf-?8$/i.test(encoding)) return await res.text();
      return g.__native_decode(res._bodyBase64(), encoding);
    } catch (e) {
      return '';
    }
  };
  var fetchFile = async function (url, init) {
    try {
      var res = await g.fetch(url, init);
      if (!res.ok) return '';
      return res._bodyBase64();
    } catch (e) {
      return '';
    }
  };
  var fetchProto = function () {
    throw new Error('fetchProto is not supported by the Dreamora JS runtime');
  };
  var fetchViaWebview = async function () {
    return { ok: false, html: '' };
  };
  var fetchModule = {
    fetchApi: fetchApi,
    fetchText: fetchText,
    fetchFile: fetchFile,
    fetchProto: fetchProto,
    fetchViaWebview: fetchViaWebview,
  };
  g.__hostRegister('@libs/fetch', fetchModule);
  g.__hostRegister('@/lib/fetch', fetchModule);

  // ── storage (per source, persisted by the app) ───────────────────────────
  function Storage() {}
  Storage.prototype.get = function (key) {
    var raw = g.__native_storage_get(String(key));
    return raw === null || raw === undefined ? undefined : JSON.parse(raw);
  };
  Storage.prototype.set = function (key, value) {
    g.__native_storage_set(String(key), value === undefined ? null : JSON.stringify(value));
  };
  Storage.prototype.delete = function (key) {
    g.__native_storage_set(String(key), null);
  };
  Storage.prototype.clear = function () {
    g.__native_storage_clear();
  };
  var storage = new Storage();
  var storageModule = { storage: storage, localStorage: storage, sessionStorage: storage };
  g.__hostRegister('@libs/storage', storageModule);
  g.__hostRegister('@/lib/storage', storageModule);

  // ── types and helpers ─────────────────────────────────────────────────────
  var FilterTypes = {
    TextInput: 'Text',
    Picker: 'Picker',
    CheckboxGroup: 'Checkbox',
    Switch: 'Switch',
    ExcludableCheckboxGroup: 'XCheckbox',
  };
  var NovelStatus = {
    Unknown: 'Unknown',
    Ongoing: 'Ongoing',
    Completed: 'Completed',
    Licensed: 'Licensed',
    PublishingFinished: 'Publishing Finished',
    Cancelled: 'Cancelled',
    OnHiatus: 'On Hiatus',
    STUB: 'STUB',
    Inactive: 'Inactive',
  };
  // Copied from LNReader src/lib/utils.ts (isUrlAbsolute).
  var isUrlAbsolute = function (url) {
    if (url) {
      if (url.indexOf('//') === 0) return true;
      if (url.indexOf('://') === -1) return false;
      if (url.indexOf('.') === -1) return false;
      if (url.indexOf('/') === -1) return false;
      if (url.indexOf(':') > url.indexOf('/')) return false;
      if (url.indexOf('://') < url.indexOf('.')) return true;
    }
    return false;
  };
  g.__hostRegister('@libs/filterInputs', { FilterTypes: FilterTypes });
  g.__hostRegister('@/types/filters', { FilterTypes: FilterTypes });
  g.__hostRegister('@libs/novelStatus', { NovelStatus: NovelStatus });
  g.__hostRegister('@/types/constants', { NovelStatus: NovelStatus });
  g.__hostRegister('@libs/isAbsoluteUrl', { isUrlAbsolute: isUrlAbsolute });
  g.__hostRegister('@/lib/utils', { isUrlAbsolute: isUrlAbsolute });
  g.__hostRegister('@/types/plugin', {});

  // ── entry points used by JsPluginRuntime.kt ──────────────────────────────
  g.__lnLoad = function (code) {
    var module = { exports: {} };
    var factory = (0, eval)('(function (module, exports, require) {\n' + code + '\n})');
    factory(module, module.exports, g.__hostRequire);
    var exported = module.exports;
    var plugin = exported && exported.default ? exported.default : exported;
    if (typeof plugin === 'function') plugin = new plugin();
    if (!plugin || typeof plugin !== 'object') {
      throw new Error('LNReader plugin does not export an object');
    }
    g.__lnPlugin = plugin;
    return JSON.stringify({
      id: plugin.id === undefined ? null : plugin.id,
      name: plugin.name === undefined ? null : plugin.name,
      site: plugin.site === undefined ? null : plugin.site,
      version: plugin.version === undefined ? null : plugin.version,
      filters: plugin.filters === undefined ? null : plugin.filters,
      hasSearch: typeof plugin.searchNovels === 'function',
      hasPages: typeof plugin.parsePage === 'function',
    });
  };

  g.__lnDispatch = async function (method, argsJson) {
    var plugin = g.__lnPlugin;
    if (!plugin) throw new Error('LNReader plugin is not loaded');
    var fn = plugin[method];
    if (typeof fn !== 'function') throw new Error('Plugin does not implement ' + method + '()');
    var args = JSON.parse(argsJson);
    var result = await fn.apply(plugin, args);
    return result === undefined ? 'null' : JSON.stringify(result);
  };
})(globalThis);
