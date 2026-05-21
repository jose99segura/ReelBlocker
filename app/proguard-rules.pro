# Reglas ProGuard / R8 para ReelBlocker release build.

# El servicio de accesibilidad lo referencia el sistema por nombre via
# AndroidManifest.xml. Aunque el nombre completo no se ofusca (porque
# las clases registradas en el manifest se preservan automaticamente),
# mantenemos explicito por claridad.
-keep class app.reelblocker.BlockerService { *; }
-keep class app.reelblocker.MainActivity { *; }

# Stats es un object Kotlin con metodos publicos llamados desde el
# servicio y la UI. R8 mantiene la API publica de objects, pero
# por seguridad ante futuras refactorizaciones:
-keepclasseswithmembernames class app.reelblocker.Stats { *; }

# Compose y Material 3 traen sus propias reglas via consumer rules
# en sus AAR. Nada que anadir aqui.

# Annotations en runtime (algun @Composable depende de ellas).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses

# Kotlin metadata (necesario para reflexion de Kotlin si alguna lib la usa).
-keep class kotlin.Metadata { *; }
