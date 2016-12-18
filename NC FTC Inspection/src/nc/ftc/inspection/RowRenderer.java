package nc.ftc.inspection;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import nc.ftc.inspection.InspectionForm.Row;

public class RowRenderer extends JPanel implements TableCellRenderer{
//	Row row;
	JLabel pan = new JLabel("hi");
	JTextField txt = new JTextField("type");
	public RowRenderer(){
//		row = r;
		this.setLayout(new GridLayout(1,2));
		this.add(pan);
		this.add(txt);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// TODO Auto-generated method stub
		Row r = (Row) value;
		return this;
	}
		
}

