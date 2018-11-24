package core;


import network.BroadcastListener;
import network.NetCellData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private boolean ourTurn = false;
    
    private BroadcastListener broadcastListener;
    private List<InetAddress> addresses = new ArrayList<>();
    private ServerSocket server;
    private Socket connection;
    private GameThread gameThread;
    
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
            DefaultListModel<InetAddress> model = (DefaultListModel<InetAddress>) serverList.getModel();
            model.clear();
            for (InetAddress address : addresses) {
                model.addElement(address);
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
                        gameThread = new GameThread();
                        gameThread.start();
                        broadcastListener.setReceivingRequests(false);
                        setOurTurn(true);
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
                if (data1.isValid()) {
                    editMode = false;
                    new Thread(() -> {
                        log.append("\nConnecting...");
                        connect(addresses.get(serverList.getSelectedIndex()));
                        gameThread = new GameThread();
                        gameThread.start();
                        broadcastListener.setReceivingResponses(false);
                        log.append("\nConnected");
                    }).start();
                }
                else {
                    log.append("\nFailed to connect: table is not valid");
                }
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                log.append("\nInvalid address selected");
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
                table1.repaint();
            }
        });
        table2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table2.rowAtPoint(e.getPoint());
                int col = table2.columnAtPoint(e.getPoint());
            
                if (!editMode && !data2.get(row, col).isHit() && ourTurn) {
                    gameThread.send(data2.get(row, col), row, col);
                }
                table2.repaint();
            }
        });
    }
    
    public void setOurTurn(boolean value) {
        ourTurn = value;
        if (value) {
            log.append("\nOut turn");
        }
        else {
            log.append("\nOpponent's turn");
        }
    }
    
    private class GameThread extends Thread {
        ObjectInputStream in;
        ObjectOutputStream out;
        
        @Override
        public void run() {
            try {
                System.out.println("Game thread started");
                out = new ObjectOutputStream(connection.getOutputStream());
                System.out.println("Output stream created");
                in = new ObjectInputStream(connection.getInputStream());
                System.out.println("Input stream created");
                
                while (true) {
                    NetCellData data = (NetCellData) in.readObject();
                    System.out.println("Data received: row " + data.getRow() + " col " + data.getCol());
                    
                    if (data.isFeedback()) {
                        TableData.CellData cell = data2.get(data.getRow(), data.getCol());
                        cell.setHasShip(data.hasShip());
                        cell.setIsHit(data.isHit());
                        if (data.isTransferTurn()) {
                            setOurTurn(true);
                        }
                    }
                    else {
                        data1.get(data.getRow(), data.getCol()).setIsHit(true);
                        if (data.isTransferTurn()) {
                            if (!data1.get(data.getRow(), data.getCol()).hasShip()) {
                                setOurTurn(true);
                            }
                        }
                        NetCellData cell = new NetCellData(data1.get(data.getRow(), data.getCol()), true, data1.get(data.getRow(), data.getCol()).hasShip(), data.getRow(), data.getCol());
                        out.writeObject(cell);
                    }
                    
                    table1.repaint();
                    table2.repaint();
                    
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException ignored) {
            }
        }
        
        public void send(TableData.CellData cell, int row, int col) {
            try {
                if (out == null) {
                    log.append("\nError: output stream is null");
                    return;
                }
                setOurTurn(false);
                NetCellData c = new NetCellData(cell, false, true, row, col);
                out.writeObject(c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
