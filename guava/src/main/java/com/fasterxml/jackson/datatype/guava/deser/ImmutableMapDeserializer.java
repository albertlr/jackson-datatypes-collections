package com.fasterxml.jackson.datatype.guava.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ValueDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import com.google.common.collect.ImmutableMap;

public class ImmutableMapDeserializer
    extends GuavaImmutableMapDeserializer<ImmutableMap<Object, Object>>
{
    public ImmutableMapDeserializer(JavaType type, KeyDeserializer keyDeser,
            ValueDeserializer<?> valueDeser, TypeDeserializer valueTypeDeser,
            NullValueProvider nuller)
    {
        super(type, keyDeser, valueDeser, valueTypeDeser, nuller);
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return ImmutableMap.of();
    }

    @Override
    public ImmutableMapDeserializer withResolved(KeyDeserializer keyDeser,
            ValueDeserializer<?> valueDeser, TypeDeserializer valueTypeDeser,
            NullValueProvider nuller) {
        return new ImmutableMapDeserializer(_containerType, keyDeser,
                valueDeser, valueTypeDeser, nuller);
    }

    @Override
    protected ImmutableMap.Builder<Object, Object> createBuilder() {
        return ImmutableMap.builder();
    }
}
