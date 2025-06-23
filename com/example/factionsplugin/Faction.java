    package com.example.factionsplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Faction {
   private final String name;
   private UUID leader;
   private Set<UUID> deputies;
   private Set<UUID> members;
   private List<FactionRank> ranks;
   private Map<UUID, FactionRank> playerRanks;

   public Faction(String name, UUID leader) {
      this.name = name;
      this.leader = leader;
      this.deputies = new HashSet();
      this.members = new HashSet();
      this.ranks = new ArrayList();
      this.playerRanks = new HashMap();
      this.members.add(leader);
      this.initializeDefaultRanks();
   }

   public String getName() {
      return this.name;
   }

   public UUID getLeader() {
      return this.leader;
   }

   public void setNewLeader(UUID newLeader) {
      if (!this.members.contains(newLeader)) {
         this.members.add(newLeader);
      }

      if (this.deputies.contains(newLeader)) {
         this.deputies.remove(newLeader);
      }

      if (this.leader != null) {
         this.members.add(this.leader);
      }

      this.leader = newLeader;
   }

   public Set<UUID> getDeputies() {
      return new HashSet(this.deputies);
   }

   public void addDeputy(UUID playerId) {
      if (!this.members.contains(playerId)) {
         this.members.add(playerId);
      }

      this.deputies.add(playerId);
   }

   public void removeDeputy(UUID playerId) {
      this.deputies.remove(playerId);
   }

   public boolean isDeputy(UUID playerId) {
      return this.deputies.contains(playerId);
   }

   public Set<UUID> getMemberUUIDs() {
      Set<UUID> allMembers = new HashSet(this.members);
      allMembers.add(this.leader);
      return allMembers;
   }

   public void addMember(UUID playerId) {
      this.members.add(playerId);
   }

   public void removeMember(UUID playerId) {
      if (this.isLeader(playerId)) {
         throw new IllegalArgumentException("Нельзя удалить лидера фракции");
      } else {
         this.members.remove(playerId);
         this.deputies.remove(playerId);
         this.playerRanks.remove(playerId);
      }
   }

   public boolean isMember(UUID playerId) {
      return this.members.contains(playerId) || this.isLeader(playerId);
   }

   public boolean isLeader(UUID playerId) {
      return this.leader.equals(playerId);
   }

   public void broadcast(String message) {
      Iterator var2 = this.getMemberUUIDs().iterator();

      while(var2.hasNext()) {
         UUID memberId = (UUID)var2.next();
         Player player = Bukkit.getPlayer(memberId);
         if (player != null && player.isOnline()) {
            player.sendMessage(message);
         }
      }

   }

   public boolean canManageRanks(UUID playerId) {
      return this.isLeader(playerId) || this.isDeputy(playerId);
   }

   public List<FactionRank> getRanks() {
      return new ArrayList(this.ranks);
   }

   public void addRank(FactionRank rank) {
      this.ranks.add(rank);
   }

   public void removeRank(FactionRank rank) {
      if (rank.isSystemRank()) {
         throw new IllegalStateException("Нельзя удалить системный ранг!");
      } else {
         List<FactionRank> ranksToUpdate = this.ranks.stream().filter((r) -> {
            return !r.isSystemRank() && r.getId() > rank.getId();
         }).sorted((r1, r2) -> {
            return Integer.compare(r1.getId(), r2.getId());
         }).toList();
         Iterator var3 = ranksToUpdate.iterator();

         while(var3.hasNext()) {
            FactionRank rankToUpdate = (FactionRank)var3.next();
            rankToUpdate.setId(rankToUpdate.getId() - 1);
         }

         FactionRank lowerRank = this.getPreviousRank(rank);
         Iterator var7 = this.getMembersWithRank(rank).iterator();

         while(var7.hasNext()) {
            UUID playerId = (UUID)var7.next();
            this.setPlayerRank(playerId, lowerRank);
         }

         this.ranks.remove(rank);
      }
   }

   public FactionRank getRankByName(String name) {
      return (FactionRank)this.ranks.stream().filter((rank) -> {
         return rank.getName().equalsIgnoreCase(name);
      }).findFirst().orElse((Object)null);
   }

   public boolean deleteRankByName(String name) {
      FactionRank rank = this.getRankByName(name);
      if (rank != null && !rank.isSystemRank()) {
         this.removeRank(rank);
         return true;
      } else {
         return false;
      }
   }

   public FactionRank createRank(String name) {
      if (this.ranks.size() >= 10) {
         throw new IllegalStateException("Достигнут максимальный лимит рангов (10)");
      } else if (this.getRankByName(name) != null) {
         throw new IllegalStateException("Ранг с таким названием уже существует!");
      } else {
         int maxCustomId = this.ranks.stream().filter((r) -> {
            return !r.isSystemRank() && r.getId() > 1 && r.getId() < 98;
         }).mapToInt(FactionRank::getId).max().orElse(1);
         int newId = maxCustomId == 1 ? 2 : maxCustomId + 1;
         if (newId >= 98) {
            throw new IllegalStateException("Достигнут максимальный уровень рангов!");
         } else {
            FactionRank newRank = new FactionRank(newId, name, false);
            this.ranks.add(newRank);
            return newRank;
         }
      }
   }

   public void initializeDefaultRanks() {
      if (this.ranks.isEmpty()) {
         this.ranks.add(new FactionRank(1, "Участник", true));
         this.ranks.add(new FactionRank(98, "Заместитель", true));
         this.ranks.add(new FactionRank(99, "Лидер", true));
      }

   }

   public void setPlayerRank(UUID playerId, FactionRank rank) {
      if (!this.isMember(playerId)) {
         throw new IllegalArgumentException("Игрок должен быть участником фракции");
      } else if (!this.ranks.contains(rank)) {
         throw new IllegalArgumentException("Указанный ранг не существует в этой фракции");
      } else {
         this.playerRanks.put(playerId, rank);
      }
   }

   public FactionRank getPlayerRank(UUID playerId) {
      if (this.isLeader(playerId)) {
         return this.getRankByName("Лидер");
      } else {
         return this.isDeputy(playerId) ? this.getRankByName("Заместитель") : (FactionRank)this.playerRanks.getOrDefault(playerId, this.getRankByName("Участник"));
      }
   }

   public List<UUID> getMembersWithRank(FactionRank rank) {
      List<UUID> result = new ArrayList();
      Iterator var3 = this.playerRanks.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<UUID, FactionRank> entry = (Entry)var3.next();
         if (((FactionRank)entry.getValue()).equals(rank)) {
            result.add((UUID)entry.getKey());
         }
      }

      return result;
   }

   public List<FactionRank> getSortedRanks() {
      List<FactionRank> sortedRanks = new ArrayList(this.ranks);
      sortedRanks.sort((r1, r2) -> {
         return Integer.compare(r1.getId(), r2.getId());
      });
      return sortedRanks;
   }

   public FactionRank getNextRank(FactionRank currentRank) {
      List<FactionRank> sortedRanks = this.getSortedRanks();
      int currentIndex = sortedRanks.indexOf(currentRank);
      return currentIndex < sortedRanks.size() - 1 ? (FactionRank)sortedRanks.get(currentIndex + 1) : null;
   }

   public FactionRank getPreviousRank(FactionRank currentRank) {
      List<FactionRank> sortedRanks = this.getSortedRanks();
      int currentIndex = sortedRanks.indexOf(currentRank);
      return currentIndex > 0 ? (FactionRank)sortedRanks.get(currentIndex - 1) : null;
   }

   public void promotePlayerOneRank(UUID playerId) {
      FactionRank currentRank = this.getPlayerRank(playerId);
      FactionRank nextRank = this.getNextRank(currentRank);
      if (nextRank == null) {
         throw new IllegalStateException("Этот игрок уже имеет максимальный ранг!");
      } else {
         if (nextRank.getName().equalsIgnoreCase("Заместитель")) {
            this.addDeputy(playerId);
         }

         this.setPlayerRank(playerId, nextRank);
      }
   }

   public void demotePlayerOneRank(UUID playerId) {
      if (this.isLeader(playerId)) {
         throw new IllegalArgumentException("Нельзя понизить лидера фракции!");
      } else {
         FactionRank currentRank = this.getPlayerRank(playerId);
         FactionRank previousRank = this.getPreviousRank(currentRank);
         if (previousRank == null) {
            throw new IllegalStateException("Этот игрок уже имеет минимальный ранг!");
         } else {
            if (currentRank.getName().equalsIgnoreCase("Заместитель")) {
               this.removeDeputy(playerId);
            }

            this.setPlayerRank(playerId, previousRank);
         }
      }
   }

   public FactionMember getMember(UUID playerId) {
      return !this.isMember(playerId) ? null : new FactionMember(playerId, this.getPlayerRank(playerId));
   }
}
    
