package com.nyasha.util;

import net.minecraft.client.MinecraftClient;


/**
 * @author yuxiangll
 * @since 2024/8/10 下午9:01
 * IntelliJ IDEA
 */

public interface IMinecraftMixin {
    MinecraftClient mc = MinecraftClient.getInstance();
}
