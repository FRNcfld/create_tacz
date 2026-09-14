package com.frnc.create_tacz;

import com.frnc.create_tacz.registry.ModBlockEntities;
import com.frnc.create_tacz.registry.ModBlocks;
import com.frnc.create_tacz.registry.ModCreativeTabs;
import com.frnc.create_tacz.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
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

        LOGGER.info("Create TaCZ initialized");
    }
}
