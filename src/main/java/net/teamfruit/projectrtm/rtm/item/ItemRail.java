package net.teamfruit.projectrtm.rtm.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.teamfruit.projectrtm.rtm.RTMItem;
import net.teamfruit.projectrtm.rtm.modelpack.ModelPackManager;
import net.teamfruit.projectrtm.rtm.modelpack.cfg.RailConfig;
import net.teamfruit.projectrtm.rtm.modelpack.cfg.RailConfig.BallastSet;
import net.teamfruit.projectrtm.rtm.modelpack.modelset.ModelSetBase;
import net.teamfruit.projectrtm.rtm.rail.util.RailProperty;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemRail extends ItemWithModel {
	public ItemRail() {
		super();
		this.setMaxStackSize(1);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		List<ModelSetBase> rails = (List<ModelSetBase>) ModelPackManager.INSTANCE.getModelList("ModelRail");
		for (ModelSetBase modelSet : rails) {
			RailConfig cfg = (RailConfig) modelSet.getConfig();
			if (cfg.defaultBallast==null) {
				continue;
			}

			for (BallastSet set : cfg.defaultBallast) {
				Block block = Block.getBlockFromName(set.blockName);
				int meta = set.blockMetadata;
				float h = set.height<=0.0F ? 0.0625F : set.height;
				if (block==null) {
					block = Blocks.air;
				}
				RailProperty prop = new RailProperty(cfg.getName(), block, meta, h);
				list.add(getRailItem(prop));
			}
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack itemStack) {
		String s = super.getItemStackDisplayName(itemStack);
		RailProperty prop = getProperty(itemStack);
		if (prop==null) {
			return s;
		}

		String localizedName = "";
		if (StatCollector.canTranslate(prop.unlocalizedName)) {
			localizedName = ", "+StatCollector.translateToLocal(prop.unlocalizedName);
		}
		return s+"("+prop.getModelSet().getConfig().getName()+localizedName+")";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean par4) {
		RailProperty prop = getProperty(itemStack);
		if (prop==null) {
			return;
		}
		//list.add(EnumChatFormatting.GRAY + "Model:" + prop.railModel);
		//String s = StatCollector.translateToLocal(prop.unlocalizedName);
		//list.add(EnumChatFormatting.GRAY + "Block:" + s);
		list.add(EnumChatFormatting.GRAY+"Height:"+prop.blockHeight);
	}

	public static RailProperty getDefaultProperty() {
		return new RailProperty("1067mm_Wood", Blocks.gravel, 0, 0.0625F);
	}

	public static ItemStack getRailItem(RailProperty prop) {
		ItemStack itemStack = new ItemStack(RTMItem.itemLargeRail, 1, 0);
		writePropToItem(prop, itemStack);
		return itemStack;
	}

	public static void writePropToItem(RailProperty prop, ItemStack itemStack) {
		NBTTagCompound nbtP = new NBTTagCompound();
		prop.writeToNBT(nbtP);
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("Property", nbtP);
		itemStack.setTagCompound(nbt);
	}

	public static RailProperty getProperty(ItemStack stack) {
		return stack.hasTagCompound() ? RailProperty.readFromNBT(stack.getTagCompound().getCompoundTag("Property")) : null;
	}

	@Override
	public String getModelName(ItemStack itemStack) {
		RailProperty prop = getProperty(itemStack);
		return (prop==null) ? this.getDefaultModelName(itemStack) : prop.railModel;
	}

	@Override
	public void setModelName(ItemStack itemStack, String name) {
		RailProperty prop = getProperty(itemStack);
		if (prop==null) {
			prop = getDefaultProperty();
		}
		prop = new RailProperty(name, prop.block, prop.blockMetadata, prop.blockHeight);
		writePropToItem(prop, itemStack);
	}

	@Override
	protected String getModelType(ItemStack itemStack) {
		return RailConfig.TYPE;
	}

	@Override
	protected String getDefaultModelName(ItemStack itemStack) {
		return "1067mm_Wood";
	}
}