package net.teamfruit.projectrtm.rtm.entity.train;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.teamfruit.projectrtm.rtm.RTMCore;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityArtillery;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityCargo;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityCargoWithModel;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityContainer;
import net.teamfruit.projectrtm.rtm.entity.train.parts.EntityTie;
import net.teamfruit.projectrtm.rtm.item.ItemCargo;

public class EntityFreightCar extends EntityTrainBase implements IInventory {
	private static final float[][] CARGO_POS = new float[][] {
			{ 0.0F, 0.0F, 8.0F },
			{ 0.0F, 0.0F, 4.0F },
			{ 0.0F, 0.0F, 0.0F },
			{ 0.0F, 0.0F, -4.0F },
			{ 0.0F, 0.0F, -8.0F } };

	private ItemStack[] cargoSlots = new ItemStack[5];
	public EntityCargo[] cargoEntities = new EntityCargo[5];

	public EntityFreightCar(World world) {
		super(world);
	}

	public EntityFreightCar(World world, String s) {
		super(world, s);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);

		NBTTagList list = nbt.getTagList("Items", 10);
		for (int i = 0; i<list.tagCount(); ++i) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			if (nbt1.hasKey("Slot", 1)) {
				byte b0 = nbt1.getByte("Slot");
				if (b0>=0&&b0<this.cargoSlots.length) {
					this.cargoSlots[b0] = ItemStack.loadItemStackFromNBT(nbt1);
				}
			}
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i<this.cargoSlots.length; ++i) {
			if (this.cargoSlots[i]!=null&&this.cargoEntities[i]!=null) {
				this.cargoEntities[i].writeCargoToItem();
				NBTTagCompound nbt0 = new NBTTagCompound();
				nbt0.setByte("Slot", (byte) i);
				this.cargoSlots[i].writeToNBT(nbt0);
				list.appendTag(nbt0);
			}
		}
		nbt.setTag("Items", list);
	}

	@Override
	public void setDead() {
		super.setDead();

		for (int i = 0; i<this.cargoEntities.length; ++i) {
			if (this.cargoEntities[i]!=null) {
				this.cargoEntities[i].setDead();
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (!this.worldObj.isRemote) {
			for (int i = 0; i<this.cargoSlots.length; ++i) {
				if (this.hasCargo(i)) {
					if (this.cargoEntities[i]==null) {
						EntityCargo entity = this.createCargoEntity((byte) i);
						entity.updatePartPos(this);
						this.worldObj.spawnEntityInWorld(entity);
						this.cargoEntities[i] = entity;
					}
				} else {
					if (this.cargoEntities[i]!=null) {
						this.cargoEntities[i].setDead();
						this.cargoEntities[i] = (EntityContainer) null;
					}
				}
			}
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource par1, float par2) {
		if (!this.worldObj.isRemote) {
			for (int i = 0; i<this.cargoSlots.length; ++i) {
				if (this.cargoSlots[i]!=null) {
					this.entityDropItem(this.cargoSlots[i], 1.0F);
				}
			}
		}
		return super.attackEntityFrom(par1, par2);
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (super.interactFirst(player)) {
			return true;
		} else {
			if (!this.worldObj.isRemote) {
				player.openGui(RTMCore.instance, RTMCore.instance.guiIdFreightCar, player.worldObj, this.getEntityId(), 0, 0);
			}
			return true;
		}
	}

	private boolean hasCargo(int par1) {
		ItemStack itemstack = this.cargoSlots[par1];
		return itemstack!=null&&itemstack.getItem() instanceof ItemCargo;
	}

	private EntityCargo createCargoEntity(byte slot) {
		EntityCargo cargo = null;
		int damage = this.cargoSlots[slot].getItemDamage();
		switch (damage) {
			case 0:
				cargo = new EntityContainer(this.worldObj, this, this.cargoSlots[slot], CARGO_POS[slot], slot);
				break;
			case 1:
				cargo = new EntityArtillery(this.worldObj, this, this.cargoSlots[slot], CARGO_POS[slot], slot);
				break;
			case 2:
				cargo = new EntityTie(this.worldObj, this, this.cargoSlots[slot], CARGO_POS[slot], slot);
				break;
			default:
				cargo = new EntityContainer(this.worldObj, this, this.cargoSlots[slot], CARGO_POS[slot], slot);
				break;
		}

		cargo.readCargoFromItem();

		if (damage==0||damage==1) {
			EntityCargoWithModel entity = (EntityCargoWithModel) cargo;
			if (entity.getModelName().length()==0) {
				entity.setModelName(entity.getDefaultName());
			}
		}

		return cargo;
	}

	@Override
	public int getSizeInventory() {
		return this.cargoSlots.length;
	}

	@Override
	public ItemStack getStackInSlot(int par1) {
		return this.cargoSlots[par1];
	}

	@Override
	public ItemStack decrStackSize(int par1, int par2) {
		if (this.cargoSlots[par1]!=null) {
			ItemStack itemstack;
			if (this.cargoSlots[par1].stackSize<=par2) {
				itemstack = this.cargoSlots[par1];
				this.cargoSlots[par1] = null;
				return itemstack;
			} else {
				itemstack = this.cargoSlots[par1].splitStack(par2);
				if (this.cargoSlots[par1].stackSize==0) {
					this.cargoSlots[par1] = null;
				}
				return itemstack;
			}
		} else {
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int par1) {
		if (this.cargoSlots[par1]!=null) {
			ItemStack itemstack = this.cargoSlots[par1];
			this.cargoSlots[par1] = null;
			return itemstack;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int par1, ItemStack itemStack) {
		this.cargoSlots[par1] = itemStack;
		if (itemStack!=null&&itemStack.stackSize>this.getInventoryStackLimit()) {
			itemStack.stackSize = this.getInventoryStackLimit();
		}
	}

	@Override
	public String getInventoryName() {
		return "Inventory_FreightCar";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) {
		return this.getDistanceSqToEntity(var1)<64.0D;
	}

	@Override
	public void openInventory() {
		;
	}

	@Override
	public void closeInventory() {
		;
	}

	@Override
	public boolean isItemValidForSlot(int var1, ItemStack var2) {
		return true;
	}
}