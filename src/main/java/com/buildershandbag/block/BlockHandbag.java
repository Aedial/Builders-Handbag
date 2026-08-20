package com.buildershandbag.block;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.buildershandbag.Tags;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.tile.TileHandbag;


/**
 * The placed form of a handbag. Its visible geometry is provided entirely by
 * {@link com.buildershandbag.client.render.RenderHandbag}; the collision boxes
 * mirror the twelve one-pixel-wide frame struts.
 */
public class BlockHandbag extends BlockContainer {

    private static final double UNIT = 1.0D / 16.0D;
    private static final AxisAlignedBB[] FRAME_STRUTS = createFrameStruts();

    public BlockHandbag() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(Tags.MODID, "handbag"));
        setTranslationKey(Tags.MODID + ".handbag");
        setHardness(2.0F);
        setResistance(6.0F);
        setLightOpacity(0);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
        return new TileHandbag();
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockState state, @Nonnull IBlockAccess source, @Nonnull BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        return NULL_AABB;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
                                      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        for (AxisAlignedBB strut : FRAME_STRUTS) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, strut);
        }
    }

    @Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state,
                         int fortune) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileHandbag) {
            ItemStack handbag = ((TileHandbag) tile).getHandbagStack();
            if (!handbag.isEmpty()) {
                drops.add(handbag);
                return;
            }
        }

        drops.add(new ItemStack(ItemRegistry.HANDBAG));
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target, World world, @Nonnull BlockPos pos,
                                  @Nonnull EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileHandbag) {
            ItemStack handbag = ((TileHandbag) tile).getHandbagStack();
            if (!handbag.isEmpty()) return handbag;
        }

        return new ItemStack(ItemRegistry.HANDBAG);
    }

    private static AxisAlignedBB[] createFrameStruts() {
        return new AxisAlignedBB[] {
            // Four vertical corners.
            box(0, 0, 0, 1, 16, 1), box(15, 0, 0, 16, 16, 1),
            box(0, 0, 15, 1, 16, 16), box(15, 0, 15, 16, 16, 16),

            // The four bottom and four top horizontal edges, stopping at the
            // corners so the boxes do not overlap.
            box(1, 0, 0, 15, 1, 1), box(1, 0, 15, 15, 1, 16),
            box(0, 0, 1, 1, 1, 15), box(15, 0, 1, 16, 1, 15),
            box(1, 15, 0, 15, 16, 1), box(1, 15, 15, 15, 16, 16),
            box(0, 15, 1, 1, 16, 15), box(15, 15, 1, 16, 16, 15)
        };
    }

    private static AxisAlignedBB box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new AxisAlignedBB(
            minX * UNIT, minY * UNIT, minZ * UNIT,
            maxX * UNIT, maxY * UNIT, maxZ * UNIT);
    }
}
