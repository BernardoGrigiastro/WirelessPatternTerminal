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
import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.client.gui.*;
import p455w0rd.wpt.sync.WPTPacket;

public class PacketMEInventoryUpdate extends WPTPacket {

	private static final int UNCOMPRESSED_PACKET_BYTE_LIMIT = 16 * 1024 * 1024;
	private static final int OPERATION_BYTE_LIMIT = 2 * 1024;
	private static final int TEMP_BUFFER_SIZE = 1024;
	private static final int STREAM_MASK = 0xff;

	// input.
	@Nullable
	private final List<IAEItemStack> list;
	// output...
	private final byte ref;

	@Nullable
	private final ByteBuf data;
	@Nullable
	private final GZIPOutputStream compressFrame;

	private int writtenBytes = 0;
	private boolean empty = true;

	public PacketMEInventoryUpdate(final ByteBuf stream) throws IOException {
		data = null;
		compressFrame = null;
		list = new LinkedList<IAEItemStack>();
		ref = stream.readByte();
		final GZIPInputStream gzReader = new GZIPInputStream(new InputStream() {
			@Override
			public int read() throws IOException {
				if (stream.readableBytes() <= 0) {
					return -1;
				}
				return stream.readByte() & STREAM_MASK;
			}
		});
		final ByteBuf uncompressed = Unpooled.buffer(stream.readableBytes());
		final byte[] tmp = new byte[TEMP_BUFFER_SIZE];
		while (gzReader.available() != 0) {
			final int bytes = gzReader.read(tmp);
			if (bytes > 0) {
				uncompressed.writeBytes(tmp, 0, bytes);
			}
		}
		gzReader.close();
		while (uncompressed.readableBytes() > 0) {
			list.add(AEItemStack.fromPacket(uncompressed));
		}
		empty = list.isEmpty();
	}

	public PacketMEInventoryUpdate() throws IOException {
		this((byte) 0);
	}

	public PacketMEInventoryUpdate(final byte ref) throws IOException {
		this.ref = ref;
		data = Unpooled.buffer(OPERATION_BYTE_LIMIT);
		data.writeInt(getPacketID());
		data.writeByte(this.ref);
		compressFrame = new GZIPOutputStream(new OutputStream() {
			@Override
			public void write(final int value) throws IOException {
				data.writeByte(value);
			}
		});
		list = null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void clientPacketData(final INetworkInfo network, final WPTPacket packet, final EntityPlayer player) {
		final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
		if (gs instanceof GuiCraftConfirm) {
			((GuiCraftConfirm) gs).postUpdate(list, ref);
		}
		if (gs instanceof GuiCraftingCPU) {
			((GuiCraftingCPU) gs).postUpdate(list, ref);
		}
		if (gs instanceof GuiWPT) {
			((GuiWPT) gs).postUpdate(list);
		}
	}

	@Nullable
	@Override
	public FMLProxyPacket getProxy() {
		try {
			compressFrame.close();
			configureWrite(data);
			return super.getProxy();
		}
		catch (final IOException e) {
		}
		return null;
	}

	public void appendItem(final IAEItemStack is) throws IOException, BufferOverflowException {
		final ByteBuf tmp = Unpooled.buffer(OPERATION_BYTE_LIMIT);
		is.writeToPacket(tmp);
		compressFrame.flush();
		if (writtenBytes + tmp.readableBytes() > UNCOMPRESSED_PACKET_BYTE_LIMIT) {
			throw new BufferOverflowException();
		}
		else {
			writtenBytes += tmp.readableBytes();
			compressFrame.write(tmp.array(), 0, tmp.readableBytes());
			empty = false;
		}
	}

	public int getLength() {
		return data.readableBytes();
	}

	public boolean isEmpty() {
		return empty;
	}

}