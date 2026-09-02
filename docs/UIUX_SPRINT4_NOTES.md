# PredIQ Android UI/UX Sprint 4

This sprint moves PredIQ Android from a generic sports feed toward an intelligence product.

## Product hierarchy
- Today starts with an intelligence briefing and ranks strong calls ahead of the broader assessed slate.
- Probability, confidence, risk and freshness are deliberately separated rather than collapsed into one number.
- Live prioritises promoted calls, source freshness and re-analysis/change context.
- Results keeps wins, losses and voids visible and frames accuracy as an auditable record.
- Explore exposes league intelligence as a first-class research surface alongside teams, players and squads.
- Match Intelligence presents the current call, evidence, failure conditions, market/competition context and re-analysis history as an evidence narrative.
- Prospective users may preview Today, Live, Results and Explore before signing in; backend access controls still protect full analysis.

## Guardrails
No backend/API contracts were removed or weakened. Existing sports-media work, Material 3 foundation and live intelligence states are preserved.

## Release verification
Sprint 4 source compile fixes have been applied. This commit intentionally re-triggers Android CI from a user-authored repository write so the final debug APK can be produced and verified before merge.
