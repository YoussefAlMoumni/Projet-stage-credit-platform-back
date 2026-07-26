package com.talan.creditplatform.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import tools.jackson.core.JsonGenerator;

public class TrimmedMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public TrimmedMessageJsonProvider() {
        setFieldName("message");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message != null) {
            message = message.replaceAll("[\\r\\n]+$", "");
        }
        JsonWritingUtils.writeStringField(generator, getFieldName(), message != null ? message : "");
    }
}
