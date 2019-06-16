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
package p455w0rd.wpt.client.gui;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.base.Joiner;

import appeng.api.AEApi;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.localization.GuiText;
import appeng.util.Platform;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.ae2wtlib.api.WTGuiObject;
import p455w0rd.ae2wtlib.api.client.gui.GuiWT;
import p455w0rd.wpt.container.ContainerCraftConfirm;
import p455w0rd.wpt.init.ModGuiHandler;
import p455w0rd.wpt.init.ModNetworking;
import p455w0rd.wpt.sync.packets.PacketSwitchGuis;
import p455w0rd.wpt.sync.packets.PacketValueConfig;

public class GuiCraftConfirm extends GuiWT {

	private final ContainerCraftConfirm ccc;

	private final int rows = 5;

	private final IItemList<IAEItemStack> storage = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
	private final IItemList<IAEItemStack> pending = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
	private final IItemList<IAEItemStack> missing = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();

	private final List<IAEItemStack> visual = new ArrayList<>();

	private int OriginalGui = 0;
	private GuiButton cancel;
	private GuiButton start;
	private GuiButton selectCPU;
	private int tooltip = -1;
	InventoryPlayer inventoryPlayer;

	public GuiCraftConfirm(final InventoryPlayer inventoryPlayer, final ITerminalHost te, final boolean isBauble, final int wctSlot) {
		super(new ContainerCraftConfirm(inventoryPlayer, te, isBauble, wctSlot));
		xSize = 238;
		ySize = 206;
		setScrollBar(WTApi.instance().createScrollbar());
		ccc = (ContainerCraftConfirm) inventorySlots;
		this.inventoryPlayer = inventoryPlayer;
		if (te instanceof WTGuiObject) {
			OriginalGui = ModGuiHandler.GUI_WPT;
		}

	}

	boolean isAutoStart() {
		return ((ContainerCraftConfirm) inventorySlots).isAutoStart();
	}

	@Override
	public void initGui() {
		super.initGui();

		start = new GuiButton(0, guiLeft + 162, guiTop + ySize - 25, 50, 20, GuiText.Start.getLocal());
		start.enabled = false;
		buttonList.add(start);

		selectCPU = new GuiButton(0, guiLeft + (219 - 180) / 2, guiTop + ySize - 68, 180, 20, GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic);
		selectCPU.enabled = false;
		buttonList.add(selectCPU);

		if (OriginalGui == 0) {
			cancel = new GuiButton(0, guiLeft + 6, guiTop + ySize - 25, 50, 20, GuiText.Cancel.getLocal());
		}

		buttonList.add(cancel);
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float btn) {
		updateCPUButtonText();

		start.enabled = !(ccc.hasNoCPU() || isSimulation());
		selectCPU.enabled = !isSimulation();

		final int gx = (width - xSize) / 2;
		final int gy = (height - ySize) / 2;

		tooltip = -1;

		final int offY = 23;
		int y = 0;
		int x = 0;
		for (int z = 0; z <= 4 * 5; z++) {
			final int minX = gx + 9 + x * 67;
			final int minY = gy + 22 + y * offY;

			if (minX < mouseX && minX + 67 > mouseX) {
				if (minY < mouseY && minY + offY - 2 > mouseY) {
					tooltip = z;
					break;
				}
			}

			x++;

			if (x > 2) {
				y++;
				x = 0;
			}
		}

		super.drawScreen(mouseX, mouseY, btn);
	}

	private void updateCPUButtonText() {
		String btnTextText = GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic.getLocal();
		if (ccc.getSelectedCpu() >= 0)// && status.selectedCpu < status.cpus.size() )
		{
			if (ccc.getName().length() > 0) {
				final String name = ccc.getName().substring(0, Math.min(20, ccc.getName().length()));
				btnTextText = GuiText.CraftingCPU.getLocal() + ": " + name;
			}
			else {
				btnTextText = GuiText.CraftingCPU.getLocal() + ": #" + ccc.getSelectedCpu();
			}
		}

		if (ccc.hasNoCPU()) {
			btnTextText = GuiText.NoCraftingCPUs.getLocal();
		}

		selectCPU.displayString = btnTextText;
	}

	private boolean isSimulation() {
		return ((ContainerCraftConfirm) inventorySlots).isSimulation();
	}

	@Override
	public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		final long bytesUsed = ccc.getUsedBytes();
		final long availBytes = ccc.getCpuAvailableBytes();
		final int availCoCPUs = ccc.getCpuCoProcessors();
		final String byteUsed = NumberFormat.getInstance().format(bytesUsed);

