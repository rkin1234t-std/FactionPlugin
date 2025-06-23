    package com.example.factionsplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionsPlugin extends JavaPlugin {
   private Map<String, Faction> factions;
   private Map<UUID, String> playerFactions;
   private Map<UUID, FactionInvite> pendingInvites;
   private FactionMenu factionMenu;
   private File factionsFile;
   private FileConfiguration factionsConfig;

   public void onEnable() {
      this.factions = new HashMap();
      this.playerFactions = new HashMap();
      this.pendingInvites = new HashMap();
      if (!this.getDataFolder().exists()) {
         this.getDataFolder().mkdir();
      }

      this.factionsFile = new File(this.getDataFolder(), "factions.yml");
      if (!this.factionsFile.exists()) {
         try {
            this.factionsFile.createNewFile();
            InputStream in = this.getResource("factions.yml");
            if (in != null) {
               Files.copy(in, this.factionsFile.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            }
         } catch (IOException var2) {
            this.getLogger().severe("Не удалось создать файл конфигурации: " + var2.getMessage());
         }
      }

      this.factionsConfig = YamlConfiguration.loadConfiguration(this.factionsFile);
      this.loadFactions();
      this.factionMenu = new FactionMenu(this);
      this.getServer().getPluginManager().registerEvents(new FactionMenuListener(this, this.factionMenu), this);
      this.getCommand("f").setExecutor(this);
      this.getLogger().info("Плагин фракций успешно загружен!");
   }

   public void onDisable() {
      this.saveFactions();
      this.getLogger().info("Плагин фракций выключен!");
   }

   private void loadFactions() {
      ConfigurationSection factionsSection = this.factionsConfig.getConfigurationSection("factions");
      if (factionsSection == null) {
         this.getLogger().info("Секция фракций не найдена в конфиге. Создаем новую...");
         this.factionsConfig.createSection("factions");
      } else {
         Iterator var2 = factionsSection.getKeys(false).iterator();

         while(true) {
            while(var2.hasNext()) {
               String factionName = (String)var2.next();
               ConfigurationSection factionSection = factionsSection.getConfigurationSection(factionName);
               if (factionSection == null) {
                  this.getLogger().warning("Пропущена загрузка фракции " + factionName + ": секция данных отсутствует");
               } else {
                  try {
                     String leaderString = factionSection.getString("leader");
                     if (leaderString == null) {
                        this.getLogger().warning("Пропущена загрузка фракции " + factionName + ": отсутствует лидер");
                     } else {
                        UUID leaderUUID = UUID.fromString(leaderString);
                        Faction faction = new Faction(factionName, leaderUUID);
                        List<String> deputyList = factionSection.getStringList("deputies");
                        Iterator var9 = deputyList.iterator();

                        while(var9.hasNext()) {
                           String deputy = (String)var9.next();

                           try {
                              UUID deputyUUID = UUID.fromString(deputy);
                              faction.addDeputy(deputyUUID);
                           } catch (IllegalArgumentException var19) {
                              this.getLogger().warning("Ошибка при загрузке заместителя фракции " + factionName + ": " + deputy);
                           }
                        }

                        List<String> memberList = factionSection.getStringList("members");
                        Iterator var22 = memberList.iterator();

                        while(var22.hasNext()) {
                           String member = (String)var22.next();

                           try {
                              UUID memberUUID = UUID.fromString(member);
                              faction.addMember(memberUUID);
                              this.playerFactions.put(memberUUID, factionName.toLowerCase());
                           } catch (IllegalArgumentException var18) {
                              this.getLogger().warning("Ошибка при загрузке участника фракции " + factionName + ": " + member);
                           }
                        }

                        ConfigurationSection ranksSection = factionSection.getConfigurationSection("ranks");
                        if (ranksSection != null) {
                           Iterator var25 = ranksSection.getKeys(false).iterator();

                           while(var25.hasNext()) {
                              String rankName = (String)var25.next();

                              try {
                                 ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
                                 if (rankSection != null) {
                                    int rankId = rankSection.getInt("id");
                                    boolean isSystem = rankSection.getBoolean("system");
                                    FactionRank rank = new FactionRank(rankId, rankName, isSystem);
                                    faction.addRank(rank);
                                 }
                              } catch (Exception var17) {
                                 this.getLogger().warning("Ошибка при загрузке ранга " + rankName + " фракции " + factionName + ": " + var17.getMessage());
                              }
                           }
                        } else {
                           faction.initializeDefaultRanks();
                        }

                        this.factions.put(factionName.toLowerCase(), faction);
                        this.getLogger().info("Загружена фракция: " + factionName + " (Лидер: " + Bukkit.getOfflinePlayer(leaderUUID).getName() + ")");
                     }
                  } catch (Exception var20) {
                     this.getLogger().severe("Ошибка при загрузке фракции " + factionName + ": " + var20.getMessage());
                  }
               }
            }

            this.getLogger().info("Загружено фракций: " + this.factions.size());
            return;
         }
      }
   }

   public void saveFactions() {
      if (this.factionsConfig == null) {
         this.factionsConfig = new YamlConfiguration();
      }

      this.factionsConfig.set("factions", (Object)null);
      ConfigurationSection factionsSection = this.factionsConfig.createSection("factions");
      Iterator var2 = this.factions.entrySet().iterator();

      while(var2.hasNext()) {
         Entry entry = (Entry)var2.next();

         try {
            Faction faction = (Faction)entry.getValue();
            ConfigurationSection factionSection = factionsSection.createSection((String)entry.getKey());
            factionSection.set("leader", faction.getLeader().toString());
            List<String> deputyList = (List)faction.getDeputies().stream().map(UUID::toString).collect(Collectors.toList());
            factionSection.set("deputies", deputyList);
            List<String> memberList = (List)faction.getMemberUUIDs().stream().map(UUID::toString).collect(Collectors.toList());
            factionSection.set("members", memberList);
            ConfigurationSection ranksSection = factionSection.createSection("ranks");
            Iterator var9 = faction.getRanks().iterator();

            while(var9.hasNext()) {
               FactionRank rank = (FactionRank)var9.next();
               ConfigurationSection rankSection = ranksSection.createSection(rank.getName());
               rankSection.set("id", rank.getId());
               rankSection.set("system", rank.isSystemRank());
            }
         } catch (Exception var13) {
            Logger var10000 = this.getLogger();
            String var10001 = (String)entry.getKey();
            var10000.severe("Ошибка при сохранении фракции " + var10001 + ": " + var13.getMessage());
         }
      }

      try {
         this.factionsConfig.save(this.factionsFile);
      } catch (IOException var12) {
         this.getLogger().severe("Не удалось сохранить данные фракций: " + var12.getMessage());
      }

   }

   public void saveFactionsAsync() {
      Bukkit.getScheduler().runTaskAsynchronously(this, this::saveFactions);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cЭта команда доступна только для игроков!");
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
         } else {
            String subCommand = args[0].toLowerCase();
            byte var8 = -1;
            switch(subCommand.hashCode()) {
            case -1423461112:
               if (subCommand.equals("accept")) {
                  var8 = 3;
               }
               break;
            case -1352294148:
               if (subCommand.equals("create")) {
                  var8 = 0;
               }
               break;
            case -1335458389:
               if (subCommand.equals("delete")) {
                  var8 = 1;
               }
               break;
            case -1335418988:
               if (subCommand.equals("demote")) {
                  var8 = 8;
               }
               break;
            case -1183699191:
               if (subCommand.equals("invite")) {
                  var8 = 2;
               }
               break;
            case -309211200:
               if (subCommand.equals("promote")) {
                  var8 = 7;
               }
               break;
            case 3079692:
               if (subCommand.equals("deny")) {
                  var8 = 4;
               }
               break;
            case 3237038:
               if (subCommand.equals("info")) {
                  var8 = 10;
               }
               break;
            case 3291718:
               if (subCommand.equals("kick")) {
                  var8 = 6;
               }
               break;
            case 3322014:
               if (subCommand.equals("list")) {
                  var8 = 11;
               }
               break;
            case 3347807:
               if (subCommand.equals("menu")) {
                  var8 = 12;
               }
               break;
            case 3492908:
               if (subCommand.equals("rank")) {
                  var8 = 13;
               }
               break;
            case 102846135:
               if (subCommand.equals("leave")) {
                  var8 = 5;
               }
               break;
            case 1290776555:
               if (subCommand.equals("setleader")) {
                  var8 = 9;
               }
            }

            switch(var8) {
            case 0:
               if (player.hasPermission("factions.admin")) {
                  this.handleCreateCommand(player, args);
               } else {
                  player.sendMessage("§cУ вас нет прав для создания фракций!");
               }
               break;
            case 1:
               if (player.hasPermission("factions.admin")) {
                  this.handleDeleteCommand(player, args);
               } else {
                  player.sendMessage("§cУ вас нет прав для удаления фракций!");
               }
               break;
            case 2:
               this.handleInviteCommand(player, args);
               break;
            case 3:
               this.handleAcceptCommand(player);
               break;
            case 4:
               this.handleDenyCommand(player);
               break;
            case 5:
               this.handleLeaveCommand(player);
               break;
            case 6:
               this.handleKickCommand(player, args);
               break;
            case 7:
               this.handlePromoteCommand(player, args);
               break;
            case 8:
               this.handleDemoteCommand(player, args);
               break;
            case 9:
               if (player.hasPermission("factions.admin")) {
                  this.handleSetLeaderCommand(player, args);
               } else {
                  player.sendMessage("§cУ вас нет прав для назначения лидера!");
               }
               break;
            case 10:
               this.handleInfoCommand(player, args);
               break;
            case 11:
               this.handleListCommand(player);
               break;
            case 12:
               this.handleMenuCommand(player);
               break;
            case 13:
               this.handleRankCommand(player, args);
               break;
            default:
               this.sendHelpMessage(player);
            }

            return true;
         }
      }
   }

   private void handleCreateCommand(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage("§cИспользование: /f create <название> <лидер>");
      } else {
         String factionName = args[1];
         String leaderName = args[2];
         if (this.factions.containsKey(factionName.toLowerCase())) {
            player.sendMessage("§cФракция с таким названием уже существует!");
         } else {
            Player leader = Bukkit.getPlayer(leaderName);
            if (leader == null) {
               player.sendMessage("§cИгрок " + leaderName + " не найден или не в сети!");
            } else if (this.getPlayerFaction(leader) != null) {
               player.sendMessage("§cИгрок " + leaderName + " уже состоит в другой фракции!");
            } else {
               Faction newFaction = new Faction(factionName, leader.getUniqueId());
               newFaction.initializeDefaultRanks();
               this.factions.put(factionName.toLowerCase(), newFaction);
               this.playerFactions.put(leader.getUniqueId(), factionName.toLowerCase());
               this.saveFactionsAsync();
               this.getLogger().info("Администратор " + player.getName() + " создал фракцию: " + factionName + " с лидером " + leaderName);
               player.sendMessage("§aФракция " + factionName + " успешно создана с лидером " + leaderName + "!");
               leader.sendMessage("§aВы были назначены лидером фракции " + factionName + "!");
            }
         }
      }
   }

   private void handleDeleteCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f delete <название>");
      } else {
         String factionName = args[1].toLowerCase();
         Faction faction = (Faction)this.factions.get(factionName);
         if (faction == null) {
            player.sendMessage("§cФракция не найдена!");
         } else {
            Iterator var5 = faction.getMemberUUIDs().iterator();

            while(var5.hasNext()) {
               UUID memberId = (UUID)var5.next();
               this.playerFactions.remove(memberId);
               Player member = Bukkit.getPlayer(memberId);
               if (member != null && member.isOnline()) {
                  member.sendMessage("§cВаша фракция была удалена администратором!");
               }
            }

            this.factions.remove(factionName);
            this.saveFactionsAsync();
            Logger var10000 = this.getLogger();
            String var10001 = player.getName();
            var10000.info("Администратор " + var10001 + " удалил фракцию: " + factionName);
            player.sendMessage("§aФракция " + factionName + " успешно удалена!");
         }
      }
   }

   private void handleInviteCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f invite <игрок>");
      } else {
         Faction faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции!");
         } else if (!faction.canManageRanks(player.getUniqueId())) {
            player.sendMessage("§cУ вас нет прав для приглашения игроков!");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               player.sendMessage("§cИгрок не найден или не в сети!");
            } else if (faction.isMember(target.getUniqueId())) {
               player.sendMessage("§cИгрок уже состоит в вашей фракции!");
            } else if (this.getPlayerFaction(target) != null) {
               player.sendMessage("§cИгрок уже состоит в другой фракции!");
            } else {
               FactionInvite invite = new FactionInvite(faction.getName(), player.getUniqueId());
               this.pendingInvites.put(target.getUniqueId(), invite);
               player.sendMessage("§aПриглашение отправлено игроку " + target.getName());
               target.sendMessage("§aВы получили приглашение вступить во фракцию " + faction.getName());
               target.sendMessage("§aУ вас есть §e60 секунд§a чтобы принять приглашение");
               target.sendMessage("§aВведите §e/f accept§a для принятия или §e/f deny§a для отказа");
               Bukkit.getScheduler().runTaskLater(this, () -> {
                  if (this.pendingInvites.containsKey(target.getUniqueId()) && ((FactionInvite)this.pendingInvites.get(target.getUniqueId())).equals(invite)) {
                     this.pendingInvites.remove(target.getUniqueId());
                     target.sendMessage("§cПриглашение во фракцию " + faction.getName() + " истекло!");
                     Player inviter = Bukkit.getPlayer(invite.getInviterUUID());
                     if (inviter != null && inviter.isOnline()) {
                        inviter.sendMessage("§cПриглашение для " + target.getName() + " истекло!");
                     }
                  }

               }, 1200L);
            }
         }
      }
   }

   private void handleAcceptCommand(Player player) {
      FactionInvite invite = (FactionInvite)this.pendingInvites.get(player.getUniqueId());
      if (invite == null) {
         player.sendMessage("§cУ вас нет активных приглашений!");
      } else if (invite.isExpired()) {
         this.pendingInvites.remove(player.getUniqueId());
         player.sendMessage("§cПриглашение истекло!");
      } else {
         Faction faction = (Faction)this.factions.get(invite.getFactionName().toLowerCase());
         if (faction == null) {
            player.sendMessage("§cФракция больше не существует!");
            this.pendingInvites.remove(player.getUniqueId());
         } else {
            this.pendingInvites.remove(player.getUniqueId());
            faction.addMember(player.getUniqueId());
            this.playerFactions.put(player.getUniqueId(), faction.getName().toLowerCase());
            this.saveFactionsAsync();
            faction.broadcast("§a" + player.getName() + " присоединился к фракции!");
            player.sendMessage("§aВы вступили во фракцию " + faction.getName());
         }
      }
   }

   private void handleDenyCommand(Player player) {
      FactionInvite invite = (FactionInvite)this.pendingInvites.get(player.getUniqueId());
      if (invite == null) {
         player.sendMessage("§cУ вас нет активных приглашений!");
      } else {
         this.pendingInvites.remove(player.getUniqueId());
         player.sendMessage("§cВы отклонили приглашение во фракцию " + invite.getFactionName());
         Player inviter = Bukkit.getPlayer(invite.getInviterUUID());
         if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage("§c" + player.getName() + " отклонил приглашение во фракцию");
         }

      }
   }

   private void handleLeaveCommand(Player player) {
      Faction faction = this.getPlayerFaction(player);
      if (faction == null) {
         player.sendMessage("§cВы не состоите в фракции!");
      } else if (faction.isLeader(player.getUniqueId())) {
         player.sendMessage("§cЛидер не может покинуть фракцию! Используйте /f delete или передайте лидерство");
      } else {
         faction.removeMember(player.getUniqueId());
         this.playerFactions.remove(player.getUniqueId());
         this.saveFactionsAsync();
         faction.broadcast("§c" + player.getName() + " покинул фракцию!");
         player.sendMessage("§cВы покинули фракцию " + faction.getName());
      }
   }

   private void handleKickCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f kick <игрок>");
      } else {
         Faction faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции!");
         } else if (!faction.canManageRanks(player.getUniqueId())) {
            player.sendMessage("§cУ вас нет прав для исключения игроков!");
         } else {
            String targetName = args[1];
            UUID targetUUID = null;
            String correctName = null;
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null) {
               targetUUID = targetPlayer.getUniqueId();
               correctName = targetPlayer.getName();
            } else {
               Iterator var8 = faction.getMemberUUIDs().iterator();

               while(var8.hasNext()) {
                  UUID memberUUID = (UUID)var8.next();
                  OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
                  if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName)) {
                     targetUUID = memberUUID;
                     correctName = offlinePlayer.getName();
                     break;
                  }
               }
            }

            if (targetUUID != null && correctName != null) {
               if (faction.isLeader(targetUUID)) {
                  player.sendMessage("§cВы не можете исключить лидера фракции!");
               } else if (faction.isDeputy(targetUUID) && !faction.isLeader(player.getUniqueId())) {
                  player.sendMessage("§cТолько лидер может исключить заместителя!");
               } else {
                  faction.removeMember(targetUUID);
                  this.playerFactions.remove(targetUUID);
                  this.saveFactionsAsync();
                  faction.broadcast("§c" + correctName + " был исключен из фракции!");
                  Player kickedPlayer = Bukkit.getPlayer(targetUUID);
                  if (kickedPlayer != null && kickedPlayer.isOnline()) {
                     kickedPlayer.sendMessage("§cВы были исключены из фракции " + faction.getName());
                  }

               }
            } else {
               player.sendMessage("§cИгрок не найден в вашей фракции!");
            }
         }
      }
   }

   private void handleDemoteCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f demote <игрок>");
      } else {
         Faction faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции!");
         } else if (!faction.canManageRanks(player.getUniqueId())) {
            player.sendMessage("§cУ вас нет прав для понижения участников!");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               player.sendMessage("§cИгрок не найден или не в сети!");
            } else if (!faction.isMember(target.getUniqueId())) {
               player.sendMessage("§cИгрок не является участником вашей фракции!");
            } else {
               try {
                  FactionRank oldRank = faction.getPlayerRank(target.getUniqueId());
                  faction.demotePlayerOneRank(target.getUniqueId());
                  FactionRank newRank = faction.getPlayerRank(target.getUniqueId());
                  String var10001 = target.getName();
                  faction.broadcast("§c" + var10001 + " понижен с ранга " + oldRank.getName() + " до " + newRank.getName() + "!");
                  this.saveFactionsAsync();
               } catch (IllegalArgumentException | IllegalStateException var7) {
                  player.sendMessage("§c" + var7.getMessage());
               }

            }
         }
      }
   }

   private void handlePromoteCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f promote <игрок>");
      } else {
         Faction faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции!");
         } else if (!faction.canManageRanks(player.getUniqueId())) {
            player.sendMessage("§cУ вас нет прав для повышения участников!");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               player.sendMessage("§cИгрок не найден или не в сети!");
            } else if (!faction.isMember(target.getUniqueId())) {
               player.sendMessage("§cИгрок не является участником вашей фракции!");
            } else {
               try {
                  FactionRank oldRank = faction.getPlayerRank(target.getUniqueId());
                  faction.promotePlayerOneRank(target.getUniqueId());
                  FactionRank newRank = faction.getPlayerRank(target.getUniqueId());
                  String var10001 = target.getName();
                  faction.broadcast("§e" + var10001 + " повышен с ранга " + oldRank.getName() + " до " + newRank.getName() + "!");
                  this.saveFactionsAsync();
               } catch (IllegalStateException var7) {
                  player.sendMessage("§c" + var7.getMessage());
               }

            }
         }
      }
   }

   private void handleSetLeaderCommand(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage("§cИспользование: /f setleader <фракция> <новый лидер>");
      } else {
         String factionName = args[1].toLowerCase();
         String newLeaderName = args[2];
         Faction faction = (Faction)this.factions.get(factionName);
         if (faction == null) {
            player.sendMessage("§cФракция не найдена!");
         } else {
            Player newLeader = Bukkit.getPlayer(newLeaderName);
            if (newLeader == null) {
               player.sendMessage("§cИгрок не найден или не в сети!");
            } else {
               Faction newLeaderFaction = this.getPlayerFaction(newLeader);
               if (newLeaderFaction != null && !newLeaderFaction.equals(faction)) {
                  player.sendMessage("§cИгрок уже состоит в другой фракции!");
               } else {
                  UUID oldLeaderUUID = faction.getLeader();
                  faction.setNewLeader(newLeader.getUniqueId());
                  this.playerFactions.put(newLeader.getUniqueId(), factionName);
                  this.saveFactionsAsync();
                  faction.broadcast("§e" + newLeader.getName() + " стал новым лидером фракции!");
                  Player oldLeader = Bukkit.getPlayer(oldLeaderUUID);
                  if (oldLeader != null && oldLeader.isOnline()) {
                     oldLeader.sendMessage("§cВы больше не являетесь лидером фракции " + faction.getName());
                  }

               }
            }
         }
      }
   }

   private void handleInfoCommand(Player player, String[] args) {
      Faction faction;
      if (args.length > 1) {
         faction = (Faction)this.factions.get(args[1].toLowerCase());
         if (faction == null) {
            player.sendMessage("§cФракция не найдена!");
            return;
         }
      } else {
         faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции! Укажите название фракции для просмотра информации.");
            return;
         }
      }

      player.sendMessage("§6=== Информация о фракции " + faction.getName() + " ===");
      player.sendMessage("§7Лидер: §f" + Bukkit.getOfflinePlayer(faction.getLeader()).getName());
      List<String> deputies = (List)faction.getDeputies().stream().map((uuid) -> {
         return Bukkit.getOfflinePlayer(uuid).getName();
      }).collect(Collectors.toList());
      int var10001 = deputies.size();
      player.sendMessage("§7Заместители (" + var10001 + "): §f" + String.join(", ", deputies));
      List<String> members = (List)faction.getMemberUUIDs().stream().filter((uuid) -> {
         return !faction.isLeader(uuid) && !faction.isDeputy(uuid);
      }).map((uuid) -> {
         return Bukkit.getOfflinePlayer(uuid).getName();
      }).collect(Collectors.toList());
      var10001 = members.size();
      player.sendMessage("§7Участники (" + var10001 + "): §f" + String.join(", ", members));
      player.sendMessage("§7Всего участников: §f" + faction.getMemberUUIDs().size());
   }

   private void handleListCommand(Player player) {
      if (this.factions.isEmpty()) {
         player.sendMessage("§cНа сервере нет активных фракций!");
      } else {
         player.sendMessage("§6=== Список фракций ===");
         Iterator var2 = this.factions.values().iterator();

         while(var2.hasNext()) {
            Faction faction = (Faction)var2.next();
            String leaderName = Bukkit.getOfflinePlayer(faction.getLeader()).getName();
            player.sendMessage("§e" + faction.getName() + " §7- Лидер: §f" + leaderName + " §7- Участников: §f" + faction.getMemberUUIDs().size());
         }

      }
   }

   private void handleMenuCommand(Player player) {
      Faction faction = this.getPlayerFaction(player);
      if (faction == null) {
         player.sendMessage("§cВы не состоите в фракции!");
      } else {
         this.factionMenu.openMainMenu(player, faction);
      }
   }

   private void handleRankCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cИспользование: /f rank <create|delete|set|list>");
      } else {
         Faction faction = this.getPlayerFaction(player);
         if (faction == null) {
            player.sendMessage("§cВы не состоите в фракции!");
         } else if (!faction.canManageRanks(player.getUniqueId())) {
            player.sendMessage("§cУ вас нет прав для управления рангами!");
         } else {
            String var4 = args[1].toLowerCase();
            byte var5 = -1;
            switch(var4.hashCode()) {
            case -1352294148:
               if (var4.equals("create")) {
                  var5 = 0;
               }
               break;
            case -1335458389:
               if (var4.equals("delete")) {
                  var5 = 1;
               }
               break;
            case 113762:
               if (var4.equals("set")) {
                  var5 = 2;
               }
               break;
            case 3322014:
               if (var4.equals("list")) {
                  var5 = 3;
               }
            }

            switch(var5) {
            case 0:
               this.handleRankCreateCommand(player, faction, args);
               break;
            case 1:
               this.handleRankDeleteCommand(player, faction, args);
               break;
            case 2:
               this.handleRankSetCommand(player, faction, args);
               break;
            case 3:
               this.handleRankListCommand(player, faction);
               break;
            default:
               player.sendMessage("§cНеизвестная подкоманда! Используйте: create, delete, set или list");
            }

         }
      }
   }

   private void handleRankCreateCommand(Player player, Faction faction, String[] args) {
      if (args.length < 3) {
         player.sendMessage("§cИспользование: /f rank create <название>");
      } else {
         String rankName = args[2];

         try {
            FactionRank newRank = faction.createRank(rankName);
            this.saveFactionsAsync();
            player.sendMessage("§aРанг " + newRank.getName() + " успешно создан!");
         } catch (IllegalStateException var6) {
            player.sendMessage("§c" + var6.getMessage());
         }

      }
   }

   private void handleRankDeleteCommand(Player player, Faction faction, String[] args) {
      if (args.length < 3) {
         player.sendMessage("§cИспользование: /f rank delete <название>");
      } else {
         String rankName = args[2];
         FactionRank rank = faction.getRankByName(rankName);
         if (rank == null) {
            player.sendMessage("§cРанг не найден!");
         } else if (rank.isSystemRank()) {
            player.sendMessage("§cНельзя удалить системный ранг!");
         } else {
            if (faction.deleteRankByName(rankName)) {
               this.saveFactionsAsync();
               faction.broadcast("§cРанг " + rankName + " был удален!");
            } else {
               player.sendMessage("§cНе удалось удалить ранг!");
            }

         }
      }
   }

   private void handleRankSetCommand(Player player, Faction faction, String[] args) {
      if (args.length < 4) {
         player.sendMessage("§cИспользование: /f rank set <игрок> <ранг>");
      } else {
         Player target = Bukkit.getPlayer(args[2]);
         if (target == null) {
            player.sendMessage("§cИгрок не найден или не в сети!");
         } else if (!faction.isMember(target.getUniqueId())) {
            player.sendMessage("§cИгрок не является участником вашей фракции!");
         } else {
            String rankName = args[3];
            FactionRank rank = faction.getRankByName(rankName);
            if (rank == null) {
               player.sendMessage("§cРанг не найден!");
            } else if (rank.isSystemRank()) {
               player.sendMessage("§cНельзя назначить системный ранг через эту команду!");
            } else {
               try {
                  faction.setPlayerRank(target.getUniqueId(), rank);
                  this.saveFactionsAsync();
                  String var10001 = target.getName();
                  faction.broadcast("§e" + var10001 + " получил ранг " + rank.getName() + "!");
               } catch (IllegalArgumentException var8) {
                  player.sendMessage("§c" + var8.getMessage());
               }

            }
         }
      }
   }

   private void handleRankListCommand(Player player, Faction faction) {
      player.sendMessage("§6=== Ранги фракции " + faction.getName() + " ===");
      List<FactionRank> ranks = new ArrayList(faction.getRanks());
      ranks.sort((r1, r2) -> {
         return Integer.compare(r2.getId(), r1.getId());
      });
      Iterator var4 = ranks.iterator();

      while(var4.hasNext()) {
         FactionRank rank = (FactionRank)var4.next();
         String prefix = rank.isSystemRank() ? "§c" : "§a";
         player.sendMessage(prefix + rank.getName() + " §7(ID: " + rank.getId() + ")");
      }

   }

   private void sendHelpMessage(Player player) {
      player.sendMessage("§6=== Помощь по командам фракций ===");
      if (player.hasPermission("factions.admin")) {
         player.sendMessage("§e/f create <название> <лидер> §7- Создать фракцию");
         player.sendMessage("§e/f delete <название> §7- Удалить фракцию");
         player.sendMessage("§e/f setleader <фракция> <игрок> §7- Назначить лидера");
      }

      player.sendMessage("§e/f invite <игрок> §7- Пригласить игрока");
      player.sendMessage("§e/f accept §7- Принять приглашение");
      player.sendMessage("§e/f deny §7- Отклонить приглашение");
      player.sendMessage("§e/f leave §7- Покинуть фракцию");
      player.sendMessage("§e/f kick <игрок> §7- Исключить игрока");
      player.sendMessage("§e/f promote <игрок> §7- Повысить игрока");
      player.sendMessage("§e/f demote <игрок> §7- Понизить игрока");
      player.sendMessage("§e/f info [фракция] §7- Информация о фракции");
      player.sendMessage("§e/f list §7- Список фракций");
      player.sendMessage("§e/f menu §7- Открыть меню фракции");
      player.sendMessage("§e/f rank create <название> §7- Создать ранг");
      player.sendMessage("§e/f rank delete <название> §7- Удалить ранг");
      player.sendMessage("§e/f rank set <игрок> <ранг> §7- Установить ранг");
      player.sendMessage("§e/f rank list §7- Список рангов");
   }

   public Faction getPlayerFaction(Player player) {
      String factionName = (String)this.playerFactions.get(player.getUniqueId());
      return factionName != null ? (Faction)this.factions.get(factionName) : null;
   }

   public Map<String, Faction> getFactions() {
      return this.factions;
   }

   public FactionMenu getFactionMenu() {
      return this.factionMenu;
   }

   public Map<UUID, String> getPlayerFactions() {
      return this.playerFactions;
   }
}
    
