package com.frnc.create_tacz.client;

import java.util.function.Consumer;

import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

/**
 * 军工厂：子弹的活动部件。
 *
 * <p>两个旋转件，绕同一根竖直轴以同一速度转：
 * <ol>
 *   <li><b>传动轴轴头</b> —— {@code create_tacz:block/shaft_stub}，从底面伸进来的一小截 4×4 轴。
 *       Create 的动力方块，传动轴一律由渲染器 / visual 画出来，<b>不是</b>建模进方块模型里的
 *       （见 {@code MechanicalPressRenderer} 把 {@code getRenderedBlockState} 覆写成
 *       {@code shaft(axis)}，{@code CreativeMotorRenderer} 用 {@code SHAFT_HALF}）。
 *       Create 没有短的通用轴局部模型，但它自己的机器也用机器专用的短轴头
 *       （{@code powered_shaft} 就是 2 像素），这里照做；</li>
 *   <li><b>展示组</b> —— 黄铜卡盘 + 一发狙击弹，我们自己的局部模型
 *       {@code create_tacz:block/rotating_bullet}。</li>
 * </ol>
 *
 * <h2>转速：跟着输入转速走，且只在真正加工时转</h2>
 * <b>不工作时两个旋转件都完全静止</b> —— 通了电但没材料、没锁定配方、或转速不到 16 RPM，
 * 它们都不转。一旦真正在加工，转速就跟着输入转速 1:1 走，上限 32 RPM。
 * 判据是 {@link MilitaryFactoryBulletsBlockEntity#isWorking()}，
 * 具体转速由方块实体算好后通过 {@code getRenderAngle()} 交给这里。
 *
 * <p>"运行中"这一个状态同时驱动了旋转与隐藏式字幕，两者不会各说各话。
 *
 * <h2>角度为什么要自己算</h2>
 * Create 自带的 {@code SingleAxisRotatingVisual} 每次只调 {@code setup(be, axis, speed)}，
 * 而 Flywheel 的 {@code rotating.vert} 里角度是
 * {@code offset + 全局渲染时间 × speed} 由 shader 积出来的 —— {@code offset} 只是个恒定的
 * 齿对齐偏移。那样一旦速度变化或断开，角度会<b>整体跳变</b>，因为它和绝对时间成正比。
 *
 * <p>所以这里给 {@code rotationalSpeed} 传 <b>0</b>，把 shader 那套积分让掉，
 * 改由 {@code rotationOffset} 承载角度，来源是方块实体的 {@code getRenderAngle()}。
 * 好处是停机时部件<b>停在原地</b>，再开机接着转，不会弹回某个固定朝向。
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

    /** 传动轴轴头：从底面往上的一小截。 */
    private final RotatingInstance shaft;

    /** 展示组：黄铜卡盘 + 子弹。 */
    private final RotatingInstance spin;

    public MilitaryFactoryVisual(VisualizationContext context, MilitaryFactoryBulletsBlockEntity be,
            float partialTick)
    {
        super(context, be, partialTick);

        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.SHAFT_STUB))
                .createInstance()
                // 模型本身就是从底面往上伸的一小截，不需要任何朝向修正
                // （Create 的 SHAFT_HALF 是朝南伸出的，才需要 rotateTo / partialFacingVertical）。
                .setup(be, AXIS, 0f)
                .setPosition(getVisualPosition());

        spin = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(ModPartialModels.ROTATING_BULLET))
                .createInstance()
                // 速度传 0，把 shader 的「时间 × 速度」积分让掉；朝向由 tick() 每刻写进 rotationOffset。
                // 不需要 rotateToFace：整组按「朝上」建模，绕自身 Y 轴转就是对的。
                .setup(be, AXIS, 0f)
                .setPosition(getVisualPosition());

        shaft.setChanged();
        spin.setChanged();
    }

    /** 每游戏刻推进角度。这是唯一的周期钩子，见类注释。 */
    @Override
    public void tick(TickableVisual.Context context)
    {
        float angle = blockEntity.getRenderAngle();

        shaft.setRotationOffset(angle);
        spin.setRotationOffset(angle);

        applyColor();

        shaft.setChanged();
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
        float angle = blockEntity.getRenderAngle();

        shaft.setRotationOffset(angle)
                .setChanged();
        spin.setRotationOffset(angle)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick)
    {
        relight(shaft, spin);
    }

    /** 方块被破坏时的碎裂贴图要跟着这些旋转件一起闪。 */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer)
    {
        consumer.accept(shaft);
        consumer.accept(spin);
    }

    @Override
    protected void _delete()
    {
        shaft.delete();
        spin.delete();
    }

    /**
     * 着色。过载时整体变红，其余情况白色；调试器的网络调试色只在调试开关打开时才上。
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
        Color color;
        if (blockEntity.isOverStressed())
        {
            color = Color.RED;
        }
        else if (KineticDebugger.isActive())
        {
            shaft.setColor(blockEntity);
            spin.setColor(blockEntity);
            return;
        }
        else
        {
            color = Color.WHITE;
        }

        shaft.setColor(color);
        spin.setColor(color);
    }
}
