# 游戏需求记录

- 项目名称待定，暂用 `game-draft` 作为目录名。
- 游戏为 2D 平面维修模拟游戏。
- 联机、登录、云存档、震动反馈、系统通知、语音聊天、相机、定位和照片导入暂无计划。按目前的功能范围，安卓端可做到零次运行时权限请求。
- 最终交付为可在安卓手机上安装运行的游戏 App。
- 开发期间，每完成一个版本，就部署到可远程访问的网站。用户打开测试链接即可在浏览器内运行游戏，整个测试流程在网页上完成。
- 首个测试版本交付网页链接和安卓 APK。后续迭代默认交付网页链接，用户提出需要时再打包并发送 APK。
- 用户集中讲需求时，先听取并记录。用户主动提问时，当场回答。等用户讲完后，再集中确认尚未明确的需求。

## 核心玩法

玩家接下客户的订单并收到旧设备，排查故障，按客户要求修理坏件或更换零件，完成后交付设备。

前期靠更换坏零件完成工单，利润一般。后期解锁维修坏件的能力，解锁条件和玩法细节待定。

首个可玩版本以台式电脑维修为方向，从简单的机箱和配件组合开始。工作台以打开侧板的机箱平面图展示主板和各个零件的安装位置。

后续逐步扩充可搭配的配件，包括不同型号的显卡（如 4090、1080）、不同接口和容量的硬盘，以及不同容量和频率的内存。

## 美术方向

采用像素风，以第 4 张竖版俯视工作台效果图为参考。[像素风工作台效果图](app/src/commonMain/composeResources/drawable/workbench_assembled.png)

现有配件沿用像素素材。五款机箱参考现实型号绘制，分别采用黑色网孔、白色玻璃侧板、炭黑配胡桃木格栅、深灰封闭面板和白色三角孔面板。

## 游戏菜单与存档

- 全局悬浮按钮可拖动。点击按钮或按 Esc 打开游戏菜单，可继续、返回主界面、保存、载入或退出。安卓返回键同样可开关菜单。
- 三个本地存档位分别保存已装和拆下的机箱及配件、螺丝、耐久、系统安装结果及仓库物品。打开菜单暂停计时，读档后回到工作台，主机关机。
- 网页存档保存在当前浏览器的站点存储中，安卓存档保存在应用内。

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

- 右上角的平板入口在「开机检测」或「工作台」右侧，主界面、工作台、检测及菜单均可打开。当前自由装机版提供商城和仓库。
- 商城包含机箱、主板、CPU、散热、内存、显卡、电源、硬盘、线材和工具分类。商城商品使用「类别-厂商-系列或接口-型号及规格」命名。商品标注游戏内测试价。新开局有 ¥50,000，足够买下各型号一件并搭配多套整机。购买从余额扣款后直接入物品栏，余额不足时购买按钮置灰。工具分类暂空。余额随存档保存，早期无余额字段的存档读入时获得测试资金。在仓库页面可逐件存入或取出，收纳整机、配件耐久、硅脂和系统状态随物品转移。旧存档中待签收的配件在读档时自动进入物品栏。
- 空工作台也显示「开机检测」入口。无机箱时，电源、重启和插入 U 盘按钮置灰。放置机箱后可手动尝试开机，主机依照已安装硬件显示启动过程和对应故障现象。

- 显示器页面右上角的「工作台」和工作台页面的「开机检测」共用同一按钮位置。显示器页面的电源键控制开关机，通电时发光。重启键重新开始启动过程。启动现象显示在屏幕内，工作台省略螺丝数量提示。
- 工具栏第二格是硅脂。拆下 CPU 散热器时，CPU 上的硅脂失效。选中硅脂后可点物品栏内的 CPU 或露出的 CPU 重涂，台面上的 CPU 会显示明亮的硅脂斑。散热器缺失或损坏时通电 5 秒后关机，散热器完好但 CPU 缺硅脂时通电 10 秒后关机。

## 硬盘、内存、显卡与电源

