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
package org.apache.calcite.sql.fun;

import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.util.Optionality;

/**
 * <code>NTILE</code> aggregate function
 * return the value of given expression evaluated at given offset.
 */
public class SqlNtileAggFunction extends SqlAggFunction {
  public SqlNtileAggFunction() {
    super(
        "NTILE",
        null,
        SqlKind.NTILE,
        ReturnTypes.RANK,
        null,
        OperandTypes.POSITIVE_INTEGER_LITERAL,
        SqlFunctionCategory.NUMERIC,
        false,
        true,
        Optionality.FORBIDDEN);
  }

  @Override public boolean allowsFraming() {
    return false;
  }

}

// End SqlNtileAggFunction.java
