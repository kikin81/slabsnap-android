This is an excellent catch. You are moving from building a basic "checklist" app into building a true **Collector's Vault**.

Handling parallels (variants) fundamentally changes the definition of a "unique" card. A sticker is no longer uniquely identified by `stickerCode` (e.g., "ARG 10"). It is now identified by the composite key of `(stickerCode, borderColor)`.

Here is the updated **Staff-Level Markdown Plan** to feed to Claude. It incorporates the "Single-Scan + DB Resolution" architecture we discussed, while seamlessly handling the parallel border colors.

---

````markdown
# Milestone 2.3: Parallel Variants, Database Resolution & Fallback Scanning

## Context & Physical Constraints

Based on the physical realities of the US Edition Panini World Cup stickers, we have two major constraints:

1. **The Code is on the Back:** The alphanumeric code (e.g., "ARG 10", "FWC 9") is _only_ printed on the back.
2. **Parallel Variants (Parallels):** The front of the card features a colored border representing rarity (White, Blue, Red, Purple, Green, Black 1/1).

_IMPORTANT:_ alongside this document are 3 photo images:

`player_card_front_and_backs.jpg` - this photo image shows player sticker cards. The top row is the "front" of the player cards. The bottom row is the "back" of the cards. Notice the front shows the player name, country, and other information like date of birth. The back shows the card "id", like ARG 10.

`mixed_cards_font_side.jpeg` - this photo image shows mixed sticker cards showcasing country flags, team picture and the stadium. These front of the cards don't have much text on them and don't have the same properties as the player cards (like player name, country etc).
The backs of these pictures show the card id (country + number) to identify them.

`mixed_cards_back_side` - this photo is the reverse side of the mixed cards previously mentioned. You can see the card id (country + number)

To prevent users from having to scan both sides of 600+ stickers, we are implementing a **Stateful Single-Scan + DB Resolution** architecture:

1. Scan the _front_ to extract text (e.g., "Lionel Messi") AND the `border_color`.
2. Query the local Room database to find the base sticker and hydrate the `stickerCode`.
3. Save/Update the specific parallel variant `(stickerCode, border_color)` as owned.
4. **Fallback:** If the card is a textless shiny foil (blinding the AI's text extraction), transition to a "Scan Back" state to get the `stickerCode`, while _retaining_ the `border_color` captured from the front scan.

## Task 1: Update the Schema (`StickerEntity.kt` & `StickerDao.kt`)

A sticker is now uniquely identified by its code AND its border color.

-   [ ] Add `val borderColor: String = "White"` to `StickerEntity`.
-   [ ] Update the `Index` in `StickerEntity` to enforce this new composite uniqueness:
    ```kotlin
    indices = [
        Index(value = ["collectionSetId", "stickerCode", "borderColor"], unique = true)
    ]
    ```
````

-   [ ] Add a search query to `StickerDao.kt` to find the _base_ sticker (for text resolution):
    ```kotlin
    @Query("""
        SELECT * FROM stickers
        WHERE collectionSetId = :setId
        AND (playerName LIKE '%' || :query || '%' OR teamName LIKE '%' || :query || '%')
        LIMIT 1
    """)
    suspend fun findBaseStickerByText(setId: Long, query: String): StickerEntity?
    ```

## Task 2: Refactor the Extractor (`CardDataExtractor.kt` & `ExtractedCardData.kt`)

Stop looking for `sticker_number` on the front. Extract generalized text and the border color.

-   [ ] Update `ExtractedCardData.kt` to:
    ```kotlin
    @Serializable
    data class ExtractedCardData(
        @SerialName("primary_text") val primaryText: String,
        @SerialName("badge_text") val badgeText: String,
        @SerialName("border_color") val borderColor: String,
        @SerialName("is_foil") val isFoil: Boolean
    )
    ```
-   [ ] Update `CardDataExtractor.EXTRACTION_PROMPT`:
    -   Instruct the AI to extract `primary_text` (main name/text on the card) and `badge_text` (if it's a shiny crest).
    -   Add instructions to extract `border_color`: "Identify the color of the border framing the sticker (e.g., White, Blue, Red, Purple, Green, Black). Default to White if unsure."
    -   Explicitly state: "Do not attempt to guess the sticker number, it is not on this side of the card."
-   [ ] Add a secondary method `extractBackCode(bitmap: Bitmap): String` to `CardDataExtractor`.
    -   Prompt: "Extract the short alphanumeric sticker code from the top right of this image (e.g., ARG 2, MAR 1, FWC 9). Return ONLY the string, no JSON."

## Task 3: ViewModel Resolution Logic (`LiveScannerViewModel.kt`)

We need to hold the color in state in case we are forced to scan the back.

-   [ ] Add `needsBackScan: Boolean = false` to `LiveScannerState`.
-   [ ] Add `pendingBorderColor: String? = null` to `LiveScannerState`.
-   [ ] In the ViewModel's primary extraction flow:
    1. Call `extractor.extract(bitmap)`.
    2. If `primaryText == "UNKNOWN"` and `badgeText == "UNKNOWN"`:
        - The AI was blinded by foil.
        - Set `pendingBorderColor = extractedData.borderColor`.
        - Set `needsBackScan = true` and send an effect telling the user to flip the card.
    3. If text _was_ found, resolve it via `repository.findBaseStickerByText()`.
    4. If found:
        - Insert or update a `StickerEntity` matching the found `stickerCode` and the extracted `borderColor`, marking `isOwned = true`.
    5. If NOT found: Set `pendingBorderColor = extractedData.borderColor` and `needsBackScan = true`.
-   [ ] In the ViewModel's secondary (Back Scan) flow:
    1. Call `extractor.extractBackCode(bitmap)`.
    2. Combine the resulting `stickerCode` with the `state.pendingBorderColor`.
    3. Insert/update the database. Reset `needsBackScan = false` and `pendingBorderColor = null`.

## Task 4: UI Fallback State (`LiveScannerScreen.kt`)

-   [ ] When `state.needsBackScan` is true:
    -   Update the UI text to explicitly instruct the user: "Scan the back of the sticker (e.g., ARG 2)".
    -   When stability is reached, route the Bitmap to `extractor.extractBackCode()` instead of the main JSON extractor.

```

***

### Staff Tips for you and Claude:
1. **The "Base" Seed Data**: When you populate your app with the 600+ Panini checklist items initially, they should all be seeded with `borderColor = "White"`. When the user scans a Blue parallel, the repository layer should *copy* the base White entity, change the color to Blue, set `isOwned = true`, and `insert()` it as a new row.
2. **State Cleanup**: Ensure Claude explicitly nullifies `pendingBorderColor` after a successful back-scan so the state doesn't leak into the next card the user scans.
```
