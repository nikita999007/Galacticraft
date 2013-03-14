package micdoodle8.mods.galacticraft.core.oxygen;

import java.util.HashSet;

import micdoodle8.mods.galacticraft.core.blocks.GCCoreBlocks;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityBreathableAir;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityOxygenDistributor;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector3;

public class OxygenBubble 
{
	public HashSet<TileEntity> connectedDistributors = new HashSet<TileEntity>();
	
	public HashSet<TileEntity> connectedAir = new HashSet<TileEntity>();

	public HashSet<TileEntity> checkedBlocks = new HashSet<TileEntity>();
	
	public TileEntity pointer;
	
    public OxygenBubble(TileEntity pointer) 
    {
    	this.pointer = pointer;
	}

//	public static void emitGasFromDistributor(TileEntity sender)
//    {
//		OxygenBubble calculation = new OxygenBubble(sender);
//    	calculation.calculate();
//    }
	
	/**
	 * Recursive loop that iterates through connected tubes and adds connected acceptors to an ArrayList.
	 * @param tile - pointer tile entity
	 */
	public void loopThrough(TileEntity tile)
	{
		if (tile != null)
		{
			TileEntity[] tiles = this.getConnectedBubbleTiles(tile);
			
			for(TileEntity bubbleTile : tiles)
			{
				if (bubbleTile instanceof GCCoreTileEntityOxygenDistributor)
				{
					this.connectedDistributors.add(bubbleTile);
				}
				else if (bubbleTile instanceof GCCoreTileEntityBreathableAir)
				{
					this.connectedAir.add(bubbleTile);
				}
			}
			
			this.checkedBlocks.add(tile);
			
			HashSet<TileEntity> allObjects = (HashSet<TileEntity>) this.connectedAir.clone();
			allObjects.addAll(this.connectedDistributors);
			
			for(TileEntity object : allObjects)
			{
				if(object != null)
				{
					if(!this.checkedBlocks.contains(object))
					{
						this.loopThrough(object);
					}
				}
			}
		}
	}
	
	public void stopProducingOxygen()
	{
		if (!this.connectedAir.isEmpty())
		{
			for (TileEntity tile : this.connectedAir)
			{
				if (tile instanceof GCCoreTileEntityBreathableAir)
				{
					GCCoreTileEntityBreathableAir airBlock = (GCCoreTileEntityBreathableAir) tile;

					if (!this.isDistributorWithinRange(airBlock, true) && airBlock.worldObj.getBlockId(airBlock.xCoord, airBlock.yCoord, airBlock.zCoord) == GCCoreBlocks.breatheableAir.blockID)
					{
						airBlock.worldObj.func_94571_i(airBlock.xCoord, airBlock.yCoord, airBlock.zCoord);
						airBlock.invalidate();
					}
				}
			}
		}
	}

