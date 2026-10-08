package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;
import com.frnc.create_tacz.client.MilitaryFactoryRenderer;
import com.frnc.create_tacz.client.MilitaryFactoryVisual;
import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * 方块实体注册。
 *
 * <p>这里是 Create 的 {@code CreateBlockEntityBuilder}，所以 visual 与渲染器都挂在同一条链上，
 * 不需要再手写 {@code EntityRenderersEvent.RegisterRenderers} 和
 * {@code FMLClientSetupEvent} 里的 {@code SimpleBlockEntityVisualizer}。
 *
 * <p>两个方法引用都指向纯客户端类，但可以安全地写在公共代码里：
 * {@code visual(...)} 内部是 {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}，
 * 方法引用的类要到那个 lambda 真正执行时才会被解析；{@code renderer(...)} 同理。
 * Create 自己也是在公共代码里这么写的。
 */
public class ModBlockEntities
{
    public static final BlockEntityEntry<MilitaryFactoryBulletsBlockEntity> MILITARY_FACTORY_BULLETS =
            CreateTacz.REGISTRATE.blockEntity("military_factory_bullets", MilitaryFactoryBulletsBlockEntity::new)
                    // 【顺序不能改】.visual(...) 必须排在 .validBlocks(...) 前面。
                    //
                    // Registrate 的 validBlocks 是 final、且返回基类 BlockEntityBuilder<T, P>，
                    // 所以一旦调过它，整条链的静态类型就退回基类 —— 而 .visual(...) 是
                    // CreateBlockEntityBuilder 才有的方法，放到 validBlocks 后面就"找不到符号"。
                    // Create 自己的注册链全是 visual 在前（见 AllBlockEntityTypes）。
                    //
                    // renderNormally 默认就是 true，等价于 Flywheel 的 neverSkipVanillaRender()：
                    // 机壳与玻璃仍然是普通方块模型，visual 只额外加那根旋转组。
                    // 这一点不能省 —— 滤波槽里的幽灵物品是渲染器画的，visual 不画。
                    .visual(() -> MilitaryFactoryVisual::new)
                    .validBlocks(ModBlocks.MILITARY_FACTORY_BULLETS)
                    .renderer(() -> MilitaryFactoryRenderer::new)
                    .register();

    /**
     * 唯一作用是强制本类初始化，理由同 {@link ModBlocks#register()}。
     */
    public static void register()
    {
    }

    private ModBlockEntities()
    {
    }
}
