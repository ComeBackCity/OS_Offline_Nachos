package nachos.vm;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.Hashtable;

public class PageTable {
    Hashtable <iptKey, TranslationEntry> table;
    static PageTable instance = null;

    private PageTable(){
        table = new Hashtable<>(Machine.processor().getNumPhysPages());
        instance = this;
    }

    public int getLength(){
        return table.size();
    }

    public static PageTable getInstance(){
        if (instance == null)
            instance = new PageTable();

        return instance;
    }

    public boolean addEntry(iptKey key, TranslationEntry entry){
        if (table.containsKey(key)){
            return false;
        }
        table.put(key, entry);
        return true;
    }

    public TranslationEntry getEntry(iptKey key){
        TranslationEntry toReturn = table.get(key);
        return toReturn;
    }

    public void updateEntry(iptKey key, TranslationEntry entry){
        table.replace(key, entry);
    }

    public void deleteEntry(iptKey key){
        table.remove(key);
    }


}
