package tk.avicia.chestcountmod;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import tk.avicia.chestcountmod.configs.ConfigsGui;
import tk.avicia.chestcountmod.configs.locations.LocationsGui;
import tk.avicia.chestcountmod.configs.locations.MultipleElements;
import tk.avicia.chestcountmod.core.LootChest;
import tk.avicia.chestcountmod.core.Mythic;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EventHandlerClass {
    private static final TextFormatting[] colors = {TextFormatting.DARK_GRAY, TextFormatting.BLACK, TextFormatting.RED,
            TextFormatting.LIGHT_PURPLE, TextFormatting.DARK_BLUE, TextFormatting.DARK_GREEN, TextFormatting.DARK_RED,
            TextFormatting.DARK_PURPLE, TextFormatting.BLUE};

    private boolean hasMythicBeenRegistered = false, searchedChest = false;
    private int chestsDry = 0;
    private BlockPos chestLocation = null;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void openChest(PlayerInteractEvent.RightClickBlock e) {
        if (e.isCanceled()) return;
        BlockPos pos = e.getPos();
        IBlockState state = e.getEntityPlayer().world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockContainer)) return;
        chestLocation = pos.toImmutable();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent event) {
        if (ChestCountMod.getMC().player == null || event.getGui() == null) {
            return;
        }
        if (!ChestCountMod.getChestCountData().hasBeenInitialized()) {
            // Keeps trying to get the chestcount data from the api until it gets it
            Thread thread = new Thread(() -> {
                ChestCountMod.PLAYER_UUID = ChestCountMod.getMC().player.getGameProfile().getId().toString();
                ChestCountMod.getChestCountData().updateChestCount();
                ChestCountMod.getMythicData().updateDry();
            });
            thread.start();
        }
        Container openContainer = ChestCountMod.getMC().player.openContainer;
        if (openContainer instanceof ContainerChest) {
            InventoryBasic lowerInventory = (InventoryBasic) ((ContainerChest) openContainer).getLowerChestInventory();
            String containerName = lowerInventory.getName();
            // It is a lootchest and it doesn't already have a new name
            if (containerName.contains("Loot Chest") && !containerName.contains("#")) {
                // All this code runs once when the loot chest has been opened
                ChestCountMod.getChestCountData().addToSessionChestCount();
                ChestCountMod.getMythicData().addToDry();
                this.chestsDry = ChestCountMod.getMythicData().getChestsDry();
                // Defaults to not having a mythic in the chest
                this.hasMythicBeenRegistered = false;
                this.searchedChest = false;
                lowerInventory.setCustomName((ChestCountMod.CONFIG.getConfigBoolean("enableColoredName") ? ChestCountMod.getRandom(colors) : "") + containerName + " #" +
                        ChestCountMod.getChestCountData().getSessionChestCount()
                        + " Tot: " + ChestCountMod.getChestCountData().getTotalChestCount());
            }
        }
    }


    @SubscribeEvent
    public void guiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (ChestCountMod.getMC().player == null || event.getGui() == null) {
            return;
        }
        Container openContainer = ChestCountMod.getMC().player.openContainer;
        if (openContainer instanceof ContainerChest) {
            InventoryBasic lowerInventory = (InventoryBasic) ((ContainerChest) openContainer).getLowerChestInventory();
            String containerName = lowerInventory.getName();
            if (containerName.contains("Loot Chest") && !this.searchedChest) {
                int tier = getLootchestTier(containerName.split(" ")[2]);
                GlStateManager.pushMatrix();
                GlStateManager.translate(1f, 1f, 1f);
                int screenWidth = event.getGui().width;
                int screenHeight = event.getGui().height;
                ChestCountMod.drawString(chestsDry + " Dry", screenWidth / 2 - 20, screenHeight / 2 - 11, new Color(64, 64, 64));
                GlStateManager.popMatrix();
                int itemCount = 0;
                for (int i = 0; i < 27; i++) {
                    if (!lowerInventory.getStackInSlot(i).getDisplayName().equals("Air")) {
                        itemCount++;
                    }
                }
                if (itemCount == 0) {
                    return; // If there are no items on the chest (or the items haven't loaded) just try again basically
                }
                boolean isMythicInChest = false;

                LootChest current = ChestCountMod.getChestManager().getChestAt(chestLocation.getX(), chestLocation.getY(), chestLocation.getZ());
                if(current == null) current = ChestCountMod.getChestManager().addChestAt(chestLocation.getX(), chestLocation.getY(), chestLocation.getZ(), tier);
                int minLevel = current.getMinLevel();
                int maxLevel = current.getMaxLevel();
                for (int i = 0; i < 27; i++) {
                    ItemStack itemStack = lowerInventory.getStackInSlot(i);
                    if (!itemStack.getDisplayName().equals("Air")) {
                        List<String> lore = itemStack.getTooltip(ChestCountMod.getMC().player, ITooltipFlag.TooltipFlags.ADVANCED);
                        // Find whether the lore includes Tier: Mythic
                        Optional<String> mythicTier = lore.stream()
                                .filter(line -> Objects.requireNonNull(TextFormatting.getTextWithoutFormattingCodes(line)).contains("Tier: Mythic")).findFirst();
                        Optional<String> itemLevel = lore.stream()
                                .filter(line -> line.contains("Lv. Range")).findFirst();
                        Optional<String> combatLevel = lore.stream().filter(line -> line.contains("Combat Lv.")).findFirst();
                        try{
                            if (itemLevel.isPresent()) {
                                if (mythicTier.isPresent()) {
                                    if (!hasMythicBeenRegistered) { // Makes sure you don't register the same mythic twice

                                        // A new mythic has been found!
                                        String mythicString = itemStack.getDisplayName() + " " + itemLevel.get();
                                        if (ChestCountMod.CONFIG.getConfigBoolean("displayMythicOnFind")) {
                                            if (ChestCountMod.CONFIG.getConfigBoolean("displayMythicTypeOnFind")) {
                                                ChestCountMod.getMC().player.sendMessage(new TextComponentString(mythicString + " : " + TextFormatting.RED + ChestCountMod.getMythicData().getChestsDry() + " dry"));
                                            } else {
                                                ChestCountMod.getMC().player.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "Mythic found : " + TextFormatting.RED + ChestCountMod.getMythicData().getChestsDry() + " dry"));
                                            }
                                        }
                                        EntityPlayerSP player = ChestCountMod.getMC().player;
                                        ChestCountMod.getMythicData().addMythic(ChestCountMod.getChestCountData().getTotalChestCount(), TextFormatting.getTextWithoutFormattingCodes(mythicString), this.chestsDry, chestLocation.getX(), chestLocation.getY(), chestLocation.getZ());

                                    }
                                } else {
                                    // not a mythic, but a box
                                    String levelRange = TextFormatting.getTextWithoutFormattingCodes(itemLevel.get().split(" ")[3]);
                                    int min = Integer.parseInt(Objects.requireNonNull(levelRange).split("-")[0]);
                                    int max = Integer.parseInt(Objects.requireNonNull(levelRange).split("-")[1]);
                                    int avg = Math.round((float)(max + min) / 2);

                                    if (avg < minLevel || minLevel == -1) minLevel = avg;
                                    if (avg > maxLevel || maxLevel == -1) maxLevel = avg;
                                }
                                isMythicInChest = true;
                            }else if(combatLevel.isPresent()){
                                if(itemStack.getDisplayName().contains("Potion") && !itemStack.getDisplayName().contains("Healing")) continue;
                                int level = Integer.parseInt(combatLevel.get().split(": ")[1]);
                                if(level < minLevel || minLevel == -1) minLevel = level;
                                if(level > maxLevel || maxLevel == -1) maxLevel = level;
                            }
                        } catch (Exception e) {
                            // If a mythic is in the chest, just catch every exception (I don't want to risk a crash with a mythic in the chest)
                            e.printStackTrace();
                        }
                    }
                }
                // After checking every item in the chest
                this.searchedChest = true;
                if (isMythicInChest) {
                    if (!this.hasMythicBeenRegistered) {
                        this.hasMythicBeenRegistered = true;
                    }
                }

                current.setMinAndMax(minLevel, maxLevel);
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (ChestCountMod.CONFIG.shouldGuiConfigBeDrawn()) {
            ChestCountMod.getMC().displayGuiScreen(new ConfigsGui());
            ChestCountMod.CONFIG.setShouldGuiConfigBeDrawn(false);
        }
        // Clicking the edit button in the Info Location configs changes its value to Editing, so when that happens we
        // open up the locations editing gui
        if (ChestCountMod.CONFIG.getConfig("infoLocation").equals("Editing")) {
            ChestCountMod.getMC().displayGuiScreen(new LocationsGui());
            ChestCountMod.CONFIG.setConfig("infoLocation", "Edit");
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderOverlay(RenderGameOverlayEvent.Chat event) {
        // The Chat RenderGameOverlayEvent renders stuff normally, it disappears in f1, you can see it when your
        // inventory is open and you can make stuff transparent
        MultipleElements elements = InfoDisplay.getElementsToDraw();
        elements.draw();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void render3D(RenderWorldLastEvent event){
        ChestManager chestManager = ChestCountMod.getChestManager();
        for(TileEntity tileEntity : Minecraft.getMinecraft().world.loadedTileEntityList){
            if(tileEntity instanceof TileEntityChest){
//                LootChest chest = new LootChest(tileEntity.getPos(), 96, 100, 2);
                LootChest chest = chestManager.getChestAt(tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
                if(Objects.nonNull(chest) && getDistanceToBlock(tileEntity.getPos()) < 48){
                    Vec3d vec3d = new Vec3d(tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());

                    float yaw = Minecraft.getMinecraft().getRenderManager().playerViewY;
                    float pitch = Minecraft.getMinecraft().getRenderManager().playerViewX;
                    boolean thirdPerson = Minecraft.getMinecraft().getRenderManager().options.thirdPersonView == 2;

                    float x = (float) (vec3d.x - Minecraft.getMinecraft().getRenderManager().viewerPosX) +0.5f;
                    float y = (float) (vec3d.y - Minecraft.getMinecraft().getRenderManager().viewerPosY);
                    float z = (float) (vec3d.z - Minecraft.getMinecraft().getRenderManager().viewerPosZ) +0.5f;

                    // There is a lootchest to render stuff above
                    if(chest.getMinLevel() == -1 || chest.getMaxLevel() == -1){
                        RenderUtils.drawNameplate(Minecraft.getMinecraft().getRenderManager().getFontRenderer(),TextFormatting.AQUA +"Chest level not yet scanned", x, y + 1.5f, z, 0, yaw, pitch, thirdPerson);
                    }else{
                        List<Mythic> mythics = ChestCountMod.getMythicData().getMythicsIn(chest.getMinLevel(), chest.getMaxLevel());
                        List<String> lines = chestManager.getChestLines(chest, mythics);

                        for(String line : lines){
                            RenderUtils.drawNameplate(Minecraft.getMinecraft().getRenderManager().getFontRenderer(),line.substring(0, line.length() -2), x, y + 1.5f, z, 0, yaw, pitch, thirdPerson);
                            y += (double)((float)Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * 1.15F * 0.025F);
                        }

                        RenderUtils.drawNameplate(Minecraft.getMinecraft().getRenderManager().getFontRenderer(),TextFormatting.AQUA +"Lv. " +TextFormatting.DARK_AQUA
                                +(chest.getMinLevel() == chest.getMaxLevel() ? chest.getMinLevel() : ("" +chest.getMinLevel() +TextFormatting.AQUA +" - " +TextFormatting.DARK_AQUA +chest.getMaxLevel())), x, y + 1.5f, z, 0, yaw, pitch, thirdPerson);
                    }
                }
            }
        }
    }

    private int getLootchestTier(String roman){
        switch(roman) {
            case "I": return 1;
            case "II": return 2;
            case "III": return 3;
            case "IV": return 4;
        }

        return -1;
    }

    public static double getDistanceToBlock(BlockPos pos) {
        double deltaX = Minecraft.getMinecraft().player.getPosition().getX() - pos.getX();
        double deltaY = Minecraft.getMinecraft().player.getPosition().getY() - pos.getY();
        double deltaZ = Minecraft.getMinecraft().player.getPosition().getZ() - pos.getZ();

        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
    }
}
