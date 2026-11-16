package com.buildershandbag.storage;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;

import com.buildershandbag.config.HandbagServerConfig;


/**
 * Shared creative material policy for player placement, building tools, and recovery.
 */
public final class HandbagCreative {

    private HandbagCreative() {
    }

    public static boolean hasFreeSupply(@Nullable EntityPlayer player) {
        return player != null && player.isCreative() && HandbagServerConfig.isCreativeSupplyEnabled();
    }
}