		final String Add = bytesUsed > 0 ? byteUsed + ' ' + GuiText.BytesUsed.getLocal() : GuiText.CalculatingWait.getLocal();
		fontRenderer.drawString(GuiText.CraftingPlan.getLocal() + " -- " + Add, 8, 7, 4210752);

		String dsp = null;

		if (isSimulation()) {
			dsp = GuiText.Simulation.getLocal();
		}
		else {
			dsp = availBytes > 0 ? GuiText.Bytes.getLocal() + ": " + availBytes + " : " + GuiText.CoProcessors.getLocal() + ": " + availCoCPUs : GuiText.Bytes.getLocal() + ": N/A : " + GuiText.CoProcessors.getLocal() + ": N/A";
		}

		final int offset = (219 - fontRenderer.getStringWidth(dsp)) / 2;
		fontRenderer.drawString(dsp, offset, 165, 4210752);

		final int sectionLength = 67;

		int x = 0;
		int y = 0;
		final int xo = 9;
		final int yo = 22;
		final int viewStart = getScrollBar().getCurrentScroll() * 3;
		final int viewEnd = viewStart + 3 * rows;

		String dspToolTip = "";
		final List<String> lineList = new LinkedList<>();
		int toolPosX = 0;
		int toolPosY = 0;

		final int offY = 23;

		for (int z = viewStart; z < Math.min(viewEnd, visual.size()); z++) {
			final IAEItemStack refStack = visual.get(z);// repo.getReferenceItem( z );
			if (refStack != null) {
				GL11.glPushMatrix();
				GL11.glScaled(0.5, 0.5, 0.5);

				final IAEItemStack stored = storage.findPrecise(refStack);
				final IAEItemStack pendingStack = pending.findPrecise(refStack);
				final IAEItemStack missingStack = missing.findPrecise(refStack);

				int lines = 0;

				if (stored != null && stored.getStackSize() > 0) {
					lines++;
				}
				if (pendingStack != null && pendingStack.getStackSize() > 0) {
					lines++;
				}
				if (pendingStack != null && pendingStack.getStackSize() > 0) {
					lines++;
				}

				final int negY = (lines - 1) * 5 / 2;
				int downY = 0;

				if (stored != null && stored.getStackSize() > 0) {
					String str = Long.toString(stored.getStackSize());
					if (stored.getStackSize() >= 10000) {
						str = Long.toString(stored.getStackSize() / 1000) + 'k';
					}
					if (stored.getStackSize() >= 10000000) {
						str = Long.toString(stored.getStackSize() / 1000000) + 'm';
					}

					str = GuiText.FromStorage.getLocal() + ": " + str;
					final int w = 4 + fontRenderer.getStringWidth(str);
					fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - w * 0.5) * 2), (y * offY + yo + 6 - negY + downY) * 2, 4210752);

					if (tooltip == z - viewStart) {
						lineList.add(GuiText.FromStorage.getLocal() + ": " + Long.toString(stored.getStackSize()));
					}

