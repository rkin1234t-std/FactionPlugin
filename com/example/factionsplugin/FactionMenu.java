    package com.example.factionsplugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class FactionMenu {
   private final FactionsPlugin plugin;

   public FactionMenu(FactionsPlugin plugin) {
      this.plugin = plugin;
   }

   private ItemStack createMenuItem(Material material, String name, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (lore != null) {
            meta.setLore(lore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private void addPlayerHead(Inventory inventory, int slot, OfflinePlayer player, String displayName, Faction faction) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta != null) {
         meta.setOwningPlayer(player);
         meta.setDisplayName(displayName);
         List<String> lore = new ArrayList();
         FactionRank rank = faction.getPlayerRank(player.getUniqueId());
         lore.add("§7Ранг: §f" + rank.getName());
         if (faction.isLeader(player.getUniqueId())) {
            lore.add("§6Лидер фракции");
         } else if (faction.isDeputy(player.getUniqueId())) {
            lore.add("§eЗаместитель");
         }

         meta.setLore(lore);
         head.setItemMeta(meta);
         inventory.setItem(slot, head);
      }

   }

   public void openMainMenu(Player player, Faction faction) {
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, 27, "§6Меню фракции: " + faction.getName());
      List<String> infoLore = new ArrayList();
      infoLore.add("§7Лидер: §f" + Bukkit.getOfflinePlayer(faction.getLeader()).getName());
      infoLore.add("§7Участников: §f" + faction.getMemberUUIDs().size());
      menu.setItem(13, this.createMenuItem(Material.GOLDEN_HELMET, "§6" + faction.getName(), infoLore));
      ArrayList membersLore;
      if (faction.canManageRanks(player.getUniqueId())) {
         membersLore = new ArrayList();
         membersLore.add("§7Нажмите для управления");
         membersLore.add("§7рангами фракции");
         menu.setItem(11, this.createMenuItem(Material.NAME_TAG, "§eРанги", membersLore));
      }

      membersLore = new ArrayList();
      membersLore.add("§7Нажмите для просмотра");
      membersLore.add("§7списка участников");
      menu.setItem(15, this.createMenuItem(Material.PLAYER_HEAD, "§eУчастники", membersLore));
      player.openInventory(menu);
   }

   public void openMembersMenu(Player player, Faction faction) {
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, 54, "§6Участники фракции");
      int slot = 10;
      Iterator var5 = faction.getMemberUUIDs().iterator();

      while(var5.hasNext()) {
         UUID memberId = (UUID)var5.next();
         OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
         if (slot % 9 == 8) {
            slot += 2;
         }

         if (slot >= 54) {
            break;
         }

         this.addPlayerHead(menu, slot++, member, "§e" + member.getName(), faction);
      }

      menu.setItem(49, this.createMenuItem(Material.ARROW, "§7Назад", (List)null));
      player.openInventory(menu);
   }

   public void openPlayerManagementMenu(Player viewer, UUID targetId, Faction faction) {
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, 27, "§6Управление игроком");
      Player target = Bukkit.getPlayer(targetId);
      if (target != null) {
         FactionRank currentRank = faction.getPlayerRank(targetId);
         FactionRank nextRank = faction.getNextRank(currentRank);
         FactionRank previousRank = faction.getPreviousRank(currentRank);
         ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)playerHead.getItemMeta();
         meta.setOwningPlayer(target);
         meta.setDisplayName("§e" + target.getName());
         List<String> lore = new ArrayList();
         lore.add("§7Текущий ранг: §f" + currentRank.getName());
         if (nextRank != null) {
            lore.add("§7Следующий ранг: §f" + nextRank.getName());
         }

         if (previousRank != null) {
            lore.add("§7Предыдущий ранг: §f" + previousRank.getName());
         }

         meta.setLore(lore);
         playerHead.setItemMeta(meta);
         menu.setItem(13, playerHead);
         ArrayList demoteLore;
         if (nextRank != null && faction.canManageRanks(viewer.getUniqueId())) {
            demoteLore = new ArrayList();
            demoteLore.add("§7Нажмите, чтобы повысить игрока");
            demoteLore.add("§7с " + currentRank.getName());
            demoteLore.add("§7до " + nextRank.getName());
            menu.setItem(11, this.createMenuItem(Material.EMERALD, "§aПовысить до " + nextRank.getName(), demoteLore));
         }

         if (previousRank != null && faction.canManageRanks(viewer.getUniqueId())) {
            demoteLore = new ArrayList();
            demoteLore.add("§7Нажмите, чтобы понизить игрока");
            demoteLore.add("§7с " + currentRank.getName());
            demoteLore.add("§7до " + previousRank.getName());
            menu.setItem(15, this.createMenuItem(Material.REDSTONE, "§cПонизить до " + previousRank.getName(), demoteLore));
         }

         if (faction.canManageRanks(viewer.getUniqueId())) {
            menu.setItem(26, this.createMenuItem(Material.BARRIER, "§cИсключить из фракции", (List)null));
         }

         menu.setItem(18, this.createMenuItem(Material.ARROW, "§7Назад", (List)null));
         viewer.openInventory(menu);
      }
   }

   public void openRanksMenu(Player player, Faction faction) {
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, 54, "§6Ранги фракции");
      List<FactionRank> sortedRanks = faction.getSortedRanks();
      int slot = 10;

      FactionRank rank;
      ArrayList lore;
      for(Iterator var6 = sortedRanks.iterator(); var6.hasNext(); menu.setItem(slot++, this.createMenuItem(Material.NAME_TAG, (rank.isSystemRank() ? "§c" : "§a") + rank.getName(), lore))) {
         rank = (FactionRank)var6.next();
         if (slot % 9 == 8) {
            slot += 2;
         }

         if (slot >= 54) {
            break;
         }

         lore = new ArrayList();
         int var10001 = rank.getId();
         lore.add("§7ID: " + var10001);
         lore.add(rank.isSystemRank() ? "§cСистемный ранг" : "§aКастомный ранг");
         List var10 = faction.getMembersWithRank(rank);
         lore.add("§7Участников с этим рангом: §f" + var10.size());
         if (!rank.isSystemRank()) {
            lore.add("");
            lore.add("§cПКМ для удаления ранга");
         }
      }

      if (faction.canManageRanks(player.getUniqueId())) {
         List<String> createLore = new ArrayList();
         createLore.add("§7Нажмите, чтобы создать");
         createLore.add("§7новый ранг");
         menu.setItem(49, this.createMenuItem(Material.EMERALD, "§aСоздать новый ранг", createLore));
      }

      menu.setItem(45, this.createMenuItem(Material.ARROW, "§7Назад", (List)null));
      player.openInventory(menu);
   }
}
    
