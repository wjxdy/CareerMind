package com.careermind.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * LLM 调用相关的基础设施 Bean：
 *  - llmRestTemplate：基于 Apache HttpClient5 的池化客户端（替换默认 SimpleClientHttpRequestFactory）
 *  - llmExecutor：业务线程池（Round 1/4 并行 + 排队消费），容量可控、带背压
 *  - kimiSemaphore：对上游 Moonshot Kimi 单 Key 的全局并发闸门，防止集中触发 RPM 限流
 */
@Configuration
@EnableAsync
@EnableScheduling
public class LLMHttpConfig {

    @Value("${llm.http.max-total:50}")
    private int maxTotal;

    @Value("${llm.http.max-per-route:20}")
    private int maxPerRoute;

    @Value("${llm.http.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${llm.http.response-timeout-ms:120000}")
    private int responseTimeoutMs;

    @Value("${llm.executor.core-size:4}")
    private int executorCore;

    @Value("${llm.executor.max-size:8}")
    private int executorMax;

    @Value("${llm.executor.queue-capacity:20}")
    private int executorQueue;

    @Value("${llm.kimi.max-concurrency:3}")
    private int kimiConcurrency;

    @Bean
    public RestTemplate llmRestTemplate() {
        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(maxTotal)
                .setMaxConnPerRoute(maxPerRoute)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofSeconds(30))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setBufferRequestBody(false); // 流式必须关闭 body 缓冲
        return new RestTemplate(factory);
    }

    /**
     * LLM 业务线程池：
     *  - core=4 / max=8 / queue=20
     *  - 拒绝策略 CallerRunsPolicy：超限时退到提交线程执行，形成背压，避免雪崩
     *  - 单咨询 Round 1/4 并行 5 个 Agent + Round 2/3 串行，4 个并发咨询基本占满
     */
    @Bean("llmExecutor")
    public ThreadPoolTaskExecutor llmExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(executorCore);
        ex.setMaxPoolSize(executorMax);
        ex.setQueueCapacity(executorQueue);
        ex.setThreadNamePrefix("llm-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(60);
        ex.initialize();
        return ex;
    }

    /**
     * 对 Moonshot Kimi 的全局并发闸门。
     * 免费/默认账户单 Key 通常有 3 RPM 级别限制，超出即 429。
     * 在应用内排队 > 在 Moonshot 侧被拒。
     */
    @Bean("kimiSemaphore")
    public Semaphore kimiSemaphore() {
        return new Semaphore(kimiConcurrency, true); // fair=true，避免饥饿
    }
}
