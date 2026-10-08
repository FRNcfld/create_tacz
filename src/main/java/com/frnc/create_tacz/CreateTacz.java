package com.frnc.create_tacz;

import com.frnc.create_tacz.registry.ModBlockEntities;
import com.frnc.create_tacz.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tacz.guns.init.ModCreativeTabs;

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

    /**
     * 本模组的 Registrate 实例，全部内容都从这里注册。
     *
     * <p>附属模组<b>只能</b>用这个工厂方法创建：{@code Create.registrate()} 带调用方包名校验，
     * 非 {@code com.simibubi.create} 包调用会直接抛 {@code UnsupportedOperationException}。
     */
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateTacz(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 必须显式接上事件总线，否则 Registrate 注册的任何东西都不会进注册表。
        REGISTRATE.registerEventListeners(modEventBus);

        // 注册链写在静态字段的初始化表达式里，而静态字段是惰性初始化的 ——
        // 不主动触碰这些类，它们就永远不会初始化，方块与方块实体一个都不会注册。
        ModBlocks.register();
        ModBlockEntities.register();

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
                .equals(ModCreativeTabs.OTHER_TAB.getKey()))
            event.accept(ModBlocks.MILITARY_FACTORY_BULLETS.get());
    }

    /**
     * 应力消耗必须在 FMLCommonSetupEvent 里登记，不能放在构造器 ——
     * 那时 Forge 的注册表还没填充，{@code ModBlocks.MILITARY_FACTORY_BULLETS.get()} 拿不到实例。
     *
     * <p>{@code BlockStressValues.IMPACTS} 是附属模组唯一公开且稳定的应力入口：
     * Create 自己的 {@code CStress} 带 {@code assertFromCreate} 校验，非 Create 方块会抛异常。
     * 值是 1 RPM 下的基础消耗，正数表示"消耗"。
     *
     * <p>SimpleRegistry 本身就是线程安全的（它的类注释明确写了可在并行 mod init 中使用），
     * 所以不需要再包一层 {@code event.enqueueWork}。
     */
    private void commonSetup(FMLCommonSetupEvent event)
    {
        BlockStressValues.IMPACTS.register(ModBlocks.MILITARY_FACTORY_BULLETS.get(), () -> 8.0);
    }
}
