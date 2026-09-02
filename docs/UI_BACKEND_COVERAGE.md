# PredIQ Android — Backend + Drive Reference Coverage

This file records the Android implementation decisions made while reconciling the current PredIQ backend with the PredIQ Google Drive UI reference corpus.

## Product rules

- Authentication remains unchanged; working Tuku/password handoff flows are preserved.
- Consumer-facing backend capability should be visible in Android when it improves the sports-intelligence experience.
- Internal-only `/sources` and `/performance` telemetry remains intentionally excluded from the consumer app because the backend explicitly restricts it to admin/analyst roles.
- Drive concepts are adapted to native Material 3 instead of copying desktop HTML literally.
- Missing backend support is never represented with fabricated data.

## Backend capability coverage

| Backend capability | Android status | Surface |
|---|---|---|
| Account / trial / subscription entitlements | Implemented | Account + app gating |
| Picks of day | Implemented | Today |
| Assessments, sport/country/competition/confidence/market filters | Implemented | Today + Filters sheet |
| Live intelligence with no-opportunity/service/cached handling | Implemented | Live |
| Results + accuracy dashboard | Implemented | Results |
| Match intelligence detail | Implemented | Match detail |
| League winner forecasts | Implemented | League winner screen |
| League intelligence profiles | Implemented | Explore > Teams |
| Team intelligence list/detail | Implemented | Explore > Teams |
| Team comparison | Implemented using real team profiles | Explore > Teams |
| Player intelligence | Implemented | Explore > Players |
| Squad depth intelligence | Implemented | Explore > Squad |
| Notification preferences | Implemented | Account > Notification sheet |
| League-specific alert following | **Implemented** using `/me/league-alerts` | Account > Notification sheet |
| Affiliate/referral dashboard | Implemented | Account |
| Payment capability + checkout | Implemented | Account / Payment sheet |
| Profile update | **Implemented** using `/me/profile` | Account > Profile sheet |
| Payment history | **Implemented** using `/payments/history` | Account > Payment history sheet |
| Device token registration/deactivation | **Implemented** using Firebase Messaging token lifecycle and `/devices/register` + `/devices/deactivate` | Background + Account notifications |
| Android notification runtime permission | **Implemented** for Android 13+ | Account > Notification sheet |
| Delivery contact update | Existing account phone/email remain authoritative; channel preferences are exposed in notifications | Account |
| Internal source/model performance telemetry | Intentionally excluded | Admin/analyst only |

## Drive reference coverage

The authoritative PredIQ Drive set contains mobile and desktop references including:

- `today_home_sub_tracker`
- `top_5_picks_mobile` / `top_5_picks_desktop_dashboard`
- `live_picks_mobile_tracker` / `live_picks_desktop_monitor`
- `live_desktop_dashboard`
- `results_accuracy_desktop`
- `match_intelligence_deep_dive`
- `league_winner_predictions` / `league_winner_predictions_desktop`
- `title_race_deep_dive_mobile` / `title_race_deep_dive_desktop`
- `league_predictions_alerts_mobile` / `league_predictions_alerts_desktop`
- `player_statistics_mobile` / `player_intelligence_dashboard_desktop`
- `squad_depth_analysis` / `squad_depth_analysis_desktop`
- `team_statistics_mobile` / `team_statistics_dashboard_desktop`
- `compare_teams_mobile_intelligence` / `compare_teams_selection` / `compare_teams_analysis`
- `alerts_notifications_settings_mobile` / `alerts_notifications_settings_desktop`

### Translation decisions

| Reference family | Android treatment |
|---|---|
| Today / subscription tracker | Merge into Today greeting, picks, trending assessments and Account entitlement progress rather than duplicate a web dashboard |
| Top 5 picks | Adapt pick hierarchy into native cards; use real `picks-of-day` and assessment confidence rather than static rank mockups |
| Live monitor/tracker | Adapt to Live with All Live / Best Opportunities, explicit refresh, last-known/service/no-opportunity states |
| Results accuracy | Adapt into Results summary metrics + graded prediction history |
| Match deep dive | Native match intelligence detail with rationale, watch-outs, freshness and share behavior |
| League winner/title race | Existing League Winner screen remains primary; league profile data is also exposed in Explore for deeper competition context |
| League prediction alerts | Implemented as persistent per-user competition following in the notification sheet using the backend league-alert contract |
| Player intelligence | Implemented as searchable player list + performance/coverage/source detail |
| Squad depth | Implemented as observed squad coverage, position groups and player evidence |
| Team statistics | Native team list/detail from `/intelligence/teams` and `/intelligence/team` |
| Compare teams | Native two-team comparison using real team intelligence profiles |
| Notification settings | Material 3 sheet maps push/WhatsApp/email, alert categories, Android runtime permission and followed competitions |
| Desktop navigation/sidebar patterns | Not copied literally; mapped to Android bottom navigation, sheets and progressive disclosure |

## Deployment note: push notifications

The Android application now contains the real Firebase Messaging token lifecycle and backend registration/deactivation contract. Token acquisition is deliberately guarded by the presence of a valid Firebase app configuration. Without deployment Firebase configuration, PredIQ does not invent or register a fake token. A production Firebase project/provider configuration and server-side push sender are deployment prerequisites for actual FCM delivery; the Android registration lifecycle itself is complete.

## Remaining intentional boundaries

1. Internal `/sources` and `/performance` model/source telemetry remains admin/analyst-only and is not exposed to consumer Android users.
2. Rich team comparison can expand when the backend publishes a stable typed comparison schema instead of generic profile JSON.
3. Bookmaker automation remains limited to official/authorised integrations; Android does not simulate unsupported bookmaker actions.
