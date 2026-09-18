package com.sample.app.ai.service;

import com.sample.app.ai.model.ScreeningChatRequest;
import com.sample.app.ai.model.ScreeningChatResponse;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("google")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
public class ScreeningChatServiceIntegrationTest {

    @Autowired
    ScreeningChatService screeningChatService;

    @Test
    void chat_against_google_genai_returns_structured_response() {

        ScreeningChatRequest request = createSampleRequest();

        // Clear any prior history and invoke the chat
        screeningChatService.clearHistory("pm-001");
        ScreeningChatResponse response = screeningChatService.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.portfolioManagerId()).isEqualTo("pm-001");
        assertThat(response.assistantMessage()).isNotBlank();
        // History should at least contain the assistant response that was just added or be non-null
        assertThat(response.history()).isNotNull();

        // Grid update may be present depending on LLM output; if present it should be a valid object
        // (no strict assertions on fields to avoid flakiness against the live model)
        // But we assert that either gridUpdate is null or a non-null object (sanity)
//        assertThat(response.gridUpdate() == null || response.gridUpdate() != null).isTrue();
    }

    private static ScreeningChatRequest createSampleRequest() {

//        2026-09-18T13:24:29.174+01:00  INFO 7739 --- [sample-app-spring-ai] [nio-8081-exec-3] c.s.app.ai.service.ScreeningChatService  : Processing screening chat request
//        ScreeningChatRequest[
//        portfolioManagerId=pm-001,
//        message=Find securities similar to the selected securities.,
//        gridState={
//        version=36.1.0,
//        sideBar={visible=false, openToolPanel=null, toolPanels={}},
//        columnPinning={leftColIds=[], rightColIds=[holdingsActions]},
//        columnSizing={columnSizingModel=[{colId=ag-Grid-SelectionColumn, width=50}, {colId=securityId, width=200},
//        {colId=securityName, width=210},
//        {colId=issuer, width=200},
//        {colId=assetClass, width=200},
//        {colId=country, width=200},
//        {colId=currency, width=120},
//        {colId=rating, width=200},
//        {colId=esgRiskLevel, width=200},
//        {colId=riskScore, width=200},
//        {colId=marginRate, width=200},
//        {colId=sanctionsFlag, width=200},
//        {colId=screeningStatus, width=200},
//        {colId=lastReviewDate, width=200}, {colId=holdingsActions, width=150}, {colId=aiBetterMargin, width=220}]},
//        columnOrder={
//        orderedColIds=[ag-Grid-SelectionColumn, securityId, securityName, issuer, assetClass, country, currency, rating, esgRiskLevel, r
//        iskScore, marginRate, sanctionsFlag, screeningStatus, lastReviewDate, holdingsActions, aiBetterMargin]},
//        rowGroupExpansion={expandedRowGroupIds=[], collapsedRowGroupIds=[]}, rowSelection={selectAll=false, toggledNodes=[0]},
//        pagination={page=0, pageSize=100}}, structuredSchema={type=object, required=[filter, sort, columnVisibility, columnSizing],
//        additionalProperties=false, properties={filter={type=[object, null], required=[filterModel], additionalProperties=false,
//        properties={filterModel={type=object, required=[securityId, securityName, issuer, assetClass, country, currency, rating,
//        esgRiskLevel, riskScore, marginRate, sanctionsFlag, screeningStatus, lastReviewDate], additionalProperties=false,
//        properties={securityId={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false,
//        properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]},
//        operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]},
//        conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false,
//        properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type,
//        enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value},
//        filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}},
//        securityName={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false,
//        properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, issuer={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, assetClass={type=[object, null], required=[filterType, values], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for set filters, enum=[set]}, values={type=array, description=Array of values to include in the filter, items={type=string, description=Filter values}}}}, country={type=[object, null], required=[filterType, values], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for set filters, enum=[set]}, values={type=array, description=Array of values to include in the filter, items={type=string, description=Filter values}}}}, currency={type=[object, null], required=[filterType, values], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for set filters, enum=[set]}, values={type=array, description=Array of values to include in the filter, items={type=string, description=Filter values}}}}, rating={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, esgRiskLevel={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, riskScore={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for number filters with multiple conditions, enum=[number]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for number filters, enum=[number]}, type={type=string, description=Number filter operation type, enum=[equals, notEqual, greaterThan, greaterThanOrEqual, lessThan, lessThanOrEqual, inRange, blank, notBlank]}, filter={type=[number, null], description=Primary filter value}, filterTo={type=[number, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, marginRate={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for number filters with multiple conditions, enum=[number]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for number filters, enum=[number]}, type={type=string, description=Number filter operation type, enum=[equals, notEqual, greaterThan, greaterThanOrEqual, lessThan, lessThanOrEqual, inRange, blank, notBlank]}, filter={type=[number, null], description=Primary filter value}, filterTo={type=[number, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, sanctionsFlag={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, screeningStatus={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters with multiple conditions, enum=[text]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, filter, filterTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for text filters, enum=[text]}, type={type=string, description=Text filter operation type, enum=[contains, notContains, equals, notEqual, startsWith, endsWith, blank, notBlank]}, filter={type=[string, null], description=Primary filter value}, filterTo={type=[string, null], description=Secondary filter value for range operations}}}, minItems=2, maxItems=2}}}, lastReviewDate={type=[object, null], required=[filterType, operator, conditions], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for date filters with multiple conditions, enum=[date]}, operator={type=string, description=Logical operator to combine multiple filter conditions. Must be included even with a single filter to adhere to the API., enum=[AND, OR]}, conditions={type=array, description=Array of filter conditions to be combined, items={type=object, required=[filterType, type, dateFrom, dateTo], additionalProperties=false, properties={filterType={type=string, description=Filter type identifier for date filters, enum=[date]}, type={type=string, description=Date filter operation type, enum=[equals, notEqual, lessThan, greaterThan, inRange, blank, notBlank]}, dateFrom={type=[string, null], description=Primary date filter value in YYYY-MM-DD HH:mm:ss format, pattern=^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$}, dateTo={type=[string, null], description=Secondary date filter value for range operations in YYYY-MM-DD HH:mm:ss format, pattern=^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$}}}, minItems=2, maxItems=2}}}}}}}, sort={type=[object, null], description=Sort configuration for the grid, required=[sortModel], additionalProperties=false, properties={sortModel={type=array, description=Array of sort configurations, items={type=object, required=[colId, sort, type], additionalProperties=false, properties={colId={type=string, description=Column ID that supports sorting, enum=[securityId, securityName, issuer, assetClass, country, currency, rating, esgRiskLevel, riskScore, marginRate, sanctionsFlag, screeningStatus, lastReviewDate]}, sort={type=string, description=Sort direction: ascending or descending, enum=[asc, desc]}, type={type=string, description=Sort type: default or absolute values, enum=[default, absolute]}}}}}}, columnVisibility={type=[object, null], description=Column visibility configuration for the grid, required=[hiddenColIds], additionalProperties=false, properties={hiddenColIds={type=array, description=Array of column IDs to hide, items={$ref=#/$defs/allColumnIds}}}}, columnSizing={type=[object, null], description=Column sizing configuration for the grid, required=[columnSizingModel], additionalProperties=false, properties={columnSizingModel={type=array, description=Array of column sizing configurations, items={anyOf=[{type=object, required=[colId, width], additionalProperties=false, properties={colId={$ref=#/$defs/resizableColumnId}, width={type=number, description=Fixed width in pixels, minimum=20}}}, {type=object, required=[colId, flex], additionalProperties=false, properties={colId={$ref=#/$defs/resizableColumnId}, flex={type=number, description=Flex sizing ratio, minimum=0}}}]}}}}}, $defs={allColumnIds={type=string, description=ag-Grid-SelectionColumn
//                securityId
//                securityName
//                issuer
//                assetClass
//                country
//                currency
//                rating
//                esgRiskLevel
//                riskScore
//                marginRate
//                sanctionsFlag
//                screeningStatus
//                lastReviewDate
//                holdingsActions
//                aiBetterMargin,
//                enum=[ag-Grid-SelectionColumn, securityId, securityName, issuer, assetClass, country, currency, rating, esgRiskLevel, riskScore, marginRate, sanctionsFlag, screeningStatus, lastReviewDate, holdingsActions, aiBetterMargin]}, resizableColumnId={type=string, description=Column ID that supports resizing, enum=[securityId, securityName, issuer, assetClass, country, currency, rating, esgRiskLevel, riskScore, marginRate, sanctionsFlag, screeningStatus, lastReviewDate, holdingsActions, aiBetterMargin]}}}, selectedRows=[{id=sec-0001, securityId=[SEC-1001], securityName=[Strategic Bond Bond 2036], issuer=[Pacific Tech Inc], assetClass=[Bond], country=[FR], currency=[GBP], rating=[AA-], esgRiskLevel=[Low], riskScore=[33], sanctionsFlag=[No], screeningStatus=[Escalated], lastReviewDate=[2026-04-15T00:00:00Z], marginRate=2.45, _version_=1876338292978352000, _root_=sec-0001}]]

        // Build example request (trimmed/representative of the provided example)
        var selectedRow = Map.<String, Object>ofEntries(
                Map.entry("id", "sec-0001"),
                Map.entry("securityId", List.of("SEC-1001")),
                Map.entry("securityName", List.of("Strategic Bond Bond 2036")),
                Map.entry("issuer", List.of("Pacific Tech Inc")),
                Map.entry("assetClass", List.of("Bond")),
                Map.entry("country", List.of("FR")),
                Map.entry("currency", List.of("GBP")),
                Map.entry("rating", List.of("AA-")),
                Map.entry("esgRiskLevel", List.of("Low")),
                Map.entry("riskScore", List.of("33")),
                Map.entry("sanctionsFlag", List.of("No")),
                Map.entry("screeningStatus", List.of("Escalated")),
                Map.entry("lastReviewDate", List.of("2026-04-15T00:00:00Z")),
                Map.entry("marginRate", 2.45)
        );

        var gridState = Map.<String, Object>ofEntries(
                Map.entry("rowSelection", Map.ofEntries(
                        Map.entry("selectAll", false),
                        Map.entry("toggledNodes", List.of(1)) // the selected rows
                ))
        );

        var structuredSchema = Map.<String, Object>ofEntries(
                Map.entry("type", "object"),
                Map.entry("required", List.of("filter", "sort", "columnVisibility", "columnSizing")),
                Map.entry("properties", Map.of(
                        "filter", Map.of("type", List.of("object", "null"), "required", List.of("filterModel")),
                        "sort", Map.of("type", List.of("object", "null"), "required", List.of("sortModel")),
                        "columnVisibility", Map.of("type", List.of("object", "null"), "required", List.of("hiddenColIds")),
                        "columnSizing", Map.of("type", List.of("object", "null"), "required", List.of("columnSizingModel"))
                ))
        );

        return new ScreeningChatRequest(
                "pm-001",
                "Find securities similar to the selected securities.",
                gridState,
                structuredSchema,
                List.of(selectedRow)
        );
    }
}
