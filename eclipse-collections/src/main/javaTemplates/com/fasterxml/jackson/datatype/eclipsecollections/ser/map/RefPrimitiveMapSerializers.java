package com.fasterxml.jackson.datatype.eclipsecollections.ser.map;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ValueSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.fasterxml.jackson.datatype.primitive_collections_base.ser.map.RefPrimitiveMapSerializer;

import org.eclipse.collections.api.map.primitive.*;

/**
 * @author yawkat
 */
@SuppressWarnings({ "Duplicates", "NewClassNamingConvention" })
public final class RefPrimitiveMapSerializers
{
    private RefPrimitiveMapSerializers() {
    }

    /* with char|boolean|byte|short|int|float|long|double value */

    public static final class Char<K> extends RefPrimitiveMapSerializer<ObjectCharMap<K>, K>
    {
        public Char(JavaType type, BeanProperty property, ValueSerializer<Object> keySerializer) {
            super(type, property, keySerializer);
        }

        @Override
        protected RefPrimitiveMapSerializer<ObjectCharMap<K>, K> withResolved(
                BeanProperty property, ValueSerializer<Object> keySerializer
        ) {
            return new Char<>(_type, property, keySerializer);
        }

        @Override
        protected void serializeEntries(ObjectCharMap<K> value, JsonGenerator g, SerializerProvider serializers)
        {
            value.forEachKeyValue((k, v) -> {
                _serializeKey(k, g, serializers);
                /* if !(char|boolean value) //
                g.writeNumber(v);
                /* elif char value */
                g.writeString(new char[]{v}, 0, 1);
                /* elif boolean value //
                g.writeBoolean(v);
                // endif */
            });
        }

        @Override
        public boolean isEmpty(SerializerProvider provider, ObjectCharMap<K> value) {
            return value.isEmpty();
        }
    }

    /* endwith */
}
