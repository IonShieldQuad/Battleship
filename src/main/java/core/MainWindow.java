package core;


import network.BroadcastListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainWindow {
    private static final int SEARCH_PORT = 19008;
    private static final int DATA_PORT = 19009;
    
    private JPanel rootPanel;
    private JTextArea log;
    private JTable table1;
    private JTable table2;
    private JButton validateButton;
    private JButton connectButton;
    private JButton hostButton;
    private JButton refreshButton;
    private JList<InetAddress> serverList;
    
    private TableData data1;
    private TableData data2;
    private boolean editMode;
    private boolean ourTurn = true;
    
    private BroadcastListener broadcastListener;
    private List<InetAddress> addresses = new ArrayList<>();
    private ServerSocket server;
    private Socket connection;
    
    private MainWindow() {
        initComponents();
    }
    
    private void initComponents() {
        data1 = new TableData();
        data2 = new TableData();
    
        initTables();
    
        editMode = true;
        
        validateButton.addActionListener(e -> {
            log.append("\nTable is " + (data1.isValid() ? "valid" : "not valid"));
        });
        
        serverList.setModel(new DefaultListModel<>());
        
        broadcastListener = new BroadcastListener(SEARCH_PORT, false, false);
        broadcastListener.setAddressCollection(addresses);
        broadcastListener.addResponseListener(e -> {
            DefaultTableModel model = (DefaultTableModel) serverList.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                model.removeRow(i);
            }
            for (InetAddress address : addresses) {
                model.addRow(new InetAddress[]{address});
            }
        });
        broadcastListener.start();
        
        refreshButton.addActionListener(e -> {
            addresses.clear();
            broadcastListener.setReceivingResponses(true);
            broadcastListener.sendRequest();
        });
        
        hostButton.addActionListener(e -> {
            if (data1.isValid()) {
                editMode = false;
                broadcastListener.setReceivingRequests(true);
                new Thread(() -> {
                    try {
                        server = new ServerSocket(DATA_PORT);
                        connection = server.accept();
                        log.append("\nConnected");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }).start();
                log.append("\nHosting game...");
            }
            else {
                log.append("\nFailed to host: table is not valid");
            }
        });
        
        connectButton.addActionListener(e -> {
            try {
                editMode = false;
                connect(addresses.get(serverList.getSelectedIndex()));
                log.append("\nConnected");
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                log.append("\n Invalid address selected");
            }
        });
    }
    
    private void connect(InetAddress address) {
        try {
            connection = new Socket(address, DATA_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void initTables() {
        table1.setModel(new DefaultTableModel(data1.size(), data1.size()){
                @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        for (int i = 0; i < data1.size(); i++) {
            for (int j = 0; j < data1.size(); j++) {
                table1.getModel().setValueAt(data1.get(i, j), i, j);
            }
        }
        
        table2.setModel(new DefaultTableModel(data2.size(), data2.size()){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        for (int i = 0; i < data2.size(); i++) {
            for (int j = 0; j < data2.size(); j++) {
                table2.getModel().setValueAt(data2.get(i, j), i, j);
            }
        }
        
        table1.setDefaultRenderer(Object.class, new BattleshipTableCellRenderer());
        BattleshipTableCellRenderer renderer2 = new BattleshipTableCellRenderer();
        renderer2.setShowShips(false);
        table2.setDefaultRenderer(Object.class, renderer2);
    
        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table1.rowAtPoint(e.getPoint());
                int col = table1.columnAtPoint(e.getPoint());
            
                if (editMode) {
                    data1.get(row, col).setHasShip(!data1.get(row, col).hasShip());
                }
            }
        });
        table2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table2.rowAtPoint(e.getPoint());
                int col = table2.columnAtPoint(e.getPoint());
            
                if (!editMode && ourTurn) {
                    data2.get(row, col).setIsHit(true);
                }
            }
        });
    }
    
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Battleship");
        MainWindow gui = new MainWindow();
        frame.setContentPane(gui.rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
