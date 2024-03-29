package ru.geekbrains.chat_server;

import ru.geekbrains.april_chat.common.ChatMessage;
import ru.geekbrains.april_chat.common.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ClientHandler {
//    static int clientCounter = 0;
//    private int clientNumber;
    private Socket socket;
    private ChatServer chatServer;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private String currentUsername;

    public ClientHandler(Socket socket, ChatServer chatServer) {
        try {
            this.chatServer = chatServer;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Client handler created!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle() {
        new Thread(() -> {
            try {
//                while(!Thread.currentThread().isInterrupted() || socket.isConnected()) {
                    authenticate();
                    readMessages();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void readMessages() throws IOException {
        try {
            while (!Thread.currentThread().isInterrupted() || socket.isConnected()) {
                String msg = inputStream.readUTF();
                ChatMessage message = ChatMessage.unmarshall(msg);
                message.setFrom(this.currentUsername);
                switch (message.getMessageType()) {
                    case PUBLIC -> chatServer.sendBroadcastMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeHandler();
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            outputStream.writeUTF(message.marshall());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentName() {
        return this.currentUsername;
    }

    private void authenticate() {
        System.out.println("Started client  auth...");

        try {
            while (true) {
                String authMessage = inputStream.readUTF();
                System.out.println("Auth received");
                ChatMessage msg = ChatMessage.unmarshall(authMessage);
                String username = chatServer.getAuthService().getUsernameByLoginAndPassword(msg.getLogin(), msg.getPassword());
                ChatMessage response = new ChatMessage();

                if (username == null) {
                    response.setMessageType(MessageType.ERROR);
                    response.setBody("Wrong username or password!");
                    System.out.println("Wrong credentials");
                } else if (chatServer.isUserOnline(username)) {
                    response.setMessageType(MessageType.ERROR);
                    response.setBody("Double auth!");
                    System.out.println("Double auth!");
                } else {
                    response.setMessageType(MessageType.AUTH_CONFIRM);
                    response.setBody(username);
                    currentUsername = username;
                    chatServer.subscribe(this);
                    System.out.println("Subscribed");
                    sendMessage(response);
                    break;
                }
                sendMessage(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/* Добавить отключение неавторизованных пользователей по таймауту
(120 сек. ждём после подключения клиента, и если он не авторизовался за это время, закрываем соединение).
 */
    private void checkGuestAuth(final String message) {
        if (message.startsWith("/authGuest ")) {
            nickname = message.split("\\s")[1];
            sendMsg("/authGuestOk " + nickname);
            sendMsg("У вас 2 минуты на авторизацию, иначе соединение закроется.");
            server.subscribe(this);
            isAuthorization = true;
            scheduledExecutor.schedule(() -> sendMsg("/authGuestNo"), 120, TimeUnit.SECONDS);

    public void closeHandler() {
        try {
            chatServer.unsubscribe(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
}
