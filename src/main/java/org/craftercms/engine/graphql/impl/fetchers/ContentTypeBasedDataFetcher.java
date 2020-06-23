/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.engine.graphql.impl.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.Selection;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.elasticsearch.ElasticsearchWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StopWatch;

import static org.craftercms.engine.graphql.SchemaUtils.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Implementation of {@link DataFetcher} that queries Elasticsearch to retrieve content based on a content type.
 * @author joseross
 * @since 3.1
 */
@SuppressWarnings("unchecked, rawtypes")
public class ContentTypeBasedDataFetcher extends RequestAwareDataFetcher<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeBasedDataFetcher.class);

    private static final String QUERY_FIELD_NAME_CONTENT_TYPE = getOriginalName(FIELD_NAME_CONTENT_TYPE);

    // Lucene regexes always match the entire string, no need to specify ^ or $
    public static final String CONTENT_TYPE_REGEX_PAGE = "/?page/.*";
    public static final String CONTENT_TYPE_REGEX_COMPONENT = "/?component/.*";
    public static final String COMPONENT_INCLUDE_REGEX = ".*\\.item\\.component";

    /**
     * The default value for the 'limit' argument
     */
    protected int defaultLimit;

    /**
     * The default value for the 'sortBy' argument
     */
    protected String defaultSortField;

    /**
     * The default value for the 'sortOrder' argument
     */
    protected String defaultSortOrder;

    /**
     * The instance of {@link ElasticsearchWrapper}
     */
    protected ElasticsearchWrapper elasticsearch;

    @Required
    public void setDefaultLimit(final int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    @Required
    public void setDefaultSortField(final String defaultSortField) {
        this.defaultSortField = defaultSortField;
    }

    @Required
    public void setDefaultSortOrder(final String defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    @Required
    public void setElasticsearch(final ElasticsearchWrapper elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object doGet(final DataFetchingEnvironment env) {
        Field field = env.getMergedField().getSingleField();
        String fieldName = field.getName();

        // Get arguments for pagination & sorting
        int offset = Optional.ofNullable(env.<Integer>getArgument(ARG_NAME_OFFSET)).orElse(0);
        int limit = Optional.ofNullable(env.<Integer>getArgument(ARG_NAME_LIMIT)).orElse(defaultLimit);
        String sortBy = Optional.ofNullable(env.<String>getArgument(ARG_NAME_SORT_BY)).orElse(defaultSortField);
        String sortOrder = Optional.ofNullable(env.<String>getArgument(ARG_NAME_SORT_ORDER)).orElse(defaultSortOrder);

        List<String> queryFieldIncludes = new LinkedList<>();
        // Add content-type to includes, we might need it for a GraphQL TypeResolver
        queryFieldIncludes.add(QUERY_FIELD_NAME_CONTENT_TYPE);

        List<Map<String, Object>> items = new LinkedList<>();
        Map<String, Object> result = new HashMap<>(2);
        result.put(FIELD_NAME_ITEMS, items);

        // Setup the ES query
        SearchSourceBuilder source = new SearchSourceBuilder();
        BoolQueryBuilder query = boolQuery();
        source
            .query(query)
            .from(offset)
            .size(limit)
            .sort(sortBy, SortOrder.fromString(sortOrder));

        StopWatch watch = new StopWatch(field.getName() + " - " + field.getAlias());

        watch.start("build filters");

        // Filter by the content-type
        switch (fieldName) {
            case FIELD_NAME_CONTENT_ITEMS:
                query.filter(existsQuery(QUERY_FIELD_NAME_CONTENT_TYPE));
                break;
            case FIELD_NAME_PAGES:
                query.filter(regexpQuery(QUERY_FIELD_NAME_CONTENT_TYPE, CONTENT_TYPE_REGEX_PAGE));
                break;
            case FIELD_NAME_COMPONENTS:
                query.filter(regexpQuery(QUERY_FIELD_NAME_CONTENT_TYPE, CONTENT_TYPE_REGEX_COMPONENT));
                break;
            default:
                // Get the content-type name from the field name
                query.filter(termQuery(QUERY_FIELD_NAME_CONTENT_TYPE, getOriginalName(fieldName)));
                break;
        }

        // Check the selected fields to build the ES query
        Optional<Field> itemsField = field.getSelectionSet().getSelections()
                            .stream()
                            .map(f -> (Field) f)
                            .filter(f -> f.getName().equals(FIELD_NAME_ITEMS))
                            .findFirst();
        if (itemsField.isPresent()) {
            List<Selection> selections = itemsField.get().getSelectionSet().getSelections();
            selections.forEach(selection -> processSelection(StringUtils.EMPTY, selection, query, queryFieldIncludes,
             env));
        }

        // Only fetch the selected fields for better performance
        source.fetchSource(queryFieldIncludes.toArray(new String[0]), new String[0]);
        watch.stop();

        logger.debug("Executing query: {}", source);

        watch.start("searching items");
        SearchResponse response = elasticsearch.search(new SearchRequest().source(source));
        watch.stop();

        watch.start("processing items");
        result.put(FIELD_NAME_TOTAL, response.getHits().getTotalHits().value);
        if (response.getHits().getTotalHits().value > 0) {
            for(SearchHit hit :  response.getHits().getHits()) {
                items.add(fixItems(hit.getSourceAsMap()));
            }
        }
        watch.stop();

        if (logger.isTraceEnabled()) {
            logger.trace(watch.prettyPrint());
        }

        return result;
    }

    /**
     * Adds the required filters to the ES query for the given field
     */
    protected void processSelection(String path, Selection currentSelection, BoolQueryBuilder query,
                                    List<String> queryFieldIncludes, DataFetchingEnvironment env)  {
        if (currentSelection instanceof Field) {
            // If the current selection is a field
            Field currentField = (Field) currentSelection;

            // Get the original field name
            String propertyName = getOriginalName(currentField.getName());
            // Build the ES-friendly path
            String fullPath = StringUtils.isEmpty(path)? propertyName : path + "." + propertyName;

            // If the field has sub selection
            if (Objects.nonNull(currentField.getSelectionSet())) {
                // If the field is a flattened component
                if (fullPath.matches(COMPONENT_INCLUDE_REGEX)) {
                    // Include the 'content-type' field to make sure the type can be resolved during runtime
                    String contentTypeFieldPath = fullPath + "." + QUERY_FIELD_NAME_CONTENT_TYPE;
                    if (!queryFieldIncludes.contains(contentTypeFieldPath)) {
                        queryFieldIncludes.add(contentTypeFieldPath);
                    }
                }

                // Process recursively and finish
                currentField.getSelectionSet().getSelections()
                    .forEach(selection -> processSelection(fullPath, selection, query, queryFieldIncludes, env));
                return;
            }

            // Add the field to the list
            logger.debug("Adding selected field '{}' to query", fullPath);
            queryFieldIncludes.add(fullPath);

            // Check the filters to build the ES query
            Optional<Argument> arg =
                currentField.getArguments().stream().filter(a -> a.getName().equals(FILTER_NAME)).findFirst();
            if (arg.isPresent()) {
                logger.debug("Adding filters for field {}", fullPath);
                Value<?> argValue = arg.get().getValue();
                if (argValue instanceof ObjectValue) {
                    List<ObjectField> filters = ((ObjectValue) argValue).getObjectFields();
                    filters.forEach((filter) -> addFieldFilterFromObjectField(fullPath, filter, query, env));
                } else if (argValue instanceof VariableReference &&
                        env.getVariables().containsKey(((VariableReference) argValue).getName())) {
                    Map<String, Object> map =
                            (Map<String, Object>) env.getVariables().get(((VariableReference) argValue).getName());
                    map.entrySet().forEach(filter -> addFieldFilterFromMapEntry(fullPath, filter, query, env));
                }
            }
        } else if (currentSelection instanceof InlineFragment) {
            // If the current selection is an inline fragment, process recursively
            InlineFragment fragment = (InlineFragment) currentSelection;
            fragment.getSelectionSet().getSelections()
                .forEach(selection -> processSelection(path, selection, query, queryFieldIncludes, env));
        } else if (currentSelection instanceof FragmentSpread) {
            // If the current selection is a fragment spread, find the fragment and process recursively
            FragmentSpread fragmentSpread = (FragmentSpread) currentSelection;
            FragmentDefinition fragmentDefinition = env.getFragmentsByName().get(fragmentSpread.getName());
            fragmentDefinition.getSelectionSet().getSelections()
                .forEach(selection -> processSelection(path, selection, query, queryFieldIncludes, env));
        }
    }

    protected void addFieldFilterFromObjectField(String path, ObjectField filter, BoolQueryBuilder query,
                                                 DataFetchingEnvironment env) {
        boolean isVariable = filter.getValue() instanceof VariableReference;
        switch (filter.getName()) {
            case ARG_NAME_NOT:
                BoolQueryBuilder notQuery = boolQuery();
                if(isVariable) {
                    ((List<Map<String, Object>>) env.getVariables()
                            .get(((VariableReference) filter.getValue()).getName()))
                            .forEach(notFilter -> notFilter.entrySet()
                                    .forEach(entry -> addFieldFilterFromMapEntry(path, entry, notQuery, env)));
                } else {
                    ((ArrayValue) filter.getValue()).getValues()
                            .forEach(notFilter -> ((ObjectValue) notFilter).getObjectFields()
                                    .forEach(notField -> addFieldFilterFromObjectField(path, notField, notQuery, env)));
                }
                if (!notQuery.filter().isEmpty()) {
                    notQuery.filter().forEach(query::mustNot);
                }
                break;
            case ARG_NAME_AND:
                if (isVariable) {
                    ((List<Map<String, Object>>) env.getVariables()
                            .get(((VariableReference) filter.getValue()).getName()))
                            .forEach(andFilter -> andFilter.entrySet()
                                    .forEach(entry -> addFieldFilterFromMapEntry(path, entry, query, env)));
                } else {
                    ((ArrayValue) filter.getValue()).getValues()
                            .forEach(andFilter -> ((ObjectValue) andFilter).getObjectFields()
                                    .forEach(andField -> addFieldFilterFromObjectField(path, andField, query, env)));
                }
                break;
            case ARG_NAME_OR:
                BoolQueryBuilder tempQuery = boolQuery();
                if (isVariable) {
                    ((List<Map<String, Object>>) env.getVariables()
                            .get(((VariableReference) filter.getValue()).getName()))
                            .forEach(orFilter -> orFilter.entrySet()
                                    .forEach(entry -> addFieldFilterFromMapEntry(path, entry, tempQuery, env)));
                } else {
                    ((ArrayValue) filter.getValue()).getValues().forEach(orFilter ->
                            ((ObjectValue) orFilter).getObjectFields()
                                    .forEach(orField -> addFieldFilterFromObjectField(path, orField, tempQuery, env)));
                }
                if (!tempQuery.filter().isEmpty()) {
                    BoolQueryBuilder orQuery = boolQuery();
                    tempQuery.filter().forEach(orQuery::should);
                    query.filter(boolQuery().must(orQuery));
                }
                break;
            default:
                QueryBuilder builder = getFilterQueryFromObjectField(path, filter, env);
                if (builder != null) {
                    query.filter(builder);
                }
        }
    }

    protected void addFieldFilterFromMapEntry(String path, Map.Entry<String, Object> filter, BoolQueryBuilder query,
                                              DataFetchingEnvironment env) {
        if (filter.getValue() instanceof List) {
            List<Map<String, Object>> actualFilters = (List<Map<String, Object>>) filter.getValue();
            switch (filter.getKey()) {
                case ARG_NAME_NOT:
                    BoolQueryBuilder notQuery = boolQuery();
                    actualFilters.forEach(notFilter -> notFilter.entrySet()
                            .forEach(notField -> addFieldFilterFromMapEntry(path, notField, notQuery, env)));
                    notQuery.filter().forEach(query::mustNot);
                    break;
                case ARG_NAME_AND:
                    actualFilters.forEach(andFilter -> andFilter.entrySet()
                            .forEach(andField -> addFieldFilterFromMapEntry(path, andField, query, env)));
                    break;
                case ARG_NAME_OR:
                    BoolQueryBuilder tempQuery = boolQuery();
                    BoolQueryBuilder orQuery = boolQuery();
                    actualFilters.forEach(orFilter -> orFilter.entrySet()
                            .forEach(orField -> addFieldFilterFromMapEntry(path, orField, tempQuery, env)));
                    tempQuery.filter().forEach(orQuery::should);
                    query.filter(boolQuery().must(orQuery));
                    break;
                default:
                    // never happens
            }
        } else {
            query.filter(getFilterQueryFromMapEntry(path, filter));
        }
    }

    protected QueryBuilder getFilterQueryFromObjectField(String fieldPath, ObjectField filter,
                                                         DataFetchingEnvironment env) {
        Object value = getRealValue(filter.getValue(), env);
        if (value == null) {
            return null;
        }
        switch (filter.getName()) {
            case ARG_NAME_EQUALS:
                return termQuery(fieldPath, value);
            case ARG_NAME_MATCHES:
                return matchQuery(fieldPath, value);
            case ARG_NAME_REGEX:
                return regexpQuery(fieldPath, value.toString());
            case ARG_NAME_LT:
                return rangeQuery(fieldPath).lt(value);
            case ARG_NAME_GT:
                return rangeQuery(fieldPath).gt(value);
            case ARG_NAME_LTE:
                return rangeQuery(fieldPath).lte(value);
            case ARG_NAME_GTE:
                return rangeQuery(fieldPath).gte(value);
            case ARG_NAME_EXISTS:
                boolean exists = (boolean) value;
                if (exists) {
                    return existsQuery(fieldPath);
                } else {
                    return boolQuery().mustNot(existsQuery(fieldPath));
                }
            default:
                // never happens
                return null;
        }
    }

    protected QueryBuilder getFilterQueryFromMapEntry(String fieldPath, Map.Entry<String, Object> filter) {
        switch (filter.getKey()) {
            case ARG_NAME_EQUALS:
                return termQuery(fieldPath, filter.getValue());
            case ARG_NAME_MATCHES:
                return matchQuery(fieldPath, filter.getValue());
            case ARG_NAME_REGEX:
                return regexpQuery(fieldPath, filter.getValue().toString());
            case ARG_NAME_LT:
                return rangeQuery(fieldPath).lt(filter.getValue());
            case ARG_NAME_GT:
                return rangeQuery(fieldPath).gt(filter.getValue());
            case ARG_NAME_LTE:
                return rangeQuery(fieldPath).lte(filter.getValue());
            case ARG_NAME_GTE:
                return rangeQuery(fieldPath).gte(filter.getValue());
            case ARG_NAME_EXISTS:
                boolean exists = (boolean) filter.getValue();
                if (exists) {
                    return existsQuery(fieldPath);
                } else {
                    return boolQuery().mustNot(existsQuery(fieldPath));
                }
            default:
                // never happens
                return null;
        }
    }

    /**
     * Extracts a scalar value, this is needed because of GraphQL strict types
     */
    protected Object getRealValue(Value value, DataFetchingEnvironment env) {
        if (value instanceof BooleanValue) {
            return ((BooleanValue)value).isValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        } else if (value instanceof VariableReference) {
            return env.getVariables().get(((VariableReference) value).getName());
        }
        return null;
    }

    /**
     * Checks for fields containing the 'item' keyword and makes sure they are always a list even if there is only
     * one value. This is needed because the GraphQL schema always needs to return the same type for a field.
     */
    protected Map<String, Object> fixItems(Map<String, Object> map) {
        Map<String, Object> temp = new LinkedHashMap<>();

        map.forEach((key, value) -> {
            String graphQLKey = getGraphQLName(key);
            if (FIELD_NAME_ITEM.equals(key)) {
                if (!(value instanceof List)) {
                    temp.put(graphQLKey, Collections.singletonList(fixItems((Map<String, Object>)value)));
                } else {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) value;
                    temp.put(graphQLKey, list.stream().map(this::fixItems).collect(Collectors.toList()));
                }
            } else if (value instanceof Map) {
                temp.put(graphQLKey, fixItems((Map<String, Object>) value));
            } else {
                temp.put(graphQLKey, value);
            }
        });

        return MapUtils.isNotEmpty(temp)? temp : map;
    }

}
