    package com.example.factionsplugin.libs.anvilgui;

import com.example.factionsplugin.libs.anvilgui.version.VersionMatcher;
import com.example.factionsplugin.libs.anvilgui.version.VersionWrapper;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class AnvilGUI {
   private static VersionWrapper WRAPPER = (new VersionMatcher()).match();
   private final Plugin plugin;
   private final Player player;
   private String inventoryTitle;
   private ItemStack inputLeft;
   private ItemStack inputRight;
   private final boolean preventClose;
   private final Consumer<Player> closeListener;
   private final BiFunction<Player, String, AnvilGUI.Response> completeFunction;
   private final Consumer<Player> inputLeftClickListener;
   private final Consumer<Player> inputRightClickListener;
   private int containerId;
   private Inventory inventory;
   private final AnvilGUI.ListenUp listener;
   private boolean open;

   /** @deprecated */
   @Deprecated
   public AnvilGUI(Plugin plugin, Player holder, String insert, BiFunction<Player, String, String> biFunction) {
      this(plugin, holder, "Repair & Name", insert, (ItemStack)null, (ItemStack)null, false, (Consumer)null, (Consumer)null, (Consumer)null, (player, text) -> {
         String response = (String)biFunction.apply(player, text);
         return response != null ? AnvilGUI.Response.text(response) : AnvilGUI.Response.close();
      });
   }

   private AnvilGUI(Plugin plugin, Player player, String inventoryTitle, String itemText, ItemStack inputLeft, ItemStack inputRight, boolean preventClose, Consumer<Player> closeListener, Consumer<Player> inputLeftClickListener, Consumer<Player> inputRightClickListener, BiFunction<Player, String, AnvilGUI.Response> completeFunction) {
      this.listener = new AnvilGUI.ListenUp();
      this.plugin = plugin;
      this.player = player;
      this.inventoryTitle = inventoryTitle;
      this.inputLeft = inputLeft;
      this.inputRight = inputRight;
      this.preventClose = preventClose;
      this.closeListener = closeListener;
      this.inputLeftClickListener = inputLeftClickListener;
      this.inputRightClickListener = inputRightClickListener;
      this.completeFunction = completeFunction;
      if (itemText != null) {
         if (inputLeft == null) {
            this.inputLeft = new ItemStack(Material.PAPER);
         }

         ItemMeta paperMeta = this.inputLeft.getItemMeta();
         paperMeta.setDisplayName(itemText);
         this.inputLeft.setItemMeta(paperMeta);
      }

      this.openInventory();
   }

   private void openInventory() {
      WRAPPER.handleInventoryCloseEvent(this.player);
      WRAPPER.setActiveContainerDefault(this.player);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      Object container = WRAPPER.newContainerAnvil(this.player, this.inventoryTitle);
      this.inventory = WRAPPER.toBukkitInventory(container);
      this.inventory.setItem(0, this.inputLeft);
      if (this.inputRight != null) {
         this.inventory.setItem(1, this.inputRight);
      }

      this.containerId = WRAPPER.getNextContainerId(this.player, container);
      WRAPPER.sendPacketOpenWindow(this.player, this.containerId, this.inventoryTitle);
      WRAPPER.setActiveContainer(this.player, container);
      WRAPPER.setActiveContainerId(container, this.containerId);
      WRAPPER.addActiveContainerSlotListener(container, this.player);
      this.open = true;
   }

   public void closeInventory() {
      this.closeInventory(true);
   }

   private void closeInventory(boolean sendClosePacket) {
      if (this.open) {
         this.open = false;
         HandlerList.unregisterAll(this.listener);
         if (sendClosePacket) {
            WRAPPER.handleInventoryCloseEvent(this.player);
            WRAPPER.setActiveContainerDefault(this.player);
            WRAPPER.sendPacketCloseWindow(this.player, this.containerId);
         }

         if (this.closeListener != null) {
            this.closeListener.accept(this.player);
         }

      }
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   // $FF: synthetic method
   AnvilGUI(Plugin x0, Player x1, String x2, String x3, ItemStack x4, ItemStack x5, boolean x6, Consumer x7, Consumer x8, Consumer x9, BiFunction x10, Object x11) {
      this(x0, x1, x2, x3, x4, x5, x6, x7, x8, x9, x10);
   }

   public static class Slot {
      private static final int[] values = new int[]{0, 1, 2};
      public static final int INPUT_LEFT = 0;
      public static final int INPUT_RIGHT = 1;
      public static final int OUTPUT = 2;

      public static int[] values() {
         return values;
      }
   }

   public static class Response {
      private final String text;
      private final Inventory openInventory;

      private Response(String text, Inventory openInventory) {
         this.text = text;
         this.openInventory = openInventory;
      }

      public String getText() {
         return this.text;
      }

      public Inventory getInventoryToOpen() {
         return this.openInventory;
      }

      public static AnvilGUI.Response close() {
         return new AnvilGUI.Response((String)null, (Inventory)null);
      }

      public static AnvilGUI.Response text(String text) {
         return new AnvilGUI.Response(text, (Inventory)null);
      }

      public static AnvilGUI.Response openInventory(Inventory inventory) {
         return new AnvilGUI.Response((String)null, inventory);
      }
   }

   public static class Builder {
      private Consumer<Player> closeListener;
      private boolean preventClose = false;
      private Consumer<Player> inputLeftClickListener;
      private Consumer<Player> inputRightClickListener;
      private BiFunction<Player, String, AnvilGUI.Response> completeFunction;
      private Plugin plugin;
      private String title = "Repair & Name";
      private String itemText;
      private ItemStack itemLeft;
      private ItemStack itemRight;

      public AnvilGUI.Builder preventClose() {
         this.preventClose = true;
         return this;
      }

      public AnvilGUI.Builder onClose(Consumer<Player> closeListener) {
         Validate.notNull(closeListener, "closeListener cannot be null");
         this.closeListener = closeListener;
         return this;
      }

      public AnvilGUI.Builder onLeftInputClick(Consumer<Player> inputLeftClickListener) {
         this.inputLeftClickListener = inputLeftClickListener;
         return this;
      }

      public AnvilGUI.Builder onRightInputClick(Consumer<Player> inputRightClickListener) {
         this.inputRightClickListener = inputRightClickListener;
         return this;
      }

      public AnvilGUI.Builder onComplete(BiFunction<Player, String, AnvilGUI.Response> completeFunction) {
         Validate.notNull(completeFunction, "Complete function cannot be null");
         this.completeFunction = completeFunction;
         return this;
      }

      public AnvilGUI.Builder plugin(Plugin plugin) {
         Validate.notNull(plugin, "Plugin cannot be null");
         this.plugin = plugin;
         return this;
      }

      public AnvilGUI.Builder text(String text) {
         Validate.notNull(text, "Text cannot be null");
         this.itemText = text;
         return this;
      }

      public AnvilGUI.Builder title(String title) {
         Validate.notNull(title, "title cannot be null");
         this.title = title;
         return this;
      }

      /** @deprecated */
      @Deprecated
      public AnvilGUI.Builder item(ItemStack item) {
         return this.itemLeft(item);
      }

      public AnvilGUI.Builder itemLeft(ItemStack item) {
         Validate.notNull(item, "item cannot be null");
         this.itemLeft = item;
         return this;
      }

      public AnvilGUI.Builder itemRight(ItemStack item) {
         this.itemRight = item;
         return this;
      }

      public AnvilGUI open(Player player) {
         Validate.notNull(this.plugin, "Plugin cannot be null");
         Validate.notNull(this.completeFunction, "Complete function cannot be null");
         Validate.notNull(player, "Player cannot be null");
         return new AnvilGUI(this.plugin, player, this.title, this.itemText, this.itemLeft, this.itemRight, this.preventClose, this.closeListener, this.inputLeftClickListener, this.inputRightClickListener, this.completeFunction);
      }
   }

   private class ListenUp implements Listener {
      private ListenUp() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent event) {
         if (event.getInventory().equals(AnvilGUI.this.inventory) && (event.getRawSlot() < 3 || event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY))) {
            event.setCancelled(true);
            Player clicker = (Player)event.getWhoClicked();
            if (event.getRawSlot() == 2) {
               ItemStack clicked = AnvilGUI.this.inventory.getItem(2);
               if (clicked == null || clicked.getType() == Material.AIR) {
                  return;
               }

               AnvilGUI.Response response = (AnvilGUI.Response)AnvilGUI.this.completeFunction.apply(clicker, clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : "");
               if (response.getText() != null) {
                  ItemMeta meta = clicked.getItemMeta();
                  meta.setDisplayName(response.getText());
                  clicked.setItemMeta(meta);
                  AnvilGUI.this.inventory.setItem(0, clicked);
               } else if (response.getInventoryToOpen() != null) {
                  clicker.openInventory(response.getInventoryToOpen());
               } else {
                  AnvilGUI.this.closeInventory();
               }
            } else if (event.getRawSlot() == 0) {
               if (AnvilGUI.this.inputLeftClickListener != null) {
                  AnvilGUI.this.inputLeftClickListener.accept(AnvilGUI.this.player);
               }
            } else if (event.getRawSlot() == 1 && AnvilGUI.this.inputRightClickListener != null) {
               AnvilGUI.this.inputRightClickListener.accept(AnvilGUI.this.player);
            }
         }

      }

      @EventHandler
      public void onInventoryDrag(InventoryDragEvent event) {
         if (event.getInventory().equals(AnvilGUI.this.inventory)) {
            int[] var2 = AnvilGUI.Slot.values();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               int slot = var2[var4];
               if (event.getRawSlots().contains(slot)) {
                  event.setCancelled(true);
                  break;
               }
            }
         }

      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (AnvilGUI.this.open && event.getInventory().equals(AnvilGUI.this.inventory)) {
            AnvilGUI.this.closeInventory(false);
            if (AnvilGUI.this.preventClose) {
               Bukkit.getScheduler().runTask(AnvilGUI.this.plugin, () -> {
                  AnvilGUI.this.openInventory();
               });
            }
         }

      }

      // $FF: synthetic method
      ListenUp(Object x1) {
         this();
      }
   }
}
    
