一旦我所属的文件夹有所变化，请更新我。

# Mock Adapter 目录

这里是前端 demo 数据的唯一整理层，负责把 `mock.js` 和 `demoPersistence` 适配成页面可直接消费的最终结构。
业务页面、组件和 store 只能通过 adapter 间接消费演示数据，不能直连原始 mock 源。

| 文件 | 地位 | 功能 |
|------|------|------|
| `frontendDemo.js` | Mock adapter | 聚合 demo 用户、项目、自动化面板、移动卡片和本地持久化状态 |
