package com.sample.app.ai.screeningchat.model;

import java.util.List;
import java.util.Map;

public record GridUpdate(
        Map<String, Object> filterModel,
        List<SortModel> sortModel,
        List<String> hiddenColIds,
        List<ColumnSizingModel> columnSizingModel
) {

    public record SortModel(
            String colId,
            String sort
    ) {}

    public record ColumnSizingModel(
            String colId,
            Integer width
    ) {}
}