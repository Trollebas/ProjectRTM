package net.teamfruit.projectrtm.rtm.entity.ai;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.teamfruit.projectrtm.ngtlib.io.NGTLog;
import net.teamfruit.projectrtm.ngtlib.item.ItemUtil;
import net.teamfruit.projectrtm.rtm.entity.npc.EntityMotorman;
import net.teamfruit.projectrtm.rtm.entity.train.util.EnumNotch;

public class EntityAIDrivingWithDiagram extends EntityAIDrivingWithSignal {
	private List<TrainDiagram> diagram = new ArrayList<TrainDiagram>();

	public class TrainDiagram {
		public final int time;
		public final String command;
		public final int pointX;
		public final int pointY;
		public final int pointZ;

		public TrainDiagram(int par1Time, String par2Command, int par3X, int par4Y, int par5Z) {
			this.time = par1Time;
			this.command = par2Command;
			this.pointX = par3X;
			this.pointY = par4Y;
			this.pointZ = par5Z;
		}
	}

	public EntityAIDrivingWithDiagram(EntityMotorman par1) {
		super(par1);
		this.setMutexBits(1);
	}

	@Override
	public boolean shouldExecute() {
		if (this.motorman.hasDiagram()) {
			return super.shouldExecute();
		}
		return false;
	}

	@Override
	public void startExecuting() {
		super.startExecuting();
		this.diagram.clear();
		String[] sArray = ItemUtil.bookToStrings(this.motorman.getDiagram());
		for (String s : sArray) {
			String[] sArray2 = s.split(" ");
			int t = 0;
			String com = "";
			int pX = 0;
			int pY = 0;
			int pZ = 0;
			try {
				t = Integer.valueOf(sArray2[0]);
				com = sArray2[1];
				pX = Integer.valueOf(sArray2[2]);
				pY = Integer.valueOf(sArray2[3]);
				pZ = Integer.valueOf(sArray2[4]);
			} catch (NumberFormatException e) {
				this.diagram.clear();
				this.diagram.add(new TrainDiagram(0, "finish", 0, 0, 0));
				NGTLog.sendChatMessageToAll("Illegal format");
				return;
			} catch (ArrayIndexOutOfBoundsException e) {
				;
			}
			this.diagram.add(new TrainDiagram(t, com, pX, pY, pZ));
			NGTLog.debug("add diagram : "+s);
		}
		this.diagram.add(new TrainDiagram(0, "finish", 0, 0, 0));
	}

	@Override
	public boolean continueExecuting() {
		return super.continueExecuting();
	}

	@Override
	public void updateTask() {
		this.runTrain();
	}

	private void runTrain() {
		TrainDiagram td = this.diagram.get(0);

		if (td.command.equals("finish")||td.command.length()==0) {
			return;
		}

		int worldTime = (int) (this.motorman.worldObj.getWorldTime()%24000);//0~23999, 1=3.6sec
		int signalLevel = this.train.getSignal();

		if (td.command.equals("start")) {
			if (worldTime>=td.time-2&&worldTime<=td.time+2) {
				if (signalLevel>=0&&signalLevel<5) {
					this.train.setNotch(4);
					this.diagram.remove(0);
					NGTLog.debug("motorman start train");
				}
			}
			return;
		}

		float distance = this.getDistanceTrain(this.train, td.pointX, td.pointZ);
		float margin = (float) (td.time-worldTime);
		float prevSpeed = this.train.getSpeed();
		float ac1 = 0.0F;//目標加速度
		int notch = 0;
		int notchS = 0;
		if (signalLevel>0) {
			notchS = EnumNotch.getNotchFromSignal(signalLevel).id;
		}

		if (td.command.equals("set_speed")) {
			float speed = (float) td.pointX/72.0F;
			ac1 = (speed-prevSpeed)/margin;
			notch = EnumNotch.getSuitableNotchFromAcceleration(ac1).id;
		} else if (td.command.equals("pass")) {
			ac1 = 2*(distance-prevSpeed*margin)/(margin*margin);
			if (ac1>0) {
				float sp0 = prevSpeed+ac1*margin;//目標速度
				notch = EnumNotch.getSuitableNotchFromSpeed(sp0).id;
			} else if (ac1<0) {
				notch = EnumNotch.getSuitableNotchFromAcceleration(ac1).id;
			}
		} else if (td.command.equals("stop")) {
			if (distance<=360.0F) {
				ac1 = -prevSpeed/margin;
				if (0.5F*prevSpeed*margin<distance) {
					ac1 += 0.00075F;
				}
				notch = EnumNotch.getSuitableNotchFromAcceleration(ac1).id;
			} else {
				distance -= 360.0F;
				margin -= 600.0F;
				ac1 = 2*(distance-(prevSpeed*margin))/(margin*margin);
				if (ac1>=0.0F) {
					if (ac1<EnumNotch.accelerate_1.acceleration&&prevSpeed>=EnumNotch.accelerate_4.max_speed) {
						notch = EnumNotch.inertia.id;
					} else {
						notch = EnumNotch.accelerate_4.id;
					}
				} else if (ac1>EnumNotch.brake_1.acceleration) {
					notch = EnumNotch.inertia.id;
				} else {
					notch = EnumNotch.getSuitableNotchFromAcceleration(ac1).id;
				}
			}

			if (worldTime>=td.time-2&&worldTime<=td.time+2) {
				notch = -4;
				NGTLog.debug("motorman stop train");
			}
		}

		if (signalLevel>0) {
			notch = notch<=notchS ? notch : notchS;
		}
		this.train.setNotch(notch);
		if (worldTime>=td.time-2&&worldTime<=td.time+2) {
			this.diagram.remove(0);
		}
		return;
	}

	private float getDistanceTrain(Entity entity, double par1, double par2) {
		float f1 = (float) (entity.posX-par1);
		float f3 = (float) (entity.posZ-par2);
		return MathHelper.sqrt_float(f1*f1+f3*f3);
	}
}