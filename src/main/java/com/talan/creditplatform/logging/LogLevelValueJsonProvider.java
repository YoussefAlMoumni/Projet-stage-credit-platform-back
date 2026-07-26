package com.talan.creditplatform.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import tools.jackson.core.JsonGenerator;

public class LogLevelValueJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public LogLevelValueJsonProvider() {
        setFieldName("level_value");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        int levelValue = mapLevel(event.getLevel());
        JsonWritingUtils.writeNumberField(generator, getFieldName(), levelValue);
    }

    private int mapLevel(Level level) {
        if (level == null) {
            return 0;
        }
        return switch (level.toInt()) {
            case Level.ERROR_INT -> 40000;
            case Level.WARN_INT -> 30000;
            case Level.INFO_INT -> 20000;
            case Level.DEBUG_INT -> 10000;
            case Level.TRACE_INT -> 5000;
            default -> level.toInt();
        };
    }
}
