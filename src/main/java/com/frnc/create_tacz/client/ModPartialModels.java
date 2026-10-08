package com.frnc.create_tacz.client;

import com.frnc.create_tacz.CreateTacz;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组的局部模型（PartialModel）。
 *
 * <p>这是 Create 的 {@code AllPartialModels} 的同款写法，也和它一样<b>只能放在客户端</b>
 * （PartialModel 依赖烘焙模型）。
 *
 * <h2>为什么不需要自己监听 ModelEvent.RegisterAdditional</h2>
 * {@code PartialModel.of(...)} 会把模型塞进 Flywheel 的一个<b>进程级全局表</b>，
 * 由 Flywheel 自己的 {@code PartialModelEventHandler.onRegisterAdditional} 在事件触发时
 * 把当时表里的所有模型统一提交烘焙。所以注册是自动的，我们要管的只有<b>时机</b>。
 *
 * <h2>时机：为什么必须有 init()</h2>
 * 静态字段是惰性初始化的 —— 光在 visual 里引用 {@code ModPartialModels.ROTATING_BULLET}，
 * 本类要到第一次渲染才初始化，那时模型早就烘焙完了。烘焙完成后再构造的 PartialModel
 * 会直接向 ModelManager 按 location 取结果，取不到就回落到 <b>missing model</b>，
 * 也就是一个整格的紫黑方块，会把整台机器连玻璃一起盖住。
 *
 * <p>所以要在 {@code FMLClientSetupEvent} 里显式调一次 {@link #init()}
 * （见 {@code ClientSetup}），把它提前。Create 也是这么做的：
 * {@code CreateClient.clientInit} 里调 {@code AllPartialModels.init()}。
 */
public class ModPartialModels
{
    /**
     * 玻璃罩里的展示组：底部传动杆 + 黄铜卡盘 + 一发狙击弹。
     * 整组按"朝上"建模，由 visual / 渲染器绕自身 Y 轴旋转。
     */
    public static final PartialModel ROTATING_BULLET =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateTacz.MOD_ID, "block/rotating_bullet"));

    /**
     * 唯一作用是强制本类初始化，方法体刻意留空 —— 静态字段在上面那几条赋值语句里。
     */
    public static void init()
    {
    }

    private ModPartialModels()
    {
    }
}
