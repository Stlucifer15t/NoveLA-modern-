# Book reviews in extensions (`getBookReviews`)

Reviews are an optional capability of a Lua extension, the same way `getBookRating`
and `getBookStatus` are optional. The app never invents reviews: if an extension
does not define `getBookReviews`, the Reviews tab says the source does not publish
reviews.

## The function

```lua
function getBookReviews(bookUrl, index)
    -- index: page number, starts at 0
    return {
        items = {
            {
                id       = "12345",          -- string or number, unique per review
                author   = "Ann",
                rating   = 4.5,              -- number 0..5 (rescale your site's scale yourself)
                time     = "2 days ago",     -- text as the site shows it, or omit
                content  = "Great read.",    -- required; reviews without text are skipped
                likes    = 3,
                replies  = 1,                -- count only; threads are not loaded yet
                spoiler  = false,            -- true hides the text behind a tap
                pinned   = false,
                edited   = false,
            },
        },
        hasNext = true,                      -- true if index + 1 has more reviews
    }
end
```

Rules:

- Return `nil` or `{ items = {}, hasNext = false }` when the book has no reviews.
  Both show "No reviews yet", not an error.
- A Lua error inside the function shows "Reviews could not be loaded" with a retry.
- `rating` is optional. Strings like `"3.5"` are accepted; values outside 0..5 are clamped.
- Implement the format the site actually serves. Do not guess selectors or endpoints:
  check them against a saved page or API response first, as the plugin guide asks for
  every other function.

## What the app does with it

- The Reviews tab loads page 0 when opened and shows a "Load more" button while `hasNext` is true.
- Spoiler reviews are hidden behind "Spoiler. Tap to show" on each card.
- Reviews are never cached across books and never mixed with another book's reviews.