					downY += 5;
				}

				boolean red = false;
				if (missingStack != null && missingStack.getStackSize() > 0) {
					String str = Long.toString(missingStack.getStackSize());
					if (missingStack.getStackSize() >= 10000) {
						str = Long.toString(missingStack.getStackSize() / 1000) + 'k';
					}
					if (missingStack.getStackSize() >= 10000000) {
						str = Long.toString(missingStack.getStackSize() / 1000000) + 'm';
					}

					str = GuiText.Missing.getLocal() + ": " + str;
					final int w = 4 + fontRenderer.getStringWidth(str);
					fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - w * 0.5) * 2), (y * offY + yo + 6 - negY + downY) * 2, 4210752);

					if (tooltip == z - viewStart) {
						lineList.add(GuiText.Missing.getLocal() + ": " + Long.toString(missingStack.getStackSize()));
					}

					red = true;
					downY += 5;
				}

				if (pendingStack != null && pendingStack.getStackSize() > 0) {
					String str = Long.toString(pendingStack.getStackSize());
					if (pendingStack.getStackSize() >= 10000) {
						str = Long.toString(pendingStack.getStackSize() / 1000) + 'k';
					}
					if (pendingStack.getStackSize() >= 10000000) {
						str = Long.toString(pendingStack.getStackSize() / 1000000) + 'm';
					}

					str = GuiText.ToCraft.getLocal() + ": " + str;
					final int w = 4 + fontRenderer.getStringWidth(str);
					fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - w * 0.5) * 2), (y * offY + yo + 6 - negY + downY) * 2, 4210752);

					if (tooltip == z - viewStart) {
						lineList.add(GuiText.ToCraft.getLocal() + ": " + Long.toString(pendingStack.getStackSize()));
					}
				}

				GL11.glPopMatrix();
				final int posX = x * (1 + sectionLength) + xo + sectionLength - 19;
				final int posY = y * offY + yo;

				final ItemStack is = refStack.copy().createItemStack();

				if (tooltip == z - viewStart) {
					dspToolTip = Platform.getItemDisplayName(is);

					if (lineList.size() > 0) {
						dspToolTip = dspToolTip + '\n' + Joiner.on("\n").join(lineList);
					}

					toolPosX = x * (1 + sectionLength) + xo + sectionLength - 8;
					toolPosY = y * offY + yo;
				}

				drawItem(posX, posY, is);

				if (red) {
					final int startX = x * (1 + sectionLength) + xo;
					final int startY = posY - 4;
					drawRect(startX, startY, startX + sectionLength, startY + offY, 0x1AFF0000);
				}

				x++;

				if (x > 2) {
					y++;
					x = 0;
				}
			}
		}

		if (tooltip >= 0 && dspToolTip.length() > 0) {
			drawTooltip(toolPosX, toolPosY + 10, dspToolTip);
		}
	}

	@Override
	public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		this.setScrollBar();
		mc.getTextureManager().bindTexture(new ResourceLocation("appliedenergistics2", "textures/guis/craftingreport.png"));
		this.drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
	}

	private void setScrollBar() {
		final int size = visual.size();

		getScrollBar().setTop(19).setLeft(218).setHeight(114);
		getScrollBar().setRange(0, (size + 2) / 3 - rows, 1);
	}

	public void postUpdate(final List<IAEItemStack> list, final byte ref) {
		switch (ref) {
		case 0:
			for (final IAEItemStack l : list) {
				this.handleInput(storage, l);
			}
			break;

		case 1:
			for (final IAEItemStack l : list) {
				this.handleInput(pending, l);
			}
			break;

		case 2:
			for (final IAEItemStack l : list) {
				this.handleInput(missing, l);
			}
			break;
		}

		for (final IAEItemStack l : list) {
			final long amt = getTotal(l);

			if (amt <= 0) {
				deleteVisualStack(l);
			}
			else {
				final IAEItemStack is = findVisualStack(l);
				is.setStackSize(amt);
			}
		}

		this.setScrollBar();
	}

	private void handleInput(final IItemList<IAEItemStack> s, final IAEItemStack l) {
		IAEItemStack a = s.findPrecise(l);

		if (l.getStackSize() <= 0) {
			if (a != null) {
				a.reset();
			}
		}
		else {
			if (a == null) {
				s.add(l.copy());
				a = s.findPrecise(l);
			}

			if (a != null) {
				a.setStackSize(l.getStackSize());
			}
		}
	}

	private long getTotal(final IAEItemStack is) {
		final IAEItemStack a = storage.findPrecise(is);
		final IAEItemStack c = pending.findPrecise(is);
		final IAEItemStack m = missing.findPrecise(is);

		long total = 0;

		if (a != null) {
			total += a.getStackSize();
		}

		if (c != null) {
			total += c.getStackSize();
		}

		if (m != null) {
			total += m.getStackSize();
		}

		return total;
	}

	private void deleteVisualStack(final IAEItemStack l) {
		final Iterator<IAEItemStack> i = visual.iterator();
		while (i.hasNext()) {
			final IAEItemStack o = i.next();
			if (o.equals(l)) {
				i.remove();
				return;
			}
		}
	}

	private IAEItemStack findVisualStack(final IAEItemStack l) {
		for (final IAEItemStack o : visual) {
			if (o.equals(l)) {
				return o;
			}
		}

		final IAEItemStack stack = l.copy();
		visual.add(stack);
		return stack;
	}

	@Override
	protected void keyTyped(final char character, final int key) throws IOException {
		if (!checkHotbarKeys(key)) {
			if (key == 28) {
				actionPerformed(start);
			}
			super.keyTyped(character, key);
		}
	}

	@Override
	protected void actionPerformed(final GuiButton btn) throws IOException {
		super.actionPerformed(btn);
		final boolean backwards = Mouse.isButtonDown(1);
		if (btn == selectCPU) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("Terminal.Cpu", backwards ? "Prev" : "Next"));
		}
		if (btn == cancel) {
			ModNetworking.instance().sendToServer(new PacketSwitchGuis(OriginalGui));
		}

		if (btn == start) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("Terminal.Start", "Start"));
		}
	}
}
