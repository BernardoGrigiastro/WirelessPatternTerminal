/*
 * This file is part of Wireless Pattern Terminal. Copyright (c) 2017, p455w0rd
 * (aka TheRealp455w0rd), All rights reserved unless otherwise stated.
 *
 * Wireless Pattern Terminal is free software: you can redistribute it and/or
 * modify it under the terms of the MIT License.
 *
 * Wireless Pattern Terminal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the MIT License for
 * more details.
 *
 * You should have received a copy of the MIT License along with Wireless
 * Pattern Terminal. If not, see <https://opensource.org/licenses/MIT>.
 */
package p455w0rd.wpt.sync.packets;

import java.io.*;

import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.IMouseWheelItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.client.gui.GuiCraftingCPU;
import p455w0rd.wpt.container.*;
import p455w0rd.wpt.sync.WPTPacket;

public class PacketValueConfig extends WPTPacket {

	private final String Name;
	private final String Value;

	// automatic.
	public PacketValueConfig(final ByteBuf stream) throws IOException {
		final DataInputStream dis = new DataInputStream(getPacketByteArray(stream, stream.readerIndex(), stream.readableBytes()));
		Name = dis.readUTF();
		Value = dis.readUTF();
	}

	// api
	public PacketValueConfig(final String name, final String value) throws IOException {
		Name = name;
		Value = value;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(bos);
		dos.writeUTF(name);
		dos.writeUTF(value);
		data.writeBytes(bos.toByteArray());
		configureWrite(data);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		final Container c = player.openContainer;

		if (Name.equals("Item") && ((player.getHeldItem(EnumHand.MAIN_HAND) != null && player.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof IMouseWheelItem) || (player.getHeldItem(EnumHand.OFF_HAND) != null && player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof IMouseWheelItem))) {
			final EnumHand hand;
			if (player.getHeldItem(EnumHand.MAIN_HAND) != null && player.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof IMouseWheelItem) {
				hand = EnumHand.MAIN_HAND;
			}
			else if (player.getHeldItem(EnumHand.OFF_HAND) != null && player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof IMouseWheelItem) {
				hand = EnumHand.OFF_HAND;
			}
			else {
				return;
			}

			final ItemStack is = player.getHeldItem(hand);
			final IMouseWheelItem si = (IMouseWheelItem) is.getItem();
			si.onWheel(is, Value.equals("WheelUp"));
		}
		else if (Name.equals("Terminal.Cpu") && c instanceof ContainerCraftingStatus) {
			final ContainerCraftingStatus qk = (ContainerCraftingStatus) c;
			qk.cycleCpu(Value.equals("Next"));
		}
		else if (Name.equals("Terminal.Cpu") && c instanceof ContainerCraftConfirm) {
			final ContainerCraftConfirm qk = (ContainerCraftConfirm) c;
			qk.cycleCpu(Value.equals("Next"));
		}
		else if (Name.equals("Terminal.Start") && c instanceof ContainerCraftConfirm) {
			final ContainerCraftConfirm qk = (ContainerCraftConfirm) c;
			qk.startJob();
		}
		else if (Name.equals("TileCrafting.Cancel") && c instanceof ContainerCraftingCPU) {
			final ContainerCraftingCPU qk = (ContainerCraftingCPU) c;
			qk.cancelCrafting();
		}
		else if (Name.startsWith("PatternTerminal.") && c instanceof ContainerWPT) {
			final ContainerWPT cpt = (ContainerWPT) c;
			if (Name.equals("PatternTerminal.CraftMode")) {
				cpt.setCraftingRecipe(Value.equals("1"));
			}
			else if (Name.equals("PatternTerminal.Encode")) {
				cpt.encode();
			}
			else if (Name.equals("PatternTerminal.Clear")) {
				cpt.clear();
			}
			else if (Name.equals("PatternTerminal.Substitute")) {
				cpt.setSubstitution(Value.equals("1"));
			}
		}
		else if (c instanceof IConfigurableObject) {
			final IConfigManager cm = ((IConfigurableObject) c).getConfigManager();

			for (final Settings e : cm.getSettings()) {
				if (e.name().equals(Name)) {
					final Enum<?> def = cm.getSetting(e);

					try {
						cm.putSetting(e, Enum.valueOf(def.getClass(), Value));
					}
					catch (final IllegalArgumentException err) {
						// :P
					}

					break;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clientPacketData(final INetworkInfo network, final WPTPacket packet, final EntityPlayer player) {
		final Container c = player.openContainer;

		if (Name.equals("CustomName")) {
			if (c instanceof ContainerWPT) {
				((ContainerWPT) c).setCustomName(Value);
			}
		}
		else if (Name.startsWith("SyncDat.")) {
			((ContainerWPT) c).stringSync(Integer.parseInt(Name.substring(8)), Value);
		}
		else if (Name.equals("CraftingStatus") && Value.equals("Clear")) {
			final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if (gs instanceof GuiCraftingCPU) {
				((GuiCraftingCPU) gs).clearItems();
			}
		}
		else if (c instanceof IConfigurableObject) {
			final IConfigManager cm = ((IConfigurableObject) c).getConfigManager();

			for (final Settings e : cm.getSettings()) {
				if (e.name().equals(Name)) {
					final Enum<?> def = cm.getSetting(e);

					try {
						cm.putSetting(e, Enum.valueOf(def.getClass(), Value));
					}
					catch (final IllegalArgumentException err) {
						// :P
					}
					break;
				}
			}
		}
	}
}