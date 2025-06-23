package com.example.factionsplugin;

import java.util.Date;
import java.util.UUID;

public class FactionMember {
   private final UUID uuid;
   private FactionRank rank;
   private final Date joinDate;

   public FactionMember(UUID uuid, FactionRank rank) {
      this.uuid = uuid;
      this.rank = rank;
      this.joinDate = new Date();
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public FactionRank getRank() {
      return this.rank;
   }

   public void setRank(FactionRank rank) {
      this.rank = rank;
   }

   public Date getJoinDate() {
      return this.joinDate;
   }
}
    
