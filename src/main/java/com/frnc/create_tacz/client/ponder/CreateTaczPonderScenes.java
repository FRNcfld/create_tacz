package com.frnc.create_tacz.client.ponder;

import com.frnc.create_tacz.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.init.ModItems;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 本模组的思索场景。
 *
 * <p>注册用的 key 直接取 Registrate 条目的注册名（{@code RegistryEntry::getId}）——
 * 方块与其 {@code .simpleItem()} 产出的 BlockItem 同名，所以一个条目同时覆盖方块和物品，
 * 玩家悬停哪一个都能开思索。
 */
public class CreateTaczPonderScenes
{
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper)
    {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        // "bullet_factory_1" 是相对 assets/create_tacz/ponder/ 的路径，不含命名空间也不含 .nbt，
        // 对应文件即 assets/create_tacz/ponder/bullet_factory_1.nbt。
        // 同一个方块挂多个场景时，思索界面里按注册顺序依次播放。
        HELPER.forComponents(ModBlocks.MILITARY_FACTORY_BULLETS)
                .addStoryBoard("bullet_factory_1", CreateTaczPonderScenes::bulletsByHand)
                .addStoryBoard("bullet_factory_2", CreateTaczPonderScenes::automatingAmmunition);
    }

    /**
     * 场景一：先把问题摆出来 —— 弹药装配台能造子弹，但它进不了自动化产线。
     *
     * <h2>这张结构图里有什么（12×3×12，坐标即文件里的坐标）</h2>
     * <ul>
     *   <li>{@code y=0} 底板（12×12 棋盘格），外加嵌在底板里的动力入口
     *       {@code (2,0,6)} 与 {@code (2,1,6)} 两截链传动；</li>
     *   <li>{@code y=1} 主带沿 X 铺在 {@code z=5}（{@code x=2} 端 → 滑轮 {@code x=5} → {@code x=9} 端），
     *       副带沿 Z 铺在 {@code x=6}（{@code z=2→4} 与 {@code z=6→8}），齿轮箱在 {@code (5,1,4)}、{@code (5,1,6)}；</li>
     *   <li>{@code y=2} 三个创造板条箱当料源，一圈黄铜/安山漏斗把料喂进三条带，输出桶在 {@code (9,2,5)}；</li>
     *   <li>三条带连同漏斗全部汇聚到 {@code (6,2,5)} 这一个格子 —— 也正是弹药装配台占着的位置。</li>
     * </ul>
     *
     * <h2>旁白顺序</h2>
     * 完整的平台出现 → 装配台单独出现 → 装配台消失、平台的动力入口腾出空缺 →
     * 链传动等部件依次出现并运转 → 装配台回到产线正中间 →
     * 三句文本依次"切换"，说明它插不进这条线。
     */
    public static void bulletsByHand(SceneBuilder builder, SceneBuildingUtil util)
    {
        // 用 Create 的构建器而不是 Ponder 原生的：modifyKineticSpeed、rotationSpeedIndicator
        // 这些是 Create 加的扩展，原生 SceneBuilder 没有。
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("bullet_factory_1", "Making Ammunition by Hand");

        // 底板已补成 12×12 正方形（结构图 size = [12, 3, 12]），正好对上 Ponder 只收一个边长的限制。
        scene.configureBasePlate(0, 0, 12);

        BlockPos benchPos = util.grid().at(6, 2, 5);
        Selection bench = util.select().position(benchPos);

        // 第一幕出现的平台要"完整没有空缺"，所以整层底板照搬。
        Selection plate = util.select().layer(0);
        // 动力入口占两格：嵌在底板里的那一格 + 它上面那格。
        // 底下这格 (2,0,6) 在结构图里嵌着链传动，第一幕会被临时换成一格棋盘格（见下），
        // 第三幕腾成"空缺"，最后用 restoreBlocks 把结构图里的原方块装回去。
        BlockPos powerFloorPos = util.grid().at(2, 0, 6);
        Selection powerFloorCell = util.select().position(powerFloorPos);
        Selection power = powerFloorCell.add(util.select().position(2, 1, 6));
        Selection gearboxes = util.select().position(5, 1, 4).add(util.select().position(5, 1, 6));
        Selection mainBelt = util.select().fromTo(2, 1, 5, 9, 1, 5);
        // 两条副带都把料往中间（z=5）送，但朝向相反、要的转速符号也相反，所以拆成两个 Selection。
        Selection sideBeltNorth = util.select().fromTo(6, 1, 2, 6, 1, 4);
        Selection sideBeltSouth = util.select().fromTo(6, 1, 6, 6, 1, 8);
        Selection sideBelts = sideBeltNorth.add(sideBeltSouth);
        Selection crates = util.select().position(6, 2, 2)
                .add(util.select().position(3, 2, 5))
                .add(util.select().position(6, 2, 8));
        Selection funnels = util.select().position(6, 2, 3).add(util.select().position(6, 2, 4))
                .add(util.select().position(4, 2, 5)).add(util.select().position(5, 2, 5))
                .add(util.select().position(7, 2, 5)).add(util.select().position(8, 2, 5))
                .add(util.select().position(6, 2, 6)).add(util.select().position(6, 2, 7));
        Selection output = util.select().position(9, 2, 5);

        // ---- 一、完整的思索平台 ----

        // 结构图在 (2,0,6) 这一格地板里嵌着链传动的一截。它不是棋盘格，直接展示出来
        // 平台就像破了个洞（远景截图里那处缺口就是它）。所以先把它临时换成一格同色的
        // 棋盘格 —— 棋盘格是按 (x+z) 的奇偶交替的，(2,6) 为偶数的格子正是浅灰混凝土 ——
        // 平台这才"完整没有空缺"。第三幕再把这一格腾空成动力入口的位置。
        scene.world().setBlock(powerFloorPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
        scene.world().showSection(plate, Direction.UP);
        scene.idle(10);

        // ---- 二、弹药装配台单独出现 ----

        scene.world().showSection(bench, Direction.UP);
        scene.idle(5);
        scene.overlay().showText(60)
                .text("Ammunition can be crafted at the Ammo Assembly Table")
                .pointAt(util.vector().topOf(benchPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        // ---- 三、装配台消失，平台腾出空缺，部件依次出现 ----

        // hideSection 会把它从底板那一节里抹掉、再作为独立小节淡出，
        // 所以后面可以对同一个位置重新 showSection 让它回来。
        scene.world().hideSection(bench, Direction.UP);
        scene.idle(15);

        // 平台空缺：这一格地板淡出，平台真的缺一块。此刻其余部件一个都还没出现，
        // 所以这个洞看得干干净净 —— 它正是接下来动力入口要装进去的位置。
        scene.world().hideSection(powerFloorCell, Direction.DOWN);
        scene.idle(20);

        // 部件依次出现：动力入口先装进这个空缺，自下而上。restoreBlocks 还原的是结构图里的
        // 原方块，连那截链传动自带的方块实体数据一起 —— 第一幕里它被临时换成了棋盘格。
        scene.world().restoreBlocks(power);
        scene.world().showSection(power, Direction.UP);
        scene.idle(8);
        scene.world().showSection(gearboxes, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(mainBelt, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(sideBelts, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(crates, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(funnels, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(output, Direction.DOWN);
        scene.idle(20);

        // 让整条线转起来。思索世界里方块实体不 tick、转速不会自己传播，所以只能按"这条带子
        // 该往哪走"分别把 Speed 写进各动能方块实体的 NBT（setKineticSpeed 内部就是写 NBT，
        // 由指令执行时的 load() 回读）。
        //
        // 符号规则来自 BeltBlockEntity#getMovementDirection：物品走向只看带子朝向的**轴**与转速符号，
        // 与朝向是东还是西、是南还是北无关 ——
        //     朝向轴为 X（东西向）：Speed > 0 → 物品向西；< 0 → 向东
        //     朝向轴为 Z（南北向）：Speed > 0 → 物品向南；< 0 → 向北
        // 于是：
        //   主带（朝向 west，轴 X）—— 西端板条箱进料、向东穿过机器、东端输出桶出货，物品流向为东 → 取负；
        //   北段副带（朝向 north，轴 Z）—— 板条箱在 z=2，要往南送进 z=5 的交汇点 → 取正；
        //   南段副带（朝向 south，轴 Z）—— 板条箱在 z=8，要往北送进 z=5 的交汇点 → 取负。
        //
        // 齿轮箱与链传动跟着主带给同一个符号。注意它们在本图里并没有真正接上动力：
        // 链传动沿 Z 方向只挨着底板与主带的末端（带的端头不接轴），两个 Y 轴齿轮箱的上下
        // 也分别是底板和漏斗、没有竖直轴。所以这两个的转向只是"看起来自洽"，
        // 将来真的在底板下接上动力源时，符号要以那个动力源为准。
        scene.world().setKineticSpeed(mainBelt.add(gearboxes).add(power), -32f);
        scene.world().setKineticSpeed(sideBeltNorth, 32f);
        scene.world().setKineticSpeed(sideBeltSouth, -32f);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 1, 6));
        scene.idle(25);

        // ---- 三、装配台回到产线正中间 ----

        scene.world().showSection(bench, Direction.UP);
        scene.idle(20);
        scene.overlay().showText(70)
                .text("But it cannot be hooked up to an automated production line")
                .pointAt(util.vector().topOf(benchPos))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(60)
                .text("Ammunition can only be made by hand")
                .pointAt(util.vector().topOf(benchPos))
                .placeNearTarget();
        scene.idle(70);

        scene.overlay().showText(70)
                .text("Which is an extremely unhealthy practice")
                .pointAt(util.vector().topOf(benchPos))
                .placeNearTarget();
        scene.idle(80);

        scene.markAsFinished();
    }

    /**
     * 场景二：把弹药工厂接进自动化产线。
     *
     * <h2>这张结构图里有什么（11×5×7，坐标即文件里的坐标）</h2>
     * <ul>
     *   <li>{@code y=0} 底板 11×7；</li>
     *   <li>{@code y=1} 齿轮箱 {@code (2,1,3)} —— 正对工厂的底面，就是动力入口；
     *       置物台 {@code (2,1,4)}；以及 {@code x=3..8, z=2..3} 的 6×2×2 保险库；</li>
     *   <li>{@code y=2} 弹药工厂 {@code (2,2,3)}（紧贴保险库）、黄铜漏斗 {@code (2,2,4)}
     *       （正压在置物台上方，负责把成品刮走）；</li>
     *   <li>{@code y=3/4} 三个滑槽与三个创造板条箱，从上往下把原料灌进保险库。</li>
     * </ul>
     *
     * <p>结构图里的滤波槽已经预置好了：一张列表过滤器，内容是 {@code tacz:ammo}（AmmoId 为
     * {@code tacz:40mm}），黑名单关（即白名单）、匹配物品属性开 —— 与第四幕的台词一致。
     */
    public static void automatingAmmunition(SceneBuilder builder, SceneBuildingUtil util)
    {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("bullet_factory_2", "Making Ammunition Automatically");

        // 底板是 11(x)×7(z)，不是正方形，而 configureBasePlate 只收一个边长。
        // 取 11、并把 z 偏移设成 -2：虚拟底板于是横跨 z=-2..8，正好以真实底板（z=0..6）为中心，
        // 镜头取景与投影都是居中的。若以后把底板补成 11×11，这里改回 (0, 0, 11) 即可。
        scene.configureBasePlate(0, -2, 11);

        // 默认镜头是沿 x 轴看，而这张图的长边恰好也在 x 上（保险库长 6 格，三个滑槽/板条箱
        // 也是一列排在 x 上），于是它们前后相叠、互相遮挡。绕竖轴顺时针转 90° 后改从南侧看：
        // 11 格宽的那一面横铺在屏幕上，三个板条箱并排，工厂顶面的过滤槽、黄铜漏斗与置物台
        // 都朝向镜头。要换到另一侧的话，把 -90 改成 90。
        scene.rotateCameraY(-90);
        scene.idle(25);

        BlockPos factoryPos = util.grid().at(2, 2, 3);
        BlockPos gearboxPos = util.grid().at(2, 1, 3);
        BlockPos depotPos = util.grid().at(2, 1, 4);
        BlockPos funnelPos = util.grid().at(2, 2, 4);
        // 贴着工厂的那一格保险库，用它代表"相邻的容器"
        BlockPos vaultPos = util.grid().at(3, 2, 3);

        Selection plate = util.select().layer(0);
        Selection gearbox = util.select().position(gearboxPos);
        Selection factory = util.select().position(factoryPos);
        Selection vault = util.select().fromTo(3, 1, 2, 8, 2, 3);
        Selection logistics = util.select().position(depotPos).add(util.select().position(funnelPos));
        // 滑槽与创造板条箱都在 x=4..6、z=3 这一列上，一层层叠起来
        Selection feed = util.select().fromTo(4, 3, 3, 6, 4, 3);

        // 文本箭头落点：对准某个方块时用它的中心；过滤槽另外给精确位置。
        Vec3 factoryAt = util.vector().centerOf(factoryPos);
        Vec3 gearboxAt = util.vector().centerOf(gearboxPos);
        Vec3 vaultAt = util.vector().centerOf(vaultPos);
        Vec3 funnelAt = util.vector().centerOf(funnelPos);
        // 过滤槽的位置取自 MilitaryFactoryBulletsBlockEntity.FilterSlot 的
        // VecHelper.voxelSpace(8, 15.5, 8)，即方块内 (0.5, 0.96875, 0.5) 处 —— 顶面正中偏下一点。
        Vec3 filterSlot = util.vector().of(factoryPos.getX() + .5, factoryPos.getY() + .96875,
                factoryPos.getZ() + .5);

        // ---- 一、平台与全部方块依次出现 ----

        scene.world().showSection(plate, Direction.UP);
        scene.idle(12);
        scene.world().showSection(gearbox, Direction.DOWN);
        scene.idle(12);
        scene.world().showSection(factory, Direction.DOWN);
        scene.idle(12);
        scene.world().showSection(vault, Direction.DOWN);
        scene.idle(12);
        scene.world().showSection(logistics, Direction.DOWN);
        scene.idle(12);
        scene.world().showSection(feed, Direction.DOWN);
        scene.idle(30);

        // ---- 二、通入应力，转速有下限 ----
        //
        // 下面每句的节奏都是固定的：showText(时长) 之后 idle 的 tick 数 > 时长，
        // 于是上一句先整个淡出、空一拍，下一句才出现。早先两句之间只隔 20 tick，
        // 两张文本框又落在同一个锚点上，就在屏幕上叠印成一团了。

        scene.overlay().showText(80)
                .text("Supply stress to the Military Factory")
                .pointAt(gearboxAt)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(15);

        // 思索世界里方块实体不 tick、转速不会自己传播，要按需把 Speed 写进 NBT。
        // 这里给 64 RPM：既越过 16 RPM 的门槛，也看得出在转。
        scene.world().setKineticSpeed(factory.add(gearbox), 64f);
        scene.effects().rotationSpeedIndicator(gearboxPos);
        scene.idle(95);

        scene.overlay().showText(80)
                .text("Its speed must be at least 16 RPM")
                .pointAt(factoryAt)
                .placeNearTarget();
        scene.idle(100);

        // ---- 三、放进列表过滤器 ----

        scene.overlay().showText(70)
                .text("Insert a list filter")
                .pointAt(factoryAt)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        // ---- 四、过滤器里放什么 ----

        scene.overlay().showText(80)
                .text("Put the ammunition you want to craft (only one type) into the list filter")
                .pointAt(filterSlot)
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(80)
                .text("Remember to set it to whitelist and to match item NBT")
                .pointAt(filterSlot)
                .placeNearTarget();
        scene.idle(100);

        // ---- 五、原料从相邻容器里自动抽取 ----

        scene.overlay().showText(80)
                .text("The Military Factory pulls ingredients from adjacent inventories")
                .pointAt(vaultAt)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // ---- 六、产出速度取决于转速 ----

        scene.overlay().showText(70)
                .text("Crafting speed depends on rotation speed")
                .pointAt(factoryAt)
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("At 256 RPM it produces one batch every 5 seconds")
                .pointAt(factoryAt)
                .placeNearTarget();
        scene.idle(100);

        // ---- 七、成品落到置物台，再被漏斗取走 ----

        // createItemOnBeltLike 会把子弹塞进置物台的 DirectBeltInputBehaviour，
        // 并顺手让它正上方的漏斗扇一下 —— 这正好是这张图里黄铜漏斗的位置。
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, sampleAmmunition());
        scene.idle(25);

        scene.overlay().showText(80)
                .text("Finished ammunition can be taken out with funnels and other logistics")
                .pointAt(funnelAt)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    /**
     * 一颗与结构图滤波槽口径一致的子弹，用来演示成品。
     *
     * <p>TaCZ 的所有口径共用同一个物品 {@code tacz:ammo}，口径存在 NBT 里，
     * 所以必须走 {@link IAmmo#setAmmoId} 设置，不能只 new 一个 ItemStack。
     */
    private static ItemStack sampleAmmunition()
    {
        ItemStack stack = new ItemStack(ModItems.AMMO.get());
        IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
        if (ammo != null)
            ammo.setAmmoId(stack, ResourceLocation.fromNamespaceAndPath("tacz", "40mm"));
        return stack;
    }

    private CreateTaczPonderScenes()
    {
    }
}
