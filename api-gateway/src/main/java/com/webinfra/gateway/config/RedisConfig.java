package com.webinfra.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisConfig {

    private static final String TOKEN_BUCKET_LUA_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])

            local last_refill = tonumber(redis.call('HGET', key, 'last_refill'))
            local tokens = tonumber(redis.call('HGET', key, 'tokens'))

            if not last_refill or not tokens then
                tokens = capacity
                last_refill = now
            else
                local delta = math.max(0, now - last_refill)
                local tokens_to_add = delta * refill_rate
                tokens = math.min(capacity, tokens + tokens_to_add)
                last_refill = now
            end

            local allowed = 0
            if tokens >= requested then
                tokens = tokens - requested
                allowed = 1
            end

            redis.call('HSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(last_refill))
            local ttl = math.ceil(capacity / refill_rate) + 60
            redis.call('EXPIRE', key, ttl)

            return { allowed, tostring(tokens) }
            """;

    @Bean
    public RedisScript<List> rateLimiterScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(TOKEN_BUCKET_LUA_SCRIPT);
        script.setResultType(List.class);
        return script;
    }
}
