package com.rld.unlimitedlogistics.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CULConfig {
    public static final CULConfig CONFIG;
    public static final ModConfigSpec SPEC;

    public ModConfigSpec.ConfigValue<Integer> GAUGE_SLOT_MAX;
    public ModConfigSpec.ConfigValue<Boolean> BULK_MIXING;

    private CULConfig(ModConfigSpec.Builder builder) {
        GAUGE_SLOT_MAX = builder.defineInRange("createunlimitedlogistics.configuration.slot_max", 576, 1, 576);
        BULK_MIXING = builder.define("createunlimitedlogistics.configuration.bulk_mixing", false);
    }

    static {
        Pair<CULConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(CULConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }
}
