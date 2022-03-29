package com.epam.catgenome.entity.vcf;

import com.epam.catgenome.entity.heatmap.HeatmapDataType;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class VcfFieldValues {
    private String fieldName;
    private List<Pair<String, String>> values;
    private Set<?> cellValues;
    private HeatmapDataType cellValueType;
    private Double maxCellValue;
    private Double minCellValue;
}
