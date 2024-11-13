package com.solar.redis.lock;

import com.solar.redis.cache.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author hushaoge
 * @date 2024/11/13 12:48
 * @description
 */
@Slf4j
@Service
public class LockService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisService redisService;

    /**
     * 使用setnx
     *
     * @param userId
     * @param productId
     * @return
     */
    public String createOrderNx(String userId, String productId) {
        String key = "orderNX:"+userId;
        try {
            // 尝试加锁
            Boolean isLocked = redisService.lock(key, 60);
            if (isLocked) {
                // 业务逻辑
                TimeUnit.SECONDS.sleep(30);
                return "Lock acquired";
            } else {
                return "Lock not acquired";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Lock not acquired due to InterruptedException";
        } finally {
            redisService.unlock(key);
        }
    }

    /**
     * 使用redisson
     * @param userId
     * @param productId
     * @return
     */
    public String createOrder(String userId, String productId) {
        RLock lock = redissonClient.getLock("order:" + userId);
        try {
            // 尝试加锁，最多等待1秒，锁定后最多持有锁10秒
            boolean isLocked = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (isLocked) {
                // 业务逻辑
                TimeUnit.SECONDS.sleep(1);
                return "Lock acquired";
            } else {
                return "Lock not acquired";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Lock not acquired due to InterruptedException";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 复杂的创建过程，可能耗时很长，需要使用看门狗
     * @param userId
     * @param productId
     */
    public String createOrderComplex(String userId, String productId) {
        RLock lock = redissonClient.getLock("order:" + userId);
        try {
            // 尝试加锁，不设值过期时间
            // 需要采用无参的方法启用看门狗续命
            boolean isLocked = lock.tryLock();
            if (isLocked) {
                TimeUnit.MINUTES.sleep(1);
                log.info("业务处理完成");
                // 业务逻辑
                return "Lock acquired";
            } else {
                return "Lock not acquired";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Lock not acquired due to InterruptedException";
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
