package com.frnc.create_tacz.client.ponder;

import com.frnc.create_tacz.CreateTacz;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组的思索（Ponder）插件。
 *
 * <p><b>为什么整个包都在 {@code client} 下：</b>思索全链路都是客户端独占的 ——
 * {@code PonderSceneRegistry.compile} 里直接取 {@code Minecraft.getInstance().level}，
 * 场景构建器 {@code PonderSceneBuilder} 也引用了客户端渲染类。所以这个包里的每一个类
 * 都<b>绝不能</b>被通用代码（{@code CreateTacz}、{@code ModBlocks}、方块实体）引用，
 * 否则专用服务器加载时会直接 {@code NoClassDefFoundError}。
 *
 * <p>它唯一的入口是 {@code ClientSetup.onClientSetup}，那是个
 * {@code Dist.CLIENT} + MOD 总线的事件处理器，服务端根本不会加载它。
 */
public class CreateTaczPonderPlugin implements PonderPlugin
{
    /** 场景与标签的命名空间、以及 {@code assets/<ns>/ponder/} 的目录名，全都取自这里。 */
    @Override
    public String getModId()
    {
        return CreateTacz.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper)
    {
        CreateTaczPonderScenes.register(helper);
    }
}
