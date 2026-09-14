package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateTacz.MOD_ID);

    public static final RegistryObject<Item> MILITARY_FACTORY_BULLETS = ITEMS.register(
            "military_factory_bullets",
            () -> new BlockItem(ModBlocks.MILITARY_FACTORY_BULLETS.get(), new Item.Properties()));

    private ModItems()
    {
    }
}
