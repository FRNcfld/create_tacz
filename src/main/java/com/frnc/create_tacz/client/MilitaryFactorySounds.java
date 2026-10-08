package com.frnc.create_tacz.client;

import com.frnc.create_tacz.content.MilitaryFactoryBulletsBlockEntity;
import com.frnc.create_tacz.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * 军工厂：子弹的"隐藏式字幕"。
 *
 * <p>机器真正在加工时，让画面上挂着一条"军工厂：子弹制造"的字幕；停下来就消失。
 *
 * <h2>实现方式：静音音效</h2>
 * Minecraft 没有直接显示字幕的 API —— 字幕是
 * {@code SoundEngine.play()} 推给 {@code SoundEventListener} 的，而
 * {@code SubtitleOverlay} 只是其中一个监听器。所以要出字幕就必须"播一个音效"。
 *
 * <p>要让它"无声却有字幕"，{@code SoundEngine.play} 上有<b>两道</b>关卡都要过，
 * 缺一个都会静默失败（详见 {@link SubtitleOnlySound} 的注释）：
 * <ol>
 *   <li>音量 0 时音效会被整个跳过，除非 {@code canStartSilent()} 返回 true；</li>
 *   <li>推字幕的条件是 {@code 相对/无衰减 || distanceSqr < volume²}，
 *       音量 0 让后半段恒假，所以必须用 {@code Attenuation.NONE}。</li>
 * </ol>
 * 两个条件都满足后：字幕照常显示，一点声音也没有。
 *
 * <h2>为什么要反复重播</h2>
 * 原版字幕会在约一秒半后自行淡出，而音效本身只播一次。所以只要机器还在工作，
 * 就按 {@link #REPEAT_INTERVAL} 的间隔重新播一次，字幕才会一直挂在屏幕上。
 * 同一个文案的重复字幕在原版 {@code SubtitleOverlay} 里会合并计数，不会刷屏。
 *
 * <p>这个类只在客户端加载（调用方用 {@code DistExecutor} 守卫），服务端不会碰到它。
 */
public class MilitaryFactorySounds
{
    /**
     * 重播间隔，单位游戏刻。
     *
     * <p>30 刻 = 1.5 秒，正好卡在原版字幕开始淡出之前，所以看上去是持续显示而不是闪烁。
     */
    private static final int REPEAT_INTERVAL = 30;

    /** 每刻调用。机器在工作且轮到重播间隔时，播一次静音字幕音效。 */
    public static void tick(MilitaryFactoryBulletsBlockEntity be)
    {
        Level level = be.getLevel();
        if (level == null || !be.isWorking())
            return;

        if (level.getGameTime() % REPEAT_INTERVAL != 0)
            return;

        playSubtitleOnly(level, be.getBlockPos());
    }

    private static void playSubtitleOnly(Level level, BlockPos pos)
    {
        Minecraft.getInstance()
                .getSoundManager()
                .play(new SubtitleOnlySound(level, pos));
    }

    /**
     * 只出字幕、不出声的音效实例。
     *
     * <h2>为什么必须覆写 {@code canStartSilent()}</h2>
     * 光把音量设成 0 是<b>不够</b>的。{@code SoundEngine.play} 在真正播放之前有这么一段
     * （读字节码得到）：
     * <pre>
     *   if (音量 × 音源滑条 == 0 &amp;&amp; !instance.canStartSilent()) {
     *       LOGGER.debug("Skipped playing sound {}, volume was zero.", ...);
     *       return;                    // ← 直接返回
     *   }
     *   ...                            // 推字幕的 SoundEventListener 调用在这之后
     * </pre>
     * 也就是说：<b>音量 0 配上默认的 {@code canStartSilent() == false}，音效会被整个跳过，
     * 字幕那段代码根本执行不到。</b>
     *
     * <p>覆写成 true 之后，音效会正常走完流程（音量仍是 0，所以听不见任何声音），
     * 字幕也会照常推给 {@code SubtitleOverlay}。
     * 原版 {@code MinecartSoundInstance} / {@code BeeSoundInstance} 覆写这个方法也是同理 ——
     * "音量被调到 0 时这个声音仍然应该存在"。
     *
     * <p>{@code Attenuation.NONE} 则是第二个必要条件：推送条件是
     * {@code 相对/无衰减 || distanceSqr < volume²}，音量 0 让后半段恒假，
     * 只有无衰减才能让字幕推出。
     */
    private static class SubtitleOnlySound extends SimpleSoundInstance
    {
        private SubtitleOnlySound(Level level, BlockPos pos)
        {
            super(ModSounds.MILITARY_FACTORY_RUNNING.getId(),
                    SoundSource.BLOCKS,
                    0.0F,                            // 音量 0：完全听不见
                    1.0F,
                    level.random,
                    false,                           // 不循环（靠上面周期性重播）
                    0,                               // 无延迟
                    SoundInstance.Attenuation.NONE,  // 不衰减，字幕才会被推送
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    false);
        }

        @Override
        public boolean canStartSilent()
        {
            return true;
        }
    }

    private MilitaryFactorySounds()
    {
    }
}
