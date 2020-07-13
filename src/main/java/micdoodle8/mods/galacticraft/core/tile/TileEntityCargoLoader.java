package micdoodle8.mods.galacticraft.core.tile;

import micdoodle8.mods.galacticraft.api.entity.ICargoEntity;
import micdoodle8.mods.galacticraft.api.entity.ICargoEntity.EnumCargoLoadingState;
import micdoodle8.mods.galacticraft.api.entity.ICargoEntity.RemovalResult;
import micdoodle8.mods.galacticraft.api.tile.ILandingPadAttachable;
import micdoodle8.mods.galacticraft.api.tile.ILockable;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.blocks.BlockCargoLoader;
import micdoodle8.mods.galacticraft.core.energy.item.ItemElectricBase;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseElectricBlockWithInventory;
import micdoodle8.mods.galacticraft.core.util.RecipeUtil;
import micdoodle8.mods.miccore.Annotations.NetworkedField;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;

public class TileEntityCargoLoader extends TileBaseElectricBlockWithInventory implements ISidedInventory, ILandingPadAttachable, ILockable
{
    public boolean outOfItems;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean targetFull;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean targetNoInventory;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean noTarget;
    @NetworkedField(targetSide = Side.CLIENT)
    public boolean locked;

    public ICargoEntity attachedFuelable;

