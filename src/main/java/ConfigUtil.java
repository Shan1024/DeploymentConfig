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
    public static final String YML_ROOT = "configurations";
    public static final String PROPERTIES_ROOT = "configurations";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    private ConfigUtil() {

    }

    public static String getConfig(File file, ConfigFileFormat configFileFormat) {

        String config = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            config = getConfig(fileInputStream, file.getName(), configFileFormat);
        } catch (FileNotFoundException e) {
            logger.warning("Ex: " + e);
        }

        return config;
    }

    public static String getConfig(FileInputStream inputStream, String fileName, ConfigFileFormat configFileFormat) {

        String config = "";
        try {

            if (configFileFormat != ConfigFileFormat.PROPERTIES) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                bufferedReader.close();

                //Convert the file to XML format
                config = convertToXml(stringBuilder.toString(), configFileFormat);
            } else {
                //Convert the properties file to XML format
                config = convertPropertiesToXml(inputStream);
            }

            //Apply the new configs
            config = applyNewConfigs(config, fileName);

        } catch (FileNotFoundException e) {
            logger.warning("File not found: " + e);
        } catch (IOException e) {
            logger.warning("IO Ex: " + e);
        }

        return config;
    }

    //  \${(sys|sec|env)(:\w+)(\.\w+)*} regex to process placeholder values
    public static String getConfig(String key) {

        String returnValue = null;

        int index = key.toString().indexOf("/");

        if (index != -1) {
            String fileName = key.toString().substring(0, index);
            String xpath = key.toString().substring(index);
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
            //            case XML:
            //                convertedConfig = data;
            //                break;
            //            case PROPERTIES:
            //                convertedConfig = convertPropertiesToXml(data);
            //                break;
            default:
                logger.warning("Unsupported file format: " + configFileFormat);
        }

        return convertedConfig;
    }

    private static String convertPropertiesToXml(InputStream inputStream) {

        String xmlString = "";

        Properties deploymentProperties = new Properties();
        try {
            deploymentProperties.load(inputStream);

            for (Object key : deploymentProperties.keySet()) {
                xmlString = xmlString + "<" + key + ">" + deploymentProperties.get(key) + "</" + key + ">";
            }

        } catch (IOException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        xmlString = "<" + PROPERTIES_ROOT + ">" + xmlString + "</" + PROPERTIES_ROOT + ">";
        return prettyFormatXml(xmlString);
    }

    private static String prettyFormatXml(String input) {

        String xmlString = "";
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.transform(xmlInput, xmlOutput);
            xmlString = xmlOutput.getWriter().toString();
        } catch (TransformerConfigurationException e) {
            logger.info("TransformerConfigurationException: " + e);
        } catch (TransformerException e) {
            logger.info("TransformerException: " + e);
        }
        return xmlString;
    }

    private static String convertYamlToXml(String data) {

        //Convert Yaml to Json
        String jsonString = convertYamlToJson(data);

        //Convert Json to Xml
        String xmlString = convertJsonToXML(jsonString);

        logger.info("xmlString: " + xmlString);
        return xmlString;
    }

    private static String convertYamlToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

    private static String convertJsonToXML(String jsonString) {
        String xmlString = "";
        try {
            JSONObject json = new JSONObject(jsonString);
            xmlString = XML.toString(json);
        } catch (JSONException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        return "<" + YML_ROOT + ">" + xmlString + "</" + YML_ROOT + ">";
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

                for (String key : newConfigs.keySet()) {

                    logger.info("key: " + key);
                    NodeList nodeList = (NodeList) xPath.compile(key).evaluate(doc, XPathConstants.NODESET);

                    if (nodeList.item(0) != null) {
                        Node firstNode = nodeList.item(0);
                        firstNode.getFirstChild().setNodeValue(newConfigs.get(key));
                    }

                    System.out.println(nodeList);
                }
                updatedString = convertXMLtoString(doc);

            } catch (ParserConfigurationException e) {
                logger.warning("Ex: " + e);
            } catch (SAXException e) {
                logger.warning("Ex: " + e);
            } catch (IOException e) {
                logger.warning("Ex: " + e);
            } catch (XPathExpressionException e) {
                logger.warning("Ex: " + e);
            }

        } else {
            logger.info("New configurations for " + formattedFileName + " was not found in "
                    + DEPLOYMENT_PROPERTIES_FILE_NAME);
        }

        return updatedString;
    }

    public static String convertXMLtoString(Document doc) {

        String xmlString = "";
        try {
            StringWriter stringWriter = new StringWriter();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));

            xmlString = stringWriter.toString();

        } catch (Exception ex) {
            logger.warning("Exception occurred while converting doc to string: " + ex);
        }
        return xmlString;
    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<String, Map<String, String>>();

        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            if (file.exists()) {

                input = new FileInputStream(file);

                deploymentProperties.load(input);

                for (Object key : deploymentProperties.keySet()) {

                    int index = key.toString().indexOf("/");

                    if (index != -1) {
                        String fileName = key.toString().substring(0, index);
                        String xpath = key.toString().substring(index);

                        String value = deploymentProperties.getProperty(key.toString());

                        if (tempPropertiesMap.containsKey(fileName)) {
                            Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                            tempMap.put(xpath, value);
                        } else {
                            Map<String, String> tempMap = new HashMap<String, String>();
                            tempMap.put(xpath, value);
                            tempPropertiesMap.put(fileName, tempMap);
                        }
                    } else {
                        logger.warning("No path specified in the config: " + key);
                    }
                }

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
