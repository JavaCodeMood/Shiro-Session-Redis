package com.github.izhangzhihao.ShiroSessionOnRedis.Service;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.io.Serializable;

import static org.junit.Assert.assertNotNull;


@SuppressWarnings("WeakerAccess")
public class ShiroSessionMessage extends DefaultMessage {

    @Autowired
    private JdkSerializationRedisSerializer redisSerializer;

    public final MessageBody msgBody;

    public ShiroSessionMessage(byte[] channel, byte[] body) {
        super(channel, body);
        assertNotNull(redisSerializer);
        msgBody = (MessageBody) redisSerializer.deserialize(body);
    }


    @SuppressWarnings("unused")
    @ToString
    @AllArgsConstructor
    public static class MessageBody implements Serializable {
        public final Serializable sessionId;
        public final String nodeId;
        public String msg = "";

        public MessageBody(Serializable sessionId, String nodeId) {
            this.sessionId = sessionId;
            this.nodeId = nodeId;
        }
    }
}
