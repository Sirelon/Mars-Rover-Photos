# Decider rules

- **Posture**: solo. Smallest shippable slice, iterate. Ships to Play internal testing and TestFlight via the `store-release` skill; store promotion is manual.
- **Users**: consumers browsing NASA Mars rover photos and educational facts; no accounts, no user-generated data.
- **Collaborators**: none. NASA's public API and the Firestore facts collection are the only external contracts.
- **Stack constraints**: Compose Multiplatform, Android and iOS from `shared/src/commonMain`; ViewModel + state per `docs/ARCHITECTURE.md`.
- **Non-negotiables**: `App*` component family and tokens per `docs/DESIGN_SYSTEM.md`. `docs/ARCHITECTURE.md` is strong precedent, not law: when an option contradicts it, escalate and offer to update the doc.
- **Tie-breaker override**: position 2 is "fastest to ship", position 3 is "easiest to undo".
- **Copy**: English only, in `shared/src/commonMain/composeResources/values/strings.xml`. No translation, so `needs-translation` is never flagged.
