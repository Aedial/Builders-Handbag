package com.buildershandbag.integration;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.elytradev.architecture.common.shape.EnumShape;

/**
 * ArchitectureCraft shape integration.
 */
final class ArchitectureCraftIntegration {

    private ArchitectureCraftIntegration() {
    }

    // TODO: If there is demand, we might consider fractional material cost
    //       (aligning with ArchitectureCraft's own cost calculations),
    //       which would require custom rendering, decoupling ItemStack
    //       from count, and custom slot count rendering. Also, I'm not
    //       sure how ArchitectureCraft calculates the cost of a shape.

    static void addOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        if (!(material.getItem() instanceof ItemBlock)) return;

        Block source = ((ItemBlock) material.getItem()).getBlock();

        for (EnumShape shape : EnumShape.values()) {
            if (shape.isCladding()) continue;

            ItemStack result = shape.kind.newStack(shape, source, material.getMetadata(), 1);
            HandbagConfigurationProvider.addOption(
                options,
                new HandbagConfigurationOption(result, HandbagIntegration.ARCHITECTURECRAFT));
        }
    }
}
