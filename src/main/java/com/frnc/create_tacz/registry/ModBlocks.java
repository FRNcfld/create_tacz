package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlock;

import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 方块注册。
 *
 * <p>走 Create 自己的 {@code CreateRegistrate} —— Create 全家的方块都是这么注册的。
 *
 * <p><b>哪些调用运行期生效、哪些只在 datagen 生效：</b>
 * Registrate 的 {@code .blockstate()} / {@code .tag()} / {@code .lang()} /
 * {@code .transform(...)} 只在 {@code runData} 阶段产出资源文件，<b>运行期是空操作</b>。
 * 本项目不跑 datagen，所以 blockstate / item model / loot table / 方块标签依然是
 * {@code src/main/resources} 下手写的 JSON。不要改用那些调用 —— 它们不会报错，
 * 只会让代码看起来「已经处理了」而实际什么都没发生。
 *
 * <p>运行期真正生效的只有三样：注册本身、{@code .simpleItem()}、以及 {@code .addLayer(...)}。
 */
public class ModBlocks
{
    public static final BlockEntry<MilitaryFactoryBulletsBlock> MILITARY_FACTORY_BULLETS =
            CreateTacz.REGISTRATE.block("military_factory_bullets", MilitaryFactoryBulletsBlock::new)
                    // 继承 Create 惯例的基础属性。关键在它带 requiresCorrectToolForDrops() ——
                    // 配合手写的 mineable/pickaxe 与 needs_iron_tool 两个方块标签，
                    // 实际效果就是铁镐及以上才能挖出东西。
                    .initialProperties(SharedProperties::stone)
                    .properties(p -> p
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .sound(SoundType.METAL)
                            // 模型带镂空玻璃，不能让相邻方块把朝向它的面剔除掉，
                            // 否则透过玻璃会看到邻面的空洞。
                            .noOcclusion())
                    // 贴图带二值透明像素（玻璃部分是 alpha=0），必须在 cutout 层渲染，
                    // 否则默认的 solid 层会把它们画成不透明黑块。
                    // 1.20.1 没有数据包方式指定方块的 RenderType，只能运行期设置。
                    .addLayer(() -> RenderType::cutout)
                    .simpleItem()
                    .register();

    /**
     * 唯一作用是强制本类初始化，方法体刻意留空。
     *
     * <p>Registrate 的注册发生在<b>静态字段的初始化表达式</b>里，而静态字段是惰性初始化的：
     * 如果没有任何代码触碰过 {@code ModBlocks}，上面那条 {@code REGISTRATE.block(...)}
     * 就永远不会执行 —— 方块一个都不会注册，而且<b>不会报任何错</b>。
     *
     * <p>Create 自己也是这个写法（{@code AllBlocks.register()} 同样是空方法，
     * 由 {@code Create.onCtor()} 调用）。
     */
    public static void register()
    {
    }

    private ModBlocks()
    {
    }
}
