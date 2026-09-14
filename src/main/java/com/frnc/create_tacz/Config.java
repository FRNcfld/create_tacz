package com.frnc.create_tacz;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 模组配置。走 Forge 标准配置系统，生成在 {@code config/create_tacz-common.toml}。
 *
 * <p>用 COMMON 而不是 SERVER：SERVER 配置是每个存档一份（{@code serverconfig/}），
 * 而这里只有一个手感数值，放全局改起来更方便。专用服务器上以服务端读到的值为准。
 */
public class Config
{
    public static final Common COMMON;
    public static final ForgeConfigSpec SPEC;

    static
    {
        Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    private Config()
    {
    }

    public static class Common
    {
        public final ForgeConfigSpec.DoubleValue bulletBatchSeconds;

        Common(ForgeConfigSpec.Builder builder)
        {
            builder.comment("军工厂：子弹").push("military_factory_bullets");

            bulletBatchSeconds = builder
                    .comment(
                            "制作一批子弹需要的时间（秒），默认 5.0。",
                            "",
                            "以 256 RPM 为基准转速，实际耗时与该转速成反比 ——",
                            "转速翻倍耗时减半，转速减半耗时翻倍。",
                            "超过 256 RPM 仍然工作，但不会更快（软上限）。",
                            "方块的最低工作转速是 16 RPM，也就是说最慢约为这个值的 16 倍。",
                            "",
                            "应力不足（转速为 0）时机器会停机，与这个值无关。")
                    .defineInRange("bulletBatchSeconds", 5.0D, 0.05D, 3600.0D);

            builder.pop();
        }
    }
}
