import com.fazecast.jSerialComm.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Replace with the name of your serial port
        String portName = "COM2"; // Example on Linux, may vary on other platforms

        // Replace with the path to your text file
        String filePath = "C:\\Users\\Tj\\Downloads\\sys6_realtime_tv (1).txt";

        // Open the serial port
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600); // Adjust the baud rate as needed

        if (serialPort.openPort()) {
            try {
                // Read and send each line from the text file
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                String line;

                while ((line = br.readLine()) != null) {
                    // Send the line over the serial port
                    serialPort.getOutputStream().write(line.getBytes());
                    serialPort.getOutputStream().write('\n'); // Add a newline character
                    serialPort.getOutputStream().flush();
                    System.out.println("Sent: " + line);
                    Thread.sleep(10); // Adjust the delay between lines if needed
                }

                br.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                serialPort.closePort();
            }
        } else {
            System.err.println("Failed to open the serial port.");
        }
    }
}
