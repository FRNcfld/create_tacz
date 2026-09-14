package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.registry.ModBlocks;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateTacz.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // 贴图带二值透明像素（玻璃部分是 alpha=0）。默认的 solid 渲染层会把它们
        // 画成不透明的黑块，必须切到 cutout 才会真的挖空。
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.MILITARY_FACTORY_BULLETS.get(), RenderType.cutout()));
    }

    private ClientSetup()
    {
    }
}
