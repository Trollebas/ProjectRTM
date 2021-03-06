package net.teamfruit.projectrtm.rtm.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.teamfruit.projectrtm.ngtlib.block.BlockUtil;
import net.teamfruit.projectrtm.rtm.RTMBlock;
import net.teamfruit.projectrtm.rtm.RTMItem;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityConverter;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityConverterCore;

public class BlockConverter extends BlockContainer {
	private static int[][] pos_iron = { { -3, 0, -1 }, { -3, 0, 0 }, { -3, 0, 1 }, { 3, 0, -1 }, { 3, 0, 0 }, { 3, 0, 1 },
			{ -3, 1, -1 }, { -3, 1, 0 }, { -3, 1, 1 }, { -1, 1, -1 }, { -1, 1, 0 }, { -1, 1, 1 }, { 0, 1, -1 }, { 0, 1, 0 }, { 0, 1, 1 }, { 1, 1, -1 }, { 1, 1, 0 }, { 1, 1, 1 }, { 3, 1, -1 }, { 3, 1, 0 }, { 3, 1, 1 },
			{ -3, 2, -1 }, { -3, 2, 0 }, { -3, 2, 1 }, { -2, 2, -1 }, { -2, 2, 0 }, { -2, 2, 1 }, { -1, 2, -2 }, { -1, 2, 2 }, { 0, 2, -2 }, { 0, 2, 2 }, { 1, 2, -2 }, { 1, 2, 2 }, { 2, 2, -1 }, { 2, 2, 0 }, { 2, 2, 1 }, { 3, 2, -1 }, { 3, 2, 0 }, { 3, 2, 1 },
			{ -3, 3, -1 }, { -3, 3, 0 }, { -3, 3, 1 }, { -2, 3, -1 }, { -2, 3, 0 }, { -2, 3, 1 }, { -1, 3, -2 }, { -1, 3, 2 }, { 0, 3, -2 }, { 0, 3, 2 }, { 1, 3, -2 }, { 1, 3, 2 }, { 2, 3, -1 }, { 2, 3, 0 }, { 2, 3, 1 }, { 3, 3, -1 }, { 3, 3, 0 }, { 3, 3, 1 },
			{ -3, 4, 0 }, { -2, 4, -1 }, { -2, 4, 0 }, { -2, 4, 1 }, { -1, 4, -2 }, { -1, 4, 2 }, { 0, 4, -2 }, { 0, 4, 2 }, { 1, 4, -2 }, { 1, 4, 2 }, { 2, 4, -1 }, { 2, 4, 0 }, { 2, 4, 1 }, { 3, 4, 0 },
			{ -2, 5, -1 }, { -2, 5, 0 }, { -2, 5, 1 }, { -1, 5, -2 }, { -1, 5, 2 }, { 0, 5, -2 }, { 0, 5, 2 }, { 0, 5, 3 }, { 1, 5, -2 }, { 1, 5, 2 }, { 2, 5, -1 }, { 2, 5, 0 }, { 2, 5, 1 },
			{ -2, 6, -1 }, { -2, 6, 0 }, { -2, 6, 1 }, { -1, 6, -2 }, { -1, 6, 2 }, { 0, 6, -2 }, { 0, 6, 2 }, { 1, 6, -2 }, { 1, 6, 2 }, { 2, 6, -1 }, { 2, 6, 0 }, { 2, 6, 1 },
			{ -1, 7, -1 }, { -1, 7, 0 }, { -1, 7, 1 }, { 0, 7, -1 }, { 0, 7, 1 }, { 1, 7, -1 }, { 1, 7, 0 }, { 1, 7, 1 } };

	private static int[][] pos_brick = { { -1, 0, -1 }, { -1, 0, 0 }, { -1, 0, 1 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, -1 }, { 1, 0, 0 }, { 1, 0, 1 } };

	public final boolean isCore;

