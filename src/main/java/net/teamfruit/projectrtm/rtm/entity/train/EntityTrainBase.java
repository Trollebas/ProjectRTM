package net.teamfruit.projectrtm.rtm.entity.train;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.teamfruit.projectrtm.ngtlib.io.NGTLog;
import net.teamfruit.projectrtm.ngtlib.math.NGTMath;
import net.teamfruit.projectrtm.ngtlib.util.NGTUtil;
import net.teamfruit.projectrtm.rtm.RTMAchievement;
import net.teamfruit.projectrtm.rtm.RTMCore;
import net.teamfruit.projectrtm.rtm.RTMItem;
import net.teamfruit.projectrtm.rtm.entity.npc.EntityMotorman;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityVehiclePart;
import net.teamfruit.projectrtm.rtm.entity.train.util.BogieController;
import net.teamfruit.projectrtm.rtm.entity.train.util.Formation;
import net.teamfruit.projectrtm.rtm.entity.train.util.FormationEntry;
import net.teamfruit.projectrtm.rtm.entity.train.util.FormationManager;
import net.teamfruit.projectrtm.rtm.entity.train.util.TrainSpeedManager;
import net.teamfruit.projectrtm.rtm.entity.train.util.TrainState;
import net.teamfruit.projectrtm.rtm.entity.train.util.TrainState.TrainStateType;
import net.teamfruit.projectrtm.rtm.entity.vehicle.EntityVehicleBase;
import net.teamfruit.projectrtm.rtm.entity.vehicle.VehicleTrackerEntry;
import net.teamfruit.projectrtm.rtm.modelpack.cfg.TrainConfig;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetTrain;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetTrainClient;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetVehicleBase;
import net.teamfruit.projectrtm.rtm.rail.TileEntityLargeRailBase;
import net.teamfruit.projectrtm.rtm.rail.TileEntityLargeRailCore;
import net.teamfruit.projectrtm.rtm.world.IChunkLoader;
import net.teamfruit.projectrtm.rtm.world.RTMChunkManager;

