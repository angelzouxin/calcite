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
package org.apache.calcite.linq4j.tree;

/**
 * Represents the default value of a type or an empty expression.
 */
public class DefaultExpression extends Expression {
  public DefaultExpression(Class type) {
    super(ExpressionType.Default, type);
  }

  @Override public Expression accept(Shuttle shuttle) {
    return shuttle.visit(this);
  }

  @Override public <R> R accept(Visitor<R> visitor) {
    return visitor.visit(this);
  }

}
