package com.example.demo.controller;

import com.example.demo.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
@RequiredArgsConstructor
@RestController
@Slf4j
public class reidsTestController {
    private static String API_ITEM_NUMLIMIT_LOCK_KEY = "API_ITEM_NUMLIMIT_LOCK_KEY:";
    @Autowired
    private RedisLockService redisLockService;
    public static int count = 0;
    //请求总数
    public static int clientTotal = 5000;
    //同时并发的线程数
    public static int threadTotal = 200;
    @GetMapping("/redis/lock/{itemId}")
    public void redisTest(@PathVariable String itemId) throws InterruptedException{
        ExecutorService executorService = Executors.newCachedThreadPool();
        //定义信号量  用于并发控制
        final Semaphore semaphore = new Semaphore(threadTotal);
        //定义 计数器闭锁
        final CountDownLatch countDownLatch = new CountDownLatch(clientTotal);
        String lockName = API_ITEM_NUMLIMIT_LOCK_KEY + itemId;
        for(int i=0 ; i<clientTotal ; i++){
            executorService.execute(()->{
                try {
                    //semaphore 控制并发数  表示执行add 方法 只有 threadTotal能同时执行
                    semaphore.acquire();
                    //报名的业务是报名保存数据花费2S，执行完毕然后获取报名表的数据进行判断看数据是否超出
                    //这里测试的话先睡几秒 然后得到（数据库操作后的数据）然后进行判断
                    redisLockService.lock(lockName);
                    if(count<10){
                        Thread.sleep(1000L);  //让线程睡两秒相当于与数据库交换时间  这样让代码还没执行完其他线程就进来了
                        count++;
                        System.out.println(count);
                    }
                    redisLockService.unlock(lockName);
                    semaphore.release();
                } catch (InterruptedException e) {
                    log.error("exception:" ,e);
                }

                //每个线程执行后 调用一下countkdown方法
                countDownLatch.countDown();
            });
        }

        //阻塞主线程    当clientTotal 个请求执行完后  打印   count 值
        countDownLatch.await();
        executorService.shutdown();
    }

    @GetMapping("/redis/lock2")
    public String lock(@RequestParam("key") String key) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                redisLockService.lock(key);
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                System.out.println(sdf.format(new Date()));
                redisLockService.unlock(key);
            }
            ).start();
        }
        return "OK";
    }
}
