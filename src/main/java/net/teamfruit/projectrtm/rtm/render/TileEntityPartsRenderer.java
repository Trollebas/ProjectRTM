package net.teamfruit.projectrtm.rtm.render;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetBase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class TileEntityPartsRenderer<MS extends ModelSetBase> extends PartsRenderer<TileEntity, MS> {
	public TileEntityPartsRenderer(String... par1) {
		super(par1);
	}

	public int getMetadata(TileEntity par1) {
		return par1==null ? 0 : ((TileEntity) par1).getBlockMetadata();
	}

	@Override
	public World getWorld(TileEntity entity) {
		return entity.getWorldObj();
	}

	public int getX(TileEntity entity) {
		return entity.xCoord;
	}

	public int getY(TileEntity entity) {
		return entity.yCoord;
	}

	public int getZ(TileEntity entity) {
		return entity.zCoord;
	}
}