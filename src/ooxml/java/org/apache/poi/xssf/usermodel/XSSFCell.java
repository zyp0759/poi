/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.usermodel;

import java.util.Calendar;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.SharedStringSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType;


public class XSSFCell implements Cell {

    private static final String FALSE_AS_STRING = "0";
    private static final String TRUE_AS_STRING  = "1";
    private final CTCell cell;
    private final XSSFRow row;
    private SharedStringSource sharedStringSource;
    private short cellNum;
    
    /**
     * Create a new XSSFCell. This method is protected to be used only by
     * tests.
     */
    protected XSSFCell(XSSFRow row) {
        this(row, CTCell.Factory.newInstance());
    }
    
    public XSSFCell(XSSFRow row, CTCell cell) {
        this.cell = cell;
        // TODO: parse cell.getR() to obtain cellnum
        this.row = row;
    }

    protected void setSharedStringSource(SharedStringSource sharedStringSource) {
        this.sharedStringSource = sharedStringSource;
    }

    public boolean getBooleanCellValue() {
        if (STCellType.B != cell.getT()) { 
            throw new NumberFormatException("You cannot get a boolean value from a non-boolean cell");
        }
        if (cell.isSetV()) {
            return (TRUE_AS_STRING.equals(this.cell.getV()));            
        }
        
        return false;
    }

    public Comment getCellComment() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCellFormula() {
        if (STCellType.STR != cell.getT()) { 
            throw new NumberFormatException("You cannot get a formula from a non-formula cell");
        }
        return this.cell.getF().getStringValue();
    }

    public short getCellNum() {
        return this.cellNum;
    }

    public CellStyle getCellStyle() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getCellType() {
        switch (this.cell.getT().intValue()) {
        case STCellType.INT_B:
            return CELL_TYPE_BOOLEAN;
        case STCellType.INT_N:
            return CELL_TYPE_NUMERIC;
        case STCellType.INT_E:
            return CELL_TYPE_ERROR;
        case STCellType.INT_S: // String is in shared strings
        case STCellType.INT_INLINE_STR: // String is inline in cell
            return CELL_TYPE_STRING;
        case STCellType.INT_STR:
            return CELL_TYPE_FORMULA;
        default:
            throw new IllegalStateException("Illegal cell type: " + this.cell.getT());
        }
    }

    public Date getDateCellValue() {
        if (STCellType.N == this.cell.getT() || STCellType.STR == this.cell.getT()) {
            double value = this.getNumericCellValue();
            if (false /* book.isUsing1904DateWindowing() */) {  // FIXME
                return HSSFDateUtil.getJavaDate(value,true);
            }
            else {
                return HSSFDateUtil.getJavaDate(value,false);
            }
        }
        throw new NumberFormatException("You cannot get a date value from a cell of type " + this.cell.getT());
    }

    public byte getErrorCellValue() {
        if (STCellType.E != cell.getT()) { 
            throw new NumberFormatException("You cannot get a error value from a non-error cell");
        }
        if (this.cell.isSetV()) {
            return Byte.parseByte(this.cell.getV());
        }
        return 0;
    }

    public double getNumericCellValue() {
        if (STCellType.N != cell.getT() && STCellType.STR != cell.getT()) { 
            throw new NumberFormatException("You cannot get a numeric value from a non-numeric cell");
        }
        if (this.cell.isSetV()) {
            return Double.parseDouble(this.cell.getV());
        }
        return Double.NaN;
    }

    public RichTextString getRichStringCellValue() {
        if(this.cell.getT() == STCellType.INLINE_STR) {
            if(this.cell.isSetV()) {
                return new XSSFRichTextString(this.cell.getV());
            } else {
                return new XSSFRichTextString("");
            }
        }
        if(this.cell.getT() == STCellType.S) {
            if(this.cell.isSetV()) {
                int sRef = Integer.parseInt(this.cell.getV());
                return new XSSFRichTextString(sharedStringSource.getSharedStringAt(sRef));
            } else {
                return new XSSFRichTextString("");
            }
        }
        throw new NumberFormatException("You cannot get a string value from a non-string cell");
    }

    public void setAsActiveCell() {
        // TODO Auto-generated method stub

    }

    public void setCellComment(Comment comment) {
        // TODO Auto-generated method stub

    }

    public void setCellErrorValue(byte value) {
        if ((this.cell.getT() != STCellType.E) && (this.cell.getT() != STCellType.STR))
        {
            this.cell.setT(STCellType.E);
        }
        this.cell.setV(String.valueOf(value));
    }

    
   
    public void setCellFormula(String formula) {
        if (this.cell.getT() != STCellType.STR)
        {
            this.cell.setT(STCellType.STR);
        }
        CTCellFormula f =  CTCellFormula.Factory.newInstance();
        f.setStringValue(formula);
        this.cell.setF(f);
        // XXX: is this correct? Should we recompute the value when the formula changes?
        if (this.cell.isSetV()) {
            this.cell.unsetV();
        }
            
    }

    public void setCellNum(short num) {
        this.cellNum = num;
    }

    public void setCellStyle(CellStyle style) {
        // TODO Auto-generated method stub

    }

    public void setCellType(int cellType) {
        switch (cellType) {
        case CELL_TYPE_BOOLEAN:
            this.cell.setT(STCellType.B);
            break;
        case CELL_TYPE_NUMERIC:
            this.cell.setT(STCellType.N);
            break;
        case CELL_TYPE_ERROR:
            this.cell.setT(STCellType.E);
            break;
        case CELL_TYPE_STRING:
            this.cell.setT(STCellType.S);
            break;
         default:
             throw new IllegalArgumentException("Illegal type: " + cellType);
        }
    }

    public void setCellValue(double value) {
        if ((this.cell.getT() != STCellType.N) && (this.cell.getT() != STCellType.STR))
        {
            this.cell.setT(STCellType.N);
        }
        this.cell.setV(String.valueOf(value));
    }

    public void setCellValue(Date value) {
        setCellValue(HSSFDateUtil.getExcelDate(value, false /*this.book.isUsing1904DateWindowing()*/)); // FIXME
    }

    public void setCellValue(Calendar value) {
        // TODO Auto-generated method stub

    }

    public void setCellValue(RichTextString value) {
        if(this.cell.getT() == STCellType.INLINE_STR) {
            this.cell.setV(value.getString());
            return;
        }
        if(this.cell.getT() != STCellType.S) {
            this.cell.setT(STCellType.S);
        }
        int sRef = sharedStringSource.putSharedString(value.getString());
        this.cell.setV(Integer.toString(sRef));
    }

    public void setCellValue(boolean value) {
        if ((this.cell.getT() != STCellType.B) && (this.cell.getT() != STCellType.STR))
        {
            this.cell.setT(STCellType.B);
        }        
        this.cell.setV(value ? TRUE_AS_STRING : FALSE_AS_STRING);
    }

    @Override
    public String toString() {
        return "[" + this.row.getRowNum() + "," + this.getCellNum() + "] " + this.cell.getV();
    }

    
}
