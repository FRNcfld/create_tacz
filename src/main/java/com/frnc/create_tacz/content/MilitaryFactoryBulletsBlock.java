package com.frnc.create_tacz.content;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * 军用子弹工厂。
 *
 * <p>实现 {@link IWrenchable} 以获得 Create 扳手的拆卸行为。WrenchItem 只在
 * 方块实现该接口时才会走这条路（否则要求方块处于 create:wrench_pickup 标签中），
 * 潜行右键会调用默认的 onSneakWrenched：掉落物直接塞进玩家背包、播放扳手拆除
 * 音效，并抛出 BlockEvent.BreakEvent 供其他模组拦截。
 *
 * <p>模型本身是有朝向的（玻璃正面在 -Z），所以做成水平朝向方块，
 * 正面在放置时朝向玩家。
 *
 * <p>BlockEntity 目前不存数据，只是为了满足 Create 径向扳手菜单的硬性要求 ——
 * 详见 {@link MilitaryFactoryBulletsBlockEntity}。
 */
public class MilitaryFactoryBulletsBlock extends HorizontalDirectionalBlock implements IWrenchable, EntityBlock
{
    public MilitaryFactoryBulletsBlock(Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new MilitaryFactoryBulletsBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * 覆写 Create 的默认实现。IWrenchable 的默认版本只在 targetedFace 是垂直轴时才
     * 处理水平朝向，等于只有敲上下两面才会转；这里抄 WrenchableDirectionalBlock 的做法，
     * 让任意面都能转。扳手正对着的那个轴转不动自己，其余情况绕该轴顺时针一格。
     */
    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace)
    {
        Direction facing = originalState.getValue(FACING);

        if (facing.getAxis() == targetedFace.getAxis())
            return originalState;

        return originalState.setValue(FACING, facing.getClockWise(targetedFace.getAxis()));
    }

    // rotate / mirror 由 HorizontalDirectionalBlock 提供，不需要覆写。
}
