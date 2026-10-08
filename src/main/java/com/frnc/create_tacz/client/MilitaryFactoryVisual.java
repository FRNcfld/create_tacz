package com.frnc.create_tacz.client;

import java.util.function.Consumer;

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
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

/**
 * 军工厂：子弹 —— 玻璃罩里缓缓旋转的展示用子弹。
 *
 * <p>整台机器的活动部件只有这一个：底部应力输入杆 + 黄铜卡盘 + 一发狙击弹，
 * 三者合成一个局部模型 {@code create_tacz:block/rotating_bullet}（见
 * {@link ModPartialModels}），由一个 {@link RotatingInstance} 绕竖直轴旋转。
 * 机壳、玻璃仍是普通方块模型。
 *
 * <h2>转速：接通才转、转起来是恒速</h2>
 * <b>转不转看动力，转多快跟动力无关。</b>没接传动杆 / 没来源 / 过载时 {@code getSpeed()} 为 0，
 * 子弹静止；只要有转速就以固定的展示转速慢慢转，不随网络快慢变化。
 * 判据在 {@link MilitaryFactoryBulletsBlockEntity#isDisplaySpinning()}。
 *
 * <h2>角度为什么要自己算</h2>
 * Create 自带的 {@code SingleAxisRotatingVisual} 每次只调 {@code setup(be, axis, speed)}，
 * 而 Flywheel 的 {@code rotating.vert} 里角度是
 * {@code offset + 全局渲染时间 × speed} 由 shader 积出来的 —— {@code offset} 只是个恒定的
 * 齿对齐偏移，{@code speed} 是恒定的度/秒。那样一旦速度变化或断开，角度会<b>整体跳变</b>，
 * 因为它和绝对时间成正比。
 *
 * <p>所以这里给 {@code rotationalSpeed} 传 <b>0</b>，把 shader 那套积分让掉，
 * 改由 {@code rotationOffset} 承载角度，来源是方块实体的 {@code getRenderAngle()}。
 * 好处是停机时子弹<b>停在原地</b>，再开机接着转，不会弹回某个固定朝向。
 *
 * <p>角度存在方块实体而不是本类字段里，有两个原因：
 * visual 会在区块重渲 / 资源重载时被销毁重建，实例字段会归零让子弹跳回起点；
 * 而且 {@link MilitaryFactoryRenderer} 的回退路径要读<b>同一个</b>角度。
 *
 * <p><b>注意角度必须在 {@link #tick} 里推，不能放 {@link #update}。</b>
 * Flywheel 的 {@code Visual.update(float)} 只在视觉对象被创建或被
 * {@code VisualManager.queueUpdate} 显式排队时调用，<b>不是每帧/每刻的钩子</b>；
 * 真正的周期钩子是 {@code TickableVisual.tick} / {@code DynamicVisual.beginFrame}。
 * 放错地方的表现就是"接了动力也不转"。
 */
public class MilitaryFactoryVisual extends KineticBlockEntityVisual<MilitaryFactoryBulletsBlockEntity>
        implements SimpleTickableVisual
{
    /** 本机的旋转轴是竖直的，见 {@code MilitaryFactoryBulletsBlock.getRotationAxis}。 */
    private static final Direction.Axis AXIS = Direction.Axis.Y;

    private final RotatingInstance spin;

    public MilitaryFactoryVisual(VisualizationContext context, MilitaryFactoryBulletsBlockEntity be,
            float partialTick)
    {
        super(context, be, partialTick);

        Instancer<RotatingInstance> instancer = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.ROTATING_BULLET));

        // 速度传 0，把 shader 的「时间 × 速度」积分让掉；朝向由 tick() 每刻写进 rotationOffset。
        // 不需要 rotateToFace：整组按「朝上」建模，绕自身 Y 轴转就是对的。
        spin = instancer.createInstance()
                .setup(be, AXIS, 0f)
                .setPosition(getVisualPosition());
    }

    /** 每游戏刻推进角度。这是唯一的周期钩子，见类注释。 */
    @Override
    public void tick(TickableVisual.Context context)
    {
        spin.setRotationOffset(blockEntity.getRenderAngle());
        applyColor();
        spin.setChanged();
    }

    /**
     * 只在被显式刷新时调用（建号、方块实体同步）。
     *
     * <p>这里<b>不能</b>调 {@code setup(blockEntity)} —— 它会把 rotationalSpeed 和
     * rotationOffset 一起覆盖掉，等于把我们手工驱动的角度抹平。只重写一次当前角度即可。
     */
    @Override
    public void update(float partialTick)
    {
        spin.setRotationOffset(blockEntity.getRenderAngle())
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
     * 着色。过载时整组变红，其余情况白色；调试器的网络调试色只在调试开关打开时才上。
     * 这样玩家不用戴护目镜也能一眼看出机器不对劲。
     *
     * <p>这里比 Create 原版要简单。它是在<b>过载状态发生变化时闪一下</b>
     * （红 = 刚转入过载，绿 = 刚恢复），由 {@code KineticBlockEntity.effects.overStressedEffect}
     * 驱动 —— 但那个字段在 {@code KineticEffectHandler} 里是包级私有，而 {@code effects}
     * 本身又是 protected，只有 {@code com.simibubi.create.content.kinetics.base} 包内的类读得到
     * （Create 自己的 {@code SingleAxisRotatingVisual} 恰好就在那个包里）。
     * 外部模组拿不到，所以退化成"过载期间持续变红" —— 信息量相同，只是不再是短促的脉冲。
     *
     * <p>{@code colorFromBE} <b>不能</b>无条件调用：它是拿网络 ID 生成的一个调试色，
     * 不是过载红，无条件调用会把整发布满该网络的调试色（实测是一层绿）。
     */
    private void applyColor()
    {
        if (blockEntity.isOverStressed())
        {
            spin.setColor(Color.RED);
        }
        else if (KineticDebugger.isActive())
        {
            spin.setColor(blockEntity);
        }
        else
        {
            spin.setColor(Color.WHITE);
        }
    }
}
