package com.epam.catgenome.entity.bed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class FileExtension {

    private final List<String> extensions;
    private final List<BedColumnMapping> mapping;

    public FileExtension(@JsonProperty("mapping") final List<BedColumnMapping> mapping,
                         @JsonProperty("extension") final List<String> extensions) {
        this.mapping = mapping;
        this.extensions = extensions;
    }

    synchronized public static Object getInstance() {
        return null;
    }

    public List<BedColumnMapping> getMapping() {
        return mapping;
    }

    @Getter
    static public class BedColumnMapping {

        private final int index;
        private final String column;
        private final BedColumnCaster cast;

        public BedColumnMapping(@JsonProperty("index") final int index,
                                @JsonProperty("column") final String column,
                                @JsonProperty("cast") final BedColumnCaster cast) {
            this.index = index;
            this.column = column;
            this.cast = cast;
        }

        public Object casted(String value) {
            return cast.cast.apply(value);
        }

    }

    public enum BedColumnCaster {
        STRING(v -> v),
        INT(Integer::parseInt),
        FLOAT(Float::parseFloat);

        private final Function<String, Object> cast;

        BedColumnCaster(final Function<String, Object> cast) {
            this.cast = cast;
        }
    }
}
