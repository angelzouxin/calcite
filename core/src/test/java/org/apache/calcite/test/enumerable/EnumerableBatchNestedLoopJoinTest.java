/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.test.enumerable;

import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.adapter.java.ReflectiveSchema;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.runtime.Hook;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.test.CalciteAssert;
import org.apache.calcite.test.JdbcTest;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 * Unit test for
 * {@link org.apache.calcite.adapter.enumerable.EnumerableBatchNestedLoopJoin}.
 */
class EnumerableBatchNestedLoopJoinTest {

  @Test void simpleInnerBatchJoinTestBuilder() {
    tester(false, new JdbcTest.HrSchema())
        .query("?")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .withRel(
            builder -> builder
                .scan("s", "depts").as("d")
                .scan("s", "emps").as("e")
                .join(JoinRelType.INNER,
                    builder.equals(
                        builder.field(2, "d", "deptno"),
                        builder.field(2, "e", "deptno")))
                .project(
                    builder.field("deptno"))
                .build())
        .returnsUnordered(
            "deptno=10",
            "deptno=10",
            "deptno=10");
  }

  @Test void simpleInnerBatchJoinTestSQL() {
    tester(false, new JdbcTest.HrSchema())
        .query(
            "select e.name from emps e join depts d on d.deptno = e.deptno")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("name=Bill",
            "name=Sebastian",
            "name=Theodore");
  }

  @Test void simpleLeftBatchJoinTestSQL() {
    tester(false, new JdbcTest.HrSchema())
        .query(
            "select e.name, d.deptno from emps e left join depts d on d.deptno = e.deptno")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("name=Bill; deptno=10",
            "name=Eric; deptno=null",
            "name=Sebastian; deptno=10",
            "name=Theodore; deptno=10");
  }

  @Test void innerBatchJoinTestSQL() {
    tester(false, new JdbcTest.HrSchemaBig())
        .query(
            "select count(e.name) from emps e join depts d on d.deptno = e.deptno")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("EXPR$0=46");
  }

  @Test void innerBatchJoinTestSQL2() {
    tester(false, new JdbcTest.HrSchemaBig())
        .query(
            "select count(e.name) from emps e join depts d on d.deptno = e.empid")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("EXPR$0=4");
  }

  @Test void leftBatchJoinTestSQL() {
    tester(false, new JdbcTest.HrSchemaBig())
        .query(
            "select count(d.deptno) from depts d left join emps e on d.deptno = e.deptno"
            + " where d.deptno <30 and d.deptno>10")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("EXPR$0=8");
  }

  @Test void testJoinSubQuery() {
    String sql = "SELECT count(name) FROM emps e WHERE e.deptno NOT IN "
        + "(SELECT d.deptno FROM depts d WHERE d.name = 'Sales')";
    tester(false, new JdbcTest.HrSchemaBig())
        .query(sql)
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_MERGE_JOIN_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_JOIN_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("EXPR$0=23");
  }

  @Test void testInnerJoinOnString() {
    String sql = "SELECT d.name, e.salary FROM depts d join emps e on d.name = e.name";
    tester(false, new JdbcTest.HrSchemaBig())
        .query(sql)
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_MERGE_JOIN_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_JOIN_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("");
  }
  @Test void testSemiJoin() {
    tester(false, new JdbcTest.HrSchemaBig())
        .query("?")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_MERGE_JOIN_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_JOIN_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .withRel(
            builder -> builder
                .scan("s", "emps").as("e")
                .scan("s", "depts").as("d")
                .semiJoin(
                    builder.equals(
                        builder.field(2, "e", "empid"),
                        builder.field(2, "d", "deptno")))
                .project(
                    builder.field("name"))
                .build())
        .returnsUnordered(
            "name=Emmanuel",
            "name=Gabriel",
            "name=Michelle",
            "name=Ursula");
  }

  @Test void testAntiJoin() {
    tester(false, new JdbcTest.HrSchema())
        .query("?")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_MERGE_JOIN_RULE);
          planner.removeRule(EnumerableRules.ENUMERABLE_JOIN_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .withRel(
            builder -> builder
                .scan("s", "emps").as("e")
                .scan("s", "emps").as("e2")
                .antiJoin(
                    builder.and(
                        builder.equals(
                            builder.field(2, "e", "deptno"),
                            builder.field(2, "e2", "deptno")),
                        builder.call(
                            SqlStdOperatorTable.GREATER_THAN,
                            builder.field(2, "e2", "salary"),
                            builder.field(2, "e", "salary"))))
                .project(
                    builder.field("name"),
                    builder.field("salary"))
                .build())
        .returnsUnordered(
            "name=Theodore; salary=11500.0",
            "name=Eric; salary=8000.0");
  }

  @Test void innerBatchJoinAndTestSQL() {
    tester(false, new JdbcTest.HrSchemaBig())
        .query(
            "select count(e.name) from emps e join depts d on d.deptno = e.empid and d.deptno = e.deptno")
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered("EXPR$0=1");
  }

  private CalciteAssert.AssertThat tester(boolean forceDecorrelate,
      Object schema) {
    return CalciteAssert.that()
        .with(CalciteConnectionProperty.LEX, Lex.JAVA)
        .with(CalciteConnectionProperty.FORCE_DECORRELATE, forceDecorrelate)
        .withSchema("s", new ReflectiveSchema(schema));
  }


  @Test void jdbcLeftBatchJoinTestSQL() {
    String sql = "SELECT id, brand_id, category_id, name, fee FROM dsp.ad_group where business_type=524288 and yn=1 and campaign_type=26  and campaign_id=102384338";
    String sql1 = "SELECT brand_code, barndname_full from dsp_alarm.dim_item_brand";
    String sql2 = "SELECT dim_item_gen_third_cate_id, dim_item_gen_third_cate_name from dsp_alarm.dim_item_gen_third_cate_re_right";
    String sql3 = "SELECT dsp.id groupId, dsp.name groupName, dsp.category_id cid3, dsp.brand_id brandId, alarm1.barndname_full brandName, alarm2.dim_item_gen_third_cate_name cid3Name\n" +
        "FROM (" + sql +") dsp LEFT JOIN (" + sql1 +") alarm1 ON dsp.brand_id = alarm1.brand_code LEFT JOIN (" + sql2 + ") alarm2 ON dsp.category_id = alarm2.dim_item_gen_third_cate_id";

    CalciteAssert.model(JdbcTest.MY_SCHEMA)
        .with(Lex.MYSQL)
        .query(
            sql3)
        .withHook(Hook.PLANNER, (Consumer<RelOptPlanner>) planner -> {
          planner.removeRule(EnumerableRules.ENUMERABLE_CORRELATE_RULE);
          planner.addRule(EnumerableRules.ENUMERABLE_BATCH_NESTED_LOOP_JOIN_RULE);
        })
        .returnsUnordered(
            "groupId=102384339; groupName=南梦蔻（Nemowco）_衬衫; cid3=1348; brandId=13348; " +
                "brandName=南梦蔻（Nemowco）; cid3Name=衬衫",
            "groupId=102384340; groupName=未知_空调; cid3=870; brandId=0; brandName=null; " +
            "cid3Name=空调");
  }

}
