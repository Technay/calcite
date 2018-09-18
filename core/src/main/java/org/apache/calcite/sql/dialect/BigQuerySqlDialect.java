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
package org.apache.calcite.sql.dialect;

import org.apache.calcite.config.NullCollation;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSetOperator;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.SqlWriter;

/**
 * A <code>SqlDialect</code> implementation for Google BigQuery's "Standard SQL"
 * dialect.
 */
public class BigQuerySqlDialect extends SqlDialect {
  public static final SqlDialect DEFAULT =
      new BigQuerySqlDialect(
          EMPTY_CONTEXT
              .withDatabaseProduct(SqlDialect.DatabaseProduct.BIG_QUERY)
              .withNullCollation(NullCollation.LOW));

  /** Creates a BigQuerySqlDialect. */
  public BigQuerySqlDialect(SqlDialect.Context context) {
    super(context);
  }

  @Override public boolean emulatesOperator(final SqlOperator operator) {
    switch (operator.getName()) {
    case "POSITION":
    case "UNION":
      return true;
    default:
      return false;
    }
  }

  @Override public void unparseSqlOperator(SqlOperator operatorRex, final SqlWriter writer,
      final SqlCall call, final int leftPrec, final int rightPrec) {
    switch (operatorRex.getName()) {
    case "POSITION":
      final SqlWriter.Frame frame = writer.startFunCall("STRPOS");
      writer.sep(",");
      call.operand(1).unparse(writer, leftPrec, rightPrec);
      writer.sep(",");
      call.operand(0).unparse(writer, leftPrec, rightPrec);
      if (3 == call.operandCount()) {
        throw new RuntimeException("3rd operand Not Supported for Function STRPOS in Big Query");
      }
      writer.endFunCall(frame);
      break;
    case "UNION":
      SqlSyntax.BINARY.unparse(writer, UNION_DISTINCT, call, leftPrec, rightPrec);
      break;
    default:
      throw new RuntimeException("Emulation for Function :- " + operatorRex.getName()
          + "is not handled.");
    }
  }

  private static final SqlOperator UNION_DISTINCT = new SqlSetOperator(
      "UNION DISTINCT", SqlKind.UNION, 14, true);

}

// End BigQuerySqlDialect.java
