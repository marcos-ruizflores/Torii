---
version: 1
slug: "frontend-src-app-tsx"
primary_target: "frontend/src/App.tsx"
related_targets: ["frontend/src/pages","frontend/src/components"]
---

# Torii app shell and search board

Scope: every route of the SPA (search + results, plans, login, sign up, my searches, 404). Mode: Operate for the search board and account screens; the plans page is Persuade but stays inside the same world.

Audience and task: travellers in Spain with flexible dates or weekend getaways, choosing a window and a stay, seeing what the scan will cost, then comparing the cheapest round trips and whether each price is good. Constraints: Spanish copy, no invented proof, plan changes disabled, FREE only gets Fast precision.

## Direction contract

THESIS: The app is an airport departures board for your own trip. Every date combination Torii tried becomes a row on the board; the cheapest lands on top. It refuses the category default of a white search card over a list of rounded result cards.

OWN-WORLD: Graphite board (#111317 ground, #181b20 panels, #22262d flap tiles with a dark split line), warm white glyphs (#eeeae0), one signal yellow from Spanish airport signage (#ffc629) for primary actions, current state and the winning price. Barlow Semi Condensed caps with tabular figures for every board glyph; Barlow for running text. Square-ish 2-6px corners, hairline rules, fixed column grid for dates, nights and prices. Verdicts are labelled flags (PRECIO BAJO / HABITUAL / ALTO) in green, dim or orange, never colour alone.

STORY: The visitor sees the board and understands Torii scans dates for them, fills the window, watches the lookup counter state the cost, scans, reads the top row as the answer and the verdict as the judgement, then books or repeats a saved search.

FIRST VIEWPORT: Top bar with a torii mark and TORII in split-flap tiles, quota chip and account links on the right. Below, the headline "Tú eliges la ventana. Torii prueba cada fecha." with the existing one-line subtitle. Then the search panel as one board row: labelled columns (Origen, Destino, Ventana, Noches, Flexibilidad, Escalas, Ofertas, Precisión, Presupuesto), a Vacaciones / Escapadas switch, the flap counter "~N consultas" and the yellow "Escanear fechas" button at the row's right end. The results board sits directly under it, idle rows of blank flaps until the first scan.

FORM: Departures board (user-pinned, beats the roll; my list position 1). Raises kept from the dealt hand: teletext's sacred cell grid (fixed columns and tabular figures so dates and prices align like a real board); cyclorama's labelled phases (verdict and loading states always carry a word, not only a colour); lexicon's guide words (the results header names the span in view: route, first and last departure date, row count). Seed key 132808f1.

SIGNATURE INTERACTION: when results arrive, the route, the winning price and the counter flip through characters and settle like split flaps (about 500ms, once per state change, static under prefers-reduced-motion). Motion otherwise limited to 150-200ms state feedback.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
