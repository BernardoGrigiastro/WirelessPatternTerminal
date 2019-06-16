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
package p455w0rd.wpt.container;

import javax.annotation.Nonnull;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.slot.SlotInaccessible;
import appeng.helpers.InventoryAction;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;
import p455w0rd.ae2wtlib.api.container.ContainerWT;
import p455w0rd.ae2wtlib.api.networking.security.WTIActionHost;
import p455w0rd.ae2wtlib.api.networking.security.WTPlayerSource;

public class ContainerCraftAmount extends ContainerWT {

	private final Slot craftingItem;
	private IAEItemStack itemToCreate;

	public ContainerCraftAmount(final InventoryPlayer ip, final ITerminalHost te, int wtSlot, boolean isWTBauble) {
		super(ip, te, wtSlot, isWTBauble);

		craftingItem = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);
		addSlotToContainer(getCraftingItem());
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		verifyPermissions(SecurityPermissions.CRAFT, false);
	}

	public IGrid getGrid() {
		final WTIActionHost h = ((WTIActionHost) getTarget());
		return h.getActionableNode(true).getGrid();
		//return obj2.getTargetGrid();
	}

	public World getWorld() {
		return getPlayer().getEntityWorld();
	}

	public IActionSource getActionSrc() {
		return new WTPlayerSource(getPlayerInv().player, (WTIActionHost) getTarget());
	}

	public Slot getCraftingItem() {
		return craftingItem;
	}

	public IAEItemStack getItemToCraft() {
		return itemToCreate;
	}

	public void setItemToCraft(@Nonnull final IAEItemStack itemToCreate) {
		this.itemToCreate = itemToCreate;
	}

	@Override
	public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
	}
}
