package nc.ftc.inspection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import nc.ftc.inspection.InspectionForm.HeaderRow;
import nc.ftc.inspection.InspectionForm.Row;

public class FormEditor extends JPanel implements Scrollable {
	//TODO open pdf by running it as a command (either cmd xxx.pf or just .pdf) runtime.exec
	//FIXME When saving, is there is a newline, replace it with <br>!!!!!! -oh no, wont work cuz it will get replaced with &lt whn loaded.... 
	//maybe store as \r,  otherwise, dont allow new lines, or just replace them with whitespace see what trey thinks
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
	class RowEdit extends JPanel implements ActionListener{
		JTextArea explain = new JTextArea();
		JTextArea rule = new JTextArea(){
			public Dimension getPreferredSize(){
				Dimension d = super.getPreferredSize();
				d.width = 100;
				return d;
			}
		};
		JMenuItem addAbove = new JMenuItem("Add Row Above");
		JMenuItem addHeaderAbove = new JMenuItem("Add Header Above");
		JMenuItem addBelow = new JMenuItem("Add Row Below");
		JMenuItem addHeaderBelow = new JMenuItem("Add Header Below");
		JMenuItem convert = new JMenuItem("Convert to Header");
		JMenuItem delete = new JMenuItem("Delete");
		JButton add = new JButton("+");
		JButton more = new JButton("...");
		JPanel left = new JPanel();
		Vector<JComponent> boxes = new Vector<JComponent>(2);
		Row row;
		JPopupMenu menu = new JPopupMenu("Menu");
		private JComboBox<String> getComboBox(){
			JComboBox<String> combo = new JComboBox<String>();
			combo.addItem("REQ");
			combo.addItem("OPT");
			combo.addItem("NA");
			combo.setSelectedItem("REQ");
			//TODO Listener
			return combo;
		}
	
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
				this.setBackground(Color.orange);//TODO use the real color
			} else{
				for(int i : row.param){
					JComboBox<String> combo = getComboBox();
					switch(i){
						case InspectionForm.NA: combo.setSelectedItem("NA");break;
						case InspectionForm.OPIONAL: combo.setSelectedItem("OPT");break;
						default: combo.setSelectedItem("REQ");break;
					}
					boxes.add(combo);
				}
			}
			
			add.addActionListener(this);
			more.addActionListener(this);
			boxes.add(add);
			left.setOpaque(false);
			
			for(JComponent comp : boxes){
				left.add(comp);
			}
			explain.setLineWrap(true);
			rule.setLineWrap(true);
			
			explain.setText(r.explain);
			rule.setText(r.rule);
			
			JPanel morePan = new JPanel();
			morePan.add(more);
			morePan.setOpaque(false);
			
			JPanel right = new JPanel();
			right.setOpaque(false);
			right.setLayout(new BorderLayout());
			right.add(morePan, BorderLayout.EAST);
			right.add(rule,  BorderLayout.WEST);
			
			this.setLayout(new BorderLayout());
			this.add(left, BorderLayout.WEST);
			this.add(right, BorderLayout.EAST);
			this.add(explain, BorderLayout.CENTER);
			
			
			menu.add(addAbove);
			menu.add(addHeaderAbove);
			menu.add(addBelow);
			menu.add(addHeaderBelow);
			menu.add(convert);
			menu.add(delete);
			
			
			addAbove.addActionListener(this);
			addBelow.addActionListener(this);
			addHeaderAbove.addActionListener(this);
			addHeaderBelow.addActionListener(this);
			convert.addActionListener(this);
			delete.addActionListener(this);
			
			addAbove.setMnemonic(KeyEvent.VK_1);
			addHeaderAbove.setMnemonic(KeyEvent.VK_2);
			addBelow.setMnemonic(KeyEvent.VK_3);
			addHeaderBelow.setMnemonic(KeyEvent.VK_4);
			convert.setMnemonic(KeyEvent.VK_5);
			delete.setMnemonic(KeyEvent.VK_6);
//			addAbove.setAccelerator(KeyStroke.getKeyStroke("1"));
//			delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
			
			
			
		}


		public void addRow(Row r, int ind){
			RowEdit edit = new RowEdit(r);
			list.add(ind, edit);
			FormEditor.this.add(edit, ind);
			this.revalidate();
			this.repaint();
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent src = (JComponent) e.getSource();
			if(src == more){
				menu.show(src, -120, 0);			
			} else if(src == add){

				row.cbCount++;
				if(row instanceof HeaderRow){
					HeaderRow h = (HeaderRow) row;
					JTextArea area = new JTextArea();
					boxes.add(area);
					
					h.titles = Arrays.copyOf(h.titles, h.cbCount);
					h.titles[h.cbCount - 1] = "";
					
					left.add(area, h.cbCount - 1);
					left.revalidate();
					left.repaint();
				} else{
					JComboBox<String> combo = getComboBox();
					boxes.add(combo);
					row.param = Arrays.copyOf(row.param, row.cbCount);
					row.param[row.cbCount - 1] = 0;
					left.add(combo, row.cbCount - 1);
				}
			} else if(src == addAbove){
				//yes this is wasteful cuz its about to be cloned and gc'd but oh well
				addRow(new Row(new String[]{"0", "", ""}), list.indexOf(this));
				
			} else if(src == addHeaderAbove){
				addRow(new HeaderRow(new String[]{"H","0", "", ""}), list.indexOf(this));
				
			} else if(src == addBelow){
				addRow(new Row(new String[]{"0", "", ""}), list.indexOf(this) + 1);
				
			} else if(src == addHeaderBelow){
				addRow(new HeaderRow(new String[]{"H","0", "", ""}), list.indexOf(this) + 1);
				
			} else if(src == convert){
				if(row instanceof HeaderRow){
					String[] raw = new String[row.cbCount + 3];
					raw[0] = Integer.toString(row.cbCount);
					for(int i = 0; i < row.cbCount; i++){
						raw[i + 1] = Integer.toString(InspectionForm.REQUIRED);
					}
					raw[row.cbCount + 1] = this.explain.getText();
					raw[row.cbCount + 2] = this.rule.getText();
//					System.out.println(raw[row.cbCount + 2] + " , "+ this.rule.getText() + " , " + row.rule);
					Row replacement = new Row(raw);
					row = replacement;
					RowEdit edit = new RowEdit(row);
//					System.out.println(edit.rule.getText()+" , " + row.rule);
					int ind = list.indexOf(this);
					FormEditor.this.remove(ind);
					FormEditor.this.add(edit, ind);
					list.set(ind, edit);
				} else {
					String[] raw = new String[row.cbCount + 4];
					raw[0] = "H";
					raw[1] = Integer.toString(row.cbCount);
					for(int i = 0; i < row.cbCount; i++){
						raw[i + 2] = "";
					}
					raw[row.cbCount + 2] = this.explain.getText();
					raw[row.cbCount + 3] = this.rule.getText();
					HeaderRow head = new HeaderRow(raw);
					row = head;
					RowEdit edit = new RowEdit(row);
					int ind = list.indexOf(this);
					FormEditor.this.remove(ind);
					FormEditor.this.add(edit, ind);
					list.set(ind, edit);
					edit.revalidate();
					edit.repaint();
					
				}
				
			} else if(src == delete){
				FormEditor.this.remove(list.indexOf(this));
				list.remove(this);
				FormEditor.this.revalidate();
				FormEditor.this.repaint();
			}
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
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
