import com.sun.net.httpserver.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import org.apache.commons.dbcp2.BasicDataSource;


public class Main {
//    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static BasicDataSource dataSource;
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/virtual_threads";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "Fluffy&Daisy1!";
    public static void main(String[] args) throws IOException, InterruptedException {
        // Set up the data source
        dataSource = new BasicDataSource();
        dataSource.setUrl(JDBC_URL);
        dataSource.setUsername(JDBC_USER);
        dataSource.setPassword(JDBC_PASSWORD);
        dataSource.setMinIdle(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxOpenPreparedStatements(100);

        // Set up the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        System.out.println("Basic Http VT Server started...");
        HttpContext context = server.createContext("/", new CrudHandler());

        // Start the HTTP server
        server.start();
        // Schedule performance monitoring task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(Main::monitorPerformance, 0, 5, TimeUnit.SECONDS);
    }

    private static void monitorPerformance() {
        //Track CPU and memory usage
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = osBean.getSystemLoadAverage();
        System.out.println("CPU Usage: " + cpuUsage + "%");
        //calc runtime and mem
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        //print out stuff
//        System.out.println("CPU Usage: " + cpuUsage + "%");
//        System.out.println("Used Memory: " + usedMemory / (1024 * 1024) + " MB");
//        System.out.println("Max Memory: " + maxMemory / (1024 * 1024) + " MB");

        //Add to csv file
        writeToFile("cpuusage.csv", cpuUsage + "");
        writeToFile("ramusage.csv", usedMemory / (1024 * 1024) + "," + maxMemory / (1024 * 1024));
    }
    //add the to files
    private static void writeToFile(String fileName, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(data);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class CrudHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Runnable run = new Runnable() {
                @Override
                public void run() {


                    String requestMethod = exchange.getRequestMethod();
                    try {


                        if (requestMethod.equalsIgnoreCase("GET")) {
                            handleGetRequest(exchange);
                        } else if (requestMethod.equalsIgnoreCase("POST")) {
                            handlePostRequest(exchange);
                        } else if (requestMethod.equalsIgnoreCase("PUT")) {
                            handlePutRequest(exchange);
                        } else if (requestMethod.equalsIgnoreCase("DELETE")) {
                            handleDeleteRequest(exchange);
                        } else {
                            sendResponse(exchange, 400, "Bad Request");
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            };
            //Virtual Threads
            Thread.startVirtualThread(run);
            //Normal Threads
//            Thread thread = new Thread(run);
//            thread.start();
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            //make get stuff
//            LOGGER.info("JDBS GET  Thread info {} ", Thread.currentThread());
            // Implement your GET logic here (e.g., retrieve data from the database)
            try (Connection connection = dataSource.getConnection()) {
                String query = "SELECT * FROM tempatures";
                try (PreparedStatement statement = connection.prepareStatement(query);
                     ResultSet resultSet = statement.executeQuery()) {

                    StringBuilder response = new StringBuilder();
                    while (resultSet.next()) {
                        int year = resultSet.getInt("year");
                        double temp = resultSet.getDouble("temp");
                        response.append(String.format("Year: %d, Temp: %f%n", year, temp));
                    }

                    sendResponse(exchange, 200, response.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error");
            }
            //sendResponse(exchange, 200, "GET request handled");
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            //make post thing
            sendResponse(exchange, 200, "POST request handled");
        }

        private void handlePutRequest(HttpExchange exchange) throws IOException {
            //make a put request
            sendResponse(exchange, 200, "PUT request handled");
        }

        private void handleDeleteRequest(HttpExchange exchange) throws IOException {
            //make a delete
            sendResponse(exchange, 200, "DELETE request handled");
        }
        //Send a responsed back to confirm that message was good
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


}