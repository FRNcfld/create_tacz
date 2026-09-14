package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateTacz.MOD_ID);

    public static final RegistryObject<Block> MILITARY_FACTORY_BULLETS = BLOCKS.register(
            "military_factory_bullets",
            () -> new MilitaryFactoryBulletsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    // 掉落物需要「正确的工具」：配合 mineable/pickaxe 与 needs_iron_tool
                    // 两个方块标签，实际效果就是铁镐及以上才能挖出东西。
                    .requiresCorrectToolForDrops()));

    private ModBlocks()
    {
    }
}
