## Project guidance

Read **[AGENTS.md](AGENTS.md)** first — layout, build commands, versioning, testing. It points to the
two prescriptive docs, both of which are binding:

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — layering, ViewModel + state, navigation, naming.
  Read before adding a screen, ViewModel, or domain model.
- **[docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md)** — tokens, the `App*` component family, motion.
  Read before writing or changing any Compose UI.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
