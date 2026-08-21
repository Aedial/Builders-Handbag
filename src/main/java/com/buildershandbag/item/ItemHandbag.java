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
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
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
import com.buildershandbag.block.BlockHandbag;
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
public class ItemHandbag extends ItemBlock {

    private static final String AE2_MODID = "appliedenergistics2";

    public ItemHandbag(BlockHandbag block) {
        super(block);
        setRegistryName(block.getRegistryName());
        setTranslationKey(block.getTranslationKey());
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
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

    // TODO: Need to figure how we place it on the floor, considering click is placing blocks.
    //       Maybe a keybind? That's kinda eh...

    @Override
    @Nonnull
    public EnumActionResult onItemUseFirst(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos,
                                           @Nonnull EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull EnumHand hand) {
        if (!player.isSneaking()) return EnumActionResult.PASS;

        openHandbag(player, world, hand);
        return EnumActionResult.SUCCESS;
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing side,
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
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, EntityPlayer player, @Nonnull EnumHand hand) {
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
                String messageKey = HandbagStorage.getConfigurations(handbag).isEmpty()
                    ? "message.buildershandbag.no_configuration"
                    : "message.buildershandbag.no_selection";
                HandbagMessages.error(player, messageKey);
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

            // ArchitectureCraft fills the placed tile from stack NBT after placement,
            // so the client needs a deferred tile sync for it to render correctly
            if (configuration.getIntegration() == HandbagIntegration.ARCHITECTURECRAFT) {
                syncArchitectureCraftTile(world, placementPos);
            }

            if (configuration.getIntegration() == HandbagIntegration.BLOCKCRAFTERY
                    && (!HandbagIntegration.BLOCKCRAFTERY.isModLoaded() || !configureBlockcraftery(
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

    private void syncArchitectureCraftTile(World world, BlockPos position) {
        TileEntity tile = world.getTileEntity(position);
        if (tile == null) return;

        HandbagNetwork.syncPlacedTile(world, position, tile);
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

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private boolean configureBlockcraftery(World world, BlockPos position, EntityPlayer player, EnumFacing side,
            float hitX, float hitY, float hitZ, ItemStack material) {
        return BlockcrafteryIntegration.configure(world, position, player, side, hitX, hitY, hitZ, material);
    }

    @Optional.Method(modid = AE2_MODID)
    private int pullMaterialFromNetwork(EntityPlayer player, ItemStack handbag, ItemStack material, int requestedAmount) {
        return Ae2Integration.refill(player, handbag, material, requestedAmount);
    }

    @Optional.Method(modid = AE2_MODID)
    @SideOnly(Side.CLIENT)
    private void addAe2Tooltip(ItemStack stack, List<String> tooltip) {
        Ae2Integration.addTooltip(stack, tooltip);
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

        List<HandbagIntegration> integrations = HandbagServerConfig.integrations.getEnabledIntegrations();
        String integrationNames = integrations.stream()
            .map(HandbagIntegration::getTranslatedName)
            .reduce((a, b) -> a + " / " + b)
            .orElse(I18n.format("tooltip.buildershandbag.no_integrations"));
        tooltip.add(I18n.format("tooltip.buildershandbag.tooltip", integrationNames));

        if (Loader.isModLoaded(AE2_MODID)) addAe2Tooltip(stack, tooltip);

        tooltip.add("");

        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(stack);
        if (configurations.isEmpty()) {
            tooltip.add(I18n.format("tooltip.buildershandbag.no_configurations"));
            return;
        }

        tooltip.add(I18n.format("tooltip.buildershandbag.configurations",
            configurations.size(), HandbagStorage.CONFIGURATION_COUNT));

        int selected = HandbagStorage.getSelected(stack);
        if (selected >= 0) {
            HandbagConfiguration configuration = configurations.get(selected);
            tooltip.add(I18n.format(
                "tooltip.buildershandbag.selected",
                configuration.getResult().getDisplayName(),
                configuration.getMaterialCount()));
        } else {
            // may not have selected on first configuration
            tooltip.add(I18n.format("tooltip.buildershandbag.no_selection"));
        }
    }
}
