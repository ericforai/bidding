# Dashboard Workbench

This folder keeps the Workbench page split by responsibility.

- `Workbench.vue` is the page shell. It wires stores, router, lifecycle loading, and child components.
- `workbench-core.js` contains pure functions only: formatting, DTO mapping, role rules, and route targets.
- `useWorkbench*.js` files are application-service composables. They perform API orchestration and state writes.
- `components/` contains display components. They receive props and emit events, and do not access APIs, stores, or router.
- `styles/` contains Workbench CSS split into small files and imported by `styles/workbench-styles.js` so Vite can bundle them without CSS `@import` waterfalls.
- Empty/error states stay presentational; API composables expose state and the page shell wires retry actions.
- Clickable cards must be keyboard reachable with visible focus states before shipping new interactions.
- Metric cards should gracefully render missing comparisons (e.g. show friendly fallback instead of raw `--`) and keep values readable without layout breakage.

Keep every new source file under 300 lines. If a file approaches that limit, split by behavior before adding more code.