- 每类新增五款并保留原有商品。容量、显存、接口和额定规格参考具体型号，商品价格用于游戏内测试。
- 硬盘覆盖 SATA 机械盘、2.5 英寸 SATA SSD 和 M.2 NVMe。M.2 装入主板插槽，SATA 盘装在机箱硬盘位并接线。仓库、配置检测和系统安装读取实际装入的型号和容量。
- 内存按单条 DDR4 DIMM 销售。混插合计容量，检测速率取已装内存中最低的标称值，按 i5-11400F 的 3200 MT/s 上限计算。
- GT 1030 由插槽供电，其余当前显卡使用 8 针供电线。用已通电显卡的标称功耗和其余部件的估算功耗判断电源是否足够。装机位置与线材以游戏中的共用规格模拟。
- 硬盘规格参考 [金士顿 NV2](https://www.kingston.com/datasheets/snv2s_en.pdf)、[金士顿 KC3000](https://www.kingston.com/en/ssd/kc3000-nvme-m2-solid-state-drive)、[三星 990 PRO](https://download.semiconductor.samsung.com/resources/data-sheet/Samsung_NVMe_SSD_990_PRO_Datasheet_Rev.1.0_10129514049640.pdf)、[WD Blue SA510](https://documents.sandisk.com/content/dam/asset-library/en_us/assets/public/sandisk/product/internal-drives/wd-blue-ssd/data-sheet-wd-blue-sa510-sata-ssd.pdf)、[WD Blue HDD](https://www.westerndigital.com/products/internal-drives/wd-blue-desktop-sata-hdd)、[希捷酷鱼](https://www.seagate.com/products/hard-drives/barracuda-hard-drive/?sku=ST1000DM010) 和 [希捷酷狼](https://www.seagate.com/files/www-content/product-content/ironwolf/en-us/docs/100807039r.pdf)。
- 内存规格参考 [英睿达 DDR4](https://www.crucial.com/memory/ddr4/ct4g4dfs8266)、[金士顿 ValueRAM](https://www.kingston.com/datasheets/KVR26N19S8_8.pdf)、[FURY Beast DDR4](https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr4-memory) 和 [i5-11400F 的内存支持](https://www.intel.com/content/www/us/en/products/sku/212271/intel-core-i511400f-processor-12m-cache-up-to-4-40-ghz/specifications.html)。
- 显卡规格参考 [微星 GT 1030](https://www.msi.com/Graphics-Card/GeForce-GT-1030-2G-LP-OC/Specification)、[七彩虹 GTX 1660 SUPER](https://www.colorful.cn/home/product?id=1719&mid=102)、[蓝宝石 PULSE RX 6600](https://www.sapphiretech.com/en/consumer/pulse-radeon-rx-6600-8g-gddr6)、[微星 RTX 3060](https://www.msi.com/Graphics-Card/GeForce-RTX-3060-VENTUS-2X-12G-OC/Specification)、[RTX 4060](https://www.msi.com/Graphics-Card/GeForce-RTX-4060-VENTUS-2X-BLACK-8G-OC/Specification) 和 [RTX 4070](https://www.msi.com/Graphics-Card/GeForce-RTX-4070-VENTUS-2X-12G-OC/Specification)。
- 电源规格参考 [海盗船 CX450](https://www.corsair.com/ww/en/p/psu/cp-9020120-ww/cx-series-cx450-450-watt-80-plus-bronze-certified-atx-psu-ww-cp-9020120-ww)、[CX650](https://www.corsair.com/ww/en/p/psu/cp-9020278-na/cx-series-cx650-650-watt-80-plus-bronze-atx-power-supply-cp-9020278-na)、[RM750x](https://www.corsair.com/ww/en/p/psu/cp-9020199-eu/rmx-series-rm750x-750-watt-80-plus-gold-fully-modular-atx-psu-cp-9020199-eu)、[RM850x](https://www.corsair.com/us/en/p/psu/cp-9020200-na/rmx-series-rm850x-850-watt-80-plus-gold-fully-modular-atx-psu-cp-9020200-na) 和 [海韵 FOCUS GX](https://seasonic.com/power-supplies/filter/product_cat-focus-series/)。

## 桌面与安装介质

- 桌面提供「我的电脑」和空回收站。C 盘展示少量系统文件夹，打开时提示无权限。安装器按实际安装和连接的物理硬盘列出目标。
- 关机时插拔 U 盘保持黑屏，插好 U 盘后按电源键，主机进入引导阶段时才显示启动设备菜单；开机时插入可在「我的电脑」查看移动磁盘，直接运行配置检测。系统摘要显示已装硬件的型号，物理内存合并为一项，展示总容量、DDR 类型、传输速率和插槽占用。
- U 盘启动后可在已连接的可用磁盘上安装系统。复制文件、展开文件、安装组件和配置启动文件完成后，从硬盘重启进入桌面。系统安装状态属于目标硬盘。

## 开发技术

采用 Kotlin Multiplatform 和 Compose Multiplatform。页面和状态切换逻辑共用，从同一项目构建安卓 App 和浏览器 WebAssembly 版本。

## 首个测试版本

- 主页只显示标题「测试」和按钮「开始游戏」。点击按钮进入第二个画面。
- 第二个画面只显示标题「进行中」和按钮「返回主页」。点击按钮回到主页。
