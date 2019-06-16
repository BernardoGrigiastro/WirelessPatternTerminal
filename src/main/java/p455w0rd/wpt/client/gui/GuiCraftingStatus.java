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

import org.lwjgl.input.Mouse;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IDefinitions;
import appeng.api.storage.ITerminalHost;
import appeng.core.localization.GuiText;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import p455w0rd.ae2wtlib.api.*;
import p455w0rd.ae2wtlib.api.client.gui.widgets.GuiItemIconButton;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.container.ContainerCraftingStatus;
import p455w0rd.wpt.init.*;
import p455w0rd.wpt.sync.packets.PacketSwitchGuis;
import p455w0rd.wpt.sync.packets.PacketValueConfig;

public class GuiCraftingStatus extends GuiCraftingCPU {

	private final ContainerCraftingStatus status;
	private GuiButton selectCPU;

	private GuiItemIconButton originalGuiBtn;
	private int originalGui;
	private ItemStack myIcon = null;

	public GuiCraftingStatus(final InventoryPlayer inventoryPlayer, final ITerminalHost te, int wtSlot, boolean isWTBauble) {
		super(new ContainerCraftingStatus(inventoryPlayer, te, wtSlot, isWTBauble));
		status = (ContainerCraftingStatus) inventorySlots;
		final Object target = status.getTarget();
		final IDefinitions definitions = AEApi.instance().definitions();
		if (target instanceof WTGuiObject) {
			myIcon = definitions.items().wirelessTerminal().maybeStack(1).orElse(null);
			if (status.getWirelessTerminal() == WTApi.instance().getWTBySlot(inventoryPlayer.player, wtSlot)) {
				myIcon = new ItemStack(status.getWirelessTerminal().getItem());
				((ICustomWirelessTerminalItem) myIcon.getItem()).injectAEPower(myIcon, 6400001, Actionable.MODULATE);
			}
			else {
				ItemStack is = new ItemStack(ModItems.WPT);
				((IWirelessPatternTerminalItem) is.getItem()).injectAEPower(is, 6400001, Actionable.MODULATE);
				myIcon = is;
			}
			originalGui = ModGuiHandler.GUI_WPT;
		}
	}

	@Override
	protected void actionPerformed(final GuiButton btn) throws IOException {
		super.actionPerformed(btn);

		final boolean backwards = Mouse.isButtonDown(1);

		if (btn == selectCPU) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("Terminal.Cpu", backwards ? "Prev" : "Next"));
		}

		if (btn == originalGuiBtn) {
			ModNetworking.instance().sendToServer(new PacketSwitchGuis(originalGui));
		}
	}

	@Override
	public void initGui() {
		super.initGui();

		selectCPU = new GuiButton(0, guiLeft + 8, guiTop + ySize - 25, 150, 20, GuiText.CraftingCPU.getLocal() + ": " + GuiText.NoCraftingCPUs);
		// selectCPU.enabled = false;
		buttonList.add(selectCPU);

		if (myIcon != null) {
			buttonList.add(originalGuiBtn = new GuiItemIconButton(guiLeft + 213, guiTop - 4, myIcon, myIcon.getDisplayName(), itemRender));
			originalGuiBtn.setHideEdge(13);
		}
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float btn) {
		updateCPUButtonText();
		super.drawScreen(mouseX, mouseY, btn);
	}

	private void updateCPUButtonText() {
		String btnTextText = GuiText.NoCraftingJobs.getLocal();

		if (status.selectedCpu >= 0)// && status.selectedCpu < status.cpus.size() )
		{
			if (status.myName.length() > 0) {
				final String name = status.myName.substring(0, Math.min(20, status.myName.length()));
				btnTextText = GuiText.CPUs.getLocal() + ": " + name;
			}
			else {
				btnTextText = GuiText.CPUs.getLocal() + ": #" + status.selectedCpu;
			}
		}

		if (status.noCPU) {
			btnTextText = GuiText.NoCraftingJobs.getLocal();
		}

		selectCPU.displayString = btnTextText;
	}

	@Override
	protected String getGuiDisplayName(final String in) {
		return in; // the cup name is on the button
	}
}
