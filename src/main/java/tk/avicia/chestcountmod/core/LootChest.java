package tk.avicia.chestcountmod.core;

import net.minecraft.util.math.BlockPos;

public class LootChest{

    private double x, y ,z;
    private int minLevel, maxLevel, tier;

    public LootChest(double x, double y, double z, int tier){
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        this.minLevel = this.maxLevel = -1;
        this.tier = tier;
    }

    public LootChest(double x, double y, double z, int minLevel, int maxLevel, int tier){
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.tier = tier;
    }

    public LootChest(BlockPos pos, int minLevel, int maxLevel, int tier){
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.tier = tier;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getTier() {
        return tier;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public void setMinAndMax(int min, int max){
        this.minLevel = min;
        this.maxLevel = max;
    }
}
