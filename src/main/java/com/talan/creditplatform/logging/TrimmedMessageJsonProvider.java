package com.talan.creditplatform.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

public class TrimmedMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public TrimmedMessageJsonProvider() {
        setFieldName("message");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws JacksonException {
        // tools.jackson (Jackson 3.x) requires two-step field writing.
        // writeStringField(String, String) does not exist in this API version.
        String message = event.getFormattedMessage();
        if (message != null) {
            // Strip trailing newlines produced by some loggers/frameworks
            message = message.replaceAll("[\\r\\n]+$", "");
        }
        generator.writeName(getFieldName());
        generator.writeString(message != null ? message : "");
    }
}
