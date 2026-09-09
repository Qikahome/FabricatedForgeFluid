# FabricatedForgeFluid (1.20.1)

A Forge-style fluid system for Fabric 1.20.1.
Forge 式流体体系在 Fabric 1.20.1 上的复刻库。

Built on Porting Lib's `FluidType` (`porting_lib:fluids`), porting Forge's fluid entity
interactions (pushing / swimming / drowning / extinguishing / fall-distance modifier)
and automatic render registration to Fabric. `FabricatedFluidType` / `FabricatedFlowingFluid`
help mods declare their own fluids.

以 Porting Lib 的 `FluidType`（`porting_lib:fluids`）为桥，把 Forge 的流体实体交互
（推动 / 游泳 / 溺水 / 灭火 / 落距修正）与客户端渲染自动注册移植到 Fabric，
并提供 `FabricatedFluidType` / `FabricatedFlowingFluid` 便于模组声明自己的流体。

## Features / 特性

- **FluidType-driven entity interactions**：`motionScale`（pushing）、`canSwim`（swimming）、
  `canDrownIn`（drowning）、`canExtinguish`（extinguishing）、`fallDistanceModifier`（fall distance）
  - **FluidType 驱动的实体交互**：`motionScale`（被动推挤）、`canSwim`（游泳）、
    `canDrownIn`（溺水）、`canExtinguish`（灭火）、`fallDistanceModifier`（落距修正）
- **Forge-consistent semantics, mod-friendly**：bridged via `tagToTypeMap`（fluid tag → FluidType），
  `isInWater()/isUnderWater()/updateSwimming()` etc. are extended at specific call sites instead of
  fully overridden — good cross-mod compatibility
  - **与 Forge 一致的判定语义，跨模组兼容**：通过 `tagToTypeMap`（fluid tag → FluidType）桥接，
    原版 `isInWater()/isUnderWater()/updateSwimming()` 等按需逐点扩展，非全量覆盖
- **Automatic render registration**：scans the fluid registry on client start and registers world
  fluid rendering for `FabricatedFluidType`（Forge-style auto-discovery），solving the Fabric
  baking-time texture timing problem
  - **自动渲染注册**：客户端启动时遍历流体注册表，对 `FabricatedFluidType` 自动注册世界流体渲染，
    解决 Fabric 烘焙期纹理时序问题
- **Forge-aligned flowing implementation**：`FabricatedFlowingFluid` provides source/flowing and a
  `Properties` builder（slopeFindDistance / levelDecreasePerBlock / tickRate etc.）
  - **对齐 Forge 的流动实现**：`FabricatedFlowingFluid` 提供 source/flowing、`Properties`
    （slopeFindDistance / levelDecreasePerBlock / tickRate 等）配置

## Dependencies / 依赖

| Item / 项 | Version / 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Java | 17 |
| Fabric Loader | >= 0.16.0 |
| Fabric API | 0.92.6+1.20.1 |
| Porting Lib | 2.3.13+1.20.1（jar-in-jar included / 整包内嵌） |
| Loom | 1.10.+ |

## Usage / 使用

### As a dependency / 作为依赖引入

**Maven（GitHub Packages，需 token）**

```groovy
repositories {
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/Qikahome/FabricatedForgeFluid"
        credentials {
            username = "Qikahome"              // 或 GITHUB_ACTOR
            password = "<GH_TOKEN>"            // 需要 read:packages 权限的 Personal Access Token
        }
    }
}

dependencies {
    modImplementation "qikahome:FabricatedForgeFluid-1.20.1:0.1.0"
}
```

**flatDir（本地 jar）**

```groovy
repositories {
    flatDir { dirs 'libs' }                    // 把构建产物 jar 放进 libs/
}

dependencies {
    modImplementation name: "FabricatedForgeFluid-1.20.1-0.1.0"
}
```

### Declaring a fluid / 声明流体

1. Extend `FabricatedFluidType`，pass the matching `TagKey<Fluid>` to the constructor
   （also register into `PortingLibFluids.FLUID_TYPES`），and make the fluid actually carry that tag
   （`data/<ns>/tags/fluid/*.json`）.
   继承 `FabricatedFluidType`，构造时传入对应的 `TagKey<Fluid>`（同时注册进
   `PortingLibFluids.FLUID_TYPES`），并让流体真实挂上该 tag（`data/<ns>/tags/fluid/*.json`）。
2. Create still/flowing fluids, `LiquidBlock` and a bucket via `FabricatedFlowingFluid.Properties`.
   用 `FabricatedFlowingFluid.Properties` 创建 still/flowing 流体、`LiquidBlock` 与桶。
3. On the client, provide textures/colour via `initializeClient(Consumer<FluidRenderHandler>)`；
   rendering is registered automatically. **Use the library's `FabricatedFluidRenderHandler`
   （not vanilla `SimpleFluidRenderHandler`）**: it pre-fills the texture sprites when the atlas is
   ready（before model baking），so models that need textures at bake time（e.g. a fluid bucket
   loader）won't get null.
   客户端通过 `initializeClient(Consumer<FluidRenderHandler>)` 提供贴图/颜色，渲染自动注册。
   **请使用库自带的 `FabricatedFluidRenderHandler`（而非原版 `SimpleFluidRenderHandler`）**：
   它会在 atlas 就绪（模型烘焙前）时预填充纹理 sprite，使依赖烘焙期纹理的模型
   （如流体桶 loader）不会拿到 null。

Example：`qikahome.fabricatedforgefluid.test.TestFluids`（registered only in dev environment）。
示例见 `qikahome.fabricatedforgefluid.test.TestFluids`（开发环境条件注册）。

## Branches / 分支

- `1.20.1`：this branch（this repository，Minecraft 1.20.1）
  本分支（Minecraft 1.20.1）

## License / 许可

LGPL-2.1. Parts are derived from MinecraftForge 1.20.1（LGPL-2.1），original copyrights retained.
LGPL-2.1。部分代码源自 MinecraftForge 1.20.1（LGPL-2.1），保留原版权声明。
