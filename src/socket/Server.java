package socket;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server implements Runnable{

    private final int port;

    public Server(int port){
        this.port = port;
    }

    public interface Callback{
        void callingBack(String s) throws ParseException;
    }
    private Callback callback;
    public void registerCallBack(Callback callback){
        this.callback = callback;
    }

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started");
        while(true) {
            clientSocket = serverSocket.accept();
            System.out.println("client connected");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while(true) {
                try {
                    String response = in.readLine();
                    System.out.println(response);
                    if(response == null) {
                        clientSocket.close();
                        break;
                    }

                    out.println(handle_response(response));
                    out.flush();

                } catch (SocketException | ParseException e){
                    System.out.println(e);
                    break;
                }
            }
        }
    }

    public void send(String data) throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println(data);
        out.flush();
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public String handle_response(String data) throws ParseException {
        String response;
        //String replyTo;
        /*
        if(data.equals("hello server")){
            response = "hello client";
            replyTo = "hello from client";
        } else if(data.equals("hello server2")){
            response = "hello client2";
            replyTo = "hello from client2";
        } else{
            response = "unknown data";
            replyTo = "unknown from client";
        }
         */
        response = "data transferred successful";

        callback.callingBack(data);
        return response;
    }

    public void run(){
        try {
            this.start(this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}