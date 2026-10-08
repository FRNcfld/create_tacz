package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.client.ponder.CreateTaczPonderPlugin;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化。
 *
 * <p>现在只剩两件事：局部模型的时机，和思索插件的注册。渲染层、visual、渲染器都已经挪到
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

        // 思索插件必须在这里挂上。Ponder 的注册时机是 FMLLoadCompleteEvent
        // （见 ForgePonderClient.ModBusClientEvents#loadCompleted），那是整个模组加载的
        // 最后一个事件，而 FMLClientSetupEvent 一定在它之前 —— 所以只要写在这里就不会漏。
        // Create 自己也是这么做的（CreateClient.clientInit 里的 PonderIndex.addPlugin）。
        //
        // 挂在这里同时保证了类型不会泄漏到服务端：本类是 Dist.CLIENT + MOD 总线的事件处理器，
        // 专用服务器不会加载它，CreateTaczPonderPlugin 及其场景类也就不会被触碰。
        PonderIndex.addPlugin(new CreateTaczPonderPlugin());
    }
}
