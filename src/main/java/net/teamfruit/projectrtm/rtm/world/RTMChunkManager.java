package net.teamfruit.projectrtm.rtm.world;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.teamfruit.projectrtm.ngtlib.io.NGTLog;
import net.teamfruit.projectrtm.rtm.RTMCore;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RTMChunkManager implements LoadingCallback, OrderedLoadingCallback, PlayerOrderedLoadingCallback {
	public static final RTMChunkManager INSTANCE = new RTMChunkManager();

	private RTMChunkManager() {
	}

	@SubscribeEvent
	public void entityEnteredChunk(EnteringChunk event) {
		if (event.entity instanceof IChunkLoader) {
			IChunkLoader loader = (IChunkLoader) event.entity;
			if (loader.isChunkLoaderEnable()) {
				loader.forceChunkLoading(event.newChunkX, event.newChunkZ);
			}
		}
	}

	/**指定範囲のChunkCoordIntPairを新たに取得*/
	public void getChunksAround(Set<ChunkCoordIntPair> set, int xChunk, int zChunk, int radius) {
		set.clear();
		for (int xx = xChunk-radius; xx<=xChunk+radius; xx++) {
			for (int zz = zChunk-radius; zz<=zChunk+radius; zz++) {
				set.add(new ChunkCoordIntPair(xx, zz));
			}
		}
	}

	public Ticket getNewTicket(World world, Type type) {
		return ForgeChunkManager.requestTicket(RTMCore.instance, world, type);
	}

	@Override
	public List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount) {
		Set set = new HashSet();
		for (Ticket ticket : tickets) {
			if (ticket.getEntity() instanceof IChunkLoader) {
				set.add(ticket);
				continue;
			}

			NBTTagCompound nbt = ticket.getModData();

			if (nbt.hasKey("TYPE")) {
				set.add(ticket);
				continue;
			}
		}
		List ticketList = new LinkedList();
		ticketList.addAll(set);
		return ticketList;
	}

	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
		for (Ticket ticket : tickets) {
			IChunkLoader loader = null;
			if (ticket.getEntity() instanceof IChunkLoader) {
				Entity entity = ticket.getEntity();
				loader = (IChunkLoader) entity;
				NGTLog.debug("[RTM] Chunk loader found at "+entity.posX+", "+entity.posY+", "+entity.posZ);
			} else if (ticket.getModData().hasKey("TYPE")) {
				TileEntity tile = getTileEntity(world, ticket);
				if (tile instanceof IChunkLoader) {
					loader = (IChunkLoader) tile;
					NGTLog.debug("[RTM] Chunk loader found at "+tile.xCoord+", "+tile.yCoord+", "+tile.zCoord);
				}
			}

			if (loader!=null) {
				loader.setChunkTicket(ticket);
				loader.forceChunkLoading();
			}
		}
	}

	@Override
	public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world) {
		return LinkedListMultimap.create();
	}

	public static void writeData(Ticket ticket, TileEntity tile) {
		NBTTagCompound nbt = ticket.getModData();
		nbt.setString("TYPE", "TileEntity");
		nbt.setInteger("BlockX", tile.xCoord);
		nbt.setInteger("BlockY", tile.yCoord);
		nbt.setInteger("BlockZ", tile.zCoord);
	}

	public static TileEntity getTileEntity(World world, Ticket ticket) {
		NBTTagCompound nbt = ticket.getModData();
		int x = nbt.getInteger("BlockX");
		int y = nbt.getInteger("BlockY");
		int z = nbt.getInteger("BlockZ");
		return world.getTileEntity(x, y, z);
	}
}