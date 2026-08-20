package com.example.usermanagement.common.db;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlLogInterceptor implements Interceptor {
    private static final Logger log = LogManager.getLogger(SqlLogInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameter = args.length > 1 ? args[1] : null;
        BoundSql boundSql = statement.getBoundSql(parameter);
        long started = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long duration = System.currentTimeMillis() - started;
            log.info("SQL_EXEC txId={} threadId={} statementId={} durationMs={} sql=\"{}\" params={}",
                    Objects.toString(ThreadContext.get("txId"), "-"),
                    Thread.currentThread().getId(),
                    statement.getId(),
                    duration,
                    normalizeSql(boundSql.getSql()),
                    safeParameter(parameter));
        }
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }

    private String safeParameter(Object parameter) {
        if (parameter == null) {
            return "{}";
        }
        String value = String.valueOf(parameter);
        return value.length() > 800 ? value.substring(0, 800) + "...(truncated)" : value;
    }
}
