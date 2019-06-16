/*
 * This file is part of Wireless Packet Terminal. Copyright (c) 2017, p455w0rd
 * (aka TheRealp455w0rd), All rights reserved unless otherwise stated.
 *
 * Wireless Packet Terminal is free software: you can redistribute it and/or
 * modify it under the terms of the MIT License.
 *
 * Wireless Packet Terminal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the MIT License for
 * more details.
 *
 * You should have received a copy of the MIT License along with Wireless
 * Pattern Terminal. If not, see <https://opensource.org/licenses/MIT>.
 */
package p455w0rd.wpt.sync.packets;

import java.util.concurrent.Future;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.ae2wtlib.api.networking.security.WTIActionHost;
import p455w0rd.wpt.container.ContainerCraftAmount;
import p455w0rd.wpt.container.ContainerCraftConfirm;
import p455w0rd.wpt.init.ModGuiHandler;
import p455w0rd.wpt.sync.WPTPacket;

public class PacketCraftRequest extends WPTPacket {

	private final long amount;
	private final boolean heldShift;

	public PacketCraftRequest(final ByteBuf stream) {
		heldShift = stream.readBoolean();
		amount = stream.readLong();
	}

	public PacketCraftRequest(final int craftAmt, final boolean shift) {
		amount = craftAmt;
		heldShift = shift;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		data.writeBoolean(shift);
		data.writeLong(amount);

		configureWrite(data);
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		if (player.openContainer instanceof ContainerCraftAmount) {
			final ContainerCraftAmount cca = (ContainerCraftAmount) player.openContainer;
			final Object target = cca.getTarget();//.getTarget();
			if (target instanceof WTIActionHost) {
				final WTIActionHost ah = (WTIActionHost) target;
				final IGridNode gn = ah.getActionableNode(true);

				if (gn == null) {
					return;
				}

				final IGrid g = gn.getGrid();
				if (g == null || cca.getItemToCraft() == null) {
					return;
				}

				cca.getItemToCraft().setStackSize(amount);

				Future<ICraftingJob> futureJob = null;
				try {
					final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
					futureJob = cg.beginCraftingJob(cca.getWorld(), cca.getGrid(), cca.getActionSrc(), cca.getItemToCraft(), null);

					int x = (int) player.posX;
					int y = (int) player.posY;
					int z = (int) player.posZ;

					ModGuiHandler.open(ModGuiHandler.GUI_CRAFT_CONFIRM, player, player.getEntityWorld(), new BlockPos(x, y, z), cca.isWTBauble(), cca.getWTSlot());

					if (player.openContainer instanceof ContainerCraftConfirm) {
						final ContainerCraftConfirm ccc = (ContainerCraftConfirm) player.openContainer;
						ccc.setAutoStart(heldShift);
						ccc.setJob(futureJob);
						cca.detectAndSendChanges();
					}
				}
				catch (final Throwable e) {
					if (futureJob != null) {
						futureJob.cancel(true);
					}
				}
			}
		}
	}
}
