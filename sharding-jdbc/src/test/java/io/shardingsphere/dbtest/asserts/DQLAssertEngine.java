/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.dbtest.asserts;

import io.shardingsphere.core.api.yaml.YamlMasterSlaveDataSourceFactory;
import io.shardingsphere.core.api.yaml.YamlShardingDataSourceFactory;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.dbtest.common.DatabaseUtil;
import io.shardingsphere.dbtest.config.DataSetsParser;
import io.shardingsphere.dbtest.config.bean.AssertSubDefinition;
import io.shardingsphere.dbtest.config.bean.DatasetDatabase;
import io.shardingsphere.dbtest.config.bean.DatasetDefinition;
import io.shardingsphere.dbtest.env.DatabaseTypeEnvironment;
import io.shardingsphere.dbtest.env.EnvironmentPath;
import io.shardingsphere.dbtest.env.datasource.DataSourceUtil;
import io.shardingsphere.dbtest.env.schema.SchemaEnvironmentManager;
import io.shardingsphere.test.sql.SQLCaseType;
import io.shardingsphere.test.sql.SQLCasesLoader;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DQL assert engine.
 * 
 * @author zhangliang 
 */
public final class DQLAssertEngine {
    
    private final AssertSubDefinition assertSubDefinition;
    
    private final SQLCaseType caseType;
    
    private final DataSource dataSource;
    
    private final DataSetEnvironmentManager dataSetEnvironmentManager;
    
    private final String sql;
    
    private final String expectedDataFile;
    
    public DQLAssertEngine(final String sqlCaseId, final String path, 
            final AssertSubDefinition assertSubDefinition, final String shardingRuleType, final DatabaseTypeEnvironment databaseTypeEnvironment, final SQLCaseType caseType) throws IOException, JAXBException, SQLException {
        this.assertSubDefinition = assertSubDefinition;
        this.caseType = caseType;
        Map<String, DataSource> dataSourceMap = createDataSourceMap(SchemaEnvironmentManager.getDataSourceNames(shardingRuleType), databaseTypeEnvironment.getDatabaseType());
        dataSource = createDataSource(shardingRuleType, dataSourceMap);
        dataSetEnvironmentManager = new DataSetEnvironmentManager(EnvironmentPath.getDataInitializeResourceFile(shardingRuleType), dataSourceMap);
        sql = SQLCasesLoader.getInstance().getSupportedSQL(sqlCaseId);
        expectedDataFile = path.substring(0, path.lastIndexOf(File.separator) + 1) + "asserts/dql/" + assertSubDefinition.getExpectedDataFile();
    }
    
    private Map<String, DataSource> createDataSourceMap(final Collection<String> dataSourceNames, final DatabaseType databaseType) {
        Map<String, DataSource> result = new HashMap<>(dataSourceNames.size(), 1);
        for (String each : dataSourceNames) {
            result.put(each, DataSourceUtil.createDataSource(databaseType, each));
        }
        return result;
    }
    
    private DataSource createDataSource(final String shardingRuleType, final Map<String, DataSource> dataSourceMap) throws SQLException, IOException {
        return "masterslaveonly".equals(shardingRuleType)
                ? YamlMasterSlaveDataSourceFactory.createDataSource(dataSourceMap, new File(EnvironmentPath.getShardingRuleResourceFile(shardingRuleType)))
                : YamlShardingDataSourceFactory.createDataSource(dataSourceMap, new File(EnvironmentPath.getShardingRuleResourceFile(shardingRuleType)));
    }
    
    /**
     * Assert DQL.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws SQLException
     * @throws ParseException
     */
    public void assertDQL() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, SQLException, ParseException {
        try {
            dataSetEnvironmentManager.initialize();
            doSelectUsePreparedStatement();
            doSelectUsePreparedStatementToExecuteSelect();
            doSelectUseStatement();
            doSelectUseStatementToExecuteSelect();
        } finally {
            dataSetEnvironmentManager.clear();
        }
    }
    
    private void doSelectUseStatement() throws SQLException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        try (Connection connection = dataSource.getConnection()) {
            DatasetDatabase datasetDatabase = DatabaseUtil.selectUseStatement(connection, sql, assertSubDefinition.getParameter());
            DatasetDefinition checkDataset = DataSetsParser.parse(new File(expectedDataFile), "data");
            DatabaseUtil.assertDatas(checkDataset, datasetDatabase);
        }
    }
    
    private void doSelectUseStatementToExecuteSelect() throws SQLException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        try (Connection connection = dataSource.getConnection()) {
            DatasetDatabase datasetDatabase = DatabaseUtil.selectUseStatementToExecuteSelect(connection, sql, assertSubDefinition.getParameter());
            DatasetDefinition checkDataset = DataSetsParser.parse(new File(expectedDataFile), "data");
            DatabaseUtil.assertDatas(checkDataset, datasetDatabase);
        }
    }
    
    private void doSelectUsePreparedStatement() throws SQLException, ParseException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        try (Connection connection = dataSource.getConnection()) {
            DatasetDatabase ddPreparedStatement = DatabaseUtil.selectUsePreparedStatement(connection, sql, assertSubDefinition.getParameter());
            DatasetDefinition checkDataset = DataSetsParser.parse(new File(expectedDataFile), "data");
            DatabaseUtil.assertDatas(checkDataset, ddPreparedStatement);
        }
    }
    
    private void doSelectUsePreparedStatementToExecuteSelect() throws SQLException, ParseException, IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        try (Connection connection = dataSource.getConnection()) {
            DatasetDatabase datasetDatabase = DatabaseUtil.selectUsePreparedStatementToExecuteSelect(connection, sql, assertSubDefinition.getParameter());
            DatasetDefinition checkDataset = DataSetsParser.parse(new File(expectedDataFile), "data");
            DatabaseUtil.assertDatas(checkDataset, datasetDatabase);
        }
    }
}