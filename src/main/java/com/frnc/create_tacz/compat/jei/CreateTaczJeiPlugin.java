package com.frnc.create_tacz.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.registry.ModBlocks;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * JEI 联动。
 *
 * <p>两件事：
 * <ol>
 *   <li>新增"军工厂：子弹"分类，列出这台机器能做的全部弹药配方；</li>
 *   <li>把本方块注册为 TaCZ 原有"枪械工作台"分类的催化剂 ——
 *       玩家翻 TaCZ 的子弹配方时能一眼看到这台机器也能做。</li>
 * </ol>
 *
 * <h2>为什么整个类只依赖 {@code compileOnly} 的 JEI 也不会出问题</h2>
 * JEI 是软依赖。这个类只会被 JEI 自己在扫描 {@code @JeiPlugin} 时加载，
 * 公共代码路径完全不引用它，所以没装 JEI 时这个类永远不会被初始化，
 * 里面引用到的 JEI 类型（以及 {@code Minecraft}）都不会触发类加载。
 */
@JeiPlugin
public class CreateTaczJeiPlugin implements IModPlugin
{
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CreateTacz.MOD_ID, "jei_plugin");

    /**
     * TaCZ 自己那个"枪械工作台"JEI 分类的 RecipeType。
     *
     * <p><b>这个 uid 不是猜的</b>，而是按 TaCZ {@code GunModPlugin.registerCategories}
     * 里完全相同的算法重算出来的：
     * <pre>
     * RecipeType.create("tacz",
     *     "gun_smith_table/" + 工作台方块id.toString().replace(':', '_'),
     *     GunSmithTableRecipe.class)
     * </pre>
     * 默认枪包的工作台方块是 {@code tacz:gun_smith_table}，代入即得
     * {@code tacz:gun_smith_table/tacz_gun_smith_table}。
     *
     * <p>JEI 的 {@code RecipeType} 相等性<b>只看 uid</b>（{@code equals} / {@code hashCode}
     * 都基于它），所以这里自己构造的实例与 TaCZ 注册的那个是同一个类型，
     * 可以直接拿来注册催化剂。
     *
     * <p><b>已知局限</b>：第三方枪包如果定义了自己的工作台方块，会算出一个不同的 uid，
     * 本机就不会出现在那些分类的催化剂里。这属于信息缺失而非功能错误 ——
     * 本机自己的分类始终可用。
     */
    private static final RecipeType<GunSmithTableRecipe> TACZ_GUN_SMITH_TABLE =
            RecipeType.create("tacz", "gun_smith_table/tacz_gun_smith_table", GunSmithTableRecipe.class);

    @Override
    public ResourceLocation getPluginUid()
    {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        registration.addRecipeCategories(
                new MilitaryFactoryBulletsCategory(registration.getJeiHelpers()
                        .getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        registration.addRecipes(MilitaryFactoryBulletsCategory.RECIPE_TYPE, collectRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        ItemStack machine = new ItemStack(ModBlocks.MILITARY_FACTORY_BULLETS.get());

        // 本机自己的分类
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, machine,
                MilitaryFactoryBulletsCategory.RECIPE_TYPE);

        // 顺带挂到 TaCZ 的枪械工作台分类上
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, machine, TACZ_GUN_SMITH_TABLE);
    }

    /**
     * 收集这台机器能做的配方。
     *
     * <p>筛选口径与方块实体里的 {@code getAmmoRecipes()} 保持一致：TaCZ 的
     * {@code tacz:gun_smith_table_crafting} 里<b>产出是弹药</b>的那些，枪和配件不算。
     * 两处若哪天要改，记得一起改，否则 JEI 上会看到机器做不出来的配方。
     *
     * <p>注册阶段客户端已经拿到同步过来的配方表，所以可以直接问 {@code RecipeManager}。
     */
    private static List<MilitaryFactoryBulletsJeiRecipe> collectRecipes()
    {
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return List.of();

        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get())
                .stream()
                .filter(recipe -> IAmmo.getIAmmoOrNull(recipe.getResultItem(level.registryAccess())) != null)
                .map(recipe -> toJeiRecipe(recipe, level))
                .toList();
    }

    /**
     * TaCZ 配方 → JEI 展示数据。
     *
     * <p>数量烘进物品堆的堆叠数：JEI 的槽位本身不表达"需要几个"，
     * 只能靠物品堆的 count 显示出来。
     */
    private static MilitaryFactoryBulletsJeiRecipe toJeiRecipe(GunSmithTableRecipe recipe, Level level)
    {
        List<List<ItemStack>> inputs = new ArrayList<>();

        for (GunSmithTableIngredient input : recipe.getInputs())
        {
            Ingredient ingredient = input.getIngredient();
            int count = input.getCount();

            List<ItemStack> candidates = new ArrayList<>();
            for (ItemStack stack : ingredient.getItems())
            {
                ItemStack copy = stack.copy();
                copy.setCount(count);
                candidates.add(copy);
            }
            inputs.add(candidates);
        }

        return new MilitaryFactoryBulletsJeiRecipe(inputs, recipe.getResultItem(level.registryAccess())
                .copy());
    }
}
