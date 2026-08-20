package com.buildershandbag.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.Handbag;
import com.buildershandbag.Tags;
import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.container.GuiHandler;
import com.buildershandbag.integration.Ae2Integration;
import com.buildershandbag.integration.BlockcrafteryIntegration;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.network.HandbagMessages;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketHandbagSync;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Handheld decoration placer. A selected configuration is placed through its
 * own ItemBlock implementation, so addon placement behaviour remains intact.
 */
public class ItemHandbag extends Item {

    private static final String AE2_MODID = "appliedenergistics2";
    private static final String BLOCKCRAFTERY_MODID = "blockcraftery";

    public ItemHandbag() {
        setRegistryName(new ResourceLocation(Tags.MODID, "handbag"));
        setTranslationKey(Tags.MODID + ".handbag");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilityProvider() {

            private final IItemHandler handler = new HandbagMaterialHandler(stack);

            @Override
            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
            }

            @Override
            @Nullable
            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                    ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler)
                    : null;
            }
        };
    }

    // TODO: Need to figure how we place it on the floor.
    //       Maybe a keybind? That's kinda eh...

    @Override
    @Nonnull
    public EnumActionResult onItemUseFirst(@Nonnull EntityPlayer player, World world, @Nonnull BlockPos pos,
            @Nonnull EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull EnumHand hand) {
        if (!player.isSneaking()) return EnumActionResult.PASS;

        openHandbag(player, world, hand);
        return EnumActionResult.SUCCESS;
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            openHandbag(player, world, hand);
            return EnumActionResult.SUCCESS;
        }
        if (world.isRemote) return EnumActionResult.SUCCESS;

        return placeConfiguration(player, world, pos, hand, side, hitX, hitY, hitZ);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack handbag = player.getHeldItem(hand);
        openHandbag(player, world, hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, handbag);
    }

    private void openHandbag(EntityPlayer player, World world, EnumHand hand) {
        if (world.isRemote) return;

        player.openGui(Handbag.instance, GuiHandler.GUI_HANDBAG, world, hand.ordinal(), 0, 0);
    }

    private EnumActionResult placeConfiguration(EntityPlayer player, World world, BlockPos clickedPos, EnumHand hand,
            EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack handbag = player.getHeldItem(hand);
        int selected = HandbagStorage.getSelected(handbag);
        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, selected);
        if (configuration == null) {
            HandbagMessages.error(player, "message.buildershandbag.no_selection");
            return EnumActionResult.FAIL;
        }

        ItemStack resultStack = configuration.getResult();
        if (!(resultStack.getItem() instanceof ItemBlock)) {
            HandbagMessages.error(player, "message.buildershandbag.invalid_configuration");
            return EnumActionResult.FAIL;
        }

        if (!ensureMaterialAvailable(player, handbag, selected, configuration)) {
            HandbagMessages.error(player, "message.buildershandbag.no_material");
            return EnumActionResult.FAIL;
        }

        BlockPos placementPos = getPlacementPosition(world, clickedPos, side);
        EnumActionResult placement = placeResultStack(
            player, world, hand,
            clickedPos, side, hitX, hitY, hitZ,
            resultStack, configuration, placementPos);
        if (placement != EnumActionResult.SUCCESS) return placement;

        HandbagStorage.consumeMaterial(handbag, selected);
        player.inventory.markDirty();

        if (player instanceof EntityPlayerMP) syncToClient((EntityPlayerMP) player, hand);

        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult placeResultStack(EntityPlayer player, World world, EnumHand hand, BlockPos clickedPos,
            EnumFacing side, float hitX, float hitY, float hitZ, ItemStack resultStack,
            HandbagConfiguration configuration, BlockPos placementPos) {
        ItemStack handbag = player.getHeldItem(hand);
        player.setHeldItem(hand, resultStack.copy());

        try {
            EnumActionResult result = resultStack.getItem().onItemUse(player, world, clickedPos, hand, side, hitX, hitY, hitZ);
            if (result != EnumActionResult.SUCCESS) return result;

            if (configuration.getIntegration() == HandbagIntegration.BLOCKCRAFTERY
                    && (!Loader.isModLoaded(BLOCKCRAFTERY_MODID) || !configureBlockcraftery(
                        world, placementPos, player,
                        side, hitX, hitY, hitZ,
                        configuration.getMaterial()))) {
                HandbagMessages.error(player, "message.buildershandbag.blockcraftery_failed");
                return EnumActionResult.FAIL;
            }

            return result;
        } finally {
            player.setHeldItem(hand, handbag);
        }
    }

    /**
     * Ensures the selected configuration has one stored material block before placement.
     * When it is empty, the AE2 link is tried first (if possible) and then the player inventory.
     */
    private boolean ensureMaterialAvailable(EntityPlayer player, ItemStack handbag, int selected,
            HandbagConfiguration configuration) {
        if (configuration.getMaterialCount() > 0) return true;

        if (Loader.isModLoaded(AE2_MODID) && HandbagServerConfig.integrations.enableAe2Refill) {
            int requested = HandbagStorage.MATERIAL_CAPACITY - configuration.getMaterialCount();
            int pulled = pullMaterialFromNetwork(player, handbag, configuration.getMaterial(), requested);
            if (pulled > 0) {
                ItemStack extracted = configuration.getMaterial();
                extracted.setCount(pulled);
                if (HandbagStorage.insertMaterial(handbag, selected, extracted, false) > 0) return true;
            }
        }

        int inventorySlot = findMaterialInInventory(player.inventory, configuration.getMaterial());
        if (inventorySlot < 0) return false;

        ItemStack material = player.inventory.getStackInSlot(inventorySlot);
        int accepted = HandbagStorage.insertMaterial(handbag, selected, material, false);
        if (accepted <= 0) return false;

        material.shrink(accepted);
        if (material.isEmpty()) player.inventory.setInventorySlotContents(inventorySlot, ItemStack.EMPTY);
        player.inventory.markDirty();

        return true;
    }

    private int findMaterialInInventory(IInventory inventory, ItemStack material) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (HandbagStorage.sameMaterial(material, inventory.getStackInSlot(slot))) return slot;
        }

        return -1;
    }

    private BlockPos getPlacementPosition(World world, BlockPos clickedPos, EnumFacing side) {
        Block block = world.getBlockState(clickedPos).getBlock();
        return block.isReplaceable(world, clickedPos) ? clickedPos : clickedPos.offset(side);
    }

    @Optional.Method(modid = BLOCKCRAFTERY_MODID)
    private boolean configureBlockcraftery(World world, BlockPos position, EntityPlayer player, EnumFacing side,
            float hitX, float hitY, float hitZ, ItemStack material) {
        return BlockcrafteryIntegration.configure(world, position, player, side, hitX, hitY, hitZ, material);
    }

    @Optional.Method(modid = AE2_MODID)
    private int pullMaterialFromNetwork(EntityPlayer player, ItemStack handbag, ItemStack material, int requestedAmount) {
        return Ae2Integration.refill(player, handbag, material, requestedAmount);
    }

    public static void syncToClient(EntityPlayerMP player, EnumHand hand) {
        ItemStack handbag = player.getHeldItem(hand);
        if (handbag.getItem() != ItemRegistry.HANDBAG) return;

        HandbagNetwork.INSTANCE.sendTo(
            new PacketHandbagSync(hand, HandbagStorage.copyData(handbag)),
            player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip,
            @Nonnull ITooltipFlag flag) {
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(stack);
        int selected = HandbagStorage.getSelected(stack);

        tooltip.add(I18n.format("tooltip.buildershandbag.configurations",
            configurations.size(), HandbagStorage.CONFIGURATION_COUNT));

        if (selected >= 0) {
            HandbagConfiguration configuration = configurations.get(selected);
            tooltip.add(I18n.format(
                "tooltip.buildershandbag.selected",
                configuration.getResult().getDisplayName(),
                configuration.getMaterialCount()));
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.buildershandbag.no_selection"));
        }

        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + I18n.format("tooltip.buildershandbag.open"));
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("tooltip.buildershandbag.scroll"));
    }
}