	public void calculate()
	{
		HashSet<TileEntity> allObjects = (HashSet<TileEntity>) this.connectedAir.clone();
		allObjects.addAll(this.connectedDistributors);
		
		for(TileEntity object : allObjects)
		{
			if(object != null)
			{	
				TileEntity tile = this.pointer.worldObj.getBlockTileEntity(object.xCoord, object.yCoord, object.zCoord);
				
				if (tile == null || tile != null && !tile.equals(object))
				{
					if (this.connectedAir.contains(object))
					{
						this.connectedAir.remove(object);
					}
					
					if (this.connectedDistributors.contains(object))
					{
						this.connectedDistributors.remove(object);
					}
				}
			}
		}
		
    	for(ForgeDirection orientation : ForgeDirection.values())
    	{
    		if(orientation != ForgeDirection.UNKNOWN)
    		{
    			Vector3 vec = new Vector3(this.pointer.xCoord, this.pointer.yCoord, this.pointer.zCoord);
    			
    			TileEntity acceptor = vec.clone().modifyPositionFromSide(orientation).getTileEntity(this.pointer.worldObj);
    			
    			if(acceptor instanceof GCCoreTileEntityOxygenDistributor || acceptor instanceof GCCoreTileEntityBreathableAir)
    			{
    				this.loopThrough(acceptor);
    			}
    		}
    	}
		
//		if (this.pointer != null)
//		FMLLog.info("" + this.pointer.worldObj.isRemote + " " + this.connectedDistributors.size() + " " + this.connectedAir.size());

		for (TileEntity tile : this.connectedDistributors)
		{
			if (tile instanceof GCCoreTileEntityOxygenDistributor)
			{
				GCCoreTileEntityOxygenDistributor distributor = (GCCoreTileEntityOxygenDistributor) tile;
				
				for (int x = distributor.xCoord - distributor.power - 1; x <= distributor.xCoord + distributor.power; x++)
				{
					for (int y = distributor.yCoord - distributor.power - 1; y <= distributor.yCoord + distributor.power; y++)
					{
						for (int z = distributor.zCoord - distributor.power - 1; z <= distributor.zCoord + distributor.power; z++)
						{
							if (distributor.getDistanceFrom(x + 0.5, y + 0.5, z + 0.5) < distributor.power)
							{
								if (distributor.worldObj.getBlockId(x, y, z) == 0 && !distributor.worldObj.isRemote)
								{
									distributor.worldObj.setBlockAndMetadataWithNotify(x, y, z, GCCoreBlocks.breatheableAir.blockID, 0, 3);
								}
							}
						}
					}
				}
			}
		}
		
		if (!this.connectedAir.isEmpty())
		{
			for (TileEntity tile : this.connectedAir)
			{
				if (tile instanceof GCCoreTileEntityBreathableAir)
				{
					GCCoreTileEntityBreathableAir airBlock = (GCCoreTileEntityBreathableAir) tile;
					
					airBlock.setBubble(this);
					
					if (!airBlock.worldObj.isRemote && !this.isDistributorWithinRange(airBlock, false) && airBlock.worldObj.getBlockId(airBlock.xCoord, airBlock.yCoord, airBlock.zCoord) == GCCoreBlocks.breatheableAir.blockID)
					{
						airBlock.worldObj.func_94571_i(airBlock.xCoord, airBlock.yCoord, airBlock.zCoord);
						airBlock.invalidate();
					}
				}
			}
		}
	}
	
	public boolean isDistributorWithinRange(GCCoreTileEntityBreathableAir airBlock, boolean extra)
	{
		if (!this.connectedDistributors.isEmpty())
		{
			for (TileEntity tile : this.connectedDistributors)
			{
				if (tile instanceof GCCoreTileEntityOxygenDistributor)
				{
					GCCoreTileEntityOxygenDistributor distributor = (GCCoreTileEntityOxygenDistributor) tile;
					
					if (distributor.getDistanceFrom(airBlock.xCoord + 0.5, airBlock.yCoord + 0.5, airBlock.zCoord + 0.5) <= distributor.power * (extra ? 1.5 : 1))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
    
    /**
     * Gets all the distributors around a tile entity.
     * @param tileEntity - center tile entity
     * @return array of IGasAcceptors
     */
    public static TileEntity[] getConnectedBubbleTiles(TileEntity tileEntity)
    {
    	TileEntity[] tiles = new TileEntity[] {null, null, null, null, null, null};
    	
    	for(ForgeDirection orientation : ForgeDirection.values())
    	{
    		if(orientation != ForgeDirection.UNKNOWN)
    		{
    			Vector3 vec = new Vector3(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
    			
    			TileEntity acceptor = vec.clone().modifyPositionFromSide(orientation).getTileEntity(tileEntity.worldObj);
    			
    			if(acceptor instanceof GCCoreTileEntityOxygenDistributor || acceptor instanceof GCCoreTileEntityBreathableAir)
    			{
    				tiles[orientation.ordinal()] = acceptor;
    			}
    		}
    	}
    	
    	return tiles;
    }
}