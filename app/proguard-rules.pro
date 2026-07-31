# Keep service account key fields (Gson deserialization)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room entities
-keep class com.aji.wa_gateway.db.entity.** { *; }

# Keep Google API client classes
-keep class com.google.api.services.sheets.v4.** { *; }
-keep class com.google.auth.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }

# Keep WebSocket message classes
-keep class * implements io.ktor.websocket.Frame { *; }
