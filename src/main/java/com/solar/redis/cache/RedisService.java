package com.solar.redis.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * @author hushaoge
 * @date 2024/11/12 8:26
 * @description
 */
@Component
public class RedisService {

    private final static long timeout = 30;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存设置
     * @param key     键
     * @param value   值
     */
    public void setKeyValue(String key, Object value) {
        // 设置随机数据，防止缓存雪崩：同一时间key都失效，大量请求直接查询数据库
        Duration expire = Duration.ofSeconds(timeout).plus(Duration.ofSeconds((int)(Math.random() * 100)));
        redisTemplate.opsForValue().set(key, value, expire);
    }

    public Object getValueByKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 锁
     * 类似于setnx
     * @param key          键
     * @param seconds      过期时间
     * @return
     */
    public Boolean lock(String key, int seconds) {
        // 这里需要设置一个唯一性的标识
        String id = getHostName() + Thread.currentThread().getId();
        return redisTemplate.opsForValue().setIfAbsent(key, id, Duration.ofSeconds(seconds));
    }

    /**
     * 释放锁-删除
     * @param key          键
     * @return
     */
    public Boolean unlock(String key) {
        String id = (String)getValueByKey(key);
        if(Objects.isNull(id)){
            return Boolean.TRUE;
        }
        // 获取当前的标志，防止释放了其他线程的锁
        String currentId = getHostName() + Thread.currentThread().getId();
        if (id.equals(currentId)) {
            return delete(key);
        } else {
            return Boolean.FALSE;
        }
    }

    private String getHostName () {
        // 获取本地主机的InetAddress实例
        String hostName = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            // 获取主机名
            hostName = localHost.getHostName();
        } catch (UnknownHostException e) {
            hostName = "Unknown";
        }
        return hostName;
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     *
     * @param luaScript lua脚本
     * @param keys      键
     * @param args      参数
     * @return
     */
    public Long execScript(String luaScript, List<String> keys, Object... args) {
        return redisTemplate.execute(RedisScript.of(luaScript, Long.class), keys, args);
    }
}
