package net.teamfruit.projectrtm.ngtlib.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.teamfruit.projectrtm.rtm.RTMMaterial;

public abstract class BlockLiquidBase extends BlockLiquid {
	public BlockLiquidBase(Material par1) {
		super(par1);
	}

	@Override
	public int tickRate(World world)//water:5, lava:30(10)
	{
		return 5;
	}

	@Override
	public int getRenderType() {
		return this.getRenderId();
	}

	public abstract int getRenderId();

	@Override
	public boolean canCollideCheck(int meta, boolean par2) {
		return par2;
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (!world.isRemote) {
			int meta = world.getBlockMetadata(x, y, z);
			int prevMeta = meta;
			meta = this.setLiquid(world, x, y, z, x, y-1, z, meta);
			if (meta==prevMeta) {
				if ((meta>=0&&this.canFlowLiquid(world, x-1, y-1, z)>0)||this.canFlowLiquid(world, x-1, y, z)+meta>15) {
					meta = this.setLiquid(world, x, y, z, x-1, y, z, meta);
				}

				if ((meta>=0&&this.canFlowLiquid(world, x+1, y-1, z)>0)||this.canFlowLiquid(world, x+1, y, z)+meta>15) {
					meta = this.setLiquid(world, x, y, z, x+1, y, z, meta);
				}

				if ((meta>=0&&this.canFlowLiquid(world, x, y-1, z-1)>0)||this.canFlowLiquid(world, x, y, z-1)+meta>15) {
					meta = this.setLiquid(world, x, y, z, x, y, z-1, meta);
				}

				if ((meta>=0&&this.canFlowLiquid(world, x, y-1, z+1)>0)||this.canFlowLiquid(world, x, y, z+1)+meta>15) {
					meta = this.setLiquid(world, x, y, z, x, y, z+1, meta);
				}
			}

			if (this==world.getBlock(x, y, z)) {
				this.meltNeighborBlocks(world, x, y, z, random);
			}

			if (meta!=prevMeta) {
				world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
			}
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block p_149695_5_) {
		if (world.getBlock(x, y, z)==this) {
			world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
		}

		//溶岩では、水との反応の処理を行う
	}

	/**@return 流し込み可能な量(0~15, -1で不可)*/
	protected int canFlowLiquid(World world, int x, int y, int z) {
		if (world.isAirBlock(x, y, z)) {
			return 15;
		} else if (world.getBlock(x, y, z)==this) {
			return 14-world.getBlockMetadata(x, y, z);
		}
		return -1;
	}

	/**
	 * ※BlockUpdateのスケジュール無し
	 * @return 処理後の自身のメタデータ(流れ切った場合は-1)
	 */
	protected int setLiquid(World world, int x, int y, int z, int targetX, int targetY, int targetZ, int myMetadata) {
		int i0 = this.canFlowLiquid(world, targetX, targetY, targetZ);
		if (i0>=0) {
			world.setBlock(targetX, targetY, targetZ, this, this.clampMetadata(15-i0), 2);
			world.scheduleBlockUpdate(targetX, targetY, targetZ, this, this.tickRate(world));
			if (myMetadata>0) {
				--myMetadata;
				world.setBlockMetadataWithNotify(x, y, z, this.clampMetadata(myMetadata), 2);
				return myMetadata;
			} else {
				world.setBlockToAir(x, y, z);
				return -1;
			}
		}
		return myMetadata;
	}

	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param block
	 * @param amount 流し込む量(1~16)
	 * @param checkBlock ブロックがあるかどうかチェック
	 * @return 余った量(流し切った場合は0)
	 */
	public static int addLiquid(World world, int x, int y, int z, Block block, int amount, boolean checkBlock) {
		Block block0 = world.getBlock(x, y, z);
		if (!checkBlock||(block0==Blocks.air||(block0==block&&block0 instanceof BlockLiquidBase))) {
			int i0 = world.getBlockMetadata(x, y, z)+amount;
			int i1 = i0&15;
			world.setBlock(x, y, z, block, i1, 2);
			return i0>i1 ? i0-i1 : 0;
		}
		return amount;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
	}

	protected int clampMetadata(int par1) {
		return MathHelper.clamp_int(par1, 0, 15);
	}

	/*@Override
	public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int par2, int par3, int par4)
	{
	    this.setBlockBounds(blockAccess.getBlockMetadata(par2, par3, par4) & 15);
	}
	
	protected void setBlockBounds(int par1)
	{
	    int j = par1;
		float f = (float)(1 + j) / 16.0F;
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, f, 1.0F);
	}*/

	private void meltNeighborBlocks(World world, int x, int y, int z, Random random) {
		int i = random.nextInt(BlockUtil.facing.length);
		int x0 = x+BlockUtil.facing[i][0];
		int y0 = y+BlockUtil.facing[i][1];
		int z0 = z+BlockUtil.facing[i][2];
		this.meltBlock(world, x0, y0, z0);
	}

	protected void meltBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial()==RTMMaterial.fireproof||block.getMaterial()==RTMMaterial.melted||block.getMaterial()==Material.air||block.getMaterial()==Material.lava||block.getMaterial()==Material.water) {
			return;
		} else if (block.getMaterial()==Material.ground) {
			//world.setBlock(x, y, z, RTMCore.slag, 15, 2);
			this.setFire(world, x, y, z);
		} else if (block==Blocks.bedrock) {
			return;
		} else if ((block.getMaterial()==Material.rock||block.getMaterial()==Material.iron||block.getMaterial()==Material.anvil)) {
			if (block.getBlockHardness(world, x, y, z)<3.5F) {
				//world.setBlock(x, y, z, RTMCore.slag, 15, 2);
				this.setFire(world, x, y, z);
			}
		} else if (block.getMaterial()==Material.sand||block.getMaterial()==Material.clay) {
			return;
		} else if (block==Blocks.tnt) {
			world.setBlock(x, y, z, Blocks.air);
			Blocks.tnt.onBlockDestroyedByPlayer(world, x, y, z, 1);
		} else if (block.getMaterial().getCanBurn()) {
			this.setFire(world, x, y, z);
		} else if (block.getMaterial()==Material.grass||block.getMaterial()==Material.circuits||block.getMaterial()==Material.sponge||block.getMaterial()==Material.plants||block.getMaterial()==Material.coral||block.getMaterial()==Material.cactus||block.getMaterial()==Material.web||block.getMaterial()==Material.gourd) {
			this.setFire(world, x, y, z);
		} else if (block.getMaterial()==Material.fire||block.getMaterial()==Material.glass||block.getMaterial()==Material.redstoneLight||block.getMaterial()==Material.ice||block.getMaterial()==Material.packedIce||block.getMaterial()==Material.snow||block.getMaterial()==Material.craftedSnow||block.getMaterial()==Material.cake) {
			world.setBlock(x, y, z, Blocks.air, 0, 2);
		}
		//dragonEgg,portal
	}

	private void setFire(World world, int x, int y, int z) {
		for (int i = 0; i<BlockUtil.facing.length; ++i) {
			int x0 = x-BlockUtil.facing[i][0];
			int y0 = y-BlockUtil.facing[i][1];
			int z0 = z-BlockUtil.facing[i][2];
			if (world.getBlock(x0, y0, z0)==Blocks.air) {
				world.setBlock(x0, y0, z0, Blocks.fire, i, 2);
			}
		}
	}
}