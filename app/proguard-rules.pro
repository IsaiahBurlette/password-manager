# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Keep entity classes used by Room reflection
-keep class com.securevault.app.data.** { *; }
