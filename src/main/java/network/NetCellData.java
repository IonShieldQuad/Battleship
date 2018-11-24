package network;

import core.TableData;

public class NetCellData extends TableData.CellData {
    private boolean feedback;
    private boolean transferTurn;
    private int row;
    private int col;
    
    public NetCellData(boolean hasShip, boolean isHit, boolean isBorder, boolean feedback, boolean transferTurn, int row, int col) {
        super(hasShip, isHit, isBorder);
        this.feedback = feedback;
        this.transferTurn = transferTurn;
        this.row = row;
        this.col = col;
    }
    
    public NetCellData(TableData.CellData cell, boolean feedback, boolean transferTurn, int row, int col) {
        super(cell.hasShip(), cell.isHit(), cell.isBorder());
        this.feedback = feedback;
        this.transferTurn = transferTurn;
        this.row = row;
        this.col = col;
    }
    
    public boolean isFeedback() {
        return feedback;
    }
    
    public void setFeedback(boolean feedback) {
        this.feedback = feedback;
    }
    
    public int getRow() {
        return row;
    }
    
    public void setRow(int row) {
        this.row = row;
    }
    
    public int getCol() {
        return col;
    }
    
    public void setCol(int col) {
        this.col = col;
    }
    
    public boolean isTransferTurn() {
        return transferTurn;
    }
    
    public void setTransferTurn(boolean transferTurn) {
        this.transferTurn = transferTurn;
    }
}
