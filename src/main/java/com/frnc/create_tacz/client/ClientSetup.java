package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化。
 *
 * <p>现在只剩一件事：局部模型的时机。渲染层、visual、渲染器都已经挪到
 * {@code ModBlockEntities} 的 Registrate 注册链上
 * （{@code .addLayer()} / {@code .visual()} / {@code .renderer()}），
 * 不再需要在这里手写 {@code ItemBlockRenderTypes.setRenderLayer}、
 * {@code SimpleBlockEntityVisualizer} 和 {@code EntityRenderersEvent.RegisterRenderers}。
 */
@Mod.EventBusSubscriber(modid = CreateTacz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // 关键顺序：PartialModel 必须在资源重载（模型烘焙）之前创建，
        // 否则 Flywheel 取不到烘焙结果，会回落到 missing model ——
        // 一个整格紫黑方块盖住整台机器。理由见 ModPartialModels 的类注释。
        //
        // 刻意【不】放进 event.enqueueWork(...)：Create 自己也是在
        // CreateClient.clientInit 里直接调 AllPartialModels.init() 的。
        ModPartialModels.init();
    }
}
