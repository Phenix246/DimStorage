package com.gaarnik.dimstorage;

import net.minecraftforge.common.MinecraftForge;

import com.gaarnik.dimstorage.common.BlockDimChest;
import com.gaarnik.dimstorage.common.ItemDimCore;
import com.gaarnik.dimstorage.common.RecipeDimChest;
import com.gaarnik.dimstorage.common.RecipeDimCore;
import com.gaarnik.dimstorage.common.TEDimChest;
import com.gaarnik.dimstorage.storage.DimStorageManager.DimStorageSaveHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

public abstract class CommonProxy {

	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new RecipeDimChest());
		MinecraftForge.EVENT_BUS.register(new DimStorageSaveHandler());
	}

	public void init(FMLInitializationEvent event) {
		// DimCore
		DimStorage.dimCore = new ItemDimCore().setUnlocalizedName("dimcore");
		GameRegistry.registerItem(DimStorage.dimCore, "item_dimcore");
		RecipeDimCore.init();
		
		// DimChest
		DimStorage.dimChest = new BlockDimChest().setBlockName("dimchest");
		GameRegistry.registerBlock(DimStorage.dimChest, "block_dimchest");
		RecipeDimChest.init();
		
		GameRegistry.registerTileEntity(TEDimChest.class, "TEDimChest");

		NetworkRegistry.INSTANCE.registerGuiHandler(DimStorage.instance, new DimStorageGUIHandler());
	}

}
