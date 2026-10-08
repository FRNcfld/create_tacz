package com.frnc.create_tacz.registry;

import com.frnc.create_tacz.CreateTacz;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 音效注册。
 *
 * <p>这里只有一个条目，而且它是<b>听不见的</b> —— 存在的唯一目的是给 Minecraft 的
 * "隐藏式字幕"（辅助功能设置里的那个开关）提供一个可以挂字幕的音效事件。
 *
 * <h2>为什么字幕要靠音效实现</h2>
 * Minecraft 没有"直接显示一条字幕"的 API。字幕走的是这样一条链：
 * {@code SoundEngine.play()} 解析出一个 {@code WeighedSoundEvents} 之后，
 * 把它推给所有已注册的 {@code SoundEventListener}，而
 * {@code SubtitleOverlay} 正是其中一个监听器。所以要有字幕，就必须播一个音效。
 *
 * <h2>为什么可以做到无声</h2>
 * 推送条件是（读 {@code SoundEngine.play} 的字节码得到）：
 * <pre>
 *   pushed = 相对/无衰减  ||  distanceSqr &lt; volume²
 * </pre>
 * 也就是说音量为 0 会<b>抑制</b>字幕，除非这个 SoundInstance 用的是
 * {@code Attenuation.NONE}。客户端那边正是这么播的：volume 0 + Attenuation.NONE，
 * 于是字幕照出、一点声音都没有。见 {@code MilitaryFactorySounds}。
 *
 * <p>字幕文案写在 {@code assets/create_tacz/sounds.json} 里，键是
 * {@code subtitles.create_tacz.military_factory_running}。
 */
public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CreateTacz.MOD_ID);

    /** 机器真正在加工时挂出的那行字幕。本身固定静音播放。 */
    public static final RegistryObject<SoundEvent> MILITARY_FACTORY_RUNNING = SOUNDS.register(
            "military_factory_running",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(CreateTacz.MOD_ID, "military_factory_running")));

    private ModSounds()
    {
    }
}
