// Copyright 2017 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.webservice.sql.evaluator

import static com.yahoo.bard.webservice.database.Database.ADDED
import static com.yahoo.bard.webservice.database.Database.DELETED
import static com.yahoo.bard.webservice.database.Database.SCHEMA
import static com.yahoo.bard.webservice.database.Database.WIKITICKER
import static com.yahoo.bard.webservice.sql.builders.Aggregator.max
import static com.yahoo.bard.webservice.sql.builders.Aggregator.min
import static com.yahoo.bard.webservice.sql.builders.Aggregator.sum
import static com.yahoo.bard.webservice.sql.builders.Havings.and
import static com.yahoo.bard.webservice.sql.builders.Havings.equal
import static com.yahoo.bard.webservice.sql.builders.Havings.gt
import static com.yahoo.bard.webservice.sql.builders.Havings.lt
import static com.yahoo.bard.webservice.sql.builders.Havings.or

import com.yahoo.bard.webservice.data.time.DefaultTimeGrain
import com.yahoo.bard.webservice.database.Database
import com.yahoo.bard.webservice.sql.ApiToFieldMapper
import com.yahoo.bard.webservice.sql.aggregation.DruidSqlAggregationConverter
import com.yahoo.bard.webservice.sql.builders.SimpleDruidQueryBuilder
import com.yahoo.bard.webservice.sql.helper.CalciteHelper
import com.yahoo.bard.webservice.sql.helper.SqlTimeConverter

import org.apache.calcite.rel.rel2sql.RelToSqlConverter
import org.apache.calcite.rex.RexNode
import org.apache.calcite.sql.SqlDialect
import org.apache.calcite.tools.RelBuilder

import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Connection
import java.util.stream.Collectors

class HavingEvaluatorSpec extends Specification {
    static int ONE = 1
    static int TWO = 2
    static Connection CONNECTION = Database.initializeDatabase()
    static SqlTimeConverter sqlTimeConverter = new SqlTimeConverter()
    static HavingEvaluator havingEvaluator = new HavingEvaluator()
    static DruidSqlAggregationConverter druidSqlTypeConverter = new DruidSqlAggregationConverter()
    static final String API = "api_"

    private static RelBuilder getBuilder() {
        RelBuilder builder = CalciteHelper.getBuilder(Database.getDataSource(), SCHEMA)
        builder.scan(WIKITICKER);
        return builder
    }

    @Unroll
    def "GetDimensionNames on #expectedHavingSql"() {
        setup:
        ApiToFieldMapper apiToFieldMapper = SimpleDruidQueryBuilder.getApiToFieldMapper(API, "")
        RelBuilder builder = getBuilder()
        RelBuilder.AggCall[] aggregationCalls = aggregations.stream().map {
            def sqlAggregation = druidSqlTypeConverter.apply(it, apiToFieldMapper).get()
            return builder.aggregateCall(
                    sqlAggregation.sqlAggFunction,
                    false,
                    null,
                    sqlAggregation.sqlAggregationAsName,
                    builder.field(sqlAggregation.sqlAggregationFieldName)
            )
        }.collect(Collectors.toList()).toArray() as RelBuilder.AggCall[]
        builder.aggregate(
                builder.groupKey(
                        sqlTimeConverter.buildGroupBy(builder, DefaultTimeGrain.DAY, "TIME")
                ),
                aggregationCalls
        )
        RexNode havingFilter = havingEvaluator.evaluateHaving(having, builder, apiToFieldMapper)
        builder.filter(havingFilter)

        expect:
        String sql = new RelToSqlConverter(SqlDialect.create(CONNECTION.getMetaData())).visitChild(0, builder.build()).
                asSelect().
                toString();
        sql.contains(expectedHavingSql)
        where: "we have"
        having                                               | aggregations                           | expectedHavingSql
        gt(ADDED, ONE)                                 | [sum(ADDED)]                     | "HAVING SUM(`${ADDED}`) > 1"
        lt(DELETED, ONE)                               | [sum(DELETED)]                   | "HAVING SUM(`${DELETED}`) < 1"
        equal(DELETED, ONE)                            | [sum(DELETED), sum(ADDED)] | "HAVING SUM(`${DELETED}`) = 1"
        or(equal(DELETED, ONE), lt(ADDED, TWO))  | [max(DELETED), min(ADDED)] | "HAVING MAX(`${DELETED}`) = 1 OR MIN(`${ADDED}`) < 2"
        and(equal(DELETED, ONE), lt(ADDED, TWO)) | [max(DELETED), min(ADDED)] | "HAVING MAX(`${DELETED}`) = 1 AND MIN(`${ADDED}`) < 2"

    }

    def "Test null input"() {
        setup:
        RelBuilder builder = getBuilder()

        expect:
        havingEvaluator.evaluateHaving(null, builder, null) == null
    }
}
