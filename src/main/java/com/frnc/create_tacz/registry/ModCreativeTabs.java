package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateTacz.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateTacz.MOD_ID))
                    .icon(() -> new ItemStack(ModBlocks.MILITARY_FACTORY_BULLETS.get()))
                    .displayItems((parameters, output) -> output.accept(ModBlocks.MILITARY_FACTORY_BULLETS.get()))
                    .build());

    private ModCreativeTabs()
    {
    }
}
