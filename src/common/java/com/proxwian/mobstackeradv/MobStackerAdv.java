package com.proxwian.mobstackeradv;

import java.nio.file.Path;

public final class MobStackerAdv {
    public static final String MOD_ID = "mobstackeradv";

    private MobStackerAdv() {
    }

    public static void init(Path configDirectory) {
        MobStackerConfig.load(configDirectory);
    }
}
