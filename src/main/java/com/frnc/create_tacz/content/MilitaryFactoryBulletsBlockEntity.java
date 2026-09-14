package com.frnc.create_tacz.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.frnc.create_tacz.Config;
import com.frnc.create_tacz.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.filter.ListFilterItem;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 军工厂：子弹的方块实体。
 *
 * <p>继承 {@link KineticBlockEntity}（本身是 {@code SmartBlockEntity} 的子类），
 * 转速、应力、behaviour、NBT 同步这些基础设施都是现成的。应力从底面接入，
 * 由 {@code MilitaryFactoryBulletsBlock.hasShaftTowards} 限定。
 *
 * <h2>滤波槽</h2>
 * 不是物品栏里的格子，而是一个 {@link FilteringBehaviour} —— 存"幽灵物品"，只做匹配标记。
 * 玩家拿<b>列表过滤器</b>（{@code create:filter}）右键顶面锁定配方，空手右键清空；
 * 交互由 Create 的 {@code ValueSettingsInputHandler} 统一处理，本类不写右键逻辑。
 * {@code withPredicate} 保证只收列表过滤器，其他物品会被拒绝并播放拒绝音效。
 * 为什么是列表过滤器而不是直接放子弹，见 {@link #resolveLockedRecipe()}。
 *
 * <h2>物料</h2>
 * <b>没有内部输入格</b>。原料直接从相邻六个面的容器里抽取，产出的子弹放在内部一格缓存里等物流来抽。
 * 这样设计是为了避免内部缓冲被单一材料填满后堵死整条产线 —— 材料始终留在上游容器里，
 * 上游自己会堆积并被察觉，而不是无声地把这台机器噎住。
 */
public class MilitaryFactoryBulletsBlockEntity extends KineticBlockEntity
{
    /**
     * 最低工作转速，硬门槛 —— 低于它机器停机。
     *
     * <p>方块那边 {@code getMinimumRequiredSpeedLevel()} 只能从 Create 的
     * NONE/SLOW(10)/MEDIUM(20)/FAST(30) 四档里选，表达不了 16，所以真正的门槛在这里。
     */
    private static final float MIN_SPEED = 16f;

    /**
     * 基准转速，同时也是耗时上限。
     *
     * <p>配置项 {@code bulletBatchSeconds}（默认 5 秒）就是在这个转速下的耗时；
     * 超过它仍然工作，只是不会再更快。
     */
    private static final float REFERENCE_SPEED = 256f;

    /**
     * 输出缓存格数。
     *
     * <p>这只是<b>缓冲</b>，不是保证：一整批产出放不下时，装不下的部分会进
     * {@link #pendingOutput} 暂存，所以格数只影响"能囤多少"，不影响正确性 ——
     * 无论第三方枪包的批量多大都不会停机。
     */
    private static final int OUTPUT_SLOTS = 9;

    private FilteringBehaviour filter;

    private SmartInventory outputInventory;

    /**
     * 装不进输出格的产出的暂存区。
     *
     * <p>存在的意义：TaCZ 的批量产出量由配方决定、单格容量由该口径的 stack_size 决定，
     * 两者没有任何约束关系（默认枪包里就有 3 个口径的产出大于单格上限）。如果要求
     * "整批必须一次装下"，这些口径的机器会静默停机。改成先收下、装不下的暂存，
     * 下游抽走后再慢慢吐，就不会停机了。
     *
     * <p>暂存非空时不开新的一批 —— 这是背压，防止下游堵死时无限堆积。
     */
    private final List<ItemStack> pendingOutput = new ArrayList<>();

    private LazyOptional<IItemHandler> itemCapability = LazyOptional.empty();

    /** 与当前滤波对应的配方。滤波变化时置为未解析，避免每 tick 全量遍历配方表。 */
    private GunSmithTableRecipe lockedRecipe;
    private boolean lockedRecipeResolved;

    /** 剩余加工 tick；-1 表示未在加工。 */
    private int processingTicks = -1;

    public MilitaryFactoryBulletsBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.MILITARY_FACTORY_BULLETS.get(), pos, state);

        outputInventory = new SmartInventory(OUTPUT_SLOTS, this, 64, true)
                .forbidInsertion();
        itemCapability = LazyOptional.of(() -> outputInventory);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours)
    {
        filter = new FilteringBehaviour(this, new FilterSlot())
                .forRecipes()
                // 只收 Create 的列表过滤器（create:filter），不收裸弹药 —— 原因见 resolveLockedRecipe
                .withPredicate(stack -> stack.getItem() instanceof ListFilterItem)
                .withCallback(stack -> invalidateLockedRecipe());
        behaviours.add(filter);
    }

    // ------------------------------------------------------------------ 物流

    /**
     * 只对外暴露输出格（且它是 forbidInsertion 的），所以物流只能从这台机器抽成品，
     * 塞不进任何东西 —— 原料由机器自己去相邻容器取。
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket)
    {
        super.write(tag, clientPacket);
        tag.put("OutputItems", outputInventory.serializeNBT());

        // 暂存区只有服务端关心（客户端没有 GUI 也不需要它），省一点同步流量
        if (!clientPacket)
        {
            ListTag pending = new ListTag();
            for (ItemStack stack : pendingOutput)
                pending.add(stack.save(new CompoundTag()));
            tag.put("PendingOutput", pending);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket)
    {
        super.read(tag, clientPacket);
        outputInventory.deserializeNBT(tag.getCompound("OutputItems"));

        if (!clientPacket)
        {
            pendingOutput.clear();
            ListTag pending = tag.getList("PendingOutput", Tag.TAG_COMPOUND);
            for (Tag entry : pending)
                pendingOutput.add(ItemStack.of((CompoundTag) entry));
        }
    }

    /**
     * 输出格禁止插入，成品只能被抽走，所以拆方块时要掉落，否则直接消失。
     * 暂存区同理 —— 里面的东西还没进输出格，不主动掉就是凭空蒸发。
     * 这里会被 KineticBlock.onRemove -> IBE.onRemove 自动调用，方块类不用管。
     */
    @Override
    public void destroy()
    {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, outputInventory);

        for (ItemStack stack : pendingOutput)
            Block.popResource(level, worldPosition, stack);
        pendingOutput.clear();
    }

    // ------------------------------------------------------------------ 加工

    @Override
    public void tick()
    {
        super.tick();

        if (level == null || level.isClientSide())
            return;

        // 先把积压的产出冲进输出格。冲不完说明下游堵了，这一 tick 就不再开新的一批。
        flushPendingOutput();
        if (!pendingOutput.isEmpty())
        {
            stopProcessing();
            return;
        }

        // 过载时 getSpeed() 返回 0，所以"应力不足停机"是天然成立的
        float speed = Math.abs(getSpeed());
        if (speed < MIN_SPEED)
        {
            stopProcessing();
            return;
        }

        GunSmithTableRecipe recipe = resolveLockedRecipe();
        // 每 tick 重新规划一次原料来源：既是"材料够不够"的检查，也是材料中途被拿走时的中止保护
        List<Extraction> plan = recipe == null ? null : planExtraction(recipe);
        if (recipe == null || plan == null)
        {
            stopProcessing();
            return;
        }

        if (processingTicks < 0)
        {
            processingTicks = getProcessingTicks(speed);
            sendData();
            return;
        }

        if (--processingTicks <= 0)
        {
            // 到点重新规划再执行，保证扣除的和当初校验的是同一批槽位
            List<Extraction> finalPlan = planExtraction(recipe);
            if (finalPlan != null)
            {
                executeExtraction(finalPlan);
                produce(recipe);
            }
            processingTicks = -1;
            sendData();
        }
    }

    /**
     * 护目镜提示。
     *
     * <p>super 现在只剩过载警告会触发 —— 它的"转速不够"分支因为方块那边
     * {@code getMinimumRequiredSpeedLevel()} 返回 NONE 而永远不会走到
     * （{@code isSpeedRequirementFulfilled()} 恒为 true）。Create 那行只说"转速不够"，
     * 说不出到底要多少，这里换成带具体数字的版本。
     *
     * <p>只在"有转速但不够"时提示。转速为 0 时不提示 —— 要么没接动力、要么被过载压停，
     * 那两种情况都有更合适的提示（或玩家自己看得见），再堆一句"需要 16 RPM"是噪音。
     */
    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking)
    {
        boolean added = super.addToTooltip(tooltip, isPlayerSneaking);

        float speed = Math.abs(getSpeed());
        if (speed > 0 && speed < MIN_SPEED)
        {
            // 这里不能用 CreateLang.translate(...) —— 它的 namespace 固定是 "create"，
            // 内部会拼成 namespace + "." + langKey，我们的键会被翻成
            // create.gui.create_tacz.speed_too_low，界面上就直接显示原始键名。
            // 用 CreateLang.builder() 只是为了复用它的 forGoggles 缩进排版。
            CreateLang.builder()
                    .add(Component.translatable("gui.create_tacz.speed_too_low", (int) MIN_SPEED, (int) speed))
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip);
            added = true;
        }
        return added;
    }

    private void stopProcessing()
    {
        if (processingTicks != -1)
        {
            processingTicks = -1;
            sendData();
        }
    }

    /**
     * 转速越高加工越快：耗时与转速成反比，在 {@link #REFERENCE_SPEED} 处封顶。
     *
     * <p>超过 256 RPM 照常工作，只是不会更快 —— 这样玩家把传动网络配得偏高时不会
     * 莫名其妙停机。低于 {@link #MIN_SPEED} 时由 {@code tick()} 里的门槛直接停机。
     *
     * <p>配置项 {@code bulletBatchSeconds}（默认 5 秒）是 256 RPM 下的耗时。
     * 比 Create 搅拌机那套 log2 公式更好预测，调参也直观。
     */
    private int getProcessingTicks(float speed)
    {
        float effectiveSpeed = Math.min(speed, REFERENCE_SPEED);
        double baseTicks = Config.COMMON.bulletBatchSeconds.get() * 20.0D;
        return Math.max(1, (int) Math.round(baseTicks * REFERENCE_SPEED / effectiveSpeed));
    }

    private void invalidateLockedRecipe()
    {
        lockedRecipeResolved = false;
        lockedRecipe = null;
    }

    /**
     * 列表过滤器锁定哪些子弹，就找产出匹配的配方。没设滤波则不工作。
     *
     * <h2>为什么滤波槽收列表过滤器而不是裸弹药</h2>
     * TaCZ 的<b>所有口径共用同一个物品</b> {@code tacz:ammo}（见 {@code ModItems}，只有
     * {@code AMMO} 一项），口径存在 NBT 里。而 Create 判断单个物品式滤波走的是
     * {@code FilterItem.testDirect -> ItemHelper.sameItem}，<b>只比物品类型、忽略 NBT</b> ——
     * 于是任何口径的滤波都会命中配方表里的第一条弹药配方（表现为"只有 9mm 能造"）。
     *
     * <p>列表过滤器（{@code create:filter}）带列表，能装多个物品，且支持按 NBT 比对，
     * 正好用来指定"要做哪种口径"。这条路也完全不依赖 TaCZ 的内部 API。
     */
    private GunSmithTableRecipe resolveLockedRecipe()
    {
        if (lockedRecipeResolved)
            return lockedRecipe;

        lockedRecipeResolved = true;
        lockedRecipe = null;

        ItemStack filterStack = filter == null ? ItemStack.EMPTY : filter.getFilter();
        if (filterStack.isEmpty())
            return null;

        FilterItemStack parsed = FilterItemStack.of(filterStack);

        for (GunSmithTableRecipe candidate : getAmmoRecipes())
        {
            if (matchesFilter(parsed, candidate.getResultItem(level.registryAccess())))
            {
                lockedRecipe = candidate;
                break;
            }
        }
        return lockedRecipe;
    }

    /**
     * 判断配方产出是否被列表过滤器接受。
     *
     * <p>直接交给 {@code FilterItemStack.test} —— <b>是否按 NBT 比对由滤镜物品自己的
     * RespectNBT 开关决定</b>，也就是尊重玩家在滤镜 GUI（{@code FilterScreen} 里的
     * {@code I_RESPECT_NBT} 按钮）里的显式选择。
     *
     * <p>（实现细节：{@code ListFilterItemStack.test} 内部用的是它自己存的 shouldRespectNBT，
     * 会忽略调用方传进去的 matchNBT 参数，所以这里传什么都不影响结果。）
     *
     * <p><b>RespectNBT 关闭时的后果需要知道</b>：匹配会退化成只比物品类型，而 TaCZ 的所有
     * 口径共用同一个物品 {@code tacz:ammo}（见 {@code ModItems}），于是列表里放的到底是哪个
     * 口径就不起作用了，机器会一直做配方表里的第一条弹药配方。
     * 症状是"我明明指定了 A 口径，它却一直在造 B" —— 先检查滤镜的 RespectNBT 开关。
     */
    private boolean matchesFilter(FilterItemStack parsed, ItemStack result)
    {
        return !result.isEmpty() && parsed.test(level, result);
    }

    /**
     * TaCZ 的配方走 {@code tacz:gun_smith_table_crafting} 这个 RecipeType。
     *
     * <p>注意不能指望 {@code Recipe#getIngredients()} —— 它在 1.20.1 是返回空列表的
     * default 方法，TaCZ 没有覆写。材料必须走 {@link GunSmithTableRecipe#getInputs()}，
     * 因为它的材料自带数量，标准 Ingredient 表达不了。
     *
     * <p>只保留产出是子弹的配方，枪和配件不归这台机器管。
     */
    private List<GunSmithTableRecipe> getAmmoRecipes()
    {
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get())
                .stream()
                .filter(recipe -> IAmmo.getIAmmoOrNull(recipe.getResultItem(level.registryAccess())) != null)
                .toList();
    }

    private ItemStack getResultOf(GunSmithTableRecipe recipe)
    {
        return recipe.getResultItem(level.registryAccess()).copy();
    }

    // ------------------------------------------------------------------ 从相邻容器取料

    /** 一次原料抽取动作：从哪个容器的哪个槽拿多少。 */
    private record Extraction(IItemHandler inventory, int slot, int amount) {}

    /** 相邻六个面的容器，只要提供 IItemHandler 就算。 */
    private List<IItemHandler> getAdjacentInventories()
    {
        List<IItemHandler> inventories = new ArrayList<>(6);

        for (Direction direction : Direction.values())
        {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null)
                continue;

            // 从邻居的角度看，接口开在朝向我们的那一面
            neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .ifPresent(inventories::add);
        }
        return inventories;
    }

    /**
     * 规划要从相邻容器里抽哪些材料。返回 null 表示材料不足。
     *
     * <p>先规划再执行（而不是边查边扣），这样模拟和实际扣除走的是同一套逻辑，
     * 不会出现"校验通过但扣到一半发现不够"的半消耗状态。Create 的
     * {@code BasinRecipe.apply} 也是 simulate-then-apply 的思路。
     *
     * <p>reserved 记录每个槽已被前面材料占用的数量：两种材料的 Ingredient 可能
     * 同时匹配同一个槽（比如 tag 有重叠），不预留的话会重复计数导致实际扣不出来。
     */
    private List<Extraction> planExtraction(GunSmithTableRecipe recipe)
    {
        List<IItemHandler> inventories = getAdjacentInventories();
        if (inventories.isEmpty())
            return null;

        Map<IItemHandler, Map<Integer, Integer>> reserved = new IdentityHashMap<>();
        List<Extraction> plan = new ArrayList<>();

        for (GunSmithTableIngredient input : recipe.getInputs())
        {
            int needed = input.getCount();

            for (IItemHandler inventory : inventories)
            {
                Map<Integer, Integer> perSlot = reserved.computeIfAbsent(inventory, $ -> new HashMap<>());

                for (int slot = 0; slot < inventory.getSlots() && needed > 0; slot++)
                {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!input.getIngredient().test(stack))
                        continue;

                    int available = stack.getCount() - perSlot.getOrDefault(slot, 0);
                    if (available <= 0)
                        continue;

                    int taken = Math.min(needed, available);
                    perSlot.merge(slot, taken, Integer::sum);
                    plan.add(new Extraction(inventory, slot, taken));
                    needed -= taken;
                }
            }

            if (needed > 0)
                return null;
        }
        return plan;
    }

    /**
     * 按计划扣除。同一槽位可能有多条计划（两种材料匹配同一槽），
     * 顺序 extract 即可 —— 槽内物品同质，按数量扣不会取错。
     */
    private void executeExtraction(List<Extraction> plan)
    {
        for (Extraction extraction : plan)
            extraction.inventory().extractItem(extraction.slot(), extraction.amount(), false);
    }

    // ------------------------------------------------------------------ 成品

    /**
     * 收下一批产出：装得进输出格的直接装，装不下的进 {@link #pendingOutput} 暂存。
     *
     * <p>不再要求"整批必须一次装下"。TaCZ 的批量大小由配方决定、单格容量由该口径的
     * stack_size 决定，两者之间没有任何约束 —— 默认枪包里就有 22wmr / 308 / 792x57
     * 三个口径的产出大于单格上限。要求整批装下的话这些口径会静默停机，第三方枪包
     * 批量更大时同样中招。
     *
     * <p>输出格对外禁插，这里要临时放开（标志位是在 SmartInventory 自己的 insertItem
     * 里生效的，不放开连我们自己都插不进去）。
     */
    private void produce(GunSmithTableRecipe recipe)
    {
        outputInventory.allowInsertion();
        try
        {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputInventory, getResultOf(recipe), false);
            if (!remainder.isEmpty())
                pendingOutput.add(remainder);
        }
        finally
        {
            outputInventory.forbidInsertion();
        }
    }

    /**
     * 把暂存的产出往输出格里冲，能冲多少冲多少，冲不完的留到下一 tick。
     *
     * <p>{@code insertItemStacked} 不会修改传入的 stack，返回的是新的余量 stack，
     * 所以这里用返回值覆盖暂存项是安全的。
     */
    private void flushPendingOutput()
    {
        if (pendingOutput.isEmpty())
            return;

        outputInventory.allowInsertion();
        try
        {
            for (int i = 0; i < pendingOutput.size(); )
            {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputInventory, pendingOutput.get(i), false);
                if (remainder.isEmpty())
                    pendingOutput.remove(i);
                else
                {
                    pendingOutput.set(i, remainder);
                    i++;
                }
            }
        }
        finally
        {
            outputInventory.forbidInsertion();
        }
    }

    /**
     * 滤波槽在世界里的位置：顶面正中，稍微低于顶面。
     *
     * <p>没有用 {@code ValueBoxTransform.Sided} 而是直接给出局部坐标 —— {@code Sided}
     * 要经过一轮 Y/X 轴旋转把"南面"的位置转到目标面，算错了会跑到方块外面去。
     * 顶面位置是固定的，直接写死更不容易错。参考 Create 的 {@code SawFilterSlot}。
     */
    public static class FilterSlot extends ValueBoxTransform
    {
        private static final Vec3 SLOT = VecHelper.voxelSpace(8, 15.5, 8);

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state)
        {
            return SLOT;
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms)
        {
            // 平铺在顶面上
            TransformStack.of(ms).rotateXDegrees(90);
        }
    }
}
