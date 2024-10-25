/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package NewSalesRecord;

import Forms.ConnectionToDatabase;
import Forms.MainPanel;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author HP
 */
public class CustSalesRecord extends javax.swing.JFrame {
Connection conn = null;
PreparedStatement pst = null;
ResultSet rs = null;
String[] productName;
String[] quantity;
double[] amountPerRows;
    /**
     * Creates new form CustSalesRecord
     */
    public CustSalesRecord() {
        initComponents();
        conn = ConnectionToDatabase.connectToDb();
        setIconImage(new ImageIcon(getClass().getResource("icon.png")).getImage());        
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        populateMedicine();
    }
    
    public void populateMedicine(){
        try{
            String sql = "SELECT product_name FROM products";
            pst = conn.prepareStatement(sql);
            rs = pst.executeQuery();
            while(rs.next()){
               medicineCombo.addItem(rs.getString("product_name"));
           }
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }
    }
    
    public int getSum() {
    int rowsCount = cartTable.getRowCount();
    int sum = 0;

    // Check if the table has at least 6 columns (index 5 means the 6th column)
    if (cartTable.getColumnCount() > 5) {
        for (int i = 0; i < rowsCount; i++) {
            // Get the total price from the last column (5th index) and add to the sum
            try {
                sum = sum + Integer.parseInt(cartTable.getValueAt(i, 5).toString());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid value in price column at row " + i + ": " + e.getMessage());
                return sum;
            }
        }
    } else {
        JOptionPane.showMessageDialog(null, "The table does not have enough columns (6 expected).");
    }

    return sum;
}

public void insertAllProducts(double val) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Calendar calobj = Calendar.getInstance();

    try {
        int rowsCount = cartTable.getRowCount();
        for (int i = 0; i < rowsCount; i++) {
            String sql = "INSERT INTO sales_record(product_name, company_name, date_of_sale, quantity, amount) VALUES (?,?,?,?,?)";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, cartTable.getValueAt(i, 1).toString()); // Corrected to use "Product Name"
                pst.setString(2, cartTable.getValueAt(i, 2).toString()); // Corrected to use "Company Name"
                pst.setString(3, df.format(calobj.getTime()));
                pst.setInt(4, Integer.parseInt(cartTable.getValueAt(i, 4).toString())); // Ensure quantity is int
                pst.setDouble(5, val); // Use setDouble for amount
                pst.executeUpdate();
            }
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Invalid quantity entered: " + e.getMessage());
    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage());
    }
}

public void getPriceOfProductName() {
    int userChoice = JOptionPane.showConfirmDialog(null, "Confirm Purchase?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

    if (userChoice == JOptionPane.YES_OPTION) {
        JOptionPane.showMessageDialog(null, "Medicine Purchase");

        try {
            int rowsCount = cartTable.getRowCount();
            for (int i = 0; i < rowsCount; i++) {
                // Get product name from first column, and company name from second column
                String productName = cartTable.getValueAt(i, 0).toString().trim(); // Correcting column index for product name
                String companyName = cartTable.getValueAt(i, 1).toString().trim(); // Company name column
                
                System.out.println("Product Name: " + productName); // Debugging step
                System.out.println("Company Name: " + companyName); // Debugging step

                // Query the price of the selected product
                String sql = "SELECT price_per_unit FROM products WHERE LOWER(product_name) = LOWER(?) AND LOWER(company_name) = LOWER(?)";
                try (PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, productName);
                    pst.setString(2, companyName); // Include company name in the query
                    
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            double pricePerUnit = rs.getDouble("price_per_unit");
                            int quantity = Integer.parseInt(cartTable.getValueAt(i, 3).toString()); // Assuming quantity is in 4th column

                            // Validate quantity
                            if (quantity <= 0) {
                                JOptionPane.showMessageDialog(null, "Invalid quantity: Quantity must be greater than 0.");
                                return;
                            }

                            double totalPrice = pricePerUnit * quantity;

                            // Check if the table has enough columns to store the total price
                            if (cartTable.getColumnCount() > 4) {
                                cartTable.setValueAt(totalPrice, i, 4); // Set the total price in the correct column
                            } else {
                                JOptionPane.showMessageDialog(null, "The table does not have enough columns to store the total price.");
                                return;
                            }

                            // Insert the product details into the sales record table
                            insertSalesRecord(productName, companyName, quantity, totalPrice);
                        } else {
                            JOptionPane.showMessageDialog(null, "Product not found: " + productName);
                        }
                    }
                }
            }

            // Optional: Reset the form and go back to the main panel
            MainPanel mp = new MainPanel();
            this.dispose();
            mp.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid quantity entered: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage());
        }
    }
}

