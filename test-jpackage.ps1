$ErrorActionPreference = "Stop"
cd C:\Users\dimab\lost-database
mvn clean package -DskipTests
rm -Recurse -Force dist-test -ErrorAction SilentlyContinue
mkdir dist-test
cp target\lost-game-database-1.0-SNAPSHOT.jar dist-test\lost.jar
jpackage --input dist-test --main-jar lost.jar --main-class com.lost.database.app.Launcher --name "LOST-Test" --type app-image --dest out-test
