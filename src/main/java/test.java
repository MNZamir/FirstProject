import com.sun.management.OperatingSystemMXBean;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DashboardId;
import oshi.SystemInfo;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Scanner;
import java.util.UUID;

public class test {

    static double load;
    static double memory;
    static double temper;
    static long epocTime;
    static String customerName;
    static String deviceName;
    static String deviceType;

    public static void readTextFile() throws IOException {
        try {
            File myObj = new ClassPathResource("thingsboard.txt").getFile();
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                customerName = myReader.nextLine();
                deviceName = myReader.nextLine();
                deviceType = myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void clientOperation(){
        // ThingsBoard REST API URL
        String url = "http://localhost:8080";

        // Default Tenant Administrator credentials
        String username = "tenant@thingsboard.org";
        String password = "tenant";

        // Creating new rest client and auth with credentials
        RestClient client = new RestClient(url);
        client.login(username, password);

        // Creating Customer
        Customer customer;
        customer = newCustomer(client);
        UUID id = UUID.fromString("7bab30a0-9f66-11ec-928f-11f63de9ad6f");
        DashboardId dashboardId = new DashboardId(id);
        client.assignDashboardToCustomer(customer.getId(),dashboardId);

        // Create Device and Assign device to customer
        newDevice(deviceName, deviceType, customer, client);

// Perform logout of current user and close the client
        client.logout();
        client.close();
    }

    public static void getCPUTemp(){
        SystemInfo systemInfo = new SystemInfo();
        temper = systemInfo.getHardware().getSensors().getCpuTemperature();
    }

    public static void getCPUUsage(){
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        load = osBean.getSystemCpuLoad();
//                    System.out.println("CPU Load : " + String.format("%.2f", load* 100) + " %");
    }

    public static void getMemoryUsed(){
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);
        long totalMemoryUsed = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
        memory = (double) totalMemoryUsed/osBean.getTotalPhysicalMemorySize() *100;
    }


    public static void postHTTP(URL url, JSONObject object) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type","application/json");
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(object.toString());
        wr.flush();
        wr.close();

//        BufferedReader iny = new BufferedReader(new InputStreamReader(con.getInputStream()));
//        String output;
//        StringBuffer response = new StringBuffer();
//
//        while ((output = iny.readLine()) != null) {
//            response.append(output);
//        }
//        iny.close();
    }

    public static void makeJson(JSONObject object){
        JSONObject value = new JSONObject();
        object.put("ts", epocTime);
        object.put("values", value);

        value.put("CPU Usage",String.format("%.2f", load* 100));
        value.put("CPU Temp",String.format("%.2f", temper));
        value.put("Memory Usage",String.format("%.2f", memory));

        StringWriter output = new StringWriter();
        object.write(output);

        System.out.println(object);
    }

    public static Customer newCustomer(RestClient client){
        // Creating Customer 1
        Customer customer = new Customer();
        customer.setTitle(customerName);
        customer = client.saveCustomer(customer);
        return customer;
    }

    public static Device newDevice(String deviceName,String deviceType, Customer customer, RestClient client){
        Device device = new Device();
        device.setName(deviceName);
        device.setType(deviceType);
        device.setCustomerId(customer.getId());
        device = client.saveDevice(device);

        return device;
    }

    public static void getEpoch(){
        epocTime = Instant.now().getEpochSecond();
    }

    public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException {

        String url = "http://localhost:8080/api/v1/DFgrIudMh0PbSjKYguuG/telemetry";
        URL obj = new URL(url);
        JSONObject object = new JSONObject();

        readTextFile();

        System.out.println("Customer Name : " + customerName);
        System.out.println("Device Name : " + deviceName);
        System.out.println("Device TYPE : " + deviceType);

//        clientOperation();

//        System.out.println("Stream Start...");

        while(true){
            getEpoch();
            getCPUTemp();
            getCPUUsage();
            getMemoryUsed();
            makeJson(object);
            postHTTP(obj, object);
            long millis = System.currentTimeMillis();
            Thread.sleep(1000 - millis % 1000);
        }




//         Get memory usage
//        OperatingSystemMXBean systemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
//
//        double totalMemory = (double) systemMXBean.getTotalPhysicalMemorySize() / 1073741824;
//        double freeMemory = (double) systemMXBean.getFreePhysicalMemorySize() / 1073741824;
//        double usedMemory = totalMemory - freeMemory;
//        double memoryperc= usedMemory/totalMemory;
//        System.out.printf("Total memory: %.2f GB\n", totalMemory);
//        System.out.printf("Free memory: %.2f GB\n", freeMemory);
//        System.out.printf("Memory utilization: %.2f \n", memoryperc);
//        System.out.printf("Memory usage: %.2f \n", usedMemory);
//
//        // Get cpu usage
//        double systemCpuLoad = systemMXBean.getSystemCpuLoad() * 100;
//        double processCpuLoad = systemMXBean.getProcessCpuLoad() * 100;
//        System.out.printf("CPU usage: %f", systemMXBean.getProcessCpuLoad());

//        Components components = JSensors.get.components();
//
//        List<Cpu> cpus = components.cpus;
//        if (cpus != null) {
//            for (final Cpu cpu : cpus) {
//                System.out.println("Found CPU component: " + cpu.name);
//                if (cpu.sensors != null) {
//                    System.out.println("Sensors: ");
//
//                    //Print temperatures
//                    List<Temperature> temps = cpu.sensors.temperatures;
//                    for (final Temperature temp : temps) {
//                        System.out.println(temp.name + ": " + temp.value + " C");
//                    }
//
//                    //Print fan speed
//                    List<Fan> fans = cpu.sensors.fans;
//                    for (final Fan fan : fans) {
//                        System.out.println(fan.name + ": " + fan.value + " RPM");
//                    }
//                }
//            }
//        }
    }

}
