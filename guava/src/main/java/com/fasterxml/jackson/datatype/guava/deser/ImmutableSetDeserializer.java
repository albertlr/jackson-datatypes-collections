package com.fasterxml.jackson.datatype.guava.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ValueDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableSet;

public class ImmutableSetDeserializer extends GuavaImmutableCollectionDeserializer<ImmutableSet<Object>>
{
    public ImmutableSetDeserializer(JavaType selfType,
            ValueDeserializer<?> deser, TypeDeserializer typeDeser,
            NullValueProvider nuller, Boolean unwrapSingle) {
        super(selfType, deser, typeDeser, nuller, unwrapSingle);
    }

    @Override
    public ImmutableSetDeserializer withResolved(ValueDeserializer<?> valueDeser, TypeDeserializer typeDeser,
            NullValueProvider nuller, Boolean unwrapSingle) {
        return new ImmutableSetDeserializer(_containerType,
                valueDeser, typeDeser, nuller, unwrapSingle);
    }
    
    @Override
    protected Builder<Object> createBuilder() {
        return ImmutableSet.builder();
    }

    @Override
    protected ImmutableSet<Object> _createEmpty(DeserializationContext ctxt) {
        return ImmutableSet.of();
    }

    @Override
    protected ImmutableSet<Object> _createWithSingleElement(DeserializationContext ctxt, Object value) {
        return ImmutableSet.of(value);
    }
}
