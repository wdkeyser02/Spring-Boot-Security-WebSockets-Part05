package willydekeyser.config;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.add(session);
        for (WebSocketSession activeSession : sessions) {
            if (activeSession.isOpen()) {
                try {
                	activeSession.sendMessage(new TextMessage(CreateString(activeSession)));
                } catch (IOException e) {
                    sessions.remove(activeSession);
				}
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
        for (WebSocketSession activeSession : sessions) {
            if (activeSession.isOpen()) {
                try {        
                	activeSession.sendMessage(new TextMessage(CreateString(activeSession)));
                } catch (IOException e) {
                    sessions.remove(activeSession);
				}
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    	JsonNode jsonNode = objectMapper.readTree(message.getPayload());
    	String userid = jsonNode.get("userid").asString();
    	String textmessage = jsonNode.get("message").asString();
        
        String htmlFragment = """
            <div id="message" class="message-box" hx-swap-oob="beforeend">
                <p>Message from User : %s & SessionId: %s</p>
                <p>%s</p>
            </div>
            """.formatted(session.getPrincipal().getName(), session.getId(), textmessage);

        for (WebSocketSession activeSession : sessions) {
            if (activeSession.getPrincipal().getName().equals(userid)) {
                try {
                	activeSession.sendMessage(new TextMessage(htmlFragment));
                } catch (IOException e) {
                    sessions.remove(activeSession);
				}
            }
        }
    }
    
    private String CreateString(WebSocketSession activeSession) {
    	StringBuilder text = new StringBuilder();
    	text.append("<p>User Name: " +  activeSession.getPrincipal().getName() + "</p>");
        text.append("<p>Connected Users:</p>");
        sessions.forEach(x -> 
        	{if (!x.getId().equals(activeSession.getId())) {
        		text.append("<p>" + x.getPrincipal().getName() + "</p>");
			}});      
    	return text.toString();
    }
}