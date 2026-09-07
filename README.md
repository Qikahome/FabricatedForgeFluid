# FabricatedForgeFluid (1.20.1)

Forge 式流体体系在 Fabric 1.20.1 上的复刻库。

以 Porting Lib 的 `FluidType`（`porting_lib:fluids`）为桥，把 Forge 的流体实体交互
（推动 / 游泳 / 溺水 / 灭火 / 落距修正）与客户端渲染自动注册移植到 Fabric，
并提供 `FabricatedFluidType` / `FabricatedFlowingFluid` 便于模组声明自己的流体。

## 特性

- **FluidType 驱动的实体交互**：`FluidType.motionScale`（被动推挤）、`canSwim`（游泳）、
  `canDrownIn`（溺水）、`canExtinguish`（灭火）、`fallDistanceModifier`（落距修正）
- **与 Forge 一致的判定语义**：通过 `tagToTypeMap`（fluid tag → FluidType）桥接，
  原版 `isInWater()/isUnderWater()/updateSwimming()` 等按需扩展，非全量覆盖，跨模组兼容性好
- **自动渲染注册**：客户端启动时遍历流体注册表，对 `FabricatedFluidType` 自动注册世界流体渲染
  （对应 Forge 的自动发现），解决 Fabric 烘焙期纹理时序问题
- **对齐 Forge 的流动实现**：`FabricatedFlowingFluid` 提供 source/flowing、`Properties`
  （slopeFindDistance / levelDecreasePerBlock / tickRate 等）配置

## 依赖

| 项 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Java | 17 |
| Fabric Loader | >= 0.16.0 |
| Fabric API | 0.92.6+1.20.1 |
| Porting Lib | 2.3.13+1.20.1（整包 jar-in-jar） |
| Loom | 1.10.+ |

## 使用

```groovy
// 依赖引入（发布后）
modImplementation "qikahome:FabricatedForgeFluid-1.20.1:0.1.0"
```

声明流体：

1. 继承 `FabricatedFluidType`，构造时传入对应的 `TagKey<Fluid>`（同时注册进
   `PortingLibFluids.FLUID_TYPES`），并让流体真实挂上该 tag（`data/<ns>/tags/fluids/*.json`）。
2. 用 `FabricatedFlowingFluid.Properties` 创建 still/flowing 流体、`LiquidBlock` 与桶。
3. 客户端通过 `initializeClient(Consumer<FluidRenderHandler>)` 提供贴图/颜色，渲染自动注册。
   请使用库自带的 `FabricatedFluidRenderHandler`（而非原版 `SimpleFluidRenderHandler`）：
   它会在 atlas 就绪（模型烘焙前）时预填充纹理 sprite，使依赖烘焙期纹理的模型
   （如流体桶 loader）不会拿到 null。

示例见 `dev.qikahome.fabricatedforgefluid.test.TestFluids`（开发环境条件注册）。

## 分支

- `1.20.1`：本分支（Forge 47.4 / Fabric，Minecraft 1.20.1）

## 许可

LGPL-2.1。部分代码源自 MinecraftForge 1.20.1（LGPL-2.1），保留原版权声明。
