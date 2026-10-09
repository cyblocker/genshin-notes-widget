# Proguard rules for Genshin Daily Notes AppWidget
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep data models for Gson serialization
-keep class com.genshin.dailynote.data.model.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Glance
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
