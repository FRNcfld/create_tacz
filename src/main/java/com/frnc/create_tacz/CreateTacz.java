package com.frnc.create_tacz;

import com.frnc.create_tacz.registry.ModBlockEntities;
import com.frnc.create_tacz.registry.ModBlocks;
import com.frnc.create_tacz.registry.ModCreativeTabs;
import com.frnc.create_tacz.registry.ModItems;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreateTacz.MOD_ID)
public class CreateTacz
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "create_tacz";

    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateTacz(FMLJavaModLoadingContext context)
    {
        // 后续的 DeferredRegister 一律注册到 context.getModEventBus()
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Create TaCZ initialized");
    }

    /**
     * 应力消耗必须在 FMLCommonSetupEvent 里登记，不能放在构造器 ——
     * 那时 Forge 的注册表还没填充，ModBlocks.MILITARY_FACTORY_BULLETS.get() 拿不到实例。
     *
     * <p>Create 的 BlockStressValues.IMPACTS 是普通显式注册表，get() 会先查显式注册
     * 再看缓存值，所以这里晚注册依然生效。正数表示"消耗"，单位是每 RPM。
     */
    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> BlockStressValues.IMPACTS.register(
                ModBlocks.MILITARY_FACTORY_BULLETS.get(), () -> 8.0));
    }
}
