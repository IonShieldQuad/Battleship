package core;


import network.BroadcastListener;
import network.NetCellData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private JPanel turnIndicator;
    private JButton resetButton;
    
    private TableData data1;
    private TableData data2;
    private boolean editMode;
    private boolean ourTurn = false;
    private boolean gameEnded = false;
    
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
        
        validateButton.addActionListener(e -> log.append("\nTable is " + (data1.isValid() ? "valid" : "not valid")));
        
        serverList.setModel(new DefaultListModel<>());
        
        broadcastListener = new BroadcastListener(SEARCH_PORT, false, false);
        broadcastListener.setAddressCollection(addresses);
        broadcastListener.addResponseListener(e -> updateServerList());
        broadcastListener.start();
        
        refreshButton.addActionListener(e -> refresh());
        
        hostButton.addActionListener(e -> host());
        
        connectButton.addActionListener(e -> connect());
        
        resetButton.addActionListener(e -> reset());
        
        turnIndicator.setBackground(Color.WHITE);
    }
    
    private void refresh() {
        if (!editMode) {
            log.append("\nGame is already started");
            return;
        }
        addresses.clear();
        updateServerList();
        broadcastListener.setReceivingResponses(true);
        broadcastListener.sendRequest();
    }
    
    private void host() {
        if (!editMode) {
            log.append("\nGame is already started");
            return;
        }
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
                } catch (SocketException ignored) {
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }).start();
            log.append("\nHosting game...");
        }
        else {
            log.append("\nFailed to host: table is not valid");
        }
    }
    
    private void connect() {
        try {
            if (!editMode) {
                log.append("\nGame is already started");
                return;
            }
            if (serverList.getSelectedIndex() < 0) {
                log.append("\nInvalid address selected");
                return;
            }
            if (data1.isValid()) {
                editMode = false;
                new Thread(() -> {
                    connect(addresses.get(serverList.getSelectedIndex()));
                    gameThread = new GameThread();
                    gameThread.start();
                    broadcastListener.setReceivingResponses(false);
                    setOurTurn(false);
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
    }
    
    private void updateServerList() {
        DefaultListModel<InetAddress> model = (DefaultListModel<InetAddress>) serverList.getModel();
        model.clear();
        for (InetAddress address : addresses) {
            model.addElement(address);
        }
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
            
                if (!editMode && !gameEnded && !data2.get(row, col).isHit() && ourTurn) {
                    gameThread.send(data2.get(row, col), row, col);
                }
                table2.repaint();
            }
        });
    }
    
    private void reset() {
        try {
            if (connection != null) {
                connection.close();
            }
            connection = null;
            if (server != null) {
                server.close();
            }
            server = null;
            broadcastListener.setReceivingRequests(false);
            broadcastListener.setReceivingResponses(false);
            if (gameThread != null) {
                gameThread.interrupt();
            }
            gameThread = null;
            editMode = true;
            ourTurn = false;
            gameEnded = false;
            addresses.clear();
            for (int i = 0; i < data1.size(); i++) {
                for (int j = 0; j < data1.size(); j++) {
                    data1.get(i, j).setIsHit(false);
                    data1.get(i, j).setHasShip(false);
                }
            }
            for (int i = 0; i < data2.size(); i++) {
                for (int j = 0; j < data2.size(); j++) {
                    data2.get(i, j).setIsHit(false);
                    data2.get(i, j).setHasShip(false);
                }
            }
            table1.repaint();
            table2.repaint();
            updateServerList();
            turnIndicator.setBackground(Color.WHITE);
            log.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setOurTurn(boolean value) {
        ourTurn = value;
        if (value) {
            turnIndicator.setBackground(Color.GREEN);
        }
        else {
            turnIndicator.setBackground(Color.RED);
        }
    }
    
    private void win() {
        turnIndicator.setBackground(Color.ORANGE);
        gameEnded = true;
        log.append("\nVICTORY");
        if (gameThread != null) {
            gameThread.interrupt();
        }
    }
    
    private void lose() {
        turnIndicator.setBackground(Color.BLUE);
        gameEnded = true;
        log.append("\nDEFEAT");
        if (gameThread != null) {
            gameThread.interrupt();
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
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    
                    NetCellData data = (NetCellData) in.readObject();
                    System.out.println("Data received: row " + data.getRow() + " col " + data.getCol());
    
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    
                    if (data.isFeedback()) {
                        TableData.CellData cell = data2.get(data.getRow(), data.getCol());
                        cell.setHasShip(data.hasShip());
                        cell.setIsHit(data.isHit());
                        log.append("\nOur shot at row " + data.getRow() + " col " + data.getCol() + " : " + (data.hasShip() ? "hit" : "miss"));
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
    
                        log.append("\nOpponent's shot at row " + data.getRow() + " col " + data.getCol() + " : " + (data1.get(data.getRow(), data.getCol()).hasShip() ? "hit" : "miss"));
                        
                        List<Point> points = data1.getShipTiles(data.getRow(), data.getCol());
                        if (points.stream().allMatch(p -> data1.get(p.x, p.y).isHit())) {
                            Set<Point> adjPoints = new HashSet<>();
                            points.forEach(p -> {
                                adjPoints.add(new Point(p.x + 1, p.y));
                                adjPoints.add(new Point(p.x - 1, p.y));
                                adjPoints.add(new Point(p.x + 1, p.y + 1));
                                adjPoints.add(new Point(p.x + 1, p.y - 1));
                                adjPoints.add(new Point(p.x - 1, p.y + 1));
                                adjPoints.add(new Point(p.x - 1, p.y - 1));
                                adjPoints.add(new Point(p.x, p.y + 1));
                                adjPoints.add(new Point(p.x, p.y - 1));
                            });
                            adjPoints.forEach(p -> {
                                data1.get(p.x, p.y).setIsHit(true);
                                NetCellData nc = new NetCellData(data1.get(p.x, p.y), true, false, p.x, p.y);
                                try {
                                    out.writeObject(nc);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
    
                    int hitShipTiles = 0;
                    for (int i = 0; i < data1.size(); i++) {
                        for (int j = 0; j < data1.size(); j++) {
                            if (data1.get(i, j).hasShip() && data1.get(i, j).isHit()) {
                                hitShipTiles++;
                            }
                        }
                    }
                    if (hitShipTiles >= data1.maxShipTileCount()) {
                        lose();
                    }
    
                    hitShipTiles = 0;
                    for (int i = 0; i < data2.size(); i++) {
                        for (int j = 0; j < data2.size(); j++) {
                            if (data2.get(i, j).hasShip() && data2.get(i, j).isHit()) {
                                hitShipTiles++;
                            }
                        }
                    }
                    if (hitShipTiles >= data2.maxShipTileCount()) {
                        win();
                    }
                    
                    table1.repaint();
                    table2.repaint();
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
