# DGH Compatibility Bridge (DGH 模组兼容桥)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![NeoForge 21.1.230](https://img.shields.io/badge/NeoForge-21.1.230-orange.svg)](https://neoforged.net/)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT%20%2F%20Custom-lightgrey.svg)](#license--许可证)

[English](#english) | [中文说明](#中文说明)

---

## English

### Introduction
**DGH Compatibility Bridge** (`dgh_cybernetics_compat`) is a Minecraft NeoForge (1.21.1) integration mod designed to seamlessly bridge **DGH (Doctor, General Hospital)** medical mechanics with multiple popular gameplay, cyberware, physics, and interaction mods.

It provides synchronized body part damage, limb amputation/transplantation, cyberware surgery integration, stretcher transportation, stasis/corpse management, physics ragdolls, and automated medical record keeping.

---

### Features & Multi-Language Support

#### 🌐 Language Support
- **Full Localization**: Complete **English (`en_us`)** and **Simplified Chinese (`zh_cn`)** translation for all UI, Keybindings, System Messages, Diagnostics, Scanner Reports, and Config Options.
- **Dedicated Server Friendly**: UI components and chat notifications automatically adapt to each player's client language using Minecraft's `Component.translatable` framework. Bedside medical record books (written item stacks) use uniform text generation to prevent multi-player encoding conflicts.

---

### Key Features & Integrations

#### 1. 🏥 Cyberware & Surgical Bed Integration (Create Cybernetics)
- **Bedside Cyberware Management**: Surgery bed UI allows direct inspection and installation/removal of cybernetic limbs, neural implants, organs, and internal chips.
- **Anatomy & Amputation Sync**: Cybernetics installation respects DGH body part integrity (head, torso, limbs). Amputated limbs dynamically disable associated cyberware slots.
- **Cinematic Surgery**: Supports cinematic interaction modes during surgery and medical checkups.

#### 2. 🚑 Emergency Medical Equipment & Scanners
- **Bedside CPR & Ventilation**: Interactive slots for Emergency Resuscitation Bags and Automatic CPR units with real-time audio and status sync.
- **Scanners & Diagnostics**: 
  - **Health Scanner**: Switchable detailed vs. rough symptom checkup.
  - **Blood Scanner**: Detailed blood status reports (% blood volume, infection/poisoning).
- **Bedside Medical Chart**: Automatically records treatment procedures, medical items used, and symptom logs into a writable Book and Quill.

#### 3. 🛌 Stretcher Transportation & Stasis Bag System
- **Stretcher Operations**: Securely transfer downed or unconscious patients onto stretchers and operating beds.
- **Stasis Bag**: Encapsulate downed players for safe long-distance transport. Wrapped corpses can undergo bed surgery directly; living players can be unwrapped safely on surgical beds.

#### 4. 🪓 Corpse & Inventory Integration (Player Corpses)
- **Corpse Relic Inspection**: Default key `F` allows players to inspect corpse items and DGH health snapshots.
- **Unified Corpse Data**: Integrates with *Player Corpses* natively without generating duplicate physical entities. Retains DGH health condition snapshots upon player death and respawn.
- **Carry On Compatibility**: Shift + Right-click empty-handed to pick up and carry downed patients or corpse bags.

#### 5. 🦴 Physics & Ragdolls (Ragdollified / Zarith)
- **Living Ragdolls**: Physics ragdoll body sync for downed entities while retaining real DGH health data.
- **Dynamic Hitbox Matching**: Localized damage detection calculated against rotated ragdoll limb hitboxes.
- **Point Blank Gunfire Integration**: Optional localized penetrating gunshot wounds and arm injury impact on gun handling/reloading.

---

### Mod Compatibility List

| Mod | Compatibility Description |
|---|---|
| **DGH** | Core medical system, body part vitality, wound & bleeding mechanics. |
| **Create Cybernetics** | Surgery bed integration, limb/organ replacement, humanity & integrity stats. |
| **Player Corpses** | Native corpse storage, DGH health snapshot persistence, relic inspection (`F`). |
| **Carry On** | Hand-carrying downed players and wrapped corpse bags. |
| **Ragdollified & Zarith** | Physics ragdoll sync for downed players and localized limb hitboxes. |
| **Point Blank** | Penetrating gunshot wounds & arm damage affecting firearm usage. |
| **Cinematic Interaction** | Immersive camera & interaction angles during medical operations. |
| **Cloth Config** | In-game configuration GUI with search and input validation. |

---

### Installation & Configuration

1. Place `dgh-compat-2.4.55.jar` into your Minecraft `mods` folder.
2. Ensure required base mods (**DGH**, **Cloth Config**) are installed.
3. Optional companion mods (**Create Cybernetics**, **Player Corpses**, **Carry On**, **Ragdollified**, **Point Blank**) will be auto-detected and bridged if present.
4. In-game configuration can be accessed via Mod Menu or Cloth Config (`DGH Integration Settings`).

---

### Building from Source

#### Requirements
- **JDK 21**
- **Gradle 8.x** (or wrapper `./gradlew`)

#### Build Command
```bash
# Windows
build-local.cmd

# Linux / macOS
./gradlew clean assemble sourceBundle
```
Output artifacts will be generated in `build/libs/` and `build/distributions/`.

---

### License
This project is released under the open-source license specified in the source code repository.

---

## 中文说明

### 简介
**DGH 模组兼容桥** (`dgh_cybernetics_compat`) 是一款针对 Minecraft NeoForge (1.21.1) 开发的综合医疗与机制兼容模组。

本模组旨在将 **DGH（医生，综合医院）** 核心的生理损伤、部位生命值、伤口自愈及治疗体系，与义体改装、物理布娃娃、玩家尸体、担架运输及枪械失能等多种玩法模组无缝连接。

---

### 语言与国际化支持

#### 🌐 双语完全支持
- **完整本地化**：模组内所有 **GUI 界面、快捷键、系统提示、设备诊断、扫描仪报告、手术说明及 Cloth Config 配置项** 均已提供完整的 **简体中文 (`zh_cn`)** 与 **英文 (`en_us`)** 双语翻译。
- **服务端/客户端感知**：客户端提示信息使用 `Component.translatable` 动态加载语言包，无论在单人还是多人在线服务器中，均能根据各玩家客户端的语言设置自动呈现对应文本。床头病历本文本采用统一格式化生成，避免多人在线时的编码兼容问题。

---

### 核心功能与兼容特性

#### 1. 🏥 义体改装与手术床联动 (Create Cybernetics)
- **手术床交互**：在义体手术床上可直接查看并装卸患者的头部、躯干、四肢义肢及各类神经/内脏芯片。
- **肢体缺失联动**：义体槽位严格遵循 DGH 的部位存续状态；部位截肢或缺失时将自动禁用并挂起对应义体。
- **电影化视角**：支持在诊疗和手术过程中启用电影化交互视角。

#### 2. 🚑 急救设备与医疗病历
- **床头急救设备**：手术床可挂载急救呼吸气囊与自动心肺复苏器，具备实时的音效与生理状态同步。
- **双扫描仪支持**：
  - **健康扫描仪**：背包持有时可切换查看患者的精准生理数据或粗略症状。
  - **血液扫描仪**：实时生成患者的血容比、失血量及血液毒素/感染报告。
- **病历本记录**：空手/持有书与笔可挂载床头病历，自动追踪追加治疗步骤与药物使用记录。

#### 3. 🛌 担架运输与停滞袋系统
- **担架转移**：支持将倒地伤员、濒死患者平稳转移至担架或手术床上。
- **停滞袋包裹**：倒地伤员可使用停滞袋打包运输。封袋尸体可在床上直接进行手术；活体伤员可安全放置于手术床上解包。

#### 4. 🪓 尸体与遗物管理 (Player Corpses)
- **按键查看遗物**：默认按 `F` 键（可在按键绑定中修改）直接查看遗物及死亡时刻的 DGH 健康快照。
- **原生尸体数据融合**：与 *Player Corpses* 原生尸体深度对接，保存并跟随死亡快照，不额外生成冗余实体。
- **Carry On 搬运**：空手潜行右键可使用 Carry On 抱起未封袋的倒地伤员或封袋尸体。

#### 5. 🦴 物理布娃娃与受击判定 (Ragdollified / Zarith)
- **活体布娃娃同步**：倒地伤员启用物理身体，保留真实 DGH 生理数据与受击包围盒。
- **旋转肢体判定**：DGH 伤害判定优先匹配服务端实时计算的物理旋转肢体箱。
- **Point Blank 枪械失能**：手臂受伤/截肢将直接影响 Point Blank 枪械的持枪稳定性与换弹速度；支持按弹着点生成部位贯穿伤。

---

### 模组兼容列表

| 模组名称 | 兼容与联动说明 |
|---|---|
| **DGH** | 提供核心生理健康、器官/肢体损伤、出血自愈等基础医疗逻辑。 |
| **Create Cybernetics** | 提供义体手术台/床联动、器官/义肢替换、人性值与数据完整性显示。 |
| **Player Corpses** | 原生尸体容器对接、DGH 健康快照持久化、`F` 键遗物查看。 |
| **Carry On** | 支持双手抱起倒地玩家及已封袋的尸体。 |
| **Ragdollified & Zarith** | 倒地实体物理布娃娃同步、物理旋转肢体精准受击箱。 |
| **Point Blank** | 贯穿伤弹着点判定、手臂失能影响持枪与换弹。 |
| **Cinematic Interaction** | 诊疗及手术过程中的电影化沉浸视角。 |
| **Cloth Config** | 提供可视化配置菜单，支持实时搜索与数值合法性校验。 |

---

### 安装与配置

1. 将 `dgh-compat-2.4.55.jar` 放入 Minecraft 的 `mods` 文件夹中。
2. 确保前置模组（**DGH**、**Cloth Config**）已正确安装。
3. 可选联动模组（**Create Cybernetics**、**Player Corpses**、**Carry On**、**Ragdollified**、**Point Blank** 等）在安装后会被模组自动识别并启用对应兼容逻辑。
4. 游戏内可通过 Mod 列表或 Cloth Config 打开配置菜单（`DGH 模组兼容配置`）。

---

### 源码构建

#### 环境要求
- **JDK 21**
- **Gradle 8.x**（或使用工程自带的 `./gradlew`）

#### 构建命令
```bash
# Windows
build-local.cmd

# Linux / macOS
./gradlew clean assemble sourceBundle
```
构建产物将保存在 `build/libs/` 与 `build/distributions/` 目录下。

---

### 许可证
本开源项目遵循源代码仓库中所附带的许可证协议。
