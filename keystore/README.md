# Signing key

`dreamora-release.p12` is a self-generated RSA-2048 key (PKCS#12, alias `novela`) that
signs **every** build of this fork — debug and release alike — with the same
certificate. Both the store password and the key password are intentionally empty:
`app/build.gradle.kts` picks the keystore up automatically, so no local
`key.properties`, no `ANDROID_KEYSTORE_PASSWORD`, no re-import per machine, and an
installed build can be updated in place instead of being uninstalled first.

```
storeFile     = keystore/dreamora-release.p12
storeType     = PKCS12
storePassword = (empty)
keyAlias      = novela
keyPassword   = (empty)
```

To regenerate (needs openssl, not keytool):

```sh
openssl genrsa -out key.pem 2048
openssl req -x509 -new -key key.pem -sha256 -days 10000 \
  -subj "/CN=NoveLA/O=NoveLA/OU=extensions" \
  -addext "keyUsage=digitalSignature" -addext "basicConstraints=critical,CA:FALSE" \
  -out cert.pem
openssl pkcs12 -export -name novela -inkey key.pem -in cert.pem \
  -out dreamora-release.p12 -passout pass: -keypbe AES-256-CBC -certpbe AES-256-CBC
```

Certificate SHA-256 fingerprint (compare it against an APK with
`apksigner verify --print-certs`):

```
91:33:5E:2C:A9:13:19:09:EF:51:DF:E0:D4:55:69:86:FB:53:8E:BF:C6:10:69:1F:BF:7D:91:78:D6:BA:17:63
```

## What this trade-off means

This key is public, because the repository is. Anyone can therefore produce updates
signed as this app, and Android will accept them as legitimate updates for anyone
installing these builds. That is fine for a personal fork and sideloaded nightly
testing — it is **not** fine for a release channel you actually distribute:

- a real release key must stay secret (CI secrets / a keystore outside the repo);
- a Play-signed app cannot rotate this certificate without a Play-managed key migration.

To use a private key instead: put `storeFile`, `storePassword`, `keyAlias`,
`keyPassword` into `local.properties` (or pass
`-PlocalPropertiesFilePath=custom.properties`, as the CI workflows do) — the `default`
signing config then overrides this bundled one. The `.jks` / `*.keystore` paths in
`.gitignore` keep a private keystore out of Git; `*.p12` is only tracked here because
this key is deliberately shared.
