package net.teamfruit.projectrtm.rtm.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.teamfruit.projectrtm.ngtlib.math.NGTMath;
import net.teamfruit.projectrtm.rtm.RTMBlock;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityCrossingGate;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityFlag;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityFluorescent;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityLight;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityPoint;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntitySignBoard;
import net.teamfruit.projectrtm.rtm.block.tileentity.TileEntityTurnstile;
import net.teamfruit.projectrtm.rtm.electric.IBlockConnective;
import net.teamfruit.projectrtm.rtm.electric.MachineType;
import net.teamfruit.projectrtm.rtm.electric.TileEntityConnector;
import net.teamfruit.projectrtm.rtm.electric.TileEntityInsulator;
import net.teamfruit.projectrtm.rtm.electric.TileEntityTicketVendor;
import net.teamfruit.projectrtm.rtm.electric.Connection.ConnectionType;
import net.teamfruit.projectrtm.rtm.entity.EntityATC;
import net.teamfruit.projectrtm.rtm.entity.EntityBumpingPost;
import net.teamfruit.projectrtm.rtm.entity.EntityInstalledObject;
import net.teamfruit.projectrtm.rtm.entity.EntityTrainDetector;
import net.teamfruit.projectrtm.rtm.modelpack.cfg.ConnectorConfig;
import net.teamfruit.projectrtm.rtm.modelpack.cfg.MachineConfig;
import net.teamfruit.projectrtm.rtm.rail.TileEntityLargeRailBase;
import net.teamfruit.projectrtm.rtm.rail.util.RailMap;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemInstalledObject extends ItemWithModel {
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;

	public ItemInstalledObject() {
		super();
		this.setHasSubtypes(true);
	}

	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, int par4, int par5, int par6, int par7, float par8, float par9, float par10) {
		if (!world.isRemote) {
			int meta = itemStack.getItemDamage();
			int x = par4;
			int y = par5;
			int z = par6;
			Block block = null;
			IstlObjType type = IstlObjType.getType(meta);

			if (par7==0)//up
			{
				--par5;
			} else if (par7==1)//down
			{
				++par5;
			} else if (par7==2)//south
			{
				--par6;
			} else if (par7==3)//north
			{
				++par6;
			} else if (par7==4)//east
			{
				--par4;
			} else if (par7==5)//west
			{
				++par4;
			}

			if (!world.isAirBlock(par4, par5, par6)) {
				return true;
			}

			if (meta<=2||type==IstlObjType.FLUORESCENT_COVERED) {
				int i1 = MathHelper.floor_double((double) (player.rotationYaw*4.0F/360.0F)+0.5D)&3;
				if (player.canPlayerEdit(par4, par5, par6, par7, itemStack)&&world.isAirBlock(par4, par5, par6)) {
					world.setBlock(par4, par5, par6, RTMBlock.fluorescent, meta, 2);
					byte dir = 0;
					switch (par7) {
						case 0:
							if (i1==0||i1==2) {
								dir = 0;
							} else if (i1==1||i1==3) {
								dir = 4;
							}
							break;
						case 1:
							if (i1==0||i1==2) {
								dir = 2;
							} else if (i1==1||i1==3) {
								dir = 6;
							}
							break;
						case 2:
							dir = 1;
							break;
						case 3:
							dir = 3;
							break;
						case 4:
							dir = 5;
							break;
						case 5:
							dir = 7;
							break;
					}
					TileEntityFluorescent tile = (TileEntityFluorescent) world.getTileEntity(par4, par5, par6);
					tile.setDir(dir);
					block = RTMBlock.fluorescent;
				}
			} else if (type==IstlObjType.CROSSING) {
				if (par7==1) {
					world.setBlock(par4, par5, par6, RTMBlock.crossingGate, 0, 3);
					TileEntityCrossingGate tile = (TileEntityCrossingGate) world.getTileEntity(par4, par5, par6);
					tile.setRotation(player, 15.0F, true);
					tile.setModelName(this.getModelName(itemStack));
					block = RTMBlock.crossingGate;
				}
			}

			else if (type==IstlObjType.TURNSTILE) {
				//当たり判定のため
				int dir = (MathHelper.floor_double((NGTMath.normalizeAngle(player.rotationYaw+180.0D)/90.0D)+0.5D)&3);
				world.setBlock(par4, par5, par6, RTMBlock.turnstile, dir, 3);
				TileEntityTurnstile tile = (TileEntityTurnstile) world.getTileEntity(par4, par5, par6);
				tile.setRotation(player, 90.0F, true);
				tile.setModelName(this.getModelName(itemStack));
				block = RTMBlock.turnstile;
			} else if (type==IstlObjType.BUMPING_POST) {
				if (par7==1&&setEntityOnRail(world, new EntityBumpingPost(world), par4, par5-1, par6, player, this.getModelName(itemStack))) {
					block = Blocks.stone;
				}
			} else if (type==IstlObjType.POINT) {
				if (par7==1) {
					world.setBlock(par4, par5, par6, RTMBlock.point, 0, 3);
					TileEntityPoint tile = (TileEntityPoint) world.getTileEntity(par4, par5, par6);
					tile.setRotation(player, 15.0F, false);
					tile.setModelName(this.getModelName(itemStack));
					block = RTMBlock.point;
				}
			} else if (type==IstlObjType.SIGNBOARD) {
				world.setBlock(par4, par5, par6, RTMBlock.signboard, par7, 3);
				TileEntitySignBoard tile = (TileEntitySignBoard) world.getTileEntity(par4, par5, par6);
				int playerFacing = (MathHelper.floor_double((NGTMath.normalizeAngle(player.rotationYaw+180.0D)/90D)+0.5D)&3);
				tile.setDirection((byte) playerFacing);
				tile.setTexture("textures/signboard/ngt_a01.png");
				block = RTMBlock.signboard;
			} else if (type==IstlObjType.TICKET_VENDOR) {
				world.setBlock(par4, par5, par6, RTMBlock.ticketVendor, 0, 3);
				TileEntityTicketVendor tile = (TileEntityTicketVendor) world.getTileEntity(par4, par5, par6);
				tile.setRotation(player, 15.0F, true);
				block = RTMBlock.ticketVendor;
			} else if (type==IstlObjType.LIGHT) {
				world.setBlock(par4, par5, par6, RTMBlock.light, par7, 3);
				TileEntityLight tile = (TileEntityLight) world.getTileEntity(par4, par5, par6);
				tile.setRotation(player, 15.0F, true);
				tile.setModelName(this.getModelName(itemStack));
				block = RTMBlock.light;
			} else if (type==IstlObjType.FLAG) {
				world.setBlock(par4, par5, par6, RTMBlock.flag, 0, 3);
				TileEntityFlag tile = (TileEntityFlag) world.getTileEntity(par4, par5, par6);
				tile.setRotation(player, 15.0F, true);
				tile.setTexture("textures/flag/flag_RTM3Anniversary.png");
				block = RTMBlock.flag;
			} else if (type==IstlObjType.ATC) {
				if (par7==1&&ItemInstalledObject.setEntityOnRail(world, new EntityATC(world), par4, par5-1, par6, player, this.getModelName(itemStack))) {
					block = Blocks.stone;
				}
			} else if (type==IstlObjType.TRAIN_DETECTOR) {
				if (par7==1&&ItemInstalledObject.setEntityOnRail(world, new EntityTrainDetector(world), par4, par5-1, par6, player, this.getModelName(itemStack))) {
					block = Blocks.stone;
				}
			} else if (type==IstlObjType.INSULATOR) {
				world.setBlock(par4, par5, par6, RTMBlock.insulator, par7, 2);
				TileEntityInsulator tile = (TileEntityInsulator) world.getTileEntity(par4, par5, par6);
				tile.setModelName(this.getModelName(itemStack));
				block = RTMBlock.insulator;
			} else if (type==IstlObjType.CONNECTOR_IN||type==IstlObjType.CONNECTOR_OUT) {
				Block block2 = world.getBlock(x, y, z);
				if (block2 instanceof IBlockConnective&&((IBlockConnective) block2).canConnect(world, x, y, z)) {
					if (type==IstlObjType.CONNECTOR_OUT) {
						par7 += 6;
					}
					world.setBlock(par4, par5, par6, RTMBlock.connector, par7, 2);
					TileEntityConnector tile = (TileEntityConnector) world.getTileEntity(par4, par5, par6);
					tile.setModelName(this.getModelName(itemStack));
					tile.setConnectionTo(x, y, z, ConnectionType.DIRECT, "");
					block = RTMBlock.connector;
				}
			}

			if (block!=null) {
				world.playSoundEffect((double) par4+0.5D, (double) par5+0.5D, (double) par6+0.5D,
						block.stepSound.func_150496_b(), (block.stepSound.getVolume()+1.0F)/2.0F, block.stepSound.getPitch()*0.8F);
				--itemStack.stackSize;
			}
		}
		return true;
	}

	public static boolean setEntityOnRail(World world, EntityInstalledObject entity, int x, int y, int z, EntityPlayer player, String modelName) {
		RailMap rm0 = TileEntityLargeRailBase.getRailMapFromCoordinates(world, null, x, y, z);
		if (rm0==null) {
			return false;
		}

		int split = 128;
		int i0 = rm0.getNearlestPoint(split, (double) x+0.5D, (double) z+0.5D);
		double posX = rm0.getRailPos(split, i0)[1];
		double posY = rm0.getRailHeight(split, i0)+0.0625D;
		double posZ = rm0.getRailPos(split, i0)[0];
		float yaw = rm0.getRailRotation(split, i0);
		float yaw2 = -player.rotationYaw+180.0F;
		float dif = MathHelper.wrapAngleTo180_float(yaw-yaw2);
		if (Math.abs(dif)>90.0F) {
			yaw += 180.0F;
		}

		entity.setPosition(posX, posY, posZ);
		entity.rotationYaw = yaw;
		entity.rotationPitch = 0.0F;
		world.spawnEntityInWorld(entity);
		entity.setModelName(modelName);
		return true;
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack) {
		return super.getUnlocalizedName()+"."+itemStack.getItemDamage();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item par1, CreativeTabs tab, List list) {
		for (IstlObjType type : IstlObjType.values()) {
			if (type==IstlObjType.NONE) {
				continue;
			}
			list.add(new ItemStack(par1, 1, type.id));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int par1) {
		int j = MathHelper.clamp_int(par1, 0, 20);
		return this.icons[j];
	}

	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		IIcon missing = register.registerIcon("ngtlib.missing");
		this.icons = new IIcon[21];
		this.icons[IstlObjType.FLUORESCENT_GLASS.id] = register.registerIcon("rtm:fluorescent");
		this.icons[IstlObjType.FLUORESCENT_DIAMOND.id] = register.registerIcon("rtm:fluorescent");
		this.icons[IstlObjType.FLUORESCENT_BROKEN.id] = register.registerIcon("rtm:fluorescent");
		this.icons[IstlObjType.INSULATOR.id] = register.registerIcon("rtm:insulator");
		this.icons[IstlObjType.FLUORESCENT_COVERED.id] = register.registerIcon("rtm:fluorescent");
		this.icons[IstlObjType.CROSSING.id] = register.registerIcon("rtm:crossing");
		this.icons[6] = missing;
		this.icons[7] = missing;
		this.icons[IstlObjType.CONNECTOR_IN.id] = register.registerIcon("rtm:itemConnector_in");
		this.icons[IstlObjType.CONNECTOR_OUT.id] = register.registerIcon("rtm:itemConnector_out");
		this.icons[IstlObjType.ATC.id] = register.registerIcon("rtm:itemATC");
		this.icons[IstlObjType.TRAIN_DETECTOR.id] = register.registerIcon("rtm:itemTrainDetector");
		this.icons[IstlObjType.TURNSTILE.id] = register.registerIcon("rtm:itemTurnstile");
		this.icons[IstlObjType.BUMPING_POST.id] = register.registerIcon("rtm:itemBumpingPost");
		this.icons[14] = missing;
		this.icons[15] = missing;
		this.icons[IstlObjType.POINT.id] = register.registerIcon("rtm:point");
		this.icons[IstlObjType.SIGNBOARD.id] = register.registerIcon("rtm:itemSignBoard");
		this.icons[IstlObjType.TICKET_VENDOR.id] = register.registerIcon("rtm:itemTicketVendor");
		this.icons[IstlObjType.LIGHT.id] = register.registerIcon("rtm:lightBlock");
		this.icons[IstlObjType.FLAG.id] = register.registerIcon("rtm:flag");
	}

	@Override
	protected String getModelType(ItemStack itemStack) {
		return IstlObjType.getType(itemStack.getItemDamage()).modelType;
	}

	@Override
	protected String getDefaultModelName(ItemStack itemStack) {
		return IstlObjType.getType(itemStack.getItemDamage()).defaultModel;
	}

	@Override
	public String getSubType(ItemStack itemStack) {
		return IstlObjType.getType(itemStack.getItemDamage()).subType;
	}

	public enum IstlObjType {
		FLUORESCENT_GLASS(0, "", "", ""),
		FLUORESCENT_DIAMOND(1, "", "", ""),
		FLUORESCENT_BROKEN(2, "", "", ""),
		/**碍子*/
		INSULATOR(3, ConnectorConfig.TYPE, "Relay", "Insulator01"),
		FLUORESCENT_COVERED(4, "", "", ""),
		/**遮断器*/
		CROSSING(5, MachineConfig.TYPE, MachineType.Gate.toString(), "CrossingGate01L"),
		CONNECTOR_IN(8, ConnectorConfig.TYPE, "Input", "Input01"),
		CONNECTOR_OUT(9, ConnectorConfig.TYPE, "Output", "Output01"),
		ATC(10, MachineConfig.TYPE, MachineType.Antenna_Send.toString(), "ATC_01"),
		TRAIN_DETECTOR(11, MachineConfig.TYPE, MachineType.Antenna_Receive.toString(), "TrainDetector_01"),
		/**改札機*/
		TURNSTILE(12, MachineConfig.TYPE, MachineType.Turnstile.toString(), "Turnstile01"),
		/**車止め*/
		BUMPING_POST(13, MachineConfig.TYPE, MachineType.BumpingPost.toString(), "BumpingPost_Type2"),
		POINT(16, MachineConfig.TYPE, MachineType.Point.toString(), "Point01M"),
		SIGNBOARD(17, "", "", ""),
		TICKET_VENDOR(18, "", "", ""),
		LIGHT(19, MachineConfig.TYPE, MachineType.Light.toString(), "SearchLight01"),
		FLAG(20, "", "", ""),
		NONE(-1, "", "", "");

		public final byte id;
		public final String modelType;
		public final String subType;
		public final String defaultModel;

		private IstlObjType(int par1, String par2, String par3, String par4) {
			this.id = (byte) par1;
			this.modelType = par2;
			this.subType = par3;
			this.defaultModel = par4;
		}

		public static IstlObjType getType(int id) {
			for (IstlObjType type : IstlObjType.values()) {
				if (type.id==id) {
					return type;
				}
			}
			return NONE;
		}
	}
}