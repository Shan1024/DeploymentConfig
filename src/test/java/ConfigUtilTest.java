/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/**
 * Test class for ConfigUtil class.
 */
public class ConfigUtilTest {

//    private static final String YML_FILENAME = "carbon.yml";
//
//    @BeforeClass
//    public void setup() {
//
//    }
//
//    @Test
//    public void testGetConfigs1() {
//        Map carbonYMLMap = readYAML();
//
//        System.out.println("OldMap: " + carbonYMLMap);
//
//        Map newMap = ConfigUtil.getConfigs(carbonYMLMap, YML_FILENAME);
//
//        System.out.println("NewMap: " + newMap);
//
//        Assert.assertEquals(newMap.get("tenant"), "shanm");
//        Assert.assertEquals(((Map) newMap.get("ports")).get("offset"), 12);
//    }
//
//    @Test
//    public void testGetConfigs2() {
//        Map carbonYMLMap = readYAML();
//
//        System.out.println("OldMap: " + carbonYMLMap);
//
//        Map newMap = ConfigUtil.getConfigs(YML_FILENAME);
//        System.out.println("Carbon: " + newMap);
//
//        Assert.assertEquals(newMap.get("tenant"), "shanm");
//        Assert.assertEquals(newMap.get("ports.offset"), "12");
//    }
//
//    @Test
//    private void testGetValue1() {
//        Object newValue = ConfigUtil.getValue("[carbon.yml]/logging.enable", false);
//        //New value from the deployment.properties file is returned
//        Assert.assertEquals(newValue, true);
//    }
//
//    @Test
//    private void testGetValue2() {
//        Object newValue = ConfigUtil.getValue("[axis.yml]/logging.enable", true);
//        //Default value returns because the property is not in the deployment.properties file
//        Assert.assertEquals(newValue, true);
//    }
//
//    @Test
//    public void convertMapToYamlTest() {
//        String expected = "id: ${sec.key}\n" + "name: ${sys.name}\n" + "version: ${env.version}\n" + "tenant: shanm\n"
//                + "ports:\n" + "  offset: 12\n" + "  https:\n" + "    port: 4848\n" + "    backup:\n"
//                + "      port: 4949\n" + "deployment:\n" + "  mode: scheduled\n" + "transports:\n" + "- transport:\n"
//                + "    name: netty\n" + "- transport:\n" + "    name: xyz\n" + "logging:\n" + "  enable: true\n";
//
//        Map carbonYMLMap = readYAML();
//        Map newMap = ConfigUtil.getConfigs(carbonYMLMap, YML_FILENAME);
//        String yamlString = ConfigUtil.convertMapToYmlString(newMap);
//
//        //        Assert.assertEquals(yamlString,expected);
//
//        try {
//            File newFile = new File("carbon_new.yml");
//            FileWriter fileWriter = new FileWriter(newFile);
//            fileWriter.write(yamlString);
//            fileWriter.flush();
//
//            fileWriter.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testConvertXMLToYAML() {
//        //        ConfigUtil.convertXMLToYAML("");
//
//        String log4jXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!--\n"
//                + " Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.\n" + "\n"
//                + " Licensed under the Apache License, Version 2.0 (the \"License\");\n"
//                + " you may not use this file except in compliance with the License.\n"
//                + " You may obtain a copy of the License at\n" + "\n" + " http://www.apache.org/licenses/LICENSE-2.0\n"
//                + "\n" + " Unless required by applicable law or agreed to in writing, software\n"
//                + " distributed under the License is distributed on an \"AS IS\" BASIS,\n"
//                + " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
//                + " See the License for the specific language governing permissions and\n"
//                + " limitations under the License.\n" + "-->\n" + "\n" + "<Configuration>\n" + "    <Appenders>\n"
//                + "\n"
//                + "        <RandomAccessFile name=\"RandomAccessFile\" fileName=\"${sys:carbon.home}/logs/carbon.log\" immediateFlush=\"false\" append=\"false\">\n"
//                + "            <PatternLayout>\n" + "                <Pattern>[%d] %5p {%c} - %m%ex%n</Pattern>\n"
//                + "            </PatternLayout>\n" + "        </RandomAccessFile>\n" + "\n"
//                + "        <Console name=\"CARBON_CONSOLE\" target=\"SYSTEM_OUT\">\n"
//                + "            <PatternLayout pattern=\"[%d] %5p {%c} - %m%ex%n\"/>\n" + "        </Console>\n"
//                + "        \n" + "    </Appenders>\n" + "    <Loggers>\n" + "\n"
//                + "        <AsyncLogger name=\"com.foo.Bar\" level=\"trace\" includeLocation=\"true\">\n"
//                + "            <AppenderRef ref=\"RandomAccessFile\"/>\n" + "        </AsyncLogger>\n" + "\n"
//                + "        <Root level=\"info\">\n" + "            <AppenderRef ref=\"RandomAccessFile\"/>\n"
//                + "            <AppenderRef ref=\"CARBON_CONSOLE\"/>\n" + "        </Root>\n" + "    </Loggers>\n"
//                + "</Configuration>";
//
//        String sample = "<?xml version=\"1.0\" ?><test content=\"mypassword\">This is a sample xml" + "file</test>";
//
//        Map map = ConfigUtil.loadXMLAsMap(sample);
//
//        System.out.println(map);
//
//        Map newMap = ConfigUtil.getConfigs(map, "log4j2.xml");
//
//        System.out.println(newMap);
//    }
//
//    private static Map readYAML() {
//
//        Map map = null;
//        Yaml yaml = new Yaml();
//
//        try {
//            InputStream ios = new FileInputStream(new File(YML_FILENAME));
//            map = yaml.loadAs(ios, Map.class);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return map;
//    }



    @Test
    public void testGetConfig(){
        File file= new File("carbon.yml");
        ConfigUtil2.getConfig(file,ConfigFileFormat.YML);
    }
}