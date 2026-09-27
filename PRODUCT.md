# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users
Travellers in Spain with flexible dates. Two equally important situations:

- **Holidays with a window**: "sometime between July and September, about two weeks". They know where they want to go and roughly how long, not the exact days.
- **Weekend getaways**: short trips on fixed weekdays (e.g. Friday to Sunday) across a range of weekends.

Their job: find the cheapest round trip that fits, without trying date after date by hand in a regular flight search.

## Product Purpose
Torii takes a destination, a date window and a trip length (or a weekday pattern for getaways), checks every departure/return combination against live flight prices and returns the cheapest offers. Success is the user booking a trip that is cheaper than what they would have found by picking dates themselves, while spending as few paid lookups as possible.

## Positioning
Torii scans every date combination in the window and tells you whether the price is actually good. Two halves of the same claim:

1. **It scans the dates for you**: the user never fixes dates; Torii explores the whole window (or every matching weekend).
2. **It gives a verdict**: each offer carries Google's price verdict (low / typical / high with the usual range) and the route has a price history built from real searches.

## Operating Context
- Search form: origin, destination, holiday window, stay length plus flexibility, max stops, number of offers, precision and optional budget. Getaway mode replaces length/flexibility with a departure and return weekday.
- A live estimate of how many lookups the search will cost is shown before searching; every lookup is a paid API call.
- Results: offer cards (airline, outbound/return times and dates, stops and stopover airports, round-trip total price, booking link, price verdict), a route map and a 7/30 day price history chart.
- Accounts: sign up / log in (JWT), "recent searches" with one-click repeat, monthly lookup quota shown in the header. Searching requires an account.
- Plans page: FREE (30 lookups/month), PRO and BUSINESS are shown but **payments are not live**: plan changes are disabled and every account is on FREE.

## Capabilities and Constraints
- Precision levels: FAST (every 3 days), BALANCED (every 2 days), EXHAUSTIVE (every day). The FREE plan only includes FAST.
- Prices are always round-trip totals, per traveller, in EUR.
- Data sources: FlightPowers (Google Flights), SerpApi, FlightAPI.io, Amadeus, with failover; only FlightPowers provides the price verdict.
- Frontend: React 19 + TypeScript + Vite + Tailwind v4, Untitled UI / React Aria components, TanStack Query, Recharts, react-simple-maps. Deployed on Vercel; backend on Railway.
- Undecided: payment gateway, custom domain, final plan prices.

## Brand Commitments
- Name: **Torii**.
- All user-facing copy in Spanish (Spain).

## Evidence on Hand
- Real functionality only: live prices, the lookup estimator, price verdicts, the price history built from real searches.
- **No** users, testimonials, reviews, press, partner logos, savings statistics or ratings exist. Do not fabricate any of them, and do not show popularity labels ("most popular") or discounts for plans that cannot be bought.

## Product Principles
1. **Every lookup costs money**: always show what a search will cost before it runs, and never hide the quota.
2. **The price is the answer**: the cheapest offer and whether it's a good price must be readable at a glance.
3. **Flexibility is the input**: the form speaks in holidays and weekends, not in API parameters.
4. **Only true claims**: no fake social proof, no controls that do nothing, no plans that can't be bought presented as available.

## Accessibility & Inclusion
WCAG 2.1 AA as the floor: keyboard access and visible focus (React Aria), AA contrast, labelled inputs, reduced-motion support.
