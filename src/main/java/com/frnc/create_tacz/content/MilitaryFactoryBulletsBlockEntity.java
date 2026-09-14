package com.frnc.create_tacz.content;

import com.frnc.create_tacz.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 目前不存任何数据，它的存在本身就是为了 Create 的径向扳手菜单。
 *
 * <p>RadialWrenchMenu 是按「有 BlockEntity 的机器」写的，renderRadialSectors 里有
 * 一句没有空判断的 blockEntity.getLevel()。方块没有 BlockEntity 时这里会抛 NPE，
 * 被外层的 catch (Exception) 吞掉，而 catch 分支每帧执行一次 allStates.remove(i)，
 * 几帧之后候选状态就被删空，菜单闪一下就没内容了，再按下去 submitChange 还会
 * 因为 allStates.get(0) 越界。
 *
 * <p>以后做成真正的加工机器时，库存/进度这些状态直接加在这里即可。
 */
public class MilitaryFactoryBulletsBlockEntity extends BlockEntity
{
    public MilitaryFactoryBulletsBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.MILITARY_FACTORY_BULLETS.get(), pos, state);
    }
}
