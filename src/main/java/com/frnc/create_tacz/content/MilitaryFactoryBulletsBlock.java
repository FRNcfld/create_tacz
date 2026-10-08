package com.frnc.create_tacz.content;

import com.frnc.create_tacz.registry.ModBlockEntities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * 军工厂：子弹。
 *
 * <p>动力学机器，旋转轴竖直（Y），只能从<b>底面</b>接传动杆。
 *
 * <h2>为什么是 KineticBlock 而不是 HorizontalKineticBlock</h2>
 * Create 的两台竖直轴机器 —— {@code MillstoneBlock} 与 {@code MechanicalMixerBlock} ——
 * 都是纯 {@link KineticBlock}，<b>没有</b> FACING 属性：竖直轴机器本来就没有"正面"这个概念。
 * 本机的模型（机壳 / 底座 / 接口 / 玻璃）是四重旋转对称的，FACING 既不影响外观，也只会换来
 * 一个扳手能转、转了却看不见任何变化的状态，以及多一倍的 blockstate 变体 —— 纯属死状态。
 *
 * <p>{@code HorizontalKineticBlock} 是给"有功能性朝向"的机器用的（比如朝向某面切的锯子），
 * 本机不属于那一类。
 *
 * <p>另外，{@code IRotate} 本身就继承自 {@code IWrenchable}，所以继承 KineticBlock 之后
 * 扳手功能不受影响 —— 动力学方块天然可被 Create 扳手操作。
 *
 * <p><b>注意</b>：去掉 FACING 之后，blockstate 文件里的四个朝向变体也必须跟着改成单变体，
 * 否则引用了不存在的属性，资源加载阶段会直接报错。
 */
public class MilitaryFactoryBulletsBlock extends KineticBlock
        implements IBE<MilitaryFactoryBulletsBlockEntity>
{
    public MilitaryFactoryBulletsBlock(Properties properties)
    {
        super(properties);
    }

    /** 竖直轴，配合下面的 hasShaftTowards 实现"只能从底面接传动杆"。 */
    @Override
    public Direction.Axis getRotationAxis(BlockState state)
    {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face)
    {
        return face == Direction.DOWN;
    }

    /**
     * 恒返回 {@link IRotate.SpeedLevel#NONE}，含义是"本方块不向 Create 声明转速档位要求"。
     *
     * <p><b>不要把它读成"要求 0 RPM"。</b>{@code SpeedLevel} 只有 NONE(0)/SLOW(1)/MEDIUM/FAST 四档：
     * SLOW 硬编码为 1，MEDIUM/FAST 取自 Create 的服务端配置，<b>表达不了实际的 16 RPM</b>。
     * 而它唯一的消费点 {@code KineticBlockEntity.isSpeedRequirementFulfilled()}
     * （{@code |speed| >= 档位值}）又是 Create 那行"转速不够"护目镜提示的开关。返回 NONE
     * 会让该判断恒为 true，于是那行说不出具体数字的提示不再出现，改由 BlockEntity 覆写的
     * {@code addToTooltip} 给出带具体转速的版本。
     *
     * <p><b>真正的 16 RPM 门槛在 BlockEntity 的 MIN_SPEED</b>，不依赖这里 ——
     * 这个方法对本方块只有"要不要让 Create 显示它那行提示"这一个作用。
     */
    @Override
    public IRotate.SpeedLevel getMinimumRequiredSpeedLevel()
    {
        return IRotate.SpeedLevel.NONE;
    }

    /** 整台机器是实心的，不让寻路穿过。Millstone 与 Mixer 也都覆写了这个。 */
    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type)
    {
        return false;
    }

    // createBlockStateDefinition / getStateForPlacement / rotate / mirror 都不需要覆写 ——
    // 本机没有任何朝向属性，KineticBlock 的默认行为已经够用。

    @Override
    public Class<MilitaryFactoryBulletsBlockEntity> getBlockEntityClass()
    {
        return MilitaryFactoryBulletsBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MilitaryFactoryBulletsBlockEntity> getBlockEntityType()
    {
        return ModBlockEntities.MILITARY_FACTORY_BULLETS.get();
    }

    // IBE 还白送两件事，都不用自己写：
    //  - getTicker 默认返回 SmartBlockEntityTicker，所以 BE 的 tick() 会被调用
    //  - KineticBlock.onRemove 内部已经调用 IBE.onRemove -> SmartBlockEntity.destroy()，
    //    所以掉落内容物只需要 BE 覆写 destroy()，方块类不用覆写 onRemove
}
