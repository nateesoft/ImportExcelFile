package com.ics.utils.importexcelfile;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

public class ColumnMappingDialog extends JDialog {

    private static final Logger logger = Logger.getLogger(ColumnMappingDialog.class.getName());
    private static final String NONE         = "(ไม่ระบุ)";
    private static final String MAPPING_FILE = "column_mapping.properties";

    // {fieldKey, displayLabel}  — เฉพาะ field ที่ต้อง map จาก Excel
    // R_No, R_Que => ใช้จาก form / auto-generate
    // R_Time, R_EntryDate => CURTIME() / CURDATE()
    // R_Remark, R_RefCode, R_SendInterface, RefCode, R_Pqty => NULL
    private static final String[][] FIELDS = {
        {"R_PCode",    "รหัสสินค้า      (R_PCode)"},
        {"R_Stock",    "คลังสินค้า      (R_Stock)"},
        {"R_Pack",     "จำนวนแพ็ก       (R_Pack)"},
        {"R_Qty",      "จำนวน           (R_Qty)"},
        {"R_Post",     "สถานะ Post      (R_Post)"},
        {"R_Unit",     "หน่วย           (R_Unit)"},
        {"R_Cost",     "ราคาต้นทุน     (R_Cost)"},
        {"R_Amount",   "ราคารวม         (R_Amount)"},
        {"R_TotalQty", "จำนวนรวม        (R_TotalQty)"},
        {"R_User",     "ผู้บันทึก       (R_User)"},
        {"R_PName",    "ชื่อสินค้า      (R_PName)"},
    };

    private final Map<String, JComboBox<String>> combos = new LinkedHashMap<>();
    private boolean confirmed = false;

    public ColumnMappingDialog(java.awt.Frame owner, String[] tableColumns) {
        super(owner, "กำหนด Column Mapping", true);

        String[] options = buildOptions(tableColumns);
        Map<String, String> saved = loadSavedMapping();

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        GridBagConstraints lc = baseConstraints(0, 0.35);
        GridBagConstraints rc = baseConstraints(1, 0.65);
        rc.fill = GridBagConstraints.HORIZONTAL;

        // Header labels
        lc.gridy = 0; formPanel.add(bold("Field ใน tranout"), lc);
        rc.gridy = 0; formPanel.add(bold("Column ใน Excel (JTable)"), rc);

        // Separator
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridy = 1; sc.gridx = 0; sc.gridwidth = 2;
        sc.fill = GridBagConstraints.HORIZONTAL;
        sc.insets = new Insets(4, 0, 8, 0);
        formPanel.add(new JSeparator(), sc);

        // Field rows
        for (int i = 0; i < FIELDS.length; i++) {
            String key   = FIELDS[i][0];
            String label = FIELDS[i][1];

            JComboBox<String> combo = new JComboBox<>(options);
            combo.setEditable(true);
            applySelection(combo, tableColumns, key, saved.get(key));
            combos.put(key, combo);

            lc.gridy = i + 2; formPanel.add(new JLabel(label), lc);
            rc.gridy = i + 2; formPanel.add(combo, rc);
        }

        // Buttons
        JButton btnOk     = new JButton("ยืนยัน");
        JButton btnCancel = new JButton("ยกเลิก");
        btnOk.addActionListener(e -> {
            confirmed = true;
            saveMappingToFile(getMapping());
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        btnPanel.add(btnCancel);
        btnPanel.add(btnOk);

        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnOk);
        pack();
        setMinimumSize(new java.awt.Dimension(520, 400));
        setLocationRelativeTo(owner);
    }

    // ---- selection logic ----

    /**
     * Priority: saved mapping → auto-detect by name → (ไม่ระบุ)
     * Combo เป็น editable จึง restore ค่าที่ user เคย type ไว้ได้โดยตรง
     * ไม่จำเป็นต้องเช็คว่าอยู่ใน dropdown list หรือไม่
     */
    private void applySelection(JComboBox<String> combo, String[] tableColumns,
                                 String fieldKey, String savedColumn) {
        // 1. saved mapping (รวมถึงค่าที่ user พิมพ์เองด้วย)
        if (savedColumn != null && !savedColumn.isEmpty()) {
            combo.setSelectedItem(savedColumn);
            return;
        }
        // 2. auto-detect ชื่อตรงกัน (case-insensitive)
        for (String col : tableColumns) {
            if (col.equalsIgnoreCase(fieldKey)) {
                combo.setSelectedItem(col);
                return;
            }
        }
        // 3. ไม่ระบุ (default)
    }

    // ---- persistence ----

    private Map<String, String> loadSavedMapping() {
        Map<String, String> result = new LinkedHashMap<>();
        File file = new File(MAPPING_FILE);
        if (!file.exists()) {
            return result;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot load column_mapping.properties", ex);
        }
        return result;
    }

    private void saveMappingToFile(Map<String, String> mapping) {
        Properties props = new Properties();
        props.putAll(mapping);
        try (FileOutputStream fos = new FileOutputStream(MAPPING_FILE)) {
            props.store(fos, "Column Mapping - tranout");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot save column_mapping.properties", ex);
        }
    }

    // ---- layout helpers ----

    private String[] buildOptions(String[] tableColumns) {
        String[] opts = new String[tableColumns.length + 1];
        opts[0] = NONE;
        System.arraycopy(tableColumns, 0, opts, 1, tableColumns.length);
        return opts;
    }

    private GridBagConstraints baseConstraints(int gridx, double weightx) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = gridx;
        c.weightx = weightx;
        c.anchor  = GridBagConstraints.WEST;
        c.insets  = new Insets(5, 8, 5, 8);
        return c;
    }

    private JLabel bold(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        return lbl;
    }

    // ---- public API ----

    public boolean isConfirmed() {
        return confirmed;
    }

    /** คืน map: tranoutField → excelColumnName (ไม่รวม field ที่เลือก NONE) */
    public Map<String, String> getMapping() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JComboBox<String>> e : combos.entrySet()) {
            String selected = (String) e.getValue().getSelectedItem();
            if (!NONE.equals(selected)) {
                result.put(e.getKey(), selected);
            }
        }
        return result;
    }
}