import org.apache.commons.codec.binary.Base64;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class EntityTrainBase extends EntityVehicleBase<TrainConfig> implements IChunkLoader {
	private static final byte DW_Bogie0 = 21;
	private static final byte DW_Bogie1 = 22;
	private static final byte DW_FormationData = 23;
	private static final byte DW_ByteArray = 24;

	public static final short MAX_AIR_COUNT = 2880;
	public static final short MIN_AIR_COUNT = 2480;
	public static final float TRAIN_WIDTH = 2.75F;
	public static final float TRAIN_HEIGHT = 1.25F-0.0625F;//レールに合わせ高さ修正

	private BogieController bogieController = new BogieController();
	private Formation formation;

	private float trainSpeed;
	/**notch x -18*/
	public int brakeCount = 72;
	public int atsCount;
	@SideOnly(Side.CLIENT)
	public int brakeAirCount;
	@SideOnly(Side.CLIENT)
	public boolean complessorActive;
	/**TrainTrackerEntryを生成したかどうか*/
	private boolean tracked;

	@SideOnly(Side.CLIENT)
	private int carPosRotationIncrements;
	@SideOnly(Side.CLIENT)
	private double carX, carY, carZ;
	@SideOnly(Side.CLIENT)
	private float carYaw, carPitch;
	@SideOnly(Side.CLIENT)
	private float carSpeed;

	public EntityTrainBase(World world) {
		super(world);
		this.setSize(TRAIN_WIDTH, TRAIN_HEIGHT);
		this.yOffset = TRAIN_HEIGHT;
		this.noClip = true;
	}

	public EntityTrainBase(World world, String s) {
		this(world);
		this.bogieController.createBogie(world, this);
	}

	public void spawnTrain(World world) {
		//spawnの順番は「先に台車」じゃないと台車描画時にガクガク（lastTickPos更新のタイミングのせい？）
		this.bogieController.setupBogiePos(this);
		this.bogieController.spawnBogies(world, this);
		//this.bogieController.moveTrainWithBogie(this, 0.0F);
		world.spawnEntityInWorld(this);
		this.formation = FormationManager.getInstance().createNewFormation(this);
	}

	/*@Override
	public Entity[] getParts()
	{
	    return null;
	}*/

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(DW_Bogie0, Integer.valueOf(0));
		this.dataWatcher.addObject(DW_Bogie1, Integer.valueOf(0));
		this.dataWatcher.addObject(DW_FormationData, new String(""));
		byte[] ba = Base64.decodeBase64(new byte[16]);
		this.dataWatcher.addObject(DW_ByteArray, Base64.encodeBase64String(ba));
	}

	@Override
	protected void setSize(float par1, float par2) {
		super.setSize(par1, par2);
		this.myEntitySize = Entity.EnumEntitySize.SIZE_1;//6だと台車とずれる？
	}

	@Override
	public AxisAlignedBB getCollisionBox(Entity par1) {
		return (par1 instanceof EntityVehiclePart) ? null : par1.boundingBox;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);

		NBTTagCompound entryData = new NBTTagCompound();
		this.writeFormationData(entryData);
		nbt.setTag("FormationEntry", entryData);

		nbt.setInteger("trainDir", this.getTrainDirection());
		nbt.setString("byteArray", this.dataWatcher.getWatchableObjectString(DW_ByteArray));
	}

	private void writeFormationData(NBTTagCompound nbt) {
		FormationEntry entry = this.formation.getEntry(this);
		nbt.setLong("FormationId", this.formation.id);
		nbt.setByte("EntryPos", entry.entryId);
		nbt.setByte("EntryDir", entry.dir);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);

		NBTTagCompound entryData = nbt.getCompoundTag("FormationEntry");
		this.readFormationData(entryData);

		this.setTrainDirection(nbt.getInteger("trainDir"));
		this.dataWatcher.updateObject(DW_ByteArray, nbt.getString("byteArray"));
	}

	private void readFormationData(NBTTagCompound nbt) {
		long id = nbt.getLong("FormationId");
		byte pos = nbt.getByte("EntryPos");
		byte dir = nbt.getByte("EntryDir");
		Formation f0 = FormationManager.getInstance().getFormation(id);
		if (f0==null) {
			this.formation = FormationManager.getInstance().createNewFormation(this);
		} else {
			this.formation = f0;
			f0.setTrain(this, pos, dir);
		}
	}

	@Override
	public void setDead() {
		super.setDead();

		if (!this.worldObj.isRemote) {
			this.releaseTicket();
			this.bogieController.setDead();

			try {
				this.formation.onRemovedTrain(this);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (!this.worldObj.isRemote) {
			this.updateChunks();
		}

		this.updateSpeed();

		if (this.existBogies()) {
			if (!this.worldObj.isRemote) {
				this.moveTrain();
			}
			this.bogieController.updateBogies();
		}

		if (this.worldObj.isRemote) {
			if (this.carPosRotationIncrements>0) {
				float f0 = 1.0F/(float) this.carPosRotationIncrements;
				double x = this.posX+(this.carX-this.posX)*(double) f0;
				double y = this.posY+(this.carY-this.posY)*(double) f0;
				double z = this.posZ+(this.carZ-this.posZ)*(double) f0;
				float yaw = this.rotationYaw+MathHelper.wrapAngleTo180_float(this.carYaw-this.rotationYaw)*f0;
				float pitch = this.rotationPitch+(this.carPitch-this.rotationPitch)*f0;
				this.setPosition(x, y, z);
				this.setRotation(yaw, pitch);
				float f2 = this.trainSpeed+(this.carSpeed-this.trainSpeed)*f0;
				this.setSpeed(f2);
				--this.carPosRotationIncrements;
			} else {
				this.setPosition(this.posX, this.posY, this.posZ);
				this.setRotation(this.rotationYaw, this.rotationPitch);
			}

			ModelSetVehicleBase<TrainConfig> set = this.getModelSet();
			if (set.getConfig().smoke!=null) {
				float speed = this.getSpeed();
				int notch = this.getNotch();
				Random random = this.worldObj.rand;

				for (int i = 0; i<set.getConfig().smoke.length; ++i) {
					Vec3 vec3 = Vec3.createVectorHelper((Double) set.getConfig().smoke[i][0], (Double) set.getConfig().smoke[i][1], (Double) set.getConfig().smoke[i][2]);
					vec3.rotateAroundX(NGTMath.toRadians(this.rotationPitch));
					vec3.rotateAroundY(NGTMath.toRadians(this.rotationYaw));
					double min = (Double) set.getConfig().smoke[i][4];
					double max = (Double) set.getConfig().smoke[i][5];
					int amount = speed>0.05F ? (int) max : (notch>0 ? (int) min+3 : (int) min);
					for (int j = 0; j<amount; ++j) {
						double d0 = this.posX+vec3.xCoord+(double) random.nextFloat()*0.5D-0.25D;
						double d1 = this.posY+vec3.yCoord;
						double d2 = this.posZ+vec3.zCoord+(double) random.nextFloat()*0.5D-0.25D;
						double smokeSpeed = 0.0625D;
						if (set.getConfig().smoke.length==7) {
							smokeSpeed = (Double) set.getConfig().smoke[i][6];
						}
						double vx = (random.nextDouble()*2.0D-1.0D)*smokeSpeed;
						double vz = (random.nextDouble()*2.0D-1.0D)*smokeSpeed;
						this.worldObj.spawnParticle((String) set.getConfig().smoke[i][3], d0, d1, d2, vx, 0.25D, vz);
					}
				}
			}
		} else//!isRemote
		{
			if (this.atsCount>0) {
				++this.atsCount;
				if (this.atsCount>=100) {
					this.stopTrain(false);
					this.atsCount = 0;
				}
			}

			if (!this.tracked) {
				this.tracked = VehicleTrackerEntry.trackingVehicle(this);
			}

			TileEntityLargeRailBase rail = TileEntityLargeRailBase.getRailFromCoordinates(this.worldObj, this.posX, this.posY, this.posZ);
			if (rail!=null) {
				TileEntityLargeRailCore railCore = rail.getRailCore();
				if (railCore!=null) {
					int signal = railCore.getSignal();
					this.setSignal(signal);
				}
			}

			this.func_145775_I();//call Block.onEntityCollidedWithBlock()
		}

		float f0 = -this.getModelSet().getConfig().rolling;
		float roll = MathHelper.wrapAngleTo180_float((this.rotationYaw-this.prevRotationYaw)*f0);
		if (this.getTrainDirection()==1) {
			roll *= -1.0F;
		}
		this.rotationRoll = roll;
	}

	@SideOnly(Side.CLIENT)
	@Override
	protected void updateAnimation() {
		super.updateAnimation();

		if (this.getTrainDirection()==0&&this.seatRotation>-MAX_SEAT_ROTATION) {
			--this.seatRotation;
		}

		if (this.getTrainDirection()==1&&this.seatRotation<MAX_SEAT_ROTATION) {
			++this.seatRotation;
		}

		this.setRollsignAnimation(this.getTrainStateData(8));

		if (this.rollsignAnimation>this.rollsignV) {
			--this.rollsignAnimation;
		} else if (this.rollsignAnimation<this.rollsignV) {
			++this.rollsignAnimation;
		}

		int doorState = this.getTrainStateData(TrainStateType.State_Door.id);
		if ((doorState&1)==1)//R
		{
			if (this.doorMoveR<MAX_DOOR_MOVE) {
				if (this.doorMoveR==0) {
					RTMCore.proxy.playSound(this, this.getModelSet().sound_doorOpen, 1.0F, 1.0F);
				}
				++this.doorMoveR;
			}
		} else {
			if (this.doorMoveR>0) {
				if (this.doorMoveR==MAX_DOOR_MOVE) {
					RTMCore.proxy.playSound(this, this.getModelSet().sound_doorClose, 1.0F, 1.0F);
				}
				--this.doorMoveR;
			}
		}

		if ((doorState&2)==2)//L
		{
			if (this.doorMoveL<MAX_DOOR_MOVE) {
				if (this.doorMoveL==0) {
					RTMCore.proxy.playSound(this, this.getModelSet().sound_doorOpen, 1.0F, 1.0F);
				}
				++this.doorMoveL;
			}
		} else {
			if (this.doorMoveL>0) {
				if (this.doorMoveL==MAX_DOOR_MOVE) {
					RTMCore.proxy.playSound(this, this.getModelSet().sound_doorClose, 1.0F, 1.0F);
				}
				--this.doorMoveL;
			}
		}

		int pantoState = this.getTrainStateData(TrainStateType.State_Pantograph.id);
		if (pantoState==TrainState.Pantograph_Down.data) {
			if (this.pantograph_F<MAX_PANTOGRAPH_MOVE) {
				++this.pantograph_F;
			}

			if (this.pantograph_B<MAX_PANTOGRAPH_MOVE) {
				++this.pantograph_B;
			}
		} else {
			if (this.pantograph_F>0) {
				--this.pantograph_F;
			}

			if (this.pantograph_B>0) {
				--this.pantograph_B;
			}
		}

		if (this.complessorActive) {
			++this.brakeAirCount;
			if (this.brakeAirCount>=MAX_AIR_COUNT) {
				this.complessorActive = false;
				//this.playBrakeReleaseSound(false);
				RTMCore.proxy.playSound(this, new ResourceLocation("rtm", "train.cp_fin"), 1.0F, 1.0F);
			}
		} else {
			if (this.brakeAirCount<MIN_AIR_COUNT) {
				this.complessorActive = true;
			}
		}
	}

	protected void playBrakeReleaseSound(boolean isStrong) {
		String sound;
		if (this.getModelSet() instanceof ModelSetTrain) {
			ModelSetTrain modelSet = (ModelSetTrain) this.getModelSet();
			sound = isStrong ? modelSet.sound_brakeRelease_s : modelSet.sound_brakeRelease_w;
		} else {
			ModelSetTrainClient modelSet = (ModelSetTrainClient) this.getModelSet();
			sound = isStrong ? modelSet.sound_brakeRelease_s : modelSet.sound_brakeRelease_w;
		}

		if (sound!=null) {
			String[] sa = sound.split(":");
			RTMCore.proxy.playSound(this, new ResourceLocation(sa[0], sa[1]), 1.0F, 1.0F);
			//this.worldObj.playSoundAtEntity(this, sound, 1.0F, 1.0F);
		}
	}

	protected void updateSpeed() {
		int notch = this.getNotch();

		if (this.riddenByEntity==null||!(this.riddenByEntity instanceof EntityPlayer||this.riddenByEntity instanceof EntityMotorman)) {
			if (notch>0) {
				this.setNotch(0);
			}
		}

		boolean isBrakeDisabled = true;
		float speed = this.getSpeed();

		//ブレーキ処理, 全ての車両で
		if (notch<0) {
			int max = notch<0 ? notch*-18 : 0;
			if (this.brakeCount<max) {
				++this.brakeCount;
				if (this.worldObj.isRemote) {
					--this.brakeAirCount;
				}
			} else if (this.brakeCount>max) {
				this.brakeCount -= (this.brakeCount-max>1) ? 2 : 1;
			}
		} else if (notch>=0)//ブレーキ解
		{
			if (this.brakeCount>0) {
				if (speed<=0.0F) {
					isBrakeDisabled = false;
				}
				this.brakeCount -= 2;
			} else if (this.brakeCount<0) {
				this.brakeCount = 0;
			}
		}

		//速度処理, 先頭車のみ
		if (this.isControlCar()) {
			if (isBrakeDisabled&&!this.worldObj.isRemote) {
				ModelSetVehicleBase<TrainConfig> set = this.getModelSet();
				float acceleration = TrainSpeedManager.getAcceleration(notch, speed, set.getConfig());

				if (notch>=0)//ブレーキ解
				{
					float deceleration;
					if (this.rotationPitch==0.0F) {
						float f1 = 0.0002F;
						deceleration = speed>0.0F ? f1 : (speed<0.0F ? -f1 : 0.0F);
					} else//坂
					{
						float dec = 0.0125F;
						float f2 = (this.getTrainDirection()==0) ? dec : -dec;
						//-1.0~1.0, 斜面で下方向へ加速
						deceleration = NGTMath.sin(this.rotationPitch)*f2;
					}
					speed += acceleration-deceleration;
				} else {
					speed += speed>0.0F ? acceleration : (speed<0.0F ? -acceleration : 0.0F);
				}

				this.setSpeed(speed);
			}
		}
	}

	/**
	 * ※ServerOnly<br>
	 * ※Bogieのnullチェック必須
	 */
	protected void moveTrain() {
		float speed = this.getSpeed();

		/*車間距離調整**********************************************/
		EntityTrainBase connectedTrain = this.getConnectedTrain(this.getTrainDirection());
		if (connectedTrain!=null&&!this.getModelSet().isDummy())//ModelSetロードまだの時は飛ばす
		{
			double dis = this.getDistanceSqToEntity(connectedTrain);
			double def = this.getDefaultDistanceToConnectedTrain(connectedTrain);
			double d0 = 0.125D;

			if (dis>(def+d0)*(def+d0)) {
				speed += 0.0125F;
			}

			if (dis<(def-d0)*(def-d0)) {
				speed -= 0.0125F;
			}
		}

		this.bogieController.moveTrainWithBogie(this, speed);
	}

	@Override
	public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}

	@Override
	protected void setRotation(float yaw, float pitch) {
		this.rotationYaw = MathHelper.wrapAngleTo180_float(yaw);
		this.rotationPitch = MathHelper.wrapAngleTo180_float(pitch);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
		this.carPosRotationIncrements = par9;
		this.carX = par1;
		this.carY = par3;
		this.carZ = par5;
		this.carYaw = par7;
		this.carPitch = par8;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setVelocity(double par1, double par3, double par5) {
		this.carSpeed = (float) par1;
	}

	@Override
	public void moveEntity(double par1, double par3, double par5) {
	}

	@Override
	public void addVelocity(double par1, double par3, double par5) {
	}

	/**連結時の車両同士の距離*/
	public double getDefaultDistanceToConnectedTrain(EntityTrainBase par1) {
		ModelSetVehicleBase<TrainConfig> modelSet0 = this.getModelSet();
		ModelSetVehicleBase<TrainConfig> modelSet1 = par1.getModelSet();
		if (modelSet0!=null&&modelSet1!=null) {
			double d0 = modelSet0.getConfig().trainDistance;
			double d1 = modelSet1.getConfig().trainDistance;
			return d0+d1;
		}
		return 20.25D;
	}

	/**車両同士が連結可能な距離内にあるか*/
	public boolean inConnectableRange(EntityTrainBase par1) {
		double d0 = this.getDefaultDistanceToConnectedTrain(par1);
		return this.getDistanceSqToEntity(par1)<=d0*d0;
	}

	@Override
	public void updateRiderPosition() {
		if (this.riddenByEntity!=null) {
			float[][] pos = this.getModelSet().getConfig().getPlayerPos();
			int dir = this.getTrainDirection();
			Vec3 v31 = Vec3.createVectorHelper(pos[dir][0], pos[dir][1], pos[dir][2]);
			v31.rotateAroundX(NGTMath.toRadians(this.rotationPitch));
			v31.rotateAroundY(NGTMath.toRadians(this.rotationYaw));
			Vec3 v32 = v31.addVector(this.posX, this.posY, this.posZ);
			this.riddenByEntity.setPosition(v32.xCoord, v32.yCoord+this.getMountedYOffset()+this.riddenByEntity.getYOffset(), v32.zCoord);

			//運転手のYaw調整, PlayerのYawは他のEntityとは逆向き
			this.riddenByEntity.rotationYaw -= MathHelper.wrapAngleTo180_float(this.rotationYaw-this.prevRotationYaw);
		}
	}

	@Override
	public double getMountedYOffset() {
		return (double) (this.height-0.93F);
	}

	@Override
	public boolean attackEntityFrom(DamageSource par1, float par2) {
		if (this.isEntityInvulnerable()||this.isDead) {
			return false;
		} else {
			if (!par1.isExplosion()&&par1.getEntity() instanceof EntityPlayer) {
				if (!this.worldObj.isRemote) {
					EntityPlayer player = (EntityPlayer) par1.getEntity();
					if (!player.capabilities.isCreativeMode) {
						int damage = 0;
						ModelSetVehicleBase<TrainConfig> model = this.getModelSet();
						if (model!=null) {
							String type = model.getConfig().getSubType();
							damage = type.equals("DC") ? 0 : (type.equals("EC") ? 1 : (type.equals("CC") ? 2 : 3));
						}
						this.entityDropItem(new ItemStack(RTMItem.itemtrain, 1, damage), 0.0F);
					}

					this.setDead();
				}
				return true;
			}
		}
		return false;
	}

	/**EntityBogieで呼ばれる*/
	protected boolean interactTrain(EntityBogie bogie, EntityPlayer player) {
		if (this.riddenByEntity!=null&&!this.riddenByEntity.equals(player)) {
			return true;
		} else {
			if (!this.worldObj.isRemote) {
				boolean canRide = this.getModelSet()!=null;
				ItemStack itemstack = player.inventory.getCurrentItem();
				int id1 = bogie.getBogieId();
				if (itemstack!=null) {
					if (itemstack.getItem()==RTMItem.crowbar) {
						if (id1>=0) {
							if (this.getConnectedTrain(id1)==null) {
								bogie.isActivated = true;
								NGTLog.sendChatMessage(player, "message.train.concatenation_mode");
							} else {
								this.formation.onDisconnectedTrain(this, id1);
								NGTLog.sendChatMessage(player, "message.train.deconcatenation");
							}
						}
					} else if (itemstack.getItem()==RTMItem.itemMotorman) {
						if (canRide&&id1>=0) {
							EntityMotorman motorman = new EntityMotorman(this.worldObj, player);
							motorman.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
							if (this.worldObj.spawnEntityInWorld(motorman)) {
								this.mountEntityToTrain(motorman, id1);
								--itemstack.stackSize;
							}
						}
					} else if (itemstack.getItem()==RTMItem.paddle) {
						NGTLog.sendChatMessage(player, "UUID:"+this.getUniqueID().toString()+"(bogie, found train)");
					}
				} else {
					if (canRide&&id1>=0) {
						this.mountEntityToTrain(player, id1);
						player.addStat(RTMAchievement.rideTrain, 1);
					}
				}
			}
			return true;
		}
	}

	private void mountEntityToTrain(Entity entity, int direction) {
		if (this.getTrainDirection()!=direction) {
			this.setSpeed(-this.getSpeed());
		}
		this.setTrainDirection(direction);
		entity.mountEntity(this);
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (player.isSneaking()) {
			if (this.worldObj.isRemote) {
				player.openGui(RTMCore.instance, RTMCore.instance.guiIdSelectEntityModel, player.worldObj, this.getEntityId(), 0, 0);
			}
			return true;
		}

		if (NGTUtil.isEquippedItem(player, RTMItem.paddle)) {
			//Debug
			if (!this.worldObj.isRemote) {
				NGTLog.sendChatMessage(player, "UUID:"+this.getUniqueID().toString()+"(train)");
			}
			return true;
		}
		return false;
	}

	@Override
	protected void onModelChanged(ModelSetVehicleBase<TrainConfig> par1) {
		super.onModelChanged(par1);

		if (!this.worldObj.isRemote) {
			TrainConfig cfg = par1.getConfig();
			float[][] fa = cfg.getBogiePos();
			float f0 = 0.0F;
			float f1 = Math.abs(fa[0][2]-fa[1][2])*0.5F;
			float f2 = (float) (cfg.trainDistance/3.0D*2.0D);

			if (f1<TRAIN_WIDTH||f2<TRAIN_WIDTH) {
				f0 = f1<f2 ? f1 : f2;
				this.setSize(f0, TRAIN_HEIGHT);

				if (this.existBogies()) {
					this.getBogie(0).setBogieSize(f0, TRAIN_HEIGHT);
					this.getBogie(1).setBogieSize(f0, TRAIN_HEIGHT);
				}
			}
		}
	}

	/**
	 * @param par1 連結される台車
	 * @param par2 連結対象の台車
	 */
	public void connectTrain(EntityBogie par1, EntityBogie par2) {
		if (!this.worldObj.isRemote&&par2.getTrain()!=null) {
			int id1 = par1.getBogieId();
			int id2 = par2.getBogieId();
			if (id1>=0&&id2>=0&&this.getConnectedTrain(id1)==null&&par2.getTrain().formation!=null) {
				this.formation.connectTrain(this, par2.getTrain(), id1, id2, par2.getTrain().formation);
				this.worldObj.playAuxSFX(1022, (int) par1.posX, (int) par1.posY, (int) par1.posZ, 0);

				EntityPlayer player = null;
				if (this.riddenByEntity instanceof EntityPlayer) {
					player = (EntityPlayer) this.riddenByEntity;
				} else if (par2.getTrain().riddenByEntity instanceof EntityPlayer) {
					player = (EntityPlayer) par2.getTrain().riddenByEntity;
				}

				if (player!=null) {
					NGTLog.sendChatMessage(player, "message.train.concatenated");
				}
			}
		}
	}

	@Override
	public String getModelType() {
		return "ModelTrain";
	}

	@Override
	public String getSubType() {
		return this.getModelSet().getConfig().getSubType();
	}

	@Override
	public String getDefaultName() {
		return "kiha600";
	}

	@Override
	public float getSpeed() {
		return this.trainSpeed;
	}

	protected void setSpeed(float par1) {
		if (this.worldObj.isRemote) {
			this.trainSpeed = par1;
		} else {
			if (this.isControlCar()) {
				this.formation.setSpeed(par1);
			}
		}
	}

	public void setSpeed_NoSync(float par1) {
		this.trainSpeed = par1;
	}

	public void stopTrain(boolean changeSpeed) {
		if (this.formation!=null) {
			this.setNotch(-8);
			if (changeSpeed) {
				this.setSpeed(0.0F);
			}
		}
	}

	public boolean isControlCar() {
		return this.getTrainStateData(TrainStateType.State_Direction.id)==TrainState.Direction_Front.data;
	}

	public boolean existBogies() {
		//当クラスのgetBogie使わないとClientでのBogie登録がされない
		return this.getBogie(0)!=null&&this.getBogie(1)!=null;
	}

	public EntityBogie getBogie(int bogieId) {
		if (this.bogieController.getBogie(bogieId)==null) {
			int id = bogieId==0 ? DW_Bogie0 : DW_Bogie1;
			Entity entity = this.worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(id));
			if (entity instanceof EntityBogie) {
				this.bogieController.setBogie(bogieId, (EntityBogie) entity);
			}
		}
		return this.bogieController.getBogie(bogieId);
	}

	/**EntityBogieから呼び出し*/
	public void setBogie(int id, EntityBogie bogie) {
		this.bogieController.setBogie(id, bogie);
		int dwid = id==0 ? DW_Bogie0 : DW_Bogie1;
		this.dataWatcher.updateObject(dwid, Integer.valueOf(bogie.getEntityId()));
	}

	/**@param par1 0 or 1*/
	public EntityTrainBase getConnectedTrain(int par1) {
		if (this.formation!=null) {
			FormationEntry entry = this.formation.getEntry(this);
			if (entry==null) {
				return null;
			}
			int pos = entry.entryId;
			int dif = (par1==0) ? -1 : 1;
			if (entry.dir==1) {
				dif *= -1;
			}
			pos += dif;
			if (pos<0||pos>=this.formation.size()) {
				return null;
			}
			FormationEntry connected = this.formation.get(pos);
			if (connected!=null) {
				return connected.train;
			}
		}
		return null;
	}

	public Formation getFormation() {
		return this.formation;
	}

	/**
	 * @param par1
	 * @param i 位置
	 * @param par3 向き
	 */
	public void setFormation(Formation par1) {
		this.formation = par1;
	}

	private byte[] getByteArray() {
		byte[] ba = Base64.decodeBase64(this.dataWatcher.getWatchableObjectString(DW_ByteArray));
		return ba.length<16 ? new byte[16] : ba;
	}

	/**@param par1 : 識別番号(0~15)*/
	private byte getByteFromDataWatcher(int par1) {
		byte[] ba = this.getByteArray();
		return ba[par1];
	}

	/**
	 * byte型データをDataWatcherで同期
	 * @param par1 : 識別番号(0~15)
	 * @param par2 : data
	 */
	private void setByteToDataWatcher(int par1, byte par2) {
		byte[] ba = this.getByteArray();
		ba[par1] = par2;
		String result = Base64.encodeBase64String(ba);
		this.dataWatcher.updateObject(DW_ByteArray, result);
	}

	/**0:前, 1:後*/
	public int getTrainDirection() {
		return this.getByteFromDataWatcher(TrainStateType.State_TrainDir.id);
	}

	public void setTrainDirection(int par1) {
		if (this.formation==null) {
			this.setTrainDirection_NoSync((byte) par1);
		} else {
			this.formation.setTrainDirection((byte) par1, this);
		}
	}

	public void setTrainDirection_NoSync(byte par1) {
		this.setByteToDataWatcher(TrainStateType.State_TrainDir.id, par1);
		int id2 = 1-par1;
		if (id2<2) {
			if (this.existBogies()) {
				this.getBogie(par1).setFront(true);
				this.getBogie(id2).setFront(false);
			}
		}
	}

	public int getNotch() {
		return this.getByteFromDataWatcher(TrainStateType.State_Notch.id);
	}

	/**プレーヤーが変更したとき呼ぶ*/
	public boolean addNotch(Entity driver, int par2) {
		if (par2!=0) {
			int i = this.getNotch();
			if (this.setNotch(i+par2)) {
				driver.worldObj.playSoundAtEntity(driver, "rtm:train.lever", 1.0F, 1.0F);
				if (i<0&&par2>0&&!this.worldObj.isRemote) {
					this.playBrakeReleaseSound(i==-1);
				}
				return true;
			}
		}
		return false;
	}

	public boolean setNotch(int par1) {
		if (this.isControlCar()) {
			if (par1<=5&&par1>=-8) {
				int prevNotch = this.getNotch();
				if (prevNotch!=par1) {
					this.setByteToDataWatcher(TrainStateType.State_Notch.id, (byte) par1);
					return true;
				}
			}
		}
		return false;
	}

	/**停止現示でATS確認後は-1*/
	public int getSignal() {
		return this.getByteFromDataWatcher(TrainStateType.State_Signal.id);
	}

	public void setSignal(int par1) {
		int signal = this.getSignal();
		if (par1>0&&signal!=-1) {
			this.setSignal2(par1);

			if (par1==1&&this.getSpeed()>0.0F) {
				++this.atsCount;
			}
		}
	}

	public void setSignal2(int par1) {
		if (par1==-1) {
			this.atsCount = 0;
		}

		this.setByteToDataWatcher(TrainStateType.State_Signal.id, (byte) par1);
	}

	/**
	 * 0:direction<br>
	 * 1:notch<br>
	 * 2:signal<br>
	 * 3:<br>
	 * 4:door<br>
	 * 5:light<br>
	 * 6:pantograph<br>
	 * 7:chunk_loader<br>
	 * 8:destination<br>
	 * 9:announcement<br>
	 * 10:direction<br>
	 */
	public byte getTrainStateData(int id) {
		return this.getByteFromDataWatcher(id);
	}

	public TrainState getTrainState(int id) {
		return TrainState.getState(id, this.getTrainStateData(id));
	}

	public void setTrainStateData(int id, byte data) {
		TrainStateType stateType = TrainState.getStateType(id);
		byte b = data<stateType.min ? stateType.max : (data>stateType.max ? stateType.min : data);
		if (this.formation!=null) {
			this.formation.setTrainStateData(id, b, this);
		}
	}

	public void setTrainStateData_NoSync(int id, byte data) {
		this.setByteToDataWatcher(id, data);
	}

	/**DataWatcher.updateWatchedObjectsFromList()から呼び出される*/
	@Override
	public void func_145781_i(int par1) {
		/*if(par1 == DW_FormationData && this.worldObj.isRemote)
		{
			this.createFormation();
		}*/
	}

	@SideOnly(Side.CLIENT)
	public float getRollsignAnimation() {
		return (float) this.rollsignAnimation/16.0F;
	}

	@SideOnly(Side.CLIENT)
	public void setRollsignAnimation(int par1) {
		this.rollsignV = par1*16;
	}

	@SideOnly(Side.CLIENT)
	@Override
	protected boolean useInteriorLight() {
		return this.getTrainStateData(TrainStateType.State_InteriorLight.id)>0;
	}

	@Override
	protected float getMoveDir() {
		int i = this.getTrainDirection();
		if (this.getBogie(i)!=null) {
			boolean b0 = this.getBogie(i).isFront();
			return ((i==0&&b0)||(i==1&&!b0)) ? 1.0F : -1.0F;
		}
		return 1.0F;
	}

	//ChunkLoader*************************************************************************************/

	private Ticket ticket;
	private final Set<ChunkCoordIntPair> loadedChunks = new HashSet();
	private int prevChunkCoordX;
	private int prevChunkCoordZ;

	/**ServerTickごとに呼び出し*/
	private void updateChunks() {
		if (this.isChunkLoaderEnable()) {
			this.forceChunkLoading();
		} else {
			this.releaseTicket();
		}

		this.prevChunkCoordX = this.chunkCoordX;
		this.prevChunkCoordZ = this.chunkCoordZ;
	}

	@Override
	public boolean isChunkLoaderEnable() {
		return this.getTrainStateData(TrainStateType.State_ChunkLoader.id)>0;
	}

	private void releaseTicket() {
		this.loadedChunks.clear();
		if (this.ticket!=null) {
			ForgeChunkManager.releaseTicket(this.ticket);
			this.ticket = null;
		}
	}

	private boolean requestTicket() {
		Ticket chunkTicket = RTMChunkManager.INSTANCE.getNewTicket(this.worldObj, Type.ENTITY);
		if (chunkTicket!=null) {
			int depth = this.getTrainStateData(TrainStateType.State_ChunkLoader.id);
			chunkTicket.getModData();
			chunkTicket.setChunkListDepth(depth);
			chunkTicket.bindEntity(this);
			this.setChunkTicket(chunkTicket);
			return true;
		}
		NGTLog.debug("[RTM] Failed to get ticket (Chunk Loader)");
		return false;
	}

	@Override
	public void setChunkTicket(Ticket par1) {
		if (this.ticket!=par1) {
			ForgeChunkManager.releaseTicket(this.ticket);
		}
		this.ticket = par1;
	}

	@Override
	public void forceChunkLoading() {
		this.forceChunkLoading(this.chunkCoordX, this.chunkCoordZ);
	}

	@Override
	public void forceChunkLoading(int x, int z) {
		if (this.worldObj.isRemote) {
			//this.setupChunks(x, z);
		} else {
			if (this.ticket==null) {
				if (!this.requestTicket()) {
					return;
				}
			}

			if (!(x==this.prevChunkCoordX&&z==this.prevChunkCoordZ)) {
				this.setupChunks(x, z);
			}

			for (ChunkCoordIntPair chunk : this.loadedChunks) {
				ForgeChunkManager.forceChunk(this.ticket, chunk);
				//ForgeChunkManager.reorderChunk(this.ticket, chunk);//並び替え
			}
			ChunkCoordIntPair myChunk = new ChunkCoordIntPair(x, z);//省くと機能しない
			ForgeChunkManager.forceChunk(this.ticket, myChunk);
		}
	}

	private void setupChunks(int xChunk, int zChunk) {
		int rad = this.getTrainStateData(TrainStateType.State_ChunkLoader.id);
		RTMChunkManager.INSTANCE.getChunksAround(this.loadedChunks, xChunk, zChunk, rad);
	}
}