package com.frnc.create_tacz.client;

import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * 军工厂：子弹的方块实体渲染器。
 *
 * <h2>职责划分（Create 的每台机器都是这两件事）</h2>
 * <ul>
 *   <li><b>渲染器</b>（本类）：滤波槽里的幽灵物品，以及 Flywheel 不生效时的旋转组回退。</li>
 *   <li><b>visual</b>（{@link MilitaryFactoryVisual}）：Flywheel 生效时的旋转组。</li>
 * </ul>
 *
 * <p>滤波槽必须由渲染器画 —— Flywheel 的 visual 系统不画 {@code ValueBox} / 物品，
 * 所以这一步不能省，也不能把本渲染器整个交给 Flywheel 跳过
 * （Registrate 那边用的 {@code renderNormally = true} 正是为此）。
 *
 * <p>回退分支参考 Create 的 {@code SawRenderer}：永远先画该画的，
 * 再 {@code supportsVisualization} 提前返回，最后画旋转件。
 */
public class MilitaryFactoryRenderer extends SafeBlockEntityRenderer<MilitaryFactoryBulletsBlockEntity>
{
    public MilitaryFactoryRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    protected void renderSafe(MilitaryFactoryBulletsBlockEntity be, float partialTicks, PoseStack ms,
            MultiBufferSource buffer, int light, int overlay)
    {
        // 滤波槽幽灵物品：与 Flywheel 无关，每次都要画。
        FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);

        // Flywheel 接管时就让位，避免和 visual 双重绘制。
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        // 回退路径：自己把旋转组画出来。
        //
        // 少了这一段，在 Flywheel 不生效的环境里（老显卡、关掉后端等）旋转组会整个消失 ——
        // 机壳和玻璃还在，只有传动杆、卡盘和子弹不见了，而且不报任何错。
        //
        // 角度取自方块实体的 renderAngle，与 visual 用的是同一个来源，
        // 所以开关 Flywheel 看到的是同一种转速。
        // kineticRotationTransform 接受显式角度，并顺带处理了调试上色与过载红/绿着色。
        SuperByteBuffer model = CachedBuffers.partial(ModPartialModels.ROTATING_BULLET, be.getBlockState());
        KineticBlockEntityRenderer.kineticRotationTransform(
                        model, be, Direction.Axis.Y, AngleHelper.rad(be.getRenderAngle()), light)
                .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }
}
