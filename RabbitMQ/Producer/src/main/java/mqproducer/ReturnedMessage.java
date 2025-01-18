package mqproducer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import java.io.IOException;

public class ReturnedMessage implements ReturnListener {
    
    
    static Logger logger = new SimpleLoggerFactory().getLogger(ReturnedMessage.class.getName());
    
    @Override
    public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties basicProperties, byte[] body) throws IOException {
		logger.info("reply code={} rplyText={} exchange={} routing key={} msg={}", replyCode, replyText, exchange, routingKey, new String(body, "UTF-8"));
    }
}
