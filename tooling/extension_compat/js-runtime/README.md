# JS host libraries

LNReader and Mangayomi plugins are JavaScript that imports npm packages (`cheerio`,
`htmlparser2`, `dayjs`, `domhandler`). The app runs them in QuickJS, which has no
module loader for npm packages, so those packages are bundled here into one script.

Rebuild after changing the dependencies:

    cd tooling/extension_compat/js-runtime
    npm ci
    npm run build

The output `src/main/assets/js/js-libs.js` is committed so the Android build does not
need Node. Versions are pinned in `package.json` and `package-lock.json`. LNReader's own
pins are cheerio ^1.0.0, dayjs ^1.11.13 and htmlparser2 ^9.1.0.
