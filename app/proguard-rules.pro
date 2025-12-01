# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Veuillez ajouter cette règle à vos règles existantes pour supprimer les avertissements liés aux annotations de Google.
-dontwarn com.google.errorprone.annotations.MustBeClosed

# RÈGLE AJOUTÉE POUR CONSERVER LES CLASSES DE TEST ET ÉVITER LES ERREURS DE MINIFICATION
# (Ceci est essentiel car R8 touche les dépendances de test comme 'androidx.test.platform.tracing')
-keep class androidx.test.platform.** { *; }

# Ignore les warnings sur les classes de test (androidx.test, espresso, etc.)
-dontwarn androidx.test.**
-dontwarn com.google.errorprone.annotations.**