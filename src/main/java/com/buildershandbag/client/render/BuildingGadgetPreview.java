package com.buildershandbag.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.integration.HandbagIntegration;


@SideOnly(Side.CLIENT)
public final class BuildingGadgetPreview {

    private final EntityPlayer player;
    private final ItemStack output;
    private BlockPos position;

    public BuildingGadgetPreview(EntityPlayer player, ItemStack gadget) {
        this.player = player;
        this.output = BuildingGadgetsTarget.get(gadget);
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    public boolean render(BlockRendererDispatcher dispatcher, IBlockState state, float brightness) {
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) return false;

        if (name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)
                && HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
            return BlockcrafteryGadgetPreview.render(dispatcher, state, brightness, output);
        }

        if (name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
                && HandbagIntegration.ARCHITECTURECRAFT.isModLoaded() && !output.isEmpty()) {
            return ArchitectureCraftGadgetPreview.render(dispatcher, state, brightness, player, position, output);
        }

        return false;
    }
}
