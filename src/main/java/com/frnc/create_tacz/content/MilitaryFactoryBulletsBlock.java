package com.frnc.create_tacz.content;

import com.frnc.create_tacz.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 军工厂：子弹。
 *
 * <p>动力学机器，从底面接入传动杆。模型自带的朝向由 {@code HORIZONTAL_FACING} 表达，
 * 放置时正面朝向玩家，扳手可以任意面旋转。
 *
 * <p>继承 {@link HorizontalKineticBlock} 之后扳手功能不受影响 —— {@code IRotate}
 * 本身就继承自 {@code IWrenchable}，所以动力学方块天然可被 Create 扳手操作。
 *
 * <p>另外注意 {@code HORIZONTAL_FACING} 和原版 {@code HorizontalDirectionalBlock.FACING}
 * 是同一个属性实例（都指向 {@code BlockStateProperties.HORIZONTAL_FACING}），
 * 所以换成动力学方块后 blockstate 文件和模型都不需要改。
 */
public class MilitaryFactoryBulletsBlock extends HorizontalKineticBlock
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
     * 恒返回 {@link IRotate.SpeedLevel#NONE}，即"本方块不声明转速要求"。
     *
     * <p>Create 的 {@code SpeedLevel} 只有 NONE(0)/SLOW(10)/MEDIUM(20)/FAST(30) 四档，
     * 表达不了实际的 16 RPM。而它唯一的消费点
     * {@code KineticBlockEntity.isSpeedRequirementFulfilled()}（{@code |speed| >= 档位值}）
     * 又是 Create 那行"转速不够"护目镜提示的开关。返回 NONE 会让该判断恒为 true，
     * 那行说不出具体数字的提示就永远不会出现，改由 BlockEntity 覆写的
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

    // 这里刻意不覆写 IWrenchable.getRotatedBlockState。
    //
    // 曾经覆写成"任意面都能转"（照抄 WrenchableDirectionalBlock），那是错的 ——
    // 它操作的是 DirectionalBlock.FACING（六向，含 up/down），所以 getClockWise 随便转都合法；
    // 而 HORIZONTAL_FACING 只有四个水平值，绕 X/Z 轴旋转会得到 UP/DOWN，
    // setValue 直接抛 IllegalArgumentException 导致客户端崩溃。
    //
    // 默认实现只在 targetedFace 是垂直轴时才转水平朝向，也就是敲上下两面才转，
    // 这对水平朝向方块是唯一合理的语义 —— 绕水平轴转等于把方块翻倒，本来就不可能。

    // createBlockStateDefinition / getStateForPlacement / rotate / mirror 都由
    // HorizontalKineticBlock 提供，不需要覆写。

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
