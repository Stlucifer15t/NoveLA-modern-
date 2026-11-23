// Entry point of the JS host library bundle. Built by `npm run build` into
// src/main/assets/js/js-libs.js. Exposes the modules plugins `require()` as __libs.
import * as cheerio from "cheerio";
import * as htmlparser2 from "htmlparser2";
import * as domhandler from "domhandler";
import dayjs from "dayjs";
import * as aes from "@noble/ciphers/aes";

globalThis.__libs = { cheerio, htmlparser2, domhandler, dayjs, aes };
