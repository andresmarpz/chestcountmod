package tk.avicia.chestcountmod;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.Sys;
import tk.avicia.chestcountmod.core.LootChest;
import tk.avicia.chestcountmod.core.Mythic;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class ChestManager{

    private Set<LootChest> chests = new CopyOnWriteArraySet<>();
    private CustomFile lootchestsFile;

    public Set<LootChest> getChests() {
        return chests;
    }

    public void initialize(){
        lootchestsFile = new CustomFile(ChestCountMod.getMC().mcDataDir, "chestcountmod/lootchests.json");
        load();
    }

    public LootChest addChestAt(double x, double y, double z, int tier){
        LootChest newChest = new LootChest(x, y, z, tier);
        chests.add(newChest);

        return newChest;
    }

    /*

        Im not really sure if this is really necessary performance-wise
        or if it does really help at all, but it couldn't be worse, could it?

     */
    @Nullable
    public LootChest getChestAt(double x, double y, double z){
        for(LootChest chest : chests)
            if(chest.getX() == x && chest.getY() == y && chest.getZ() == z)
                return chest;

        return null;
    }

    public void save(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(chests);

        lootchestsFile.writeJson(json);
    }

    public void load(){
        try{
            Gson gson = new Gson();
            String obj = lootchestsFile.readJsonAsString();
            Type listType = new TypeToken<Set<LootChest>>() {}.getType();
            Set<LootChest> lootchests = gson.fromJson(obj, listType);
            chests.addAll(lootchests);
        }
        catch(Exception ex){
            System.out.println("[ChestCountMod] Could not load lootchests. Not yet initialized?");
        }
    }

    public List<String> getChestLines(LootChest chest, List<Mythic> mythics){
        Stack<String> blocks = new Stack<>();
        if(chest.getTier() < 3) mythics.removeIf(mythic -> mythic.getType() == Mythic.Type.CHESTPLATE); // Remove Disco if exists in tier I or II
        if(mythics.size() <= 12)
            mythics.forEach(mythic -> blocks.push(TextFormatting.DARK_PURPLE +mythic.getName()));
        else{
            // Sort by priority ascending
            List<Mythic> priority = mythics.stream().filter(mythic -> mythic.getPriority() > 0).sorted(Comparator.comparingInt(Mythic::getPriority)).collect(Collectors.toList());
            // Reverse to sort descending
            Collections.reverse(priority);
            // Push highest prio at the bottom of the stack
            priority.forEach(p -> blocks.push(TextFormatting.DARK_PURPLE +p.getName()));
            mythics.removeAll(priority);

            // Push best types first
            Mythic.Type[] types = Mythic.Type.values();
            for(Mythic.Type type : types){
                if(type == Mythic.Type.CHESTPLATE && chest.getTier() < 3) continue;

                List<Mythic> filtered = mythics.stream().filter(mythic -> mythic.getType() == type).collect(Collectors.toList());
                if(filtered.isEmpty()) continue;

                blocks.push(TextFormatting.DARK_PURPLE +type.getLabel() +TextFormatting.GRAY +": " +TextFormatting.YELLOW +filtered.size());
            }
        }

        // Split the entire string into lines
        List<String> lines = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        // Since the important blocks are at the bottom, do a reverse sort on the stack
        // to pull importants first
        // first line in list will be displayed at the bottom so we'll reverse the list later
        Collections.reverse(blocks);
        while(!blocks.isEmpty() && lines.size() < 3) {
            builder.append(blocks.pop()).append(TextFormatting.GRAY).append(", ");
            if(builder.length() > 50){
                lines.add(builder.toString());
                builder.setLength(0);
            }
        }

        // If there were more than 3 lines, ignore the rest since it's sorted by priority
        if(!blocks.isEmpty()){
            // Ignored stuff
            List<String> blocksLeft = new ArrayList<>(blocks);
            List<String> withAmount = blocksLeft.stream().filter(bl -> bl.contains(": ")).collect(Collectors.toList());
            int amount = 0;
            for(String block : withAmount)
                amount += Integer.parseInt(TextFormatting.getTextWithoutFormattingCodes(block).split(": ")[1]);
            blocksLeft.removeAll(withAmount);
            lines.add(TextFormatting.GRAY +"" +TextFormatting.ITALIC +"Ignored: " +TextFormatting.LIGHT_PURPLE +(amount + blocksLeft.size()) +TextFormatting.GRAY +", ");
        }

        return Lists.reverse(lines);
    }
}
