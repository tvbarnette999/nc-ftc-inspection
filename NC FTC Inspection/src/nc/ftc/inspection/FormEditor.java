package nc.ftc.inspection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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

import nc.ftc.inspection.InspectionForm.CB_LEVEL;
import nc.ftc.inspection.InspectionForm.HeaderRow;
import nc.ftc.inspection.InspectionForm.Row;

public class FormEditor extends JPanel implements Scrollable {
	//TODO open pdf by running it as a command (either cmd xxx.pf or just .pdf) runtime.exec
	//FIXME When saving, is there is a newline, replace it with <br>!!!!!! -oh no, wont work cuz it will get replaced with &lt whn loaded.... 
	//maybe store as \r,  otherwise, dont allow new lines, or just replace them with whitespace see what trey thinks
	//TODO When saving a file, if a change in # of CB occured, handle that with all Team Objects - resize arrays immediately.
	//TODO fix checkmarks so we dont have to replace with &#x2714; <-- this would mean checking every character, and if above u+255 converting it to that
	InspectionForm form;
	Vector<RowEdit> list = new Vector<RowEdit>();
	String newDelimiter;
	static Color back;
	public FormEditor(){
		
	}
	public FormEditor(InspectionForm form){
		setForm(form);
	}
	
	public void setForm(InspectionForm form){
		this.form = form;
		this.newDelimiter = form.delimiter;
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
		JMenuItem convert;
		JMenuItem delete = new JMenuItem("Delete");
		JButton add = new JButton("+");
		JButton more = new JButton("...");
		JPanel left = new JPanel();
		//TODO get rid of boxes
		Vector<JComponent> boxes = new Vector<JComponent>(2);
		JPopupMenu menu = new JPopupMenu("Menu");
		boolean header;
		private JComboBox<CB_LEVEL> getComboBox(){
			JComboBox<CB_LEVEL> combo = new JComboBox<CB_LEVEL>(CB_LEVEL.values());
			combo.setSelectedItem("REQ");
			return combo;
		}
	
		public RowEdit(Row row){
			back = this.getBackground();
			header = row instanceof HeaderRow;
			if(header){
				convert = new JMenuItem("Convert to Non-Header");
				for(String s : ((HeaderRow) row).titles){
					JTextArea label = new JTextArea(s.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("<br>", "\n"));
					boxes.addElement(label);
				}
				this.setBackground(Color.decode(form.color));//TODO use the real color
			} else{
				convert = new JMenuItem("Convert to Header");
				for(int i : row.param){
					JComboBox<CB_LEVEL> combo = getComboBox();
					switch(CB_LEVEL.get(i)){
						case NA: combo.setSelectedItem(CB_LEVEL.NA);break;
						case OPTIONAL: combo.setSelectedItem(CB_LEVEL.OPTIONAL);break;
						default: combo.setSelectedItem(CB_LEVEL.REQUIRED);break;
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
			
			if(row.explain != null)explain.setText(row.explain.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("<br>", "\n"));
			if(row.rule != null)rule.setText(row.rule.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("<br>", "\n"));
			
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
				
//				row.cbCount++;
				if(header){
//					HeaderRow h = (HeaderRow) row;
					JTextArea area = new JTextArea();
					boxes.add(area);
//					
//					h.titles = Arrays.copyOf(h.titles, h.cbCount);
//					h.titles[h.cbCount - 1] = "";
					
					left.add(area, left.getComponentCount() - 1); //-1 for + button, -1 for index
					left.revalidate();
					left.repaint();
				} else{
					JComboBox<CB_LEVEL> combo = getComboBox();
					boxes.add(combo);
//					row.param = Arrays.copyOf(row.param, row.cbCount);
//					row.param[row.cbCount - 1] = 0;

					System.out.println(left.getComponentCount());
					left.add(combo,left.getComponentCount() - 1);
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
				int count = left.getComponentCount() - 1;
				left.removeAll();
				
				header = !header;
				boxes.clear();
				if(header){
					convert.setText("Convert to Non-Header");
					
					for(int  i = 0; i < count; i ++){
						JTextArea label = new JTextArea(){
							public Dimension getPrefferedSize(){
								Dimension d = super.getPreferredSize();
								if(d.width < 20) d.width = 20;
								return d;
							}
						};
						
						boxes.add(label);
					}
					this.setBackground(Color.decode(form.color));//TODO use the real color
				} else{
					convert.setText("Convert to Header");
					
					for(int i = 0; i < count; i++){
						JComboBox<CB_LEVEL> combo = getComboBox();
						combo.setSelectedItem(CB_LEVEL.REQUIRED);
						boxes.add(combo);
					}
					this.setBackground(back);
				}
				for(JComponent comp : boxes){
					left.add(comp);
				}
				left.add(add);
				left.revalidate();
				left.repaint();
				
			} else if(src == delete){
				FormEditor.this.remove(list.indexOf(this));
				list.remove(this);
				FormEditor.this.revalidate();
				FormEditor.this.repaint();
			}
		}
	}
	

	public void setDelimiter(String del){
		System.out.println("Delimiter set to " + del);
		this.newDelimiter = del;
	}
	
	
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		
		
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
		
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
		
		return 0;
	}
	
}
