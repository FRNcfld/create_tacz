package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.registry.ModBlockEntities;
import com.frnc.create_tacz.registry.ModBlocks;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateTacz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // 关键顺序：PartialModel 必须在资源重载（模型烘焙）之前创建，
        // 否则 Flywheel 取不到烘焙结果，会回落到 missing model ——
        // 一个整格紫黑方块盖住整台机器。理由见 MilitaryFactoryVisual#init。
        MilitaryFactoryVisual.init();

        event.enqueueWork(() ->
        {
            // 贴图带二值透明像素（玻璃部分是 alpha=0）。默认的 solid 渲染层会把它们
            // 画成不透明的黑块，必须切到 cutout 才会真的挖空。
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.MILITARY_FACTORY_BULLETS.get(), RenderType.cutout());

            // 底部应力输入口的传动杆交给 Flywheel 画，它才会跟着转速转。
            //
            // neverSkipVanillaRender() 是关键：默认会跳过方块自身的模型渲染，
            // 那是给「整个模型就是 visual」的方块（比如 Create 的传动杆）用的；
            // 本机的机壳、齿轮、玻璃仍然是普通方块模型，visual 只额外加一根轴。
            //
            // 只能在客户端注册 —— MilitaryFactoryVisual 引用了 AllInstanceTypes
            // 这类纯客户端类，放到公共代码里会在服务端加载时炸掉。
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.MILITARY_FACTORY_BULLETS.get())
                    .factory(MilitaryFactoryVisual::new)
                    .neverSkipVanillaRender()
                    .apply();
        });
    }

    /**
     * Create 用 Registrate 的 {@code .renderer(...)} 给方块实体挂渲染器，我们走等价的
     * Forge 事件。挂 SmartBlockEntityRenderer 是为了让滤波槽里的物品能在方块上显示出来 ——
     * 它内部就是调 {@code FilteringRenderer.renderOnBlockEntity}。
     *
     * <p>滤波槽里放的是 Create 的列表过滤器（普通物品），Create 原生的 FIXED 上下文就能
     * 正常渲染，所以这里不需要自定义渲染器。
     */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlockEntities.MILITARY_FACTORY_BULLETS.get(),
                SmartBlockEntityRenderer::new);
    }
}
