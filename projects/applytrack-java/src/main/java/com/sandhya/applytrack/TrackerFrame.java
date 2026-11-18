package com.sandhya.applytrack;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Deliberately small desktop layer: business rules remain in TrackerService. */
public final class TrackerFrame extends JFrame {
    private final TrackerService service;
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Company","Role","Status","Applied","Follow-up","Notes"},0) {
        public boolean isCellEditable(int row,int col){return false;}
        public Class<?> getColumnClass(int col){return col==0?Long.class:String.class;}
    };
    private final JTable table=new JTable(model);
    private final JLabel summary=new JLabel();
    private final JCheckBox dueOnly=new JCheckBox("Due by today");
    public TrackerFrame(TrackerService service) {
        super("ApplyTrack | Internship and job applications"); this.service=service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setSize(1080,540); setLocationRelativeTo(null);
        JPanel top=new JPanel(new BorderLayout(12,12)); top.setBorder(BorderFactory.createEmptyBorder(18,18,12,18));
        JLabel title=new JLabel("ApplyTrack"); title.setFont(new Font("SansSerif",Font.BOLD,26));
        top.add(title,BorderLayout.NORTH); top.add(summary,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        JButton add=new JButton("Add application"),update=new JButton("Update selected"),refresh=new JButton("Refresh");
        actions.add(add); actions.add(update); actions.add(refresh); actions.add(dueOnly); top.add(actions,BorderLayout.SOUTH);
        add(top,BorderLayout.NORTH); table.setRowHeight(28); table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); add(new JScrollPane(table),BorderLayout.CENTER);
        JLabel location=new JLabel("Stored locally: "+service.location()); location.setBorder(BorderFactory.createEmptyBorder(10,18,10,18)); add(location,BorderLayout.SOUTH);
        add.addActionListener(e->addApplication()); update.addActionListener(e->updateApplication());
        refresh.addActionListener(e->refresh()); dueOnly.addActionListener(e->refresh()); refresh();
    }
    private void addApplication() {
        JTextField company=new JTextField(),role=new JTextField(),applied=new JTextField(LocalDate.now().toString()),follow=new JTextField(),notes=new JTextField();
        JPanel panel=form(new String[]{"Company","Role","Applied date YYYY-MM-DD","Follow-up date optional","Notes"},new JComponent[]{company,role,applied,follow,notes});
        if(JOptionPane.showConfirmDialog(this,panel,"Add application",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
        try { service.add(company.getText(),role.getText(),LocalDate.parse(applied.getText().trim()),Main.date(follow.getText().trim()),notes.getText()); refresh(); }
        catch(Exception e){error(e);}
    }
    private void updateApplication() {
        int row=table.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Select an application first.");return;}
        long id=((Number)table.getValueAt(row,0)).longValue();
        JComboBox<Status> status=new JComboBox<Status>(Status.values()); status.setSelectedItem(Status.valueOf(table.getValueAt(row,3).toString()));
        JTextField follow=new JTextField(table.getValueAt(row,5).toString()),notes=new JTextField(table.getValueAt(row,6).toString());
        JPanel panel=form(new String[]{"Status","Follow-up date optional","Notes"},new JComponent[]{status,follow,notes});
        if(JOptionPane.showConfirmDialog(this,panel,"Update #"+id,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try {service.update(id,(Status)status.getSelectedItem(),Main.date(follow.getText().trim()),notes.getText());refresh();}
        catch(Exception e){error(e);}
    }
    private JPanel form(String[] labels,JComponent[] fields) {
        JPanel p=new JPanel(new GridLayout(0,2,12,10));
        for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);} return p;
    }
    private void refresh() {
        try {
            List<Application> rows=dueOnly.isSelected()?service.due(LocalDate.now()):service.list();
            model.setRowCount(0);
            for(Application a:rows)model.addRow(new Object[]{a.id,a.company,a.role,a.status.name(),a.appliedOn.toString(),a.followUpOn==null?"":a.followUpOn.toString(),a.notes});
            summary.setText("All applications: "+service.list().size()+"  |  Follow-ups due: "+service.due(LocalDate.now()).size()+"  |  "+service.summary());
        } catch(Exception e){error(e);}
    }
    private void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Please check your input",JOptionPane.ERROR_MESSAGE);}
}
