package tk.avicia.chestcountmod;

import com.google.gson.*;
import org.lwjgl.Sys;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import scala.xml.Null;
import tk.avicia.chestcountmod.core.Mythic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class MythicData {

    private List<Mythic> mythicList = new CopyOnWriteArrayList<>();
    private int chestsDry = 0;

    public MythicData() {
        Thread test = new Thread(this::loadMythicsFromAPI);

        test.start();
        try{
            test.join();
            mythicList.sort(Comparator.comparing(Mythic::getType));
            mythicList.sort(Comparator.comparing(Mythic::getPriority).reversed());
        }catch(Exception ignored){}
    }

    public JsonObject getMythicData() {
        CustomFile mythicData = new CustomFile(ChestCountMod.getMC().mcDataDir, "chestcountmod/mythicData.json");

        return mythicData.readJson();
    }

    public void addMythic(int chestCount, String mythic, int dry, int x, int y, int z) {
        try {

            JsonObject currentData = getMythicData();
            JsonObject newData = new JsonObject();
            newData.addProperty("chestCount", chestCount);
            newData.addProperty("mythic", mythic);
            newData.addProperty("dry", dry);
            newData.addProperty("x", x);
            newData.addProperty("y", y);
            newData.addProperty("z", z);
            if (!currentData.has(ChestCountMod.PLAYER_UUID)) {
                currentData.add(ChestCountMod.PLAYER_UUID, new JsonArray());
            }
            currentData.get(ChestCountMod.PLAYER_UUID).getAsJsonArray().add(newData);
            CustomFile mythicData = new CustomFile(ChestCountMod.getMC().mcDataDir, "chestcountmod/mythicData.json");
            mythicData.writeJson(currentData);
            updateDry();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void updateDry() {
        try {
            JsonObject lastMythic = getLastMythic();
            if (lastMythic != null) {
                chestsDry = ChestCountMod.getChestCountData().getTotalChestCount() - lastMythic.get("chestCount").getAsInt();
            } else {
                chestsDry = ChestCountMod.getChestCountData().getTotalChestCount();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void addToDry() {
        chestsDry++;
    }

    public int getChestsDry() {
        return chestsDry;
    }

    public JsonObject getLastMythic() {
        JsonObject mythicData = getMythicData().getAsJsonObject();
        if (!mythicData.has(ChestCountMod.PLAYER_UUID)) {
            mythicData.add(ChestCountMod.PLAYER_UUID, new JsonArray());
        }
        JsonArray mythics = mythicData.get(ChestCountMod.PLAYER_UUID).getAsJsonArray();
        if (mythics.size() > 0) {
            return mythics.get(mythics.size() - 1).getAsJsonObject();
        } else {
            return null;
        }
    }

    public void loadMythicsFromAPI() {
        try {
            URL urlObject = new URL("https://api.wynncraft.com/public_api.php?action=itemDB&category=all");
            HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JsonArray itemList = new JsonParser().parse(response.toString()).getAsJsonObject()
                        .getAsJsonArray("items");

                for(JsonElement element : itemList){
                    try{
                        JsonObject item = (JsonObject) element;
                        String name = item.get("name").getAsString();
                        String tier = item.get("tier").getAsString();
                        String type = item.get("type").getAsString();
                        int level = item.get("level").getAsInt();
                        if(tier.equalsIgnoreCase("mythic")) mythicList.add(new Mythic(name, Mythic.Type.valueOf(type.toUpperCase()), level, getMythicPriority(name)));
                    }catch(JsonParseException | NullPointerException ignored){}
                }
            } else {
                System.out.println("GET request not worked");
                throw new NotFound();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Mythic> getMythicsIn(int min, int max){
        return mythicList.stream().filter(mythic -> mythic.getLevel() >= min && mythic.getLevel() <= max).collect(Collectors.toList());
    }

    public int getMythicPriority(String name){
        switch(name){
            case "Discoverer": return 100;

            case "Oblivion": return 99;
            case "Warp": return 98;
            case "Idol": return 97;
            case "Weathered": return 96;
            case "Toxoplasmosis": return 95;
            case "Guardian": return 94;

            case "Ignis": return 93;
            case "Olympic": return 92;
            case "Aftershock": return 91;

            case "Crabs": return 90;
            case "Inferno": return 89;
            case "Nirvana": return 88;
            case "Divzer": return 87;
            case "Stratiformis": return 86;
            case "Spring": return 85;

            case "Warchief": return 84;
            case "Moontower": return 83;
            default: return 0;
        }
    }
}
