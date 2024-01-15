import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static JTextArea selectedText;
    public static JTextArea fileSelectText;
    public static JProgressBar loadingBar;
    public static JPanel availableList;
    public static String localIP = null;
    public static ArrayList<JButton> availableIPs = new ArrayList<>();
    public static String username = "user";
    public static boolean type10 = false;
    public static JFrame mainFrame;
    public static String outputdir;

    public static void main(String[] args) throws SocketException, FileNotFoundException {
        Scanner config = new Scanner(new File("config.ini"));
        while(config.hasNextLine()){
            String parameter = config.nextLine();
            String parameterName = parameter.split("=")[0];
            String parameterValue = parameter.split("=")[1];
            if(parameterName.equals("ip")) localIP = parameterValue;
            else if(parameterName.equals("type10")) type10 = !parameterValue.equals("false");
            else if(parameterName.equals("username")) username = parameterValue;
            else if(parameterName.equals("outputdir")) outputdir = parameterValue;
        }

        FlatDarkLaf.setup();
        availableList = new JPanel(new MigLayout("fillx"));
        mainFrame = new JFrame("File Transfer - 1.0.0");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                sendDisconnect();
            }
        });

        new IP();
        new Receiver();
        new Bomber();
        new Populater();


        JPanel mainPanel = new JPanel(new MigLayout("fillx", "[fill, grow][]"));

        JPanel selectedPanel = new JPanel(new MigLayout("fillx", "[fill]"));
        selectedText = new JTextArea("Selected:");
        selectedText.setEnabled(false);
        selectedPanel.add(selectedText);
        JButton sendButton = new JButton("Send file");
        sendButton.addActionListener(e -> {
            if(fileSelectText.getText().equals("Drag and drop here.")){
                return;
            }
            if(selectedText.getText().equals("Selected:")){
                return;
            }
            try {
                sendFile(fileSelectText.getText());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        selectedPanel.add(sendButton, "right, width 100!");

        JPanel fileSelectPanel = new JPanel(new MigLayout("fill"));
        setSizeOfComponent(fileSelectPanel, 400, 300);
        DragListener dl1 = new DragListener();
        fileSelectPanel.setDropTarget(dl1);
        fileSelectText = new JTextArea("Drag and drop here.");
        DragListener dl2 = new DragListener();
        fileSelectText.setDropTarget(dl2);
        fileSelectText.setEnabled(false);
        fileSelectPanel.add(fileSelectText, "center center");

        JPanel loadingBarPanel = new JPanel();
        loadingBar = new JProgressBar();
        setSizeOfComponent(loadingBar, 400, 25);
        loadingBar.setStringPainted(true);
        loadingBar.setBorderPainted(true);
        loadingBarPanel.add(loadingBar);

        setSizeOfComponent(availableList, 200, 400);
        availableList.setBorder(sendButton.getBorder());
        selectedText.setEnabled(false);
        for(JButton IP:availableIPs){
            availableList.add(IP, "wrap, width 100%, top");
        }

        mainPanel.add(selectedPanel);
        mainPanel.add(availableList, "span 1 3, wrap");
        mainPanel.add(fileSelectPanel, "wrap");
        mainPanel.add(loadingBarPanel);

        mainFrame.add(mainPanel);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }
    public static void updateIPs(){
        availableList.removeAll();
        for(JButton IP:availableIPs){
            availableList.add(IP, "wrap, width 100%, top");
        }
    }

    public static void setSizeOfComponent(JComponent o, int width, int height){
        o.setMinimumSize(new Dimension(width, height));
        o.setMaximumSize(new Dimension(width, height));
        o.setPreferredSize(new Dimension(width, height));
    }

    public static void sendFile(String path) throws IOException {
        loadingBar.setValue(0);
        String IP = Main.selectedText.getText().replace("Selected: ", "");
        Socket s = new Socket(IP, 997);
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        dos.writeUTF((new File(path).getName()));
        dos.write(Files.readAllBytes(Paths.get(path)));
        loadingBar.setValue(100);
        s.close();
        dos.flush();
    }

    public static void sendDisconnect(){
        DatagramSocket ds = Bomber.ds;
        byte[] buf = ("-" + Main.username).getBytes();

        for(JButton button:availableIPs){
            String ip = button.getText().split(":")[0];
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), 811);
                ds.send(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
class DragListener extends DropTarget {

    @Override
    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable t = dtde.getTransferable();
        DataFlavor[] df = t.getTransferDataFlavors();
        for(DataFlavor f:df){
            try{
                if(f.isFlavorJavaFileListType()){
                    File file = ((java.util.List<File>)t.getTransferData(f)).get(0);
                    Main.fileSelectText.setText(file.getPath());
                }
            }catch (Exception e){
                System.out.println(e);
            }
        }
    }
}

class Receiver extends Thread{

    Receiver(){
        start();
    }

    @Override
    public void run(){
        try {
            ServerSocket ss = new ServerSocket(997);
            while(true){
                Socket s = ss.accept();
                DataInputStream dis = new DataInputStream(s.getInputStream());
                String fileName = dis.readUTF();
                JPanel optionPanel = new JPanel();
                JTextArea optionPanelText = new JTextArea(s.getInetAddress().toString().replace("/", "") + " wants to send you \"" + fileName + "\".\nDo you want to accept it?");
                optionPanelText.setEnabled(false);
                optionPanel.add(optionPanelText);
                int result = JOptionPane.showConfirmDialog(null, optionPanel, "Received file.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch (result) {
                    case JOptionPane.CANCEL_OPTION:
                        s.close();
                        continue;
                }
                byte[] data = dis.readAllBytes();
                try (FileOutputStream fos = new FileOutputStream(Main.outputdir + "\\" + fileName)) {
                    fos.write(data);
                }
                s.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


class Bomber extends Thread{
    public static DatagramSocket ds;

    Bomber() throws SocketException {
        ds = new DatagramSocket(811);
        start();
    }

    @Override
    public void run(){
        try {

            if(Main.type10) {
                for (int i = 20; i < 25; i++) {
                    for (int j = 0; j < 253; j++) {
                        if (i == IP.third && j == IP.fourth) continue;
                        byte[] buf = Main.username.getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("10.0." + i + "." + j), 811);
                        ds.send(packet);
                    }
                }
            } else {
                for (int i = 1; i < 3; i++) {
                    for (int j = 0; j < 253; j++) {
                        if (i == IP.third && j == IP.fourth) continue;
                        byte[] buf = Main.username.getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("192.168." + i + "." + j), 811);
                        ds.send(packet);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class Populater extends Thread{
    public static DatagramSocket ds;

    Populater(){
        ds = Bomber.ds;
        start();
    }

    @Override
    public void run(){
        try {
            while(true){
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                ds.receive(packet);
                String name = new String(buf).trim();
                String ipReceived = packet.getAddress().toString().replace("/", "");

                if(name.contains("-")){
                    int i=0;
                    for(JButton button:Main.availableIPs){
                        String ip = button.getText().split(":")[0];
                        if(ip.equals(ipReceived)){
                            Main.availableIPs.remove(i);
                            break;
                        }
                        i++;
                    }
                    Main.updateIPs();
                    Main.mainFrame.repaint();
                    Main.mainFrame.setVisible(true);
                    continue;
                }
                JButton button = new JButton((ipReceived + ":" + name.replace("!", "")));
                button.addActionListener(e -> Main.selectedText.setText("Selected: " + ipReceived));
                Main.availableIPs.add(button);
                for(JButton IP:Main.availableIPs){
                    Main.availableList.add(IP, "wrap, width 100%, top");
                }
                Main.mainFrame.repaint();
                Main.mainFrame.setVisible(true);

                if(!name.contains("!")){
                    byte[] bufSend = ("!" + Main.username).getBytes();
                    DatagramPacket packetSend = new DatagramPacket(bufSend, bufSend.length, InetAddress.getByName(ipReceived), 811);
                    ds.send(packetSend);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class IP{
    String ip;
    public static int third;
    public static int fourth;
    IP(){
        if(Main.localIP!=null){
            ip = Main.localIP;
        } else if(Main.type10) {
            try {
                if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress().startsWith("10."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress();
                else if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[1].getHostAddress().startsWith("10."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[1].getHostAddress();
                else if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[2].getHostAddress().startsWith("10."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[2].getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        } else{
            try {
                if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress().startsWith("192."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[0].getHostAddress();
                else if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[1].getHostAddress().startsWith("192."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[1].getHostAddress();
                else if (InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[2].getHostAddress().startsWith("192."))
                    ip = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())[2].getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        third = Integer.parseInt(ip.split("\\.")[2]);
        fourth = Integer.parseInt(ip.split("\\.")[3]);
    }
}