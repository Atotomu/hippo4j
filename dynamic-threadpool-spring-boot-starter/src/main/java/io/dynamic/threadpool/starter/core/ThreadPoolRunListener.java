package io.dynamic.threadpool.starter.core;

import io.dynamic.threadpool.starter.common.CommonThreadPool;
import io.dynamic.threadpool.starter.config.ApplicationContextHolder;
import io.dynamic.threadpool.starter.model.PoolParameterInfo;
import io.dynamic.threadpool.starter.toolkit.BlockingQueueUtil;
import io.dynamic.threadpool.starter.toolkit.HttpClientUtil;
import io.dynamic.threadpool.starter.wrap.DynamicThreadPoolWrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池启动监听
 *
 * @author chen.ma
 * @date 2021/6/20 16:34
 */
public class ThreadPoolRunListener implements ApplicationRunner {

    @Autowired
    private HttpClientUtil httpClientUtil;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, DynamicThreadPoolWrap> executorMap =
                ApplicationContextHolder.getBeansOfType(DynamicThreadPoolWrap.class);

        executorMap.forEach((key, val) -> {

            Map<String, Object> queryStrMap = new HashMap(16);
            queryStrMap.put("tdId", val.getTpId());
            queryStrMap.put("itemId", val.getItemId());
            queryStrMap.put("tenant", val.getTenant());

            PoolParameterInfo ppi = httpClientUtil.restApiGet(buildUrl(), queryStrMap, PoolParameterInfo.class);
            if (ppi != null) {
                // 使用相关参数创建线程池
                TimeUnit unit = TimeUnit.SECONDS;
                BlockingQueue workQueue = BlockingQueueUtil.createBlockingQueue(ppi.getQueueType(), ppi.getCapacity());
                ThreadPoolExecutor resultTpe = new ThreadPoolExecutor(ppi.getCoreSize(), ppi.getMaxSize(), ppi.getKeepAliveTime(), unit, workQueue);
                val.setPool(resultTpe);
            } else if (val.getPool() == null) {
                val.setPool(CommonThreadPool.getInstance(val.getTpId()));
            }

            GlobalThreadPoolManage.register(buildOnlyId(val), val);
        });
    }

    private String buildUrl() {
        return "http://127.0.0.1:6691/v1/cs/configs";
    }

    private String buildOnlyId(DynamicThreadPoolWrap poolWrap) {
        return poolWrap.getTenant() + "_" + poolWrap.getItemId() + "_" + poolWrap.getTpId();
    }

}