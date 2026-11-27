# 电脑维修工作台（暂名）

一款面向安卓手机的横屏 2D 像素风电脑维修游戏。长期玩法是接单、根据现象排查故障、维修或更换配件，再开机验收、交付。**截至 2026-11-27，可玩的版本是自由装机工作台**。玩家从空台面开始，在平板商城买机箱和配件，组装、接线、安装系统并开机查看配置。[打开网页试玩（使用获授权的账号登录）](https://game-draft-test-261119.yanchn19910704.chatgpt.site/)

## 第一次试玩

1. 点「开始游戏」，打开平板商城。开局有 ¥50,000 测试资金，43 种商品各买一件合计 ¥24,999。
2. 可用 AIR 903、PRIME B560M-K、i5-11400F、原装散热器、一条 DDR4、GT 1030、550 W 电源、主板供电线和一块 KC3000 搭一台测试机。买完直接进入物品栏。
3. 把机箱拖上工作台，再拖入主板、CPU、内存、显卡、电源、主板供电线和 M.2 硬盘。选中硅脂点 CPU，再装散热器。用螺丝刀长按空孔固定部件，最后装侧板。这张 GT 1030 由插槽供电，M.2 硬盘直接插主板。
4. 点「开机检测」，关机状态插入 U 盘，再按电源键并选择 U 盘启动。选好目标磁盘，等待安装完成，从硬盘重启。在桌面打开「我的电脑」中的移动磁盘，运行配置检测即可核对这台机器，也可以打开「硬件跑分」测试整机。
5. 用悬浮菜单把进度保存到三个本地槽位中的一个。想换一台机器继续组装时，回工作台点「收纳机箱」，以后可从物品栏拖回。

## 项目要求与协作约定

- 项目名称待定，暂用 `game-draft` 作为目录名。
- 游戏为 2D 平面维修模拟游戏。
- 联机、登录、云存档、震动反馈、系统通知、语音聊天、相机、定位和照片导入暂无计划。按目前的功能范围，安卓端可做到零次运行时权限请求。
- 最终交付为可在安卓手机上安装运行的游戏 App。
- 开发期间，每完成一个版本，就部署到可远程访问的网站。用户打开测试链接即可在浏览器内运行游戏，整个测试流程在网页上完成。
- 首个测试版本交付网页链接和安卓 APK。后续迭代默认交付网页链接，用户提出需要时再打包并发送 APK。
- 用户集中讲需求时，先听取并记录。用户主动提问时，当场回答。等用户讲完后，再集中确认尚未明确的需求。

## 迭代进展

| 日期 | 已完成的里程碑 |
| --- | --- |
| 11 月 19 日 | 首版在「测试／开始游戏」与「进行中／返回主页」之间切换，用来验证 Kotlin 安卓和网页双端交付，提供 0.1.0 APK。 |
| 11 月 20–21 日 | 建立像素工作台、部件图层、长按拆卸、螺丝和拖放安装。试做接单与交单，客户只描述症状，交付前需要手动开机。 |
| 11 月 23–24 日 | 加入手动电源、重启、耐久、散热与硅脂、U 盘装系统、虚拟桌面、配置检测、平板商城和本地存档。测试订单先用显卡线松动，后改为重装系统。 |
| 11 月 24–25 日 | 按设计决定移除全部测试订单与预装机，改为空工作台和五款可购买机箱。实现半成品整机收纳、直接到货和手动仓储，修复打开商城后拖放机箱失败的问题。 |
| 11 月 25 日 | 六款主板支持独立插槽。硬盘、内存、显卡、电源各增加五款。商城全部定价，加入钱包、旧档兼容和不同配置的供电判定。 |
| 11 月 27 日 | U 盘新增硬件跑分，支持四阶段负载演示、综合分和逐设备结果。平板新增维修台，可维修或合并物品栏里的单件配件。 |

自由装机配件扩充对应 Git 提交 `263c8c5`。安卓构建配置目前为 `0.1.2`（versionCode 3）。历史 APK 早于最新网页的商城与配件扩充。

## 当前商城

| 分类 | 款数 | 测试价 | 可选配置 |
| --- | ---: | --- | --- |
| 机箱 | 5 | ¥399–999 | AIR 903 BASE、H5 Flow、North、Pure Base 500、FRAME 4000D |
| 主板 | 6 | ¥329–1,499 | H510、B560、H570、Z590，详见下方插槽表 |
| CPU / 散热 | 各 1 | ¥499 / ¥59 | i5-11400F、LGA1200 原装风冷 |
| 内存 | 7 | ¥49–429 | 单条 DDR4，4–32 GB，2666–3600 MT/s |
| 显卡 | 6 | ¥329–3,299 | GT 1030、GTX 1660 SUPER、RX 6600、RTX 3060、4060、4070 |
| 电源 | 6 | ¥239–999 | 450、550、650、750、850、1000 W |
| 硬盘 | 8 | ¥169–999 | 250 GB–4 TB，包含 M.2 NVMe、2.5 英寸 SATA SSD 和机械硬盘 |
| 线材 / 工具 | 3 / 0 | ¥15–25 / — | 主板供电线、显卡供电线、SATA 硬盘线组。初始工具栏已有螺丝刀和硅脂。 |

所有商品的逐项价格、型号和参数以[商城目录](app/src/commonMain/kotlin/com/gamedraft/preview/model/WorkshopShop.kt)为准。

## 未完成工作与待定规则

| 方向 | 已确定的需求 | 待实现或决定 |
| --- | --- | --- |
| 正式订单与交付 | 客户只说症状。交单页用短目标，未完成为绿色，完成后灰字加删除线。交付前由玩家实际开机检测。 | 重建正式订单池、收机与交付流程。历史目标有「配件完好」「正常启动」，重装订单还有「系统已重装」。自由装机版当前没有订单入口。 |
| 原机配置验收 | 记录接单时原配置。交付要零件齐全、无坏件且无降级。硬盘允许同品牌替换，容量和耐久均不得低于原盘。 | 为正式订单保存原机基准，明确其他类别如何比较性能，并验证换件后的交付。 |
| 故障排查与耐久 | 用黑屏、启动停滞等现象和玩家操作提供线索。健康订单配件曾约定为 50–100 耐久，商城新品为 100，低于 50 视为损坏。 | 扩充内存、硬盘等独立故障表现，设计低耐久的进一步作用和更多检测工具。故障原因由玩家判断，避免直接播报。 |
| 维修与经营 | 前期通过换件赚取有限利润，后期可修理零部件。平板计划支持出售配件和升级工具。 | 基础维修与合并已实现。解锁条件、维修台升级、工具商品、回收价格、工单报酬和正式经济平衡待定。¥50,000 是测试开局资金。 |
| 硬件与美术 | 参考现实型号、接口、容量、插槽和外观，采用原创像素图形。 | 继续丰富 CPU、散热及其他平台和兼容规则。当前仅有一种 CPU，供电按估算负载判断，内存速率按当前平台上限简化。 |
| 发布与验证 | 网页链接用于频繁测试，安卓用于最终交付。 | 项目正式名称待定。最新配件版仍需安卓真机回归，正式发行版本、签名与发布渠道待定。 |

## 核心玩法

玩家接下客户的订单并收到旧设备，排查故障，按客户要求修理坏件或更换零件，完成后交付设备。

前期靠更换坏零件完成工单，利润一般。后期解锁维修坏件的能力，解锁条件和玩法细节待定。

首个可玩版本以台式电脑维修为方向，从简单的机箱和配件组合开始。工作台以打开侧板的机箱平面图展示主板和各个零件的安装位置。

后续逐步扩充可搭配的配件，包括不同型号的显卡（如 4090、1080）、不同接口和容量的硬盘，以及不同容量和频率的内存。

## 美术方向

采用像素风，以第 4 张竖版俯视工作台效果图为参考，实际游玩界面为横屏。[像素风工作台效果图](app/src/commonMain/composeResources/drawable/workbench_assembled.png)

现有配件沿用像素素材。五款机箱参考现实型号绘制，分别采用黑色网孔、白色玻璃侧板、炭黑配胡桃木格栅、深灰封闭面板和白色三角孔面板。

## 游戏菜单与存档

- 全局悬浮按钮可拖动。点击按钮或按 Esc 打开游戏菜单，可继续、返回主界面、保存、载入或退出。安卓返回键同样可开关菜单。
- 三个本地存档位分别保存已装和拆下的机箱及配件、螺丝、耐久、系统安装结果及仓库物品。打开菜单暂停计时，读档后回到工作台，主机关机。
- 网页存档属于当前浏览器和试玩站点，清理该站点的数据会清除存档。安卓使用应用内 SharedPreferences，两端各自保存。

## 工作台与拆装

- 开始游戏后进入空工作台。先从物品栏拖入购买的机箱，再按安装位装入配件。所有机箱支持本批 ATX 和 mATX 主板。内存、显卡和 M.2 固态硬盘的安装位由主板型号决定。放置机箱后，同型号侧板在物品栏供安装。机箱清空后可长按取回。
- 零件按安装位置和前后遮挡关系展示。拆下机箱盖后才能接触内部，拆下散热器后才能接触处理器。
- 横屏工作台右侧分设工具栏与物品栏。工具栏横向滚动，每格为方形图标，第一格放螺丝刀。选中后，高亮可操作的螺丝和空孔。用初始螺丝刀长按每颗螺丝或空孔约 0.9 秒，即可卸下或装回。
- 拆装遵循遮挡和前置关系，螺丝和部件共用以触点为圆心的大进度环。提前松开即取消。完成后满环变绿，继续按住时在原位放大一次再还原。抬起发起长按的手指时，圆环立即消失。部件的螺丝全部拆下后，长按约 0.65 秒取下部件。拆下的部件自动进入物品栏。
- 从物品栏向台面轻拖即可拿起机箱或配件，上下滑动仍可浏览物品栏。零件对准兼容的安装提示框后松手，零件吸附到原位。带螺丝的部件就位后，玩家逐颗装回螺丝。拖放未完成时，零件保留在物品栏。
- 空工作台的台面可接收机箱。点击台面的「收纳机箱」可把当前机箱连同已装配件、螺丝、侧板、磁盘系统和启动盘选择放进物品栏。收纳的机箱可随存档保存，拖回空工作台时恢复为关机状态。

- 工具栏只显示图标，物品栏与两格工具的总宽一致，每行放两个零件，超出可视高度时上下滑动。空物品栏保留空位。
- 两根同规格内存条使用同一外观、名称，均可装入任一空闲内存槽。
- CPU 散热器可单独购买、拆装，并提供散热功能。
- 每个零部件都有 0～100 的耐久。商城新品耐久为 100。拆下后可在物品栏查看数值，低于 50 视为损坏。

## 主板

本批使用 LGA1200 和 DDR4，规格按当前的第十一代酷睿处理器计算。显卡位按物理全长 PCIe 插槽统计，M.2 仅计可安装 SSD 的插槽。每个位置独立记录部件、固定螺丝和硬盘系统。

| 主板 | 全长 PCIe | M.2 SSD | DDR4 DIMM | 厂商规格 |
| --- | ---: | ---: | ---: | --- |
| 技嘉 Z590 AORUS MASTER rev. 1.0 | 3 | 3 | 4 | [技嘉](https://www.gigabyte.com/Motherboard/Z590-AORUS-MASTER-rev-10/sp) |
| 华硕 TUF GAMING Z590-PLUS WIFI | 2 | 3 | 4 | [华硕](https://www.asus.com/motherboards-components/motherboards/tuf-gaming/tuf-gaming-z590-plus-wifi/techspec/) |
| 华擎 H570 Steel Legend | 2 | 3 | 4 | [华擎](https://www.asrock.com/mb/Intel/H570%20Steel%20Legend/index.asp#Specification) |
| 微星 B560M PRO-VDH | 1 | 2 | 4 | [微星](https://www.msi.com/Motherboard/B560M-PRO-VDH/Specification) |
| 华硕 PRIME B560M-K | 1 | 2 | 2 | [华硕](https://www.asus.com/motherboards-components/motherboards/prime/prime-b560m-k/techspec/) |
| 华硕 PRIME H510M-K | 1 | 1 | 2 | [华硕](https://www.asus.com/motherboards-components/motherboards/prime/prime-h510m-k/techspec/) |

## 平板与商城

- 右上角的平板入口在「开机检测」或「工作台」右侧，主界面、工作台、检测及菜单均可打开。当前自由装机版提供商城、仓库和维修台。
- 商城包含机箱、主板、CPU、散热、内存、显卡、电源、硬盘、线材和工具分类。商城商品使用「类别-厂商-系列或接口-型号及规格」命名。商品标注游戏内测试价。新开局有 ¥50,000，足够买下各型号一件并搭配多套整机。购买从余额扣款后直接入物品栏，余额不足时购买按钮置灰。工具分类暂空。余额随存档保存，早期无余额字段的存档读入时获得测试资金。在仓库页面可逐件存入或取出，收纳整机、配件耐久、硅脂和系统状态随物品转移。旧存档中待签收的配件在读档时自动进入物品栏。
- 空工作台也显示「开机检测」入口。无机箱时，电源、重启和插入 U 盘按钮置灰。放置机箱后可手动尝试开机，主机依照已安装硬件显示启动过程和对应故障现象。

- 显示器页面右上角的「工作台」和工作台页面的「开机检测」共用同一按钮位置。显示器页面的电源键控制开关机，通电时发光。重启键重新开始启动过程。启动现象显示在屏幕内，工作台省略螺丝数量提示。
- 工具栏第二格是硅脂。拆下 CPU 散热器时，CPU 上的硅脂失效。选中硅脂后可点物品栏内的 CPU 或露出的 CPU 重涂，台面上的 CPU 会显示明亮的硅脂斑。散热器缺失或损坏时通电 5 秒后关机，散热器完好但 CPU 缺硅脂时通电 10 秒后关机。

## 维修台

在平板中打开「维修台」。要处理已装配件，先拆进物品栏。仓库里的配件可以取出后操作。收纳的整机可先放回工作台再拆件。

- 维修一件配件。成功率为当前耐久的一半，耐久 80 对应 40%，耐久 81 对应 40.5%。成功恢复到 100，失败降到 0。耐久 0 的配件不能再次维修，可以用作合并材料。
- 合并两个同品牌、同型号、同容量的配件。每件最低按 10 耐久折算，相加后取 80%，向下取整并封顶 100。合并必定成功，保留主件并消耗副件。硬盘沿用主件的系统状态。
- 维修和合并的升级列入后续开发。

## 硬盘、内存、显卡与电源

- 每类新增五款并保留原有商品。容量、显存、接口和额定规格参考具体型号，商品价格用于游戏内测试。
- 硬盘覆盖 SATA 机械盘、2.5 英寸 SATA SSD 和 M.2 NVMe。M.2 装入主板插槽，SATA 盘装在机箱硬盘位并接线。仓库、配置检测和系统安装读取实际装入的型号和容量。
- 内存按单条 DDR4 DIMM 销售。混插合计容量，检测速率取已装内存中最低的标称值，按 i5-11400F 的 3200 MT/s 上限计算。
- GT 1030 由插槽供电，其余当前显卡使用 8 针供电线。用已通电显卡的标称功耗和其余部件的估算功耗判断电源是否足够。装机位置与线材以游戏中的共用规格模拟。
- 硬盘规格参考 [金士顿 NV2](https://www.kingston.com/datasheets/snv2s_en.pdf)、[金士顿 KC3000](https://www.kingston.com/en/ssd/kc3000-nvme-m2-solid-state-drive)、[三星 990 PRO](https://download.semiconductor.samsung.com/resources/data-sheet/Samsung_NVMe_SSD_990_PRO_Datasheet_Rev.1.0_10129514049640.pdf)、[WD Blue SA510](https://documents.sandisk.com/content/dam/asset-library/en_us/assets/public/sandisk/product/internal-drives/wd-blue-ssd/data-sheet-wd-blue-sa510-sata-ssd.pdf)、[WD Blue HDD](https://www.westerndigital.com/products/internal-drives/wd-blue-desktop-sata-hdd)、[希捷酷鱼](https://www.seagate.com/products/hard-drives/barracuda-hard-drive/?sku=ST1000DM010) 和 [希捷酷狼](https://www.seagate.com/files/www-content/product-content/ironwolf/en-us/docs/100807039r.pdf)。
- 内存规格参考 [英睿达 DDR4](https://www.crucial.com/memory/ddr4/ct4g4dfs8266)、[金士顿 ValueRAM](https://www.kingston.com/datasheets/KVR26N19S8_8.pdf)、[FURY Beast DDR4](https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr4-memory) 和 [i5-11400F 的内存支持](https://www.intel.com/content/www/us/en/products/sku/212271/intel-core-i511400f-processor-12m-cache-up-to-4-40-ghz/specifications.html)。
- 显卡规格参考 [微星 GT 1030](https://www.msi.com/Graphics-Card/GeForce-GT-1030-2G-LP-OC/Specification)、[七彩虹 GTX 1660 SUPER](https://www.colorful.cn/home/product?id=1719&mid=102)、[蓝宝石 PULSE RX 6600](https://www.sapphiretech.com/en/consumer/pulse-radeon-rx-6600-8g-gddr6)、[微星 RTX 3060](https://www.msi.com/Graphics-Card/GeForce-RTX-3060-VENTUS-2X-12G-OC/Specification)、[RTX 4060](https://www.msi.com/Graphics-Card/GeForce-RTX-4060-VENTUS-2X-BLACK-8G-OC/Specification) 和 [RTX 4070](https://www.msi.com/Graphics-Card/GeForce-RTX-4070-VENTUS-2X-12G-OC/Specification)。
- 电源规格参考 [海盗船 CX450](https://www.corsair.com/ww/en/p/psu/cp-9020120-ww/cx-series-cx450-450-watt-80-plus-bronze-certified-atx-psu-ww-cp-9020120-ww)、[CX650](https://www.corsair.com/ww/en/p/psu/cp-9020278-na/cx-series-cx650-650-watt-80-plus-bronze-atx-power-supply-cp-9020278-na)、[RM750x](https://www.corsair.com/ww/en/p/psu/cp-9020199-eu/rmx-series-rm750x-750-watt-80-plus-gold-fully-modular-atx-psu-cp-9020199-eu)、[RM850x](https://www.corsair.com/us/en/p/psu/cp-9020200-na/rmx-series-rm850x-850-watt-80-plus-gold-fully-modular-atx-psu-cp-9020200-na) 和 [海韵 FOCUS GX](https://seasonic.com/power-supplies/filter/product_cat-focus-series/)。

## 当前硬件型号清单

| 类别 | 品牌 | 型号 | 规格 |
| --- | --- | --- | --- |
| CPU | 英特尔 Intel | Core i5-11400F | 6 核 12 线程，LGA1200 |
| 显卡 | 微星 MSI | GeForce GT 1030 2G LP OC | 2 GB GDDR5 |
| 显卡 | 七彩虹 Colorful | 战斧 GeForce GTX 1660 SUPER | 6 GB GDDR6 |
| 显卡 | 蓝宝石 Sapphire | PULSE Radeon RX 6600 | 8 GB GDDR6 |
| 显卡 | 微星 MSI | GeForce RTX 3060 VENTUS 2X 12G OC | 12 GB GDDR6 |
| 显卡 | 微星 MSI | GeForce RTX 4060 VENTUS 2X BLACK 8G OC | 8 GB GDDR6 |
| 显卡 | 微星 MSI | GeForce RTX 4070 VENTUS 2X 12G OC | 12 GB GDDR6X |
| 内存 | 英睿达 Crucial | CT4G4DFS8266 | 4 GB DDR4-2666 |
| 内存 | 金士顿 Kingston | ValueRAM KVR26N19S8/8 | 8 GB DDR4-2666 |
| 内存 | 金士顿 Kingston | FURY Beast KF432C16BB/8 | 8 GB DDR4-3200 |
| 内存 | 金士顿 Kingston | FURY Beast KF426C16BB/16 | 16 GB DDR4-2666 |
| 内存 | 金士顿 Kingston | FURY Beast KF432C16BB/16 | 16 GB DDR4-3200 |
| 内存 | 金士顿 Kingston | FURY Beast KF432C16BB/32 | 32 GB DDR4-3200 |
| 内存 | 金士顿 Kingston | FURY Beast KF436C18BB/32 | 32 GB DDR4-3600 |
| 硬盘 | 希捷 Seagate | BarraCuda ST1000DM010 | 1 TB，SATA HDD |
| 硬盘 | 金士顿 Kingston | NV2 | 250 GB，M.2 NVMe SSD |
| 硬盘 | 西部数据 WD | Blue SA510 | 500 GB，2.5 英寸 SATA SSD |
| 硬盘 | 金士顿 Kingston | KC3000 | 512 GB，M.2 NVMe SSD |
| 硬盘 | 希捷 Seagate | IronWolf ST2000VN004 | 2 TB，SATA HDD |
| 硬盘 | 金士顿 Kingston | KC3000 | 1 TB（游戏内 1024 GB），M.2 NVMe SSD |
| 硬盘 | 西部数据 WD | Blue WD40EZAX | 4 TB，SATA HDD |
| 硬盘 | 三星 Samsung | 990 PRO | 2 TB，M.2 NVMe SSD |

## 桌面与安装介质

- 桌面提供「我的电脑」和空回收站。C 盘展示少量系统文件夹，打开时提示无权限。安装器按实际安装和连接的物理硬盘列出目标。
- 关机时插拔 U 盘保持黑屏，插好 U 盘后按电源键，主机进入引导阶段时才显示启动设备菜单。开机时插入可在「我的电脑」查看移动磁盘，直接运行配置检测。系统摘要显示已装硬件的型号，物理内存合并为一项，展示总容量、DDR 类型、传输速率和插槽占用。
- U 盘启动后可在已连接的可用磁盘上安装系统。复制文件、展开文件、安装组件和配置启动文件完成后，从硬盘重启进入桌面。系统安装状态属于目标硬盘。
- 进入游戏桌面后，在 U 盘内打开「硬件跑分」。测试约 15 秒，依次模拟处理器单核与多核渲染、显卡 3D 渲染、内存带宽与延迟、存储顺序读取与随机访问。报告给出综合分、四类分项分和各个已安装设备的检测结果。综合分采用处理器 30%、主显卡 35%、整组内存 20%、系统盘 15%。内存得分考虑容量、有效速率与通道，NVMe 得分受插槽速率限制。结果使用游戏内的硬件参数，同一配置可复测对比。运行时可取消，拔出 U 盘会结束本次测试。


## 代码目录

安卓、浏览器和测试分别使用 Gradle 标准源集。共享 Kotlin 文件按职责分目录，使用同一包名 `com.gamedraft.preview`。

| 路径 | 内容 |
| --- | --- |
| [model](app/src/commonMain/kotlin/com/gamedraft/preview/model/) | 装机状态、依赖和插槽、商品与钱包、整机收纳、存档编解码 |
| [ui](app/src/commonMain/kotlin/com/gamedraft/preview/ui/) | 游戏入口、拖放和长按交互、平板、全局菜单、开机与桌面 |
| [art](app/src/commonMain/kotlin/com/gamedraft/preview/art/) | 工作台绘制、部件命中区域、机箱和各型号硬件的像素绘图 |
| [composeResources](app/src/commonMain/composeResources/) | 工作台底图、空机箱与已装机素材、中文字体子集和字体许可。增加界面文字时检查新汉字的字形覆盖。 |
| [androidMain](app/src/androidMain/) | Android Manifest、横屏沉浸模式、系统返回键和 SharedPreferences 存档 |
| [wasmJsMain](app/src/wasmJsMain/) | 浏览器入口、全屏、localStorage 存档和 HTML 页面 |
| [wasmJsTest](app/src/wasmJsTest/) | 装机与存档、商城与供电、横屏及旋转布局拖放回归 |
| [gradle/wrapper](gradle/wrapper/) 和 [kotlin-js-store/wasm](kotlin-js-store/wasm/) | 固定 Gradle 版本及网页依赖锁文件 |
| [.openai/hosting.json](.openai/hosting.json) | 现有试玩站点配置，静态产物目录为 `dist/` |
| `artifacts/apk/` | 本地历史测试包 0.1.0、0.1.1、0.1.2，作为本地产物归档 |

## 开发、构建与测试

项目使用 Kotlin Multiplatform 2.2.21、Compose Multiplatform 1.9.3、Android Gradle Plugin 8.11.1、Gradle 8.13。开发环境使用 JDK 21 和 Android SDK Platform 36。安卓最低 API 24（Android 7.0），目标 API 36。浏览器需支持 WebAssembly GC。可用 Android Studio 打开仓库根目录，或从根目录运行 Gradle Wrapper。Windows 将 `./gradlew` 替换为 `gradlew.bat`。

| 目的 | 命令 | 结果 |
| --- | --- | --- |
| 本机网页调试 | `./gradlew :app:wasmJsBrowserDevelopmentRun` | 从终端输出的本地网页地址进入 |
| 构建可部署网页 | `./gradlew :app:stageWebPreview` | 将 HTML、JS、Wasm、字体和素材集中输出到 `dist/` |
| 本地预览发布产物 | `python3 -m http.server 8000 --directory dist` | 浏览器打开 `http://localhost:8000/` |
| 构建安卓调试包 | `./gradlew :app:assembleDebug` | 输出 `app/build/outputs/apk/debug/app-debug.apk` |
| 编译测试 | `./gradlew :app:compileTestKotlinWasmJs` | 检查网页测试代码能否构建 |
| 执行浏览器测试 | `./gradlew :app:wasmJsBrowserTest` | 在已配置的浏览器测试环境中执行 |

已覆盖的回归包括六款主板满配与收纳后恢复、商城扣款与旧档迁移、不同磁盘和混插内存识别、显卡与电源组合，以及购买机箱后拖到工作台再收纳。此前使用模拟触摸验证过横屏与竖屏旋转布局。安卓真机体验以实际测试的构建为准。

日常提交包含双端源码、游戏资源、构建配置和测试。网页导出目录、历史 APK、Gradle 缓存、本机 SDK 路径和签名文件由 Git 忽略。网页版发布时使用现有 Sites 站点和访问权限，将同一源码版本对应的 `dist/` 发布至原测试链接。GitHub 用于托管源码。
