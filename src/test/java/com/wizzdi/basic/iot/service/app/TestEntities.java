package com.wizzdi.basic.iot.service.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.basic.iot.client.*;
import com.wizzdi.basic.iot.service.ClientIdHolder;
import com.wizzdi.basic.iot.service.ObjectHolder;
import com.wizzdi.basic.iot.service.utils.KeyUtils;
import com.wizzdi.flexicore.file.model.FileResource;
import com.wizzdi.flexicore.file.service.FileResourceService;
import com.wizzdi.flexicore.file.service.MD5Service;
import info.schnatterer.mobynamesgenerator.MobyNamesGenerator;
import org.apache.commons.io.IOUtils;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class TestEntities {
    private static final Logger logger = LoggerFactory.getLogger(TestEntities.class);

    @Value("${basic.iot.test.keyPath}")
    private String keyPath;
    @Value("${basic.iot.test.publicKeyPath}")
    private String publicKeyPath;
    @Value("${basic.iot.test.mqtt.url:ssl://localhost:8883}")
    private String[] mqttURLs;
    @Value("${basic.iot.test.mqtt.username:#{null}}")
    private String username;
    @Value("${basic.iot.test.mqtt.password:#{null}}")
    private char[] password;
    @Value("${basic.iot.test.id:#{null}}")
    private String clientId;
    private static final Queue<IOTMessageSubscriber> messageSubscribers = new LinkedBlockingQueue<>();


    @Bean
    public ClientIdHolder clientIdHolder() {
        return new ClientIdHolder(Optional.ofNullable(clientId).orElse(MobyNamesGenerator.getRandomName()+System.currentTimeMillis()));
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password);

        options.setServerURIs(mqttURLs);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public PrivateKey clientPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return KeyUtils.readPrivateKey(keyPath);

    }

    @Bean
    public PublicKey clientPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return KeyUtils.readPublicKey(publicKeyPath);

    }

    @Bean
    public MqttPahoMessageHandler mqttPahoMessageHandlerClient(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler someMqttClient = new MqttPahoMessageHandler("iot-client-out", mqttClientFactory);
        someMqttClient.setDefaultQos(1);
        return someMqttClient;
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttPahoMessageDrivenChannelAdapterClient(MqttPahoClientFactory mqttClientFactory,ClientIdHolder clientIdHolder) {
        MqttPahoMessageDrivenChannelAdapter messageProducer = new MqttPahoMessageDrivenChannelAdapter("iot-client-in", mqttClientFactory, BasicIOTClient.getInTopic(clientIdHolder.getGatewayId()));
        messageProducer.setQos(1);
        return messageProducer;
    }


    @Bean
    public BasicIOTClient testBasicIOTClient(PrivateKey clientPrivateKey, ObjectMapper objectMapper,ClientIdHolder clientIdHolder) {
        return new BasicIOTClient(clientIdHolder.getGatewayId(), clientPrivateKey, objectMapper, messageSubscribers,true);

    }


    @Bean
    public IntegrationFlow clientInputIntegrationFlow(BasicIOTClient testBasicIOTClient, MqttPahoMessageDrivenChannelAdapter mqttPahoMessageDrivenChannelAdapterClient) {
        return IntegrationFlows.from(
                        mqttPahoMessageDrivenChannelAdapterClient)
                .handle(testBasicIOTClient)
                .get();
    }


    @Bean
    public IntegrationFlow clientOutputIntegrationFlow(MqttPahoMessageHandler mqttPahoMessageHandlerClient) {
        return f -> f.handle(mqttPahoMessageHandlerClient);
    }

    @Bean
    public BasicIOTConnection testBasicIOTConnection(BasicIOTClient testBasicIOTClient, IntegrationFlow clientOutputIntegrationFlow, IntegrationFlow clientInputIntegrationFlow, MqttPahoMessageDrivenChannelAdapter mqttPahoMessageDrivenChannelAdapterClient) {
        return testBasicIOTClient.open(clientInputIntegrationFlow, clientOutputIntegrationFlow, mqttPahoMessageDrivenChannelAdapterClient);
    }

    @Bean
    @Lazy
    public ObjectHolder<FileResource> firmwareFile(MD5Service md5Service, FileResourceService fileResourceService, SecurityContextBase adminSecurityContext) throws IOException {
        Random rd = new Random();
        byte[] data = new byte[80000];
        rd.nextBytes(data);
        String md5 = md5Service.generateMD5(data);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            return new ObjectHolder<>(fileResourceService.uploadFileResource("test.jpg", adminSecurityContext, md5, md5, true, byteArrayInputStream));

        }
    }


    @Bean
    public String jsonSchema() throws IOException {
        return IOUtils.resourceToString("light.schema.json", StandardCharsets.UTF_8, TestEntities.class.getClassLoader());
    }

    public static void addMessageSubscriber(IOTMessageSubscriber iotMessageSubscriber) {
        messageSubscribers.add(iotMessageSubscriber);
    }

    public static void removeMessageSubscriber(IOTMessageSubscriber iotMessageSubscriber) {
        messageSubscribers.remove(iotMessageSubscriber);
    }

    public static void clearMessageSubscribers() {
        messageSubscribers.clear();
    }


}
