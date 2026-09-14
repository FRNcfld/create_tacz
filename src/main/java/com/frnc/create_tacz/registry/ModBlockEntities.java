package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateTacz.MOD_ID);

    public static final RegistryObject<BlockEntityType<MilitaryFactoryBulletsBlockEntity>> MILITARY_FACTORY_BULLETS =
            BLOCK_ENTITIES.register("military_factory_bullets",
                    () -> BlockEntityType.Builder.of(MilitaryFactoryBulletsBlockEntity::new,
                            ModBlocks.MILITARY_FACTORY_BULLETS.get()).build(null));

    private ModBlockEntities()
    {
    }
}
