package com.project.Blog_Management_System.Deserializers;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class CustomHtmlSanitizationDeserializer extends StdDeserializer<String> {

    private static final PolicyFactory CUSTOM_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.STYLES)
            .and(Sanitizers.TABLES);

    public CustomHtmlSanitizationDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();
        return (value == null) ? null : CUSTOM_POLICY.sanitize(value).trim();
    }

}
