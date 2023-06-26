package com.chen;

import redis.clients.jedis.Jedis;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost",6379);
        jedis.auth("cm021035");

        RedisLogCounter redisLogCounter = new RedisLogCounter(jedis);

        String[] names = new String[]{"chenmin","liqiang","花儿","陈敏杰"};
        Random random = new Random();
//        new Thread(()->{
//            for(int i = 0;i < 1000;i++){
//                int j = random.nextInt(4);
//                String message = names[j];
//                redisLogCounter.log_recent("login",message + " login " + i,LogLevel.INFO);
//            }
//        }).start();

        new Thread(()->{
            for(int i = 0;i < 100;i++){
                int j = random.nextInt(4);
                String name = names[j];
                redisLogCounter.log_common("login",name + " login",LogLevel.INFO,5);
            }
        }).start();

    }
}
