package com.project.Blog_Management_System.Deserializers;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class BasicHtmlSanitizationDeserializer extends StdDeserializer<String> {

    private static final PolicyFactory BASIC_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS);

    public BasicHtmlSanitizationDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();
        return (value == null) ? null : BASIC_POLICY.sanitize(value).trim();
    }
}
