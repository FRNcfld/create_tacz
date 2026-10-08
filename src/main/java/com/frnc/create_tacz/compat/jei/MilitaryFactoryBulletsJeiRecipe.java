package com.frnc.create_tacz.compat.jei;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * "军工厂：子弹"在 JEI 里显示一条配方所需的全部数据。
 *
 * <p>刻意做成一个自己的 record，而不是直接复用 TaCZ 的 {@code GunSmithTableRecipe}：
 * <ul>
 *   <li>TaCZ 的配方对象带着 Ingredient + 数量，JEI 的槽位需要的是"每个槽放哪些候选物品"，
 *       两者形状不一样，转换一次比在渲染时反复转换清楚；</li>
 *   <li>数量已经烘进物品堆的 count 里（见 {@code CreateTaczJeiPlugin#toJeiRecipe}），
 *       因为 JEI 的槽位本身不表达"需要几个"，只能靠物品堆的堆叠数显示。</li>
 * </ul>
 *
 * @param inputs 每个元素是一个输入槽的候选物品（同一个槽的多个候选 = 标签/多选一）
 * @param output 产出。TaCZ 的子弹口径存在 NBT 上，所以这个物品堆自带 NBT
 */
public record MilitaryFactoryBulletsJeiRecipe(List<List<ItemStack>> inputs, ItemStack output)
{
}
