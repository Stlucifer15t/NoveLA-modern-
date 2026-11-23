# Handoff prompt: book reviews in the extension repo

Copy everything below the line into another session that has write access to the
extension repo.

---

You are working on the Lua extension repository `Stlucifer15t/external-sources`
(a fork of `HnDK0/external-sources`). The Android app "Dreamora" (repo
`Stlucifer15t/NoveLA-modern-`) loads its extensions from this repo. Your job is to add
an optional `getBookReviews` function to the Lua scripts of the sites that publish
reviews, so the app's Reviews tab shows real reviews.

## Facts to start from

- Fork default branch: `v2`. The app currently loads
  `https://raw.githubusercontent.com/HnDK0/external-sources/refs/heads/main/index.yaml`.
  The fork has no `main` branch yet. Confirm with the owner which branch the app
  should load, then update `AppPreferences.kt` (L678) and `ExtensionsScreenState.kt`
  (L16) in the app repo to match.
- Upstream `main` head at the time of writing: `ed336ec151fb8b9880bacfdc3359f985e71917fd`.
- Review-capable scripts in `en/`: `webnovel.lua`, `novelfire.lua`, `royal_road.lua`,
  `scribblehub.lua`, `freewebnovel.lua`. Only the first three are known to publish
  reviews (per Novery's implementation). Verify ScribbleHub and FreeWebNovel before
  adding anything.
- Plugin guide: `lua-plugin-guide-en.md`. The optional-hook pattern is `getBookRating`
  (around L230). Follow it.

## The contract (must match exactly)

```lua
function getBookReviews(bookUrl, index)
    -- index: page number, starts at 0
    return {
        items = {
            {
                id       = "12345",        -- string or number, unique per review
                author   = "Ann",
                rating   = 4.5,            -- number 0..5 (rescale the site's scale)
                time     = "2 days ago",   -- text as shown, or omit
                content  = "Great read.",  -- required; reviews without text are skipped
                likes    = 3,
                replies  = 1,              -- count only
                spoiler  = false,
                pinned   = false,
                edited   = false,
            },
        },
        hasNext = true,                    -- true if index + 1 has more reviews
    }
end
```

- Return `nil` or `{ items = {}, hasNext = false }` when there are no reviews.
- A Lua error shows "Reviews could not be loaded" with a retry in the app.
- The full contract is in `guides/REVIEWS_EXTENSION_API.md` in the app repo.

## Hard rules

1. **Do not guess selectors or endpoints.** Each parser must be checked against a real
   saved page or API response for that site. If you do not have one, stop and ask for
   it. Do not write a parser from memory.
2. Novery's reference implementation is GPL-3.0 (`/tmp/novery`, provider files
   `NovelFireProvider.kt` L425, `RoyalRoadProvider.kt` L351, `WebnovelProvider.kt`
   L707). Read it for the endpoints and fields only. Do not copy its code.
3. Keep existing script behaviour unchanged. Only add `getBookReviews`.
4. Test with real output before you claim it works. Report the exact sample you tested
   against and what was returned.
5. One commit per script, with a message that names the site and the sample date.

## Deliverables

- `en/<site>.lua` with `getBookReviews` for each verified site.
- A note in the commit message of what was verified and how.
- A list of sites skipped, and why.

## Open questions for the owner

- Which branch the app should load (`main` after creating it, or `v2`).
- Which sites to cover. The app code supports any of them.