private void insertSalesRecord(String productName, String companyName, int quantity, double totalPrice) {
    String insertSQL = "INSERT INTO sales_record (product_name, company_name, date_of_sale, quantity, amount) VALUES (?, ?, CURDATE(), ?, ?)";
    try (PreparedStatement pst = conn.prepareStatement(insertSQL)) {
        pst.setString(1, productName);
        pst.setString(2, companyName);
        pst.setInt(3, quantity);
        pst.setDouble(4, totalPrice);
        pst.executeUpdate();
        JOptionPane.showMessageDialog(null, "Sales record inserted successfully.");
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error inserting into sales_record: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        quantityTxt = new javax.swing.JTextField();
        productCategoryCombo = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        cartTable = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        medicineCombo = new javax.swing.JComboBox();
        productCompanyCombo = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        addToCartBtn = new javax.swing.JButton();
        removeBtn = new javax.swing.JButton();
        doneBtn = new javax.swing.JButton();
        backBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Medical Store Management System");

        jLabel1.setFont(new java.awt.Font("Calibri", 0, 18)); // NOI18N
        jLabel1.setText("Medical Store Management System");

        quantityTxt.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        quantityTxt.setEnabled(false);

        productCategoryCombo.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        productCategoryCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " " }));
        productCategoryCombo.setEnabled(false);
        productCategoryCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                productCategoryComboItemStateChanged(evt);
            }
        });
        productCategoryCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productCategoryComboActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jLabel2.setText("Product Category");

        jLabel3.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jLabel3.setText("Quantity");

        cartTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Medicine Name", "Company Name", "Product Category", "Quantity", "Price"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(cartTable);

        jLabel5.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jLabel5.setText("Select Medicine");

        medicineCombo.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        medicineCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " " }));
        medicineCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                medicineComboItemStateChanged(evt);
            }
        });
        medicineCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                medicineComboActionPerformed(evt);
            }
        });
        medicineCombo.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                medicineComboPropertyChange(evt);
            }
        });

        productCompanyCombo.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        productCompanyCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { " " }));
        productCompanyCombo.setEnabled(false);
        productCompanyCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                productCompanyComboItemStateChanged(evt);
            }
        });
        productCompanyCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productCompanyComboActionPerformed(evt);
            }
        });
        productCompanyCombo.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                productCompanyComboPropertyChange(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        jLabel6.setText("Company Name");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1087, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(quantityTxt))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(productCategoryCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(medicineCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(productCompanyCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(medicineCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(productCompanyCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(productCategoryCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(quantityTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        addToCartBtn.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        addToCartBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/NewSalesRecord/shop-cart-add-icon.png"))); // NOI18N
        addToCartBtn.setText("Add to cart");
        addToCartBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addToCartBtnActionPerformed(evt);
            }
        });

        removeBtn.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        removeBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/NewSalesRecord/Apps-Dialog-Remove-icon.png"))); // NOI18N
        removeBtn.setText("Remove");
        removeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBtnActionPerformed(evt);
            }
        });

        doneBtn.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        doneBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/NewSalesRecord/Accept-icon.png"))); // NOI18N
        doneBtn.setText("Confirm Purchases");
        doneBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneBtnActionPerformed(evt);
            }
        });

        backBtn.setFont(new java.awt.Font("Calibri", 0, 14)); // NOI18N
        backBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/NewSalesRecord/Go-back-icon.png"))); // NOI18N
        backBtn.setText("Back");
        backBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addToCartBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(54, 54, 54)
                        .addComponent(removeBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(46, 46, 46)
                        .addComponent(doneBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(36, 36, 36)
                        .addComponent(backBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(331, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jLabel1)
                .addGap(28, 28, 28)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(53, 53, 53)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(doneBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(removeBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addToCartBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(backBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(137, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void productCategoryComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_productCategoryComboItemStateChanged
        
    }//GEN-LAST:event_productCategoryComboItemStateChanged

    private void productCategoryComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productCategoryComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_productCategoryComboActionPerformed

    private void productCompanyComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_productCompanyComboItemStateChanged
        // TODO add your handling code here:
        

    }//GEN-LAST:event_productCompanyComboItemStateChanged

    private void productCompanyComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productCompanyComboActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_productCompanyComboActionPerformed

    private void productCompanyComboPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_productCompanyComboPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_productCompanyComboPropertyChange

    private void medicineComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_medicineComboActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_medicineComboActionPerformed

    private void medicineComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_medicineComboItemStateChanged
        // TODO add your handling code here:
        productCompanyCombo.enable(true);
        productCategoryCombo.enable(true);
        String selectedMedicine = (String) medicineCombo.getSelectedItem();

    try {
        // Query to get company name from the company_name table for the selected medicine
        String sqlCompany = "SELECT company FROM company_name WHERE product_name = ?";
        pst = conn.prepareStatement(sqlCompany);
        pst.setString(1, selectedMedicine); // Set the selected medicine as a parameter
        rs = pst.executeQuery();

        // Clear previous items in the company combo box
        productCompanyCombo.removeAllItems();

        // Populate company combo box
        while (rs.next()) {
            productCompanyCombo.addItem(rs.getString("company"));
        }

        // Query to get product category from the product_category table for the selected medicine
        String sqlCategory = "SELECT product FROM product_category WHERE product_name = ?";
        pst = conn.prepareStatement(sqlCategory);
        pst.setString(1, selectedMedicine); // Set the selected medicine as a parameter
        rs = pst.executeQuery();

        // Clear previous items in the category combo box
        productCategoryCombo.removeAllItems();

        // Populate category combo box
        while (rs.next()) {
            productCategoryCombo.addItem(rs.getString("product"));
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e);
    }
    }//GEN-LAST:event_medicineComboItemStateChanged

    private void medicineComboPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_medicineComboPropertyChange
        // TODO add your handling code here:
        quantityTxt.enable(true);
    }//GEN-LAST:event_medicineComboPropertyChange

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backBtnActionPerformed
        MainPanel mp = new MainPanel();
        this.dispose();        
        mp.setVisible(true);
    }//GEN-LAST:event_backBtnActionPerformed

    private void removeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnActionPerformed
        DefaultTableModel model = (DefaultTableModel)cartTable.getModel();
        if (cartTable.getSelectedRow() != -1) {
            model.removeRow(cartTable.getSelectedRow());
        }
    }//GEN-LAST:event_removeBtnActionPerformed

    private void doneBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneBtnActionPerformed
        // TODO add your handling code here:
                getPriceOfProductName();    
    }//GEN-LAST:event_doneBtnActionPerformed

    private void addToCartBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addToCartBtnActionPerformed
        // TODO add your handling code here:
        DefaultTableModel model = (DefaultTableModel) cartTable.getModel();

    // Get the selected medicine name, company, and category from the UI components
    String selectedMedicine = medicineCombo.getSelectedItem().toString();
    String selectedCompany = productCompanyCombo.getSelectedItem().toString();
    String selectedCategory = productCategoryCombo.getSelectedItem().toString();
    int selectedQuantity = Integer.parseInt(quantityTxt.getText());

    try {
        // Fetch the price per unit from the database for the selected medicine
        String sql = "SELECT price_per_unit FROM products WHERE product_name = ?";
        pst = conn.prepareStatement(sql);
        pst.setString(1, selectedMedicine);
        rs = pst.executeQuery();

        if (rs.next()) {
            double pricePerUnit = Double.parseDouble(rs.getString("price_per_unit"));
            double totalPrice = pricePerUnit * selectedQuantity;

            // Add the selected medicine details and calculated price to the table
            model.addRow(new Object[]{
                selectedMedicine,
                selectedCompany,
                selectedCategory,
                selectedQuantity,
                totalPrice
            });
        } else {
            JOptionPane.showMessageDialog(null, "Price not found for the selected medicine.");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage());
    }
    }//GEN-LAST:event_addToCartBtnActionPerformed
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        CustSalesRecord cs = new CustSalesRecord();
        cs.setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addToCartBtn;
    private javax.swing.JButton backBtn;
    private javax.swing.JTable cartTable;
    private javax.swing.JButton doneBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox medicineCombo;
    private javax.swing.JComboBox productCategoryCombo;
    private javax.swing.JComboBox productCompanyCombo;
    public javax.swing.JTextField quantityTxt;
    private javax.swing.JButton removeBtn;
    // End of variables declaration//GEN-END:variables
}
