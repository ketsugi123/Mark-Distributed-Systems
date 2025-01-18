package rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;

import java.io.IOException;

public class ReturnedMessage implements ReturnListener {


    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(ReturnedMessage.class.getName());
    
    @Override
    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties basicProperties, byte[] body) throws IOException {
		logger.info("reply code={} rplyText={} exchange={} routing key={} msg={}", replyCode, replyText, exchange, routingKey, new String(body, "UTF-8"));
    }
}
