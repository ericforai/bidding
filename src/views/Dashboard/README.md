# Dashboard Workbench

This folder keeps the Workbench page split by responsibility.

- `Workbench.vue` is the page shell. It wires stores, router, lifecycle loading, and child components.
- `workbench-core.js` contains pure functions only: formatting, DTO mapping, role rules, and route targets.
- `workbench-quick-start-core.js` contains pure permission checks, form validation, and payload builders for the one-stop quick-start flows.
- `useWorkbench*.js` files are application-service composables. They perform API orchestration and state writes.
- `components/` contains display components. They receive props and emit events, and do not access APIs, stores, or router.
- `components/WorkbenchQuickStart.vue` is the Workbench quick-start surface for bid support, qualification/contract borrow, and bid expense requests. Its side effects stay in `useWorkbenchQuickStart.js`.
- `styles/` contains Workbench CSS split into small files and imported by `styles/workbench-styles.js` so Vite can bundle them without CSS `@import` waterfalls.
- `MetricCards.vue` renders responsive KPI cards; the grid must adapt to the sidebar-constrained dashboard width without wrapping labels, values, or comparison text into vertical fragments.
- Empty/error states stay presentational; API composables expose state and the page shell wires retry actions.
- Clickable cards must be keyboard reachable with visible focus states before shipping new interactions.

Keep every new source file under 300 lines. If a file approaches that limit, split by behavior before adding more code.
