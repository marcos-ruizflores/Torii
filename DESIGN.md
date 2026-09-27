---
name: Torii
description: A departures board for your own trip; every date combination Torii scanned is a row, the cheapest on top.
colors:
  signal-yellow: "#ffc629"
  signal-yellow-hover: "#ffd152"
  signal-yellow-deep: "#c99300"
  ink-on-signal: "#15120a"
  glyph-white: "#eeeae0"
  graphite-ground: "#111317"
  graphite-panel: "#181b20"
  graphite-raised: "#22262d"
  graphite-rule-strong: "#353b45"
  graphite-mute: "#505762"
  steel-text-secondary: "#b2b7c0"
  steel-text-tertiary: "#969ca6"
  flap-tile: "#2c313a"
  flap-tile-low: "#242830"
  flap-seam: "#0a0b0d"
  caution-orange: "oklch(75% 0.183 55.934)"
  caution-orange-solid: "oklch(70.5% 0.213 47.604)"
  go-green: "oklch(79.2% 0.209 151.711)"
  fault-red: "oklch(70.4% 0.191 22.216)"
typography:
  display:
    fontFamily: "Barlow Semi Condensed, Barlow, -apple-system, Segoe UI, Arial, sans-serif"
    fontSize: "36px"
    fontWeight: 600
    lineHeight: "44px"
    letterSpacing: "-0.72px"
  headline:
    fontFamily: "Barlow Semi Condensed, Barlow, -apple-system, Segoe UI, Arial, sans-serif"
    fontSize: "30px"
    fontWeight: 600
    lineHeight: "38px"
  title:
    fontFamily: "Barlow Semi Condensed, Barlow, -apple-system, Segoe UI, Arial, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: "28px"
    letterSpacing: "0.025em"
  glyph:
    fontFamily: "Barlow Semi Condensed, Barlow, -apple-system, Segoe UI, Arial, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: "28px"
    fontFeature: "\"tnum\" 1"
  body:
    fontFamily: "Barlow, -apple-system, Segoe UI, Roboto, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    fontFeature: "\"tnum\" 1"
  body-small:
    fontFamily: "Barlow, -apple-system, Segoe UI, Roboto, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
  label:
    fontFamily: "Barlow Semi Condensed, Barlow, -apple-system, Segoe UI, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: "16px"
    letterSpacing: "0.08em"
rounded:
  md: "4px"
  lg: "5px"
  xl: "6px"
  2xl: "8px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "20px"
  xl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.signal-yellow}"
    textColor: "{colors.ink-on-signal}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: "10px 16px"
  button-primary-hover:
    backgroundColor: "{colors.signal-yellow-hover}"
    textColor: "{colors.ink-on-signal}"
  button-secondary:
    backgroundColor: "{colors.graphite-ground}"
    textColor: "{colors.steel-text-secondary}"
    typography: "{typography.body-small}"
    rounded: "{rounded.lg}"
    padding: "8px 12px"
  button-tertiary:
    textColor: "{colors.steel-text-tertiary}"
    typography: "{typography.body-small}"
    rounded: "{rounded.lg}"
    padding: "8px 12px"
  board-panel:
    backgroundColor: "{colors.graphite-panel}"
    textColor: "{colors.glyph-white}"
    rounded: "{rounded.xl}"
    padding: "20px"
  board-row-winning:
    backgroundColor: "{colors.graphite-ground}"
    padding: "16px 20px"
  input-field:
    backgroundColor: "{colors.graphite-ground}"
    textColor: "{colors.glyph-white}"
    typography: "{typography.glyph}"
    rounded: "{rounded.lg}"
    padding: "10px 14px"
  switch-option-active:
    backgroundColor: "{colors.signal-yellow}"
    textColor: "{colors.ink-on-signal}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  switch-option:
    textColor: "{colors.steel-text-tertiary}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  flap-tile:
    backgroundColor: "{colors.flap-tile}"
    textColor: "{colors.glyph-white}"
    width: "0.86em"
    height: "1.3em"
  nav-link:
    textColor: "{colors.steel-text-secondary}"
    typography: "{typography.body-small}"
    rounded: "{rounded.md}"
    padding: "6px 10px"
  nav-link-active:
    textColor: "{colors.signal-yellow}"
