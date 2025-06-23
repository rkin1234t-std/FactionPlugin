    package com.example.factionsplugin;

import java.util.Objects;

public class FactionRank {
   private int id;
   private final String name;
   private final boolean systemRank;

   public FactionRank(int id, String name, boolean systemRank) {
      this.id = id;
      this.name = name;
      this.systemRank = systemRank;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public boolean isSystemRank() {
      return this.systemRank;
   }

   public void setId(int id) {
      this.id = id;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FactionRank that = (FactionRank)o;
         return this.id == that.id && this.name.equals(that.name);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.id, this.name});
   }
}
    
