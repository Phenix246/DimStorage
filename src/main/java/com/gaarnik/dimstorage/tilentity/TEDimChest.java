package com.gaarnik.dimstorage.tilentity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.gaarnik.dimstorage.DimStorage;
import com.gaarnik.dimstorage.DimStorageGUIHandler;
import com.gaarnik.dimstorage.network.DimStorageNetwork;
import com.gaarnik.dimstorage.storage.DimStorageManager;
import com.gaarnik.dimstorage.storage.chest.DimChestStorage;

import cpw.mods.fml.common.Optional.Interface;

@Interface(iface="dan200.computer.api.IPeripheral", modid="ComputerCraft")
public class TEDimChest extends TileEntity implements IInventory, ISidedInventory {
	// ****************************************************************
	private static final float MIN_MOVABLE_POSITION = 0f;
	private static final float MAX_MOVABLE_POSITION = 0.5f;

	private static final float OPENING_SPEED = 0.05f;

	// ****************************************************************
	private DimChestStorage storage;

	private String owner;
	private int freq;
	private boolean locked;

	private byte direction;

    private int openCount;
	private float movablePartState;
	
	private String customName;

	// ****************************************************************
	public TEDimChest() {
		this.init("public", 1);
	}
	
	public TEDimChest(EntityPlayer player) {
		this.init("public", 1);
		this.worldObj = player.worldObj;
		this.reloadStorage();
	}
	
	private void init(String owner, int freq) {
		this.owner = owner;
		this.freq = freq;
		this.locked = false;

		this.direction = 0;

		this.movablePartState = MIN_MOVABLE_POSITION;
	}

	// ****************************************************************
	@Override
	public void updateEntity() {
		super.updateEntity();

		if(this.openCount > 0) {
			if(this.movablePartState < MAX_MOVABLE_POSITION)
				this.movablePartState += OPENING_SPEED;
			
			if(this.movablePartState > MAX_MOVABLE_POSITION)
				this.movablePartState = MAX_MOVABLE_POSITION;
		}
		else {
			if(this.movablePartState > MIN_MOVABLE_POSITION)
				this.movablePartState -= OPENING_SPEED;
			
			if(this.movablePartState < MIN_MOVABLE_POSITION)
				this.movablePartState = MIN_MOVABLE_POSITION;
		}
	}

	@Override
	public void validate() {
		super.validate();

		if(!(worldObj instanceof WorldServer) == worldObj.isRemote)
			this.reloadStorage();
	}

	public void reloadStorage() {
		this.storage = (DimChestStorage) DimStorageManager.instance(worldObj.isRemote).getStorage(this.owner, this.freq, DimChestStorage.TYPE);
	}

	public void swapOwner() {
		if(!this.worldObj.isRemote)
			return;

		if(this.owner.equals("public"))
			this.owner = Minecraft.getMinecraft().thePlayer.getCommandSenderName();
		else
			this.owner = "public";

		this.reloadStorage();
	}

	public void changeFreq(int freq) {
		if(!this.worldObj.isRemote)
			return;

		if(freq >= 1 && freq <= 999) {
			this.freq = freq;
			this.reloadStorage();
		}
	}

	public void swapLocked() {
		this.locked = !this.locked;
		this.reloadStorage();
	}
	
