package com.chen;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日志记录器
 */
public class RedisLogCounter {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private Calendar calendar = Calendar.getInstance();
    private Jedis jedis;
    private Pipeline pipeline;

    RedisLogCounter(Jedis jedis){
        this.jedis = jedis;
        this.pipeline = jedis.pipelined();
    }

    /**
     *  记录最新日志
     * @param name      模块名称
     * @param message   消息
     * @param servrity  日志级别
     */
    public void log_recent(String name,String message,LogLevel servrity){
        String destination = "recent:" + name + ":" + servrity;
        message = sdf.format(new Date()) + " " +message;
        //添加到队列中
        jedis.lpush(destination.getBytes(),message.getBytes());
        //对日志列表进行修剪，让它只包含最新的100条消息
        jedis.ltrim(destination.getBytes(),0,99);
    }

    /**
     *
     * @param name
     * @param message
     * @param logLevel
     * @param timeout
     */
    public void log_common(String name,String message,LogLevel logLevel,int timeout){
        String destination = "common:" + name + ":" + logLevel;
        String start_key = destination + ":start";
        int nowTime = calendar.get(Calendar.SECOND);
        int endTime = nowTime + timeout;
        //对记录当前小时数的键进行监视，确保轮换操作可以正确的执行
        jedis.watch(start_key);
        while (calendar.get(Calendar.SECOND) < endTime){

            try{
                //获取当前小时
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String existing = jedis.get(start_key);
                //开启事务
                Transaction multi = jedis.multi();
                if(existing != null && existing != "" && Integer.parseInt(existing) < hour){
                    multi.rename(destination,destination+":last");
                    multi.rename(start_key,destination+":pstart");
                    multi.set(start_key,hour+"");
                }else{
                    multi.set(start_key,hour+"");
                }
                multi.zincrby(destination.getBytes(),1,message.getBytes());
                multi.exec();
                log_recent(name,message,logLevel);

                return;
            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }


    }
}
