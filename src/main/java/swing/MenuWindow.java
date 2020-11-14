package swing;

import app.CircleRegionControl;
import app.SharedObject;
import draw.TrajDrawManager;
import processing.core.PApplet;
import util.PSC;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuWindow extends JWindow {
    private final JLabel tipsLabel;

    public MenuWindow(int width, int height, PApplet pApplet) {
        setSize(width, height);
        setLocation(0, 0);

        int buttonWidth = width / 7;
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0));

        //button
        JButton oButton = new JButton("Origin");
        ActionListener oButtonActionListen = new ActionListener() {//监听
            @Override
            public void actionPerformed(ActionEvent ae) {
                SharedObject.getInstance().updateRegionPreList(0);
            }
        };
        oButton.addActionListener(oButtonActionListen);
        oButton.setSize(buttonWidth, height);


        JButton dButton = new JButton("Destination");
        ActionListener dButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(1);
            }
        };
        dButton.addActionListener(dButtonActionListen);
        dButton.setSize(buttonWidth, height);

        JButton wButton = new JButton("WayPoint");
        ActionListener wButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(2);

            }
        };
        wButton.addActionListener(wButtonActionListen);
        wButton.setSize(buttonWidth, height);

        JButton wLayerButton = new JButton("NextLayer");
        ActionListener wLayerButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (SharedObject.getInstance().isCircleRegion()) {
                    CircleRegionControl.getCircleRegionControl().updateLayer();
                } else {
                    SharedObject.getInstance().updateWLayer();
                }
            }
        };
        wLayerButton.addActionListener(wLayerButtonActionListen);
        wLayerButton.setSize(buttonWidth, height);

        JButton wGroupButton = new JButton("NextGroup");
        ActionListener wGroupButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (SharedObject.getInstance().isCircleRegion()) {
                    CircleRegionControl.getCircleRegionControl().addNewGroup();
                } else {
                    SharedObject.getInstance().addNewGroup();
                }
            }
        };
        wGroupButton.addActionListener(wGroupButtonActionListen);
        wGroupButton.setSize(buttonWidth, height);

        JButton dragButton = new JButton("DragRegionOff");
        ActionListener dragButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setDragRegion();
                if (SharedObject.getInstance().isDragRegion()) {
                    dragButton.setText("DragRegionOn");
                    dragButton.setBackground(Color.DARK_GRAY);

                } else {
                    dragButton.setText("DragRegionOff");
                    dragButton.setBackground(Color.GRAY);

                }
            }
        };
        dragButton.addActionListener(dragButtonActionListen);
        dragButton.setSize(buttonWidth, height);

        JButton finishSelectButton = new JButton("FinishSelect");
        ActionListener finishSelectButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().calTrajSelectResList();
            }
        };
        finishSelectButton.addActionListener(finishSelectButtonActionListen);
        finishSelectButton.setSize(buttonWidth, height);

        JButton screenShotButton = new JButton("ScreenShot");
        ActionListener screenShotButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setScreenShot(true);
            }
        };
        screenShotButton.addActionListener(screenShotButtonActionListen);
        screenShotButton.setSize(buttonWidth, height);

        JButton clearRegionButton = new JButton("ClearAllRegions");
        ActionListener clearRegionActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("clear all!");
                SharedObject.getInstance().setAllMainColor(PSC.RED);
                TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
                tdm.cleanAllImg(TrajDrawManager.SLT);
                tdm.startAllNewRenderTask(TrajDrawManager.MAIN);
//                SharedObject.getInstance().cleanRegions();
                CircleRegionControl.getCircleRegionControl().cleanCircleRegions();
                // clear old select res deeply
                SharedObject.getInstance().dropAllSelectRes();
            }
        };
        clearRegionButton.addActionListener(clearRegionActionListen);
        clearRegionButton.setSize(buttonWidth, height);

        JButton exitButton = new JButton("Exit");
        ActionListener exitButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pApplet.exit();
            }
        };

        oButton.setBackground(Color.GRAY);
        dButton.setBackground(Color.GRAY);
        wButton.setBackground(Color.GRAY);
        wLayerButton.setBackground(Color.GRAY);
        wGroupButton.setBackground(Color.GRAY);
        dragButton.setBackground(Color.GRAY);
        finishSelectButton.setBackground(Color.GRAY);
        screenShotButton.setBackground(Color.GRAY);
        clearRegionButton.setBackground(Color.GRAY);
        exitButton.setBackground(Color.GRAY);

        exitButton.addActionListener(exitButtonActionListen);
        exitButton.setSize(buttonWidth, height);

        Container panel2 = getContentPane();
        tipsLabel = new JLabel();
        tipsLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
        setTips("App Init");
        panel2.add(tipsLabel, BorderLayout.PAGE_START);
        panel2.add(panel, BorderLayout.CENTER);

        panel.add(oButton);
        panel.add(dButton);
        panel.add(wButton);
        panel.add(wLayerButton);
        panel.add(wGroupButton);
        panel.add(dragButton);
        panel.add(finishSelectButton);
        panel.add(screenShotButton);
        panel.add(clearRegionButton);
        panel.add(exitButton);

        setAlwaysOnTop(true);
    }

    public void setTips(String tips) {
        tipsLabel.setText(tips);
    }
}
