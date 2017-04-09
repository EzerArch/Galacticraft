package micdoodle8.mods.galacticraft.planets.mars.tile;

import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.BlockMulti;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.core.tile.IMultiBlock;
import micdoodle8.mods.galacticraft.core.tile.TileEntityMulti;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.planets.mars.network.PacketSimpleMars;
import micdoodle8.mods.galacticraft.planets.mars.network.PacketSimpleMars.EnumSimplePacketMars;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityCryogenicChamber extends TileEntityMulti implements IMultiBlock
{
    public boolean isOccupied;

    public TileEntityCryogenicChamber()
    {
        super(null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return AxisAlignedBB.fromBounds(getPos().getX() - 1, getPos().getY(), getPos().getZ() - 1, getPos().getX() + 2, getPos().getY() + 3, getPos().getZ() + 2);
    }

    @Override
    public boolean onActivated(EntityPlayer entityPlayer)
    {
        if (this.worldObj.isRemote)
        {
            return false;
        }

        EnumStatus enumstatus = this.sleepInBedAt(entityPlayer, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());

        switch (enumstatus)
        {
        case OK:
            ((EntityPlayerMP) entityPlayer).playerNetServerHandler.setPlayerLocation(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, entityPlayer.rotationYaw, entityPlayer.rotationPitch);
            GalacticraftCore.packetPipeline.sendTo(new PacketSimpleMars(EnumSimplePacketMars.C_BEGIN_CRYOGENIC_SLEEP, GCCoreUtil.getDimensionID(entityPlayer.worldObj), new Object[] { this.getPos() }), (EntityPlayerMP) entityPlayer);
            return true;
        case NOT_POSSIBLE_NOW:
            GCPlayerStats stats = GCPlayerStats.get(entityPlayer);
            entityPlayer.addChatMessage(new ChatComponentText(GCCoreUtil.translateWithFormat("gui.cryogenic.chat.cant_use", stats.getCryogenicChamberCooldown() / 20)));
            return false;
        default:
            return false;
        }
    }

    public EnumStatus sleepInBedAt(EntityPlayer entityPlayer, int par1, int par2, int par3)
    {
        if (!this.worldObj.isRemote)
        {
            if (entityPlayer.isPlayerSleeping() || !entityPlayer.isEntityAlive())
            {
                return EnumStatus.OTHER_PROBLEM;
            }

            if (this.worldObj.getBiomeGenForCoords(new BlockPos(par1, par2, par3)) == BiomeGenBase.hell)
            {
                return EnumStatus.NOT_POSSIBLE_HERE;
            }

            GCPlayerStats stats = GCPlayerStats.get(entityPlayer);
            if (stats.getCryogenicChamberCooldown() > 0)
            {
                return EnumStatus.NOT_POSSIBLE_NOW;
            }
        }

        if (entityPlayer.isRiding())
        {
            entityPlayer.mountEntity((Entity) null);
        }

        entityPlayer.setPosition(this.getPos().getX() + 0.5F, this.getPos().getY() + 1.9F, this.getPos().getZ() + 0.5F);

        entityPlayer.sleeping = true;
        entityPlayer.sleepTimer = 0;
        entityPlayer.playerLocation = new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());
        entityPlayer.motionX = entityPlayer.motionZ = entityPlayer.motionY = 0.0D;

        if (!this.worldObj.isRemote)
        {
            this.worldObj.updateAllPlayersSleepingFlag();
        }

        return EnumStatus.OK;
    }

//    @Override
//    public boolean canUpdate()
//    {
//        return true;
//    }

    @Override
    public void update()
    {
        super.update();

        // TODO: Find a more efficient way to fix this
        //          Broken since 1.8 and this is an inefficient fix
        for (int y = 1; y < 3; y++)
        {
            final BlockPos vecToAdd = new BlockPos(getPos().getX(), getPos().getY() + y, getPos().getZ());

            TileEntity tile = this.worldObj.getTileEntity(vecToAdd);
            if (tile instanceof TileEntityMulti)
            {
                BlockPos pos = ((TileEntityMulti) tile).mainBlockPosition;
                if (pos == null || !pos.equals(getPos()))
                {
                    ((TileEntityMulti) tile).mainBlockPosition = getPos();
                }
            }
        }
    }

    @Override
    public void onCreate(World world, BlockPos placedPosition)
    {
        this.mainBlockPosition = placedPosition;
        this.markDirty();
        int buildHeight = this.worldObj.getHeight() - 1;

        for (int y = 0; y < 3; y++)
        {
            if (placedPosition.getY() + y > buildHeight)
            {
                return;
            }
            final BlockPos vecToAdd = new BlockPos(placedPosition.getX(), placedPosition.getY() + y, placedPosition.getZ());

            if (!vecToAdd.equals(placedPosition))
            {
                ((BlockMulti) GCBlocks.fakeBlock).makeFakeBlock(world, vecToAdd, placedPosition, 5);
            }
        }
    }

    @Override
    public void onDestroy(TileEntity callingBlock)
    {
        final BlockVec3 thisBlock = new BlockVec3(this);
        int fakeBlockCount = 0;

        for (int y = 0; y < 3; y++)
        {
            if (y == 0)
            {
                continue;
            }

            if (this.worldObj.getBlockState(new BlockPos(thisBlock.x, thisBlock.y + y, thisBlock.z)).getBlock() == GCBlocks.fakeBlock)
            {
                fakeBlockCount++;
            }
        }

        if (fakeBlockCount == 0)
        {
            return;
        }

        for (int y = 0; y < 3; y++)
        {
            if (this.worldObj.isRemote && this.worldObj.rand.nextDouble() < 0.1D)
            {
                BlockPos pos1 = new BlockPos(thisBlock.x, thisBlock.y + y, thisBlock.z);
                FMLClientHandler.instance().getClient().effectRenderer.addBlockDestroyEffects(pos1, worldObj.getBlockState(pos1));
            }
            this.worldObj.destroyBlock(new BlockPos(thisBlock.x, thisBlock.y + y, thisBlock.z), true);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.isOccupied = nbt.getBoolean("IsChamberOccupied");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setBoolean("IsChamberOccupied", this.isOccupied);
    }
}