	public BlockConverter(boolean par1) {
		super(Material.rock);
		this.isCore = par1;
		this.setLightLevel(par1 ? 1.0F : 0.0F);
		if (par1) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.9375F, 1.0F);
		}
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public boolean hasTileEntity() {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		if (this.isCore) {
			return new TileEntityConverterCore();
		} else {
			return new TileEntityConverter();
		}
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (this==RTMBlock.converterCore) {
			entity.attackEntityFrom(DamageSource.lava, 1.0F);
			entity.setFire(1);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (!world.isRemote) {
			TileEntity tile0 = world.getTileEntity(x, y, z);
			if (tile0 instanceof TileEntityConverter) {
				TileEntityConverterCore tile1 = ((TileEntityConverter) tile0).getCore();
				if (tile1!=null&&world.getBlock(tile1.xCoord, tile1.yCoord, tile1.zCoord)==RTMBlock.converterCore) {
					createConverter(world, tile1.xCoord, tile1.yCoord-4, tile1.zCoord, tile1.getDirection(), true);
					this.dropBlockAsItem(world, tile1.xCoord, tile1.yCoord-4, tile1.zCoord, new ItemStack(RTMItem.steel_ingot, 64, 0));//104x2
					this.dropBlockAsItem(world, tile1.xCoord, tile1.yCoord-4, tile1.zCoord, new ItemStack(RTMItem.steel_ingot, 64, 0));
					this.dropBlockAsItem(world, tile1.xCoord, tile1.yCoord-4, tile1.zCoord, new ItemStack(RTMItem.steel_ingot, 64, 0));
					this.dropBlockAsItem(world, tile1.xCoord, tile1.yCoord-4, tile1.zCoord, new ItemStack(RTMItem.steel_ingot, 16, 0));
				}
			}
		}
	}

	/**
	 * 転炉を生成できるかチェック
	 * @return 0~3 (-1で不可)*/
	public static byte shouldCreateConverter(World world, int x, int y, int z) {
		if (world.getBlock(x, y+2, z)!=RTMBlock.fireBrick) {
			return -1;
		}

		for (int i = 2; i<7; ++i)//check:Brick
		{
			for (int j = 0; j<pos_brick.length; ++j) {
				if (world.getBlock(x+pos_brick[j][0], y+i, z+pos_brick[j][2])!=RTMBlock.fireBrick) {
					return -1;
				}
			}
		}

		boolean flag;
		for (int i = 0; i<4; ++i)//check:Iron
		{
			flag = true;
			for (int j = 0; j<pos_iron.length; ++j) {
				int[] p0 = BlockUtil.rotateBlockPos((byte) i, pos_iron[j][0], pos_iron[j][1], pos_iron[j][2]);
				if (world.getBlock(x+p0[0], y+p0[1], z+p0[2])!=RTMBlock.steelMaterial) {
					flag = false;
				}
			}

			if (flag) {
				return (byte) i;
			}
		}

		return -1;
	}

	public static void createConverter(World world, int x, int y, int z, byte dir, boolean setAir) {
		if (setAir) {
			world.setBlock(x, y+4, z, Blocks.air, 0, 2);
			world.setBlock(x, y+2, z, Blocks.air, 0, 2);
		} else {
			world.setBlock(x, y+4, z, RTMBlock.converterCore, 0, 2);
			TileEntityConverterCore tile = (TileEntityConverterCore) world.getTileEntity(x, y+4, z);
			tile.setDirection(dir);

			setConverterBlock(world, x, y+2, z, x, y+4, z);
		}

		for (int i = 2; i<7; ++i) {
			for (int j = 0; j<pos_brick.length; ++j) {
				if (setAir) {
					world.setBlock(x+pos_brick[j][0], y+i, z+pos_brick[j][2], Blocks.air, 0, 2);
				} else {
					setConverterBlock(world, x+pos_brick[j][0], y+i, z+pos_brick[j][2], x, y+4, z);
				}
			}
		}

		for (int j = 0; j<pos_iron.length; ++j) {
			int[] p0 = BlockUtil.rotateBlockPos(dir, pos_iron[j][0], pos_iron[j][1], pos_iron[j][2]);
			if (setAir) {
				world.setBlock(x+p0[0], y+p0[1], z+p0[2], Blocks.air, 0, 2);
			} else {
				setConverterBlock(world, x+p0[0], y+p0[1], z+p0[2], x, y+4, z);
			}
		}
	}

	private static void setConverterBlock(World world, int x, int y, int z, int coreX, int coreY, int coreZ) {
		world.setBlock(x, y, z, RTMBlock.converterBase, 0, 2);
		TileEntityConverter tile = (TileEntityConverter) world.getTileEntity(x, y, z);
		tile.setCorePos(coreX, coreY, coreZ);
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		return null;
	}
}