---

# Design System: Torii

## Overview

**Creative North Star: "The Departures Board"**

Torii is an airport departures board for one traveller's trip. Every date combination the scan tried becomes a row; the cheapest lands on top and is the only thing on the board that glows. The whole app lives on one dark graphite board: cool, dense, fixed-column, read in condensed capitals and tabular figures, with split-flap tiles for the glyphs that change (route, winning price, lookup counter) and for the wordmark.

Density is operational, not editorial. Panels are flat graphite slabs separated by hairline rules; information sits in labelled columns that align from row to row, like a real board. Colour carries meaning and almost nothing else: one signal yellow taken from Spanish airport wayfinding marks the action, the current state and the answer; green and orange speak only in verdicts and stop counts, always next to a word.

The system rejects the category default of a white search card above a stack of rounded result cards. Results are rows on a board, not cards.

**Key Characteristics:**
- Always dark: the `.dark-mode` palette is set on `<html>`; there is no light theme.
- One accent, signal yellow, reserved for action, current state and the winning price.
- Barlow Semi Condensed caps for every board glyph and label; Barlow for running text; tabular figures everywhere.
- Split-flap tiles for changing glyphs and the wordmark; blank ticking flaps while scanning.
- Fixed-column results grid; flat panels with hairline rules; cut corners (4-8px).

## Colors

A cool graphite board with warm white glyphs and a single sodium-yellow signal; status hues appear only as labelled verdicts.

