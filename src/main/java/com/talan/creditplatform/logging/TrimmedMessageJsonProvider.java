package com.talan.creditplatform.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;

import java.io.IOException;

public class TrimmedMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public TrimmedMessageJsonProvider() {
        setFieldName("message");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String message = event.getFormattedMessage();
        if (message != null) {
            // Strip trailing newlines produced by some loggers/frameworks
            message = message.replaceAll("[\\r\\n]+$", "");
        }
        generator.writeStringField(getFieldName(), message != null ? message : "");
    }
}
