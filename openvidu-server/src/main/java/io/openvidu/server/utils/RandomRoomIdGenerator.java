package io.openvidu.server.utils;

import io.openvidu.server.annotation.DistributedLock;
import io.openvidu.server.common.dao.RandomIdPoolMapper;
import io.openvidu.server.common.pojo.RandomIdPool;
import io.openvidu.server.common.pojo.RandomIdPoolExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RandomRoomIdGenerator {

    private static final Object lock = new Object();

    @Autowired
    private RandomIdPoolMapper randomIdPoolMapper;


    public static final Integer MAX_ROOM_ID = 999999999;

    @PostConstruct
    public void init() {
        int count = checkRemaining();
        if (count > 0) {
            return;
        }

        Set<Integer> set = new HashSet<>();

        Random r = new Random();
        while (set.size() < 1000) {
            int roomId = r.nextInt(MAX_ROOM_ID);
            if (set.add(roomId)) {
                returnRoomId(roomId);
            }
        }
    }


    @Scheduled(cron = "0 10 0/1 * * ?")
    @DistributedLock(key = "generatorSchedule")
    public void generatorSchedule() {
        if (checkRemaining() > 5000) {
            return;
        }

        List<RandomIdPool> randomIdPools = randomIdPoolMapper.selectByExample(null);
        Set<Long> set = randomIdPools.stream().map(RandomIdPool::getRoomId).collect(Collectors.toSet());
        Random r = new Random();
        while (set.size() < 10000) {
            long roomId = r.nextInt(MAX_ROOM_ID);
            if (set.add(roomId)) {
                returnRoomId(roomId);
            }
        }
    }


    public int checkRemaining() {
        return (int) randomIdPoolMapper.countByExample(new RandomIdPoolExample());
    }


    @Transactional
    public String offerRoomId() {
        synchronized (lock) {
            RandomIdPool firstId = randomIdPoolMapper.getFirstId();
            randomIdPoolMapper.deleteByPrimaryKey(firstId.getId());
            return String.format("%09d", firstId.getRoomId());
        }
    }

    public void returnRoomId(String roomId) {
        returnRoomId(Long.parseLong(roomId));
    }

    public void returnRoomId(long roomId) {
        RandomIdPool randomIdPool = new RandomIdPool();
        randomIdPool.setRoomId(roomId);
        randomIdPoolMapper.insert(randomIdPool);
    }
}
