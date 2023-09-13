import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.util.Properties;
public class Main {

    public static void main(String[] args) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("Config.txt")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Accessing configuration values
        String comPort1 = properties.getProperty("port.name");
        int BaudRate = Integer.parseInt(properties.getProperty("port.baud"));
        String vmixUrl = properties.getProperty("vmix.url");
        int startCamNumber = Integer.parseInt(properties.getProperty("vmix.StartCamNumber"));
        int WideNumber = Integer.parseInt(properties.getProperty("vmix.WideCamNumber"));
        String testdebug = properties.getProperty("Debug.enabled");

        boolean debug = false;
        if(testdebug.equals("true")){
            debug=true;
        }
        else {
            debug=false;
        }

        if (debug) {
            // Printing configuration values
            System.out.println("Listed below is the list of values pulled form the Config:");
            System.out.println("Com Port: " + comPort1);
            System.out.println("Baud Rate: " + BaudRate);
            System.out.println("Vmix Ip And Port: " + vmixUrl);
            System.out.println("The Input number of your wide shot: " + startCamNumber);
            System.out.println("The Input number of your start shot: " + WideNumber);
        }

        String comPortName = comPort1; // Replace with the actual COM port name

        int inputNumber = 1;
        SerialPort comPort = SerialPort.getCommPort(comPortName);
        comPort.setBaudRate(BaudRate);

        if (comPort.openPort()) {
            System.out.println("Port opened successfully.");
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0); // Adjust timeout as needed
            InputStream inputStream = comPort.getInputStream();

            StringBuilder messageBuilder = new StringBuilder();
            boolean receivingData = false;

            try {
                while (true) {
                    String lane = "";
                    try {
                        int data = inputStream.read();
                        if (data == -1) {
                            if(debug) {
                                System.out.println("No more data to read.");
                            }
                            break; // End of stream
                        }

                        char c = (char) data;

                        if (c == 1) { // <SOH>
                            messageBuilder.setLength(0);
                            receivingData = true; // Start receiving data
                        } else if (c == 4 && receivingData) { // <EOT> when receiving data
                            String message = messageBuilder.toString().trim();
                            //System.out.println("Received Data: " + message);

                            if (message.startsWith("SR") && message.length() >= 7) {
                                String mm_ss_t = message.substring(2,9); // Extract mm:ss.t including the decimal part
                                if(debug) {
                                    System.out.println(mm_ss_t);
                                }
                                if(mm_ss_t.equals("  :  .0")) {
                                    cut(vmixUrl, startCamNumber, debug);
                                    CutWithDelay(5000,vmixUrl, WideNumber,debug);
                                }

                            } else {
                                // Use a regular expression to extract relevant information
                                Pattern pattern = Pattern.compile("FT\\s*(\\d+)\\s*(\\d+)\\s*((?:\\d+:)?\\d{2}\\.\\d{2})");
                                Matcher matcher = pattern.matcher(message);

                                if (matcher.find()) {
                                    lane = matcher.group(1);
                                    String place = matcher.group(2);
                                    String mm_ss_tt = matcher.group(3);
                                    cut(vmixUrl, startCamNumber, debug );
                                    if(debug) {
                                        System.out.println("Score Data: Lane " + lane + ", Place " + place + ", Time " + mm_ss_tt);
                                    }
                                }
                            }

                            receivingData = false; // Stop receiving after processing
                        } else if (receivingData) {
                            messageBuilder.append(c);
                        }
                    } catch (SerialPortTimeoutException timeoutException) {
                        // Handle timeout exception (no data received)
                        //System.out.println("Timeout: No data received.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                comPort.closePort();
                System.out.println("Port closed.");
            }
        } else {
            System.err.println("Failed to open the port.");
        }
    }
    // Method to cut to a specific input in vMix
    public static void cut (String vmixApiUrl, int Input,boolean debug){
        try {
            cutToInput(vmixApiUrl, Input, debug );
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void cutToInput(String vmixApiUrl, int inputNumber, boolean debug) throws Exception {
        try {
            // Create a URL object for the vMix API endpoint
            URL url = new URL("http://"+vmixApiUrl + "/API/?Function=CutDirect&input=" + inputNumber);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Get the response from the vMix API
            int responseCode = connection.getResponseCode();
            if(debug) {
                System.out.println(connection.getResponseCode());
            }
            // Close the connection
            connection.disconnect();

            // Return true if the request was successful (HTTP status code 200)

        } catch (Exception e) {
            e.printStackTrace();
            // Return false if an exception occurred

        }
    }
    private static void CutWithDelay(int delay, String vmixUrl, int cam, boolean debug) {
        // Create a thread to print "test the test2" after 1 second
        Thread delayedThread = new Thread(() -> {
            try {
                Thread.sleep(delay); // Delay for 1 second
                cut(vmixUrl, cam, debug);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start the delayed thread
        delayedThread.start();
    }

}
