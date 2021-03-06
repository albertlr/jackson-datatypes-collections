package com.fasterxml.jackson.datatype.guava.deser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import com.google.common.base.Optional;

public class GuavaOptionalDeserializer
    extends ReferenceTypeDeserializer<Optional<?>>
{
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public GuavaOptionalDeserializer(JavaType fullType, ValueInstantiator inst,
            TypeDeserializer typeDeser, ValueDeserializer<?> deser)
    {
        super(fullType, inst, typeDeser, deser);
    }
    
    /*
    /**********************************************************
    /* Abstract method implementations
    /**********************************************************
     */

    @Override
    public GuavaOptionalDeserializer withResolved(TypeDeserializer typeDeser, ValueDeserializer<?> valueDeser) {
        return new GuavaOptionalDeserializer(_fullType, _valueInstantiator,
                typeDeser, valueDeser);
    }

    @Override
    public Optional<?> getNullValue(DeserializationContext ctxt) {
        // 07-May-2019, tatu: [databind#2303]: make sure to delegate
        return Optional.fromNullable(_valueDeserializer.getNullValue(ctxt));
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return getEmptyValue(ctxt);
    }

    @Override
    public Optional<?> referenceValue(Object contents) {
        return Optional.fromNullable(contents);
    }

    @Override
    public Object getReferenced(Optional<?> reference) {
        return reference.get();
    }

    @Override
    public Optional<?> updateReference(Optional<?> reference, Object contents) {
        return Optional.fromNullable(contents);
    }

    // Default ought to be fine:
//    public Boolean supportsUpdate(DeserializationConfig config) { }
}
