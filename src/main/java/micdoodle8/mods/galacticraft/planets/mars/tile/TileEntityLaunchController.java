package micdoodle8.mods.galacticraft.planets.mars.tile;

import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.api.tile.ILandingPadAttachable;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.BlockLandingPadFull;
import micdoodle8.mods.galacticraft.core.energy.item.ItemElectricBase;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseElectricBlockWithInventory;
import micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad;
// import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
// import micdoodle8.mods.galacticraft.core.util.GCLog;
import micdoodle8.mods.galacticraft.core.world.ChunkLoadingCallback;
import micdoodle8.mods.galacticraft.core.world.IChunkLoader;
import micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars;
import micdoodle8.mods.galacticraft.planets.mars.blocks.BlockMachineMars;
import micdoodle8.mods.galacticraft.planets.mars.network.PacketSimpleMars;
import micdoodle8.mods.galacticraft.planets.mars.network.PacketSimpleMars.EnumSimplePacketMars;
import micdoodle8.mods.miccore.Annotations.NetworkedField;
import net.minecraft.block.Block;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
// import java.util.HashMap;
import java.util.List;
// import java.util.Map;

public class TileEntityLaunchController extends TileBaseElectricBlockWithInventory implements IChunkLoader, ISidedInventory, ILandingPadAttachable
{
    public static final int WATTS_PER_TICK = 1;
    private ItemStack[] containingItems = new ItemStack[1];
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean launchPadRemovalDisabled = true;
    private Ticket chunkLoadTicket;
    private List<BlockPos> connectedPads = new ArrayList<BlockPos>();
    @NetworkedField(targetSide = Side.CLIENT)
    public int frequency = -1;
    @NetworkedField(targetSide = Side.CLIENT)
    public int destFrequency = -1;
    @NetworkedField(targetSide = Side.CLIENT)
    public String ownerName = "";
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean frequencyValid;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean destFrequencyValid;
    @NetworkedField(targetSide = Side.CLIENT)
    public int launchDropdownSelection;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean launchSchedulingEnabled;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean controlEnabled;
    public boolean hideTargetDestination = true;
    public boolean requiresClientUpdate;
    public Object attachedDock = null;
    private boolean frequencyCheckNeeded = false;
//    private static Map<Integer, Long> tickCounts = new HashMap();
//    private static Map<Integer, Integer> instanceCounts = new HashMap();

