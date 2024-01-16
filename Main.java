import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static JTextArea selectedText;
    public static JTextArea fileSelectText;
    public static ArrayList<String> filePaths = new ArrayList<>();
    public static JProgressBar loadingBar;
    public static JPanel availableList;
    public static String localIP = null;
    public static int infoPort = 80;
    public static int filePort = 997;
    public static ArrayList<JButton> availableIPs = new ArrayList<>();
    public static String username = "user";
    public static boolean type10 = false;
    public static JFrame mainFrame;
    public static String outputdir;
    public static JTextArea noticesText;
    public static ArrayList<Integer> rejected = new ArrayList<>();
    public static JButton sendButton;

    public static void main(String[] args) throws SocketException, FileNotFoundException {
        Scanner config = new Scanner(new File("config.ini"));
        while(config.hasNextLine()){
            String parameter = config.nextLine();
            String parameterName = parameter.split("=")[0];
            String parameterValue = parameter.split("=")[1];
            if(parameterName.equals("ip")) localIP = parameterValue;
            else if(parameterName.equals("type10")) type10 = parameterValue.equals("false")?false:true;
            else if(parameterName.equals("username")) username = parameterValue;
            else if(parameterName.equals("outputdir")) outputdir = parameterValue;
        }

        FlatDarkLaf.setup();
        availableList = new JPanel(new MigLayout("fillx"));
        mainFrame = new JFrame("File Transfer - 1.0.4");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(false);
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
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            if(fileSelectText.getText().equals("Drag and drop here.")){
                noticesText.setText(noticesText.getText() + "[ ! ] Upload at least on file before sending.\n");
                return;
            }
            if(selectedText.getText().equals("Selected:")){
                noticesText.setText(noticesText.getText() + "[ ! ] Select an IP before sending.\n");
                return;
            }
            new MainFileSender();
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

        JPanel loadingBarPanel = new JPanel(new MigLayout("insets 0", "grow"));
        loadingBar = new JProgressBar();
        setSizeOfComponent(loadingBar, 400, 25);
        loadingBar.setStringPainted(true);
        loadingBar.setBorderPainted(true);
        loadingBarPanel.add(loadingBar, "left");

        JButton addIPButton = new JButton("+");
        addIPButton.addActionListener(e -> {
            JPanel panel = new JPanel(new FlowLayout());
            JTextArea addIP = new JTextArea();
            setSizeOfComponent(addIP, 200, 20);
            JTextArea addColon = new JTextArea(":");
            setSizeOfComponent(addColon, 10, 23);
            addColon.setEditable(false);
            JTextArea addUsername = new JTextArea();
            addUsername.setAlignmentX(JTextField.CENTER);
            addUsername.setAlignmentY(JTextField.CENTER);
            setSizeOfComponent(addUsername, 100, 20);
            panel.add(addIP);
            panel.add(addColon);
            panel.add(addUsername);

            int res = JOptionPane.showOptionDialog(null, panel, "Add IP", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null, null);

            if(res==0 && !addIP.getText().isEmpty() && !addUsername.getText().isEmpty()) {
                JButton button = new JButton(addIP.getText() + ":" + addUsername.getText());
                button.addActionListener(e2 -> {
                    if(Main.selectedText.getText().contains(addIP.getText())) return;
                    if(Main.selectedText.getText().equals("Selected:"))
                        Main.selectedText.setText(Main.selectedText.getText() + " " + addIP.getText());
                    else Main.selectedText.setText(Main.selectedText.getText() + ", " + addIP.getText());
                });
                button.addMouseListener(new PopClickListener());
                availableIPs.add(button);
                updateIPs();
                mainFrame.setVisible(true);
                mainFrame.repaint();
            }
        });
        addIPButton.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> {
            JFrame frame = new JFrame("Settings");
            JPanel panel = new JPanel(new MigLayout("fill", "[fill, grow]"));

            JTextArea usernameText = new JTextArea("Username");
            usernameText.setEnabled(false);
            JTextArea usernameInput = new JTextArea(username);
            panel.add(usernameText, "split 2");
            panel.add(usernameInput, "wrap");

            JTextArea IPText = new JTextArea("Local IP");
            IPText.setEnabled(false);
            JTextArea IPInput = new JTextArea(localIP);
            panel.add(IPText, "split 2");
            panel.add(IPInput, "wrap");


            JTextArea outputText = new JTextArea("Output directory");
            outputText.setEnabled(false);

            JButton chooseDirectoryButton = new JButton("Choose");
            JTextArea directoryTextArea = new JTextArea(outputdir);
            directoryTextArea.setEnabled(false);
            chooseDirectoryButton.addActionListener(event -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showDialog(null, "Choose Directory");

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    directoryTextArea.setText(selectedDirectory.getAbsolutePath());
                }
                frame.pack();
            });
            panel.add(outputText, "split 3");
            panel.add(directoryTextArea);
            panel.add(chooseDirectoryButton, "wrap");

            JTextArea type10Text = new JTextArea("Type10");
            type10Text.setEnabled(false);
            String[] choices = {"true","false"};
            JComboBox type10Input = new JComboBox(choices);
            if(type10) type10Input.setSelectedIndex(0);
            else type10Input.setSelectedIndex(1);
            panel.add(type10Text, "split 2");
            panel.add(type10Input, "wrap");

            JButton save = new JButton("Save");
            save.addActionListener(e1 -> {
                username = usernameInput.getText();
                localIP = IPInput.getText();
                outputdir = directoryTextArea.getText();
                type10 = (type10Input.getSelectedItem().toString()).equals("false")?false:true;
                frame.setVisible(false);
                try {
                    Scanner scanner = new Scanner(new File("config.ini"));
                    String content = "";
                    while(scanner.hasNext()){
                        content += scanner.nextLine() + "\n";
                    }
                    FileWriter fw = new FileWriter("config.ini",false);
                    for(String line:content.split("\n")){
                        String parameterName = line.split("=")[0];
                        if(parameterName.equals("username")) fw.write(parameterName + "=" + username + "\n");
                        else if(parameterName.equals("ip")) fw.write(parameterName + "=" + localIP + "\n");
                        else if(parameterName.equals("outputdir")) fw.write(parameterName + "=" + outputdir + "\n");
                        else if(parameterName.equals("type10")) fw.write(parameterName + "=" + type10 + "\n");
                    }
                    fw.close();
                    sendUsernameChange(username);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            panel.add(save, "span 2");

            frame.add(panel);
            frame.setVisible(true);
            frame.pack();
            frame.setLocationRelativeTo(null);
        });
        settingsButton.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel mainRightPanel = new JPanel(new MigLayout("fill", "[fill, grow]"));
        JPanel mainIPsPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", "[fill, grow]"));
        availableList.setBorder(sendButton.getBorder());
        availableList.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane IPsScrollPane = new JScrollPane(availableList);
        IPsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        IPsScrollPane.setPreferredSize(new Dimension(200, 170));
        IPsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainIPsPanel.add(IPsScrollPane);
        mainIPsPanel.setBorder(sendButton.getBorder());
        mainRightPanel.add(mainIPsPanel, "top, wrap");
        mainRightPanel.add(settingsButton, "left, split 2");
        mainRightPanel.add(addIPButton, "al right, wrap");

        JPanel noticesPanel = new JPanel(new MigLayout("fill, insets 0", "[fill, grow]", "[fill, grow]"));
        noticesPanel.setBorder(sendButton.getBorder());
        setSizeOfComponent(noticesPanel, 205, 170);
        noticesText = new JTextArea("[ + ] Successfully connected to the network.\n");
        noticesText.setLineWrap(true);
        noticesText.setWrapStyleWord(true);
        noticesText.setEditable(false);
        JScrollPane noticesScrollPane = new JScrollPane(noticesText);
        noticesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        noticesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        noticesPanel.add(noticesScrollPane);
        mainRightPanel.add(noticesPanel, "bottom, width 100%");

        mainPanel.add(selectedPanel);
        mainPanel.add(mainRightPanel, "span 1 3, wrap");
        mainPanel.add(fileSelectPanel, "wrap");
        mainPanel.add(loadingBarPanel, "span 2 1");

        mainFrame.add(mainPanel);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    public static void sendUsernameChange(String newUsername) throws IOException {
        DatagramSocket ds = Bomber.ds;
        byte[] data = ("*" + localIP + ":" + newUsername).getBytes();
        for(JButton button:availableIPs){
            String targetIP = button.getText().split(":")[0];
            DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName(targetIP), infoPort);
            ds.send(dp);
        }
    }
    public static void updateIPs(){
        availableList.removeAll();
        for(JButton IP:availableIPs){
            availableList.add(IP, "wrap, width 185, top center");
        }
    }

    public static void setSizeOfComponent(JComponent o, int width, int height){
        o.setMinimumSize(new Dimension(width, height));
        o.setMaximumSize(new Dimension(width, height));
        o.setPreferredSize(new Dimension(width, height));
    }

    public static void sendDisconnect(){
        DatagramSocket ds = Bomber.ds;
        byte[] buf = ("-" + Main.username).getBytes();

        for(JButton button:availableIPs){
            if(button.getText().split(":").length==0 || !button.getText().split(":")[0].matches("(\\b25[0-5]|\\b2[0-4][0-9]|\\b[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}")) continue;
            try {
                String ip = button.getText().split(":")[0];
                DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), infoPort);
                ds.send(dp);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
class MainFileSender extends Thread{
    static int index;

    MainFileSender(){
        start();
        index = Main.rejected.size();
        Main.rejected.add(0);
    }

    @Override
    public void run(){
        Main.sendButton.setEnabled(false);
        String source = Main.selectedText.getText().replace("Selected: ", "");
        String[] IPs = source.split(", ");
        ArrayList<FileSender> array = new ArrayList<>();
        for(String IP:IPs){
            array.add(new FileSender(IP, index));
        }
        for(FileSender fs:array){
            try {
                fs.join(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(!(Main.rejected.get(index)==IPs.length))
            Main.loadingBar.setValue(100);
        Main.fileSelectText.setText("Drag and drop here.");
        Main.selectedText.setText("Selected:");
        Main.filePaths.clear();
        if(!(Main.rejected.get(index)==IPs.length))
            Main.noticesText.setText(Main.noticesText.getText() + "[ + ] Sent " + (Main.filePaths.size() == 1 ? "file" : "files") +
                    " to " + (IPs.length-Main.rejected.get(index)) + " " + (IPs.length==1?"user":"users") + ".\n");
        Main.sendButton.setEnabled(true);
        Main.mainFrame.repaint();
        Main.mainFrame.setVisible(true);
        Main.rejected.remove(index);
    }
}
class FileSender extends Thread{
    String IP;
    int rejectedIndex;
    FileSender(String IP, int rejectedIndex){
        this.IP = IP;
        this.rejectedIndex = rejectedIndex;
        start();
    }

    @Override
    public void run(){
        try {
            Socket s = new Socket(IP, Main.filePort);
            System.out.println("inviato " + IP);
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            DataInputStream dis = new DataInputStream(s.getInputStream());
            dos.writeUTF(String.valueOf(Main.filePaths.size()));
            String answer = dis.readUTF();
            if(answer.equals("no")) {
                Main.noticesText.setText(Main.noticesText.getText() + "[ ! ] User " + IP + " rejected your " + (Main.filePaths.size() == 1 ? "file" : "files") + ".\n");
                Main.rejected.set(rejectedIndex, (Main.rejected.get(rejectedIndex))+1);
                Main.selectedText.setText(Main.selectedText.getText().replace(", " + IP, ""));
                Main.selectedText.setText(Main.selectedText.getText().replace(IP + ", ", ""));
                s.close();
                this.interrupt();
                return;
            }
            ArrayList<String> filePaths = Main.filePaths;
            for(String path:filePaths) {
                File file = new File(path);
                dos.writeUTF(file.getName());
                dos.writeInt((int)file.length());
                byte[] bytes = new byte[(int)file.length()];
                try(FileInputStream fis = new FileInputStream(file)) {
                    fis.read(bytes);
                }
                dos.write(bytes);
                dos.flush();
                dis.readUTF();
            }
            s.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        this.interrupt();
    }
}

class PopClickListener extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            doPop(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            doPop(e);
    }

    private void doPop(MouseEvent e) {
        JButton button = (JButton) e.getSource();
        PopUpIP menu = new PopUpIP(button.getText().split(":")[0], button.getText().split(":")[1]);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
}

class PopUpIP extends JPopupMenu {
    JMenuItem anItem;
    public PopUpIP(String IP, String name) {
        anItem = new JMenuItem("Send message");
        JPanel panel = new JPanel();
        JTextArea message = new JTextArea();
        Main.setSizeOfComponent(message, 200, 20);
        panel.add(message);
        anItem.addActionListener(e -> {
            int res = JOptionPane.showOptionDialog(null, panel, "Send message to " + IP + ":" + name, JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null, null);
            if(res==0 && !message.getText().isEmpty()){
                try {
                    String messageText = "+" + Main.username + ": " + message.getText();
                    DatagramPacket dp = new DatagramPacket(messageText.getBytes(), messageText.getBytes().length, InetAddress.getByName(IP), Main.infoPort);
                    DatagramSocket ds = Bomber.ds;
                    ds.send(dp);
                    Main.noticesText.setText(Main.noticesText.getText() + Main.username + ": " + message.getText() + "\n");
                    Main.mainFrame.repaint();
                    Main.mainFrame.setVisible(true);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        });
        add(anItem);
    }
}
class DragListener extends DropTarget {
    @Override
    public void drop(DropTargetDropEvent dtde) {
        Main.filePaths.clear();
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable t = dtde.getTransferable();
        DataFlavor[] df = t.getTransferDataFlavors();
        for(DataFlavor f:df){
            try{
                if(f.isFlavorJavaFileListType()){
                    List<File> filesData = (List<File>)t.getTransferData(f);
                    int i=0;
                    for(File file : filesData){
                        if(i==0) Main.fileSelectText.setText(file.getName());
                        else Main.fileSelectText.setText(Main.fileSelectText.getText() + ", " + file.getName());
                        Main.filePaths.add(file.getPath());
                        i++;
                    }
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
            ServerSocket ss = new ServerSocket(Main.filePort);
            while(true){
                Socket s = ss.accept();
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                int filesNumber = Integer.parseInt(dis.readUTF());
                JPanel optionPanel = new JPanel(new GridLayout(2, 1));
                JTextArea optionPanelText = new JTextArea(s.getInetAddress().toString().replace("/", "") + " wants to send you " + filesNumber + " " + (filesNumber==1?"file":"files") + ".\nDo you want to accept " + (filesNumber==1?"it":"them") + "?");
                optionPanelText.setEnabled(false);
                optionPanel.add(optionPanelText);
                JTextArea timerText = new JTextArea("10 seconds left");
                timerText.setEnabled(false);
                optionPanel.add(timerText);
                Timer timer = new Timer(1000 , new ActionListener() {
                    int timeLeft = 11;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (timeLeft <= 1) {
                            Window win = SwingUtilities.getWindowAncestor(optionPanel);
                            win.setVisible(false);
                            try {
                                dos.writeUTF("no");
                                dos.close();
                                dis.close();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            ((Timer)e.getSource()).stop();
                        } else{
                            timeLeft--;
                            timerText.setText(timeLeft + " " + (timeLeft==1?"second":"seconds") + " left");
                        }
                    }
                });
                timer.setInitialDelay(0);
                timer.start();
                int result = JOptionPane.showConfirmDialog(null, optionPanel, "Received file.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch (result) {
                    case JOptionPane.CANCEL_OPTION:
                        dos.writeUTF("no");
                        timer.stop();
                        s.close();
                        continue;
                }
                timer.stop();
                dos.writeUTF("yes");
                for(int i=0; i<filesNumber; i++){
                    String fileName = dis.readUTF();
                    int fileSize = dis.readInt();
                    byte[] data = new byte[fileSize];
                    dis.readFully(data);
                    try (FileOutputStream fos = new FileOutputStream(Main.outputdir + "/" + fileName)) {
                        fos.write(data);
                    }
                    dos.writeUTF("done");
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
        ds = new DatagramSocket(Main.infoPort);
        start();
    }

    @Override
    public void run(){
        try {
            byte[] buf = Main.username.getBytes();
            if(Main.type10) {
                for (int i = 20; i < 25; i++) {
                    for (int j = 0; j < 253; j++) {
                        if (i == IP.third && j == IP.fourth) continue;
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("10.0." + i + "." + j), Main.infoPort);
                        ds.send(packet);
                    }
                }
                for (int i = 149; i < 151; i++) {
                    for (int j = 0; j < 253; j++) {
                        if (i == IP.third && j == IP.fourth) continue;
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("10.0." + i + "." + j), Main.infoPort);
                        ds.send(packet);
                    }
                }
            } else {
                for (int i = 1; i < 3; i++) {
                    for (int j = 0; j < 253; j++) {
                        if (i == IP.third && j == IP.fourth) continue;
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("192.168." + i + "." + j), Main.infoPort);
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

                if(name.startsWith("*")){
                    String targetIP = name.replace("*", "").split(":")[0];
                    String newUsername = name.replace("*", "").split(":")[1];
                    int i=0;
                    for(JButton button:Main.availableIPs){
                        String currentIP = button.getText().split(":")[0];
                        if(currentIP.equals(targetIP)) Main.availableIPs.get(i).setText(currentIP + ":" + newUsername);
                        i++;
                    }
                    Main.mainFrame.repaint();
                    Main.mainFrame.setVisible(true);
                    continue;
                }

                if(name.startsWith("+")){
                    String message = name.replace("+", "");
                    Main.noticesText.setText(Main.noticesText.getText() + message + "\n");
                    Main.mainFrame.repaint();
                    Main.mainFrame.setVisible(true);
                    continue;
                }
                String ipReceived = packet.getAddress().toString().replace("/", "");

                if(name.startsWith("-")){
                    int i=0;
                    for(JButton button:Main.availableIPs){
                        String ip = button.getText().split(":")[0];
                        if(ip.equals(ipReceived)){
                            Main.availableIPs.remove(i);
                            if(!Main.selectedText.getText().equals("Selected:")){
                                if(Main.selectedText.getText().contains(ipReceived)) {
                                    Main.selectedText.setText(Main.selectedText.getText().replace(ipReceived + ", ", ""));
                                    Main.selectedText.setText(Main.selectedText.getText().replace(" " + ipReceived, ""));
                                }
                            }
                            break;
                        }
                        i++;
                    }
                    Main.updateIPs();
                    Main.mainFrame.repaint();
                    Main.mainFrame.setVisible(true);
                    continue;
                }

                if(name.isEmpty()) continue;
                if(arrayContains(Main.availableIPs, ipReceived)){
                    int i=0;
                    for(JButton button:Main.availableIPs){
                        if(button.getText().split(":")[0].equals(ipReceived)){
                            Main.availableIPs.remove(i);
                            break;
                        }
                        i++;
                    }
                }

                JButton button = new JButton((ipReceived + ":" + name.replace("!", "")));
                button.addActionListener(e -> {
                    if(Main.selectedText.getText().contains(ipReceived)) return;
                    if(Main.selectedText.getText().equals("Selected:"))
                        Main.selectedText.setText(Main.selectedText.getText() + " " + ipReceived);
                    else Main.selectedText.setText(Main.selectedText.getText() + ", " + ipReceived);
                });
                button.addMouseListener(new PopClickListener());
                Main.availableIPs.add(button);
                Main.updateIPs();
                Main.mainFrame.repaint();
                Main.mainFrame.setVisible(true);

                if(!name.startsWith("!")){
                    byte[] bufSend = ("!" + Main.username).getBytes();
                    DatagramPacket packetSend = new DatagramPacket(bufSend, bufSend.length, InetAddress.getByName(ipReceived), Main.infoPort);
                    ds.send(packetSend);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean arrayContains(ArrayList<JButton> array, String IP){
        for(JButton button:array){
            if(IP.equals(button.getText().split(":")[0])) return true;
        }
        return false;
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
        Main.localIP = ip;
        third = Integer.parseInt(ip.split("\\.")[2]);
        fourth = Integer.parseInt(ip.split("\\.")[3]);
    }
}