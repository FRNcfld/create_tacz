package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * 军工厂：子弹 —— 玻璃罩里缓缓旋转的展示用子弹。
 *
 * <p>整台机器的活动部件只有这一个：底部应力输入杆 + 黄铜卡盘 + 一发狙击弹，
 * 三者合成一个局部模型 {@code create_tacz:block/rotating_bullet}，由一个
 * {@link RotatingInstance} 绕竖直轴旋转。机壳、玻璃仍是普通方块模型。
 *
 * <h2>为什么用 visual 而不是 Create 的渲染器</h2>
 * {@code KineticBlockEntityRenderer} 继承的是 {@code SafeBlockEntityRenderer} 而不是
 * {@code SmartBlockEntityRenderer}，换过去会丢掉滤波槽幽灵物品的渲染；而且它在 Flywheel
 * 可用时直接 return，不可用时渲染 {@code getRotatedModel(...)}，也就是把整个机壳转起来。
 *
 * <h2>转速：接通才转、转起来是恒速</h2>
 * <b>转不转看动力，转多快跟动力无关。</b>没接传动杆 / 没来源 / 过载时 {@code getSpeed()} 为 0，
 * 子弹静止；只要有转速就以固定的 {@link #SPIN_RPM} 慢慢转，不随网络快慢变化。
 *
 * <h2>角度为什么要自己算</h2>
 * Create 自带的 {@code SingleAxisRotatingVisual} 每次只调 {@code setup(be, axis, speed)}，
 * 而 {@code KineticBlockEntity} 并不提供随时间变化的角度（{@code getRotationAngleOffset}
 * 恒为 0），所以角度是 shader 按「{@code rotationalSpeed} × 全局时间」积出来的。
 * 那样一旦速度变化或断开，角度会<b>整体跳变</b>——它和绝对时间成正比。
 *
 * <p>所以这里给 {@code rotationalSpeed} 传 <b>0</b>，把 shader 那套积分让掉，
 * 改由本类累计 {@link #angle} 再写进 {@code rotationOffset}（单位是度，
 * 见 {@code KineticBlockEntityVisual.rotationOffset} 返回的 22.5/11.25）。
 * 好处是停机时子弹<b>停在原地</b>，再开机接着转，不会弹回某个固定朝向。
 *
 * <p><b>注意累计必须放在 {@link #tick} 里，不能放 {@link #update}。</b>
 * Flywheel 的 {@code Visual.update(float)} 只在视觉对象被创建或被
 * {@code VisualManager.queueUpdate} 显式排队时调用（方块实体同步等），
 * <b>不是每帧/每刻的钩子</b>；真正的周期钩子是 {@code TickableVisual.tick} /
 * {@code DynamicVisual.beginFrame}。放错地方的表现就是「接了动力也不转」。
 */
public class MilitaryFactoryVisual extends KineticBlockEntityVisual<MilitaryFactoryBulletsBlockEntity>
        implements SimpleTickableVisual
{
    /** 旋转组：底部输入杆 + 卡盘 + 子弹。整组按「朝上」建模，绕自身 Y 轴旋转。 */
    public static final PartialModel ROTATING_BULLET =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateTacz.MOD_ID, "block/rotating_bullet"));

    /** 本机的旋转轴是竖直的，见 {@code MilitaryFactoryBulletsBlock.getRotationAxis}。 */
    private static final Direction.Axis AXIS = Direction.Axis.Y;

    /** 展示转速：4 RPM，约 15 秒一圈。 */
    private static final float SPIN_RPM = 4.0f;

    /**
     * 每游戏刻转过的角度。
     *
     * <p>Create 内部按「度/秒」处理速度（{@code setup} 里乘 6，所以 256 → 256 RPM），
     * 于是 4 RPM = 24 度/秒；一游戏刻是 1/20 秒，即 1.2 度/刻。
     * 对着一发约 2.4 格宽的子弹，1.2 度只让边缘移动约 0.05 格，肉眼是一格一格看不出来的。
     */
    private static final float DEGREES_PER_TICK = SPIN_RPM * 6.0f / 20.0f;

    /**
     * 触发本类的静态初始化，从而在正确的时间点创建 {@link #ROTATING_BULLET}。
     *
     * <p><b>必须在 {@code FMLClientSetupEvent} 阶段调用</b>，也就是资源重载之前。
     * Flywheel 只在 {@code ModelEvent.RegisterAdditional} 触发的那一刻，把<b>当时已经存在</b>的
     * PartialModel 提交给模型烘焙；{@code BakingCompleted} 之后再创建的就没人管了。
     * 而 Flywheel 的 {@code PartialModel} 构造器在烘焙完成后的分支里，会直接向
     * {@code ModelManager} 按 location 取烘焙结果 —— 取不到就回落到 <b>missing model</b>，
     * 也就是一个整格的紫黑方块，会把整台机器连玻璃一起盖住。
     *
     * <p>这正是 Create 自己的做法：{@code CreateClient.clientInit} 里调
     * {@code AllPartialModels.init()}。只是引用一下 {@code MilitaryFactoryVisual::new}
     * 并不会让本类初始化，要等到第一次渲染才初始化，那就太晚了。
     */
    public static void init()
    {
        if (ROTATING_BULLET == null)
            throw new IllegalStateException("create_tacz:block/rotating_bullet 未能初始化");
    }

    private final RotatingInstance spin;

    /** 自己累计的角度（度）。没有动力时保持不变。 */
    private float angle;

    public MilitaryFactoryVisual(VisualizationContext context, MilitaryFactoryBulletsBlockEntity be,
            float partialTick)
    {
        super(context, be, partialTick);

        Instancer<RotatingInstance> instancer = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ROTATING_BULLET));

        // 速度传 0，把 shader 的「速度 × 时间」让掉；朝向由 tick() 每刻写进 rotationOffset。
        // 不需要 rotateToFace：整组按「朝上」建模，绕自身 Y 轴转就是对的。
        spin = instancer.createInstance()
                .setup(be, AXIS, 0f)
                .setPosition(getVisualPosition());
    }

    /** 每游戏刻推进角度。这是唯一的周期钩子，见类注释。 */
    @Override
    public void tick(TickableVisual.Context context)
    {
        if (!isPowered())
            return;

        angle = (angle + DEGREES_PER_TICK) % 360f;

        // 颜色【不能】无条件 setColor(blockEntity)：colorFromBE 是拿网络 ID
        // (Color.generateFromLong) 生成一个调试色，不是过载红。Create 自己在
        // setup(be, axis, speed) 里也只在 KineticDebugger.isActive() 时才上色 ——
        // 无条件调用会把整发布满该网络的调试色（实测是一层绿）。
        if (KineticDebugger.isActive())
            spin.setColor(blockEntity);

        spin.setRotationOffset(angle)
                .setChanged();
    }

    /** 只在被显式刷新时调用（建号、方块实体同步），把当前角度重新写一遍即可。 */
    @Override
    public void update(float partialTick)
    {
        spin.setRotationOffset(angle)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick)
    {
        relight(spin);
    }

    /** 方块被破坏时的碎裂贴图要跟着这个旋转组一起闪。 */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer)
    {
        consumer.accept(spin);
    }

    @Override
    protected void _delete()
    {
        spin.delete();
    }

    /**
     * 接上传动杆、有转速、而且没过载 —— 与 Create 机器「转不转」的判据保持一致。
     *
     * <p>用 {@code getSpeed() != 0} 而不是本机加工用的 {@code MIN_SPEED}：那 16 RPM 是
     * 「够不够加工」的门槛，而这里的子弹只是挂在这根输入轴上，轴转它就转。
     */
    private boolean isPowered()
    {
        return blockEntity.getSpeed() != 0f && !blockEntity.isOverStressed();
    }
}
