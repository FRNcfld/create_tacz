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
     * 传动轴轴头：底面那个 4×4 的轴端，由 visual / 渲染器绕 Y 轴旋转。
     *
     * <p>按 Create 自己的 {@code powered_shaft} 里那个底部轴头的样式做的（同样的
     * {@code create:block/axis} / {@code axis_top} 贴图、同样省掉共面的顶面）。
     *
     * <p>为什么不用 Create 现成的 {@code AllPartialModels.SHAFT_HALF}：它是<b>半格</b>（8 像素）长，
     * 装进本机后会把子弹的下半截整个包住。Create 没有更短的通用轴局部模型，但它自己的机器也用
     * 机器专用的短轴头（{@code powered_shaft} 就是 2 像素），所以这里照做 ——
     * 关键是<b>轴由 visual / 渲染器画，而不是建模进方块模型</b>，这与 Create 一致。
     *
     * <h2>为什么只有 1 像素高（这个高度是算出来的，不是随手定的）</h2>
     * 要求"完全不挡到弹壳"：
     * <ul>
     *   <li>弹壳上比轴（4 宽）<b>窄</b>的最低部件是 {@code rim}，下沿在 <b>y1.7</b> ——
     *       轴一旦高过它，就会把这截弹壳包进自己的实心体积里；</li>
     *   <li>{@code carrier}（y1..1.7）比轴<b>宽</b>（4.8），是它挡住轴，不是轴挡住它；</li>
     *   <li>而 y0..1 那一层已经被机壳自己的底面层（{@code base_*} 与 {@code port_*}）占满，
     *       开口只有正中那个 4×4 的洞。</li>
     * </ul>
     * 三者夹出来的结果：轴顶只能到 y1.7，而 y1..1.7 又整个在卡盘内部 ——
     * 所以侧面看得见的只有 y0..1，正好填满机壳底部那个开口。
     * 也就是说<b>从正下方能看到轴端，从侧面看不到</b>（和 Create 的 Millstone 一样：
     * 底部接轴的机器，侧面本来就被机壳挡住）。想让它从侧面也可见，就得改机壳或弹壳的几何。
     */
    public static final PartialModel SHAFT_STUB =
            PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateTacz.MOD_ID, "block/shaft_stub"));

    /**
     * 玻璃罩里的展示组：黄铜卡盘 + 一发狙击弹（轴头是上面那个单独的局部模型）。
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
