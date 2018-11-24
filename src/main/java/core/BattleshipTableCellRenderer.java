package core;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class BattleshipTableCellRenderer extends DefaultTableCellRenderer {
    private static final Color EMPTY_COLOR = new Color(0x2464b9);
    private static final Color MISS_COLOR = new Color(0x80acff);
    private static final Color SHIP_COLOR = new Color(0x909090);
    private static final Color HIT_COLOR = new Color(0xbe1500);
    
    private boolean showShips = true;
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableData.CellData data = (TableData.CellData) value;
        
        if (data.hasShip()) {
            if (data.isHit()) {
                c.setBackground(HIT_COLOR);
                c.setForeground(HIT_COLOR);
            }
            else {
                if (showShips) {
                    c.setBackground(SHIP_COLOR);
                    c.setForeground(SHIP_COLOR);
                }
                else {
                    c.setBackground(EMPTY_COLOR);
                    c.setForeground(EMPTY_COLOR);
                }
            }
        }
        else {
            if (data.isHit()){
                c.setBackground(MISS_COLOR);
                c.setForeground(MISS_COLOR);
            }
            else {
                c.setBackground(EMPTY_COLOR);
                c.setForeground(EMPTY_COLOR);
            }
        }
        
        return c;
    }
    
    public boolean isShowShips() {
        return showShips;
    }
    
    public void setShowShips(boolean showShips) {
        this.showShips = showShips;
    }
}
