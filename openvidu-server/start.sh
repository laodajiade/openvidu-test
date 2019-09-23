echo "--- begin start mtgconcenter ---"

# decide to update config or not(just reserved)

chmod +x openvidu-server-2.11.0.jar
ulimit -c unlimited
ulimit -n 102400

java -server -XX:MaxDirectMemorySize=100m -Xms500m -Xmx500m -XX:NewRatio=3 -XX:+HeapDumpOnOutOfMemoryError -jar openvidu-server-2.11.0.jar >/dev/null 2>&1&

