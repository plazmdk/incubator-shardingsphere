/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sql.parser.integrate.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sql.parser.SQLParseEngineFactory;
import org.apache.shardingsphere.sql.parser.integrate.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.sql.parser.integrate.asserts.statement.SQLStatementAssert;
import org.apache.shardingsphere.sql.parser.integrate.jaxb.SQLParserTestCasesRegistry;
import org.apache.shardingsphere.sql.parser.integrate.jaxb.SQLParserTestCasesRegistryFactory;
import org.apache.shardingsphere.sql.parser.integrate.jaxb.statement.SQLParserTestCase;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.test.sql.SQLCaseType;
import org.apache.shardingsphere.test.sql.loader.SQLCasesLoader;
import org.apache.shardingsphere.test.sql.loader.SQLCasesRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
@Slf4j
public final class SQLParserParameterizedTest {
    
    private static final SQLCasesLoader SQL_CASES_LOADER = SQLCasesRegistry.getInstance().getSqlCasesLoader();
    
    private static final SQLParserTestCasesRegistry SQL_PARSER_TEST_CASES_REGISTRY = SQLParserTestCasesRegistryFactory.getInstance().getRegistry();
    
    private static final Properties PROPS = new Properties();
    
    private final String sqlCaseId;
    
    private final String databaseType;
    
    private final SQLCaseType sqlCaseType;
    
    static {
        try {
            PROPS.load(new FileInputStream(SQLParserParameterizedTest.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/runtime-config.properties"));
        } catch (final IOException ex) {
            log.warn("Can not find file `runtime-config.properties`, use default properties configuration.", ex);
        }
    }
    
    @Parameters(name = "{0} ({2}) -> {1}")
    public static Collection<Object[]> getTestParameters() {
        checkTestCases();
        return SQL_CASES_LOADER.getSQLTestParameters();
    }
    
    private static void checkTestCases() {
        Collection<String> allSQLCaseIDs = new HashSet<>(SQL_CASES_LOADER.getAllSQLCaseIDs());
        if (allSQLCaseIDs.size() != SQL_PARSER_TEST_CASES_REGISTRY.getAllSQLCaseIDs().size()) {
            allSQLCaseIDs.removeAll(SQL_PARSER_TEST_CASES_REGISTRY.getAllSQLCaseIDs());
            fail(String.format("The count of SQL cases and SQL parser cases are mismatched, missing cases are: %s", allSQLCaseIDs));
        }
    }
    
    @Test
    public void assertSupportedSQL() {
        String sql = SQL_CASES_LOADER.getSQL(sqlCaseId, sqlCaseType, SQL_PARSER_TEST_CASES_REGISTRY.get(sqlCaseId).getParameters());
        SQLParserTestCase expected = SQLParserTestCasesRegistryFactory.getInstance().getRegistry().get(sqlCaseId);
        if (expected.isLongSQL() && Boolean.parseBoolean(PROPS.getProperty("long.sql.skip", Boolean.TRUE.toString()))) {
            return;
        }
        SQLCaseAssertContext assertContext = new SQLCaseAssertContext(sqlCaseId, sqlCaseType);
        SQLStatement actual = SQLParseEngineFactory.getSQLParseEngine("H2".equals(databaseType) ? "MySQL" : databaseType).parse(sql, false);
        if (!expected.isLongSQL()) {
            SQLStatementAssert.assertIs(assertContext, actual, expected);
        }
    }
}
