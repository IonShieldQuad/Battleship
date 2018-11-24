package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableData implements Serializable {
    private static final int SIZE = 10;
    private static final int[] SHIPS = {4, 3, 2, 1};
    
    private List<List<CellData>> data;
    
    public TableData() {
        data = new ArrayList<>();
        
        for (int i = 0; i < SIZE + 2; i++) {
            data.add(Arrays.asList(new CellData[SIZE + 2]));
        }
    
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                if (i == 0 || j == 0 || i == SIZE + 1 || j == SIZE + 1) {
                    data.get(i).set(j, new CellData(false, false, true));
                }
                else {
                    data.get(i).set(j, new CellData(false, false, false));
                }
            }
        }
    }
    
    public CellData get(int row, int col) {
        return data.get(row + 1).get(col + 1);
    }
    
    public boolean isValid() {
        int[] ships = new int[SHIPS.length];
        
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                
                if (get(i, j).hasShip()) {
                    
                    //Check diagonals
                    if (get(i - 1, j - 1).hasShip() || get(i + 1, j - 1).hasShip() || get(i - 1, j + 1).hasShip() || get(i + 1, j + 1).hasShip()) {
                        return false;
                    }
                    
                    //No turns of intersections in ships
                    if (get(i - 1, j).hasShip() || get(i + 1, j).hasShip()) {
                        if (get(i, j - 1).hasShip() || get(i, j + 1).hasShip()) {
                            return false;
                        }
                    }
    
                    //No turns of intersections in ships
                    if (get(i, j - 1).hasShip() || get(i, j + 1).hasShip()) {
                        if (get(i - 1, j).hasShip() || get(i + 1, j).hasShip()) {
                            return false;
                        }
                    }
    
                    int size = 0;
                    boolean validSize = false;
                    //Size of ship (vertical)
                    if (!get(i + 1, j).hasShip()) {
                        for (int k = i - 1; get(k, j).hasShip(); k--) {
                            size++;
                            validSize = true;
                        }
                        if (size > ships.length) {
                            return false;
                        }
                    }
    
                    //Size of ship (horizontal)
                    if (!validSize && !get(i, j + 1).hasShip()) {
                        for (int k = j - 1; get(i, k).hasShip(); k--) {
                            size++;
                            validSize = true;
                        }
                        if (size > ships.length) {
                            return false;
                        }
                    }
                    
                    if (!validSize && !get(i - 1, j).hasShip() && !get(i + 1, j).hasShip() && !get(i, j - 1).hasShip() && !get(i, j + 1).hasShip()) {
                        validSize = true;
                    }
                    
                    if (validSize) {
                        ships[size]++;
                    }
                    
                }
                
            }
        }
        
        //Check if there are enough ships of proper size
        for (int i = 0; i < ships.length; i++) {
            if (ships[i] != SHIPS[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    public int size() {
        return SIZE;
    }
    
    public static class CellData implements Serializable {
        private boolean hasShip;
        private boolean isHit;
        private boolean isBorder;
    
        public CellData(boolean hasShip, boolean isHit, boolean isBorder) {
            this.hasShip = hasShip;
            this.isHit = isHit;
            this.isBorder = isBorder;
        }
    
        public boolean isHit() {
            return isHit;
        }
    
        public void setIsHit(boolean hit) {
            isHit = hit;
        }
    
        public boolean hasShip() {
            return hasShip;
        }
    
        public void setHasShip(boolean hasShip) {
            this.hasShip = hasShip;
        }
    
        public boolean isBorder() {
            return isBorder;
        }
    
        public void setBorder(boolean border) {
            isBorder = border;
        }
    }
}
