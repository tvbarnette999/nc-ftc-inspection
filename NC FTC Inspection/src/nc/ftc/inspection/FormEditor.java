package nc.ftc.inspection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;

import nc.ftc.inspection.InspectionForm.HeaderRow;
import nc.ftc.inspection.InspectionForm.Row;

public class FormEditor extends JPanel implements Scrollable {
	//TODO open pdf by running it as a command (either cmd xxx.pf or just .pdf) runtime.exec
	InspectionForm form;
	Vector<RowEdit> list = new Vector<RowEdit>();
	public FormEditor(){
		
	}
	public FormEditor(InspectionForm form){
		setForm(form);
	}
	
	public void setForm(InspectionForm form){
		this.form = form;
		list.clear();
		this.removeAll();
		for(Row r : form.rows){
			list.add(new RowEdit(r));
		}
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		for(RowEdit re : list){
			this.add(re);
		}
		this.revalidate();
	}
	class RowEdit extends JPanel{
		JTextArea explain = new JTextArea();
		JTextArea rule = new JTextArea();
		JPanel left = new JPanel();
		Vector<JComponent> boxes = new Vector<JComponent>(2);
		Row row;
		
		public RowEdit(Row r){
			try {
				row = (Row) r.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			if(row instanceof HeaderRow){
				for(String s : ((HeaderRow) row).titles){
					JTextArea label = new JTextArea(s);	
					boxes.addElement(label);
				}
				this.setBackground(Color.orange);
			} else{
				for(int i : row.param){
					JComboBox<String> combo = new JComboBox<String>();
					combo.addItem("REQ");
					combo.addItem("OPT");
					combo.addItem("NA");
					switch(i){
						case InspectionForm.NA: combo.setSelectedItem("NA");break;
						case InspectionForm.OPIONAL: combo.setSelectedItem("OPT");break;
						default: combo.setSelectedItem("REQ");break;
					}
					boxes.add(combo);
//					System.out.println(combo.getPreferredSize());
//					System.out.println(combo.getMaximumSize());
//					System.out.println(combo.getSize());
//					System.out.println(combo.getMinimumSize());
				}
			}
			JButton add = new JButton("+");
			boxes.add(add);
			left.setOpaque(false);
			for(JComponent comp : boxes){
				left.add(comp);
			}
			explain.setLineWrap(true);
			rule.setLineWrap(true);
			explain.setText(r.explain);
			rule.setText(r.rule);
			rule.setPreferredSize(new Dimension(100,20));
			this.setLayout(new BorderLayout());
			this.add(left, BorderLayout.WEST);
			this.add(rule, BorderLayout.EAST);
			this.add(explain, BorderLayout.CENTER);
		}
	}
	

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		// TODO Auto-generated method stub
		
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
