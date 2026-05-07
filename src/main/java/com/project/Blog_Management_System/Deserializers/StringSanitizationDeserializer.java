package com.project.Blog_Management_System.Deserializers;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class StringSanitizationDeserializer extends StdDeserializer<String> {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder().toFactory();

    public StringSanitizationDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();
        return (value == null) ? null : POLICY.sanitize(value).trim();
    }
}
