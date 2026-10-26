package com.buildershandbag.storage;

import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.buildershandbag.Tags;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketHandbagSupplySync;


/**
 * Server availability snapshots for building-tool previews on the client.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class HandbagSupplyCache {

    private static final int SYNC_INTERVAL = 10;
    private static final Map<EntityPlayer, NBTTagCompound> COUNTS = Collections.synchronizedMap(new WeakHashMap<>());

    private HandbagSupplyCache() {
    }

    public static void update(EntityPlayer player, NBTTagCompound data) {
        COUNTS.put(player, data.copy());
    }

    public static int getCount(EntityPlayer player, ItemStack requested) {
        if (!HandbagOutputSupply.hasConfiguration(player, requested)) return 0;

        int cached = 0;
        NBTTagCompound data = COUNTS.get(player);
        if (data != null) {
            NBTTagList entries = data.getTagList("Outputs", Constants.NBT.TAG_COMPOUND);
            for (int index = 0; index < entries.tagCount(); index++) {
                NBTTagCompound entry = entries.getCompoundTagAt(index);
                if (HandbagOutputs.sameOutput(new ItemStack(entry.getCompoundTag("Stack")), requested)) {
                    cached = entry.getInteger("Available");
                    break;
                }
            }
        }

        return Math.max(cached, HandbagOutputSupply.countInventory(player, requested));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)
                || event.player.ticksExisted % SYNC_INTERVAL != 0) return;
        if (!isBuildingTool(event.player.getHeldItemMainhand())
                && !isBuildingTool(event.player.getHeldItemOffhand())) return;

        NBTTagCompound data = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        for (ItemStack handbag : HandbagOutputSupply.getHandbags(event.player)) {
            for (HandbagConfiguration configuration : HandbagStorage.getConfigurations(handbag)) {
                if (!configuration.isAvailable()) continue;

                ItemStack output = HandbagOutputs.getResult(configuration);
                boolean included = false;
                for (int index = 0; index < entries.tagCount(); index++) {
                    ItemStack existing = new ItemStack(entries.getCompoundTagAt(index).getCompoundTag("Stack"));
                    if (HandbagOutputs.sameOutput(output, existing)) {
                        included = true;
                        break;
                    }
                }

                if (included) continue;

                NBTTagCompound entry = new NBTTagCompound();
                entry.setTag("Stack", output.writeToNBT(new NBTTagCompound()));
                entry.setInteger("Available", HandbagOutputSupply.count(event.player, output));
                entries.appendTag(entry);
            }
        }

        data.setTag("Outputs", entries);
        if (data.equals(COUNTS.get(event.player))) return;

        update(event.player, data);
        HandbagNetwork.INSTANCE.sendTo(new PacketHandbagSupplySync(data), (EntityPlayerMP) event.player);
    }

    private static boolean isBuildingTool(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && (name.getNamespace().equals("buildinggadgets")
            || name.getNamespace().equals("betterbuilderswands"));
    }
}
