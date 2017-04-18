package org.endeavourhealth.dbpatcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.endeavourhealth.dbpatcher.configuration.Database;
import org.endeavourhealth.dbpatcher.helpers.LogHelper;
import org.endeavourhealth.dbpatcher.helpers.ResourceHelper;
import org.endeavourhealth.dbpatcher.helpers.XmlHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class ConfigParser {
    private final static String DATABASE_SCHEMA_FILENAME = "database.xsd";

    private final static LogHelper LOG = LogHelper.getLogger(ConfigParser.class);

    private String jdbcUrl;
    private String username;
    private String password;
    private String basePath;
    private String schemaPath;
    private String functionsPath;
    private String triggersPath;
    private String scriptsPath;

    public ConfigParser(String databaseXmlPath, String jdbcUrlOverride, String usernameOverride, String passwordOverride) throws DBPatcherException {
        determineConfiguration(databaseXmlPath, jdbcUrlOverride, usernameOverride, passwordOverride);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public String getFunctionsPath() {
        return functionsPath;
    }

    public String getTriggersPath() {
        return triggersPath;
    }

    public String getScriptsPath() { return scriptsPath; }

    private void determineConfiguration(String databaseXmlPath, String jdbcUrlOverride, String usernameOverride, String passwordOverride) throws DBPatcherException {

        File xmlFile = getDatabaseXmlFile(databaseXmlPath);
        Database database = getDatabaseXml(xmlFile);

        LOG.info(" Using configuration:");

        printConfiguration("Database xml file", getCanonicalPath(xmlFile));

        this.jdbcUrl = getOptionalValue("JDBC URL", database.getJdbcUrl(), jdbcUrlOverride);
        this.username = getOptionalValue("Username", database.getUsername(), usernameOverride);
        this.password = getOptionalValue("Password", database.getPassword(), passwordOverride, true);

        if (StringUtils.isEmpty(this.jdbcUrl))
            throw new DBPatcherException("Empty JDBC URL");

        if (StringUtils.isEmpty(this.username))
            throw new DBPatcherException("Empty username");

        this.basePath = xmlFile.getParentFile().getPath();
        printPath("base", this.basePath);

        this.schemaPath = getAndPrintCanonicalPath("schema", database.getPaths().getSchema());
        this.functionsPath = getAndPrintCanonicalPath("functions", database.getPaths().getFunctions());
        this.triggersPath = getAndPrintCanonicalPath("triggers", database.getPaths().getTriggers());
        this.scriptsPath = getAndPrintCanonicalPath("scripts", database.getPaths().getScripts());
    }

    public void patch() {
    }

    private String getOptionalValue(String valueName, String value, String overriddenValue) {
        return getOptionalValue(valueName, value, overriddenValue, false);
    }

    private String getOptionalValue(String valueName, String value, String overriddenValue, boolean hidePrintedValue) {
        if (overriddenValue != null) {
            printConfiguration(valueName + " (overridden)", overriddenValue, hidePrintedValue);
            return overriddenValue;
        }

        printConfiguration(valueName, value, hidePrintedValue);
        return value;
    }

    private void printConfiguration(String name, String value) {
        printConfiguration(name, value, false);
    }

    private void printConfiguration(String name, String value, boolean hideValue) {
        String printedValue = value;

        if (hideValue)
            printedValue = "(value hidden)";

        LOG.info("  " + StringUtils.rightPad(name + ":  ", 27) + printedValue);
    }

    private String getAndPrintCanonicalPath(String pathName, String relativePathValue) throws DBPatcherException {
        if (relativePathValue == null)
            return null;

        File path = new File(Paths.get(this.basePath, relativePathValue).toString());

        if (!path.isDirectory() || (!path.exists()))
            throw new DBPatcherException("Could not find " + pathName + " path '" + getCanonicalPath(path) + "'");

        printPath(pathName, getCanonicalPath(path));

        return getCanonicalPath(path);
    }

    private void printPath(String pathName, String pathValue) throws DBPatcherException {
        printConfiguration(WordUtils.capitalize(pathName) + " path", getCanonicalPath(new File(pathValue)));
    }

    private static String getCanonicalPath(File file) throws DBPatcherException {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new DBPatcherException("Error attempting to resolve path '" + file.getAbsolutePath());
        }
    }

    private static Database getDatabaseXml(File xmlFile) throws DBPatcherException {
        try {
            String xsd = ResourceHelper.getResourceAsString(DATABASE_SCHEMA_FILENAME);
            String xml = FileUtils.readFileToString(xmlFile, StandardCharsets.UTF_8);

            XmlHelper.validate(xml, xsd);

            return XmlHelper.deserialize(xml, Database.class);
        } catch (Exception e) {
            throw new DBPatcherException("Error reading '" + xmlFile.getPath() + "' - " + e.getMessage(), e);
        }
    }

    private static File getDatabaseXmlFile(String path) throws DBPatcherException {
        File xmlFile = new File(path);

        if (!xmlFile.isFile() || (!xmlFile.exists()))
            throw new DBPatcherException("Could not find database configuration file '" + path + "'");

        return xmlFile;
    }
}
