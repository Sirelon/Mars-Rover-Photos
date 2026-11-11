# Desktop hot reload & the Compose Hot Reload MCP server

The desktop target is the fastest place to see and drive shared UI, and it is wired for
[Compose Hot Reload](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html): the
`org.jetbrains.compose.hot-reload` plugin is applied to `desktopApp`, and `.mcp.json` at the repo root
registers its MCP server (`compose-hot-reload`, backed by `./gradlew :desktopApp:hotMcpServer`) so an
agent can look at and interact with the running app instead of guessing from code. Use it whenever a
change touches Compose UI, navigation or ViewModel state that the desktop app can reach; it replaces
"build, install on a phone, describe what you think you'd see".

1. **Start the app once** and leave it running for the session: `./gradlew :desktopApp:hotRun`
   (blocking — run it as a background task; first run provisions a JetBrains Runtime via the foojay
   resolver, later runs start in seconds). The MCP server needs no running app up front: it waits and
   attaches when the window appears, and `status` flips to `connected: true`.
2. **Edit, then `reload`.** The `reload` tool recompiles and swaps the changed classes into the live
   process (`{"reloaded": true}`); when compilation fails, `status` carries the compiler output in
   `lastErrorDetails`. Batch related edits and reload once. Alternative: start with
   `hotRun --auto` and call `await_reload` after saving — the build watches the sources and reloads by
   itself (`status.buildContinuous` tells you which mode is active). Do not `restart` for UI edits;
   only when you changed `main`, DI wiring, or something a class swap cannot express. `restart` keeps
   the same mode, but the original `hotRun` Gradle invocation exits — the app lives on.
3. **Verify with `take_screenshot` and `get_semantic_tree`.** The screenshot is captured in-process
   from the Compose scene at the window's density (pass `save_to` to keep it as PR evidence), so it is
   correct even when the window is hidden behind other windows. The tree returns roles, text,
   `testTag`s, `actions` and `nodeId`s, with a separate root for each open dialog / sheet / popup. If a
   screenshot ever shows the desktop wallpaper, the runtime has fallen back to a screen grab — check
   that `compose-hot-reload` and `compose-multiplatform` in the catalog still satisfy the requirement
   recorded next to them (Hot Reload with `ComposeDesktopEntryPoint` capture + Compose 1.13+).
4. **Drive the UI** with `click` / `long_click` / `type_text` / `scroll` / `scroll_to_index`,
   targeting `nodeId`s whose `actions` contain `onClick`; `resize_window` exercises the adaptive
   breakpoints from `docs/DESIGN_SYSTEM.md` without a tablet; `reset_ui` returns to the initial
   composition.
5. **Before declaring a change working**, check `get_ui_error` (a composable threw while rendering;
   `status.uiErrorWindows` lists affected windows) and `get_logs` (`Logger.d` output and exceptions).

Desktop is the JVM target, so it exercises everything in `commonMain` plus `desktopMain` actuals;
Android-only (`androidMain`, `androidApp/`) and iOS-only code still needs a device. Firebase runs
against the desktop shim (Firestore reads are real; the Realtime Database URL is deactivated and logs
a one-line warning), and `BuildInfo.versionName` is `"unknown"`, so version-gated features
(What's New) will not trigger there. The hot-reload run does not go through `assembleDebug`, so the
debug build label (`AGENTS.md` § Debug build label) does not apply to it.
