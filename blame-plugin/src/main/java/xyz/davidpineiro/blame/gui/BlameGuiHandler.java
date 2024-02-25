package xyz.davidpineiro.blame.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import xyz.davidpineiro.blame.Blame;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlameGuiHandler implements Listener {

    private final Blame blame;

    private enum State{
        MAIN_MENU
        ;

        State(Supplier<Inventory> initInventory,
              Consumer<InventoryClickEvent> eventProcessor){

        }
    }

    public BlameGuiHandler(Blame blame) {
        this.blame = blame;
    }

    public void register() {
        blame.getServer().getPluginManager().registerEvents(this, blame);
    }

    @EventHandler
    public void onGui(InventoryClickEvent e){
        final InventoryView view = e.getView();

        e.setCancelled(true);
    }
}
