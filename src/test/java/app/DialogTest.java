package app;


import model.BlockType;
import processing.core.PApplet;
import util.GBC;
import util.PSC;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class DialogTest extends PApplet {
    public void setup() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        createSwingDialog(0);
    }

    public void draw() {

    }

    private void createSwingDialog(int mapIdx) {
        final JDialog dialog = new JDialog(frame, "Select Data Set", true);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // main part

        GridBagLayout gbLayout = new GridBagLayout();
        JPanel centerPanel = new JPanel(gbLayout);

        JLabel infoLabel = new JLabel("Choose the base data set for map " + mapIdx);
        centerPanel.add(infoLabel, new GBC(0, 0, 2, 1).setAnchor(GBC.WEST).setInsets(10, 10, 15, 10));

        JLabel typeLabel = new JLabel("Data Type: ");
        JComboBox<BlockType> typeComboBox = new JComboBox<>(new BlockType[]{
                BlockType.FULL, BlockType.VFGS});
        centerPanel.add(typeLabel, new GBC(0, 1).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(typeComboBox, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        JLabel rateLabel = new JLabel("Sample Rate: ");
        JComboBox<Double> rateComboBox = new JComboBox<>(Arrays.stream(PSC.RATE_LIST)
                .boxed().toArray(Double[]::new));
        centerPanel.add(rateLabel, new GBC(0, 2).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(rateComboBox, new GBC(1, 2).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        JLabel deltaLabel = new JLabel("Delta: ");
        JComboBox<Integer> deltaComboBox = new JComboBox<>(Arrays.stream(PSC.DELTA_LIST)
                .boxed().toArray(Integer[]::new));
        centerPanel.add(deltaLabel, new GBC(0, 3).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(deltaComboBox, new GBC(1, 3).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // bottom part

        Box bottomBox = Box.createHorizontalBox();
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dialog.dispose());
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(cancelBtn);
        bottomBox.add(Box.createHorizontalStrut(20));
        bottomBox.add(okBtn);
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(bottomBox, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        PApplet.main(DialogTest.class.getName());
    }
}