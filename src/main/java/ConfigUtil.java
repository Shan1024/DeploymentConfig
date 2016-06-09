import ConfigFileTypes.AbstractConfigFileFormat;
import ConfigFileTypes.YAML;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigUtil {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());
    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    private static final String ROOT_ELEMENT = "configurations";
    private static final String FILE_REGEX = "\\[.+\\.(yml|properties)\\]";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    private enum ConfigFileFormat {
        YML, XML, PROPERTIES
    }

    private ConfigUtil() {

    }

    public static <T extends AbstractConfigFileFormat> T getConfig(File file, Class<T> klass) {

        T newConfigs = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            newConfigs = getConfig(fileInputStream, file.getName(), klass);
        } catch (FileNotFoundException e) {
            logger.warning("Ex: " + e);
        }
        return newConfigs;
    }

    public static <T extends AbstractConfigFileFormat> T getConfig(FileInputStream inputStream, String fileName, Class<T> klass) {

        String xmlString = "";
        ConfigFileFormat configFileFormat = ConfigFileFormat.XML;
        ;
        try {
            if (klass.isAssignableFrom(ConfigFileTypes.Properties.class)) {
                //Convert the properties file to XML format
                xmlString = convertPropertiesToXml(inputStream);
                configFileFormat = ConfigFileFormat.PROPERTIES;
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();

                //Convert the file to XML format
                if (klass.isAssignableFrom(ConfigFileTypes.XML.class)) {
                    xmlString = stringBuilder.toString();
                    configFileFormat = ConfigFileFormat.XML;
                } else if (klass.isAssignableFrom(ConfigFileTypes.YAML.class)) {
                    xmlString = convertToXml(stringBuilder.toString(), ConfigFileFormat.YML);
                    configFileFormat = ConfigFileFormat.YML;
                }
            }

            //Apply the new configs
            xmlString = applyNewConfigs(xmlString, fileName);

        } catch (FileNotFoundException e) {
            logger.warning("File not found: " + e);
        } catch (IOException e) {
            logger.warning("IO Ex: " + e);
        }

        //Convert xml back to original format
        String convertedString = convertToOriginalFormat(xmlString, configFileFormat);

        AbstractConfigFileFormat baseObject;
        if (klass.isAssignableFrom(ConfigFileTypes.YAML.class)) {
            baseObject = new YAML();
        } else if (klass.isAssignableFrom(ConfigFileTypes.XML.class)) {
            baseObject = new ConfigFileTypes.XML();
        } else if (klass.isAssignableFrom(ConfigFileTypes.Properties.class)) {
            baseObject = new ConfigFileTypes.Properties();
        } else {
            throw new IllegalArgumentException("Unsupported type " + klass.getTypeName());
        }
        baseObject.setValue(convertedString);

        return klass.cast(baseObject);
    }

    private static String convertToOriginalFormat(String xmlString, ConfigFileFormat fileFormat) {
        switch (fileFormat) {
            case XML:
                return xmlString;
            case YML:
                return convertXmlToYaml(xmlString);
            case PROPERTIES:
                return convertXmlToProperties(xmlString);
            default:
                throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
        }
    }

    //  \${(sys|sec|env)(:\w+)(\.\w+)*} - regex to identify placeholder values
    public static String getConfig(String key) {

        String returnValue = null;
        int index = key.indexOf("/");
        if (index != -1) {
            String fileName = key.substring(0, index);
            String xpath = key.substring(index);

            if (fileName.matches(FILE_REGEX)) {
                xpath = "/" + ROOT_ELEMENT + xpath;
            }
            if (deploymentPropertiesMap.containsKey(fileName)) {
                Map<String, String> configMap = deploymentPropertiesMap.get(fileName);
                if (configMap.containsKey(xpath)) {
                    returnValue = configMap.get(xpath);
                }
            } else {
                logger.warning(xpath + " was not found");
            }
        }
        return returnValue;
    }

    private static String convertToXml(String data, ConfigFileFormat configFileFormat) {
        //No need to convert xml to xml
        String convertedConfig = data;
        switch (configFileFormat) {
            case YML:
                convertedConfig = convertYamlToXml(data);
                break;
            default:
                logger.warning("Unsupported file format: " + configFileFormat);
                break;
        }
        return convertedConfig;
    }

    private static String convertYamlToXml(String data) {
        String jsonString = convertYamlToJson(data);
        return convertJsonToXml(jsonString);
    }

    private static String convertXmlToYaml(String xmlString) {
        String jsonString = convertXmlToJson(xmlString);
        return convertJsonToYaml(jsonString);
    }

    private static String convertYamlToJson(String yamlString) {
        String jsonString;
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        jsonString = jsonObject.toString();
        return jsonString;
    }

    private static String convertJsonToYaml(String jsonString) {
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(jsonString, Map.class);
        //Remove root element
        if (map.containsKey(ROOT_ELEMENT)) {
            map = (Map) map.get(ROOT_ELEMENT);
        }
        return yaml.dumpAsMap(map);
    }

    private static String convertJsonToXml(String jsonString) {
        String xmlString = "";
        try {
            JSONObject json = new JSONObject(jsonString);
            xmlString = XML.toString(json);
        } catch (JSONException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return xmlString;
    }

    private static String convertXmlToJson(String xmlString) {
        String jsonString = "";
        try {
            JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
            jsonString = xmlJSONObj.toString();
        } catch (JSONException e) {
            logger.warning("Ex: " + e);
        }
        return jsonString;
    }

    private static String convertPropertiesToXml(InputStream inputStream) {
        String xmlString = "";
        Properties deploymentProperties = new Properties();

        try {
            deploymentProperties.load(inputStream);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(xmlString);
            for (Map.Entry<Object, Object> entry : deploymentProperties.entrySet()) {
                stringBuilder.append(createXmlElement(entry.getKey().toString(), entry.getValue().toString()));
            }
            xmlString = stringBuilder.toString();
        } catch (IOException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return prettyFormatXmlString(xmlString);
    }

    private static String convertXmlToProperties(String xmlString) {
        String jsonString = convertXmlToJson(xmlString);
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(jsonString, Map.class);
        //Remove root element
        if (map.containsKey(ROOT_ELEMENT)) {
            map = (Map) map.get(ROOT_ELEMENT);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Object entryObject : map.entrySet()) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) entryObject;
            stringBuilder.append(entry.getKey() + " = " + entry.getValue()).append("\n");
        }
        return stringBuilder.toString();
    }

    private static String createXmlElement(String tagName, String text) {
        return "<" + tagName + ">" + text + "</" + tagName + ">";
    }

    private static String applyNewConfigs(String xmlString, String fileName) {

        String formattedFileName = "[" + fileName + "]";
        String updatedString = xmlString;

        if (deploymentPropertiesMap.containsKey(formattedFileName)) {
            Map<String, String> newConfigs = deploymentPropertiesMap.get(formattedFileName);
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

            try {
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xmlString)));
                XPath xPath = XPathFactory.newInstance().newXPath();

                newConfigs.keySet().forEach(key -> {
                    logger.info("key: " + key);
                    try {
                        NodeList nodeList = (NodeList) xPath.compile(key).evaluate(doc, XPathConstants.NODESET);
                        if (nodeList.item(0) != null) {
                            Node firstNode = nodeList.item(0);
                            firstNode.getFirstChild().setNodeValue(newConfigs.get(key));
                        }
                        System.out.println(nodeList);
                    } catch (XPathExpressionException e) {
                        logger.warning("Ex: " + e);
                    }
                });
                updatedString = convertXMLtoString(doc);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                logger.warning("Ex: " + e);
            }
        } else {
            logger.info("New configurations for " + formattedFileName + " was not found in "
                    + DEPLOYMENT_PROPERTIES_FILE_NAME);
        }
        return updatedString;
    }

    private static String prettyFormatXmlString(String input) {
        Source xmlInput = new StreamSource(new StringReader(input));
        return convertXmlSourceToString(xmlInput);
    }

    private static String convertXMLtoString(Document doc) {
        DOMSource domSource = new DOMSource(doc);
        return convertXmlSourceToString(domSource);
    }

    private static String convertXmlSourceToString(Source source) {

        String xmlString = "";

        try {
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, xmlOutput);
            xmlString = xmlOutput.getWriter().toString();
        } catch (Exception ex) {
            logger.warning("Exception occurred while converting doc to string: " + ex);
        }
        return xmlString;
    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<>();
        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            if (file.exists()) {
                input = new FileInputStream(file);
                deploymentProperties.load(input);

                deploymentProperties.keySet().forEach(key -> {
                    String keyString = key.toString();
                    int index = keyString.indexOf("/");
                    String fileName = keyString.substring(0, index);
                    String xpath = keyString.substring(index);
                    String value = deploymentProperties.getProperty(keyString);

                    if (fileName.matches(FILE_REGEX)) {
                        xpath = "/" + ROOT_ELEMENT + xpath;
                    }

                    if (tempPropertiesMap.containsKey(fileName)) {
                        Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                        tempMap.put(xpath, value);
                    } else {
                        Map<String, String> tempMap = new HashMap<>();
                        tempMap.put(xpath, value);
                        tempPropertiesMap.put(fileName, tempMap);
                    }
                });
            } else {
                logger.warning(DEPLOYMENT_PROPERTIES_FILE_NAME + " file not found at " + file.getAbsolutePath());
            }
        } catch (IOException ioException) {
            logger.warning("Error occurred during reading the " + DEPLOYMENT_PROPERTIES_FILE_NAME + " file. Error: "
                    + ioException.toString());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioException2) {
                    logger.warning("Error occurred while closing the InputStream. Error: " + ioException2.toString());
                }
            }
        }
        return tempPropertiesMap;
    }

}
