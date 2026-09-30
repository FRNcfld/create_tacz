package com.frnc.create_tacz;

import com.frnc.create_tacz.registry.ModBlockEntities;
import com.frnc.create_tacz.registry.ModBlocks;
import com.frnc.create_tacz.registry.ModItems;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CreateTacz::addToTaczCreativeTab);

        LOGGER.info("Create TaCZ initialized");
    }

    /**
     * 把本方块也挂到 TaCZ 自己的创造模式分页 {@code tacz:other} 下。
     *
     * <p>那个分页装的是枪械工作台、靶子、雕像这些功能性方块 —— 一台"自动量产子弹的机器"
     * 摆在枪械工作台旁边最容易被找到。不挂的话玩家只能在我们的模组分页里翻。
     *
     * <p>用 {@code RegistryObject.getKey()} 取标签键，而不是硬编码 {@code "tacz:other"}：
     * TaCZ 万一改了键名，引用字段会直接编译报错，而字符串比较会静默失效。
     *
     * <p>本模组不再注册自己的创造分页，方块只在这里出现。
     */
    private static void addToTaczCreativeTab(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey()
                .equals(com.tacz.guns.init.ModCreativeTabs.OTHER_TAB.getKey()))
            event.accept(ModItems.MILITARY_FACTORY_BULLETS);
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
