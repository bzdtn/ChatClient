package ua.kiev.prog;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/*
---Reserved constructions

showall
<login>:<message>
exit
setstatus:<msg>
getstatus:<login>
joinroomm:<name> <password>
exitroom:<name>

login, password - A-Za-z0-9, no white spaces and no ,.;:/?''""()[]{}\|

 */
public class Main {

    private static String theCookie = null;

    private enum Command {
        STATUS,
        GET_STATUS
    }

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)){

            System.out.println("Enter login: ");
            String login = scanner.nextLine();
            System.out.println("Enter password: ");
            String password = scanner.nextLine();

            if(!isLogin(login, password)){
                return;
            }
//            char[] chs = (theCookie.toCharArray());
//            chs[70] = '0';
//            theCookie = new String(chs);
            GetThread thread = new GetThread(theCookie); // thread for HTTP GET
            thread.setDaemon(true);
            thread.start();

            while (true) {
                String text = scanner.nextLine();
                if (text.isEmpty()) {
                    continue;
                }
// text analyser
// single commands
                if (text.equals("exit")) {
                    thread.interrupt();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logout();
                    break; //m.setText("User '" + login + "' has left the Chat");
                }

                if (text.equals("showall")) {
                    getUsersList();
                    continue;
                }


// <command>:<data>
                String to = null;
                int indx = text.indexOf(':');
                if (indx > 0) {
                    String command = text.substring(0, indx);
                    String data = text.substring(indx + 1);
                    if (command.equals("setstatus")) {
                        setStatus(data);
                        continue;
                    }
                    if (command.equals("getstatus")) {
                        if (data.matches("[a-zA-Z0-9]*")) {
                            getUserStatus(data);
                        } else {
                            System.out.println("Invalid user name");
                        }
                        continue;
                    }
                    if (command.equals("joinroom")) {
                        String[] params = data.split(" ", 2);
                        joinRoom(params[0], params[1]);
                        continue;
                    }
                    if (command.equals("exitroom")) {
                        exitRoom(data);
                        continue;
                    }
                    if (command.matches("[a-zA-Z0-9]*")){ // private message or room message
                        to = command;
                        text = data;
                    }
                }

                Message m = new Message();
                m.setTo(to);
                m.setText(text);
                m.setFrom(login);

                try {
                    String getErrorInfo = null;
                    int res = m.send("http://localhost:8080/add", theCookie);
                    if (res != 200) {
                        System.out.println("HTTP error: " + res);
                    }
                } catch (IOException ex) {
                    System.out.println("Error: " + ex.getMessage());
                    logout();
                    return;
                }
            }
        }
    }

    private static boolean isLogin(String login, String password){
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/login");
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("login", login);
            http.setRequestProperty("password", password);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
                return false;
            }
            StringBuilder cookie = new StringBuilder();
            String headerName = null;
            for (int i=1; (headerName = http.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    if (cookie.length() > 0) {cookie.append("; ");}
                    cookie = cookie.append(http.getHeaderField(i));
                }
            }
            theCookie = cookie.toString();
            System.out.println("Login SUCCESS");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Login FAIL");
        return false;
    }

    private static void logout() {
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/logout");
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Cookie", theCookie); //otherwise will be NEW session
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
                return;
            }
            System.out.println("Logout SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getUsersList() {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/getusers");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Cookie", theCookie);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setStatus(String status) {
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/setstatus");
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Cookie", theCookie);
            http.setRequestProperty("userStatus", status);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
                return;
            }
            System.out.println("New status set SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Status setting FAIL");
        }
    }

    private static void getUserStatus(String user) {
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/getstatus?user=" + user);
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestProperty("Cookie", theCookie);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Status getting FAIL");
        }
    }

    private static boolean joinRoom(String room, String password) {
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/joinroom");
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Cookie", theCookie);
            http.setDoOutput(true);
            http.setRequestProperty("room", room);
            http.setRequestProperty("password", password);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
                return false;
            }
            System.out.println("Room joining SUCCESS");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Room joining FAIL");
        return false;
    }

    private static void exitRoom(String room) {
        URL obj = null;
        try {
            obj = new URL("http://localhost:8080/exitroom");
            HttpURLConnection http = (HttpURLConnection) obj.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Cookie", theCookie);
            http.setDoOutput(true);
            http.setRequestProperty("room", room);
            int res = http.getResponseCode();
            if (res != 200) {
                System.out.println("HTTP error: " + res + ", " + http.getHeaderField("errorInfo"));
                return;
            }
            System.out.println("Room exit SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/*
//        Message loginMsg = new Message();
//        loginMsg.setFrom(login);
//        loginMsg.setText(password);
//        loginMsg.setTo("Server");
//        try {
//            int res = loginMsg.send("http://localhost:8080/login"); //?from=" + login + "&text=" + password);
//            if (res != 200) {
//                System.out.println("HTTP error: " + res);
//                return false;
//            }
//            System.out.println("Login success");
//        } catch (IOException ex) {
//            System.out.println("Error: " + ex.getMessage());
//            return false;
//        }
//        return true;

 */