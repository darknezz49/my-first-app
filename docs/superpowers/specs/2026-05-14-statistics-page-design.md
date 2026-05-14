# 统计分析二级界面 — 设计文档

> 归物本（GuiWuBen）新增统计分析功能

## 概述

在现有物品列表首页的基础上，新增一个"统计"Tab，提供三类图表分析。通过底部导航栏在首页和统计页之间切换，统计页内部用 Tab 滑动切换三个图表。

---

## 导航与路由

```
MainActivity
  └── GuiWuBenApp (Scaffold + BottomNavBar)
        ├── Tab "首页" → 现有 HomeScreen（route: home）
        └── Tab "统计" → StatScreen（route: stats，内嵌 HorizontalPager + TabRow）
              ├── Tab "月消费趋势" → StatTrendTab
              ├── Tab "价格占比"   → StatPieTab
              └── Tab "日均成本"   → StatDailyCostTab
```

**改动说明：**

- `GuiWuNavGraph.kt` 层面：新增 `stats` 路由以及三个统计子 Tab，外层包 Scaffold + NavigationBar
- `HomeScreen.kt`：去掉自身 Scaffold（仅保留内容部分），Scaffold 提升到 GuiWuBenApp 层
- 导航栏两个 item：首页（home icon）、统计（bar chart icon）
- 进入添加/编辑页时底部导航栏隐藏（由 NavHost 层级决定，add_item 路由在底部栏外层）

---

## 图表组件

### 图表库

使用 **Vico**（Compose-native 图表库）。若 Vico 不提供饼图，饼图用 Compose Canvas 手绘。

### 图表 1：月消费趋势

- **类型：** 柱状图 + 可选折线（消费数量）叠加
- **横轴：** 月份（yyyy-MM），仅显示有数据的月份范围
- **柱体：** 当月消费总金额（元）
- **折线：** 当月购买物品数量（第二条 y 轴）
- **Vico 组件：** `CartesianChartHost` + `ColumnCartesianLayer` + `LineCartesianLayer`
- **边界：** 只有 1 个月数据则单柱显示

### 图表 2：物品价格占比

- **类型：** 饼图 / 环形图
- **每个扇形：** 物品名称（标签）+ 购买价格（数值）
- **归并：** 物品 > 8 个时，价格最低的归并为"其他（X 件）"
- **边界：** 只有 1 个物品时显示单色完整扇形

### 图表 3：日均成本分布

- **类型：** 水平柱状图
- **横轴：** 日均成本（价格 / 已拥有天数）
- **纵轴标签：** 物品名称，按日均成本从高到低排列
- **颜色：** 按日均成本高低渐变（暖色→冷色）
- **边界：** 已拥有 0 天时，日均成本 = 价格本身；> 15 件只显示 Top 15

### 数据计算

- 在 StatScreen 层从 `dao.getAllItems()` Flow 拿到完整列表后，一次性计算三个数据集
- 通过参数传给三个子 Tab，子 Tab 只负责渲染
- 无需额外数据库查询

---

## 边界与空状态

| 场景 | 行为 |
|---|---|
| 物品列表为空 | 三个 Tab 各显示空状态提示 |
| 只有 1 个物品 | 所有图表正常渲染单数据点 |
| 物品 > 20 件 | 饼图归并 >8，日均分布取 Top 15，趋势图不受影响 |
| 新物品当月添加 | 已陪伴 0 天，日均成本 = 价格（避免除以 0） |

---

## 状态管理

- 不引入 ViewModel，延续现有 composable 级状态管理
- StatScreen 用 `remember` 缓存计算结果，随 items Flow 自动更新
- HorizontalPager 默认保留相邻页状态，无需额外处理

---

## 文件变更

```
新增:
  app/src/main/java/com/example/myfirstapp/ui/
  ├── StatScreen.kt           -- TabRow + HorizontalPager 壳
  ├── StatTrendTab.kt         -- 月消费趋势图表
  ├── StatPieTab.kt           -- 物品价格占比图表
  ├── StatDailyCostTab.kt     -- 日均成本分布图表

修改:
  app/build.gradle.kts                              -- 添加 Vico 依赖
  app/src/main/java/com/example/myfirstapp/
  ├── navigation/GuiWuNavGraph.kt   -- Scaffold + BottomNavBar + 统计路由
  └── ui/HomeScreen.kt              -- 去掉自身 Scaffold
```

---

## 技术依赖

- `com.patrykandpatrick.vico:compose-m3` — Vico Compose + Material 3 集成
- `com.google.accompanist:accompanist-pager`（如果需要 HorizontalPager 兼容）或使用 Compose Foundation 内置的 HorizontalPager
