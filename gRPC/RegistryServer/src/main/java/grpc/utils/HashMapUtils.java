package grpc.utils;

import shared.General.ServerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapUtils {
    public static ServerInfo findLowestOrZeroKey(ConcurrentHashMap<ServerInfo, Integer> map) {
        ServerInfo resultKey = null;
        int lowestValue = Integer.MAX_VALUE;

        // List to store entries with value 0
        List<ServerInfo> zeroValueKeys = new ArrayList<>();

        for (Map.Entry<ServerInfo, Integer> entry : map.entrySet()) {
            int value = entry.getValue();

            if (value == 0) {
                zeroValueKeys.add(entry.getKey());
            }

            if (value < lowestValue) {
                lowestValue = value;
                resultKey = entry.getKey();
            }
        }

        // If there are any keys with value 0, return a random one
        if (!zeroValueKeys.isEmpty()) {
            Random random = new Random();
            return zeroValueKeys.get(random.nextInt(zeroValueKeys.size()));
        }

        // Otherwise, return the key with the lowest value
        return resultKey;
    }
}