	public boolean activate(World world, int x, int y, int z, EntityPlayer player) {
		if(this.isUseableByPlayer(player)) {
			if(this.storage.isClient())
				this.storage.empty();
			
			player.openGui(DimStorage.instance, DimStorageGUIHandler.GUI_DIMCHEST, world, x, y, z);
		}
		else {
			if(this.storage.isClient())
				player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("tileentity.dimchest.accessDenied")));
		}
		
		return true;
	}

	// ****************************************************************
	@Override
	public void openInventory() {
		if(this.storage.isClient())
            return;

        synchronized(this) {
            this.openCount++;
            
            if(this.openCount == 1)
                DimStorageNetwork.sendOpenStorageToPlayers(this);
        }
	}

	@Override
	public void closeInventory() {
		if(this.storage.isClient())
            return;

        synchronized(this) {
            this.openCount--;
            
            if(this.openCount == 0)
            	DimStorageNetwork.sendOpenStorageToPlayers(this);
        }
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		return this.storage.decrStackSize(i, j);
	}

	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.dimchest";
	}

	@Override
	public int getInventoryStackLimit() {
		return this.storage.getInventoryStackLimit();
	}

	@Override
	public int getSizeInventory() {
		return this.storage.getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return this.storage.getStackInSlot(slot);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		return this.storage.getStackInSlotOnClosing(slot);
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}
	
	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return this.storage.isItemValidForSlot(i, itemstack);
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		if(this.owner.equals("public") || this.owner.equals(entityplayer.getCommandSenderName()))
			return true;
		
		return !this.locked;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.storage.setInventorySlotContents(slot, stack);
	}

	// ****************************************************************
	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j) {
		return !this.locked;
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int j) {
		return !this.locked;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int var1) {
		int[] slots = new int[54];
		
		for(int i=0;i<54;i++)
			slots[i] = i;
		
		return slots;
	}
	
	// ****************************************************************
	// Not used until CC API is released for 1.7.2
	/*@Override
	public String[] getMethodNames() {
		return new String[] {
				"getOwner", "getFreq", "isLocked",
				"setOwner", "setPublic", "setFreq"
		};
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception {
		switch(method) {

		case 0: // getOwner
			return new Object[] {this.getOwner()};

		case 1: // getFreq
			return new Object[] {this.getFreq()};

		case 2: // isLocked
			return new Object[] {this.isLocked()};

		case 3: // setOwner
			if(this.isLocked())
				throw new Exception("DimChest is locked !");

			this.setOwner((String) arguments[0]);

			this.reloadStorage();
			this.onInventoryChanged();
			DimStorageNetwork.sendUpdateStorageToServer(this);
			
			return new Object[] { true };

		case 4: // setPublic
			if(this.isLocked())
				throw new Exception("DimChest is locked !");

			this.setOwner("public");

			this.reloadStorage();
			this.onInventoryChanged();
			DimStorageNetwork.sendUpdateStorageToServer(this);
			
			return new Object[] { true };

		case 5: // setFreq
			if(this.isLocked())
				throw new Exception("DimChest is locked !");

			Double freq = (Double) arguments[0];
			this.setFreq(freq.intValue());
			
			this.reloadStorage();
			this.onInventoryChanged();
			DimStorageNetwork.sendUpdateStorageToServer(this);
			
			return new Object[] { true };

		default:
			return null;

		}
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computer) {}

	@Override
	public void detach(IComputerAccess computer) {}

	@Override
	public String getType() {
		return this.storage.getType();
	}*/

	// ****************************************************************
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbtTag = new NBTTagCompound();
		this.writeToNBT(nbtTag);

		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbtTag);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		this.readFromNBT(packet.func_148857_g());
	}

	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		this.owner = tag.getString("owner");
		this.freq = tag.getInteger("freq");
		this.locked = tag.getBoolean("locked");

		this.direction = tag.getByte("direction");
		
		if(tag.hasKey("CustomName"))
			this.customName = tag.getString("CustomName");
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		tag.setString("owner", this.owner);
		tag.setInteger("freq", this.freq);
		tag.setBoolean("locked", this.locked);

		tag.setByte("direction", this.direction);
		if(this.hasCustomInventoryName())
			tag.setString("CustomName", this.customName);
	}

	// ****************************************************************

	// ****************************************************************
	public DimChestStorage getStorage() { return this.storage; }
	
	public String getOwner() { return this.owner; }
	public void setOwner(String owner) { this.owner = owner; }

	public int getFreq() { return this.freq; }
	public void setFreq(int freq) { this.freq = freq; }

	public boolean isLocked() { return this.locked; }
	public void setLocked(boolean locked) { this.locked = locked; }

	public byte getDirection() { return this.direction; }
	public void setDirection(byte direction) { this.direction = direction; }

	public int getOpenCount() { return this.openCount; }
	public void setOpenCount(int count) { this.openCount = count; }
	
	public float getMovablePartState() { return this.movablePartState; }
	
	public void setCustomGuiName(String name) {
		this.customName = name;
	}
	
	public String getCustomGuiName() { 
		if(! this.customName.isEmpty() && this.customName != null)
			return this.customName; 
		return "";
	}

}
