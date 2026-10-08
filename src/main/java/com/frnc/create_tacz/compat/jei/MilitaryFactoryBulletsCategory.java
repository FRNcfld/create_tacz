package com.frnc.create_tacz.compat.jei;

import java.util.List;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.registry.ModBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 里的"军工厂：子弹"分类。
 *
 * <p>内容就是这台机器能做的全部弹药配方 —— 与方块实体用的是同一套筛选口径
 * （TaCZ 的 {@code tacz:gun_smith_table_crafting} 里产出是弹药的那些），
 * 所以 JEI 上看到什么，机器上就能做什么。
 *
 * <p>滤波槽不在这个分类里体现：机器一次只做一种口径，那由方块上的列表过滤器决定，
 * 属于运行时配置而不是配方本身。
 */
public class MilitaryFactoryBulletsCategory implements IRecipeCategory<MilitaryFactoryBulletsJeiRecipe>
{
    public static final RecipeType<MilitaryFactoryBulletsJeiRecipe> RECIPE_TYPE =
            RecipeType.create(CreateTacz.MOD_ID, "military_factory_bullets", MilitaryFactoryBulletsJeiRecipe.class);

    private static final int WIDTH = 100;
    private static final int HEIGHT = 56;

    private static final int INPUT_X = 1;
    private static final int OUTPUT_X = WIDTH - 20;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 18;
    private static final int ARROW_X = 42;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public MilitaryFactoryBulletsCategory(IGuiHelper guiHelper)
    {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.MILITARY_FACTORY_BULLETS.get()));
        this.arrow = guiHelper.getRecipeArrow();
        this.title = Component.translatable("jei.create_tacz.military_factory_bullets");
    }

    @Override
    public RecipeType<MilitaryFactoryBulletsJeiRecipe> getRecipeType()
    {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle()
    {
        return title;
    }

    /**
     * 尺寸必须自己给。
     *
     * <p>JEI 15.20 起 {@code getBackground()} 已标记为待删除，背景改由各槽位自带的
     * {@code setStandardSlotBackground()} / {@code setOutputSlotBackground()} 提供。
     * 而 {@code getWidth()} / {@code getHeight()} 的默认实现是去问 {@code getBackground()}，
     * 问不到就直接抛 {@code IllegalStateException} —— 所以这两个必须覆写。
     */
    @Override
    public int getWidth()
    {
        return WIDTH;
    }

    @Override
    public int getHeight()
    {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MilitaryFactoryBulletsJeiRecipe recipe, IFocusGroup focuses)
    {
        List<List<ItemStack>> inputs = recipe.inputs();
        int columnTop = columnTop(inputs.size());

        for (int i = 0; i < inputs.size(); i++)
        {
            builder.addInputSlot(INPUT_X, columnTop + i * SLOT_SPACING)
                    .setStandardSlotBackground()
                    .addItemStacks(inputs.get(i));
        }

        builder.addOutputSlot(OUTPUT_X, columnTop + (columnHeight(inputs.size()) - SLOT_SIZE) / 2)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(MilitaryFactoryBulletsJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
            double mouseX, double mouseY)
    {
        int columnTop = columnTop(recipe.inputs().size());
        int middle = columnTop + columnHeight(recipe.inputs().size()) / 2;
        arrow.draw(graphics, ARROW_X, middle - arrow.getHeight() / 2);
    }

    /** 输入列的高度：n 个槽位加上它们之间的间距。 */
    private static int columnHeight(int inputCount)
    {
        return (inputCount - 1) * SLOT_SPACING + SLOT_SIZE;
    }

    /** 整列在分类里垂直居中，这样 1 个材料和多材料看起来都不会偏。 */
    private static int columnTop(int inputCount)
    {
        return (HEIGHT - columnHeight(inputCount)) / 2;
    }
}
