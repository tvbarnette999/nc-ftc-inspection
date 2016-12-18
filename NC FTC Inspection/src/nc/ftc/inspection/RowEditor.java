package nc.ftc.inspection;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.Renderer;
import javax.swing.table.TableCellEditor;

import nc.ftc.inspection.InspectionForm.Row;

public class RowEditor extends AbstractCellEditor implements TableCellEditor {
	RowRenderer render= new RowRenderer();
	Row r;
	@Override
	public Object getCellEditorValue() {
		// TODO Auto-generated method stub
		
		return r;
	}
	 public Component getTableCellEditorComponent(JTable table,
             Object value, boolean isSelected, int row, int column) {
		 r = (Row) value;
		 return render.getTableCellRendererComponent(table, value, isSelected, false, row, column);
	 }

}
