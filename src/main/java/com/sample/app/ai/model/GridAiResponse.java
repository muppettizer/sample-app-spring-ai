package com.sample.app.ai.model;

import java.util.List;
import java.util.Map;

public record GridAiResponse(
        FilterState filter,
        SortState sort,
        ColumnVisibilityState columnVisibility,
        ColumnSizingState columnSizing
) {
    public record FilterState(
            Map<String, Object> filterModel
    ) {}

    public record SortState(
            List<SortModel> sortModel
    ) {}

    public record SortModel(
            String colId,
            String sort
    ) {}

    public record ColumnVisibilityState(
            List<String> hiddenColIds
    ) {}

    public record ColumnSizingState(
            List<ColumnSizingModel> columnSizingModel
    ) {}

    public record ColumnSizingModel(
            String colId,
            Integer width
    ) {}
}