### Primary
- **Signal Yellow** (#ffc629): primary buttons ("Escanear fechas", "Crear cuenta", the cheapest row's "Reservar"), the active Vacaciones/Escapadas and 7/30 días options, the active nav link, the torii mark, the winning price flaps, the lookup counter at normal load, the route line and destination marker on the map, the price-history line and its fading area fill, checkmarks on the tariff board, text selection, the input caret and the focus ring family.
- **Signal Yellow Hover** (#ffd152): hover and loading state of yellow buttons and the focus ring only. Never a second resting yellow.
- **Ink on Signal** (#15120a): every glyph and icon sitting on yellow (buttons, active switch options, selected date cells, checkbox and radio marks, selected text).

### Secondary
- **Caution Orange** (oklch(75% 0.183 55.934)): "Precio alto" verdicts, the "above average" note in the price history, the medium-load lookup count. The theme's warning tokens are remapped to orange so yellow never means caution.
- **Caution Orange Solid** (oklch(70.5% 0.213 47.604)): verdict marks for "Precio alto" and the quota meter past 75%.
- **Go Green** (oklch(79.2% 0.209 151.711)): "Precio bajo" verdicts, "Directo" stops, the period minimum in the price history and its dashed guide.
- **Fault Red** (oklch(70.4% 0.191 22.216)): errors, over-quota and heavy-search warnings; the quota meter turns solid red when empty.

### Neutral
- **Graphite Ground** (#111317): page background; also the recessed well for the winning results row, the user's current tariff column, inputs, secondary buttons and segmented-switch trays.
- **Graphite Panel** (#181b20): every board panel (search, results, route, history, saved searches, auth form).
- **Graphite Raised** (#22262d): hairline rules between rows and around panels, map land masses, the quota meter track.
- **Graphite Rule Strong** (#353b45): input and secondary-button rings, blank flap glyph colour, scrollbar thumb.
- **Graphite Mute** (#505762): quiet marks such as the "Precio habitual" verdict square.
- **Glyph White** (#eeeae0): primary text and board glyphs; the only neutral with a warm cast, like lit characters on a board.
- **Steel Secondary** (#b2b7c0): secondary text, nav links at rest, secondary buttons.
- **Steel Tertiary** (#969ca6): labels, sub-lines (times, "vía DOH", ranges), placeholders and the lead paragraph. Kept at this value so placeholders hold AA on graphite.
- **Flap Tile / Flap Tile Low / Flap Seam** (#2c313a, #242830, #0a0b0d): the upper and lower halves of a split-flap tile and the 1px split line between them. Used only by flap tiles.

### Named Rules
**The One Signal Rule.** Signal yellow means "act here", "you are here" or "this is the answer". It never decorates, never tags a category, never warns.

**The Worded Verdict Rule.** Status colour never stands alone. Every verdict is a word in condensed caps plus a small square mark ("PRECIO BAJO", "PRECIO HABITUAL", "PRECIO ALTO"); "DIRECTO" is a word, not a green dot.

**The Warm Glyph Rule.** Only glyph white carries warmth; every other neutral stays cool graphite.

## Typography

**Display Font:** Barlow Semi Condensed (with Barlow, system sans fallback), self-hosted at 500/600/700
**Body Font:** Barlow (with system sans fallback), self-hosted at 400/500/600

**Character:** A condensed grotesque with the no-nonsense cut of transport signage for everything the board displays, paired with its regular-width sibling for sentences. Tabular figures are set on `html`, so every date, count and price aligns column to column.

### Hierarchy
- **Display** (600, 36px/44px, -0.72px; 30px/38px below 640px): the one page headline, sentence case, balanced wrap, max ~48rem.
- **Headline** (600, 30px/38px; 24px/32px on auth screens): page titles on secondary pages (Mis búsquedas, Página no encontrada, login and sign up).
- **Title** (600, 18px/28px, 0.025em, uppercase): panel names at the left of each panel header: TU VIAJE, SALIDAS, RUTA, HISTÓRICO, tariff names.
- **Glyph** (600, 18px, uppercase, tabular): board values: dates ("16 NOV"), nights, stop counts, verdict words (14px), non-winning prices (20px), history stats (20px). Form input values use it at 17px with 0.02em tracking.
- **Body** (400, 16px/24px; lead paragraph 18px/28px in Steel Tertiary, max ~42rem): running text and empty-state explanations, max 65ch.
- **Body small** (400, 14px/20px): times, sub-lines, helper text, nav.
- **Label** (600, 12px/16px, 0.08em, uppercase, Steel Tertiary): every column header and form field label, the lookup counter caption, the quota counter, the results guide words ("16 NOV — 27 NOV · 5 OFERTAS").

### Named Rules
**The Board Voice Rule.** Anything the board displays (labels, values, verdicts, panel names) is Barlow Semi Condensed in capitals; anything the board says to you in a sentence is Barlow in sentence case.

**The Tabular Rule.** Figures are always tabular. A price or date that shifts width between rows is a bug.

## Layout

Single column inside a 1152px container (max-w-6xl) with 16px side padding (24px from 640px), 24px top and 64px bottom. Sections stack with a 32px gap: header, headline block, search panel, results board, then route and history side by side from 1024px (two equal columns, 32px gap), then saved searches.

Panels use 20px inner padding (24px for the search form from 640px). Panel headers are a 16px/20px band with a hairline below. The rhythm is a 4px grid; 16px is the default gap between columns, 20px between form rows.

**Search form:** a 12-column grid from 1024px (Origen and Destino 2 each, Ventana 4, Noches and Flexibilidad 2 each; second row Escalas, Ofertas, Precisión 3, Presupuesto), with the lookup counter and the yellow scan button stacked right-aligned at the row's end. Two columns below 1024px, the window and counter spanning both.

**Results board:** from 768px a fixed-column grid: Salida 6.5rem, Vuelta 6.5rem, Noches 4rem, Aerolínea minmax(7rem, 1fr), Escalas minmax(6.5rem, 8rem), Veredicto 8rem, Precio 7rem (right-aligned), Reserva 7.5rem, 16px column gap, 16px/20px row padding. Below 768px each row becomes the same fixed two-column grid (1fr / auto): dates and nights, airline and stops, verdict and price with the book button beneath. Every row has identical structure so the eye can scan down.

**Tariff board:** a real table from 768px (min 40rem, horizontal scroll if needed); stacked plan blocks with definition rows on phones.

**Header:** mark and flap wordmark left; nav, a vertical hairline, quota counter, user and "Salir" right. On phones the quota counter and a single "Cuenta" menu share the wordmark line.

Breakpoints: 640px, 768px (results grid), 1024px (form grid, side-by-side panels).

## Elevation & Depth

The board is flat. Panels have no drop shadow; they are Graphite Panel slabs on Graphite Ground outlined by a 1px inset ring in Graphite Raised, and depth reads through tonal steps: ground (#111317) below panel (#181b20) below raised rules (#22262d). The winning row and the current plan column go the other way, recessing to the ground colour so they read as the lit slot on the board.

The only real shadows are physical: flap tiles carry a hairline top highlight and a tight contact shadow so they read as tiles, and the inherited Untitled UI buttons and inputs keep their near-invisible skeuomorphic inset.

### Shadow Vocabulary
- **Flap tile** (`box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.05), 0 1px 2px rgb(0 0 0 / 0.45)`): split-flap tiles only.
- **Control inset** (`box-shadow: 0 0 0 1px rgba(0,0,0,0.18) inset, 0 -2px 0 0 rgba(0,0,0,0.05) inset, 0 1px 2px rgba(0,0,0,0.05)`): primary and secondary buttons; inputs use just the last layer.

### Named Rules
**The Flat Board Rule.** Panels never lift. Separation is a hairline or a tonal step, never a drop shadow.

**The Recessed Winner Rule.** The cheapest row and the user's current tariff drop to Graphite Ground inside their panel; emphasis is a recess, not a lift or a border.

## Shapes

Corners are cut, not moulded: controls and panels stay between 4px and 8px. Panels 6px; buttons, inputs and switch trays 5px; switch options and nav links 4px; 8px is the ceiling. Flap tiles round at 0.14em so they scale with the glyph. Marks are square: verdict marks are 8px squares with 1px corners, map airports are 10px squares, the torii gate mark is solid-filled. Rules are 1px hairlines. The quota meter is the one rounded-pill shape, a 4px-high progress bar.

## Components

### Buttons
Solid, compact and unmistakable; yellow only where the next move is.
- **Shape:** gently cut corners (5px), 1px inner highlight.
- **Primary:** Signal Yellow with Ink on Signal text, semibold. Large for the scan button (10px 16px, 16px text, leading search icon), small elsewhere (8px 12px, 14px). One primary per view region: the scan button, "Crear cuenta", the cheapest row's "Reservar", the active period option.
- **Hover / Focus:** hover lightens to Signal Yellow Hover over 100ms linear; focus is a 2px outline offset 2px in the brand focus colour. Loading keeps the hover colour.
- **Secondary:** Graphite Ground with a Graphite Rule Strong ring and Steel Secondary text; every non-winning "Reservar", "Iniciar sesión", "Repetir", "Cuenta".
- **Tertiary:** text only in Steel Tertiary, hover fills the ground; "Salir", inactive period option.

### Segmented switches
- **Style:** a Graphite Ground tray with a hairline ring and 4px padding holding two options.
- **State:** the active option is solid Signal Yellow with Ink on Signal; inactive options are Steel Tertiary and brighten to Glyph White on hover (150ms). The trip-mode switch (VACACIONES / ESCAPADAS) sets its options in label type at 14px; the history period switch uses small buttons.

### Cards / Containers (board panels)
- **Corner Style:** 6px.
- **Background:** Graphite Panel.
- **Shadow Strategy:** none; see Elevation & Depth.
- **Border:** 1px inset ring in Graphite Raised; internal sections split by the same hairline.
- **Internal Padding:** 20px (header band 16px/20px).
- **Header:** panel title in Title type on the left, a flap route or guide words beside it, secondary context right-aligned.

### Inputs / Fields
- **Style:** Graphite Ground fill, 1px inset ring in Graphite Rule Strong, 5px corners; label above in Label type; value in Glyph type (17px, 600); helper text below in Body small, Steel Tertiary; caret Signal Yellow.
- **Focus:** the ring thickens to 2px in the brand colour.
- **Error / Disabled:** error ring in the subtle error tone; disabled at 50% opacity.

### Navigation
- **Style:** 14px semibold links with 4px corners and 6px/10px padding. Rest Steel Secondary, hover Glyph White, active page Signal Yellow; 150ms colour transitions and a 2px focus outline.
- **Quota counter:** a Label-type "12/30 CONSULTAS" with the used count in Glyph White above a 4px meter: yellow while healthy, orange past 75%, red when empty; "Consultas ilimitadas" when uncapped.
- **Mobile:** links collapse into one "Cuenta" dropdown beside the quota counter.

### Split-flap text (signature)
The board's own component. Each character is a tile 0.86em x 1.3em, top half Flap Tile, bottom half Flap Tile Low, a 1px Flap Seam across the middle, the glyph in Barlow Semi Condensed 600 caps, 0.08em gaps. Tones: ink (Glyph White), signal (Signal Yellow, winning price, counter at normal load, 404), dim (Steel Tertiary, the counter at high load or over quota). Spaces render as blank tiles in Graphite Rule Strong.
- **On change:** tiles run through the alphabet every 38ms and settle left to right (five flips plus 1.4 per position), so a short route or price settles in roughly half a second and a nine-tile route in about 600ms. It runs once per value change.
- **Still:** the wordmark, tariff prices and saved-search routes are drawn settled. Under prefers-reduced-motion every flap just switches.
- **Accessibility:** screen readers get the plain text; tiles are aria-hidden.
- **Blank and ticking rows:** idle and scanning results show three rows of blank tiles; while scanning they tick (420ms, cubic-bezier(0.22, 1, 0.36, 1), staggered 0.09s and 0.17s) and stop under reduced motion.

### Results row
Fixed columns (see Layout). Dates and nights in Glyph type with times below in Body small; airline in semibold with "LA MÁS BARATA" in 12px yellow condensed caps under it on the cheapest row only; stops as "1 ESCALA" with "vía XXX" beneath, or "DIRECTO" in green; verdict as word plus square mark with "Lo normal: 610–1050 €" below; price right-aligned (20px glyph, or yellow flap tiles at 24px on the cheapest row); book button last. Rows split by hairlines; the cheapest recessed to Graphite Ground.

### Charts and map
SVG colours resolve the same CSS tokens at runtime (they cannot take `var()`). History: Signal Yellow line with a 22%-to-0 yellow area fill, dashed hairline grid in Graphite Raised, 12px Steel Tertiary ticks, the minimum as a dashed green guide; a three-cell stats strip (HOY, MÍNIMO, MEDIA) above. Map: Graphite Raised land with Graphite Ground borders, a 2px dashed yellow route, a white square at origin and a yellow square at destination with condensed 17px labels.

## Do's and Don'ts

### Do:
- **Do** keep every screen on the dark graphite board: Graphite Ground page, Graphite Panel panels, 1px Graphite Raised hairlines.
- **Do** reserve Signal Yellow (#ffc629) for the primary action, the current state and the winning price; use #ffd152 only for hover.
- **Do** put Ink on Signal (#15120a) on every yellow surface.
- **Do** set column headers and field labels in Barlow Semi Condensed 12px, 600, 0.08em, uppercase.
- **Do** align tabular data in fixed columns on desktop and a fixed two-column grid per row on phones.
- **Do** word every verdict and pair it with a square mark: green "PRECIO BAJO", dim "PRECIO HABITUAL", orange "PRECIO ALTO".
- **Do** use split-flap tiles for values that change on the board and for the wordmark, settling once per change and static under prefers-reduced-motion.
- **Do** show idle and scanning results as rows of blank flaps, not spinners or skeleton bars.

### Don't:
- **Don't** use yellow for warnings, caution or category tags; caution is orange (oklch(75% 0.183 55.934)).
- **Don't** show status by colour alone.
- **Don't** render results as rounded cards; they are rows on one board.
- **Don't** lift panels with drop shadows; the flap tile's contact shadow is the only visible one.
- **Don't** round panels, buttons, inputs or tiles past 8px.
- **Don't** add a light theme or a white surface.
- **Don't** flip tiles on every render or loop them outside the scanning state.
