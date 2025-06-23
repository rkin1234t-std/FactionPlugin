    package com.example.factionsplugin;

import java.util.UUID;

public class FactionInvite {
   private final String factionName;
   private final UUID inviterUUID;
   private final long expirationTime;
   private static final long INVITE_DURATION = 60000L;

   public FactionInvite(String factionName, UUID inviterUUID) {
      this.factionName = factionName;
      this.inviterUUID = inviterUUID;
      this.expirationTime = System.currentTimeMillis() + 60000L;
   }

   public String getFactionName() {
      return this.factionName;
   }

   public UUID getInviterUUID() {
      return this.inviterUUID;
   }

   public boolean isExpired() {
      return System.currentTimeMillis() > this.expirationTime;
   }

   public long getTimeLeft() {
      return Math.max(0L, this.expirationTime - System.currentTimeMillis());
   }
}
    