    public TileEntityLaunchController()
    {
        this.storage.setMaxExtract(10);
        this.noRedstoneControl = true;
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.worldObj.isRemote)
        {
//            if (ConfigManagerCore.enableDebug)
//            {
//            	int dim = this.worldObj);
//            	Long tickCount = tickCounts.get(dim);
//            	if (tickCount == null)
//            	{
//            		tickCount = 0L;
//            		tickCounts.put(dim, tickCount);
//            		instanceCounts.put(dim, 0);
//            	}
//            	int instanceCount = instanceCounts.get(dim);
//	        	if (this.worldObj.getTotalWorldTime() > tickCount)
//	            {
//	            	tickCount = this.worldObj.getTotalWorldTime();
//	            	if (tickCount % 20L == 0L) GCLog.debug("Dim " + dim + ": Number of Launch Controllers updating each tick: " + instanceCount);
//	            	instanceCount = 1;
//	            }
//	            else
//	            	instanceCount++;
//	        	tickCounts.put(dim, tickCount);
//	        	instanceCounts.put(dim, instanceCount);
//            }
        	
      		this.controlEnabled = this.launchSchedulingEnabled && this.hasEnoughEnergyToRun && !this.getDisabled(0);
        	
        	if (this.frequencyCheckNeeded)
            {
                this.checkDestFrequencyValid();
                this.frequencyCheckNeeded = false;
            }

            if (this.requiresClientUpdate)
            {
                // PacketDispatcher.sendPacketToAllPlayers(this.getPacket());
                // TODO
                this.requiresClientUpdate = false;
            }

            if (this.ticks % 40 == 0)
            {
                this.setFrequency(this.frequency);
                this.setDestinationFrequency(this.destFrequency);
            }

            if (this.ticks % 20 == 0)
            {
                if (this.chunkLoadTicket != null)
                {
                    for (int i = 0; i < this.connectedPads.size(); i++)
                    {
                        BlockPos coords = this.connectedPads.get(i);
                        Block block = this.worldObj.getBlockState(coords).getBlock();

                        if (block != GCBlocks.landingPadFull)
                        {
                            this.connectedPads.remove(i);
                            ForgeChunkManager.unforceChunk(this.chunkLoadTicket, new ChunkCoordIntPair(coords.getX() >> 4, coords.getZ() >> 4));
                        }
                    }
                }
            }
        }
        else
        {
            if (this.frequency == -1 && this.destFrequency == -1)
            {
                GalacticraftCore.packetPipeline.sendToServer(new PacketSimpleMars(EnumSimplePacketMars.S_UPDATE_ADVANCED_GUI, GCCoreUtil.getDimensionID(this.worldObj), new Object[] { 5, this.getPos(), 0 }));
            }
        }
    }

    @Override
    public String getOwnerName()
    {
        return this.ownerName;
    }

    @Override
    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    @Override
    public void invalidate()
    {
        super.invalidate();

        if (this.chunkLoadTicket != null)
        {
            ForgeChunkManager.releaseTicket(this.chunkLoadTicket);
        }
    }

    @Override
    public void onTicketLoaded(Ticket ticket, boolean placed)
    {
        if (!this.worldObj.isRemote && ConfigManagerMars.launchControllerChunkLoad)
        {
            if (ticket == null)
            {
                return;
            }

            if (this.chunkLoadTicket == null)
            {
                this.chunkLoadTicket = ticket;
            }

            NBTTagCompound nbt = this.chunkLoadTicket.getModData();
            nbt.setInteger("ChunkLoaderTileX", this.getPos().getX());
            nbt.setInteger("ChunkLoaderTileY", this.getPos().getY());
            nbt.setInteger("ChunkLoaderTileZ", this.getPos().getZ());

            for (int x = -2; x <= 2; x++)
            {
                for (int z = -2; z <= 2; z++)
                {
                    Block blockID = this.worldObj.getBlockState(this.getPos().add(x, 0, z)).getBlock();

                    if (blockID instanceof BlockLandingPadFull)
                    {
                        if (this.getPos().getX() + x >> 4 != this.getPos().getX() >> 4 || this.getPos().getZ() + z >> 4 != this.getPos().getZ() >> 4)
                        {
                            this.connectedPads.add(new BlockPos(this.getPos().getX() + x, this.getPos().getY(), this.getPos().getZ() + z));

                            if (placed)
                            {
                                ChunkLoadingCallback.forceChunk(this.chunkLoadTicket, this.worldObj, this.getPos().getX() + x, this.getPos().getY(), this.getPos().getZ() + z, this.getOwnerName());
                            }
                            else
                            {
                                ChunkLoadingCallback.addToList(this.worldObj, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getOwnerName());
                            }
                        }
                    }
                }
            }

            ChunkLoadingCallback.forceChunk(this.chunkLoadTicket, this.worldObj, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), this.getOwnerName());
        }
    }

    @Override
    public Ticket getTicket()
    {
        return this.chunkLoadTicket;
    }

    @Override
    public BlockPos getCoords()
    {
        return new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.containingItems = this.readStandardItemsFromNBT(nbt);

        this.ownerName = nbt.getString("OwnerName");
        this.launchDropdownSelection = nbt.getInteger("LaunchSelection");
        this.frequency = nbt.getInteger("ControllerFrequency");
        this.destFrequency = nbt.getInteger("TargetFrequency");
        this.frequencyCheckNeeded = true;
        this.launchPadRemovalDisabled = nbt.getBoolean("LaunchPadRemovalDisabled");
        this.launchSchedulingEnabled = nbt.getBoolean("LaunchPadSchedulingEnabled");
        this.hideTargetDestination = nbt.getBoolean("HideTargetDestination");
        this.requiresClientUpdate = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        this.writeStandardItemsToNBT(nbt);
        nbt.setString("OwnerName", this.ownerName);
        nbt.setInteger("LaunchSelection", this.launchDropdownSelection);
        nbt.setInteger("ControllerFrequency", this.frequency);
        nbt.setInteger("TargetFrequency", this.destFrequency);
        nbt.setBoolean("LaunchPadRemovalDisabled", this.launchPadRemovalDisabled);
        nbt.setBoolean("LaunchPadSchedulingEnabled", this.launchSchedulingEnabled);
        nbt.setBoolean("HideTargetDestination", this.hideTargetDestination);
    }

    @Override
    public ItemStack[] getContainingItems()
    {
        return this.containingItems;
    }

    @Override
    public String getName()
    {
        return GCCoreUtil.translate("container.launchcontroller.name");
    }

    @Override
    public boolean hasCustomName()
    {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, ItemStack itemStack)
    {
        return slotID == 0 && ItemElectricBase.isElectricItem(itemStack.getItem());
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        return new int[] { 0 };
    }

    @Override
    public boolean canInsertItem(int slotID, ItemStack par2ItemStack, EnumFacing par3)
    {
        return this.isItemValidForSlot(slotID, par2ItemStack);
    }

    @Override
    public boolean canExtractItem(int slotID, ItemStack par2ItemStack, EnumFacing par3)
    {
        return slotID == 0;
    }

    @Override
    public boolean shouldUseEnergy()
    {
        return !this.getDisabled(0);
    }

    @Override
    public void setDisabled(int index, boolean disabled)
    {
        if (this.disableCooldown == 0)
        {
            switch (index)
            {
            case 0:
                this.disabled = disabled;
                this.disableCooldown = 10;
                break;
            case 1:
                this.launchSchedulingEnabled = disabled;
                break;
            case 2:
                this.hideTargetDestination = disabled;
                this.disableCooldown = 10;
                break;
            }
        }
    }

    @Override
    public boolean getDisabled(int index)
    {
        switch (index)
        {
        case 0:
            return this.disabled;
        case 1:
            return this.launchSchedulingEnabled;
        case 2:
            return this.hideTargetDestination;
        }

        return true;
    }

    @Override
    public boolean canAttachToLandingPad(IBlockAccess world, BlockPos pos)
    {
        TileEntity tile = world.getTileEntity(pos);

        return tile instanceof TileEntityLandingPad;
    }

    public void setFrequency(int frequency)
    {
        this.frequency = frequency;

        if (this.frequency >= 0 && FMLCommonHandler.instance().getMinecraftServerInstance() != null)
        {
            this.frequencyValid = true;
            WorldServer[] servers = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers;

            worldLoop:
            for (int i = 0; i < servers.length; i++)
            {
                WorldServer world = servers[i];

                for (TileEntity tile2 : new ArrayList<TileEntity>(world.loadedTileEntityList))
                {
                    if (this != tile2)
                    {
                        tile2 = world.getTileEntity(tile2.getPos());
                        if (tile2 == null)
                        {
                            continue;
                        }

                        if (tile2 instanceof TileEntityLaunchController)
                        {
                            TileEntityLaunchController launchController2 = (TileEntityLaunchController) tile2;

                            if (launchController2.frequency == this.frequency)
                            {
                                this.frequencyValid = false;
                                break worldLoop;
                            }
                        }
                    }
                }
            }
        }
        else
        {
            this.frequencyValid = false;
        }
    }

    public void setDestinationFrequency(int frequency)
    {
        if (frequency != this.destFrequency)
        {
            this.destFrequency = frequency;
            this.checkDestFrequencyValid();
            this.updateRocketOnDockSettings();
        }
    }

    public void checkDestFrequencyValid()
    {
        if (!this.worldObj.isRemote && FMLCommonHandler.instance().getMinecraftServerInstance() != null)
        {
            this.destFrequencyValid = false;
            if (this.destFrequency >= 0)
            {
                WorldServer[] servers = FMLCommonHandler.instance().getMinecraftServerInstance().worldServers;
                for (int i = 0; i < servers.length; i++)
                {
                    WorldServer world = servers[i];

                    for (TileEntity tile2 : new ArrayList<TileEntity>(world.loadedTileEntityList))
                    {
                        if (this != tile2)
                        {
                            tile2 = world.getTileEntity(tile2.getPos());
                            if (tile2 == null)
                            {
                                continue;
                            }

                            if (tile2 instanceof TileEntityLaunchController)
                            {
                                TileEntityLaunchController launchController2 = (TileEntityLaunchController) tile2;

                                if (launchController2.frequency == this.destFrequency)
                                {
                                    this.destFrequencyValid = true;
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean validFrequency()
    {
        this.checkDestFrequencyValid();
        return !this.getDisabled(0) && this.hasEnoughEnergyToRun && this.frequencyValid && this.destFrequencyValid;
    }

    public void setLaunchDropdownSelection(int newvalue)
    {
        if (newvalue != this.launchDropdownSelection)
        {
            this.launchDropdownSelection = newvalue;
            this.checkDestFrequencyValid();
            this.updateRocketOnDockSettings();
        }
    }

    public void setLaunchSchedulingEnabled(boolean newvalue)
    {
        if (newvalue != this.launchSchedulingEnabled)
        {
            this.launchSchedulingEnabled = newvalue;
            this.checkDestFrequencyValid();
            this.updateRocketOnDockSettings();
        }
    }

    public void updateRocketOnDockSettings()
    {
        if (this.attachedDock instanceof TileEntityLandingPad)
        {
            TileEntityLandingPad pad = ((TileEntityLandingPad) this.attachedDock);
            IDockable rocket = pad.getDockedEntity();
            if (rocket instanceof EntityAutoRocket)
            {
                ((EntityAutoRocket) rocket).updateControllerSettings(pad);
            }
        }
    }

    @Override
    public IChatComponent getDisplayName()
    {
        return null;
    }

    public void setAttachedPad(IFuelDock pad)
    {
        this.attachedDock = pad;
    }

    @Override
    public EnumFacing getFront()
    {
        return this.worldObj.getBlockState(getPos()).getValue(BlockMachineMars.FACING);
    }

    @Override
    public EnumFacing getElectricInputDirection()
    {
        return getFront().rotateY();
    }
}
