# PredIQ UI V2 — Implementation Map

## 1. Existing Android Architecture

- **Presentation Layer:**
    - `MainActivity`: Entry point, handles edge-to-edge, system bars, and WebSocket lifecycle.
    - `PrediqContractApp`: Main navigation host for the "Contract" UI (Current UI).
    - `PrediqContractViewModel`: Manages state for V2 and V3 API interactions.
    - `PrediqViewModel`: Handles Authentication, Tuku identity, and Profile/Account actions.
    - `Compose`: Material 3 based components, with custom colors and typography defined in `ui/theme`.

- **Data Layer:**
    - `PrediqApi`: V1 REST client for Auth, Me, Picks, Payments, Intelligence.
    - `V2Api`: REST client for `/api/v2` endpoints (Today, Live, Results, Research, Follows).
    - `V3Api`: REST client for `/api/v3/intelligence` (Slate, Odds Builder, Event Detail).
    - `PrediqRepository`: Orchestrates data flow for common tasks.
    - `SessionStore`: Preferences DataStore for JWT tokens and Tuku handoff.
    - `PrediqLiveStream`: OkHttp WebSocket wrapper for live event updates.

- **Integrations:**
    - `Tuku Core`: Identity and auth provider.
    - `Firebase`: FCM for push notifications (`PushRegistration`).
    - `Coil`: Image loading.

## 2. Backend Contracts Actually Available

### V2 API (`/api/v2`)
- `today`: `V2TodayResponse` (Briefing, Changes, Top Picks, Waiting, Upcoming, Filters).
- `live`: `V2LiveResponse` (Summary, Following, Opportunities, Changes, Games, Filters).
- `predictions/{id}`: `V2PredictionDetail` (Decision, Outlook, Alternatives, Timeline, Evidence).
- `results`: `V2ResultsFeed` (Decision cards with result outcomes).
- `results/summary`: `V2ResultsSummary` (Record totals, Calibration, Market/Sport/Competition slices).
- `research`: `V2ResearchResponse` (Teams in picks, Leagues, Players to watch).
- `me`: `V2AccountResponse` (Profile, Membership, Following summary).
- `follows`: `V2FollowsResponse` (List of followed entities and alert preferences).
- `notifications`: `V2NotificationSettings` (Push, Email, WhatsApp toggles and alert categories).

### V3 API (`/api/v3/intelligence`)
- `slate`: `V3SlateResponse` (V3 cards for Odds Builder).
- `events/{id}`: `V3EventDetail` (Market groups, Alternatives, Evidence, Why/Watch-outs).
- `tickets/build`: `V3TicketResponse` (Combined odds, Risk profile, Legs).
- `tickets/recalculate`: Updated `V3TicketResponse`.
- `tickets`: `V3SavedTicketsResponse`.

### Media API (`/api/v1/media`)
- `catalog`: List of entities with `optimized_image_url`.
- `entity-image`: Resolve image by type/name/sport.

## 3. Screens Currently Supported (Legacy UI)

- **Main Navigation (Contract UI):** Today, Live, Builder, Tickets, Account.
- **Detail Screens:** Prediction Detail, Live Match Detail, Result Review, Team Detail, Player Detail, Competition Detail, V3 Event Detail.
- **Auxiliary:** Search, Following, Notifications Preferences, Inbox, Upcoming, Evidence Sources, Team Compare, Profile, Plans, Payments, Referral, Saved Odds, Performance Breakdown.

## 4. Data Available for Each Screen (UI V2 Mapping)

| V2 Destination | Backend Source | Key Data Points |
| :--- | :--- | :--- |
| **Today** | `api/v2/today` | Briefing metrics, Top Picks (Decision Cards), Upcoming games. |
| **Live** | `api/v2/live` | Live Summary, Opportunities, Games in progress, Score updates. |
| **Build** | `api/v3/intelligence/slate` | Event cards, Choices, Probabilities, Why/Why-not signals. |
| **Tickets** | `api/v3/intelligence/tickets` | Saved tickets, Ticket legs, Combined odds, Status. |
| **Me** | `api/v2/me` | User profile, Membership status, Following stats, Referral summary. |

## 5. Missing Frontend Mappings

- `V2DecisionCard` uses `JsonObject` for `reasons`, `integrity`, `teams`, `lineup` in `V2PredictionDetail`. These need structured mapping if UI V2 requires specific breakdown.
- `V3SlateCard` `context` and `why` lists are present but usage is minimal in current UI.
- `V2Freshness` state labels/colors mapping in Android is hardcoded in some places.

## 6. Media Data Currently Exposed

- `optimized_image_url`: Optimized WebP/SVG assets.
- `image_kind`: `logo`, `headshot`.
- `rights_status`: Used by backend to filter blocked assets.

## 7. Backend Fields Currently Ignored by Android

- `V2ClosingMarket`: `clv_probability`, `clv_price` (Available in DTO but rarely shown).
- `V2Alternative`: `rank` (Available in DTO).
- `V3Evidence`: `source_reliability`, `context_coverage` (Available in DTO).
- `V2ExpectedGoals`: `home`, `away` (Sometimes null, ignored if so).

## 8. Navigation Routes that Already Work

- All routes in `PrediqContractApp.kt` are functional and should be re-mapped to the new Shell where appropriate.

## 9. Existing Functionality that Must Be Preserved

- **Authentication:** Tuku login/register/refresh.
- **WebSocket:** Live score and status updates.
- **Push Notifications:** Firebase registration and preference management.
- **Odds Builder:** Recalculation logic and saved tickets.
- **Account Management:** Profile updates, Payment history (Mobile Money).

## 10. Components that May Safely be Replaced

- All `ui/contract/` screens (Legacy "Contract" UI).
- `MainContractTabs` (Old Shell).
- `PrediqHeader`, `WhiteCard`, `BrightCard`, `InfoDark` (Legacy components).

> [!IMPORTANT]
> **Implementation Guardrails:**
> - Keep `PrediqContractApp` and `PrediqAppV2` coexist for now.
> - New UI lives in `com.getprediq.app.ui.v2`.
> - Do not change Backend `api_v2` or `api_v3` contracts.
