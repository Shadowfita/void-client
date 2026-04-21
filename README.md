# Void Client

Deobfuscated 634 2010-12-14 client

# Build

Build the client with Gradle, no install needed:

```gradle
./gradlew shadowJar
```

You will find the built `void-client.jar` in `/client/build/libs`.

If you're having issues with OpenGL/DirectX make sure you're using or running with a 32bit jre.

```bash
java -jar void-client.jar -d32
```
