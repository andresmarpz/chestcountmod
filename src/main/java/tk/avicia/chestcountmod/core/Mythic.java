package tk.avicia.chestcountmod.core;

public class Mythic{

    private String name;
    private Type type;
    private int level, priority;

    public Mythic(String name, Type type, int level){
        this.name = name;
        this.type = type;
        this.level = level;
    }

    public Mythic(String name, Type type, int level, int priority){
        this.name = name;
        this.type = type;
        this.level = level;
        this.priority = priority;
    }

    public String getName() {
        if(name.equalsIgnoreCase("Crusade Sabatons")) // Crabs for convenience
            return "Crabs";
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getPriority() {
        return priority;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public enum Type{
        CHESTPLATE("Chestplate"),
        DAGGER("Dagger"),
        WAND("Wand"),
        BOW("Bow"),
        SPEAR("Spear"),
        RELIK("Relik"),
        BOOTS("Boots");

        private final String label;

        Type(String label){
            this.label = label;
        }

        public String getLabel(){
            return this.label;
        }
    }
}
