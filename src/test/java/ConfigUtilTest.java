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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.kernel.core.util.ConfigUtil;
import org.wso2.carbon.kernel.core.util.configfiletypes.Properties;
import org.wso2.carbon.kernel.core.util.configfiletypes.XML;
import org.wso2.carbon.kernel.core.util.configfiletypes.YAML;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * This class is to demonstrate the sample uses of the ConfigUtil class.
 *
 * @since 5.1.0
 */
public class ConfigUtilTest {

    private static Logger logger = LoggerFactory.getLogger(ConfigUtilTest.class.getName());

    @Test
    public void xmlExample() {
        setUpEnvironment();
        try {
            File file = new File("Example.xml");

            JAXBContext jaxbContext = JAXBContext.newInstance(Configurations.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Configurations configurations = (Configurations) unmarshaller.unmarshal(file);

            Assert.assertEquals(configurations.getTenant(), "tenant");
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).getName(), "abc");
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).getPort(), 8000);
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).isSecure(), false);
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).getName(), "xyz");
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).getPort(), 9000);
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).isSecure(), false);

            XML configXml = ConfigUtil.getConfig(file, XML.class);

            Source xmlInput = new StreamSource(new StringReader(configXml.getValue()));
            jaxbContext = JAXBContext.newInstance(Configurations.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            configurations = (Configurations) unmarshaller.unmarshal(xmlInput);

            Assert.assertEquals(configurations.getTenant(), "new_tenant");
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).getName(), "abc");
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).getPort(), 8080);
            Assert.assertEquals(configurations.getTransports().getTransport().get(0).isSecure(), true);
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).getName(), "xyz");
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).getPort(), 9090);
            Assert.assertEquals(configurations.getTransports().getTransport().get(1).isSecure(), true);

        } catch (JAXBException e) {
            logger.error(e.toString());
        }
    }

    @Test
    public void ymlExample() {
        setUpEnvironment();
        try {
            File file = new File("Example.yml");
            FileInputStream fileInputStream = new FileInputStream(file);
            Yaml yaml = new Yaml();
            Map map = yaml.loadAs(fileInputStream, Map.class);
            ArrayList transports = (ArrayList) map.get("transports");
            LinkedHashMap transport1 = (LinkedHashMap) ((LinkedHashMap) transports.get(0)).get("transport");
            LinkedHashMap transport2 = (LinkedHashMap) ((LinkedHashMap) transports.get(1)).get("transport");

            Assert.assertEquals(map.get("tenant"), "tenant");
            Assert.assertEquals(transport1.get("name"), "abc");
            Assert.assertEquals(transport1.get("port"), 8000);
            Assert.assertEquals(transport1.get("secure"), false);
            Assert.assertEquals(transport2.get("name"), "xyz");
            Assert.assertEquals(transport2.get("port"), 9000);
            Assert.assertEquals(transport2.get("secure"), false);

            fileInputStream = new FileInputStream(file);
            YAML configYml = ConfigUtil.getConfig(fileInputStream, file.getName(), YAML.class);
            yaml = new Yaml();
            map = yaml.loadAs(configYml.getValue(), Map.class);
            transports = (ArrayList) map.get("transports");
            transport1 = (LinkedHashMap) ((LinkedHashMap) transports.get(0)).get("transport");
            transport2 = (LinkedHashMap) ((LinkedHashMap) transports.get(1)).get("transport");

            Assert.assertEquals(map.get("tenant"), "new_tenant");
            Assert.assertEquals(transport1.get("name"), "abc");
            Assert.assertEquals(transport1.get("port"), 8080);
            Assert.assertEquals(transport1.get("secure"), true);
            Assert.assertEquals(transport2.get("name"), "xyz");
            Assert.assertEquals(transport2.get("port"), 9090);
            Assert.assertEquals(transport2.get("secure"), true);

        } catch (FileNotFoundException e) {
            logger.error(e.toString());
        }
    }

    @Test
    public void propertiesExample() {
        setUpEnvironment();
        try {
            File file = new File("Example.properties");
            FileInputStream fileInputStream = new FileInputStream(file);
            java.util.Properties properties = new java.util.Properties();
            properties.load(fileInputStream);

            Assert.assertEquals(properties.get("tenant"), "tenant");
            Assert.assertEquals(properties.get("transport.abc.port"), "8000");
            Assert.assertEquals(properties.get("transport.abc.secure"), "false");
            Assert.assertEquals(properties.get("transport.xyz.port"), "9000");
            Assert.assertEquals(properties.get("transport.xyz.secure"), "false");

            fileInputStream = new FileInputStream(file);
            Properties configProperties = ConfigUtil.getConfig(fileInputStream, file.getName(), Properties.class);
            properties = new java.util.Properties();
            properties.load(new StringReader(configProperties.getValue()));

            Assert.assertEquals(properties.get("tenant"), "new_tenant");
            Assert.assertEquals(properties.get("transport.abc.port"), "8080");
            Assert.assertEquals(properties.get("transport.abc.secure"), "true");
            Assert.assertEquals(properties.get("transport.xyz.port"), "9090");
            Assert.assertEquals(properties.get("transport.xyz.secure"), "true");

        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    @Test
    public void getConfigTest() {

        setUpEnvironment();

        String newValue = ConfigUtil.getConfig("[Example.xml]/configurations/tenant");
        Assert.assertEquals(newValue, "new_tenant");
        newValue = ConfigUtil.getConfig("[Example.xml]/configurations/transports/transport/port");
        Assert.assertEquals(newValue, "8080");
        newValue = ConfigUtil.getConfig("[Example.xml]/configurations/transports/transport[name='abc']/@secure");
        Assert.assertEquals(newValue, "true");
        newValue = ConfigUtil.getConfig("[Example.xml]/configurations/transports/transport[name='xyz']/port");
        Assert.assertEquals(newValue, "9090");
        newValue = ConfigUtil.getConfig("[Example.xml]/configurations/transports/transport[2]/@secure");
        Assert.assertEquals(newValue, "true");

        newValue = ConfigUtil.getConfig("[Example.yml]/tenant");
        Assert.assertEquals(newValue, "new_tenant");
        newValue = ConfigUtil.getConfig("[Example.yml]/transports/transport/port");
        Assert.assertEquals(newValue, "8080");
        newValue = ConfigUtil.getConfig("[Example.yml]/transports/transport[name='abc']/secure");
        Assert.assertEquals(newValue, "true");
        newValue = ConfigUtil.getConfig("[Example.yml]/transports/transport[name='xyz']/port");
        Assert.assertEquals(newValue, "9090");
        newValue = ConfigUtil.getConfig("[Example.yml]/transports[2]/transport/secure");
        Assert.assertEquals(newValue, "true");

        newValue = ConfigUtil.getConfig("[Example.properties]/tenant");
        Assert.assertEquals(newValue, "new_tenant");
        newValue = ConfigUtil.getConfig("[Example.properties]/transport.abc.port");
        Assert.assertEquals(newValue, "8080");
        newValue = ConfigUtil.getConfig("[Example.properties]/transport.abc.secure");
        Assert.assertEquals(newValue, "true");
        newValue = ConfigUtil.getConfig("[Example.properties]/transport.xyz.port");
        Assert.assertEquals(newValue, "9090");
        newValue = ConfigUtil.getConfig("[Example.properties]/transport.xyz.secure");
        Assert.assertEquals(newValue, "true");
    }

    private void setUpEnvironment() {
        System.setProperty("https.port", "8080");
        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.put("HTTP_PORT", "9090");
        setEnv(envVarMap);
        System.setProperty("abc.http.port", "8080");
    }

    @SuppressWarnings("unchecked")
    private static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                logger.info(e2.toString());
            }
        } catch (Exception e1) {
            logger.info(e1.toString());
        }
    }
}
