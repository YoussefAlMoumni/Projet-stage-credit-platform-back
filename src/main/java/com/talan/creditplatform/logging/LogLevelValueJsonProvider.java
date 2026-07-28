package com.talan.creditplatform.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

public class LogLevelValueJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public LogLevelValueJsonProvider() {
        setFieldName("level_value");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws JacksonException {
        // tools.jackson (Jackson 3.x) requires two-step field writing.
        // writeNumberField(String, int) does not exist in this API version.
        generator.writeName(getFieldName());
        generator.writeNumber(mapLevel(event.getLevel()));
    }

    private int mapLevel(Level level) {
        if (level == null) {
            return 0;
        }
        return switch (level.toInt()) {
            case Level.ERROR_INT -> 40000;
            case Level.WARN_INT  -> 30000;
            case Level.INFO_INT  -> 20000;
            case Level.DEBUG_INT -> 10000;
            case Level.TRACE_INT -> 5000;
            default -> level.toInt();
        };
    }
}