    public TileEntityCargoLoader()
    {
        super("container.cargoloader.name");
        this.storage.setMaxExtract(45);
        this.inventory = NonNullList.withSize(15, ItemStack.EMPTY);
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.getWorld().isRemote)
        {
            if (this.ticks % 100 == 0)
            {
                this.checkForCargoEntity();
            }

            if (this.attachedFuelable != null)
            {
                this.noTarget = false;
                ItemStack stack = this.removeCargo(false).resultStack;

                if (!stack.isEmpty())
                {
                    this.outOfItems = false;

                    EnumCargoLoadingState state = this.attachedFuelable.addCargo(stack, false);

                    this.targetFull = state == EnumCargoLoadingState.FULL;
                    this.targetNoInventory = state == EnumCargoLoadingState.NOINVENTORY;
                    this.noTarget = state == EnumCargoLoadingState.NOTARGET;

                    if (this.ticks % (this.poweredByTierGC > 1 ? 9 : 15) == 0 && state == EnumCargoLoadingState.SUCCESS && !this.disabled && this.hasEnoughEnergyToRun)
                    {
                        this.attachedFuelable.addCargo(this.removeCargo(true).resultStack, true);
                    }
                }
                else
                {
                    this.outOfItems = true;
                }
            }
            else
            {
                this.noTarget = true;
            }
        }
    }

    public void checkForCargoEntity()
    {
        boolean foundFuelable = false;

        BlockVec3 thisVec = new BlockVec3(this);
        for (final EnumFacing dir : EnumFacing.VALUES)
        {
            final TileEntity pad = thisVec.getTileEntityOnSide(this.getWorld(), dir);

            if (pad != null && pad instanceof TileEntityMulti)
            {
                final TileEntity mainTile = ((TileEntityMulti) pad).getMainBlockTile();

                if (mainTile instanceof ICargoEntity)
                {
                    this.attachedFuelable = (ICargoEntity) mainTile;
                    foundFuelable = true;
                    break;
                }
            }
            else if (pad != null && pad instanceof ICargoEntity)
            {
                this.attachedFuelable = (ICargoEntity) pad;
                foundFuelable = true;
                break;
            }
        }

        if (!foundFuelable)
        {
            this.attachedFuelable = null;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.locked = nbt.getBoolean("locked");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setBoolean("locked", this.locked);
        return nbt;
    }

    @Override
    public boolean hasCustomName()
    {
        return true;
    }

    // ISidedInventory Implementation:

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        return side != this.getElectricInputDirection() ? new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 } : new int[] { };
    }

    @Override
    public boolean canInsertItem(int slotID, ItemStack itemstack, EnumFacing side)
    {
        if (side != this.getElectricInputDirection())
        {
            if (slotID == 0)
            {
                return ItemElectricBase.isElectricItem(itemstack.getItem());
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }


    @Override
    public boolean canExtractItem(int slotID, ItemStack itemstack, EnumFacing side)
    {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, ItemStack itemstack)
    {
        if (slotID == 0)
        {
            return ItemElectricBase.isElectricItem(itemstack.getItem());
        }
        else
        {
            return true;
        }
    }

    @Override
    public boolean shouldUseEnergy()
    {
        return !this.getDisabled(0);
    }

    public RemovalResult removeCargo(boolean doRemove)
    {
        for (int i = 1; i < this.getInventory().size(); i++)
        {
            ItemStack stackAt = this.getInventory().get(i);

            if (!stackAt.isEmpty())
            {
                ItemStack resultStack = stackAt.copy();
                resultStack.setCount(1);

                if (doRemove)
                {
                    stackAt.shrink(1);
                    if (stackAt.isEmpty())
                    {
                        this.getInventory().set(i, ItemStack.EMPTY);
                    }
                }

                if (doRemove)
                {
                    this.markDirty();
                }

                return new RemovalResult(EnumCargoLoadingState.SUCCESS, resultStack);
            }
        }

        return new RemovalResult(EnumCargoLoadingState.EMPTY, ItemStack.EMPTY);
    }

    //Used by Abandoned Base worldgen
    public EnumCargoLoadingState addCargo(ItemStack stack, boolean doAdd)
    {
        int count = 1;

        for (count = 1; count < this.getInventory().size(); count++)
        {
            ItemStack stackAt = this.getInventory().get(count);

            if (RecipeUtil.stacksMatch(stack, stackAt) && stackAt.getCount() < stackAt.getMaxStackSize())
            {
                if (stackAt.getCount() + stack.getCount() <= stackAt.getMaxStackSize())
                {
                    if (doAdd)
                    {
                        stackAt.grow(stack.getCount());
                        this.markDirty();
                    }

                    return EnumCargoLoadingState.SUCCESS;
                }
                else
                {
                    //Part of the stack can fill this slot but there will be some left over
                    int origSize = stackAt.getCount();
                    int surplus = origSize + stack.getCount() - stackAt.getMaxStackSize();

                    if (doAdd)
                    {
                        stackAt.setCount(stackAt.getMaxStackSize());
                        this.markDirty();
                    }

                    stack.setCount(surplus);
                    if (this.addCargo(stack, doAdd) == EnumCargoLoadingState.SUCCESS)
                    {
                        return EnumCargoLoadingState.SUCCESS;
                    }

                    stackAt.setCount(origSize);
                    return EnumCargoLoadingState.FULL;
                }
            }
        }

        int size = this.getInventory().size();
        for (count = 1; count < size; count++)
        {
            ItemStack stackAt = this.getInventory().get(count);

            if (stackAt.isEmpty())
            {
                if (doAdd)
                {
                    this.getInventory().set(count, stack);
                    this.markDirty();
                }

                return EnumCargoLoadingState.SUCCESS;
            }
        }

        return EnumCargoLoadingState.FULL;
    }

    @Override
    public boolean canAttachToLandingPad(IBlockAccess world, BlockPos pos)
    {
        return true;
    }

    @Override
    public EnumFacing getFront()
    {
        IBlockState state = this.world.getBlockState(getPos()); 
        if (state.getBlock() instanceof BlockCargoLoader)
        {
            return (state.getValue(BlockCargoLoader.FACING));
        }
        return EnumFacing.NORTH;
    }

    @Override
    public EnumFacing getElectricInputDirection()
    {
        return getFront().rotateY();
    }

    @Override
    public void clearLockedInventory()
    {
        for (int i = 1; i < 15; i++)
        {
            this.getInventory().set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean getLocked()
    {
        return this.locked;
    }
}
