    package com.example.factionsplugin;

import com.example.factionsplugin.libs.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class FactionMenuListener implements Listener {
   private final FactionsPlugin plugin;
   private final FactionMenu factionMenu;

   public FactionMenuListener(FactionsPlugin plugin, FactionMenu factionMenu) {
      this.plugin = plugin;
      this.factionMenu = factionMenu;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         String title = event.getView().getTitle();
         if (title.startsWith("§6")) {
            event.setCancelled(true);
         }

         ItemStack clicked = event.getCurrentItem();
         if (clicked != null && clicked.getType() != Material.AIR) {
            Faction faction = this.plugin.getPlayerFaction(player);
            if (faction != null) {
               if (title.equals("§6Меню фракции: " + faction.getName())) {
                  if (clicked.getType() == Material.NAME_TAG) {
                     this.factionMenu.openRanksMenu(player, faction);
                  } else if (clicked.getType() == Material.PLAYER_HEAD) {
                     this.factionMenu.openMembersMenu(player, faction);
                  }
               } else {
                  String rankName;
                  Player target;
                  if (title.equals("§6Участники фракции")) {
                     if (clicked.getType() == Material.ARROW) {
                        this.factionMenu.openMainMenu(player, faction);
                     } else if (clicked.getType() == Material.PLAYER_HEAD) {
                        rankName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                        target = Bukkit.getPlayer(rankName);
                        if (target != null) {
                           this.factionMenu.openPlayerManagementMenu(player, target.getUniqueId(), faction);
                        }
                     }
                  } else if (title.equals("§6Управление игроком")) {
                     if (clicked.getType() == Material.ARROW) {
                        this.factionMenu.openMembersMenu(player, faction);
                     } else {
                        FactionRank oldRank;
                        FactionRank newRank;
                        String var10001;
                        if (clicked.getType() == Material.EMERALD) {
                           rankName = ChatColor.stripColor(event.getInventory().getItem(13).getItemMeta().getDisplayName());
                           target = Bukkit.getPlayer(rankName);
                           if (target != null) {
                              try {
                                 oldRank = faction.getPlayerRank(target.getUniqueId());
                                 faction.promotePlayerOneRank(target.getUniqueId());
                                 newRank = faction.getPlayerRank(target.getUniqueId());
                                 var10001 = target.getName();
                                 faction.broadcast("§e" + var10001 + " повышен с ранга " + oldRank.getName() + " до " + newRank.getName() + "!");
                                 this.plugin.saveFactionsAsync();
                                 this.factionMenu.openPlayerManagementMenu(player, target.getUniqueId(), faction);
                              } catch (IllegalStateException var11) {
                                 player.sendMessage("§c" + var11.getMessage());
                              }
                           }
                        } else if (clicked.getType() == Material.REDSTONE) {
                           rankName = ChatColor.stripColor(event.getInventory().getItem(13).getItemMeta().getDisplayName());
                           target = Bukkit.getPlayer(rankName);
                           if (target != null) {
                              try {
                                 oldRank = faction.getPlayerRank(target.getUniqueId());
                                 faction.demotePlayerOneRank(target.getUniqueId());
                                 newRank = faction.getPlayerRank(target.getUniqueId());
                                 var10001 = target.getName();
                                 faction.broadcast("§c" + var10001 + " понижен с ранга " + oldRank.getName() + " до " + newRank.getName() + "!");
                                 this.plugin.saveFactionsAsync();
                                 this.factionMenu.openPlayerManagementMenu(player, target.getUniqueId(), faction);
                              } catch (IllegalArgumentException | IllegalStateException var10) {
                                 player.sendMessage("§c" + var10.getMessage());
                              }
                           }
                        } else if (clicked.getType() == Material.BARRIER) {
                           rankName = ChatColor.stripColor(event.getInventory().getItem(13).getItemMeta().getDisplayName());
                           target = Bukkit.getPlayer(rankName);
                           if (target != null) {
                              faction.removeMember(target.getUniqueId());
                              faction.broadcast("§c" + target.getName() + " был исключен из фракции!");
                              this.plugin.saveFactionsAsync();
                              player.closeInventory();
                           }
                        }
                     }
                  } else if (title.equals("§6Ранги фракции")) {
                     if (clicked.getType() == Material.EMERALD) {
                        (new AnvilGUI.Builder()).onComplete((p, text) -> {
                           try {
                              faction.createRank(text);
                              this.plugin.saveFactionsAsync();
                              p.sendMessage("§aРанг '" + text + "' успешно создан!");
                              Bukkit.getScheduler().runTask(this.plugin, () -> {
                                 this.factionMenu.openRanksMenu(p, faction);
                              });
                           } catch (IllegalStateException var5) {
                              p.sendMessage("§c" + var5.getMessage());
                           }

                           return AnvilGUI.Response.close();
                        }).text("Введите название").title("Создание ранга").plugin(this.plugin).open(player);
                     } else if (clicked.getType() == Material.ARROW) {
                        this.factionMenu.openMainMenu(player, faction);
                     } else if (event.getClick() == ClickType.RIGHT && clicked.getType() == Material.NAME_TAG) {
                        rankName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                        FactionRank rank = faction.getRankByName(rankName);
                        if (rank != null && !rank.isSystemRank()) {
                           faction.removeRank(rank);
                           this.plugin.saveFactionsAsync();
                           player.sendMessage("§aРанг '" + rankName + "' успешно удален!");
                           this.factionMenu.openRanksMenu(player, faction);
                        }
                     }
                  }
               }

            }
         }
      }
   }
}
    
