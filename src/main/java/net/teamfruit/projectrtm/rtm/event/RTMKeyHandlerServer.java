package net.teamfruit.projectrtm.rtm.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.teamfruit.projectrtm.rtm.RTMCore;
import net.teamfruit.projectrtm.rtm.entity.train.EntityTrainBase;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityArtillery;
import net.teamfruit.projectrtm.rtm.entity.vehicle.EntityVehicle;

public final class RTMKeyHandlerServer {
	public static final RTMKeyHandlerServer INSTANCE = new RTMKeyHandlerServer();

	private RTMKeyHandlerServer() {
	}

	public void onKeyDown(EntityPlayer player, byte keyCode, String sound) {
		if (keyCode==RTMCore.KEY_Forward) {
			this.setTrainNotch(player, 1);
		} else if (keyCode==RTMCore.KEY_Back) {
			this.setTrainNotch(player, -1);
		}
		if (keyCode==RTMCore.KEY_JUMP) {
			this.setVehicleState(player, 1);
		}
		if (keyCode==RTMCore.KEY_SNEAK) {
			this.setVehicleState(player, -1);
		} else if (keyCode==RTMCore.KEY_Horn) {
			this.playSound(player, sound, 10000.0F);
		} else if (keyCode==RTMCore.KEY_Chime) {
			this.playSound(player, sound, 1.0F);
		} else if (keyCode==RTMCore.KEY_ControlPanel) {
			player.openGui(RTMCore.instance, RTMCore.instance.guiIdTrainControlPanel, player.worldObj, player.ridingEntity.getEntityId(), 0, 0);
		} else if (keyCode==RTMCore.KEY_Fire) {
			if (player.isRiding()&&player.ridingEntity instanceof EntityArtillery) {
				((EntityArtillery) player.ridingEntity).onFireKeyDown(player);
			}
		} else if (keyCode==RTMCore.KEY_ATS) {
			this.setATS(player);
		}
	}

	private void setTrainNotch(EntityPlayer player, int notch) {
		EntityTrainBase train = this.getRidingTrain(player);
		if (train!=null) {
			train.addNotch(player, notch);
		}
	}

	private void setVehicleState(EntityPlayer player, int updown) {
		if (player.isRiding()&&player.ridingEntity instanceof EntityVehicle) {
			EntityVehicle vehicle = (EntityVehicle) player.ridingEntity;
			vehicle.setUpDown(updown);
		}
	}

	private void playSound(EntityPlayer player, String sound, float vol) {
		EntityTrainBase train = this.getRidingTrain(player);
		if (train!=null) {
			String[] sa = sound.split(":");
			if (sa.length==2) {
				RTMCore.proxy.playSound(train, new ResourceLocation(sa[0], sa[1]), vol, 1.0F);
			}
		}
	}

	private void setATS(EntityPlayer player) {
		EntityTrainBase train = this.getRidingTrain(player);
		if (train!=null) {
			int signal = train.getSignal();
			if (signal==1) {
				train.setSignal2(-1);
			} else if (signal==-1&&train.getNotch()==-8) {
				train.setSignal2(0);
			}
		}
	}

	private EntityTrainBase getRidingTrain(EntityPlayer player) {
		if (player.isRiding()&&player.ridingEntity instanceof EntityTrainBase) {
			return (EntityTrainBase) player.ridingEntity;
		}
		return null;
	}
}